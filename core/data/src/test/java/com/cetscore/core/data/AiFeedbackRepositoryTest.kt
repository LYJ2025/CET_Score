package com.cetscore.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cetscore.core.data.db.CetScoreDatabase
import com.cetscore.core.data.entity.ExamRecord
import com.cetscore.core.data.entity.RecordKind
import com.cetscore.core.data.model.AssessmentDetail
import com.cetscore.core.data.model.QuestionGroupAnswer
import com.cetscore.core.data.repository.ExamRecordRepository
import com.cetscore.score.domain.model.ExamType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AI 批改的存取测试。
 *
 * 重点验证「按题型分开存」这件事：作文与翻译互不覆盖，
 * 否则用户存第二份时会把第一份弄丢。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AiFeedbackRepositoryTest {

    private lateinit var db: CetScoreDatabase
    private lateinit var repository: ExamRecordRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            CetScoreDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = ExamRecordRepository(db.examRecordDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun newRecord(): Long = repository.insertRecord(
        ExamRecord(
            examType = "CET6",
            createdAt = 1_700_000_000_000,
            totalScore = 520,
            listeningScore = 180f,
            readingScore = 180f,
            writingScore = 80f,
            translationScore = 80f,
            note = null,
            detailJson = repository.encodeDetail(
                AssessmentDetail(
                    groups = listOf(QuestionGroupAnswer("listening_1_15", 12)),
                    writingScore = 8,
                    translationScore = 8,
                )
            ),
        )
    )

    @Test
    fun `保存作文批改后应能读回`() = runTest {
        val id = newRecord()
        assertTrue(repository.saveAiFeedback(id, "WRITING", " 作文批改内容 ", "DeepSeek"))

        val entry = repository.decodeFeedback(repository.getById(id)!!).get("WRITING")
        assertNotNull(entry)
        assertEquals("作文批改内容", entry!!.text)   // 首尾空白被裁掉
        assertEquals("DeepSeek", entry.model)
    }

    @Test
    fun `作文与翻译两份批改互不覆盖`() = runTest {
        val id = newRecord()
        repository.saveAiFeedback(id, "WRITING", "作文那份", "DeepSeek")
        repository.saveAiFeedback(id, "TRANSLATION", "翻译那份", "DeepSeek")

        val set = repository.decodeFeedback(repository.getById(id)!!)
        assertEquals(2, set.entries.size)
        assertEquals("作文那份", set.get("WRITING")?.text)
        assertEquals("翻译那份", set.get("TRANSLATION")?.text)
    }

    @Test
    fun `同一题型重复保存视为覆盖`() = runTest {
        val id = newRecord()
        repository.saveAiFeedback(id, "WRITING", "第一版", "A")
        repository.saveAiFeedback(id, "WRITING", "第二版", "B")

        val set = repository.decodeFeedback(repository.getById(id)!!)
        assertEquals(1, set.entries.size)
        assertEquals("第二版", set.get("WRITING")?.text)
        assertEquals("B", set.get("WRITING")?.model)
    }

    @Test
    fun `删除某一题型不影响另一份`() = runTest {
        val id = newRecord()
        repository.saveAiFeedback(id, "WRITING", "作文那份", null)
        repository.saveAiFeedback(id, "TRANSLATION", "翻译那份", null)

        repository.removeAiFeedback(id, "WRITING")

        val set = repository.decodeFeedback(repository.getById(id)!!)
        assertNull(set.get("WRITING"))
        assertEquals("翻译那份", set.get("TRANSLATION")?.text)
    }

    @Test
    fun `删光所有批改后整列应为null`() = runTest {
        val id = newRecord()
        repository.saveAiFeedback(id, "WRITING", "唯一一份", null)
        repository.removeAiFeedback(id, "WRITING")

        assertNull(repository.getById(id)!!.aiFeedbackJson)
    }

    @Test
    fun `保存到不存在的记录应返回false且不抛异常`() = runTest {
        assertFalse(repository.saveAiFeedback(9999L, "WRITING", "内容", null))
    }

    @Test
    fun `新建仅批改记录分数为0且标记为AI_ONLY`() = runTest {
        val id = repository.createAiOnlyRecord(
            examType = ExamType.CET6,
            task = "WRITING",
            text = "只有批改",
            model = "DeepSeek",
            createdAt = 1_700_000_100_000,
        )

        val record = repository.getById(id)!!
        assertEquals(0, record.totalScore)
        assertEquals(RecordKind.AI_ONLY, record.recordKind)
        assertTrue(record.isAiOnly)
        assertEquals("只有批改", repository.decodeFeedback(record).get("WRITING")?.text)
        assertEquals(ExamType.CET6.name, record.examType)
    }

    @Test
    fun `普通记录默认是SCORE类型`() = runTest {
        val id = newRecord()
        val record = repository.getById(id)!!
        assertEquals(RecordKind.SCORE, record.recordKind)
        assertFalse(record.isAiOnly)
        assertFalse(record.hasAiFeedback)
    }

    @Test
    fun `kind为null的旧记录按SCORE处理`() = runTest {
        // 迁移 1→2 后旧记录的 kind 列是 NULL
        assertEquals(RecordKind.SCORE, RecordKind.fromName(null))
        assertEquals(RecordKind.SCORE, RecordKind.fromName("不认识的类型"))
        assertEquals(RecordKind.AI_ONLY, RecordKind.fromName("AI_ONLY"))
    }

    @Test
    fun `批改JSON损坏时按没有批改处理而不是崩溃`() = runTest {
        val id = newRecord()
        db.examRecordDao().updateAiFeedbackJson(id, "这不是 JSON{{{")

        val set = repository.decodeFeedback(repository.getById(id)!!)
        assertTrue(set.entries.isEmpty())
        assertFalse(set.hasContent)
    }
}

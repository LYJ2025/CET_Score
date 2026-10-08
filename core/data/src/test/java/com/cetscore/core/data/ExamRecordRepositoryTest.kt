package com.cetscore.core.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cetscore.core.data.db.CetScoreDatabase
import com.cetscore.core.data.entity.ExamRecord
import com.cetscore.core.data.model.AssessmentDetail
import com.cetscore.core.data.model.QuestionGroupAnswer
import com.cetscore.core.data.repository.ExamRecordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Room DAO 与 Repository 的增删改查测试。
 *
 * 用 Robolectric 在 JVM 上跑真实的 SQLite（不需要模拟器），
 * 数据库用内存库，测试之间互不干扰。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ExamRecordRepositoryTest {

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

    private fun sampleRecord(
        examType: String = "CET4",
        total: Int = 500,
        createdAt: Long = 1_700_000_000_000,
        note: String? = null,
    ) = ExamRecord(
        examType = examType,
        createdAt = createdAt,
        totalScore = total,
        listeningScore = 200f,
        readingScore = 180f,
        writingScore = 60f,
        translationScore = 60f,
        note = note,
        detailJson = repository.encodeDetail(
            AssessmentDetail(
                groups = listOf(QuestionGroupAnswer("listening_1_15", 12)),
                writingScore = 8,
                translationScore = 8,
            )
        ),
    )

    // ---------- 增 ----------

    @Test
    fun `插入记录应返回自增id且能被查到`() = runTest {
        val id = repository.insertRecord(sampleRecord())
        assertTrue("id 应大于 0", id > 0)

        val found = repository.getById(id)
        assertNotNull(found)
        assertEquals(500, found!!.totalScore)
        assertEquals("CET4", found.examType)
    }

    @Test
    fun `插入多条记录应全部可查`() = runTest {
        repository.insertRecord(sampleRecord(total = 400, createdAt = 1000))
        repository.insertRecord(sampleRecord(total = 500, createdAt = 2000))
        repository.insertRecord(sampleRecord(total = 600, createdAt = 3000))

        assertEquals(3, repository.count())
        val all = repository.getAllRecords().first()
        assertEquals(3, all.size)
    }

    // ---------- 查 ----------

    @Test
    fun `getAllRecords应按时间倒序`() = runTest {
        repository.insertRecord(sampleRecord(total = 400, createdAt = 1000))
        repository.insertRecord(sampleRecord(total = 600, createdAt = 3000))
        repository.insertRecord(sampleRecord(total = 500, createdAt = 2000))

        val all = repository.getAllRecords().first()
        // 倒序：3000 → 2000 → 1000
        assertEquals(listOf(600, 500, 400), all.map { it.totalScore })
    }

    @Test
    fun `按类型筛选应只返回对应考试`() = runTest {
        repository.insertRecord(sampleRecord(examType = "CET4", total = 450, createdAt = 1000))
        repository.insertRecord(sampleRecord(examType = "CET6", total = 550, createdAt = 2000))
        repository.insertRecord(sampleRecord(examType = "CET6", total = 600, createdAt = 3000))

        val cet4 = repository.getRecordsByType(
            com.cetscore.score.domain.model.ExamType.CET4
        ).first()
        assertEquals(1, cet4.size)
        assertEquals(450, cet4.first().totalScore)

        val cet6 = repository.getRecordsByType(
            com.cetscore.score.domain.model.ExamType.CET6
        ).first()
        assertEquals(2, cet6.size)
    }

    // ---------- 改 ----------

    @Test
    fun `更新备注应生效`() = runTest {
        val id = repository.insertRecord(sampleRecord(note = null))
        repository.updateNote(id, "这次阅读错很多")
        assertEquals("这次阅读错很多", repository.getById(id)?.note)
    }

    @Test
    fun `备注置空应存为null`() = runTest {
        val id = repository.insertRecord(sampleRecord(note = "原备注"))
        repository.updateNote(id, null)
        assertNull(repository.getById(id)?.note)
    }

    @Test
    fun `更新备注不影响其他字段`() = runTest {
        val id = repository.insertRecord(sampleRecord(total = 523, note = null))
        repository.updateNote(id, "新备注")

        val record = repository.getById(id)!!
        assertEquals("新备注", record.note)
        assertEquals(523, record.totalScore)      // 总分不变
        assertEquals(200f, record.listeningScore) // 各模块分数不变
    }

    // ---------- 删 ----------

    @Test
    fun `删除记录应从库中移除`() = runTest {
        val id = repository.insertRecord(sampleRecord())
        assertEquals(1, repository.count())

        repository.deleteRecord(id)
        assertEquals(0, repository.count())
        assertNull(repository.getById(id))
    }

    @Test
    fun `删除不存在的id不应报错`() = runTest {
        repository.insertRecord(sampleRecord())
        repository.deleteRecord(9999L)   // 不存在的 id
        assertEquals(1, repository.count())
    }

    // ---------- detailJson ----------

    @Test
    fun `detailJson应能正确编解码`() {
        val detail = AssessmentDetail(
            groups = listOf(
                QuestionGroupAnswer("listening_1_15", 12),
                QuestionGroupAnswer("reading_46_55", 7),
            ),
            writingScore = 11,
            translationScore = 9,
        )
        val json = repository.encodeDetail(detail)
        val back = repository.decodeDetail(json)

        assertEquals(2, back.groups.size)
        assertEquals(12, back.answerOf("listening_1_15"))
        assertEquals(7, back.answerOf("reading_46_55"))
        assertEquals(11, back.writingScore)
        assertEquals(9, back.translationScore)
    }

    @Test
    fun `detailJson字段顺序变化不影响解析`() {
        // 模拟老版本只有部分字段的 JSON
        val legacy = """{"groups":[{"groupId":"listening_1_15","correctCount":10}]}"""
        val detail = repository.decodeDetail(legacy)
        assertEquals(1, detail.groups.size)
        assertEquals(10, detail.answerOf("listening_1_15"))
        assertEquals(0, detail.writingScore)
    }

    @Test
    fun `detailJson非法内容应降级为空明细而非崩溃`() {
        val detail = repository.decodeDetail("这不是合法 JSON{{{")
        assertTrue(detail.groups.isEmpty())
        assertEquals(0, detail.writingScore)
    }

    @Test
    fun `含未知字段的detailJson应被忽略`() {
        val withExtra = """
            {"groups":[{"groupId":"a","correctCount":3,"unknownField":123}],"futureField":true}
        """.trimIndent()
        val detail = repository.decodeDetail(withExtra)
        assertEquals(3, detail.answerOf("a"))
    }
}

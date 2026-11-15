package com.cetscore.core.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cetscore.core.data.db.CetScoreDatabase
import com.cetscore.core.data.db.Migrations
import com.cetscore.core.data.repository.ExamRecordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 数据库升级的真实迁移测试。
 *
 * 做法：按 v1 的表结构手工建库并塞两条旧记录，再用 Room 以正式配置打开，
 * 让 `MIGRATION_1_2` 真正跑一遍。能验证：
 *  - 迁移 SQL 本身可执行
 *  - 迁移后的表结构与 entity 对得上（Room 会做 schema 校验，不一致直接抛异常）
 *  - 旧记录一条不丢
 *
 * 这类测试的价值在于：迁移写错时线下就暴露，
 * 而不是等用户升级时才发现数据没了。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationTest {

    private val dbName = "migration_1_to_2_test.db"
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    @Test
    fun `v1升级到v2后旧记录全部保留且新列为空`() = runTest {
        createV1DatabaseWithTwoRecords()

        val db = Room.databaseBuilder(context, CetScoreDatabase::class.java, dbName)
            .addMigrations(*Migrations.ALL)
            .allowMainThreadQueries()
            .build()
        val repository = ExamRecordRepository(db.examRecordDao())

        val all = repository.getAllRecords().first()
        assertEquals("两条旧记录都要在", 2, all.size)
        // 列表按 createdAt 倒序：316 那条时间更晚，排在前面
        assertEquals(listOf(316, 369), all.map { it.totalScore })

        // 新加的列对旧记录必须是空的，且不能被误判成「仅批改」
        assertTrue("旧记录不应有批改", all.all { it.aiFeedbackJson == null })
        assertTrue("旧记录不应有批改", all.all { !it.hasAiFeedback })
        assertTrue("旧记录都应视为 SCORE 类型", all.all { !it.isAiOnly })
        assertTrue("旧记录都应视为 SCORE 类型", all.all { it.recordKind.name == "SCORE" })

        // 迁移后写入批改也要正常
        val target = all.first { it.totalScore == 369 }
        repository.saveAiFeedback(target.id, "WRITING", "迁移后新存的批改", "DeepSeek")
        val reloaded = repository.getById(target.id)!!
        assertEquals(
            "迁移后新存的批改",
            repository.decodeFeedback(reloaded).get("WRITING")?.text,
        )
        assertNull(repository.getById(all.first { it.totalScore == 316 }.id)!!.aiFeedbackJson)

        db.close()
    }

    /** 按 v1（没有 aiFeedbackJson / kind 两列）建库 */
    private fun createV1DatabaseWithTwoRecords() {
        val path = context.getDatabasePath(dbName)
        path.parentFile?.mkdirs()

        val db = SQLiteDatabase.openOrCreateDatabase(path, null)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `exam_records` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `examType` TEXT NOT NULL,
                `createdAt` INTEGER NOT NULL,
                `totalScore` INTEGER NOT NULL,
                `listeningScore` REAL NOT NULL,
                `readingScore` REAL NOT NULL,
                `writingScore` REAL NOT NULL,
                `translationScore` REAL NOT NULL,
                `note` TEXT,
                `detailJson` TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO exam_records
            (examType, createdAt, totalScore, listeningScore, readingScore,
             writingScore, translationScore, note, detailJson)
            VALUES ('CET6', 1700000000000, 369, 120.5, 130.0, 60.0, 58.5, '旧备注',
                    '{"groups":[],"writingScore":0,"translationScore":0}')
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO exam_records
            (examType, createdAt, totalScore, listeningScore, readingScore,
             writingScore, translationScore, note, detailJson)
            VALUES ('CET4', 1700000100000, 316, 100.0, 110.0, 53.0, 53.0, NULL,
                    '{"groups":[],"writingScore":0,"translationScore":0}')
            """.trimIndent()
        )
        db.version = 1
        db.close()
    }
}

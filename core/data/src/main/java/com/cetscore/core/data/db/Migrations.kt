package com.cetscore.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移集中管理。
 *
 * ## 为什么要有这个文件
 *
 * 之前 `CetScoreDatabase` 用了 `fallbackToDestructiveMigration()`，
 * 意味着**任何表结构变化（version +1）都会直接删库重建** ——
 * 用户每升级一次 App，历史记录就被清空一次。
 *
 * 估分记录是用户长期积累的数据，不能因为开发改个字段就清空。
 *
 * ## 规则（必须遵守）
 *
 * 1. `@Database(version = N)` 每次 +1，**必须**在这里加 `MIGRATION_(N-1)_N`
 * 2. 新加的 Migration 写进 [ALL]，并按版本顺序排列
 * 3. 优先只做「新增表」「新增带默认值的列」；改列类型走「建新表 + 拷数据」
 * 4. **禁止**再引入 `fallbackToDestructiveMigration()`
 *
 * 漏写 Migration 时 Room 会抛 `IllegalStateException` 让升级失败，
 * 而不是静默清库 —— 宁可让开发者立刻发现，也不能让用户丢数据。
 */
object Migrations {

    /**
     * 1 → 2：记录表支持保存 AI 批改。
     *
     * 新增两列，都是可空的，旧记录无需回填：
     * - `aiFeedbackJson`：按题型保存的批改集合（JSON），null 表示没有批改
     * - `kind`：记录类型（SCORE / AI_ONLY）。旧记录为 NULL，
     *   由 `RecordKind.fromName` 兜底成 SCORE
     *
     * 只做 ADD COLUMN，不重建表，几万条记录也是毫秒级，不会卡启动。
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE exam_records ADD COLUMN aiFeedbackJson TEXT")
            db.execSQL("ALTER TABLE exam_records ADD COLUMN kind TEXT")
        }
    }

    /** 全部迁移，按版本顺序。 */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2)
}

package com.cetscore.core.data.db

import androidx.room.migration.Migration

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
 *
 * 当前数据库版本为 1，尚无历史迁移。
 * AI 配置存在 SharedPreferences（见 ApiConfigStore），不进数据库，
 * 因此本版本不需要新增表。
 */
object Migrations {

    /** 全部迁移，按版本顺序。目前版本 1 起步，暂无迁移。 */
    val ALL: Array<Migration> = emptyArray()
}

package com.cetscore.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cetscore.core.data.dao.ExamRecordDao
import com.cetscore.core.data.entity.ExamRecord

@Database(
    entities = [ExamRecord::class],
    version = 1,
    exportSchema = false,
)
abstract class CetScoreDatabase : RoomDatabase() {

    abstract fun examRecordDao(): ExamRecordDao

    companion object {
        const val DB_NAME = "cet_six_score.db"

        @Volatile
        private var instance: CetScoreDatabase? = null

        fun get(context: Context): CetScoreDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): CetScoreDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                CetScoreDatabase::class.java,
                DB_NAME,
            )
                // 纯本地数据库，不做任何网络同步
                //
                // 注意：这里**严禁**使用 fallbackToDestructiveMigration()。
                // 它会在表结构变化时直接删库重建，导致用户升级一次就丢光历史记录。
                // 以后 version +1 时，必须在 Migrations.ALL 里补上对应迁移。
                .addMigrations(*Migrations.ALL)
                .build()
    }
}

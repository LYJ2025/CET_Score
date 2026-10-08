package com.cetsix.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.cetsix.core.data.dao.ExamRecordDao
import com.cetsix.core.data.entity.ExamRecord

@Database(
    entities = [ExamRecord::class],
    version = 1,
    exportSchema = false,
)
abstract class CetSixDatabase : RoomDatabase() {

    abstract fun examRecordDao(): ExamRecordDao

    companion object {
        const val DB_NAME = "cet_six_score.db"

        @Volatile
        private var instance: CetSixDatabase? = null

        fun get(context: Context): CetSixDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): CetSixDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                CetSixDatabase::class.java,
                DB_NAME,
            )
                // 纯本地数据库，不做任何网络同步
                .fallbackToDestructiveMigration()
                .build()
    }
}

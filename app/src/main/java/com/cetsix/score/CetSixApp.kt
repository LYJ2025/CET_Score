package com.cetsix.score

import android.app.Application
import com.cetsix.core.data.db.CetSixDatabase
import com.cetsix.core.data.repository.ExamRecordRepository

/**
 * 应用级依赖容器。
 *
 * 刻意不用 Hilt —— 这个 App 只有一个数据库、一个 Repository，
 * 手写单例比引入注解处理器更轻，KSP 负担也更小。
 */
class CetSixApp : Application() {

    /** 懒加载：首次访问时才建数据库，避免启动时阻塞 */
    val repository: ExamRecordRepository by lazy {
        ExamRecordRepository(CetSixDatabase.get(this).examRecordDao())
    }
}

package com.cetscore.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.cetscore.core.data.entity.ExamRecord
import kotlinx.coroutines.flow.Flow

/**
 * 估分记录 DAO。
 *
 * 全部读取接口返回 Flow，配合 ViewModel + StateFlow 实现数据变化自动刷新。
 */
@Dao
interface ExamRecordDao {

    /** 按时间倒序读取全部记录（历史页 / 趋势图共用） */
    @Query("SELECT * FROM exam_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ExamRecord>>

    /** 按考试类型过滤（趋势图分 CET4 / CET6 展示） */
    @Query("SELECT * FROM exam_records WHERE examType = :examType ORDER BY createdAt DESC")
    fun observeByExamType(examType: String): Flow<List<ExamRecord>>

    @Query("SELECT * FROM exam_records WHERE id = :id")
    suspend fun getById(id: Long): ExamRecord?

    @Insert
    suspend fun insert(record: ExamRecord): Long

    /** 只更新备注，历史页的"编辑备注"用 */
    @Query("UPDATE exam_records SET note = :note WHERE id = :id")
    suspend fun updateNote(id: Long, note: String?)

    @Query("DELETE FROM exam_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM exam_records")
    suspend fun count(): Int
}

package com.cetsix.core.data.repository

import com.cetsix.core.data.dao.ExamRecordDao
import com.cetsix.core.data.entity.ExamRecord
import com.cetsix.core.data.model.AssessmentDetail
import com.cetsix.score.domain.model.ExamType   // 统一用 domain 层的枚举，避免两份定义
import com.cetsix.core.data.model.QuestionGroupAnswer
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

/**
 * 估分记录仓库。ViewModel 只依赖它，不直接碰 DAO。
 *
 * detailJson 的序列化/反序列化封装在这里，
 * 上层只传 [AssessmentDetail] 对象，不接触 JSON 字符串。
 */
class ExamRecordRepository(
    private val dao: ExamRecordDao,
    private val json: Json = DefaultJson,
) {

    fun getAllRecords(): Flow<List<ExamRecord>> = dao.observeAll()

    fun getRecordsByType(examType: ExamType): Flow<List<ExamRecord>> =
        dao.observeByExamType(examType.name)

    suspend fun getById(id: Long): ExamRecord? = dao.getById(id)

    suspend fun count(): Int = dao.count()

    /** 插入并返回自增 id */
    suspend fun insertRecord(record: ExamRecord): Long = dao.insert(record)

    suspend fun updateNote(id: Long, note: String?) = dao.updateNote(id, note)

    suspend fun deleteRecord(id: Long) = dao.deleteById(id)

    // ---------- detailJson 辅助 ----------

    fun encodeDetail(detail: AssessmentDetail): String = json.encodeToString(detail)

    /** 解析失败时返回空明细，避免一条脏数据导致整个历史页崩溃 */
    fun decodeDetail(detailJson: String): AssessmentDetail =
        runCatching { json.decodeFromString<AssessmentDetail>(detailJson) }
            .getOrElse { AssessmentDetail(emptyList(), 0, 0) }

    fun decodeDetailList(detailJson: String): List<QuestionGroupAnswer> =
        decodeDetail(detailJson).groups

    companion object {
        val DefaultJson: Json = Json {
            ignoreUnknownKeys = true   // 以后新增字段时老记录仍可解析
            encodeDefaults = true
            prettyPrint = false
        }
    }
}

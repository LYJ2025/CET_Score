package com.cetscore.core.data.repository

import com.cetscore.core.data.dao.ExamRecordDao
import com.cetscore.core.data.entity.ExamRecord
import com.cetscore.core.data.entity.RecordKind
import com.cetscore.core.data.model.AiFeedbackEntry
import com.cetscore.core.data.model.AiFeedbackSet
import com.cetscore.core.data.model.AssessmentDetail
import com.cetscore.score.domain.model.ExamType   // 统一用 domain 层的枚举，避免两份定义
import com.cetscore.core.data.model.QuestionGroupAnswer
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

    // ---------- AI 批改 ----------

    /** 解析记录上的批改集合，脏数据按"没有批改"处理 */
    fun decodeFeedback(record: ExamRecord): AiFeedbackSet =
        record.aiFeedbackJson?.let { raw ->
            runCatching { json.decodeFromString<AiFeedbackSet>(raw) }.getOrNull()
        } ?: AiFeedbackSet()

    private fun encodeFeedback(set: AiFeedbackSet): String? =
        if (set.isEmpty) null else json.encodeToString(set)

    /**
     * 把一份批改挂到已有记录上。
     *
     * 按题型存：已有作文批改时再存翻译，两份都会保留。
     * 同一题型重复保存视为覆盖（用户重新评了一次）。
     *
     * @return 记录是否存在
     */
    suspend fun saveAiFeedback(
        recordId: Long,
        task: String,
        text: String,
        model: String?,
        updatedAt: Long = System.currentTimeMillis(),
    ): Boolean {
        val record = dao.getById(recordId) ?: return false
        val updated = decodeFeedback(record).with(
            task,
            AiFeedbackEntry(text = text.trim(), model = model, updatedAt = updatedAt),
        )
        dao.updateAiFeedbackJson(recordId, encodeFeedback(updated))
        return true
    }

    /** 删掉某一题型的批改；删空后整列置 null */
    suspend fun removeAiFeedback(recordId: Long, task: String) {
        val record = dao.getById(recordId) ?: return
        val set = decodeFeedback(record)
        if (set.get(task) == null) return
        dao.updateAiFeedbackJson(recordId, encodeFeedback(set.without(task)))
    }

    /**
     * 新建一条「只有 AI 批改」的记录。
     *
     * 分数与题组明细都留空（全 0），kind 标记为 AI_ONLY，
     * 历史页据此显示"仅 AI 批改"，而不是把 0 分当成真实估分。
     */
    suspend fun createAiOnlyRecord(
        examType: ExamType,
        task: String,
        text: String,
        model: String?,
        createdAt: Long = System.currentTimeMillis(),
    ): Long {
        val set = AiFeedbackSet().with(
            task,
            AiFeedbackEntry(text = text.trim(), model = model, updatedAt = createdAt),
        )
        return dao.insert(
            ExamRecord(
                examType = examType.name,
                createdAt = createdAt,
                totalScore = 0,
                listeningScore = 0f,
                readingScore = 0f,
                writingScore = 0f,
                translationScore = 0f,
                note = null,
                detailJson = encodeDetail(AssessmentDetail()),
                aiFeedbackJson = encodeFeedback(set),
                kind = RecordKind.AI_ONLY.name,
            )
        )
    }

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

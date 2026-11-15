package com.cetscore.core.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** 记录类型 */
enum class RecordKind {
    /** 正常估分记录：有四模块分数与题组明细 */
    SCORE,

    /**
     * 只保存 AI 批改的记录。
     *
     * 用户可能只想留下一次作文/翻译的批改，还没做完整套题的估分，
     * 这类记录的分数全为 0，但**不能显示成 0 分**，否则会被误认为考砸了。
     */
    AI_ONLY,
    ;

    companion object {
        fun fromName(name: String?): RecordKind =
            entries.firstOrNull { it.name == name } ?: SCORE
    }
}

/**
 * 一次估分记录。
 *
 * 各模块分数均为 **710 分制**（不是百分制）：
 * 听力 248.5 / 阅读 248.5 / 写作 106.5 / 翻译 106.5，合计 710。
 *
 * detailJson 存各题组的对错明细，用 kotlinx.serialization 序列化，
 * 便于以后扩展题型而不需要改表结构。
 *
 * aiFeedbackJson 存按题型分开的 AI 批改（见 AiFeedbackSet），
 * 同一列里可同时容纳作文与翻译两份，互不覆盖。
 */
@Entity(tableName = "exam_records")
data class ExamRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    /** CET4 / CET6，以 name 存字符串而非枚举下标，避免枚举顺序变化导致数据错乱 */
    @ColumnInfo(name = "examType")
    val examType: String,

    @ColumnInfo(name = "createdAt")
    val createdAt: Long,

    /** 总分 0..710 */
    @ColumnInfo(name = "totalScore")
    val totalScore: Int,

    @ColumnInfo(name = "listeningScore")
    val listeningScore: Float,

    @ColumnInfo(name = "readingScore")
    val readingScore: Float,

    @ColumnInfo(name = "writingScore")
    val writingScore: Float,

    @ColumnInfo(name = "translationScore")
    val translationScore: Float,

    /** 备注，长度不限制 */
    @ColumnInfo(name = "note")
    val note: String? = null,

    /** QuestionGroupAnswer 列表的 JSON */
    @ColumnInfo(name = "detailJson")
    val detailJson: String,

    /** AiFeedbackSet 的 JSON；null 表示没有任何 AI 批改 */
    @ColumnInfo(name = "aiFeedbackJson")
    val aiFeedbackJson: String? = null,

    /**
     * 记录类型。
     *
     * 可空是刻意的：迁移 1→2 只加列不写值时，旧记录这一列是 NULL，
     * 由 [RecordKind.fromName] 兜底成 SCORE。
     * 这样迁移不必依赖 SQL 默认值，Room 的 schema 校验也不会因为
     * 默认值写法差异而失败。
     */
    @ColumnInfo(name = "kind")
    val kind: String? = null,
) {
    val recordKind: RecordKind get() = RecordKind.fromName(kind)

    /** 只保存了 AI 批改、没有估分结果 */
    val isAiOnly: Boolean get() = recordKind == RecordKind.AI_ONLY

    /** 是否挂着 AI 批改（可能有内容，也可能是空串） */
    val hasAiFeedback: Boolean get() = !aiFeedbackJson.isNullOrBlank()
}

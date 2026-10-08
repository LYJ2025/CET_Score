package com.cetsix.core.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 一次估分记录。
 *
 * 各模块分数均为 **710 分制**（不是百分制）：
 * 听力 248.5 / 阅读 248.5 / 写作 106.5 / 翻译 106.5，合计 710。
 *
 * detailJson 存各题组的对错明细，用 kotlinx.serialization 序列化，
 * 便于以后扩展题型而不需要改表结构。
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
)

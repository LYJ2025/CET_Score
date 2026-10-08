package com.cetscore.core.data.model

import kotlinx.serialization.Serializable

/**
 * 题组定义 —— **不建表**，用 data class + JSON 序列化存入 detailJson。
 *
 * 为什么不建表：题组结构在四六级之间基本固定（只有听力题组的名称不同），
 * 且未来若新增题型也不需要迁移表结构。存 JSON 更灵活。
 *
 * @param id 唯一标识，例如 "listening_1_15"
 * @param title 简标题，例如 "听力 1-15"
 * @param fullName 全称，例如 "听力理解 · 短篇新闻与长对话"
 * @param startIndex 起始题号（含）
 * @param endIndex 结束题号（含）
 * @param perQuestionScore 每题原始分
 * @param totalRawScore 该组原始总分
 */
@Serializable
data class QuestionGroup(
    val id: String,
    val title: String,
    val fullName: String,
    val startIndex: Int,
    val endIndex: Int,
    val perQuestionScore: Float,
    val totalRawScore: Float,
) {
    /** 该组题目数量 = endIndex - startIndex + 1 */
    val questionCount: Int get() = endIndex - startIndex + 1

    init {
        require(startIndex >= 1) { "startIndex 必须 >= 1" }
        require(endIndex >= startIndex) { "endIndex 必须 >= startIndex" }
        require(perQuestionScore > 0f) { "perQuestionScore 必须 > 0" }
    }
}

/**
 * 用户对某个题组的作答情况 —— 存进 detailJson 的实际结构。
 *
 * @param correctCount 用户自评答对的题数
 */
@Serializable
data class QuestionGroupAnswer(
    val groupId: String,
    val correctCount: Int,
) {
    init {
        require(correctCount >= 0) { "correctCount 不能为负" }
    }
}

/** 一次估分的完整作答明细，对应 detailJson 字段 */
@Serializable
data class AssessmentDetail(
    val groups: List<QuestionGroupAnswer> = emptyList(),
    val writingScore: Int = 0,
    val translationScore: Int = 0,
) {
    init {
        require(writingScore in 0..MAX_WRITING_SCORE) { "写作分数必须在 0..$MAX_WRITING_SCORE" }
        require(translationScore in 0..MAX_TRANSLATION_SCORE) { "翻译分数必须在 0..$MAX_TRANSLATION_SCORE" }
    }

    fun answerOf(groupId: String): Int =
        groups.firstOrNull { it.groupId == groupId }?.correctCount ?: 0

    companion object {
        const val MAX_WRITING_SCORE = 15
        const val MAX_TRANSLATION_SCORE = 15
    }
}

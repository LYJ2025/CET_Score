package com.cetsix.score.domain.model

/**
 * 估分结果。
 *
 * 分值口径说明：
 *  - rawScore / percentScore 为 **百分制**（听力阅读各 35，写作翻译各 15）
 *  - totalScore 为 **710 分制**
 *  - proportion 为该部分占总分（710 制）的百分比，六部分之和恒为 100.0
 */
data class ScoreResult(
    val examType: ExamType,
    /** 710 分制总分，0..710 */
    val totalScore: Int,
    /** 百分制总分，0..100 */
    val percentTotal: Float,
    val listeningPercent: Float,
    val readingPercent: Float,
    val writingPercent: Float,
    val translationPercent: Float,
    /** 各模块的 710 分制得分 */
    val listeningTotal: Float,
    val readingTotal: Float,
    val writingTotal: Float,
    val translationTotal: Float,
    /** 客观题题组明细 */
    val groupResults: List<GroupResult>,
    /** 各部分占比，之和为 100.0 */
    val proportions: List<Proportion>,
) {
    val isPassed: Boolean get() = totalScore >= ExamConfig.PASS_SCORE

    /** 距及格线还差多少分，已通过时返回 0 */
    val gapToPass: Int get() = (ExamConfig.PASS_SCORE - totalScore).coerceAtLeast(0)

    val allDetails: List<GroupResult> get() = groupResults

    companion object {
        val ZERO: ScoreResult = ScoreCalculator.calculate(
            config = ExamConfig.CET4,
            answers = emptyMap(),
            writingScore = 0,
            translationScore = 0,
        )
    }
}

/** 单个题组的得分明细 */
data class GroupResult(
    val groupId: String,
    val title: String,
    val fullName: String,
    val section: ScoreSection,
    val correctCount: Int,
    val questionCount: Int,
    /** 百分制原始得分（0..totalRawScore） */
    val rawScore: Float,
    /** 该组满分（百分制） */
    val rawFullScore: Float,
    /** 710 分制得分 */
    val totalScore: Float,
    /** 正确率 0..1 */
    val accuracy: Float,
) {
    /** "12/15" 形式的正确率文本 */
    val accuracyText: String get() = "$correctCount/$questionCount"
}

/**
 * 各部分占比。
 *
 * @param proportion 百分比 0..100，六部分之和恒为 100.0（最大余数法保证）
 */
data class Proportion(
    val section: ScoreSection,
    val fullName: String,
    val rawScore: Float,
    val totalScore: Float,
    val proportion: Float,
) {
    /** "35.0%" 形式的占比文本，保留一位小数 */
    val proportionText: String
        get() = if (proportion <= 0f) "0.0%"
        else "${trimTrailingZero(proportion)}%"

    private fun trimTrailingZero(value: Float): String {
        val rounded = Math.round(value * 10.0) / 10.0
        return if (rounded % 1.0 == 0.0) rounded.toInt().toString()
        else rounded.toString()
    }
}

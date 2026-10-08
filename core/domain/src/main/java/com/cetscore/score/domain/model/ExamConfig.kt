package com.cetscore.score.domain.model

/**
 * 题组定义（配置用，不含用户作答）。
 *
 * @param id 唯一标识，如 "listening_1_15"
 * @param title 简标题，如 "听力 1-15"
 * @param fullName 全称，如 "听力理解 · 短篇新闻与长对话"
 * @param startIndex 起始题号（含）
 * @param endIndex 结束题号（含）
 * @param perQuestionScore 每题原始分
 * @param totalRawScore 该组原始总分
 */
data class QuestionGroup(
    val id: String,
    val title: String,
    val fullName: String,
    val startIndex: Int,
    val endIndex: Int,
    val perQuestionScore: Float,
    val totalRawScore: Float,
) {
    val questionCount: Int get() = endIndex - startIndex + 1

    init {
        require(startIndex >= 1) { "startIndex 必须 >= 1" }
        require(endIndex >= startIndex) { "endIndex 必须 >= startIndex" }
        require(perQuestionScore > 0f) { "perQuestionScore 必须 > 0" }
    }
}

/** 题组所属的部分，用于结果页的模块归类 */
enum class ScoreSection(val label: String) {
    LISTENING("听力理解"),
    READING("阅读理解"),
    WRITING("写作"),
    TRANSLATION("翻译"),
}

/**
 * 一份考试配置：题组 + 评分标准。
 * 四级与六级各一份，题型不同但换算逻辑一致。
 */
data class ExamConfig(
    val examType: ExamType,
    val listeningGroups: List<QuestionGroup>,
    val readingGroups: List<QuestionGroup>,
    val writingRubric: WritingTranslationRubric,
    val translationRubric: WritingTranslationRubric,
) {
    val allGroups: List<QuestionGroup> get() = listeningGroups + readingGroups

    /** 听力原始总分（35） */
    val listeningRawTotal: Float get() = listeningGroups.sumOf { it.totalRawScore.toDouble() }.toFloat()

    /** 阅读原始总分（35） */
    val readingRawTotal: Float get() = readingGroups.sumOf { it.totalRawScore.toDouble() }.toFloat()

    fun groupById(id: String): QuestionGroup? = allGroups.firstOrNull { it.id == id }

    companion object {
        /** 百分制官方比例：听力 35 / 阅读 35 / 写作 15 / 翻译 15 = 100 */
        const val LISTENING_WEIGHT = 35f
        const val READING_WEIGHT = 35f
        const val WRITING_WEIGHT = 15f
        const val TRANSLATION_WEIGHT = 15f

        /** 100（百分制）× 7.1 = 710（总分制） */
        const val TOTAL_SCORE = 710

        /** 及格线 */
        const val PASS_SCORE = 425

        val CET4: ExamConfig = ExamConfig(
            examType = ExamType.CET4,
            listeningGroups = listOf(
                QuestionGroup(
                    id = "listening_1_15",
                    title = "听力 1-15",
                    fullName = "听力理解 · 短篇新闻与长对话",
                    startIndex = 1,
                    endIndex = 15,
                    perQuestionScore = 1f,
                    totalRawScore = 15f,
                ),
                QuestionGroup(
                    id = "listening_16_25",
                    title = "听力 16-25",
                    fullName = "听力理解 · 听力篇章",
                    startIndex = 16,
                    endIndex = 25,
                    perQuestionScore = 2f,
                    totalRawScore = 20f,
                ),
            ),
            readingGroups = listOf(
                QuestionGroup(
                    id = "reading_26_35",
                    title = "阅读 26-35",
                    fullName = "阅读理解 · 选词填空",
                    startIndex = 26,
                    endIndex = 35,
                    perQuestionScore = 0.5f,
                    totalRawScore = 5f,
                ),
                QuestionGroup(
                    id = "reading_36_45",
                    title = "阅读 36-45",
                    fullName = "阅读理解 · 长篇阅读匹配",
                    startIndex = 36,
                    endIndex = 45,
                    perQuestionScore = 1f,
                    totalRawScore = 10f,
                ),
                QuestionGroup(
                    id = "reading_46_55",
                    title = "阅读 46-55",
                    fullName = "阅读理解 · 仔细阅读",
                    startIndex = 46,
                    endIndex = 55,
                    perQuestionScore = 2f,
                    totalRawScore = 20f,
                ),
            ),
            writingRubric = WritingTranslationRubric.writingCet4(),
            translationRubric = WritingTranslationRubric.translationCet4(),
        )

        val CET6: ExamConfig = ExamConfig(
            examType = ExamType.CET6,
            listeningGroups = listOf(
                QuestionGroup(
                    id = "listening_1_15",
                    title = "听力 1-15",
                    fullName = "听力理解 · 长对话与听力篇章",
                    startIndex = 1,
                    endIndex = 15,
                    perQuestionScore = 1f,
                    totalRawScore = 15f,
                ),
                QuestionGroup(
                    id = "listening_16_25",
                    title = "听力 16-25",
                    fullName = "听力理解 · 讲座/讲话",
                    startIndex = 16,
                    endIndex = 25,
                    perQuestionScore = 2f,
                    totalRawScore = 20f,
                ),
            ),
            // 阅读题组四六级完全相同
            readingGroups = CET4.readingGroups,
            writingRubric = WritingTranslationRubric.writingCet6(),
            translationRubric = WritingTranslationRubric.translationCet6(),
        )

        fun of(type: ExamType): ExamConfig = when (type) {
            ExamType.CET4 -> CET4
            ExamType.CET6 -> CET6
        }
    }
}

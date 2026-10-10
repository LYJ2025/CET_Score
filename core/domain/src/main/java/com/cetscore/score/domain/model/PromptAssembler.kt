package com.cetscore.score.domain.model

/**
 * 提示词组装 —— **纯函数，不涉及 IO**。
 *
 * 四份模板各自都是完整提示词，拼接时只做两件事：
 * 1. 取对应「级别 + 题型」的模板正文（**绝不混入其他模块**）
 * 2. 追加用户的原题目与作答
 */
object PromptAssembler {

    /** 作答区引导语 */
    const val ANSWER_GUIDE = "【我的作答】："

    /** 原题目区引导语 */
    const val QUESTION_GUIDE = "【题目要求/翻译原文】："

    /** 未填原题目时的提示 */
    const val NO_QUESTION_HINT = "（未填原题目，AI 可能无法判断切题度）"

    /** 原题目过长时的提示 */
    const val QUESTION_TOO_LONG_HINT = "（原题目过长，已截断，请核对题干是否完整）"

    /** 原题目字数上限 —— 超过则截断，避免挤占 AI 上下文 */
    const val MAX_QUESTION_LENGTH = 2_000

    /**
     * 组装最终要复制的内容。
     *
     * 结构：
     * ```
     * {对应模块的完整提示词}
     *
     * ---
     *
     * 【题目要求/翻译原文】：
     * {原题目}
     *
     * 【我的作答】：
     * {用户作答}
     * ```
     *
     * @param templateBody 该模块模板的正文（已从 assets 读出）
     * @param question 原题目 / 翻译原文（可空）
     * @param answer 用户作答 / 译文
     */
    fun assemble(
        templateBody: String,
        question: String?,
        answer: String,
    ): String = buildString {
        append(templateBody.trimEnd())
        append("\n\n---\n\n")
        append(QUESTION_GUIDE).append('\n')
        append(normalizeQuestion(question))
        append("\n\n")
        append(ANSWER_GUIDE).append('\n')
        append(answer.trim())
    }

    /**
     * 处理原题目：空则给提示，过长则截断。
     */
    fun normalizeQuestion(question: String?): String {
        val q = question?.trim().orEmpty()
        return when {
            q.isEmpty() -> NO_QUESTION_HINT
            q.length > MAX_QUESTION_LENGTH ->
                q.take(MAX_QUESTION_LENGTH) + "\n" + QUESTION_TOO_LONG_HINT
            else -> q
        }
    }

    /** 原题目为空时应提示用户 */
    fun shouldWarnMissingQuestion(question: String?): Boolean =
        question?.trim().isNullOrEmpty()

    /** 原题目是否超长 */
    fun isQuestionTooLong(question: String?): Boolean =
        (question?.trim()?.length ?: 0) > MAX_QUESTION_LENGTH
}

/**
 * 作文词数要求 —— **仅作文适用**。
 *
 * 四级 120 words to 180 words；六级 150 words to 200 words。
 * 翻译题型没有词数要求，因此这里只覆盖 WRITING。
 */
data class EssayLengthRule(
    val minWords: Int,
    val maxWords: Int,
) {
    /** 展示文案，例如「120 words to 180 words」 */
    val display: String get() = "$minWords words to $maxWords words"

    companion object {
        val CET4 = EssayLengthRule(120, 180)
        val CET6 = EssayLengthRule(150, 200)

        fun of(examType: ExamType): EssayLengthRule = when (examType) {
            ExamType.CET4 -> CET4
            ExamType.CET6 -> CET6
        }

        /**
         * 底部提示文案。
         *
         * 作文返回对应级别的词数要求；**翻译返回 null** —— 不显示任何词数注释。
         */
        fun footerHint(examType: ExamType, questionType: QuestionType): String? =
            when (questionType) {
                QuestionType.WRITING ->
                    "${examType.shortLabel}作文词数要求：${of(examType).display}"

                QuestionType.TRANSLATION -> null
            }
    }
}
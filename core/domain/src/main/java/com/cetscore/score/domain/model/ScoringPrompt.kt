package com.cetscore.score.domain.model

/**
 * 提示词模板处理 —— 纯函数，不涉及 IO 与 Android。
 *
 * 刻意放在 core:domain：
 * 这样占位符替换、复制文本拼接这些**规则性逻辑**
 * 可以脱离 Android 运行时直接跑单元测试。
 */
object ScoringPrompt {

    /** 模板中的占位符 */
    const val PLACEHOLDER_EXAM = "{考试级别}"
    const val PLACEHOLDER_TASK = "{题型}"

    /** 作答区引导语 —— 模板末尾自带；用户未填内容时保留 */
    const val ANSWER_GUIDE = "【我的作答】："

    /**
     * 替换模板中的占位符。
     *
     * @param raw 模板原文
     * @param examType 四级 / 六级
     * @param taskType 作文 / 翻译
     */
    fun buildPrompt(raw: String, examType: ExamType, taskType: ScoringTask): String =
        raw.replace(PLACEHOLDER_EXAM, examType.shortLabel)
            .replace(PLACEHOLDER_TASK, taskType.label)

    /**
     * 拼接最终要复制到剪贴板的内容。
     *
     * 规则：
     *  - 用户填了作答 → `提示词` + 空行 + `【我的作答】：` + 换行 + 内容
     *  - 用户没填       → 只返回提示词（模板末尾自带引导语，由用户自己粘贴）
     */
    fun buildCopyText(prompt: String, userAnswer: String?): String {
        val answer = userAnswer?.trim().orEmpty()
        return if (answer.isEmpty()) prompt else "$prompt\n\n$ANSWER_GUIDE\n$answer"
    }

    /** 模板中是否还残留未替换的占位符 */
    fun hasUnreplacedPlaceholder(text: String): Boolean =
        text.contains(PLACEHOLDER_EXAM) || text.contains(PLACEHOLDER_TASK)
}

/** 题型 */
enum class ScoringTask(val label: String) {
    ESSAY("作文"),
    TRANSLATION("翻译"),
}

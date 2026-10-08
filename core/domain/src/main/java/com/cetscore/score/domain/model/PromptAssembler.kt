package com.cetscore.score.domain.model

/**
 * 提示词组装器 —— **纯函数，不涉及 IO 与 Android**。
 *
 * 架构：公共部分 + 四套独立模块，按固定顺序拼接：
 * ```
 * common
 * + 对应模块（cet4-writing / cet4-translation / cet6-writing / cet6-translation）
 * + 【考试级别】
 * + 【题型】
 * + 【题目要求 / 翻译原文】
 * + 【我的作答】
 * + 输出格式要求
 * ```
 *
 * 放在 core:domain 的原因：拼接规则是纯逻辑，
 * 应能脱离 Android 运行时直接单元测试。
 */
object PromptAssembler {

    /** assets 中的目录 */
    const val MODULE_DIR = "prompts/modules"

    /** 公共部分文件名 */
    const val FILE_COMMON = "common.md"

    /** 输出格式文件名（所有题型共用） */
    const val FILE_OUTPUT_FORMAT = "output-format.md"

    /** 标记原题目为空时的提示 */
    const val NO_QUESTION_HINT = "（未填原题目，AI 可能无法判断切题度）"

    /** 标记原题目过长时的提示 */
    const val QUESTION_TOO_LONG_HINT = "（原题目过长，已截断，请核对题干是否完整）"

    /** 原题目字数上限 —— 超过则截断，避免提示词过长挤占 AI 上下文 */
    const val MAX_QUESTION_LENGTH = 2_000

    /**
     * 四套独立模块的文件名。
     *
     * 用嵌套 when 而非并列条件，避免出现拼写错误导致落到 else 分支。
     */
    fun moduleFileName(examType: ExamType, taskType: ScoringTask): String =
        when (examType) {
            ExamType.CET4 -> when (taskType) {
                ScoringTask.ESSAY -> "cet4-writing.md"
                ScoringTask.TRANSLATION -> "cet4-translation.md"
            }

            ExamType.CET6 -> when (taskType) {
                ScoringTask.ESSAY -> "cet6-writing.md"
                ScoringTask.TRANSLATION -> "cet6-translation.md"
            }
        }

    /**
     * 组装完整提示词。
     *
     * @param common 公共部分（角色 + 评分总原则 + 降档红线 + 四六级差异）
     * @param module 对应题型的独立模块
     * @param outputFormat 输出格式与硬性约束
     * @param examType 四级 / 六级
     * @param taskType 作文 / 翻译
     * @param question 原题目 / 翻译原文（可空）
     * @param answer 我的作答 / 译文
     */
    fun assemble(
        common: String,
        module: String,
        outputFormat: String,
        examType: ExamType,
        taskType: ScoringTask,
        question: String?,
        answer: String,
    ): String = buildString {
        append(common.trimEnd())
        append("\n\n---\n\n")
        append(module.trimEnd())
        append("\n\n---\n\n")

        append("【考试级别】：").append(examType.shortLabel).append('\n')
        append("【题型】：").append(taskType.label).append('\n')
        append("【题目要求/翻译原文】：\n")
        append(normalizeQuestion(question)).append('\n')
        append('\n')
        append("【我的作答】：\n").append(answer.trim()).append('\n')

        append("\n---\n\n")
        append(outputFormat.trim())
    }

    /**
     * 处理原题目文本：空则给提示，过长则截断。
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

    /** 原题目为空时是否需要提示用户。UI 层用它决定要不要显示警示条。 */
    fun shouldWarnMissingQuestion(question: String?): Boolean =
        question?.trim().isNullOrEmpty()

    /** 原题目是否超长 */
    fun isQuestionTooLong(question: String?): Boolean =
        (question?.trim()?.length ?: 0) > MAX_QUESTION_LENGTH
}
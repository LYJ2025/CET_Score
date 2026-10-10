package com.cetscore.score.domain.model

/**
 * 提示词模板条目 —— 四级/六级 × 作文/翻译 共四份独立文件。
 *
 * 每个文件都是**完整可独立使用的提示词**，不含占位符，
 * 复制时绝不会混入其他模块内容。
 */
data class PromptTemplate(
    val examType: ExamType,
    val questionType: QuestionType,
    val assetPath: String,
)

/**
 * 四份提示词的映射表。
 *
 * 命名规则：`prompts/cet{4|6}_{writing|translation}.md`
 */
object PromptTemplates {

    private const val DIR = "prompts"

    val all: List<PromptTemplate> = listOf(
        PromptTemplate(ExamType.CET4, QuestionType.WRITING, "$DIR/cet4_writing.md"),
        PromptTemplate(ExamType.CET4, QuestionType.TRANSLATION, "$DIR/cet4_translation.md"),
        PromptTemplate(ExamType.CET6, QuestionType.WRITING, "$DIR/cet6_writing.md"),
        PromptTemplate(ExamType.CET6, QuestionType.TRANSLATION, "$DIR/cet6_translation.md"),
    )

    private val byKey: Map<Pair<ExamType, QuestionType>, PromptTemplate> =
        all.associateBy { it.examType to it.questionType }

    /** 按「级别 + 题型」取对应模板；找不到时抛异常而非静默回退 */
    fun get(examType: ExamType, questionType: QuestionType): PromptTemplate =
        byKey[examType to questionType]
            ?: error("未注册的提示词模板：$examType + $questionType")
}

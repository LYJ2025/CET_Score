package com.cetscore.core.data.model

import kotlinx.serialization.Serializable

/**
 * 一条 AI 批改。按题型分别保存，作文与翻译互不覆盖。
 *
 * @param text  AI 返回的完整批改文本（Markdown）
 * @param model 批改时使用的配置名 / 模型名，仅用于展示"这是哪个 AI 改的"
 * @param updatedAt 最后修改时间戳，用户手动编辑后也会更新
 */
@Serializable
data class AiFeedbackEntry(
    val text: String,
    val model: String? = null,
    val updatedAt: Long = 0L,
)

/**
 * 一条记录上挂的全部 AI 批改。
 *
 * key 用题型名（WRITING / TRANSLATION）而不是枚举下标，
 * 与 [com.cetscore.core.data.entity.ExamRecord.examType] 的存法保持一致。
 *
 * 之所以按题型分成多段而不是只留一个字符串：
 * 一次考试同时有作文和翻译，两项都想保存批改，
 * 单字段会在存第二份时把第一份覆盖掉。
 */
@Serializable
data class AiFeedbackSet(
    val entries: Map<String, AiFeedbackEntry> = emptyMap(),
) {
    fun get(task: String): AiFeedbackEntry? = entries[task]

    fun with(task: String, entry: AiFeedbackEntry): AiFeedbackSet =
        copy(entries = entries + (task to entry))

    fun without(task: String): AiFeedbackSet =
        copy(entries = entries - task)

    val isEmpty: Boolean get() = entries.isEmpty()

    /** 有内容才算有批改；空字符串视为没有 */
    val hasContent: Boolean get() = entries.values.any { it.text.isNotBlank() }
}

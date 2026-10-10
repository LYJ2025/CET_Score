package com.cetscore.score.domain.model

/**
 * 题型。core:data 里存字符串，算法层用枚举，两边通过
 * [QuestionType.name] / [fromName] 转换。
 */
enum class QuestionType(val label: String) {
    WRITING("作文"),
    TRANSLATION("翻译");

    companion object {
        fun fromName(value: String?): QuestionType =
            entries.firstOrNull { it.name == value } ?: WRITING
    }
}

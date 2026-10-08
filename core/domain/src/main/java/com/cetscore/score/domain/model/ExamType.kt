package com.cetscore.score.domain.model

/**
 * 考试类型。core:data 里已有一份，这里保持同构：
 * 数据层存字符串，算法层用枚举，两边通过 [ExamType.name] / [fromName] 转换。
 */
enum class ExamType(val label: String, val shortLabel: String) {
    CET4("四级估分", "四级"),
    CET6("六级估分", "六级");

    companion object {
        fun fromName(name: String?): ExamType =
            entries.firstOrNull { it.name == name } ?: CET4
    }
}

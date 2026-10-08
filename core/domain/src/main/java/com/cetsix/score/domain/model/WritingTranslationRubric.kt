package com.cetsix.score.domain.model

/**
 * 写作/翻译的档位评分标准。
 *
 * 每个档位对应一个分数区间，用户先点选档位、再在该区间内用滑块微调。
 */
data class RubricBand(
    val min: Int,
    val max: Int,
    val description: String,
) {
    val rangeText: String get() = "$min-$max 分"

    /** 区间中位数，作为选中该档位时的默认分值 */
    val defaultScore: Int get() = (min + max) / 2

    init {
        require(min <= max) { "档位区间非法：$min-$max" }
        require(min >= 0) { "档位下限不能为负" }
    }

    fun contains(score: Int): Boolean = score in min..max
}

data class WritingTranslationRubric(
    val title: String,
    val bands: List<RubricBand>,
) {
    /** 档位按分数从高到低排列，便于 UI 横向展示 */
    val sortedBands: List<RubricBand> get() = bands.sortedByDescending { it.min }

    fun bandOf(score: Int): RubricBand =
        sortedBands.firstOrNull { it.contains(score) } ?: sortedBands.last()

    init {
        require(bands.isNotEmpty()) { "档位不能为空" }
        require(bands.all { it.max <= MAX_SCORE }) { "档位不能超过 $MAX_SCORE" }
    }

    companion object {
        const val MAX_SCORE = 15

        fun writingCet4() = WritingTranslationRubric(
            title = "写作",
            bands = listOf(
                RubricBand(13, 15, "切题，表达思想清楚，文字通顺，连贯性好，基本无语言错误"),
                RubricBand(10, 12, "切题，表达思想清楚，文字连贯，但有少量语言错误"),
                RubricBand(7, 9, "基本切题，有些地方表达不够清楚，文字勉强连贯，语言错误较多"),
                RubricBand(4, 6, "基本切题，表达思想不清楚，连贯性差，有较多严重语言错误"),
                RubricBand(1, 3, "条理不清，思路紊乱，语言支离破碎"),
                RubricBand(0, 0, "未作答或只有几个孤立的词"),
            ),
        )

        fun writingCet6() = WritingTranslationRubric(
            title = "写作",
            bands = listOf(
                RubricBand(13, 15, "切题，表达思想清楚，文字通顺，连贯性好，基本无语言错误"),
                RubricBand(10, 12, "切题，表达较清楚，文字连贯，仅有少量语言错误"),
                RubricBand(7, 9, "基本切题，表达尚可，文字勉强连贯，语言错误较多"),
                RubricBand(4, 6, "基本切题但表达不清，连贯性较差，有较多严重语言错误"),
                RubricBand(1, 3, "条理不清，思路紊乱，语言支离破碎"),
                RubricBand(0, 0, "未作答或内容过少"),
            ),
        )

        fun translationCet4() = WritingTranslationRubric(
            title = "翻译",
            bands = listOf(
                RubricBand(13, 15, "译文准确表达原文意思，用词贴切，行文流畅，基本无语言错误"),
                RubricBand(10, 12, "译文基本表达原文意思，文字通顺、连贯，有少量语言错误"),
                RubricBand(7, 9, "勉强表达原文意思，用词欠准确，有较多语言错误"),
                RubricBand(4, 6, "只译出部分内容，有较多严重语言错误"),
                RubricBand(1, 3, "只译出个别词，有严重语言错误"),
                RubricBand(0, 0, "未作答"),
            ),
        )

        fun translationCet6() = WritingTranslationRubric(
            title = "翻译",
            bands = listOf(
                RubricBand(13, 15, "译文准确完整表达原文意思，用词贴切，行文流畅，基本无语言错误"),
                RubricBand(10, 12, "译文基本完整表达原文意思，文字通顺、连贯，有少量语言错误"),
                RubricBand(7, 9, "基本表达原文大意，用词欠准确，有较多语言错误"),
                RubricBand(4, 6, "只译出部分内容，表达不完整，有较多严重语言错误"),
                RubricBand(1, 3, "只译出个别词，表达残缺，有严重语言错误"),
                RubricBand(0, 0, "未作答"),
            ),
        )
    }
}

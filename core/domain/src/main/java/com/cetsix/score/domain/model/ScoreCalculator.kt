package com.cetsix.score.domain.model

/**
 * 估分计算器 —— 纯函数，无副作用，不联网，不调用任何 AI。
 *
 * 换算链路：
 *   1. 客观题答对原始分（四六级听力 35 分、阅读 35 分）
 *   2. 写作/翻译用档位自评分（各 0..15）
 *   3. 相加得百分制总分（0..100）
 *   4. × 7.1 得 710 分制总分，四舍五入取整
 *
 * 公式：
 *   总分 = (听力答对原始分 + 阅读答对原始分 + 写作自评 + 翻译自评) × 7.1
 */
object ScoreCalculator {

    /** 百分制 → 710 分制的放大系数 */
    private const val TOTAL_FACTOR = 7.1f

    /**
     * 计算估分结果。
     *
     * @param config 考试配置（四级或六级）
     * @param answers 题组 id → 答对题数。缺失的题组按 0 题处理
     * @param writingScore 写作自评 0..15
     * @param translationScore 翻译自评 0..15
     */
    fun calculate(
        config: ExamConfig,
        answers: Map<String, Int>,
        writingScore: Int,
        translationScore: Int,
    ): ScoreResult {
        require(writingScore in 0..ExamConfig.WRITING_WEIGHT.toInt()) {
            "写作分数必须在 0..15"
        }
        require(translationScore in 0..ExamConfig.TRANSLATION_WEIGHT.toInt()) {
            "翻译分数必须在 0..15"
        }

        // ---------- 1. 逐题组算分 ----------
        val listeningResults = config.listeningGroups.map { group ->
            groupResult(group, answers[group.id] ?: 0, ScoreSection.LISTENING)
        }
        val readingResults = config.readingGroups.map { group ->
            groupResult(group, answers[group.id] ?: 0, ScoreSection.READING)
        }

        // ---------- 2. 汇总百分制 ----------
        // 听力/阅读：原始总分恰好是 35，答对原始分即百分制得分
        val listeningPercent = listeningResults.sumOf { it.rawScore.toDouble() }.toFloat()
        val readingPercent = readingResults.sumOf { it.rawScore.toDouble() }.toFloat()
        val writingPercent = writingScore.toFloat()
        val translationPercent = translationScore.toFloat()

        val percentTotal = listeningPercent + readingPercent + writingPercent + translationPercent

        // ---------- 3. 换算到 710 分制 ----------
        val totalScore = roundToHalfUpTo710(percentTotal)

        // ---------- 4. 各部分占比（最大余数法，六项之和恒为 100.0） ----------
        //
        // 按需求文档定义，环形图展示 **6 个部分**：
        //   听力理解（两个题组合并为一项）
        //   阅读理解 · 选词填空 / 长篇阅读匹配 / 仔细阅读（三组各自一项）
        //   写作
        //   翻译
        //
        // 注意：听力虽然内部有 2 个题组（1-15、16-25），但占比层面合并成一个"听力理解"，
        // 这样才与需求中的 6 部分一一对应。
        val parts = buildList {
            // 听力：两个题组合并为一项"听力理解"
            add(Part(ScoreSection.LISTENING, listeningPercent))
            // 阅读：三组各自一项，用题组全称
            readingResults.forEach { add(Part(ScoreSection.READING, it.rawScore, it.fullName)) }
            add(Part(ScoreSection.WRITING, writingPercent))
            add(Part(ScoreSection.TRANSLATION, translationPercent))
        }
        val percents = largestRemainderPercentages(parts.map { it.rawScore })

        val proportions = parts.mapIndexed { i, part ->
            Proportion(
                section = part.section,
                fullName = part.name,
                rawScore = part.rawScore,
                totalScore = toTotal(part.rawScore),
                proportion = percents[i],
            )
        }

        return ScoreResult(
            examType = config.examType,
            totalScore = totalScore,
            percentTotal = percentTotal,
            listeningPercent = listeningPercent,
            readingPercent = readingPercent,
            writingPercent = writingPercent,
            translationPercent = translationPercent,
            listeningTotal = toTotal(listeningPercent),
            readingTotal = toTotal(readingPercent),
            writingTotal = toTotal(writingPercent),
            translationTotal = toTotal(translationPercent),
            groupResults = listeningResults + readingResults,
            proportions = proportions,
        )
    }

    /**
     * 占比中的一个部分。
     *
     * @param name 图例上显示的全称；不传则用 section 的标准名（如"听力理解"）
     */
    private data class Part(
        val section: ScoreSection,
        val rawScore: Float,
        val name: String = section.label,
    )

    /** 单个题组得分：答对题数 × 每题分，并做上下限保护 */
    private fun groupResult(
        group: QuestionGroup,
        correctCount: Int,
        section: ScoreSection,
    ): GroupResult {
        val safeCorrect = correctCount.coerceIn(0, group.questionCount)
        val rawScore = safeCorrect * group.perQuestionScore
        val accuracy = if (group.questionCount == 0) 0f
        else safeCorrect.toFloat() / group.questionCount
        return GroupResult(
            groupId = group.id,
            title = group.title,
            fullName = group.fullName,
            section = section,
            correctCount = safeCorrect,
            questionCount = group.questionCount,
            rawScore = rawScore,
            rawFullScore = group.totalRawScore,
            totalScore = toTotal(rawScore),
            accuracy = accuracy,
        )
    }

    /** 百分制 → 710 分制，保留一位小数用于展示 */
    private fun toTotal(percent: Float): Float =
        (Math.round(percent.toDouble() * TOTAL_FACTOR * 10.0) / 10.0).toFloat()

    /**
     * 总分：百分制 × 7.1，四舍五入取整，上限 710。
     *
     * 用 Math.round 而非 kotlin.math.roundToInt：
     * 后者在部分边界（如 248.4999999）因浮点表示可能出现意外结果，
     * Math.round 的行为在 [0, 710] 区间内更可预期。
     */
    private fun roundToHalfUpTo710(percentTotal: Float): Int {
        val rounded = Math.round((percentTotal * TOTAL_FACTOR).toDouble()).toInt()
        return rounded.coerceIn(0, ExamConfig.TOTAL_SCORE)
    }

    /**
     * 最大余数法求百分比 —— **各项之和精确等于 100.0**。
     *
     * 关键设计：分配精度必须与展示精度对齐。
     * UI 上占比只显示一位小数（即 0.1%），所以这里以 0.1% 为最小单位分配
     * （UNITS = 1000 个单位 = 100%），分配完直接除以 UNITS 即可，
     * **不需要再做一次四舍五入**。
     *
     * 为什么不先分到极细精度再四舍五入：
     * 六等分时每项是 16.666…%，各自四舍五入到 16.7% 后总和变成 100.2%，
     * 环形图按此比例画扇形会出现明显缺口。
     *
     * 算法：
     *  1. 每项先向下取整到 0.1% 单位
     *  2. 把剩余单位按「小数部分从大到小」依次补给各项（最大余数法）
     *  3. 结果必然精确合计 100.0%
     *
     * @param values 各部分的值，不需预先归一化
     * @return 百分比列表（0..100，一位小数）；总和为 0 时全返 0
     */
    fun largestRemainderPercentages(values: List<Float>): List<Float> {
        val total = values.sumOf { it.toDouble() }
        if (total <= 0.0 || values.isEmpty()) return values.map { 0f }

        // 1000 个单位 = 100%，即每个单位代表 0.1%
        val UNITS = 1000
        val ratios = values.map { it.toDouble() / total }

        val units = ratios.map { (it * UNITS).toInt() }.toMutableList()
        var remainder = UNITS - units.sum()

        // 余数补给小数部分最大的项
        val fractional = ratios.mapIndexed { i, r -> r * UNITS - units[i] }
        val order = fractional.indices.sortedByDescending { fractional[it] }
        var k = 0
        while (remainder > 0 && order.isNotEmpty()) {
            units[order[k % order.size]] += 1
            remainder--
            k++
        }

        // 单位 → 百分比，天然只有一位小数，无需再四舍五入
        return units.map { u -> (u.toDouble() * 100.0 / UNITS).toFloat() }
    }
}

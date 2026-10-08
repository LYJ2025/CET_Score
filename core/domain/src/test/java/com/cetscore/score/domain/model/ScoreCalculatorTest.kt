package com.cetscore.score.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 估分算法单元测试。
 *
 * 核心验证点：
 *  1. 满分 710、零分 0
 *  2. 听力满分单科 = 35 × 7.1 ≈ 248.5 → 总分 249
 *  3. 题组边界（0 / 满 / 中间）
 *  4. 占比之和恒为 100.0（最大余数法）
 */
class ScoreCalculatorTest {

    private val config4 = ExamConfig.CET4
    private val config6 = ExamConfig.CET6

    /** 构造一份"全对"或"全错"的答案表 */
    private fun answersOf(config: ExamConfig, all: Boolean): Map<String, Int> =
        config.allGroups.associate { it.id to if (all) it.questionCount else 0 }

    // ---------- 1. 极值 ----------

    @Test
    fun `全对且写作翻译满分应得710`() {
        val result = ScoreCalculator.calculate(
            config = config4,
            answers = answersOf(config4, all = true),
            writingScore = 15,
            translationScore = 15,
        )
        // 百分制 35 + 35 + 15 + 15 = 100 → × 7.1 = 710
        assertEquals(100f, result.percentTotal, 0.001f)
        assertEquals(710, result.totalScore)
        assertTrue(result.isPassed)
    }

    @Test
    fun `全错且写作翻译零分应得0`() {
        val result = ScoreCalculator.calculate(
            config = config4,
            answers = answersOf(config4, all = false),
            writingScore = 0,
            translationScore = 0,
        )
        assertEquals(0f, result.percentTotal, 0.001f)
        assertEquals(0, result.totalScore)
        assertTrue(!result.isPassed)
    }

    @Test
    fun `六级全对同样得710`() {
        val result = ScoreCalculator.calculate(
            config = config6,
            answers = answersOf(config6, all = true),
            writingScore = 15,
            translationScore = 15,
        )
        assertEquals(710, result.totalScore)
    }

    // ---------- 2. 单科满分 ----------

    @Test
    fun `听力全对其他全错应得249`() {
        // 听力 35 百分制 → 35 × 7.1 = 248.5 → 四舍五入 249
        val result = ScoreCalculator.calculate(
            config = config4,
            answers = answersOf(config4, all = false).toMutableMap().apply {
                config4.listeningGroups.forEach { this[it.id] = it.questionCount }
            },
            writingScore = 0,
            translationScore = 0,
        )
        assertEquals(35f, result.listeningPercent, 0.001f)
        assertEquals(249, result.totalScore)
        assertEquals(248.5f, result.listeningTotal, 0.01f)
    }

    @Test
    fun `阅读全对其他全错应得249`() {
        val result = ScoreCalculator.calculate(
            config = config4,
            answers = answersOf(config4, all = false).toMutableMap().apply {
                config4.readingGroups.forEach { this[it.id] = it.questionCount }
            },
            writingScore = 0,
            translationScore = 0,
        )
        assertEquals(35f, result.readingPercent, 0.001f)
        assertEquals(249, result.totalScore)
    }

    @Test
    fun `仅写作翻译满分应得213`() {
        // (0 + 0 + 15 + 15) × 7.1 = 213
        val result = ScoreCalculator.calculate(
            config = config4,
            answers = answersOf(config4, all = false),
            writingScore = 15,
            translationScore = 15,
        )
        assertEquals(213, result.totalScore)
    }

    // ---------- 3. 题组边界 ----------

    @Test
    fun `题组边界0题满题与中间值`() {
        val result = ScoreCalculator.calculate(
            config = config4,
            answers = mapOf(
                // 听力 1-15：0 题 → 0 分
                "listening_1_15" to 0,
                // 听力 16-25：满题 10 题 × 2 分 = 20 分
                "listening_16_25" to 10,
                // 阅读 26-35：中间 5 题 × 0.5 = 2.5 分
                "reading_26_35" to 5,
                // 阅读 36-45：满题 10 题 × 1 = 10 分
                "reading_36_45" to 10,
                // 阅读 46-55：0 题
                "reading_46_55" to 0,
            ),
            writingScore = 8,
            translationScore = 11,
        )

        // 百分制 = 0 + 20 + 2.5 + 10 + 0 + 8 + 11 = 51.5
        assertEquals(51.5f, result.percentTotal, 0.001f)
        // 51.5 × 7.1 = 365.65 → 366
        assertEquals(366, result.totalScore)

        // 逐题组核对
        val g1 = result.groupResults.first { it.groupId == "listening_1_15" }
        assertEquals(0f, g1.rawScore, 0.001f)
        assertEquals(0f, g1.accuracy, 0.001f)

        val g2 = result.groupResults.first { it.groupId == "listening_16_25" }
        assertEquals(20f, g2.rawScore, 0.001f)
        assertEquals(1f, g2.accuracy, 0.001f)

        val g3 = result.groupResults.first { it.groupId == "reading_26_35" }
        assertEquals(2.5f, g3.rawScore, 0.001f)      // 5 × 0.5
        assertEquals(0.5f, g3.accuracy, 0.001f)     // 5/10

        val g4 = result.groupResults.first { it.groupId == "reading_46_55" }
        assertEquals(0f, g4.rawScore, 0.001f)
    }

    @Test
    fun `答对题数超上限应被截断`() {
        val result = ScoreCalculator.calculate(
            config = config4,
            answers = mapOf("listening_1_15" to 999),
            writingScore = 0,
            translationScore = 0,
        )
        val g = result.groupResults.first { it.groupId == "listening_1_15" }
        assertEquals(15, g.correctCount)          // 被截到题组总数
        assertEquals(15f, g.rawScore, 0.001f)
    }

    @Test
    fun `答对题数为负应被截断到0`() {
        val result = ScoreCalculator.calculate(
            config = config4,
            answers = mapOf("reading_46_55" to -5),
            writingScore = 0,
            translationScore = 0,
        )
        val g = result.groupResults.first { it.groupId == "reading_46_55" }
        assertEquals(0, g.correctCount)
        assertEquals(0f, g.rawScore, 0.001f)
    }

    // ---------- 4. 四六级题型差异 ----------

    @Test
    fun `四六级听力题组分值一致但名称不同`() {
        val r4 = ScoreCalculator.calculate(
            config4, answersOf(config4, true), 15, 15
        )
        val r6 = ScoreCalculator.calculate(
            config6, answersOf(config6, true), 15, 15
        )
        assertEquals(r4.totalScore, r6.totalScore)

        // 名称应不同
        assertEquals("听力理解 · 短篇新闻与长对话", r4.groupResults[0].fullName)
        assertEquals("听力理解 · 长对话与听力篇章", r6.groupResults[0].fullName)

        // 阅读部分四六级共用同一批题组
        assertEquals(
            config4.readingGroups.map { it.id },
            config6.readingGroups.map { it.id },
        )
    }

    // ---------- 5. 占比 ----------

    @Test
    fun `各部分占比之和为100且为6项`() {
        val result = ScoreCalculator.calculate(
            config4,
            answers = mapOf(
                "listening_1_15" to 12,
                "listening_16_25" to 8,
                "reading_26_35" to 6,
                "reading_36_45" to 7,
                "reading_46_55" to 15,
            ),
            writingScore = 11,
            translationScore = 9,
        )
        // 按需求：听力合并为 1 项 + 阅读 3 项 + 写作 + 翻译 = 6 项
        assertEquals(6, result.proportions.size)

        // 分配精度与展示精度对齐（0.1%），因此应精确等于 100.0
        val sum = result.proportions.sumOf { it.proportion.toDouble() }
        assertEquals(100.0, sum, 1e-4)

        // 图例全称应与需求文档一致
        assertEquals(
            listOf(
                "听力理解",
                "阅读理解 · 选词填空",
                "阅读理解 · 长篇阅读匹配",
                "阅读理解 · 仔细阅读",
                "写作",
                "翻译",
            ),
            result.proportions.map { it.fullName },
        )

        // 听力占比 = (12×1 + 8×2) / 51.5
        val listeningP = result.proportions.first { it.section == ScoreSection.LISTENING }
        assertEquals(28f, listeningP.rawScore, 0.001f)
    }

    @Test
    fun `零分时占比全为0`() {
        val result = ScoreCalculator.calculate(config4, emptyMap(), 0, 0)
        assertTrue(result.proportions.all { it.proportion == 0f })
        assertEquals(0f, result.proportions.sumOf { it.proportion.toDouble() }.toFloat(), 0.001f)
    }

    @Test
    fun `最大余数法在各种输入下都归一到100`() {
        val cases = listOf(
            listOf(1f, 1f, 1f, 1f, 1f, 1f),
            listOf(35f, 5f, 10f, 20f, 15f, 15f),
            listOf(0f, 0f, 0f, 0f, 0f, 15f),
            listOf(0.5f, 0.5f, 0.5f, 0.5f, 0.5f, 0.5f),
            listOf(3f, 3f, 3f, 3f, 3f, 2f),
            listOf(35f, 35f, 0f, 0f, 0f, 0f),
            listOf(7f),
            listOf(1f, 1f, 1f),
        )
        cases.forEach { case ->
            val result = ScoreCalculator.largestRemainderPercentages(case)
            assertEquals(case.size, result.size)
            // 算法在「0.1% 单位」上做整数分配，逻辑上精确等于 100.0。
            // 断言里用 1e-4 容差吸收 Float→Double 的表示误差（约 4e-6），
            // 这样既能抓住真实的分配错误，又不会因浮点表示误报。
            val sum = result.sumOf { it.toDouble() }
            assertEquals("case=$case 实际=$sum", 100.0, sum, 1e-4)
            // 每项都应在 0..100 之间，且只有一位小数
            result.forEach {
                assertTrue("case=$case 越界: $it", it >= 0f && it <= 100f)
                assertEquals("case=$case 小数位数超过 1: $it", it, roundTo1(it), 1e-3f)
            }
        }
    }

    /** 保留一位小数 */
    private fun roundTo1(v: Float): Float = (Math.round(v * 10.0) / 10.0).toFloat()

    @Test
    fun `最大余数法零输入返回全0`() {
        val result = ScoreCalculator.largestRemainderPercentages(listOf(0f, 0f, 0f))
        assertTrue(result.all { it == 0f })
    }

    // ---------- 6. 及格线 ----------

    @Test
    fun `425分及格判定`() {
        // 需要百分制 ≥ 425/7.1 ≈ 59.86 → 60
        // 构造 60 百分制：听力 35 + 阅读 25，写作翻译 0
        val answers = config4.allGroups.associate { it.id to 0 }.toMutableMap()
        config4.listeningGroups.forEach { answers[it.id] = it.questionCount }
        // 阅读：选词填空 0.5×10=5，长篇匹配 1×10=10，仔细阅读 2×5=10 → 25
        answers["reading_26_35"] = 10
        answers["reading_36_45"] = 10
        answers["reading_46_55"] = 5

        val result = ScoreCalculator.calculate(config4, answers, 0, 0)
        // 百分制 = 35 + 25 = 60 → × 7.1 = 426
        assertEquals(426, result.totalScore)
        assertTrue(result.isPassed)
        assertEquals(0, result.gapToPass)
    }
}

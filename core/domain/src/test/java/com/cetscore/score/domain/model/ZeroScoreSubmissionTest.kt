package com.cetscore.score.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 估分输入完整性判断测试 —— 重点是**全 0 分必须允许提交**。
 *
 * 真实场景中"全错"和"没作答"都是有意义的成绩，
 * 不能因为分数是 0 就判定用户没填。
 */
class ZeroScoreSubmissionTest {

    private val config = ExamConfig.of(ExamType.CET4)

    /** 构造一组"所有题组都已作答、且全部为 0"的答案 */
    private fun allZeroAnswers(): Map<String, Int> =
        config.allGroups.associate { it.id to 0 }

    // ---------- 算法层：全 0 应得 0 分 ----------

    @Test
    fun `所有题组答对0题且写作翻译0分应得总分0`() {
        val result = ScoreCalculator.calculate(
            config = config,
            answers = allZeroAnswers(),
            writingScore = 0,
            translationScore = 0,
        )

        assertEquals("总分应为 0", 0, result.totalScore)
        assertEquals("听力百分制应为 0", 0f, result.listeningPercent, 0.001f)
        assertEquals("阅读百分制应为 0", 0f, result.readingPercent, 0.001f)
        assertEquals("写作应为 0", 0f, result.writingPercent, 0.001f)
        assertEquals("翻译应为 0", 0f, result.translationPercent, 0.001f)
    }

    @Test
    fun `全0时各部分占比均为0且合计为0`() {
        val result = ScoreCalculator.calculate(
            config = config,
            answers = allZeroAnswers(),
            writingScore = 0,
            translationScore = 0,
        )

        // 占比全部为 0
        result.proportions.forEach { p ->
            assertEquals("${p.fullName} 占比应为 0", 0f, p.proportion, 0.001f)
        }

        // 全 0 时合计也是 0 —— 此时没有"占比最大项"可分配剩余单位，
        // 这是合理行为：六项都是 0，占比自然全是 0，环形图会整体为空。
        val sum = result.proportions.sumOf { it.proportion.toDouble() }
        assertEquals("全 0 时占比合计应为 0", 0.0, sum, 1e-4)
    }

    @Test
    fun `全0时不被判定为及格`() {
        val result = ScoreCalculator.calculate(
            config = config,
            answers = allZeroAnswers(),
            writingScore = 0,
            translationScore = 0,
        )
        assertFalse("0 分不应算及格", result.isPassed)
        assertEquals("距及格线应差 425", 425, result.gapToPass)
    }

    @Test
    fun `全0时各题组得分均为0`() {
        val result = ScoreCalculator.calculate(
            config = config,
            answers = allZeroAnswers(),
            writingScore = 0,
            translationScore = 0,
        )

        result.groupResults.forEach { group ->
            assertEquals("${group.title} 得分应为 0", 0f, group.rawScore, 0.001f)
            assertEquals("${group.title} 正确率应为 0", 0f, group.accuracy, 0.001f)
        }
    }

    // ---------- 输入完整性：全 0 也算"已填" ----------

    @Test
    fun `全0答案被视为已作答所有题组`() {
        // 模拟 UI 状态：所有题组都已在 map 中（值为 0）
        val answers = allZeroAnswers()
        assertTrue(
            "全 0 应被视为已完成所有题组",
            config.allGroups.all { answers.containsKey(it.id) },
        )
    }

    @Test
    fun `缺任何题组都算未完成`() {
        val answers = allZeroAnswers().toMutableMap()
        answers.remove(config.allGroups.first().id)

        assertFalse(
            "缺题组时不应算完成",
            config.allGroups.all { answers.containsKey(it.id) },
        )
    }

    @Test
    fun `0分档是合法选择`() {
        // 写作/翻译的默认值就是 0，本身就是合法的分数
        assertEquals(0, 0.coerceIn(0, 15))
        // 步进器对 0 的处理：coerceIn(0, x) 应保持 0
        assertEquals(0, (-5).coerceIn(0, 15))
        assertEquals(15, (99).coerceIn(0, 15))
    }

    @Test
    fun `部分题目为0其余非0也能正常计算`() {
        val answers = allZeroAnswers().toMutableMap().apply {
            config.listeningGroups.first().let { put(it.id, 3) }
        }
        val result = ScoreCalculator.calculate(
            config = config,
            answers = answers,
            writingScore = 0,
            translationScore = 0,
        )

        // 听力 3 题 × 1 分 = 3 分原始分 → ×7.1 = 21
        assertTrue("总分应大于 0", result.totalScore > 0)
        assertEquals(21, result.totalScore)
    }
}
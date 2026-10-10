package com.cetscore.score.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 替换式导航状态机测试。
 *
 * 核心验证「**替换而非叠加**」：
 *  - 进一层 → 上一层按钮消失
 *  - 退一层 → 下一层按钮消失
 */
class AssistantNavigationTest {

    // ---------- 初始态 ----------

    @Test
    fun `初始应为第一屏级别选择`() {
        val nav = AssistantNavigation()
        assertTrue(nav.currentStep is AssistantStep.LevelSelect)
        assertTrue(nav.isFirstScreen)
        assertNull(nav.examType)
        assertNull(nav.questionType)
    }

    // ---------- 第一屏 → 第二屏 ----------

    @Test
    fun `点四级应进入第二屏题型选择`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET4)

        assertTrue(nav.currentStep is AssistantStep.TaskSelect)
        assertFalse("进入第二屏后不再是第一屏", nav.isFirstScreen)
        assertEquals(ExamType.CET4, nav.examType)
    }

    @Test
    fun `第二屏不应残留题型_因为还没选`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET6)

        assertEquals(ExamType.CET6, nav.examType)
        // 题型未选，说明当前屏只显示"作文/翻译"两个按钮
        assertNull(nav.questionType)
    }

    // ---------- 第二屏 → 第三屏 ----------

    @Test
    fun `点作文应进入第三屏评分界面`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET4)
        nav.selectTask(QuestionType.WRITING)

        val step = nav.currentStep
        assertTrue(step is AssistantStep.Scoring)
        step as AssistantStep.Scoring
        assertEquals(ExamType.CET4, step.examType)
        assertEquals(QuestionType.WRITING, step.questionType)
    }

    @Test
    fun `完整路径四级作文应得到正确上下文`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET4)
        nav.selectTask(QuestionType.WRITING)

        val step = nav.currentStep as AssistantStep.Scoring
        // 第一屏点四级 → 第二屏点作文 → 第三屏是"四级-作文"
        assertEquals("四级", step.examType.shortLabel)
        assertEquals("作文", step.questionType.label)
    }

    @Test
    fun `完整路径六级翻译应得到正确上下文`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET6)
        nav.selectTask(QuestionType.TRANSLATION)

        val step = nav.currentStep as AssistantStep.Scoring
        assertEquals("六级", step.examType.shortLabel)
        assertEquals("翻译", step.questionType.label)
    }

    // ---------- 返回：第三屏 → 第二屏（级别保留）----------

    @Test
    fun `第三屏返回应回到第二屏且级别保留`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET6)
        nav.selectTask(QuestionType.WRITING)

        val consumed = nav.back()

        assertTrue("返回应被消费", consumed)
        assertTrue("应回到第二屏题型选择", nav.currentStep is AssistantStep.TaskSelect)
        assertEquals("级别信息必须保留", ExamType.CET6, nav.examType)
    }

    @Test
    fun `第三屏返回后可换题型`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET4)
        nav.selectTask(QuestionType.WRITING)
        nav.back()

        // 在第二屏改选翻译
        nav.selectTask(QuestionType.TRANSLATION)

        val step = nav.currentStep as AssistantStep.Scoring
        assertEquals("级别仍是四级", ExamType.CET4, step.examType)
        assertEquals("题型已改为翻译", QuestionType.TRANSLATION, step.questionType)
    }

    // ---------- 返回：第二屏 → 第一屏（级别清空）----------

    @Test
    fun `第二屏返回应回到第一屏且清空级别`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET4)

        val consumed = nav.back()

        assertTrue(consumed)
        assertTrue("应回到第一屏", nav.currentStep is AssistantStep.LevelSelect)
        assertNull("按需求第二屏返回要清空级别", nav.examType)
        assertNull(nav.questionType)
    }

    @Test
    fun `第一屏返回不消费_交给宿主处理`() {
        val nav = AssistantNavigation()
        assertFalse("已在第一屏时返回应返回 false", nav.back())
    }

    @Test
    fun `完整往返后状态应回到初始`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET4)
        nav.selectTask(QuestionType.WRITING)
        nav.back()   // → 第二屏
        nav.back()   // → 第一屏

        assertTrue(nav.isFirstScreen)
        assertNull(nav.examType)
        assertNull(nav.questionType)
    }

    // ---------- 切换级别应作废已选题型 ----------

    @Test
    fun `在第二屏换级别应清空题型`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET4)
        nav.selectTask(QuestionType.WRITING)
        nav.back()   // 回第二屏

        // 改选级别
        nav.selectLevel(ExamType.CET6)

        assertEquals(ExamType.CET6, nav.examType)
        assertNull("换级别后题型应作废", nav.questionType)
    }

    // ---------- 从首页直接跳第三屏 ----------

    @Test
    fun `jumpToScoring应直接进入第三屏`() {
        val nav = AssistantNavigation()
        nav.jumpToScoring(ExamType.CET6, QuestionType.TRANSLATION)

        val step = nav.currentStep as AssistantStep.Scoring
        assertEquals(ExamType.CET6, step.examType)
        assertEquals(QuestionType.TRANSLATION, step.questionType)
        assertEquals(ExamType.CET6, nav.examType)
    }

    @Test
    fun `jumpToScoring后返回仍回第二屏并保留级别`() {
        val nav = AssistantNavigation()
        nav.jumpToScoring(ExamType.CET4, QuestionType.WRITING)
        nav.back()

        assertTrue(nav.currentStep is AssistantStep.TaskSelect)
        assertEquals(ExamType.CET4, nav.examType)
    }

    // ---------- 首页入口：必须停在第一屏 ----------
    //
    // 回归防护：曾经首页点「AI 评分助手」被直接送到第三屏（跳过级别与题型选择），
    // 根因是导航层无条件调用了 jumpToScoring。
    // 首页入口的正确行为是**不做任何跳转**，保持第一屏。

    @Test
    fun `首页入口不跳转时停在第一屏`() {
        val nav = AssistantNavigation()
        // 首页入口：什么都不做
        assertTrue(
            "首页入口应停在第一屏，等用户选择级别",
            nav.currentStep is AssistantStep.LevelSelect,
        )
        assertTrue(nav.isFirstScreen)
        assertNull(nav.examType)
    }

    @Test
    fun `首页入口走完整流程仍是三级替换`() {
        val nav = AssistantNavigation()

        // 第一屏
        assertTrue(nav.currentStep is AssistantStep.LevelSelect)

        // 点六级 → 第二屏，且第一屏按钮不再出现
        nav.selectLevel(ExamType.CET6)
        assertTrue(nav.currentStep is AssistantStep.TaskSelect)

        // 点翻译 → 第三屏
        nav.selectTask(QuestionType.TRANSLATION)
        val step = nav.currentStep as AssistantStep.Scoring
        assertEquals(ExamType.CET6, step.examType)
        assertEquals(QuestionType.TRANSLATION, step.questionType)
    }

    @Test
    fun `从第三屏退回第一屏后再重新进入仍从第一屏开始`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET4)
        nav.selectTask(QuestionType.WRITING)
        nav.back()   // → 第二屏
        nav.back()   // → 第一屏，级别清空

        // 模拟退出页面后重新从首页进入（新实例）
        val fresh = AssistantNavigation()
        assertTrue("重新进入应从第一屏开始", fresh.currentStep is AssistantStep.LevelSelect)
    }

    @Test
    fun `未选级别时selectTask应补设默认级别`() {
        val nav = AssistantNavigation()
        nav.selectTask(QuestionType.WRITING)

        val step = nav.currentStep as AssistantStep.Scoring
        assertEquals("应补设默认级别四级", ExamType.CET4, step.examType)
    }

    // ---------- reset ----------

    @Test
    fun `reset应回到初始态`() {
        val nav = AssistantNavigation()
        nav.selectLevel(ExamType.CET6)
        nav.selectTask(QuestionType.TRANSLATION)

        nav.reset()

        assertTrue(nav.isFirstScreen)
        assertNull(nav.examType)
        assertNull(nav.questionType)
    }

    // ---------- 替换语义：同一时刻只有一个屏的状态 ----------

    @Test
    fun `任意时刻currentStep只对应一屏`() {
        val nav = AssistantNavigation()
        val steps = listOf<() -> Unit>(
            { nav.selectLevel(ExamType.CET4) },
            { nav.selectTask(QuestionType.WRITING) },
            { nav.back() },
            { nav.selectTask(QuestionType.TRANSLATION) },
            { nav.back() },
            { nav.back() },
        )

        steps.forEach {
            it()
            // 每次都必须是三种状态之一，不存在"叠加态"
            val step = nav.currentStep
            val ok = step is AssistantStep.LevelSelect ||
                step is AssistantStep.TaskSelect ||
                step is AssistantStep.Scoring
            assertTrue("出现了非法状态: $step", ok)
        }
    }
}
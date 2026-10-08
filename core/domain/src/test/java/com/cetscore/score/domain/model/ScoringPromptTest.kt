package com.cetscore.score.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提示词模板处理测试。
 *
 * 覆盖：占位符替换、复制文本拼接规则、残留占位符检测。
 */
class ScoringPromptTest {

    /** 用一个精简模板做测试，避免依赖完整 asset */
    private val template = """
        你是英语教师。
        【考试级别】：{考试级别}
        【题型】：{题型}
        【我的作答】：
        （请在此粘贴你的作文或翻译译文）
    """.trimIndent()

    // ---------- 占位符替换 ----------

    @Test
    fun `四级作文应正确替换两个占位符`() {
        val result = ScoringPrompt.buildPrompt(template, ExamType.CET4, ScoringTask.ESSAY)
        assertTrue(result.contains("【考试级别】：四级"))
        assertTrue(result.contains("【题型】：作文"))
        assertFalse(ScoringPrompt.hasUnreplacedPlaceholder(result))
    }

    @Test
    fun `六级翻译应正确替换两个占位符`() {
        val result = ScoringPrompt.buildPrompt(template, ExamType.CET6, ScoringTask.TRANSLATION)
        assertTrue(result.contains("【考试级别】：六级"))
        assertTrue(result.contains("【题型】：翻译"))
        assertFalse(ScoringPrompt.hasUnreplacedPlaceholder(result))
    }

    @Test
    fun `四级六种组合都不应残留占位符`() {
        ExamType.entries.forEach { exam ->
            ScoringTask.entries.forEach { task ->
                val result = ScoringPrompt.buildPrompt(template, exam, task)
                assertFalse(
                    "$exam/$task 仍有占位符残留",
                    ScoringPrompt.hasUnreplacedPlaceholder(result),
                )
            }
        }
    }

    @Test
    fun `替换不应破坏模板其余内容`() {
        val result = ScoringPrompt.buildPrompt(template, ExamType.CET4, ScoringTask.ESSAY)
        assertTrue(result.contains("你是英语教师"))
        assertTrue(result.contains("请在此粘贴你的作文或翻译译文"))
    }

    // ---------- 复制文本拼接 ----------

    @Test
    fun `未填作答时只返回提示词`() {
        val prompt = ScoringPrompt.buildPrompt(template, ExamType.CET4, ScoringTask.ESSAY)
        val result = ScoringPrompt.buildCopyText(prompt, null)
        // 原样返回，不追加任何内容
        assertEquals(prompt, result)
    }

    @Test
    fun `作答为空白字符串时等同于未填`() {
        val prompt = ScoringPrompt.buildPrompt(template, ExamType.CET4, ScoringTask.ESSAY)
        assertEquals(prompt, ScoringPrompt.buildCopyText(prompt, ""))
        assertEquals(prompt, ScoringPrompt.buildCopyText(prompt, "   \n  "))
    }

    @Test
    fun `填写作答时应追加引导语和内容`() {
        val prompt = ScoringPrompt.buildPrompt(template, ExamType.CET4, ScoringTask.ESSAY)
        val answer = "My essay content."
        val result = ScoringPrompt.buildCopyText(prompt, answer)

        assertTrue(result.startsWith(prompt))
        assertTrue(result.contains(ScoringPrompt.ANSWER_GUIDE))
        assertTrue(result.contains(answer))
        // 结构：提示词 + 空行 + 引导语 + 换行 + 内容
        assertTrue(result.contains("\n\n${ScoringPrompt.ANSWER_GUIDE}\n$answer"))
    }

    @Test
    fun `作答前后空白应被去除`() {
        val prompt = ScoringPrompt.buildPrompt(template, ExamType.CET4, ScoringTask.ESSAY)
        val result = ScoringPrompt.buildCopyText(prompt, "\n\n  My essay.  \n\n")
        // 末尾不应有多余空白
        assertTrue(result.endsWith("My essay."))
    }

    @Test
    fun `未填作答时模板末尾应保留引导语`() {
        val prompt = ScoringPrompt.buildPrompt(template, ExamType.CET4, ScoringTask.ESSAY)
        val result = ScoringPrompt.buildCopyText(prompt, null)
        // 模板自带引导语，用户可直接在 AI 里粘贴作答
        assertTrue(result.contains(ScoringPrompt.ANSWER_GUIDE))
    }

    @Test
    fun `填写作答时只追加一段作答内容`() {
        val prompt = ScoringPrompt.buildPrompt(template, ExamType.CET4, ScoringTask.ESSAY)
        val result = ScoringPrompt.buildCopyText(prompt, "My essay.")

        // 拼接只发生一次：末尾恰好是一段「引导语 + 换行 + 作答」
        assertTrue(
            "结尾应为「引导语\\nMy essay.」，实际结尾为：${result.takeLast(30)}",
            result.endsWith("${ScoringPrompt.ANSWER_GUIDE}\nMy essay."),
        )
        // 且只追加了一个换行分隔的空行
        assertFalse("不应出现连续三个换行", result.contains("\n\n\n"))
    }

    // ---------- 残留检测 ----------

    @Test
    fun `未替换的模板应被检测出残留占位符`() {
        assertTrue(ScoringPrompt.hasUnreplacedPlaceholder(template))
    }

    @Test
    fun `正常文本不应被误报`() {
        assertFalse(ScoringPrompt.hasUnreplacedPlaceholder("普通文本，无占位符"))
    }

    @Test
    fun `空模板处理不应崩溃`() {
        // 模板替换：空串替换后仍是空串
        assertEquals("", ScoringPrompt.buildPrompt("", ExamType.CET4, ScoringTask.ESSAY))

        // 空模板 + 未填作答 → 空串
        assertEquals("", ScoringPrompt.buildCopyText("", null))

        // 空模板 + 填了作答 → 仍产出「引导语 + 作答」，
        // 这样即使模板异常，用户填的作答也不会丢失。
        // 注意前面会带上 "\n\n" 分隔符（由空模板与引导语拼接而来）。
        assertEquals(
            "\n\n${ScoringPrompt.ANSWER_GUIDE}\nanswer",
            ScoringPrompt.buildCopyText("", "answer"),
        )
    }
}

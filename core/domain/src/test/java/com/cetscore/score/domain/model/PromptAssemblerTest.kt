package com.cetscore.score.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提示词组装测试。
 *
 * 核心：**复制内容只能来自当前模块的模板**，绝不混入其他模块文字。
 */
class PromptAssemblerTest {

    private val cet4Writing = "【模板】四级作文评分标准 120 words to 180 words"
    private val cet6Translation = "【模板】六级翻译评分标准 中译英"

    // ---------- 基本结构 ----------

    @Test
    fun `组装结果包含模板与题目与作答`() {
        val result = PromptAssembler.assemble(
            templateBody = cet4Writing,
            question = "写一篇关于校园活动的短文",
            answer = "My essay content.",
        )

        assertTrue("应包含模板", result.contains(cet4Writing))
        assertTrue("应包含原题目", result.contains("写一篇关于校园活动的短文"))
        assertTrue("应包含作答", result.contains("My essay content."))
        assertTrue("应有题目引导语", result.contains(PromptAssembler.QUESTION_GUIDE))
        assertTrue("应有作答引导语", result.contains(PromptAssembler.ANSWER_GUIDE))
    }

    @Test
    fun `顺序为模板_题目_作答`() {
        val result = PromptAssembler.assemble(
            templateBody = cet4Writing,
            question = "Q_MARK",
            answer = "A_MARK",
        )

        val iTemplate = result.indexOf(cet4Writing)
        val iQuestion = result.indexOf("Q_MARK")
        val iAnswer = result.indexOf("A_MARK")

        assertTrue("模板应最先", iTemplate < iQuestion)
        assertTrue("题目应在作答之前", iQuestion < iAnswer)
    }

    // ---------- 绝不混入其他模块 ----------

    @Test
    fun `组装四级作文时不应出现翻译模块内容`() {
        val result = PromptAssembler.assemble(
            templateBody = cet4Writing,
            question = "题目",
            answer = "作文",
        )
        assertFalse(
            "四级作文的复制内容不应混入六级翻译模板",
            result.contains(cet6Translation),
        )
    }

    @Test
    fun `不同模板组装结果互不干扰`() {
        val a = PromptAssembler.assemble(cet4Writing, "q", "a")
        val b = PromptAssembler.assemble(cet6Translation, "q", "a")

        assertTrue(a.contains(cet4Writing))
        assertFalse(a.contains(cet6Translation))
        assertTrue(b.contains(cet6Translation))
        assertFalse(b.contains(cet4Writing))
    }

    // ---------- 空作答（零分场景）----------

    @Test
    fun `作答为空时仍能组装出完整内容`() {
        val result = PromptAssembler.assemble(
            templateBody = cet4Writing,
            question = "题目",
            answer = "",
        )
        assertTrue("即使作答为空也应可组装", result.contains(cet4Writing))
        assertTrue("应保留作答引导语", result.contains(PromptAssembler.ANSWER_GUIDE))
    }

    @Test
    fun `作答前后空白被去除`() {
        val result = PromptAssembler.assemble(
            templateBody = cet4Writing,
            question = "题目",
            answer = "\n\n  My essay.  \n",
        )
        assertTrue(result.contains("My essay."))
    }

    // ---------- 原题目处理 ----------

    @Test
    fun `原题目为空时插入提示语`() {
        val result = PromptAssembler.assemble(cet4Writing, question = null, answer = "a")
        assertTrue(result.contains(PromptAssembler.NO_QUESTION_HINT))
    }

    @Test
    fun `原题目为空白等同于未填`() {
        assertTrue(PromptAssembler.shouldWarnMissingQuestion(""))
        assertTrue(PromptAssembler.shouldWarnMissingQuestion("  \n "))
        assertTrue(PromptAssembler.shouldWarnMissingQuestion(null))
        assertFalse(PromptAssembler.shouldWarnMissingQuestion("题目"))
    }

    @Test
    fun `原题目超长时截断并提示`() {
        val long = "题".repeat(PromptAssembler.MAX_QUESTION_LENGTH + 500)
        val result = PromptAssembler.assemble(cet4Writing, long, "a")
        assertTrue(result.contains(PromptAssembler.QUESTION_TOO_LONG_HINT))
        assertFalse(result.contains(long))
    }

    @Test
    fun `原题目长度刚超阈值即提示`() {
        assertFalse(PromptAssembler.isQuestionTooLong("a".repeat(10)))
        assertTrue(
            PromptAssembler.isQuestionTooLong(
                "a".repeat(PromptAssembler.MAX_QUESTION_LENGTH + 1)
            )
        )
    }

    @Test
    fun `normalizeQuestion空值返回提示语`() {
        assertEquals(
            PromptAssembler.NO_QUESTION_HINT,
            PromptAssembler.normalizeQuestion(null),
        )
    }

    // ---------- 边界 ----------

    @Test
    fun `模板为空时不崩溃`() {
        val result = PromptAssembler.assemble("", "题目", "作答")
        assertTrue(result.contains("作答"))
    }
}
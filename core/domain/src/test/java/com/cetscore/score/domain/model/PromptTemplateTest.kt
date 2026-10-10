package com.cetscore.score.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 四份提示词模板的映射与词数规则测试。
 */
class PromptTemplateTest {

    // ---------- 模板映射 ----------

    @Test
    fun `四种组合各自映射到独立文件`() {
        assertEquals(
            "prompts/cet4_writing.md",
            PromptTemplates.get(ExamType.CET4, QuestionType.WRITING).assetPath,
        )
        assertEquals(
            "prompts/cet4_translation.md",
            PromptTemplates.get(ExamType.CET4, QuestionType.TRANSLATION).assetPath,
        )
        assertEquals(
            "prompts/cet6_writing.md",
            PromptTemplates.get(ExamType.CET6, QuestionType.WRITING).assetPath,
        )
        assertEquals(
            "prompts/cet6_translation.md",
            PromptTemplates.get(ExamType.CET6, QuestionType.TRANSLATION).assetPath,
        )
    }

    @Test
    fun `四份模板路径互不相同`() {
        val paths = PromptTemplates.all.map { it.assetPath }
        assertEquals("四份模板必须是四个独立文件", 4, paths.toSet().size)
    }

    @Test
    fun `模板总数为四`() {
        assertEquals(4, PromptTemplates.all.size)
    }

    @Test
    fun `按组合查询能覆盖全部四种情况`() {
        ExamType.entries.forEach { level ->
            QuestionType.entries.forEach { type ->
                assertNotNull(
                    "$level + $type 应有对应模板",
                    PromptTemplates.get(level, type),
                )
            }
        }
    }

    // ---------- 作文词数要求 ----------

    @Test
    fun `四级作文词数要求为120到180`() {
        val rule = EssayLengthRule.of(ExamType.CET4)
        assertEquals(120, rule.minWords)
        assertEquals(180, rule.maxWords)
        assertEquals("120 words to 180 words", rule.display)
    }

    @Test
    fun `六级作文词数要求为150到200`() {
        val rule = EssayLengthRule.of(ExamType.CET6)
        assertEquals(150, rule.minWords)
        assertEquals(200, rule.maxWords)
        assertEquals("150 words to 200 words", rule.display)
    }

    @Test
    fun `四级六级词数要求不同`() {
        assertFalse(
            "四级与六级词数要求不应相同",
            EssayLengthRule.of(ExamType.CET4) == EssayLengthRule.of(ExamType.CET6),
        )
    }

    // ---------- 底部提示：仅作文显示 ----------

    @Test
    fun `四级作文底部提示为120words到180words`() {
        assertEquals(
            "四级作文词数要求：120 words to 180 words",
            EssayLengthRule.footerHint(ExamType.CET4, QuestionType.WRITING),
        )
    }

    @Test
    fun `六级作文底部提示为150words到200words`() {
        assertEquals(
            "六级作文词数要求：150 words to 200 words",
            EssayLengthRule.footerHint(ExamType.CET6, QuestionType.WRITING),
        )
    }

    @Test
    fun `翻译题型不显示任何词数提示`() {
        assertNull(
            "四级翻译不应有词数提示",
            EssayLengthRule.footerHint(ExamType.CET4, QuestionType.TRANSLATION),
        )
        assertNull(
            "六级翻译不应有词数提示",
            EssayLengthRule.footerHint(ExamType.CET6, QuestionType.TRANSLATION),
        )
    }

    @Test
    fun `切换级别时作文提示同步变化`() {
        val c4 = EssayLengthRule.footerHint(ExamType.CET4, QuestionType.WRITING)
        val c6 = EssayLengthRule.footerHint(ExamType.CET6, QuestionType.WRITING)
        assertTrue("四级六级提示应不同", c4 != c6)
        assertTrue(c4!!.contains("120"))
        assertTrue(c6!!.contains("150"))
    }

    // ---------- 题型枚举 ----------

    @Test
    fun `题型枚举含作文与翻译`() {
        assertEquals(2, QuestionType.entries.size)
        assertEquals("作文", QuestionType.WRITING.label)
        assertEquals("翻译", QuestionType.TRANSLATION.label)
    }

    @Test
    fun `题型fromName容错`() {
        assertEquals(QuestionType.WRITING, QuestionType.fromName("WRITING"))
        assertEquals(QuestionType.TRANSLATION, QuestionType.fromName("TRANSLATION"))
        assertEquals(QuestionType.WRITING, QuestionType.fromName("UNKNOWN"))
        assertEquals(QuestionType.WRITING, QuestionType.fromName(null))
    }
}
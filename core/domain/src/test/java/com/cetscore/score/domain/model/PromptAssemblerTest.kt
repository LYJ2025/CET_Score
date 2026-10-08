package com.cetscore.score.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 提示词组装器测试 —— 验证 common + 四模块的拼接规则与原题目处理。
 */
class PromptAssemblerTest {

    private val common = "【公共部分】角色与总原则"
    private val outputFormat = "【输出格式】档位判定/维度分析/逐句诊断"
    private val module = "【模块】本题型专属标准"

    // ---------- 四套模块文件名 ----------

    @Test
    fun `四套模块文件名映射正确`() {
        assertEquals(
            "cet4-writing.md",
            PromptAssembler.moduleFileName(ExamType.CET4, ScoringTask.ESSAY),
        )
        assertEquals(
            "cet4-translation.md",
            PromptAssembler.moduleFileName(ExamType.CET4, ScoringTask.TRANSLATION),
        )
        assertEquals(
            "cet6-writing.md",
            PromptAssembler.moduleFileName(ExamType.CET6, ScoringTask.ESSAY),
        )
        assertEquals(
            "cet6-translation.md",
            PromptAssembler.moduleFileName(ExamType.CET6, ScoringTask.TRANSLATION),
        )
    }

    @Test
    fun `四套模块文件名互不相同`() {
        val names = ExamType.entries.flatMap { exam ->
            ScoringTask.entries.map { task ->
                PromptAssembler.moduleFileName(exam, task)
            }
        }
        assertEquals("四套模块必须各自独立文件", 4, names.toSet().size)
    }

    // ---------- 拼接顺序 ----------

    @Test
    fun `拼接顺序应为common_模块_级别题型_题目作答_输出格式`() {
        val result = PromptAssembler.assemble(
            common = common,
            module = module,
            outputFormat = outputFormat,
            examType = ExamType.CET4,
            taskType = ScoringTask.ESSAY,
            question = "写一篇关于校园活动的短文",
            answer = "My essay content.",
        )

        val iCommon = result.indexOf(common)
        val iModule = result.indexOf(module)
        val iLevel = result.indexOf("【考试级别】：四级")
        val iTask = result.indexOf("【题型】：作文")
        val iQuestion = result.indexOf("写一篇关于校园活动的短文")
        val iAnswer = result.indexOf("My essay content.")
        val iFormat = result.indexOf(outputFormat)

        assertTrue("common 应存在", iCommon >= 0)
        assertTrue("模块应存在", iModule >= 0)
        assertTrue("级别应存在", iLevel >= 0)
        assertTrue("题型应存在", iTask >= 0)
        assertTrue("题目应存在", iQuestion >= 0)
        assertTrue("作答应存在", iAnswer >= 0)
        assertTrue("输出格式应存在", iFormat >= 0)

        // 严格递增即正确顺序
        assertTrue("顺序错误: common($iCommon) 应最先", iCommon < iModule)
        assertTrue("顺序错误: 模块($iModule) 应在级别($iLevel) 前", iModule < iLevel)
        assertTrue("顺序错误: 级别($iLevel) 应在题型($iTask) 前", iLevel < iTask)
        assertTrue("顺序错误: 题型($iTask) 应在题目($iQuestion) 前", iTask < iQuestion)
        assertTrue("顺序错误: 题目($iQuestion) 应在作答($iAnswer) 前", iQuestion < iAnswer)
        assertTrue("顺序错误: 作答($iAnswer) 应在输出格式($iFormat) 前", iAnswer < iFormat)
    }

    @Test
    fun `四级六级应写入对应级别`() {
        val cet4 = PromptAssembler.assemble(common, module, outputFormat,
            ExamType.CET4, ScoringTask.ESSAY, "q", "a")
        val cet6 = PromptAssembler.assemble(common, module, outputFormat,
            ExamType.CET6, ScoringTask.TRANSLATION, "q", "a")

        assertTrue(cet4.contains("【考试级别】：四级"))
        assertTrue(cet6.contains("【考试级别】：六级"))
        assertTrue(cet6.contains("【题型】：翻译"))
    }

    // ---------- 原题目为空 ----------

    @Test
    fun `原题目为空时插入提示语`() {
        val result = PromptAssembler.assemble(common, module, outputFormat,
            ExamType.CET4, ScoringTask.ESSAY, question = null, answer = "a")

        assertTrue(
            "应包含未填提示",
            result.contains(PromptAssembler.NO_QUESTION_HINT),
        )
    }

    @Test
    fun `原题目为空白字符串等同于未填`() {
        assertTrue(PromptAssembler.shouldWarnMissingQuestion(""))
        assertTrue(PromptAssembler.shouldWarnMissingQuestion("   \n "))
        assertTrue(PromptAssembler.shouldWarnMissingQuestion(null))
    }

    @Test
    fun `原题目已填则不提示`() {
        assertFalse(PromptAssembler.shouldWarnMissingQuestion("题目内容"))
    }

    @Test
    fun `原题目为null时不会崩溃`() {
        val result = PromptAssembler.assemble(common, module, outputFormat,
            ExamType.CET4, ScoringTask.ESSAY, question = null, answer = "我的作文")
        assertTrue(result.contains("我的作文"))
    }

    // ---------- 原题目超长 ----------

    @Test
    fun `原题目超长应截断并提示`() {
        val long = "题".repeat(PromptAssembler.MAX_QUESTION_LENGTH + 500)
        val result = PromptAssembler.assemble(common, module, outputFormat,
            ExamType.CET4, ScoringTask.ESSAY, question = long, answer = "a")

        assertTrue("应包含截断提示", result.contains(PromptAssembler.QUESTION_TOO_LONG_HINT))
        // 截断后长度不应包含全部原文
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

    // ---------- 作答处理 ----------

    @Test
    fun `作答前后空白应被去除`() {
        val result = PromptAssembler.assemble(common, module, outputFormat,
            ExamType.CET4, ScoringTask.ESSAY, question = "q", answer = "\n\n  我的作文  \n")
        assertTrue(result.contains("【我的作答】：\n我的作文"))
    }

    @Test
    fun `作答内容完整保留`() {
        val essay = """
            第一段内容。
            第二段内容。
        """.trimIndent()
        val result = PromptAssembler.assemble(common, module, outputFormat,
            ExamType.CET4, ScoringTask.ESSAY, question = "q", answer = essay)
        assertTrue(result.contains(essay))
    }

    // ---------- 边界 ----------

    @Test
    fun `各段均为空时不应崩溃`() {
        val result = PromptAssembler.assemble("", "", "",
            ExamType.CET4, ScoringTask.ESSAY, question = null, answer = "")
        assertTrue(result.contains("【考试级别】：四级"))
    }

    @Test
    fun `译文含换行应原样保留`() {
        val translation = "It is reported that...\nwhich indicates that..."
        val result = PromptAssembler.assemble(common, module, outputFormat,
            ExamType.CET6, ScoringTask.TRANSLATION, question = "中文原文", answer = translation)
        assertTrue(result.contains(translation))
    }
}
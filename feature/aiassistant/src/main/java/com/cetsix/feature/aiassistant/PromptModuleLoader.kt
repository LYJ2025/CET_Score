package com.cetsix.feature.aiassistant

import android.content.Context
import com.cetsix.score.domain.model.ExamType
import com.cetsix.score.domain.model.PromptAssembler
import com.cetsix.score.domain.model.ScoringTask
import java.io.IOException

/**
 * 提示词模块加载器 —— **只负责从 assets 读文件**。
 *
 * 拼接规则在 core:domain 的 [PromptAssembler]（纯函数，可单测）；
 * 这里仅做 IO 与缓存。
 */
class PromptModuleLoader(private val context: Context) {

    /** 文件内容缓存 —— 同一份模板只读一次 */
    private val cache = mutableMapOf<String, String>()

    /** 读取指定文件，内容以 "---" 分隔的多个部分返回 */
    private fun read(name: String): String = cache.getOrPut(name) {
        try {
            context.assets.open("${PromptAssembler.MODULE_DIR}/$name")
                .bufferedReader()
                .use { it.readText() }
        } catch (e: IOException) {
            ""
        }
    }

    /** 公共部分 */
    fun common(): String = read(PromptAssembler.FILE_COMMON)

    /** 输出格式（所有题型共用） */
    fun outputFormat(): String = read(PromptAssembler.FILE_OUTPUT_FORMAT)

    /** 对应题型的独立模块 */
    fun module(examType: ExamType, taskType: ScoringTask): String =
        read(PromptAssembler.moduleFileName(examType, taskType))

    /**
     * 组装完整提示词。
     *
     * @param question 原题目 / 翻译原文（可空，为空时自动插入提示）
     * @param answer 我的作答 / 译文
     */
    fun buildPrompt(
        examType: ExamType,
        taskType: ScoringTask,
        question: String?,
        answer: String,
    ): String = PromptAssembler.assemble(
        common = common(),
        module = module(examType, taskType),
        outputFormat = outputFormat(),
        examType = examType,
        taskType = taskType,
        question = question,
        answer = answer,
    )

    /** 模板是否加载正常 */
    fun isReady(): Boolean = common().isNotEmpty()
}

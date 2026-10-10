package com.cetscore.feature.aiassistant

import android.content.Context
import com.cetscore.score.domain.model.ExamType
import com.cetscore.score.domain.model.PromptAssembler
import com.cetscore.score.domain.model.PromptTemplate
import com.cetscore.score.domain.model.PromptTemplates
import com.cetscore.score.domain.model.QuestionType
import java.io.IOException

/**
 * 提示词仓库 —— **只负责从 assets 读文件与缓存**。
 *
 * 四份模板各自独立，切换「级别 + 题型」时只加载对应那一份，
 * 绝不会混入其他模块内容。
 */
class PromptRepository(context: Context) {

    private val appContext = context.applicationContext

    /** 文件内容缓存 —— key 为 assetPath */
    private val cache = mutableMapOf<String, String>()

    /** 读取指定模板的正文 */
    fun raw(template: PromptTemplate): String = cache.getOrPut(template.assetPath) {
        try {
            appContext.assets.open(template.assetPath)
                .bufferedReader()
                .use { it.readText() }
        } catch (e: IOException) {
            ""
        }
    }

    /** 按「级别 + 题型」取模板正文 */
    fun raw(examType: ExamType, questionType: QuestionType): String =
        raw(PromptTemplates.get(examType, questionType))

    /**
     * 组装最终要复制的内容。
     *
     * @param question 原题目 / 翻译原文（可空）
     * @param answer 用户作答 / 译文
     */
    fun buildCopyText(
        examType: ExamType,
        questionType: QuestionType,
        question: String?,
        answer: String,
    ): String = PromptAssembler.assemble(
        templateBody = raw(examType, questionType),
        question = question,
        answer = answer,
    )

    /** 模板是否加载正常 */
    fun isReady(examType: ExamType, questionType: QuestionType): Boolean =
        raw(examType, questionType).isNotEmpty()
}
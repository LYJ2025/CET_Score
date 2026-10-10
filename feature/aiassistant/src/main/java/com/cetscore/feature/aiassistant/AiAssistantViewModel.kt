package com.cetscore.feature.aiassistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cetscore.score.domain.model.AssistantNavigation
import com.cetscore.score.domain.model.AssistantStep
import com.cetscore.score.domain.model.EssayLengthRule
import com.cetscore.score.domain.model.ExamType
import com.cetscore.score.domain.model.PromptAssembler
import com.cetscore.score.domain.model.QuestionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** AI 助手运行模式 */
enum class AssistMode(val label: String, val description: String) {
    NORMAL(
        "普通模式",
        "复制提示词到任意 AI 工具（ChatGPT / DeepSeek / 豆包等）",
    ),
    PRO(
        "专业模式",
        "软件内置 AI 直接评分（需配置 API Key，会发送作答到第三方 API）",
    ),
}

/** AI 评分助手 UI 状态 */
data class AiAssistantUiState(
    // ---------- 替换式导航 ----------
    val step: AssistantStep = AssistantStep.LevelSelect,
    val examType: ExamType? = null,

    // ---------- 输入 ----------
    /** 原题目 / 翻译原文（可空） */
    val question: String = "",
    /** 我的作答 / 译文 */
    val answer: String = "",

    // ---------- 模式 ----------
    val mode: AssistMode = AssistMode.NORMAL,

    // ---------- 结果 ----------
    /** 当前模块模板的正文（预览区显示的内容） */
    val templateBody: String = "",
    val templatesReady: Boolean = false,
) {
    /** 当前是否在第三屏（评分界面） */
    val isScoringScreen: Boolean get() = step is AssistantStep.Scoring

    /** 第三屏的题型；非第三屏时为 null */
    val questionType: QuestionType?
        get() = (step as? AssistantStep.Scoring)?.questionType

    /**
     * 底部词数提示。
     *
     * 作文 → 显示对应级别的要求；**翻译 → null（不显示任何词数注释）**。
     */
    val lengthHint: String?
        get() {
            val level = examType ?: return null
            val type = questionType ?: return null
            return EssayLengthRule.footerHint(level, type)
        }

    /** 作答是否为空 —— 仅用于提示，**不阻止提交** */
    val isAnswerEmpty: Boolean get() = answer.isBlank()

    /** 原题目缺失，应提示用户 */
    val shouldWarnQuestion: Boolean
        get() = PromptAssembler.shouldWarnMissingQuestion(question)

    /** 原题目超长 */
    val isQuestionTooLong: Boolean
        get() = PromptAssembler.isQuestionTooLong(question)

    /**
     * 是否可提交。
     *
     * 只检查模板是否就绪，**不要求作答非空** ——
     * 真实场景中"完全没做"本身就是有效答案，应允许提交让 AI 评 0 分。
     */
    val canSubmit: Boolean get() = templatesReady && templateBody.isNotEmpty()

    /** 标题，例如"四级 · 作文" */
    val scoringTitle: String
        get() = (step as? AssistantStep.Scoring)?.let {
            "${it.examType.shortLabel} · ${it.questionType.label}"
        } ?: "AI 评分助手"

    /** 空作答时复制内容里的占位引导语 */
    private val answerPlaceholder = "（请在此粘贴你的作文或译文）"

    /**
     * 要复制到剪贴板的完整内容。
     *
     * = 对应模块模板全文 + 原题目 + 我的作答
     */
    val copyText: String
        get() = PromptAssembler.assemble(
            templateBody = templateBody,
            question = question,
            answer = if (isAnswerEmpty) answerPlaceholder else answer,
        )
}

/**
 * AI 评分助手 ViewModel。
 *
 * 职责：
 *  - 持有 [AssistantNavigation] 做替换式导航的状态管理
 *  - 按「级别 + 题型」加载对应模板并组装
 *  - 切换普通 / 专业模式
 *
 * **不负责写剪贴板**，也不负责网络请求 —— 两者分别由 UI 层与 pro 层执行。
 */
class AiAssistantViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val repository = PromptRepository(application)

    /** 替换式导航状态机 */
    private val navigation = AssistantNavigation()

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    // ==================== 替换式导航 ====================

    /** 第一屏：选择级别（四级 / 六级） */
    fun onLevelSelected(examType: ExamType) {
        navigation.selectLevel(examType)
        _uiState.update {
            it.copy(step = navigation.currentStep, examType = navigation.examType)
        }
    }

    /** 第二屏：选择题型（作文 / 翻译） */
    fun onTaskSelected(questionType: QuestionType) {
        navigation.selectTask(questionType)
        _uiState.update {
            it.copy(step = navigation.currentStep, examType = navigation.examType)
        }
        loadTemplate()
    }

    /**
     * 返回上一层。
     *
     * - 第三屏 → 第二屏（级别保留）
     * - 第二屏 → 第一屏（级别清空）
     * - 第一屏 → 返回 false，交给宿主关闭页面
     */
    fun onBack(): Boolean {
        val consumed = navigation.back()
        _uiState.update {
            it.copy(
                step = navigation.currentStep,
                examType = navigation.examType,
                // 回到第一屏时清空输入与模板，避免残留上一轮的题目与作答
                templateBody = if (consumed) it.templateBody else "",
                question = if (consumed) it.question else "",
                answer = if (consumed) it.answer else "",
            )
        }
        if (!consumed) loadTemplate()
        return consumed
    }

    /** 从首页等外部入口直接进入第三屏 */
    fun jumpToScoring(examType: ExamType, questionType: QuestionType) {
        navigation.jumpToScoring(examType, questionType)
        _uiState.update {
            it.copy(step = navigation.currentStep, examType = navigation.examType)
        }
        loadTemplate()
    }

    // ==================== 输入 ====================

    fun onQuestionChange(text: String) {
        _uiState.update { it.copy(question = text) }
    }

    fun onAnswerChange(text: String) {
        _uiState.update { it.copy(answer = text) }
    }

    fun onClearAnswer() {
        _uiState.update { it.copy(answer = "") }
    }

    // ==================== 模式 ====================

    fun onModeChange(mode: AssistMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    // ==================== 模板加载 ====================

    /** 按当前「级别 + 题型」加载对应模板 */
    private fun loadTemplate() {
        val state = _uiState.value
        val level = state.examType
        val type = state.questionType
        if (level == null || type == null) {
            _uiState.update { it.copy(templateBody = "", templatesReady = false) }
            return
        }

        viewModelScope.launch {
            val body = withContext(Dispatchers.IO) {
                repository.raw(level, type)
            }
            // 期间用户可能已切走，需重新校验上下文再写回（防止预览区残留上一模块内容）
            val current = _uiState.value
            if (current.examType == level && current.questionType == type) {
                _uiState.update {
                    it.copy(templateBody = body, templatesReady = body.isNotEmpty())
                }
            }
        }
    }

    /** 要复制到剪贴板的完整内容 */
    fun buildCopyText(): String = _uiState.value.copyText
}
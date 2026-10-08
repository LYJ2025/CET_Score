package com.cetsix.feature.aiassistant

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cetsix.score.domain.model.AssistantNavigation
import com.cetsix.score.domain.model.AssistantStep
import com.cetsix.score.domain.model.ExamType
import com.cetsix.score.domain.model.PromptAssembler
import com.cetsix.score.domain.model.ScoringTask
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
    /** 已选级别；返回第一屏时会被清空 */
    val examType: ExamType? = null,

    // ---------- 输入 ----------
    /** 原题目 / 翻译原文（可空） */
    val question: String = "",
    /** 我的作答 / 译文 */
    val answer: String = "",

    // ---------- 模式 ----------
    val mode: AssistMode = AssistMode.NORMAL,

    // ---------- 结果 ----------
    /** 已组装的完整提示词 */
    val prompt: String = "",
    val templatesReady: Boolean = false,
) {
    /** 当前是否在第三屏（评分界面） */
    val isScoringScreen: Boolean get() = step is AssistantStep.Scoring

    /** 第三屏的上下文组合 */
    val scoringContext: Pair<ExamType, ScoringTask>?
        get() = (step as? AssistantStep.Scoring)?.let { it.examType to it.taskType }

    /** 作答是否为空 */
    val isAnswerEmpty: Boolean get() = answer.isBlank()

    /** 原题目缺失，应提示用户 */
    val shouldWarnQuestion: Boolean
        get() = PromptAssembler.shouldWarnMissingQuestion(question)

    /** 原题目超长 */
    val isQuestionTooLong: Boolean
        get() = PromptAssembler.isQuestionTooLong(question)

    /** 是否可以提交（作答非空） */
    val canSubmit: Boolean get() = !isAnswerEmpty && templatesReady

    /** 标题，例如"四级 · 作文" */
    val scoringTitle: String
        get() = scoringContext?.let { (exam, task) -> "${exam.shortLabel} · ${task.label}" }
            ?: "AI 评分助手"
}

/**
 * AI 评分助手 ViewModel。
 *
 * 职责：
 *  - 持有 [AssistantNavigation] 做替换式导航的状态管理
 *  - 加载并组装提示词
 *  - 切换普通 / 专业模式
 *
 * **不负责写剪贴板**，也不负责网络请求 —— 两者分别由 UI 层与 online 专属层执行。
 */
class AiAssistantViewModel(
    application: Application,
) : AndroidViewModel(application) {

    private val loader = PromptModuleLoader(application)

    /** 替换式导航状态机 */
    private val navigation = AssistantNavigation()

    private val _uiState = MutableStateFlow(AiAssistantUiState())
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val ready = withContext(Dispatchers.IO) { loader.isReady() }
            _uiState.update { it.copy(templatesReady = ready) }
        }
    }

    // ==================== 替换式导航 ====================

    /** 第一屏：选择级别（四级 / 六级） */
    fun onLevelSelected(examType: ExamType) {
        navigation.selectLevel(examType)
        _uiState.update {
            it.copy(step = navigation.currentStep, examType = navigation.examType)
        }
    }

    /** 第二屏：选择题型（作文 / 翻译） */
    fun onTaskSelected(taskType: ScoringTask) {
        navigation.selectTask(taskType)
        _uiState.update {
            it.copy(step = navigation.currentStep, examType = navigation.examType)
        }
        refreshPrompt()
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
                // 回到第一屏时清空输入与提示词，避免残留上一轮的题目与作答
                prompt = if (consumed) it.prompt else "",
                question = if (consumed) it.question else "",
                answer = if (consumed) it.answer else "",
            )
        }
        return consumed
    }

    /** 从首页等外部入口直接进入第三屏 */
    fun jumpToScoring(examType: ExamType, taskType: ScoringTask) {
        navigation.jumpToScoring(examType, taskType)
        _uiState.update {
            it.copy(step = navigation.currentStep, examType = navigation.examType)
        }
        refreshPrompt()
    }

    // ==================== 输入 ====================

    fun onQuestionChange(text: String) {
        _uiState.update { it.copy(question = text) }
        refreshPrompt()
    }

    fun onAnswerChange(text: String) {
        _uiState.update { it.copy(answer = text) }
        refreshPrompt()
    }

    fun onClearAnswer() {
        _uiState.update { it.copy(answer = "") }
        refreshPrompt()
    }

    // ==================== 模式 ====================

    fun onModeChange(mode: AssistMode) {
        _uiState.update { it.copy(mode = mode) }
    }

    // ==================== 提示词组装 ====================

    /** 按当前输入重新组装提示词 */
    fun refreshPrompt() {
        val state = _uiState.value
        val context = state.scoringContext
        if (context == null || !state.templatesReady) {
            _uiState.update { it.copy(prompt = "") }
            return
        }
        val (examType, taskType) = context

        viewModelScope.launch {
            val prompt = withContext(Dispatchers.Default) {
                loader.buildPrompt(
                    examType = examType,
                    taskType = taskType,
                    question = state.question,
                    answer = state.answer,
                )
            }
            // 期间用户可能已切走，需重新校验上下文再写回
            if (_uiState.value.scoringContext == context) {
                _uiState.update { it.copy(prompt = prompt) }
            }
        }
    }

    /** 要复制到剪贴板的完整内容 */
    fun buildCopyText(): String = _uiState.value.prompt
}
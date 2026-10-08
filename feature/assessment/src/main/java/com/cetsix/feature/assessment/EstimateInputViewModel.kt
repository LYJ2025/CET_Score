package com.cetsix.feature.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cetsix.score.domain.model.ExamConfig
import com.cetsix.score.domain.model.ExamType
import com.cetsix.score.domain.model.QuestionGroup
import com.cetsix.score.domain.model.ScoreCalculator
import com.cetsix.score.domain.model.ScoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 估分输入页的 UI 状态。
 *
 * 用不可变 data class 整体暴露，Compose 侧只需 collectAsStateWithLifecycle。
 */
data class EstimateInputUiState(
    val examType: ExamType = ExamType.CET4,
    /** 题组 id → 答对题数 */
    val answers: Map<String, Int> = emptyMap(),
    val writingScore: Int = 0,
    val translationScore: Int = 0,
    /** 是否已作答过至少一题，用于按钮高亮判断 */
    val hasAnyInput: Boolean = false,
) {
    val config: ExamConfig get() = ExamConfig.of(examType)

    /**
     * 输入是否完整。
     *
     * 规则：每个题组都至少要明确"答对几题"（可以是 0，但必须被用户碰过），
     * 且写作/翻译都要选过档位。
     */
    val isInputComplete: Boolean
        get() = config.allGroups.all { answers.containsKey(it.id) } &&
            answers.values.any { it > 0 } &&
            writingScore > 0 &&
            translationScore > 0

    /** 实时预估总分，用于卡片上方的即时反馈 */
    val liveTotalScore: Int
        get() = if (!hasAnyInput) 0
        else ScoreCalculator.calculate(
            config = config,
            answers = answers,
            writingScore = writingScore,
            translationScore = translationScore,
        ).totalScore

    /** 实时预估百分制总分（用于进度条） */
    val livePercentTotal: Float
        get() = if (!hasAnyInput) 0f
        else ScoreCalculator.calculate(
            config = config,
            answers = answers,
            writingScore = writingScore,
            translationScore = translationScore,
        ).percentTotal

    val listeningGroups: List<QuestionGroup> get() = config.listeningGroups
    val readingGroups: List<QuestionGroup> get() = config.readingGroups
}

/**
 * 估分输入页 ViewModel。
 *
 * 只做状态管理，算分交给 core:domain 的纯函数 ScoreCalculator，
 * 这样算法可以脱离 Android 单独测试。
 */
class EstimateInputViewModel(
    private val examType: ExamType = ExamType.CET4,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EstimateInputUiState(examType = examType))
    val uiState: StateFlow<EstimateInputUiState> = _uiState.asStateFlow()

    /** 跳转到结果页时携带的完整结果 */
    private val _result = MutableStateFlow<ScoreResult?>(null)
    val result: StateFlow<ScoreResult?> = _result.asStateFlow()

    /** 题组答对题数变化 */
    fun onAnswerChange(groupId: String, correctCount: Int) {
        _uiState.update { state ->
            val group = state.config.groupById(groupId) ?: return@update state
            val safe = correctCount.coerceIn(0, group.questionCount)
            state.copy(
                answers = state.answers + (groupId to safe),
                hasAnyInput = true,
            )
        }
    }

    fun onWritingScoreChange(score: Int) {
        _uiState.update { it.copy(writingScore = score.coerceIn(0, 15), hasAnyInput = true) }
    }

    fun onTranslationScoreChange(score: Int) {
        _uiState.update { it.copy(translationScore = score.coerceIn(0, 15), hasAnyInput = true) }
    }

    /** 一键重置为全 0 */
    fun onReset() {
        _uiState.update {
            it.copy(answers = emptyMap(), writingScore = 0, translationScore = 0, hasAnyInput = false)
        }
    }

    /**
     * 计算并产出结果。
     * 输入不完整时不产出，由 UI 层禁用按钮来保证。
     */
    fun onCalculate() {
        val state = _uiState.value
        if (!state.isInputComplete) return
        viewModelScope.launch {
            _result.value = ScoreCalculator.calculate(
                config = state.config,
                answers = state.answers,
                writingScore = state.writingScore,
                translationScore = state.translationScore,
            )
        }
    }

    /**
     * 同步计算并返回结果。
     *
     * 用同步返回而非 StateFlow，是为了让导航层拿到结果后再决定是否跳转，
     * 避免"先跳转再等数据"导致结果页先闪一下空状态。
     * 输入不完整时返回 null。
     */
    fun calculateNow(): ScoreResult? {
        val state = _uiState.value
        if (!state.isInputComplete) return null
        return ScoreCalculator.calculate(
            config = state.config,
            answers = state.answers,
            writingScore = state.writingScore,
            translationScore = state.translationScore,
        )
    }

    /** 结果已被消费，避免返回首页后重复触发 */
    fun onResultConsumed() {
        _result.value = null
    }
}

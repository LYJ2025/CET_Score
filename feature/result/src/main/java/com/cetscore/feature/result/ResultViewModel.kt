package com.cetscore.feature.result

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cetscore.core.data.entity.ExamRecord
import com.cetscore.core.data.model.AssessmentDetail
import com.cetscore.core.data.model.QuestionGroupAnswer
import com.cetscore.core.data.repository.ExamRecordRepository
import com.cetscore.score.domain.model.ExamType
import com.cetscore.score.domain.model.ScoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 结果页 UI 状态 */
data class ResultUiState(
    val result: ScoreResult? = null,
    val createdAt: Long = 0L,
    /** 已保存的记录 id，null 表示尚未保存 */
    val savedRecordId: Long? = null,
    val showNoteDialog: Boolean = false,
    val saving: Boolean = false,
) {
    val isSaved: Boolean get() = savedRecordId != null
}

class ResultViewModel(
    private val repository: ExamRecordRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultUiState())
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    /** 由导航层传入的计算结果 */
    fun setResult(result: ScoreResult, createdAt: Long) {
        _uiState.value = ResultUiState(result = result, createdAt = createdAt)
    }

    fun onShowNoteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showNoteDialog = show)
    }

    /**
     * 保存记录到本地。
     *
     * detailJson 存各题组答对题数与写作/翻译自评分，
     * 后续历史页/趋势图可据此还原明细。
     */
    fun onSave(note: String?) {
        val state = _uiState.value
        val result = state.result ?: return
        if (state.isSaved || state.saving) return

        _uiState.value = state.copy(saving = true)
        viewModelScope.launch {
            val detail = AssessmentDetail(
                groups = result.groupResults.map {
                    QuestionGroupAnswer(it.groupId, it.correctCount)
                },
                writingScore = result.writingPercent.toInt(),
                translationScore = result.translationPercent.toInt(),
            )
            val id = repository.insertRecord(
                ExamRecord(
                    examType = result.examType.name,
                    createdAt = state.createdAt,
                    totalScore = result.totalScore,
                    listeningScore = result.listeningTotal,
                    readingScore = result.readingTotal,
                    writingScore = result.writingTotal,
                    translationScore = result.translationTotal,
                    note = note,
                    detailJson = repository.encodeDetail(detail),
                )
            )
            _uiState.value = _uiState.value.copy(savedRecordId = id, saving = false, showNoteDialog = false)
        }
    }
}

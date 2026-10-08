package com.cetsix.feature.trend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cetsix.core.data.entity.ExamRecord
import com.cetsix.core.data.repository.ExamRecordRepository
import com.cetsix.score.domain.model.ExamType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class TrendFilter(val label: String) {
    ALL("全部"),
    CET4("四级"),
    CET6("六级"),
}

data class TrendUiState(
    val filter: TrendFilter = TrendFilter.ALL,
    val records: List<ExamRecord> = emptyList(),
    val isLoading: Boolean = true,
    /** 当前展开详情的记录 */
    val selectedRecord: ExamRecord? = null,
) {
    val isEmpty: Boolean get() = !isLoading && records.isEmpty()
    /** 是否有带备注的记录 —— 用于提示用户图上的小圆环含义 */
    val hasAnyNote: Boolean get() = records.any { !it.note.isNullOrBlank() }
}

class TrendViewModel(
    private val repository: ExamRecordRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(TrendFilter.ALL)
    private val _selected = MutableStateFlow<ExamRecord?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<TrendUiState> = combine(
        _filter,
        repository.getAllRecords(),
        _selected,
    ) { filter, all, selected ->
        val filtered = when (filter) {
            TrendFilter.ALL -> all
            TrendFilter.CET4 -> all.filter { it.examType == ExamType.CET4.name }
            TrendFilter.CET6 -> all.filter { it.examType == ExamType.CET6.name }
        }
        // 折线图需要时间正序
        val chronological = filtered.sortedBy { it.createdAt }
        TrendUiState(
            filter = filter,
            records = chronological,
            isLoading = false,
            selectedRecord = selected,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TrendUiState(),
    )

    fun onFilterChange(filter: TrendFilter) {
        _filter.value = filter
        _selected.value = null
    }

    /** 点击数据点：index 对应按时间正序后的下标 */
    fun onPointClick(index: Int) {
        _selected.value = uiState.value.records.getOrNull(index)
    }

    fun onCloseDetail() {
        _selected.value = null
    }

    fun onDeleteSelected() {
        val record = _selected.value ?: return
        viewModelScope.launch {
            repository.deleteRecord(record.id)
            _selected.value = null
        }
    }
}

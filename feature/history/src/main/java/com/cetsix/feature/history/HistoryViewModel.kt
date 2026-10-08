package com.cetsix.feature.history

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

/** 历史页筛选类型 */
enum class HistoryFilter(val label: String) {
    ALL("全部"),
    CET4("四级"),
    CET6("六级"),
}

/** 历史页 UI 状态 */
data class HistoryUiState(
    val filter: HistoryFilter = HistoryFilter.ALL,
    val records: List<ExamRecord> = emptyList(),
    val isLoading: Boolean = true,
    /** 正在编辑备注的记录 id */
    val editingNoteId: Long? = null,
    /** 正在查看详情的记录 id */
    val detailRecordId: Long? = null,
) {
    val isEmpty: Boolean get() = !isLoading && records.isEmpty()
}

/**
 * 历史记录页 ViewModel。
 *
 * 记录列表由 Room 的 Flow 驱动，保存/编辑/删除后 UI 自动刷新，无需手动 reload。
 */
class HistoryViewModel(
    private val repository: ExamRecordRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(HistoryFilter.ALL)
    val filter: StateFlow<HistoryFilter> = _filter.asStateFlow()

    private val _editingNoteId = MutableStateFlow<Long?>(null)
    private val _detailRecordId = MutableStateFlow<Long?>(null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HistoryUiState> = combine(
        _filter,
        repository.getAllRecords(),
        _editingNoteId,
        _detailRecordId,
    ) { filter, all, editingId, detailId ->
        val filtered = when (filter) {
            HistoryFilter.ALL -> all
            HistoryFilter.CET4 -> all.filter { it.examType == ExamType.CET4.name }
            HistoryFilter.CET6 -> all.filter { it.examType == ExamType.CET6.name }
        }
        HistoryUiState(
            filter = filter,
            records = filtered,
            isLoading = false,
            editingNoteId = editingId,
            detailRecordId = detailId,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    fun onFilterChange(filter: HistoryFilter) {
        _filter.value = filter
    }

    fun onEditNote(id: Long) {
        _detailRecordId.value = null      // 编辑时先关掉详情面板
        _editingNoteId.value = id
    }

    fun onDismissNoteDialog() {
        _editingNoteId.value = null
    }

    /** 点击卡片查看详情 */
    fun onShowDetail(id: Long) {
        _detailRecordId.value = id
    }

    fun onDismissDetail() {
        _detailRecordId.value = null
    }

    fun onUpdateNote(id: Long, note: String?) {
        viewModelScope.launch {
            repository.updateNote(id, note?.trim()?.ifEmpty { null })
            _editingNoteId.value = null
        }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch {
            repository.deleteRecord(id)
            // 若删除的正是正在编辑/查看的记录，关闭对应弹窗
            if (_editingNoteId.value == id) _editingNoteId.value = null
            if (_detailRecordId.value == id) _detailRecordId.value = null
        }
    }
}

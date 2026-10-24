package com.cetscore.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cetscore.core.data.entity.ExamRecord
import com.cetscore.core.data.repository.ExamRecordRepository
import com.cetscore.score.domain.model.ExamConfig
import com.cetscore.score.domain.model.ExamType
import com.cetscore.score.domain.model.ScoreCalculator
import com.cetscore.score.domain.model.ScoreResult
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
    /** 详情面板要展示的记录 */
    val detailRecord: ExamRecord? = null,
    /**
     * 由 detailJson 重新算出的完整结果（含占比与题组明细）。
     *
     * 为 null 有两种情况：
     *  - 当前没有打开详情
     *  - 该记录是早期版本存的，detailJson 里没有题组明细
     */
    val detailResult: ScoreResult? = null,
) {
    val isEmpty: Boolean get() = !isLoading && records.isEmpty()

    /** 打开详情但算不出明细 —— 需要在 UI 上说明原因，而不是显示一堆 0 */
    val detailMissingDetail: Boolean
        get() = detailRecord != null && detailResult == null
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
        // 详情可能来自被筛选掉的记录（例如先看详情再切筛选），所以从全量里找
        val detailRecord = detailId?.let { id -> all.firstOrNull { it.id == id } }
        HistoryUiState(
            filter = filter,
            records = filtered,
            isLoading = false,
            editingNoteId = editingId,
            detailRecordId = detailId,
            detailRecord = detailRecord,
            detailResult = detailRecord?.let(::recompute),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HistoryUiState(),
    )

    /**
     * 用保存时的 detailJson 重新跑一遍计算器。
     *
     * 为什么重算而不是存快照：
     *  - 数据库只存题组对题数与写作/翻译自评，占比图与题组得分都由这几项推导
     *  - 重算能保证历史详情与结果页**完全一致**（同一个 ScoreCalculator）
     *
     * 早期记录可能没有明细（detailJson 里 groups 为空），此时返回 null，
     * 由 UI 提示"该记录未保存明细"，而不是显示一排 0 分误导用户。
     */
    private fun recompute(record: ExamRecord): ScoreResult? {
        // 复用 repository 的解码逻辑（内部已做 try-catch，脏数据不会崩）
        val detail = repository.decodeDetail(record.detailJson)

        if (detail.groups.isEmpty()) return null

        return runCatching {
            ScoreCalculator.calculate(
                config = ExamConfig.of(ExamType.fromName(record.examType)),
                answers = detail.groups.associate { it.groupId to it.correctCount },
                writingScore = detail.writingScore,
                translationScore = detail.translationScore,
            )
        }.getOrNull()
    }

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

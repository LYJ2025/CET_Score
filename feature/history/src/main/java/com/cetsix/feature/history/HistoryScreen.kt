package com.cetsix.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cetsix.core.data.entity.ExamRecord
import com.cetsix.core.ui.component.GlassBackground
import com.cetsix.core.ui.component.GlassCard
import com.cetsix.core.ui.theme.Dimens
import com.cetsix.core.ui.theme.LocalIsDarkTheme
import com.cetsix.score.domain.model.ExamType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 历史记录页。
 *
 * 列表按时间倒序。每行显示：考试类型标签 + 总分 + 时间 + 备注首行。
 * 长按卡片弹出菜单：编辑备注 / 删除。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    uiState: HistoryUiState,
    onFilterChange: (HistoryFilter) -> Unit,
    onShowDetail: (Long) -> Unit,
    onDismissDetail: () -> Unit,
    onEditNote: (Long) -> Unit,
    onDismissNoteDialog: () -> Unit,
    onUpdateNote: (Long, String?) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hazeState = rememberHazeState()
    val dark = LocalIsDarkTheme.current

    GlassBackground(hazeState = hazeState, darkTheme = dark) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                Spacer(Modifier.height(Dimens.SpaceM))

                // ---------- 标题 + 筛选 ----------
                Text(
                    text = "历史记录",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Dimens.SpaceM))

                FilterTabs(
                    hazeState = hazeState,
                    selected = uiState.filter,
                    onSelect = onFilterChange,
                )

                Spacer(Modifier.height(Dimens.SpaceM))

                // ---------- 列表 / 空状态 ----------
                when {
                    uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    uiState.isEmpty -> EmptyState(hazeState = hazeState)

                    else -> LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                        contentPadding = PaddingValues(bottom = Dimens.SpaceXL),
                    ) {
                        items(uiState.records, key = { it.id }) { record ->
                            RecordRow(
                                hazeState = hazeState,
                                record = record,
                                onClick = { onShowDetail(record.id) },
                                onEditNote = { onEditNote(record.id) },
                                onDelete = { onDelete(record.id) },
                            )
                        }
                    }
                }
            }
        }
    }

    // ---------- 记录详情面板（点击卡片进入） ----------
    val detailId = uiState.detailRecordId
    if (detailId != null) {
        val record = uiState.records.firstOrNull { it.id == detailId }
        if (record != null) {
            HistoryDetailBottomSheet(
                record = record,
                onDismiss = onDismissDetail,
                onEditNote = { onEditNote(record.id) },
                onDelete = { onDelete(record.id) },
            )
        }
    }

    // ---------- 备注编辑弹窗 ----------
    val editingId = uiState.editingNoteId
    if (editingId != null) {
        val record = uiState.records.firstOrNull { it.id == editingId }
        NoteEditDialog(
            initialValue = record?.note,
            onConfirm = { note -> onUpdateNote(editingId, note) },
            onDismiss = onDismissNoteDialog,
        )
    }
}

/** 全部 / 四级 / 六级 切换 */
@Composable
private fun FilterTabs(
    hazeState: HazeState,
    selected: HistoryFilter,
    onSelect: (HistoryFilter) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        HistoryFilter.entries.forEach { filter ->
            val isSelected = filter == selected
            GlassCard(
                hazeState = hazeState,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(Dimens.CornerSmall))
                    .clickable { onSelect(filter) },
                cornerRadius = Dimens.CornerSmall,
                blurRadius = 12.dp,
                tintAlpha = if (isSelected) 0.50f else 0.24f,
                borderAlpha = if (isSelected) 0.75f else 0.28f,
                elevation = if (isSelected) 6.dp else 2.dp,
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                Text(
                    text = filter.label,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 单条记录 */
@Composable
private fun RecordRow(
    hazeState: HazeState,
    record: ExamRecord,
    onClick: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
) {
    val dark = LocalIsDarkTheme.current
    val examType = ExamType.fromName(record.examType)
    var menuOpen by remember { mutableStateOf(false) }

    Box {
        GlassCard(
            hazeState = hazeState,
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuOpen = true },
                ),
            cornerRadius = Dimens.CornerMedium,
            blurRadius = 20.dp,
            elevation = 6.dp,
            contentPadding = PaddingValues(Dimens.SpaceL),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 左侧：类型标签 + 时间
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // 四级/六级 用不同颜色区分
                    val badgeColor = when (examType) {
                        ExamType.CET4 -> Color(0xFF7C8AA8)
                        ExamType.CET6 -> Color(0xFF9E8FA8)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(badgeColor.copy(alpha = if (dark) 0.32f else 0.18f))
                            .padding(horizontal = Dimens.SpaceM, vertical = 3.dp),
                    ) {
                        Text(
                            text = examType.shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = badgeColor,
                        )
                    }
                    Text(
                        text = formatDateTime(record.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                // 右侧：总分
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${record.totalScore}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "/ 710",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 备注：超过一行只显示首行 + 省略号
            val note = record.note
            if (!note.isNullOrBlank()) {
                Spacer(Modifier.height(Dimens.SpaceS))
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        // 长按菜单
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text("编辑备注") },
                onClick = {
                    menuOpen = false
                    onEditNote()
                },
            )
            DropdownMenuItem(
                text = { Text("删除记录") },
                onClick = {
                    menuOpen = false
                    onDelete()
                },
            )
        }
    }
}

/** 空状态 */
@Composable
private fun EmptyState(hazeState: HazeState) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(Dimens.ScreenPadding),
        ) {
            // 简单插画：空心圆环 + 虚线感
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                )
            }
            Spacer(Modifier.height(Dimens.SpaceL))
            Text(
                text = "还没有估分记录",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Dimens.SpaceXS))
            Text(
                text = "去估一次吧",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

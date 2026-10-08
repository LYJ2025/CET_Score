package com.cetsix.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cetsix.core.data.entity.ExamRecord
import com.cetsix.core.ui.theme.Dimens
import com.cetsix.score.domain.model.ExamType

/**
 * 记录详情底部面板（历史页）。
 *
 * 需求："备注如果超过 1 行，显示前 1 行 + 省略号，点击卡片进入详情可看全文"。
 * 本面板展示完整备注，内部可滚动 —— 备注再长也不会撑破布局。
 *
 * 与趋势图的 RecordDetailBottomSheet 行为一致，
 * 但历史页额外提供"编辑备注"入口，方便就地修改。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailBottomSheet(
    record: ExamRecord,
    onDismiss: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val examType = ExamType.fromName(record.examType)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = Dimens.ScreenPadding)
                .padding(top = Dimens.SpaceM, bottom = Dimens.SpaceL),
        ) {
            // ---------- 标题 ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "${examType.shortLabel} · ${formatDateTime(record.createdAt)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${record.totalScore} 分",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }

            Spacer(Modifier.height(Dimens.SpaceM))

            // ---------- 各模块分数 ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                ModuleScore("听力", record.listeningScore, 248.5f)
                ModuleScore("阅读", record.readingScore, 248.5f)
                ModuleScore("写作", record.writingScore, 106.5f)
                ModuleScore("翻译", record.translationScore, 106.5f)
            }

            // ---------- 完整备注 ----------
            val note = record.note
            Spacer(Modifier.height(Dimens.SpaceL))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "备注",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                TextButton(onClick = onEditNote) { Text("编辑") }
            }
            Spacer(Modifier.height(Dimens.SpaceS))

            if (note.isNullOrBlank()) {
                Text(
                    text = "（无备注）",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            } else {
                // 限高 + 垂直滚动：长备注只在本面板内滚动，不影响页面布局
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text(
                        text = note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 22.sp,
                    )
                }
                Spacer(Modifier.height(Dimens.SpaceXS))
                Text(
                    text = "${note.length} 字",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }

            // ---------- 删除 ----------
            Spacer(Modifier.height(Dimens.SpaceM))
            TextButton(onClick = onDelete) {
                Text("删除这条记录", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ModuleScore(label: String, score: Float, full: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatScoreValue(score),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "/ ${formatScoreValue(full)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

/** 分值格式化：整数不带小数点 */
private fun formatScoreValue(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString()
    else String.format("%.1f", value)

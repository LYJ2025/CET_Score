package com.cetscore.feature.trend

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cetscore.core.data.entity.ExamRecord
import com.cetscore.core.ui.theme.Dimens
import com.cetscore.score.domain.model.ExamType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 记录详情底部面板。
 *
 * 点击折线图上的数据点后弹出。这里是**唯一**展示完整备注的地方，
 * 与图表布局完全解耦 —— 备注再长也只在本面板内滚动，
 * 不会影响图表尺寸或挤压数据点。
 */
@Composable
fun RecordDetailBottomSheet(
    record: ExamRecord,
    onClose: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    val examType = ExamType.fromName(record.examType)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = Dimens.ScreenPadding)
            .padding(top = Dimens.SpaceM, bottom = Dimens.SpaceL),
    ) {
        // 标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "${examType.shortLabel} · ${formatDate(record.createdAt)}",
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
            TextButton(onClick = onClose) {
                Text("关闭")
            }
        }

        Spacer(Modifier.height(Dimens.SpaceM))

        // 各模块分数
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ScoreItem("听力", record.listeningScore, 248.5f)
            ScoreItem("阅读", record.readingScore, 248.5f)
            ScoreItem("写作", record.writingScore, 106.5f)
            ScoreItem("翻译", record.translationScore, 106.5f)
        }

        // 完整备注：可滚动，长文本换行显示
        val note = record.note
        if (!note.isNullOrBlank()) {
            Spacer(Modifier.height(Dimens.SpaceL))
            Text(
                text = "备注",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Dimens.SpaceS))
            BoxScrollableNote(note)
        }

        if (onDelete != null) {
            Spacer(Modifier.height(Dimens.SpaceM))
            TextButton(onClick = onDelete) {
                Text("删除这条记录", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** 备注正文：限高 + 垂直滚动，保证再长也不撑破面板 */
@Composable
private fun BoxScrollableNote(note: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = note,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
        )
    }
}

/** 单个模块分数 */
@Composable
private fun ScoreItem(label: String, score: Float, full: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = if (score == score.toInt().toFloat()) score.toInt().toString()
            else String.format("%.1f", score),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "/ ${if (full == full.toInt().toFloat()) full.toInt().toString() else full}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

internal fun formatDate(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

package com.cetscore.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cetscore.core.data.entity.ExamRecord
import com.cetscore.core.ui.AppCopy
import com.cetscore.core.ui.component.DonutChartWithLegend
import com.cetscore.core.ui.component.GlassCard
import com.cetscore.core.ui.component.GroupDetailRow
import com.cetscore.core.ui.component.ModuleScoreGrid
import com.cetscore.core.ui.component.ScoreSectionHeader
import com.cetscore.core.ui.theme.Dimens
import com.cetscore.core.ui.theme.LocalIsDarkTheme
import com.cetscore.core.ui.theme.scoreColor
import com.cetscore.score.domain.model.ExamConfig
import com.cetscore.score.domain.model.ExamType
import com.cetscore.score.domain.model.ScoreResult
import dev.chrisbanes.haze.rememberHazeState

/**
 * 记录详情底部面板（历史页）。
 *
 * 展示内容与估分结果页**保持一致**：
 *  总分 + 进度条 + 及格提示 / 各模块得分 / 各部分占比环形图 / 题组明细 / 备注
 *
 * 之前只显示总分和四个模块分，用户无法回看当时每题的情况；
 * 现在由 [ScoreResult] 驱动，与结果页共用同一套组件，避免两边显示不一致。
 *
 * @param result 由 detailJson 重算出的完整结果；早期记录没有明细时为 null
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailBottomSheet(
    record: ExamRecord,
    result: ScoreResult?,
    onDismiss: () -> Unit,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val examType = ExamType.fromName(record.examType)

    // 底部面板内自建 hazeState：卡片退化为半透明色块，风格仍与全局一致
    val hazeState = rememberHazeState()
    val dark = LocalIsDarkTheme.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .heightIn(max = 720.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding)
                .padding(top = Dimens.SpaceM, bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
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
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${record.totalScore}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "/ 710",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }

            // ---------- 及格线进度 ----------
            val ratio = record.totalScore.toFloat() / ExamConfig.TOTAL_SCORE
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(ratio.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary, scoreColor(ratio))
                            )
                        ),
                )
            }
            Text(
                text = if (record.totalScore >= ExamConfig.PASS_SCORE) {
                    "已达到 425 分及格线"
                } else {
                    "距 425 分及格线还差 ${ExamConfig.PASS_SCORE - record.totalScore} 分"
                },
                style = MaterialTheme.typography.labelMedium,
                color = if (record.totalScore >= ExamConfig.PASS_SCORE) Color(0xFF6BA88B)
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = AppCopy.PASS_HINT,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            )

            // ---------- 明细区 ----------
            if (result != null) {
                // 各模块得分
                ScoreSectionHeader("各模块得分")
                ModuleScoreGrid(hazeState = hazeState, result = result)

                // 各部分占比
                ScoreSectionHeader("各部分得分占比")
                GlassCard(
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = Dimens.CornerMedium,
                    blurRadius = 22.dp,
                    elevation = 8.dp,
                    contentPadding = PaddingValues(Dimens.SpaceL),
                ) {
                    DonutChartWithLegend(
                        proportions = result.proportions,
                        centerTotal = result.totalScore,
                        centerLabel = "预估总分",
                    )
                }

                // 题组明细
                ScoreSectionHeader("题组明细")
                GlassCard(
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = Dimens.CornerMedium,
                    blurRadius = 22.dp,
                    elevation = 8.dp,
                    contentPadding = PaddingValues(Dimens.SpaceL),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
                        result.groupResults.forEach { group ->
                            GroupDetailRow(group = group)
                        }
                    }
                }
            } else {
                // 早期记录只有总分与四个模块分，没有题组明细
                ScoreSectionHeader("各模块得分")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    ModuleScore("听力", record.listeningScore, 248.5f)
                    ModuleScore("阅读", record.readingScore, 248.5f)
                    ModuleScore("写作", record.writingScore, 106.5f)
                    ModuleScore("翻译", record.translationScore, 106.5f)
                }
                Text(
                    text = "这条记录保存时还没有题组明细（旧版本数据），" +
                        "因此无法显示各部分占比与每题对错。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }

            // ---------- 完整备注 ----------
            val note = record.note
            ScoreSectionHeader("备注")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (note.isNullOrBlank()) "（无备注）" else "${note.length} 字",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
                TextButton(onClick = onEditNote) { Text("编辑") }
            }

            if (!note.isNullOrBlank()) {
                // 限高 + 垂直滚动：长备注只在本面板内滚动
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
            }

            // ---------- 删除 ----------
            Spacer(Modifier.height(Dimens.SpaceS))
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

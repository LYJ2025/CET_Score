package com.cetsix.feature.result

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cetsix.core.ui.AppCopy
import com.cetsix.core.ui.component.GlassBackground
import com.cetsix.core.ui.component.GlassCard
import com.cetsix.core.ui.theme.Dimens
import com.cetsix.core.ui.theme.LocalIsDarkTheme
import com.cetsix.core.ui.theme.scoreColor
import com.cetsix.score.domain.model.ExamConfig
import com.cetsix.score.domain.model.GroupResult
import com.cetsix.score.domain.model.ScoreResult
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 估分结果页。
 *
 * 结构（对应 Prompt 7）：
 *  1. 顶部大卡片：预估总分 + 考试类型与时间 + 进度条 + 及格提示
 *  2. 模块得分：听力 / 阅读 / 写作 / 翻译 四项
 *  3. 各部分得分占比：环形图 + 图例
 *  4. 明细列表：每个题组一行
 *  5. 底部按钮：保存记录 / 重新估分
 */
@Composable
fun EstimateResultScreen(
    result: ScoreResult?,
    createdAt: Long,
    isSaved: Boolean,
    showNoteDialog: Boolean,
    hazeState: HazeState = rememberHazeState(),
    onShowNoteDialog: (Boolean) -> Unit,
    onSave: (String?) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalIsDarkTheme.current

    GlassBackground(hazeState = hazeState, darkTheme = dark) {
        Box(modifier = modifier.fillMaxSize()) {
            if (result == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "暂无估分结果",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                return@GlassBackground
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                Spacer(Modifier.height(Dimens.SpaceS))

                // ---------- 1. 总分大卡 ----------
                TotalScoreCard(hazeState = hazeState, result = result, createdAt = createdAt)

                // ---------- 2. 模块得分 ----------
                SectionHeader("各模块得分")
                ModuleScoreGrid(hazeState = hazeState, result = result)

                // ---------- 3. 占比环形图 ----------
                SectionHeader("各部分得分占比")
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

                // ---------- 4. 明细列表 ----------
                SectionHeader("题组明细")
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
                            GroupDetailRow(group)
                        }
                    }
                }

                // ---------- 免责声明 ----------
                Text(
                    text = AppCopy.DISCLAIMER,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.padding(horizontal = Dimens.SpaceS),
                )

                Spacer(Modifier.height(110.dp))   // 给底部按钮留空间
            }

            // ---------- 5. 底部按钮 ----------
            BottomActions(
                hazeState = hazeState,
                isSaved = isSaved,
                onSave = { onShowNoteDialog(true) },
                onRestart = onRestart,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }

    // 备注输入弹窗
    if (showNoteDialog) {
        NoteInputDialog(
            initialValue = null,
            onConfirm = onSave,
            onDismiss = { onShowNoteDialog(false) },
        )
    }
}

/** 顶部总分卡片 */
@Composable
private fun TotalScoreCard(hazeState: HazeState, result: ScoreResult, createdAt: Long) {
    val ratio = result.totalScore.toFloat() / ExamConfig.TOTAL_SCORE
    val accent = MaterialTheme.colorScheme.primary

    GlassCard(
        hazeState = hazeState,
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = Dimens.CornerLarge,
        blurRadius = 28.dp,
        elevation = 12.dp,
        contentPadding = PaddingValues(Dimens.SpaceXL),
    ) {
        Text(
            text = "预估总分",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.SpaceS))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${result.totalScore}",
                style = MaterialTheme.typography.displayLarge,
                color = scoreColor(ratio),
            )
            Spacer(Modifier.size(Dimens.SpaceS))
            Text(
                text = "/ 710",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

        Spacer(Modifier.height(Dimens.SpaceXS))
        Text(
            text = "${result.examType.shortLabel} · ${formatDateTime(createdAt)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(Dimens.SpaceM))

        // 进度条
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
                    .background(Brush.horizontalGradient(listOf(accent, scoreColor(ratio)))),
            )
        }

        Spacer(Modifier.height(Dimens.SpaceM))
        // 及格状态 + 需求指定的固定提示语
        Text(
            text = if (result.isPassed) "已达到 425 分及格线" else "距 425 分及格线还差 ${result.gapToPass} 分",
            style = MaterialTheme.typography.labelMedium,
            color = if (result.isPassed) Color(0xFF6BA88B) else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.SpaceXS))
        Text(
            text = AppCopy.PASS_HINT,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
        )
    }
}

/** 模块得分 2×2 网格 */
@Composable
private fun ModuleScoreGrid(hazeState: HazeState, result: ScoreResult) {
    val items = listOf(
        Triple("听力理解", result.listeningTotal, 35f),
        Triple("阅读理解", result.readingTotal, 35f),
        Triple("写作", result.writingTotal, 15f),
        Triple("翻译", result.translationTotal, 15f),
    )

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        items.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                rowItems.forEach { (label, score, full) ->
                    ModuleScoreTile(
                        hazeState = hazeState,
                        label = label,
                        score = score,
                        fullScore = full * 7.1f,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ModuleScoreTile(
    hazeState: HazeState,
    label: String,
    score: Float,
    fullScore: Float,
    modifier: Modifier = Modifier,
) {
    val ratio = if (fullScore > 0) score / fullScore else 0f

    GlassCard(
        hazeState = hazeState,
        modifier = modifier,
        cornerRadius = Dimens.CornerSmall,
        blurRadius = 16.dp,
        elevation = 5.dp,
        contentPadding = PaddingValues(Dimens.SpaceM),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatOneDecimal(score),
            style = MaterialTheme.typography.headlineMedium,
            color = scoreColor(ratio),
        )
        Text(
            text = "/ ${formatOneDecimal(fullScore)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 题组明细行 */
@Composable
private fun GroupDetailRow(group: GroupResult) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = group.fullName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "答对 ${group.accuracyText}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "得 ${formatOneDecimal(group.rawScore)} 分",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** 底部两个按钮 */
@Composable
private fun BottomActions(
    hazeState: HazeState,
    isSaved: Boolean,
    onSave: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, MaterialTheme.colorScheme.background.copy(alpha = 0.9f))
                )
            )
            .navigationBarsPadding()
            .padding(Dimens.SpaceL),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            // 保存记录
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(Dimens.CornerMedium))
                    .background(
                        if (isSaved) {
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f),
                                )
                            )
                        } else {
                            Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.8f)))
                        }
                    )
                    .clickable(enabled = !isSaved) { onSave() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isSaved) "已保存" else "保存记录",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSaved) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                )
            }

            // 重新估分
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(54.dp)
                    .clip(RoundedCornerShape(Dimens.CornerMedium))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    .clickable(onClick = onRestart),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "重新估分",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

/** 分区标题 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(top = Dimens.SpaceS),
    )
}

/** 时间格式化 */
internal fun formatDateTime(timestamp: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(timestamp))

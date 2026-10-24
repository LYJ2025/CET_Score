package com.cetscore.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cetscore.core.ui.theme.Dimens
import com.cetscore.core.ui.theme.scoreColor
import com.cetscore.score.domain.model.GroupResult
import com.cetscore.score.domain.model.Proportion
import dev.chrisbanes.haze.HazeState

/**
 * 估分结果的可复用展示组件。
 *
 * 抽到 core:ui 的原因：结果页与历史详情页需要**完全一致**的展示，
 * 否则同一份数据在两个界面看起来不一样，用户会以为算错了。
 */

/** 分区标题 */
@Composable
fun ScoreSectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(top = Dimens.SpaceS),
    )
}

/**
 * 模块得分 2×2 网格。
 *
 * 传入的 [com.cetscore.score.domain.model.ScoreResult] 中，
 * 各 total 字段已是 710 分制，所以满分按 35/15 × 7.1 换算。
 */
@Composable
fun ModuleScoreGrid(
    hazeState: HazeState,
    result: com.cetscore.score.domain.model.ScoreResult,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        Triple("听力理解", result.listeningTotal, 35f),
        Triple("阅读理解", result.readingTotal, 35f),
        Triple("写作", result.writingTotal, 15f),
        Triple("翻译", result.translationTotal, 15f),
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
    ) {
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

/** 题组明细行：左标题 + 右对题数与得分 */
@Composable
fun GroupDetailRow(group: GroupResult, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
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

/**
 * 环形图 + 图例。
 *
 * **纯 Compose Canvas 手绘，不引入图表库**：用 [Canvas] 的 drawArc 逐段绘制扇形。
 *
 * 关键处理：
 *  1. **占比为 0 的部分不绘制**。若绘制 0% 的扇形，drawArc 的起止角相同，
 *     在部分设备上会画出长度为 0 但有描边宽度的异常弧线（视觉上像一条线）。
 *  2. **各部分占比由 ScoreCalculator 用最大余数法保证合计 100%**，
 *     所以这里按比例换算角度不会出现缺口。
 *  3. 图例用全称 + 换行（全称较长需支持换行，不允许溢出）。
 */
@Composable
fun DonutChartWithLegend(
    proportions: List<Proportion>,
    centerTotal: Int,
    centerLabel: String,
    modifier: Modifier = Modifier,
    colors: List<Color> = defaultDonutColors(),
    donutThickness: Float = 0.42f,   // 环宽占外半径的比例
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // ---------- 左侧：环形图 ----------
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                val strokeWidth = size.minDimension * donutThickness
                val outerRadius = (size.minDimension - strokeWidth) / 2f
                val topLeft = Offset(
                    x = (size.width - outerRadius * 2) / 2f,
                    y = (size.height - outerRadius * 2) / 2f,
                )
                val arcSize = Size(outerRadius * 2, outerRadius * 2)

                val drawable = proportions.filter { it.proportion > 0f }
                var startAngle = -90f   // 从 12 点方向开始

                drawable.forEachIndexed { index, p ->
                    val sweep = p.proportion / 100f * 360f
                    if (sweep <= 0f) return@forEachIndexed
                    drawArc(
                        color = colors[index % colors.size],
                        startAngle = startAngle,
                        // sweep 减一个极小量，消除相邻扇形之间的抗锯齿缝隙
                        sweepAngle = (sweep - 0.35f).coerceAtLeast(0.1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth),
                    )
                    startAngle += sweep
                }
            }

            // ---------- 中心：总分 ----------
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$centerTotal",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = centerLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // ---------- 右侧：图例 ----------
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.SpaceM),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            proportions.forEachIndexed { index, p ->
                LegendRow(
                    color = if (p.proportion > 0f) colors[index % colors.size]
                    else Color.Transparent,
                    name = p.fullName,
                    percentText = p.proportionText,
                    rawText = "${formatOneDecimal(p.rawScore)} 分",
                    dimmed = p.proportion <= 0f,
                )
            }
        }
    }
}

/** 图例一行：色块 + 全称 + 百分比 + 原始分 */
@Composable
private fun LegendRow(
    color: Color,
    name: String,
    percentText: String,
    rawText: String,
    dimmed: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(
                    if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f)
                    else color
                ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            lineHeight = 14.sp,
        )
        Spacer(Modifier.width(6.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = percentText,
                style = MaterialTheme.typography.labelMedium,
                color = if (dimmed) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = rawText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/**
 * 环形图配色。
 *
 * 低饱和度、彼此可区分，且在浅色/深色背景下都有足够对比度。
 * 顺序上把同属一个大模块的相邻色排在一起（听力 2 色、阅读 3 色）。
 */
fun defaultDonutColors(): List<Color> = listOf(
    Color(0xFF7C8AA8),   // 听力 A —— 雾霾蓝
    Color(0xFFA8B4C8),   // 听力 B —— 浅雾蓝
    Color(0xFF8FA6C4),   // 阅读 A —— 灰蓝
    Color(0xFFA9BCC9),   // 阅读 B —— 更浅的灰蓝
    Color(0xFFC9A87A),   // 阅读 C —— 暖沙色
    Color(0xFF6BA88B),   // 写作 —— 雾绿
    Color(0xFF9E8FA8),   // 翻译 —— 灰紫
)

/** 一位小数，整数则省略小数位 */
fun formatOneDecimal(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString()
    else String.format("%.1f", value)

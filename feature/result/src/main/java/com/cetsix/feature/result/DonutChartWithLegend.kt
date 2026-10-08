package com.cetsix.feature.result

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cetsix.core.ui.theme.Dimens
import com.cetsix.score.domain.model.Proportion
import kotlin.math.min

/**
 * 环形图 + 图例。
 *
 * **纯 Compose Canvas 手绘，不引入图表库**：
 * 用 [Canvas] 的 drawArc 逐段绘制扇形。
 *
 * 关键处理：
 *  1. **占比为 0 的部分不绘制**。若绘制 0% 的扇形，drawArc 的起止角相同，
 *     在部分设备上会画出长度为 0 但有描边宽度的异常弧线（视觉上像一条线）。
 *  2. **各部分占比由 ScoreCalculator 用最大余数法保证合计 100%**，
 *     所以这里按比例换算角度不会出现缺口。
 *  3. 图例用全称 + 换行（Prompt 要求全称较长需支持换行，不允许溢出）。
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

                // 只绘制占比 > 0 的部分，避免 0% 扇形画出异常细线
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
        // 色块
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
        // 全称：允许换行（Prompt 明确要求）
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
            lineHeight = 14.sp,
        )
        Spacer(Modifier.width(6.dp))
        // 占比 + 原始分
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
internal fun formatOneDecimal(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString()
    else String.format("%.1f", value)

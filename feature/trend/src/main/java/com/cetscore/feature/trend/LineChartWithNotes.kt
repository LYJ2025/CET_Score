package com.cetscore.feature.trend

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.cetscore.core.data.entity.ExamRecord
import com.cetscore.score.domain.model.ExamConfig
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 带备注标记的折线图 —— **纯 Compose Canvas 手绘，不引入图表库**。
 *
 * ============================ 关键设计 ============================
 * 如何避免长备注把图表"撑爆"：
 *
 * 1. **坐标系内绝不渲染备注文字。**
 *    备注可能有几百字，若直接画在数据点旁，文本会超出图表宽度、
 *    挤压相邻数据点，甚至触发布局溢出导致整个页面崩溃。
 *
 * 2. **有备注的点只画一个极小的圆环标记**（直径 6dp），
 *    视觉上表示"这条记录有备注"，不占用额外空间。
 *
 * 3. **X 轴标签做抽稀**：超过 [maxXLabels] 个点时，只显示部分标签，
 *    避免密集文字互相重叠。
 *
 * 4. **点击数据点才展示详情**，由调用方弹出 Bottom Sheet。
 *    详情在图表外部渲染，文本长度与图表布局完全解耦。
 *
 * 5. **图表高度固定**（由外部传入），文本再长也不会改变图表尺寸。
 * =================================================================
 *
 * @param records 数据点，按时间正序传入（内部会再排一次保证正确）
 * @param onPointClick 点击某个数据点时回调其索引
 */
@Composable
fun LineChartWithNotes(
    records: List<ExamRecord>,
    onPointClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF7C8AA8),
    gridColor: Color = Color(0x668897A6),
    passLineColor: Color = Color(0xFFC98A6A),
    chartHeight: Dp = 260.dp,
    maxXLabels: Int = 6,
) {
    // 按时间正序：折线从左到右应为时间顺序
    val sorted = remember(records) { records.sortedBy { it.createdAt } }

    // 命中区域：记录每个数据点的 x 坐标，便于点击判定
    var hitPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(chartHeight),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(sorted) {
                    detectTapGestures { offset ->
                        // 找到距离点击位置最近的数据点
                        val threshold = 44.dp.toPx()
                        val nearest = hitPoints.minByOrNull { p ->
                            abs(p.x - offset.x)
                        }
                        if (nearest != null && abs(nearest.x - offset.x) <= threshold) {
                            val index = hitPoints.indexOf(nearest)
                            if (index >= 0) onPointClick(index)
                        }
                    }
                },
        ) {
            val leftPad = 34.dp.toPx()      // 左侧留白放 Y 轴刻度
            val bottomPad = 26.dp.toPx()    // 底部留白放 X 轴标签
            val topPad = 12.dp.toPx()
            val rightPad = 10.dp.toPx()

            val chartWidth = size.width - leftPad - rightPad
            val chartHeight = size.height - bottomPad - topPad
            if (chartWidth <= 0f || chartHeight <= 0f) return@Canvas

            // ---------- 网格与 Y 轴刻度 ----------
            // Y 轴固定 0..710
            val gridLines = 5
            repeat(gridLines) { i ->
                val ratio = i.toFloat() / (gridLines - 1)
                val y = topPad + chartHeight * ratio

                drawLine(
                    color = gridColor.copy(alpha = 0.18f),
                    start = Offset(leftPad, y),
                    end = Offset(size.width - rightPad, y),
                    strokeWidth = 1f,
                )
                // Y 轴刻度文字用 drawContext 无法直接画文本，这里省略刻度值，
                // 由外层用 Box 叠加 Text 实现（见 TrendScreen）
            }

            if (sorted.isEmpty()) return@Canvas

            // ---------- 及格线 425 ----------
            val passRatio = ExamConfig.PASS_SCORE.toFloat() / ExamConfig.TOTAL_SCORE
            val passY = topPad + chartHeight * (1f - passRatio)
            drawLine(
                color = passLineColor,
                start = Offset(leftPad, passY),
                end = Offset(size.width - rightPad, passY),
                strokeWidth = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
            )

            // ---------- 数据点坐标 ----------
            val stepX = if (sorted.size > 1) chartWidth / (sorted.size - 1) else 0f
            val points = sorted.mapIndexed { index, record ->
                val ratio = record.totalScore.toFloat() / ExamConfig.TOTAL_SCORE
                val x = if (sorted.size == 1) leftPad + chartWidth / 2f
                else leftPad + stepX * index
                val y = topPad + chartHeight * (1f - ratio)
                Offset(x, y)
            }
            hitPoints = points

            // ---------- 渐变填充 ----------
            if (points.size > 1) {
                val linePath = Path().apply {
                    moveTo(points.first().x, topPad + chartHeight)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, topPad + chartHeight)
                    close()
                }
                drawPath(
                    path = linePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.28f),
                            lineColor.copy(alpha = 0.02f),
                        ),
                        startY = topPad,
                        endY = topPad + chartHeight,
                    ),
                )
            }

            // ---------- 折线 ----------
            if (points.size > 1) {
                val strokePath = Path().apply {
                    moveTo(points.first().x, points.first().y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                drawPath(
                    path = strokePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }

            // ---------- 数据点 + 备注标记 ----------
            points.forEachIndexed { index, point ->
                val hasNote = !sorted[index].note.isNullOrBlank()

                // 外圈光晕
                drawCircle(
                    color = lineColor.copy(alpha = 0.18f),
                    radius = 9.dp.toPx(),
                    center = point,
                )
                // 实心点
                drawCircle(
                    color = Color.White,
                    radius = 5.5.dp.toPx(),
                    center = point,
                )
                drawCircle(
                    color = lineColor,
                    radius = 4.dp.toPx(),
                    center = point,
                )

                // 有备注：额外画一个极小的圆环标记（直径 6dp）
                // 只用图形标记，绝不画备注文字
                if (hasNote) {
                    drawCircle(
                        color = Color(0xFFC9A87A),
                        radius = 3.dp.toPx(),
                        center = point.copy(x = point.x + 9.dp.toPx(), y = point.y - 9.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx()),
                    )
                }
            }

            // ---------- X 轴标签（抽稀） ----------
            val labelStep = if (points.size <= maxXLabels) 1
            else (points.size / maxXLabels).coerceAtLeast(1)
            points.forEachIndexed { index, point ->
                if (index % labelStep == 0 || index == points.lastIndex) {
                    // 标签用简短日期，由外部叠加 Text；此处不画文字，避免长文本撑爆图表
                    drawCircle(
                        color = gridColor.copy(alpha = 0.5f),
                        radius = 1.5.dp.toPx(),
                        center = Offset(point.x, topPad + chartHeight + 8.dp.toPx()),
                    )
                }
            }
        }
    }
}

package com.cetsix.core.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.min

/**
 * 应用背景 —— 毛玻璃的"底"。
 *
 * 关键点：玻璃卡片要看出模糊效果，其后必须有可见的内容。
 * 所以这里画低饱和渐变 + 若干柔光光斑（blob）+ 细网格，
 * 卡片覆盖上去才有层次。
 *
 * 同时给整层打 hazeSource，玻璃卡片就能采到这张背景。
 */
@Composable
fun GlassBackground(
    hazeState: HazeState,
    darkTheme: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val baseColors = if (darkTheme) {
        listOf(Color(0xFF1A1B2E), Color(0xFF1F2A2E), Color(0xFF171A28))
    } else {
        listOf(Color(0xFFF7F4F1), Color(0xFFF3E7EC), Color(0xFFEFF1F4))
    }

    // 光斑：低饱和、冷调，不喧宾夺主
    val blobs = if (darkTheme) {
        listOf(
            Color(0xFF3E5F82) to Offset(0.15f, 0.18f),
            Color(0xFF2E6B6B) to Offset(0.85f, 0.30f),
            Color(0xFF4A4270) to Offset(0.50f, 0.85f),
            Color(0xFF35607A) to Offset(0.10f, 0.70f),
        )
    } else {
        listOf(
            Color(0xFFD9E4F0) to Offset(0.15f, 0.18f),
            Color(0xFFE8DCE6) to Offset(0.85f, 0.30f),
            Color(0xFFDDE7E4) to Offset(0.50f, 0.85f),
            Color(0xFFE4E2EE) to Offset(0.10f, 0.70f),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(baseColors))
            .hazeSource(state = hazeState),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val shortest = min(size.width, size.height)

            // 柔光光斑：径向渐变，中心 alpha ~0.5 往外淡出
            blobs.forEach { (color, pos) ->
                val center = Offset(size.width * pos.x, size.height * pos.y)
                val radius = shortest * 0.55f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(color.copy(alpha = 0.50f), Color.Transparent),
                        center = center,
                        radius = radius,
                    ),
                    radius = radius,
                    center = center,
                )
            }

            // 细网格：给模糊一点结构感，否则纯渐变模糊后看不出效果
            val step = 36.dp.toPx()
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = if (darkTheme) Color.White.copy(alpha = 0.035f)
                    else Color(0xFF8899AA).copy(alpha = 0.07f),
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f,
                )
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = if (darkTheme) Color.White.copy(alpha = 0.035f)
                    else Color(0xFF8899AA).copy(alpha = 0.07f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += step
            }
        }

        content()
    }
}

package com.cetsix.core.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import com.cetsix.core.ui.theme.Dimens
import com.cetsix.core.ui.theme.LocalIsDarkTheme

/**
 * 毛玻璃卡片 —— 全项目统一的基础容器。
 *
 * 构成要素：
 *  1. 背景模糊（hazeEffect，API 31+ 走 RenderEffect，以下走 RenderScript 回退）
 *  2. 半透明白/黑叠加（让文字在模糊层上仍可读）
 *  3. 1dp 细腻描边（模拟玻璃边缘反光）
 *  4. 柔和阴影
 *
 * @param hazeState 与背景 hazeSource 绑定的状态
 */
@Composable
fun GlassCard(
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = Dimens.CornerLarge,
    blurRadius: Dp = 24.dp,
    // 玻璃底色透明度：浅色模式用白，深色模式用黑
    tintAlpha: Float = 0.42f,
    borderAlpha: Float = 0.55f,
    elevation: Dp = 10.dp,
    contentPadding: PaddingValues = PaddingValues(Dimens.SpaceL),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dark = LocalIsDarkTheme.current
    val shape = RoundedCornerShape(cornerRadius)

    // 玻璃叠加色：深色模式压暗，浅色模式提亮
    val overlay = if (dark) Color.Black.copy(alpha = tintAlpha) else Color.White.copy(alpha = tintAlpha)
    val borderColor = if (dark) Color.White.copy(alpha = borderAlpha * 0.5f)
    else Color.White.copy(alpha = borderAlpha)

    // 高光：左上角更亮，模拟玻璃受光
    val sheen = if (dark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.55f)

    var base = modifier
        .shadow(
            elevation = elevation,
            shape = shape,
            ambientColor = if (dark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8899AA).copy(alpha = 0.28f),
            spotColor = if (dark) Color.Black.copy(alpha = 0.5f) else Color(0xFF8899AA).copy(alpha = 0.24f),
        )
        .clip(shape)
        .hazeEffect(state = hazeState) {
            this.blurRadius = blurRadius
            tints = listOf(
                HazeTint(
                    color = overlay,
                    blendMode = BlendMode.SrcOver,
                )
            )
        }
        .background(overlay, shape)
        // 左上高光
        .background(
            Brush.linearGradient(
                listOf(sheen, Color.Transparent),
                start = Offset.Zero,
                end = Offset(400f, 700f),
            ),
            shape,
        )
        .border(BorderStroke(1.dp, borderColor), shape)
        .padding(contentPadding)

    if (onClick != null) {
        base = base.clickable(onClick = onClick)
    }

    Column(modifier = base, content = content)
}

/**
 * 大号入口卡片 —— 首页「四级估分」「六级估分」用。
 * 左侧竖排标题 + 副标题，右侧留一个装饰性图标位。
 */
@Composable
fun GlassEntryCard(
    hazeState: HazeState,
    title: String,
    subtitle: String,
    badge: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val dark = LocalIsDarkTheme.current
    val accent = MaterialTheme.colorScheme.primary

    GlassCard(
        hazeState = hazeState,
        modifier = modifier.fillMaxWidth(),
        cornerRadius = Dimens.CornerLarge,
        blurRadius = 28.dp,
        tintAlpha = if (dark) 0.50f else 0.50f,
        elevation = 12.dp,
        contentPadding = PaddingValues(Dimens.SpaceL),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                // 徽标
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelMedium,
                    color = accent,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 右侧圆形装饰，模拟玻璃球折射
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        Brush.linearGradient(
                            listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.35f))
                        )
                    )
                    .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
            }
        }
    }
}

/** 小标签，用于分类/状态标记 */
@Composable
fun GlassChip(
    hazeState: HazeState,
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    GlassCard(
        hazeState = hazeState,
        modifier = modifier,
        cornerRadius = 50.dp,
        blurRadius = 12.dp,
        tintAlpha = 0.35f,
        elevation = 2.dp,
        contentPadding = PaddingValues(horizontal = Dimens.SpaceM, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
        )
    }
}

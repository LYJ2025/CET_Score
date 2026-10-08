package com.cetsix.core.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cetsix.core.ui.theme.Dimens
import com.cetsix.core.ui.theme.LocalIsDarkTheme
import com.cetsix.score.domain.model.RubricBand
import com.cetsix.score.domain.model.WritingTranslationRubric
import dev.chrisbanes.haze.HazeState
import kotlin.math.roundToInt

/**
 * 写作 / 翻译的档位选择器。
 *
 * 交互流程（对应 Prompt 4 的要求）：
 *  1. 横向滑动展示 6 个档位卡片，用户点选一档
 *  2. 选中后展开 0..15 的滑块，允许在该档位区间内微调
 *  3. 默认值取该档位区间的中位数（如 13-15 档默认 14）
 *
 * @param rubric 评分标准（写作/翻译各一份，四六级文案不同）
 * @param score 当前分数 0..15
 * @param onScoreChange 分数变化回调
 */
@Composable
fun ScorePicker(
    rubric: WritingTranslationRubric,
    score: Int,
    onScoreChange: (Int) -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    val dark = LocalIsDarkTheme.current
    val bands = rubric.sortedBands          // 高分档在前
    val currentBand = rubric.bandOf(score)
    val listState = rememberLazyListState()

    // 分数变化时，让对应档位自动滚到可见区域
    LaunchedEffect(currentBand.min) {
        val index = bands.indexOfFirst { it.min == currentBand.min }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        // ---------- 档位横向列表 ----------
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(bands, key = { it.min }) { band ->
                BandCard(
                    hazeState = hazeState,
                    band = band,
                    selected = band.min == currentBand.min,
                    onClick = {
                        // 选中即跳到该档位默认分（中位数）
                        onScoreChange(band.defaultScore)
                    },
                )
            }
        }

        // ---------- 当前档位说明 ----------
        GlassCard(
            hazeState = hazeState,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = Dimens.CornerSmall,
            blurRadius = 14.dp,
            tintAlpha = if (dark) 0.34f else 0.40f,
            elevation = 3.dp,
            contentPadding = PaddingValues(Dimens.SpaceM),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = currentBand.rangeText,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "${rubric.title} · ${score} 分",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(Dimens.SpaceXS))
            Text(
                text = currentBand.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---------- 微调滑块 ----------
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "微调",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${score} / 15",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Slider(
                value = score.toFloat(),
                onValueChange = { onScoreChange(it.roundToInt().coerceIn(0, 15)) },
                valueRange = 0f..15f,
                // 15 档刻度，每 1 分一格
                steps = 14,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                    activeTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    inactiveTickColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .semantics { contentDescription = "${rubric.title}分数滑块" },
            )
        }
    }
}

/** 单个档位卡片 */
@Composable
private fun BandCard(
    hazeState: HazeState,
    band: RubricBand,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val dark = LocalIsDarkTheme.current
    val accent = MaterialTheme.colorScheme.primary

    // 选中态更不透明，未选中更透明 —— 用玻璃层深浅表达选中，比描边更柔和
    GlassCard(
        hazeState = hazeState,
        modifier = Modifier
            .width(132.dp)
            .heightIn(min = 84.dp)
            .then(
                if (selected) Modifier.border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(listOf(accent, accent.copy(alpha = 0.4f))),
                    shape = RoundedCornerShape(Dimens.CornerSmall),
                ) else Modifier
            ),
        cornerRadius = Dimens.CornerSmall,
        blurRadius = 14.dp,
        tintAlpha = if (selected) 0.52f else 0.26f,
        borderAlpha = if (selected) 0.85f else 0.30f,
        elevation = if (selected) 8.dp else 2.dp,
        contentPadding = PaddingValues(Dimens.SpaceM),
        onClick = onClick,
    ) {
        Text(
            text = band.rangeText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Dimens.SpaceXS))
        // 描述最多两行，超出省略
        Text(
            text = band.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
            maxLines = 3,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

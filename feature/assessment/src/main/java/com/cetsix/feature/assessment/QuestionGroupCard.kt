package com.cetsix.feature.assessment

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cetsix.core.ui.component.GlassCard
import com.cetsix.core.ui.component.NumberStepper
import com.cetsix.core.ui.theme.Dimens
import com.cetsix.core.ui.theme.LocalIsDarkTheme
import com.cetsix.score.domain.model.QuestionGroup
import dev.chrisbanes.haze.HazeState

/**
 * 题组卡片。
 *
 * 布局：
 *  - 左：标题（"听力 1-15"）+ 副标题（题组全称）
 *  - 右上："每题 1 分 · 共 15 分"
 *  - 右下：当前得分 xx / 满分
 *  - 底部：数字步进器
 *
 * @param correctCount 当前答对题数
 * @param touched 用户是否已明确操作过该题组（未操作过时得分显示为占位符）
 */
@Composable
fun QuestionGroupCard(
    hazeState: HazeState,
    group: QuestionGroup,
    correctCount: Int,
    touched: Boolean,
    onCorrectCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalIsDarkTheme.current
    val accent = MaterialTheme.colorScheme.primary

    // 当前得分 = 答对题数 × 每题分
    val currentRawScore = correctCount * group.perQuestionScore

    GlassCard(
        hazeState = hazeState,
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        blurRadius = 22.dp,
        elevation = 8.dp,
        contentPadding = PaddingValues(Dimens.SpaceL),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            // ---------- 左侧标题区 ----------
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = group.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Dimens.SpaceXS))
                // 每题分值说明
                Text(
                    text = "每题 ${formatScore(group.perQuestionScore)} 分 · 共 ${formatScore(group.totalRawScore)} 分",
                    style = MaterialTheme.typography.labelMedium,
                    color = accent.copy(alpha = 0.85f),
                )
            }

            // ---------- 右上角分值 ----------
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.CornerSmall))
                    .background(accent.copy(alpha = if (dark) 0.20f else 0.12f))
                    .padding(horizontal = Dimens.SpaceM, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "${formatScore(currentRawScore)} / ${formatScore(group.totalRawScore)}",
                    style = MaterialTheme.typography.titleMedium,
                    color = accent,
                )
            }
        }

        Spacer(Modifier.height(Dimens.SpaceM))

        // ---------- 步进器行 ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NumberStepper(
                value = correctCount,
                onValueChange = onCorrectCountChange,
                min = 0,
                max = group.questionCount,
                step = 1,
                label = "${group.title} 答对题数",
            )

            Text(
                text = "答对 ${correctCount} / ${group.questionCount} 题",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---------- 未作答提示 ----------
        if (!touched) {
            Spacer(Modifier.height(Dimens.SpaceS))
            Text(
                text = "未选择（按 0 题计）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
    }
}

/** 分值格式化：整数不带小数点，0.5 这类保留一位 */
internal fun formatScore(value: Float): String =
    if (value == value.toInt().toFloat()) value.toInt().toString()
    else String.format("%.1f", value)

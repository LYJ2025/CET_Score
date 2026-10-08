package com.cetscore.feature.aiassistant

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cetscore.core.ui.component.GlassCard
import com.cetscore.core.ui.theme.Dimens
import com.cetscore.core.ui.theme.LocalIsDarkTheme
import com.cetscore.score.domain.model.PromptAssembler
import dev.chrisbanes.haze.HazeState

/**
 * 通用输入卡片：标题 + 多行输入框 + 字数 + 可选警示。
 *
 * @param warning 非 null 时显示警示条（如"未填原题目，AI 可能无法判断切题度"）
 */
@Composable
fun InputCard(
    hazeState: HazeState,
    title: String,
    hint: String,
    value: String,
    onValueChange: (String) -> Unit,
    minHeight: Int,
    warning: String?,
    modifier: Modifier = Modifier,
) {
    GlassCard(
        hazeState = hazeState,
        modifier = modifier.fillMaxWidth(),
        cornerRadius = Dimens.CornerMedium,
        blurRadius = 22.dp,
        elevation = 8.dp,
        contentPadding = PaddingValues(Dimens.SpaceL),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "${value.trim().length} 字",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Dimens.SpaceS))

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight.dp),
            placeholder = { Text(hint, style = MaterialTheme.typography.bodyMedium) },
            shape = RoundedCornerShape(Dimens.CornerSmall),
            maxLines = 10,
        )

        if (warning != null) {
            Spacer(Modifier.height(Dimens.SpaceS))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.CornerSmall))
                    .background(
                        Color(0xFFC98A6A).copy(alpha = 0.12f)
                    )
                    .padding(Dimens.SpaceS),
            ) {
                Text(
                    text = warning,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC98A6A),
                )
            }
        }
    }
}

/**
 * 普通模式面板：提示词预览 + 一键复制。
 *
 * 按需求，**不显示**：API Key 输入框、评分结果窗口、思考过程区。
 */
@Composable
fun NormalModePanel(
    hazeState: HazeState,
    prompt: String,
    canSubmit: Boolean,
    hasAnswer: Boolean,
    onCopyPrompt: (String) -> Unit,
    onClearAnswer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {
        // ---------- 提示词预览 ----------
        GlassCard(
            hazeState = hazeState,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = Dimens.CornerMedium,
            blurRadius = 22.dp,
            elevation = 8.dp,
            contentPadding = PaddingValues(Dimens.SpaceL),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "提示词预览",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${prompt.length} 字符",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(Dimens.SpaceS))

            if (prompt.isEmpty()) {
                Text(
                    text = "填写上方内容后自动生成",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 280.dp)
                        .clip(RoundedCornerShape(Dimens.CornerSmall))
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(Dimens.SpaceM),
                ) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        // ---------- 按钮 ----------
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            MainActionButton(
                text = if (hasAnswer) "生成提示词并复制" else "请先填写作答",
                enabled = canSubmit,
                onClick = { onCopyPrompt(prompt) },
                modifier = Modifier.weight(1f),
            )
            SecondaryActionButton(
                text = "清空作答",
                enabled = hasAnswer,
                onClick = onClearAnswer,
            )
        }

        // ---------- 说明 ----------
        Text(
            text = "复制后请打开任意 AI 对话框，粘贴提示词，即可获得评分。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
    }
}

/** 主操作按钮 */
@Composable
fun MainActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalIsDarkTheme.current
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(Dimens.CornerMedium))
            .background(
                if (enabled) {
                    Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.82f)))
                } else {
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = if (dark) 0.18f else 0.16f),
                            MaterialTheme.colorScheme.onSurfaceVariant
                                .copy(alpha = if (dark) 0.12f else 0.12f),
                        )
                    )
                }
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 次操作按钮 */
@Composable
fun SecondaryActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(Dimens.CornerMedium))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}

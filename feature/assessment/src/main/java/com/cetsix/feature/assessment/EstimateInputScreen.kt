package com.cetsix.feature.assessment

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cetsix.core.ui.component.GlassBackground
import com.cetsix.core.ui.component.GlassCard
import com.cetsix.core.ui.component.NumberStepper
import com.cetsix.core.ui.component.ScorePicker
import com.cetsix.core.ui.theme.CetSixTheme
import com.cetsix.core.ui.theme.Dimens
import com.cetsix.core.ui.theme.LocalIsDarkTheme
import com.cetsix.score.domain.model.ExamConfig
import com.cetsix.score.domain.model.ExamType
import com.cetsix.score.domain.model.QuestionGroup
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState

/**
 * 估分输入页。
 *
 * 页面结构（对应 Prompt 5）：
 *  1. 顶部：考试类型标题 + 返回按钮
 *  2. 听力区：每个题组一张毛玻璃卡片，内含数字步进器
 *  3. 阅读区：同上
 *  4. 写作区：档位选择器 + 滑块
 *  5. 翻译区：同上
 *  6. 底部固定按钮："计算预估分"
 *
 * 底部按钮仅在所有题组都已明确作答、写作与翻译都已选档时高亮。
 */
@Composable
fun EstimateInputScreen(
    uiState: EstimateInputUiState,
    onAnswerChange: (String, Int) -> Unit,
    onWritingChange: (Int) -> Unit,
    onTranslationChange: (Int) -> Unit,
    onCalculate: () -> Unit,
    onAiRefineWriting: () -> Unit,
    onAiRefineTranslation: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hazeState = rememberHazeState()
    val dark = LocalIsDarkTheme.current

    GlassBackground(hazeState = hazeState, darkTheme = dark) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            ) {
                Spacer(Modifier.height(Dimens.SpaceS))

                // ---------- 顶部标题栏 ----------
                TopBar(
                    hazeState = hazeState,
                    title = uiState.examType.label,
                    liveScore = uiState.liveTotalScore,
                    onBack = onBack,
                )

                // ---------- 听力区 ----------
                SectionTitle("听力理解", "共 25 题 · 原始分 35")

                uiState.listeningGroups.forEach { group ->
                    QuestionGroupCard(
                        hazeState = hazeState,
                        group = group,
                        correctCount = uiState.answers[group.id] ?: 0,
                        touched = uiState.answers.containsKey(group.id),
                        onCorrectCountChange = { onAnswerChange(group.id, it) },
                    )
                }

                // ---------- 阅读区 ----------
                SectionTitle("阅读理解", "共 25 题 · 原始分 35")

                uiState.readingGroups.forEach { group ->
                    QuestionGroupCard(
                        hazeState = hazeState,
                        group = group,
                        correctCount = uiState.answers[group.id] ?: 0,
                        touched = uiState.answers.containsKey(group.id),
                        onCorrectCountChange = { onAnswerChange(group.id, it) },
                    )
                }

                // ---------- 写作区 ----------
                SectionTitleWithAction(
                    title = "写作",
                    subtitle = "自评 0-15 分",
                    hazeState = hazeState,
                    actionText = "用 AI 精评",
                    onAction = onAiRefineWriting,
                )
                GlassCard(
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = Dimens.CornerMedium,
                    blurRadius = 22.dp,
                    elevation = 8.dp,
                    contentPadding = PaddingValues(Dimens.SpaceL),
                ) {
                    ScorePicker(
                        rubric = uiState.config.writingRubric,
                        score = uiState.writingScore,
                        onScoreChange = onWritingChange,
                        hazeState = hazeState,
                    )
                }

                // ---------- 翻译区 ----------
                SectionTitleWithAction(
                    title = "翻译",
                    subtitle = "自评 0-15 分",
                    hazeState = hazeState,
                    actionText = "用 AI 精评",
                    onAction = onAiRefineTranslation,
                )
                GlassCard(
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = Dimens.CornerMedium,
                    blurRadius = 22.dp,
                    elevation = 8.dp,
                    contentPadding = PaddingValues(Dimens.SpaceL),
                ) {
                    ScorePicker(
                        rubric = uiState.config.translationRubric,
                        score = uiState.translationScore,
                        onScoreChange = onTranslationChange,
                        hazeState = hazeState,
                    )
                }

                Spacer(Modifier.height(96.dp))   // 给底部固定按钮留出空间
            }

            // ---------- 底部固定按钮 ----------
            CalculateButton(
                hazeState = hazeState,
                enabled = uiState.isInputComplete,
                liveScore = uiState.liveTotalScore,
                hasInput = uiState.hasAnyInput,
                onClick = onCalculate,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** 顶部标题栏：返回按钮 + 考试类型 + 实时预估分 */
@Composable
private fun TopBar(
    hazeState: HazeState,
    title: String,
    liveScore: Int,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 返回按钮
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        )
                    )
                )
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "‹",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        Spacer(Modifier.size(Dimens.SpaceM))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "满分 710 分 · 逐题自评",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 实时预估分
        if (liveScore > 0) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$liveScore",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "预估分",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** 带右上角操作按钮的分区标题（用于"用 AI 精评"入口） */
@Composable
private fun SectionTitleWithAction(
    title: String,
    subtitle: String,
    hazeState: HazeState,
    actionText: String,
    onAction: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.SpaceS),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // 小号毛玻璃按钮
        GlassCard(
            hazeState = hazeState,
            modifier = Modifier.clip(RoundedCornerShape(50)),
            cornerRadius = 50.dp,
            blurRadius = 12.dp,
            tintAlpha = 0.40f,
            elevation = 3.dp,
            contentPadding = PaddingValues(
                horizontal = Dimens.SpaceM,
                vertical = 7.dp,
            ),
            onClick = onAction,
        ) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** 分区标题 */
@Composable
private fun SectionTitle(title: String, subtitle: String) {
    Column(modifier = Modifier.padding(top = Dimens.SpaceS)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 底部固定按钮。
 *
 * 未完成输入时置灰（alpha 降低 + 禁用点击），
 * 已实时有输入时显示当前预估分。
 */
@Composable
private fun CalculateButton(
    hazeState: HazeState,
    enabled: Boolean,
    liveScore: Int,
    hasInput: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dark = LocalIsDarkTheme.current
    val accent = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                    )
                )
            )
            .navigationBarsPadding()
            .padding(Dimens.SpaceL),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(Dimens.CornerLarge))
                .background(
                    if (enabled) {
                        Brush.horizontalGradient(
                            listOf(accent, accent.copy(alpha = 0.78f))
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (dark) 0.18f else 0.16f),
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (dark) 0.12f else 0.12f),
                            )
                        )
                    }
                )
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = when {
                    !hasInput -> "计算预估分"
                    enabled -> "计算预估分 · $liveScore"
                    else -> "请完成所有题目"
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 900)
@Composable
private fun EstimateInputPreview() {
    CetSixTheme(darkTheme = false) {
        EstimateInputScreen(
            uiState = EstimateInputUiState(
                examType = ExamType.CET4,
                answers = mapOf("listening_1_15" to 12, "reading_26_35" to 7),
                writingScore = 11,
                translationScore = 9,
                hasAnyInput = true,
            ),
            onAnswerChange = { _, _ -> },
            onWritingChange = {},
            onTranslationChange = {},
            onCalculate = {},
            onAiRefineWriting = {},
            onAiRefineTranslation = {},
            onBack = {},
        )
    }
}

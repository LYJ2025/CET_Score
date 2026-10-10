package com.cetscore.feature.aiassistant

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cetscore.core.ui.component.GlassBackground
import com.cetscore.core.ui.component.GlassCard
import com.cetscore.core.ui.theme.Dimens
import com.cetscore.core.ui.theme.LocalIsDarkTheme
import com.cetscore.score.domain.model.AssistantStep
import com.cetscore.score.domain.model.ExamType
import com.cetscore.score.domain.model.QuestionType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState

/**
 * AI 评分助手页 —— **替换式导航**。
 *
 * ============================ 替换而非叠加 ============================
 * ```
 * 第一屏：  四级 | 六级
 *            ↓ 点四级
 * 第二屏：  作文 | 翻译        ← 四级/六级按钮从这里消失
 *            ↓ 点作文
 * 第三屏：  四级-作文 评分界面
 * ```
 *
 * 实现方式：**每次只渲染当前屏的内容**（`when (step)` 分支），
 * 上一屏的按钮根本不在 Composition 里，天然不可能"叠加"。
 * 这是最可靠的"替换"语义 —— 不依赖 zIndex、不依赖显隐动画。
 *
 * 返回：
 *  - 第三屏 → 第二屏（级别保留）
 *  - 第二屏 → 第一屏（级别清空）
 *  - 第一屏 → 交给宿主（关闭页面）
 *
 * 普通模式与专业模式**共存于同一个 App**，由用户在本页顶部切换。
 * 专业模式需要用户自行配置 API Key；未配置时面板会引导去设置。
 */
@Composable
fun AiAssistantScreen(
    uiState: AiAssistantUiState,
    onLevelSelected: (ExamType) -> Unit,
    onTaskSelected: (QuestionType) -> Unit,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    onClearAnswer: () -> Unit,
    onModeChange: (AssistMode) -> Unit,
    onCopyPrompt: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState = rememberHazeState(),
    proSlot: @Composable () -> Unit = {},
) {
    GlassBackground(hazeState = hazeState, darkTheme = LocalIsDarkTheme.current) {
        Box(modifier = modifier.fillMaxSize()) {
            // ★ 只渲染当前屏 —— 替换式导航的核心
            when (val step = uiState.step) {
                is AssistantStep.LevelSelect -> LevelSelectScreen(
                    hazeState = hazeState,
                    onLevelSelected = onLevelSelected,
                    onBack = onBack,
                )

                is AssistantStep.TaskSelect -> TaskSelectScreen(
                    hazeState = hazeState,
                    examType = uiState.examType,
                    onTaskSelected = onTaskSelected,
                    onBack = onBack,
                )

                is AssistantStep.Scoring -> ScoringScreen(
                    hazeState = hazeState,
                    uiState = uiState,
                    step = step,
                    onQuestionChange = onQuestionChange,
                    onAnswerChange = onAnswerChange,
                    onClearAnswer = onClearAnswer,
                    onModeChange = onModeChange,
                    onCopyPrompt = onCopyPrompt,
                    onBack = onBack,
                    proSlot = proSlot,
                )
            }
        }
    }
}

/** 简易 TopBar */
@Composable
private fun SimpleTopBar(
    title: String,
    subtitle: String?,
    hazeState: HazeState,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ============================================================
// 第一屏：级别选择
// ============================================================

@Composable
private fun LevelSelectScreen(
    hazeState: HazeState,
    onLevelSelected: (ExamType) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
    ) {
        Spacer(Modifier.height(Dimens.SpaceM))

        SimpleTopBar(
            title = "AI 评分助手",
            subtitle = "选择考试级别",
            hazeState = hazeState,
            onBack = onBack,
        )

        Spacer(Modifier.height(Dimens.SpaceXL))

        // 屏幕上只有两个按钮：四级 / 六级
        ExamType.entries.forEach { type ->
            GlassCard(
                hazeState = hazeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.CornerLarge))
                    .clickable { onLevelSelected(type) },
                cornerRadius = Dimens.CornerLarge,
                blurRadius = 24.dp,
                elevation = 10.dp,
                contentPadding = PaddingValues(Dimens.SpaceXL),
            ) {
                Text(
                    text = type.shortLabel,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Dimens.SpaceXS))
                Text(
                    text = "710 分制 · 作文与翻译 15 分制评分",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Dimens.SpaceL))
        Text(
            text = "选择后将进入题型选择，本页按钮会消失",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

// ============================================================
// 第二屏：题型选择
// ============================================================

@Composable
private fun TaskSelectScreen(
    hazeState: HazeState,
    examType: ExamType?,
    onTaskSelected: (QuestionType) -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
    ) {
        Spacer(Modifier.height(Dimens.SpaceM))

        SimpleTopBar(
            title = examType?.shortLabel ?: "选择题型",
            subtitle = "选择题型",
            hazeState = hazeState,
            onBack = onBack,
        )

        Spacer(Modifier.height(Dimens.SpaceXL))

        // ★ 这一屏只有作文 / 翻译两个按钮，四级/六级按钮已不在 Composition 中
        QuestionType.entries.forEach { task ->
            GlassCard(
                hazeState = hazeState,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(Dimens.CornerLarge))
                    .clickable { onTaskSelected(task) },
                cornerRadius = Dimens.CornerLarge,
                blurRadius = 24.dp,
                elevation = 10.dp,
                contentPadding = PaddingValues(Dimens.SpaceXL),
            ) {
                Text(
                    text = task.label,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Dimens.SpaceXS))
                Text(
                    text = when (task) {
                        QuestionType.WRITING ->
                            "${examType?.shortLabel ?: ""} · 120/150 words to 180/200 words · 含档位标准与范文"

                        QuestionType.TRANSLATION ->
                            "中译英 · 含信达雅维度与常见扣分点清单"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Dimens.SpaceL))
        Text(
            text = "级别已记录为 ${examType?.shortLabel ?: "—"}，返回上一屏会清空",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}

// ============================================================
// 第三屏：评分界面
// ============================================================

@Composable
private fun ScoringScreen(
    hazeState: HazeState,
    uiState: AiAssistantUiState,
    step: AssistantStep.Scoring,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    onClearAnswer: () -> Unit,
    onModeChange: (AssistMode) -> Unit,
    onCopyPrompt: (String) -> Unit,
    onBack: () -> Unit,
    proSlot: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
    ) {
        Spacer(Modifier.height(Dimens.SpaceM))

        SimpleTopBar(
            title = uiState.scoringTitle,
            subtitle = "粘贴原题目与作答，生成评分提示词",
            hazeState = hazeState,
            onBack = onBack,
        )

        // ---------- 模式切换（仅在 online 版显示专业模式）----------
        ModeSelector(
            hazeState = hazeState,
            current = uiState.mode,
            onSelect = onModeChange,
        )

        // ---------- 原题目输入 ----------
        InputCard(
            hazeState = hazeState,
            title = if (step.questionType == QuestionType.WRITING) "原题目" else "中文原文（翻译题）",
            hint = if (step.questionType == QuestionType.WRITING) {
                "粘贴作文题目要求（选填）"
            } else {
                "粘贴待翻译的中文原文（选填，中译英）"
            },
            value = uiState.question,
            onValueChange = onQuestionChange,
            minHeight = 100,
            warning = when {
                uiState.isQuestionTooLong -> "原题目过长，生成时会自动截断"
                uiState.shouldWarnQuestion -> "未填原题目，AI 可能无法判断切题度"
                else -> null
            },
        )

        // ---------- 作答输入 ----------
        InputCard(
            hazeState = hazeState,
            title = if (step.questionType == QuestionType.WRITING) "我的作文" else "我的译文",
            hint = if (step.questionType == QuestionType.WRITING) {
                "粘贴你的作文（选填，留空表示未作答）"
            } else {
                "粘贴你的译文（选填，留空表示未作答）"
            },
            value = uiState.answer,
            onValueChange = onAnswerChange,
            minHeight = 180,
            warning = null,
        )

        // ---------- 底部词数提示（仅作文显示，翻译不显示）----------
        uiState.lengthHint?.let { hint ->
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = Dimens.SpaceS),
            )
        }

        // ---------- 模式对应的操作区 ----------
        if (uiState.mode == AssistMode.NORMAL) {
            NormalModePanel(
                hazeState = hazeState,
                templateBody = uiState.templateBody,
                canSubmit = uiState.canSubmit,
                hasAnswer = !uiState.isAnswerEmpty,
                onCopyPrompt = onCopyPrompt,
                onClearAnswer = onClearAnswer,
            )
        } else {
            // 专业模式由 online flavor 提供实现；offline 下不会走到这里
            proSlot()
        }

        Spacer(Modifier.height(Dimens.SpaceXL))
    }
}

/** 模式切换分段控件 */
@Composable
private fun ModeSelector(
    hazeState: HazeState,
    current: AssistMode,
    onSelect: (AssistMode) -> Unit,
) {
    // 两种模式始终都在，由用户切换
    val modes = AssistMode.entries.toList()

    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
            modes.forEach { mode ->
                val selected = mode == current
                GlassCard(
                    hazeState = hazeState,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(Dimens.CornerSmall))
                        .clickable { onSelect(mode) },
                    cornerRadius = Dimens.CornerSmall,
                    blurRadius = 14.dp,
                    tintAlpha = if (selected) 0.52f else 0.24f,
                    borderAlpha = if (selected) 0.78f else 0.28f,
                    elevation = if (selected) 6.dp else 2.dp,
                    contentPadding = PaddingValues(vertical = 10.dp),
                ) {
                    Text(
                        text = mode.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = modes.first { it == current }.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

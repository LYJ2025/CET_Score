package com.cetscore.feature.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.cetscore.core.ui.AppCopy
import com.cetscore.core.ui.component.GlassBackground
import com.cetscore.core.ui.component.GlassCard
import com.cetscore.core.ui.component.GlassEntryCard
import com.cetscore.core.ui.theme.CetScoreTheme
import com.cetscore.core.ui.theme.Dimens
import com.cetscore.core.ui.theme.LocalIsDarkTheme
import com.cetscore.score.domain.model.ExamType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState

/**
 * 首页 —— 两个考试入口 + 历史/趋势入口。
 */
@Composable
fun HomeScreen(
    onExamSelected: (ExamType) -> Unit,
    onHistoryClick: () -> Unit = {},
    onTrendClick: () -> Unit = {},
    onAiAssistantClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val hazeState = rememberHazeState()

    GlassBackground(hazeState = hazeState, darkTheme = LocalIsDarkTheme.current) {
        Box(modifier = modifier.fillMaxSize()) {
            HomeContent(
                hazeState = hazeState,
                onExamSelected = onExamSelected,
                onHistoryClick = onHistoryClick,
                onTrendClick = onTrendClick,
                onAiAssistantClick = onAiAssistantClick,
            )
        }
    }
}

@Composable
private fun HomeContent(
    hazeState: HazeState,
    onExamSelected: (ExamType) -> Unit,
    onHistoryClick: () -> Unit,
    onTrendClick: () -> Unit,
    onAiAssistantClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Dimens.ScreenPadding),
        verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL),
    ) {
        Spacer(Modifier.height(Dimens.SpaceM))

        // ---------- 顶部标题 ----------
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceXS)) {
            Text(
                text = "四六级估分助手",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "本地估分 · 数据仅存手机",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---------- 考试入口 ----------
        GlassEntryCard(
            hazeState = hazeState,
            title = "四级估分",
            subtitle = "710 分制 · 逐题自评",
            badge = "CET-4",
            onClick = { onExamSelected(ExamType.CET4) },
        )

        GlassEntryCard(
            hazeState = hazeState,
            title = "六级估分",
            subtitle = "710 分制 · 逐题自评",
            badge = "CET-6",
            onClick = { onExamSelected(ExamType.CET6) },
        )

        // ---------- AI 评分助手 ----------
        GlassEntryCard(
            hazeState = hazeState,
            title = "AI 评分助手",
            subtitle = "复制提示词，用 AI 精评作文",
            badge = "OFFLINE",
            onClick = onAiAssistantClick,
        )

        // ---------- 历史 / 趋势 ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
        ) {
            NavTile(
                hazeState = hazeState,
                label = "历史记录",
                desc = "查看过往成绩",
                modifier = Modifier.weight(1f),
                onClick = onHistoryClick,
            )
            NavTile(
                hazeState = hazeState,
                label = "成绩趋势",
                desc = "折线图分析",
                modifier = Modifier.weight(1f),
                onClick = onTrendClick,
            )
        }

        // ---------- 免责声明 ----------
        GlassCard(hazeState = hazeState, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = AppCopy.DISCLAIMER,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(Dimens.SpaceL))
    }
}

@Composable
private fun NavTile(
    hazeState: HazeState,
    label: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    GlassCard(
        hazeState = hazeState,
        modifier = modifier,
        cornerRadius = Dimens.CornerMedium,
        blurRadius = 20.dp,
        elevation = 6.dp,
        contentPadding = PaddingValues(Dimens.SpaceL),
        onClick = onClick,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Dimens.SpaceXS))
        Text(
            text = desc,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenPreview() {
    CetScoreTheme(darkTheme = false) {
        HomeScreen(onExamSelected = {})
    }
}

@Preview(showBackground = true, widthDp = 400, heightDp = 860)
@Composable
private fun HomeScreenDarkPreview() {
    CetScoreTheme(darkTheme = true) {
        HomeScreen(onExamSelected = {})
    }
}

package com.cetsix.feature.trend

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cetsix.core.ui.component.GlassBackground
import com.cetsix.core.ui.component.GlassCard
import com.cetsix.core.ui.theme.Dimens
import com.cetsix.core.ui.theme.LocalIsDarkTheme
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState

/**
 * 趋势图页。
 *
 * 展示历次估分总分变化折线图，含 425 及格参考线。
 * 点击数据点弹出详情面板，完整备注只在面板内展示（不进入图表坐标系）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendScreen(
    uiState: TrendUiState,
    onFilterChange: (TrendFilter) -> Unit,
    onPointClick: (Int) -> Unit,
    onCloseDetail: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState = rememberHazeState(),
) {
    val dark = LocalIsDarkTheme.current
    val sheetState = rememberModalBottomSheetState()

    GlassBackground(hazeState = hazeState, darkTheme = dark) {
        Box(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                Spacer(Modifier.height(Dimens.SpaceM))

                Text(
                    text = "成绩趋势",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(Dimens.SpaceM))

                // ---------- 筛选切换 ----------
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                    TrendFilter.entries.forEach { filter ->
                        val isSelected = filter == uiState.filter
                        GlassCard(
                            hazeState = hazeState,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(Dimens.CornerSmall))
                                .clickable { onFilterChange(filter) },
                            cornerRadius = Dimens.CornerSmall,
                            blurRadius = 12.dp,
                            tintAlpha = if (isSelected) 0.50f else 0.24f,
                            borderAlpha = if (isSelected) 0.75f else 0.28f,
                            elevation = if (isSelected) 6.dp else 2.dp,
                            contentPadding = PaddingValues(vertical = 10.dp),
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Dimens.SpaceM))

                when {
                    uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text("加载中…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    uiState.isEmpty -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "还没有估分记录",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(Modifier.height(Dimens.SpaceXS))
                            Text(
                                text = "保存记录后即可查看趋势",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    else -> {
                        // ---------- 折线图 ----------
                        GlassCard(
                            hazeState = hazeState,
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = Dimens.CornerMedium,
                            blurRadius = 22.dp,
                            elevation = 8.dp,
                            contentPadding = PaddingValues(
                                start = Dimens.SpaceS,
                                end = Dimens.SpaceM,
                                top = Dimens.SpaceL,
                                bottom = Dimens.SpaceM,
                            ),
                        ) {
                            Column {
                                // Y 轴刻度（叠加在图表左侧）
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .align(Alignment.CenterStart)
                                            .padding(start = Dimens.SpaceS),
                                        verticalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        listOf("710", "533", "355", "178", "0").forEach {
                                            Text(
                                                text = it,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    .copy(alpha = 0.55f),
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(Dimens.SpaceS))

                                LineChartWithNotes(
                                    records = uiState.records,
                                    onPointClick = onPointClick,
                                    modifier = Modifier.fillMaxWidth(),
                                )

                                Spacer(Modifier.height(Dimens.SpaceS))

                                // 图例说明
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
                                ) {
                                    // 及格线说明
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 14.dp, height = 2.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    Color(0xFFC98A6A)
                                                ),
                                        )
                                        Spacer(Modifier.size(4.dp))
                                        Text(
                                            text = "425 及格线",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }

                                    // 备注标记说明
                                    if (uiState.hasAnyNote) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        Color(0xFFC9A87A)
                                                    ),
                                            )
                                            Spacer(Modifier.size(4.dp))
                                            Text(
                                                text = "有备注",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(Dimens.SpaceXS))
                                Text(
                                    text = "点击数据点查看该次估分详情",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ---------- 点击数据点后弹出的详情面板 ----------
    val record = uiState.selectedRecord
    if (record != null) {
        ModalBottomSheet(
            onDismissRequest = onCloseDetail,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            RecordDetailBottomSheet(
                record = record,
                onClose = onCloseDetail,
                onDelete = onDeleteSelected,
            )
        }
    }
}

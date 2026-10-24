package com.cetscore.feature.aiassistant.pro

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cetscore.core.ui.component.GlassCard
import com.cetscore.core.ui.theme.Dimens
import com.cetscore.score.domain.model.PromptAssembler
import dev.chrisbanes.haze.HazeState

/**
 * 专业模式面板 —— **仅在 online flavor 编译**。
 *
 * 界面要求（严格遵守）：
 *  - 上方：原题目 + 我的作答 + 「开始 AI 评分」按钮
 *  - 下方：AI 思考过程 / 流式输出窗口（可滚动、自动吸底）
 *  - 隐藏：复制提示词按钮、提示词预览、所有多余 UI
 */
@Composable
fun ProScoringPanel(
    hazeState: HazeState,
    bridge: ProApiBridgeImpl,
    templateBody: String,
    question: String,
    answer: String,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
) {
    var showSettings by remember { mutableStateOf(false) }
    var showConsent by remember { mutableStateOf(false) }
    var pendingPrompt by remember { mutableStateOf("") }

    val state by bridge.state.collectAsStateWithLifecycle()

    // 首次使用专业模式：明确告知「作答将发送至第三方 API」
    if (showConsent) {
        ConsentDialog(
            onConfirm = {
                showConsent = false
                bridge.startScoring(pendingPrompt,
                    onStage = {}, onDelta = {},
                    onDone = {},
                )
            },
            onDismiss = { showConsent = false },
        )
    }

    // 配置页
    if (showSettings) {
        ApiSettingsDialog(
            config = bridge.configStore(),
            onDismiss = { showSettings = false },
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)) {

        // ---------- 未配置提示 ----------
        if (!bridge.isConfigured) {
            ConfigNotice(
                hazeState = hazeState,
                message = bridge.validate() ?: "请先配置 API Key",
                onOpenSettings = { showSettings = true },
            )
        }

        // ---------- 上方：输入 + 开始按钮 ----------
        GlassCard(
            hazeState = hazeState,
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = Dimens.CornerMedium,
            blurRadius = 22.dp,
            elevation = 8.dp,
            contentPadding = PaddingValues(Dimens.SpaceL),
        ) {
            Text(
                text = "原题目",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = question,
                onValueChange = onQuestionChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("选填，不填则 AI 无法判断切题度") },
                shape = RoundedCornerShape(Dimens.CornerSmall),
                maxLines = 3,
            )

            Spacer(Modifier.height(Dimens.SpaceS))

            Text(
                text = "我的作答",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = answer,
                onValueChange = onAnswerChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("粘贴作文或译文") },
                shape = RoundedCornerShape(Dimens.CornerSmall),
                maxLines = 6,
            )

            Spacer(Modifier.height(Dimens.SpaceM))

            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
                // 三个条件缺一不可：有作答、已配置 API、模板已加载
                val canStart = answer.isNotBlank() &&
                    bridge.isConfigured &&
                    templateBody.isNotEmpty()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(Dimens.CornerMedium))
                        .background(
                            if (canStart) {
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                                    )
                                )
                            } else {
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f),
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f),
                                    )
                                )
                            }
                        )
                        .clickable(
                            enabled = canStart,
                        ) {
                            // 首次使用先弹隐私告知
                            // 必须走 PromptAssembler：只发模板会导致 AI 收不到
                            // 题目与作答，回「你尚未提供原文和译文」
                            pendingPrompt = PromptAssembler.assemble(
                                templateBody = templateBody,
                                question = question,
                                answer = answer,
                            )
                            showConsent = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            state is ProScoringState.Running -> "评分中…"
                            else -> "开始 AI 评分"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (canStart) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .clip(RoundedCornerShape(Dimens.CornerMedium))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        .clickable { showSettings = true }
                        .padding(horizontal = Dimens.SpaceM),
                    contentAlignment = Alignment.Center,
                ) {
                    // 显示当前使用的 AI 名称，切换后一眼可见
                    Text(
                        text = bridge.configStore().activeProfile()?.displayName ?: "设置",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 92.dp),
                    )
                }
            }
        }

        // ---------- 下方：流式输出窗口 ----------
        StreamingOutputWindow(hazeState = hazeState, state = state)
    }
}

/** 流式输出窗口：可滚动、自动吸底、完成后高亮分数 */
@Composable
private fun StreamingOutputWindow(hazeState: HazeState, state: ProScoringState) {
    val scrollState = rememberScrollState()

    // 自动吸底
    LaunchedEffect(state) {
        val len = (state as? ProScoringState.Running)?.output?.length
            ?: (state as? ProScoringState.Done)?.text?.length
            ?: (state as? ProScoringState.Failed)?.partialText?.length
            ?: 0
        if (len > 0) scrollState.animateScrollTo(scrollState.maxValue + 2000)
    }

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
                text = "AI 评分过程",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            when (state) {
                is ProScoringState.Running -> Text(
                    text = state.stage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )

                is ProScoringState.Done -> Text(
                    text = "已完成",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF6BA88B),
                )

                is ProScoringState.Failed -> Text(
                    text = "已中断",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFC98A6A),
                )

                ProScoringState.Idle -> Text(
                    text = "待开始",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Dimens.SpaceS))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp, max = 400.dp)
                .clip(RoundedCornerShape(Dimens.CornerSmall))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .verticalScroll(scrollState)
                .padding(Dimens.SpaceM),
        ) {
            val text = when (state) {
                is ProScoringState.Running -> state.output.ifEmpty {
                    "（等待 AI 输出…）"
                }

                is ProScoringState.Done -> state.text
                is ProScoringState.Failed -> state.partialText.ifEmpty {
                    "（无输出内容）"
                }

                ProScoringState.Idle -> "点击上方「开始 AI 评分」后，AI 的分析过程会逐字显示在这里"
            }

            Column {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // 完成后高亮「最终得分」
                if (state is ProScoringState.Done) {
                    SseParser.extractScore(state.text)?.let { highlight ->
                        Spacer(Modifier.height(Dimens.SpaceM))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(Dimens.CornerSmall))
                                .background(Color(0xFF6BA88B).copy(alpha = 0.15f))
                                .padding(Dimens.SpaceM),
                        ) {
                            Text(
                                text = highlight,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF4A8A6A),
                            )
                        }
                    }
                }

                // 失败时保留已输出内容，并给出重试提示
                if (state is ProScoringState.Failed) {
                    Spacer(Modifier.height(Dimens.SpaceM))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(Dimens.CornerSmall))
                            .background(Color(0xFFC98A6A).copy(alpha = 0.12f))
                            .padding(Dimens.SpaceM),
                    ) {
                        Text(
                            text = "${state.message}\n已保留上述输出内容，可重试或换个模型",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFC98A6A),
                        )
                    }
                }
            }
        }
    }
}

/** 未配置 API 的提示条 */
@Composable
private fun ConfigNotice(
    hazeState: HazeState,
    message: String,
    onOpenSettings: () -> Unit,
) {
    GlassCard(
        hazeState = hazeState,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenSettings),
        cornerRadius = Dimens.CornerSmall,
        blurRadius = 14.dp,
        tintAlpha = 0.45f,
        elevation = 4.dp,
        contentPadding = PaddingValues(Dimens.SpaceM),
    ) {
        Text(
            text = "$message（点击前往设置）",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** 首次使用专业模式的隐私告知弹窗 */
@Composable
private fun ConsentDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("数据将发送至第三方 API", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                "你即将使用专业模式。\n\n" +
                    "你的原题目与作答内容将被发送到你配置的 AI 服务商" +
                    "（如 DeepSeek、OpenAI 等）进行处理，\n" +
                    "评分结果也会由该服务商返回。\n\n" +
                    "App 本身不存储你的 API Key 到云端，" +
                    "但第三方服务的数据处理规则由其自行负责。\n\n" +
                    "是否继续？",
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("我已了解，继续")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(Dimens.CornerMedium),
    )
}

/**
 * AI 配置弹窗 —— 最多保存 [ApiConfigStore.MAX_PROFILES] 套，可随时切换。
 *
 * 顶部是已保存配置的标签，点击即可切换；下方编辑当前这一套的四个字段。
 * 切换标签时字段会自动重新加载（用 editingSlot 作为 remember 的 key）。
 */
@Composable
private fun ApiSettingsDialog(config: ApiConfigStore, onDismiss: () -> Unit) {
    var profiles by remember { mutableStateOf(config.profiles()) }
    var editingSlot by remember {
        mutableStateOf(
            config.activeSlot().takeIf { it >= 0 }
                ?: config.profiles().firstOrNull()?.slot
                ?: 0
        )
    }
    // 删除 / 新增时递增，强制重载输入框内容（槽位号可能没变）
    var resetToken by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    val saved = config.profiles().firstOrNull { it.slot == editingSlot }

    var name by remember(editingSlot, resetToken) { mutableStateOf(saved?.name.orEmpty()) }
    var apiKey by remember(editingSlot, resetToken) { mutableStateOf(saved?.apiKey.orEmpty()) }
    var baseUrl by remember(editingSlot, resetToken) {
        mutableStateOf(saved?.baseUrl?.ifBlank { ApiConfigStore.DEFAULT_BASE_URL } ?: ApiConfigStore.DEFAULT_BASE_URL)
    }
    var model by remember(editingSlot, resetToken) {
        mutableStateOf(saved?.model?.ifBlank { ApiConfigStore.DEFAULT_MODEL } ?: ApiConfigStore.DEFAULT_MODEL)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI 设置", style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            ) {
                Text(
                    text = "支持任何 OpenAI 兼容接口。最多保存 " +
                        "${ApiConfigStore.MAX_PROFILES} 套，随时切换用哪个改卷。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // ---------- 配置切换 ----------
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    profiles.forEach { p ->
                        SlotChip(
                            label = p.displayName,
                            selected = p.slot == editingSlot,
                            modifier = Modifier.weight(1f),
                            onClick = { editingSlot = p.slot; error = null },
                        )
                    }
                    if (profiles.size < ApiConfigStore.MAX_PROFILES) {
                        SlotChip(
                            label = "＋ 新增",
                            selected = saved == null,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                val empty = config.firstEmptySlot()
                                if (empty >= 0) {
                                    resetToken++
                                    editingSlot = empty
                                    error = null
                                }
                            },
                        )
                    }
                }

                if (profiles.isEmpty()) {
                    Text(
                        text = "还没有配置，填好下面四项后点「保存」即可。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("配置名称（选填）") },
                    placeholder = { Text("如：DeepSeek 官方") },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.CornerSmall),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.CornerSmall),
                )
                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it },
                    label = { Text("Base URL") },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.CornerSmall),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("模型名") },
                    singleLine = true,
                    shape = RoundedCornerShape(Dimens.CornerSmall),
                )

                error?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                // ---------- 删除当前配置 ----------
                if (saved != null) {
                    TextButton(
                        onClick = {
                            config.deleteSlot(editingSlot)
                            profiles = config.profiles()
                            resetToken++
                            editingSlot = profiles.firstOrNull()?.slot ?: 0
                            error = null
                        }
                    ) {
                        Text(
                            text = "删除这套配置",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val profile = AiProfile(
                    slot = editingSlot,
                    name = name,
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    model = model,
                )
                if (!profile.isComplete) {
                    error = "API Key、Base URL、模型名都要填"
                    return@TextButton
                }
                if (!config.save(profile)) {
                    error = "最多只能保存 ${ApiConfigStore.MAX_PROFILES} 套配置"
                    return@TextButton
                }
                // 保存后立即设为当前使用
                config.selectSlot(editingSlot)
                profiles = config.profiles()
                error = null
                onDismiss()
            }) {
                Text("保存并使用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
        shape = RoundedCornerShape(Dimens.CornerMedium),
    )
}

/** 配置切换标签 */
@Composable
private fun SlotChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.CornerSmall))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


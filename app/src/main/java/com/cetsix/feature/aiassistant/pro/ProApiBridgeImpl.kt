package com.cetsix.feature.aiassistant.pro

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 专业模式评分状态。
 */
sealed interface ProScoringState {
    /** 空闲 */
    data object Idle : ProScoringState

    /** 进行中（含阶段提示与已输出文本） */
    data class Running(
        val stage: String = "",
        val output: String = "",
    ) : ProScoringState

    /** 已完成 */
    data class Done(val text: String) : ProScoringState

    /** 失败（保留已输出内容供查看） */
    data class Failed(
        val message: String,
        val partialText: String,
    ) : ProScoringState
}

/**
 * 专业模式桥接实现。
 *
 * 用 StateFlow 暴露评分状态，UI 直接 collect，
 * 避免把回调层层透传。整个 App 内单例，保证流式输出不因重组中断。
 */
class ProApiBridgeImpl(
    context: Context,
    private val scope: CoroutineScope,
) {

    private val config = ApiConfigStore(context.applicationContext)
    private val client = ScoringApiClient(config)

    private val _state = MutableStateFlow<ProScoringState>(ProScoringState.Idle)
    val state: StateFlow<ProScoringState> = _state.asStateFlow()

    private var job: Job? = null

    val isConfigured: Boolean get() = config.isConfigured

    /** 配置校验结果；null 表示配置正常 */
    fun validate(): String? = config.validate()

    fun startScoring(
        prompt: String,
        onStage: (String) -> Unit,
        onDelta: (String) -> Unit,
        onDone: (Result<String>) -> Unit,
    ) {
        if (job?.isActive == true) return

        job = scope.launch {
            client.streamScore(
                prompt = prompt,
                onStage = { stage ->
                    _state.value = (_state.value as? ProScoringState.Running
                        ?: ProScoringState.Running()).copy(stage = stage)
                    onStage(stage)
                },
                onDelta = { delta ->
                    val running = _state.value as? ProScoringState.Running
                        ?: ProScoringState.Running()
                    _state.value = running.copy(output = running.output + delta)
                    onDelta(delta)
                },
                onDone = { result ->
                    result.onSuccess { text ->
                        _state.value = ProScoringState.Done(text)
                    }.onFailure { e ->
                        // 流式中断时保留已输出内容，方便判断在哪断的
                        val partial = (_state.value as? ProScoringState.Running)?.output.orEmpty()
                        _state.value = ProScoringState.Failed(
                            message = e.message ?: "评分失败",
                            partialText = partial,
                        )
                    }
                    onDone(result)
                },
            )
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        // 取消时保留已输出内容
        val partial = (_state.value as? ProScoringState.Running)?.output.orEmpty()
        _state.value = if (partial.isEmpty()) {
            ProScoringState.Idle
        } else {
            ProScoringState.Failed("已取消", partial)
        }
    }

    /** 重置状态 */
    fun reset() {
        job?.cancel()
        job = null
        _state.value = ProScoringState.Idle
    }

    /** 供设置页读写配置 */
    fun configStore(): ApiConfigStore = config
}

package com.cetscore.core.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cetscore.core.ui.theme.LocalIsDarkTheme
import com.cetscore.score.domain.model.StepperStateMachine
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 数字步进器：`- [数字] +`
 *
 * 用于输入"答对题数"。行为规范：
 *  1. **单击**（1 秒内松开）：点一下加 1 / 减 1
 *  2. **长按**（持续 ≥1 秒）：进入快速模式，每 100ms 自动加/减 1
 *  3. 松手 / 触摸取消 / 指针移出：立即停止，无需清理（无定时器存活）
 *  4. **单击与长按互斥**：长按触发后，松开时不会再额外加/减 1
 *  5. 下限 0，不可为负
 *  6. 长按快速模式有视觉反馈（放大）
 *  7. 快速连点：每次点击独立 ±1，不会累积成"连击加速"
 *
 * ============================ 曾经的严重 bug ============================
 * **根因不是 setInterval，而是 Compose 里 pointerInput 的 key 变化会重启协程。**
 *
 * 错误写法：
 * ```kotlin
 * .pointerInput(enabled, value) {          // ← ① key 里带了 value
 *     detectTapGestures(onPress = {
 *         onValueChange(first)              // ← ② 立刻改 value
 *         val job = scope.launch { ... }    // ← ③ 在重组作用域启动
 *         tryAwaitRelease()
 *         job.cancel()                      // ← ④ 永远执行不到
 *     })
 * }
 * ```
 *
 * 故障链路：
 *  1. 按下瞬间 `onValueChange` 改掉 `value`
 *  2. `value` 是 pointerInput 的 key，**key 变化 → pointerInput 协程被立即取消重启**
 *  3. `tryAwaitRelease()` / `job.cancel()` 随协程一起死掉 → **清理逻辑永不执行**
 *  4. 但 `job` 启动在 `rememberCoroutineScope()`（重组作用域），不是 pointerInput 作用域
 *     → 成了**孤儿协程**，每 100ms 加一次直到顶到上限
 *
 * 现象即「点一下就疯狂自己加/减，停不下来」。
 *
 * ============================ 本文件的修复策略 ============================
 * 1. **pointerInput 的 key 只含 `enabled`**；数值通过 `rememberUpdatedState` 读取
 *    → 协程不会因数值变化被重启。
 *
 * 2. **完全不 spawn 长生命周期协程** —— 连发由 [StepperStateMachine] 在
 *    指针事件循环内部按时间戳驱动。手指抬起 → `break` → 连发自然停止，
 *    **没有任何需要清理的定时器**，从根上杜绝泄漏。
 *
 * 附：为什么这里不能简单地 `launch` 一个协程 ——
 * `PointerInputScope` 与 `AwaitPointerEventScope` **都不是** `CoroutineScope`
 * （只继承 `Density`）。`awaitEachGesture` 的 lambda 带 restricts-suspension，
 * 只能调用 receiver 为 `AwaitPointerEventScope` 的扩展函数及其成员，
 * 所以 `coroutineScope { }` / `launch { }` 在这里都用不了 —— 状态机方案正好绕开。
 * ============================================================================
 */
@Composable
fun NumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 100,
    step: Int = 1,
    enabled: Boolean = true,
    label: String = "答对题数",
) {
    // 按下与长按快速模式的视觉反馈状态
    var pressing by remember { mutableStateOf(false) }
    var fastMode by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (fastMode) 1.18f else if (pressing) 1.10f else 1f,
        label = "stepperScale",
    )

    val canDec = enabled && value > min
    val canInc = enabled && value < max

    Row(
        modifier = modifier.semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StepperButton(
            symbol = "−",
            isIncrement = false,
            value = value,
            enabled = canDec,
            scale = scale,
            min = min,
            max = max,
            step = step,
            onValueChange = onValueChange,
            onPressingChange = { pressing = it },
            onFastModeChange = { fastMode = it },
            contentDescription = "减少答对题数",
        )

        Box(
            modifier = Modifier.width(52.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "$value",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        StepperButton(
            symbol = "+",
            isIncrement = true,
            value = value,
            enabled = canInc,
            scale = scale,
            min = min,
            max = max,
            step = step,
            onValueChange = onValueChange,
            onPressingChange = { pressing = it },
            onFastModeChange = { fastMode = it },
            contentDescription = "增加答对题数",
        )
    }
}

/**
 * 单个 +/- 按钮。
 *
 * @param isIncrement true 为 +，false 为 −
 * @param onFastModeChange 进入长按快速模式时回调（用于视觉反馈）
 */
@Composable
private fun StepperButton(
    symbol: String,
    isIncrement: Boolean,
    value: Int,
    enabled: Boolean,
    scale: Float,
    min: Int,
    max: Int,
    step: Int,
    onValueChange: (Int) -> Unit,
    onPressingChange: (Boolean) -> Unit,
    onFastModeChange: (Boolean) -> Unit,
    contentDescription: String,
) {
    val dark = LocalIsDarkTheme.current
    val haptics = LocalHapticFeedback.current

    // ★ 用 rememberUpdatedState 持有最新值与回调，
    //   这样 pointerInput 的 key 就不必包含它们，协程也不会被重启。
    val currentValue by rememberUpdatedState(value)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFastMode by rememberUpdatedState(onFastModeChange)
    val currentOnPressing by rememberUpdatedState(onPressingChange)

    val delta = if (isIncrement) step else -step

    val bg = when {
        !enabled -> if (dark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.05f)
        else -> MaterialTheme.colorScheme.primary.copy(alpha = if (dark) 0.30f else 0.16f)
    }
    val fg = if (enabled) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Box(
        modifier = Modifier
            .size(42.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bg)
            // ★ key 只有 enabled —— 不含 value，也不含任何会随点击变化的量。
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput

                val slop = viewConfiguration.touchSlop
                val interval = StepperStateMachine.REPEAT_INTERVAL_MS

                awaitEachGesture {
                    // ---- 等待按下 ----
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()

                    // ---- 首次点击立即生效（单击就是这一步）----
                    val first = (currentValue + delta).coerceIn(min, max)
                    if (first != currentValue) currentOnValueChange(first)

                    currentOnPressing(true)
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                    // ---- 长按连发：由状态机在事件循环内驱动 ----
                    val machine = StepperStateMachine(
                        startValue = first,
                        delta = delta,
                        min = min,
                        max = max,
                    )

                    // elapsed 是喂给状态机的虚拟时钟，保证行为确定、可测
                    var elapsed = 0L
                    var lastRealAt = System.currentTimeMillis()
                    var fastModeShown = false

                    try {
                        while (true) {
                            machine.tick(elapsed)?.let { v ->
                                currentOnValueChange(v)
                                haptics.performHapticFeedback(
                                    HapticFeedbackType.TextHandleMove
                                )
                                if (!fastModeShown) {
                                    fastModeShown = true
                                    currentOnFastMode(true)   // 长按视觉反馈
                                }
                            }

                            // 等下一个指针事件；超时则推进虚拟时钟后重试。
                            // withTimeoutOrNull 是 AwaitPointerEventScope 的成员，可安全调用。
                            val event = withTimeoutOrNull(interval) { awaitPointerEvent() }

                            if (event == null) {
                                // 超时（手指按住不动）：推进虚拟时钟，继续出数
                                elapsed += interval
                                lastRealAt = System.currentTimeMillis()
                                continue
                            }

                            val change = event.changes.firstOrNull { it.id == down.id }
                            // 三种结束情况：指针消失 / 抬起 / 被取消
                            if (change == null) break
                            if (change.changedToUpIgnoreConsumed()) break
                            if (!change.pressed) break

                            // 指针移出按钮范围（鼠标按住拖走）
                            val s = size
                            if (s.width > 0 && s.height > 0) {
                                val p = change.position
                                val out = p.x < -slop || p.y < -slop ||
                                    p.x > s.width + slop || p.y > s.height + slop
                                if (out) break
                            }

                            // 有事件到达：按真实流逝时间推进虚拟时钟
                            val now = System.currentTimeMillis()
                            elapsed += (now - lastRealAt).coerceAtLeast(0L)
                            lastRealAt = now
                        }
                    } finally {
                        // ★ 松手即停止：仅复位 UI 状态，没有定时器需要清理。
                        //   单击与长按互斥 —— 松开时不会再额外加/减 1。
                        machine.onRelease()
                        currentOnFastMode(false)
                        currentOnPressing(false)
                    }
                }
            }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = symbol, style = MaterialTheme.typography.titleLarge, color = fg)
    }
}
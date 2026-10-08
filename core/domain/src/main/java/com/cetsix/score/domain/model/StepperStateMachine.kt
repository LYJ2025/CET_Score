package com.cetsix.score.domain.model

/**
 * 步进器长按连发状态机 —— **纯逻辑，无协程、无定时器、无 Android 依赖**。
 *
 * 存在的意义：把「什么时候该出下一个数」从 UI 手势代码里剥离出来，
 * 既能单测，也让 UI 侧不再需要 spawn 长生命周期协程（那正是原 bug 的根源）。
 *
 * 使用方式：在手指按住的循环里反复调用 [tick]，每次传入当前时间戳：
 * ```kotlin
 * val machine = StepperStateMachine(startValue = first, delta = 1, min = 0, max = 15)
 * while (手指未抬起) {
 *     machine.tick(now, lastEmitAt)?.let { emit(it) }
 *     // ...等待下一个事件或超时...
 * }
 * ```
 *
 * 状态转换：
 * ```
 *             press
 *               │
 *               ▼
 *         ┌───────────┐  经过 longPressDelay   ┌──────────────┐
 *         │  PENDING  │ ────────────────────► │ FAST_REPEAT  │
 *         │ （不发数） │                       │ （每 interval │
 *         └───────────┘                       │   发一个数）  │
 *               │                             └──────────────┘
 *        release/cancel                                   │
 *               └─────────────────────────────────────────┘
 *                             停止（不再出数）
 * ```
 *
 * 关键性质：
 *  - 单击（未达长按阈值就抬起）→ 全程不发数，只有效力 UI 层那一次 ±1
 *  - 长按成立后每次 [tick] 最多发一个数，**不会一次 tick 连发多个**
 *  - 到达 min/max 边界后进入 [isSettled]，不再出数（天然防越界）
 *  - 无定时器、无协程 → **不存在需要清理的资源**，松手即停
 */
class StepperStateMachine(
    private val startValue: Int,
    private val delta: Int,
    private val min: Int,
    private val max: Int,
    private val longPressDelayMs: Long = LONG_PRESS_DELAY_MS,
    private val intervalMs: Long = REPEAT_INTERVAL_MS,
) {
    enum class State {
        /** 已按下但未达长按阈值，不发数 */
        PENDING,

        /** 已进入快速连发 */
        FAST_REPEAT,

        /** 已到达边界，无法继续 */
        SETTLED,
    }

    /** 当前状态 */
    var state: State = State.PENDING
        private set

    /** 当前值（含所有已发出的增量） */
    var currentValue: Int = startValue
        private set

    /**
     * 上一次发数的时间戳，由状态机**自己维护**。
     *
     * 关键：不能依赖调用方传回的 `lastEmitAt` 来做去重 ——
     * 若调用方传错或漏更新，同一时间戳会被反复判定为"已过间隔"，
     * 导致一次 tick 调用连发多个数（快速点击时数字会跳着涨）。
     * 自己记时间戳是唯一可靠做法。
     */
    private var lastEmitAt: Long = Long.MIN_VALUE

    /** 是否已到达边界 */
    val isSettled: Boolean get() = state == State.SETTLED

    /** 是否已进入长按快速模式 */
    val isFastMode: Boolean get() = state == State.FAST_REPEAT

    /**
     * 推进一次。
     *
     * @param now 当前时间戳（毫秒）
     * @return 本次应输出的新值；不需要输出时返回 null
     */
    fun tick(now: Long): Int? {
        when (state) {
            State.PENDING -> {
                // 长按判定未通过：一个数都不发。
                // 以"按下时刻"为基准，因此首次 tick 传入的就是 pressStart。
                if (lastEmitAt == Long.MIN_VALUE) {
                    lastEmitAt = now
                    return null
                }
                if (now - lastEmitAt < longPressDelayMs) return null

                // 长按成立
                state = State.FAST_REPEAT
                // 进入快速模式这一刻本身不发数，等下一个间隔再发，
                // 这样"进入快速模式"与"开始出数"分离，节奏更自然。
                return null
            }

            State.FAST_REPEAT -> {
                // 距上次出数不足一个间隔，不发。
                // ★ 用状态机自己记录的时间戳判断，保证同一时刻只发一个。
                if (now - lastEmitAt < intervalMs) return null

                val next = (currentValue + delta).coerceIn(min, max)
                if (next == currentValue) {
                    // 触顶或触底：收敛，不再出数
                    state = State.SETTLED
                    return null
                }
                currentValue = next
                lastEmitAt = now
                return next
            }

            State.SETTLED -> return null
        }
    }

    /** 用户松开手指 */
    fun onRelease() {
        if (state != State.SETTLED) state = State.SETTLED
    }

    companion object {
        /** 长按判定时长：1 秒 */
        const val LONG_PRESS_DELAY_MS = 1_000L

        /** 快速模式连发间隔：100ms */
        const val REPEAT_INTERVAL_MS = 100L
    }
}
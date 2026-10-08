package com.cetscore.score.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 步进器长按连发状态机测试 —— 对「点一下就疯狂自增」bug 的回归防护。
 *
 * 纯逻辑测试，无需协程与模拟器，毫秒级完成。
 */
class StepperStateMachineTest {

    private fun machine(
        start: Int = 0,
        delta: Int = 1,
        min: Int = 0,
        max: Int = 100,
    ) = StepperStateMachine(start, delta, min, max)

    /** 模拟按住 [durationMs] 毫秒，每 [intervalMs] 调一次 tick，返回发出的值 */
    private fun holdPress(
        m: StepperStateMachine,
        durationMs: Long,
        intervalMs: Long = 50,
    ): List<Int> {
        val out = mutableListOf<Int>()
        var t = 0L
        while (t <= durationMs) {
            m.tick(t)?.let { out += it }
            t += intervalMs
        }
        return out
    }

    // ---------- 需求 1/2：单击与长按的时间边界 ----------

    @Test
    fun `长按阈值内不发任何数`() {
        val m = machine()
        val out = holdPress(m, durationMs = 900)   // 900ms < 1000ms
        assertTrue("长按未成立时不应发数，实际: $out", out.isEmpty())
    }

    @Test
    fun `超过1秒后开始发数`() {
        val m = machine(start = 0, delta = 1, max = 1000)
        val out = holdPress(m, durationMs = 1500, intervalMs = 50)
        assertTrue("超过阈值后应发数，实际: $out", out.isNotEmpty())
        assertEquals("首个发出的值应为 1", 1, out.first())
    }

    @Test
    fun `常量符合需求_长按1秒_间隔100毫秒`() {
        assertEquals(1_000L, StepperStateMachine.LONG_PRESS_DELAY_MS)
        assertEquals(100L, StepperStateMachine.REPEAT_INTERVAL_MS)
    }

    @Test
    fun `快速模式每100ms最多发一个数`() {
        val m = machine(start = 0, delta = 1, max = 1000)
        val out = mutableListOf<Long>()

        // 精确推进：每次 tick 都给足间隔
        var t = 0L
        while (t <= 1500) {
            m.tick(t)?.let { out += t }
            t += 100
        }
        // t=1000 时刚进入 FAST_REPEAT（不发数），t=1100 发第 1 个，之后每 100ms 一个
        assertEquals(listOf(1100L, 1200L, 1300L, 1400L, 1500L), out)
    }

    @Test
    fun `高频tick不会一次发多个数`() {
        val m = machine(start = 0, delta = 1, max = 1000)
        // 连续在同一个时间戳调用多次 tick，最多发一个
        var count = 0
        repeat(10) {
            if (m.tick(1_500) != null) count++
        }
        assertTrue("同一时间戳不应重复发数，实际发了 $count 个", count <= 1)
    }

    // ---------- 需求 3：松手即停（核心回归点）----------

    @Test
    fun `onRelease后不再发数`() {
        val m = machine(start = 0, delta = 1, max = 1000)
        var lastEmit = 0L
        repeat(30) { i ->
            val t = i * 100L
            m.tick(t)
        }
        val beforeRelease = m.currentValue
        assertTrue("释放前应已增加", beforeRelease > 0)

        // 松手
        m.onRelease()

        // 释放后无论推进多久都不再变化
        val after = m.currentValue
        assertNull("释放后不应再发数", m.tick(99_999))
        assertEquals(after, m.currentValue)
    }

    @Test
    fun `长时间按住也不会无限增长到超过上限`() {
        val m = machine(start = 0, delta = 1, min = 0, max = 15)
        // 模拟一直按住 10 分钟
        val out = holdPress(m, durationMs = 600_000, intervalMs = 100)
        assertEquals(15, out.max())          // 恰好停在 15
        assertEquals(15, m.currentValue)
        assertTrue(m.isSettled)
    }

    @Test
    fun `settled后tick不再返回任何值`() {
        val m = machine(start = 14, delta = 1, min = 0, max = 15)
        holdPress(m, durationMs = 3_000, intervalMs = 50)
        assertTrue(m.isSettled)
        assertNull(m.tick(100_000))
        assertNull(m.tick(100_000))
    }

    // ---------- 需求 4：单击与长按互斥 ----------

    @Test
    fun `短按全程不发数_单击只算UI那一次`() {
        val m = machine()
        // 300ms 后松手
        val out = holdPress(m, durationMs = 300, intervalMs = 50)
        m.onRelease()
        assertTrue("短按不应发数（单击的 ±1 由 UI 层负责），实际: $out", out.isEmpty())
    }

    @Test
    fun `长按触发后松手不再额外加一次`() {
        val m = machine(start = 0, delta = 1, max = 1000)
        var lastEmit = 0L
        var t = 0L
        while (t <= 1400) {
            m.tick(t)
            t += 100
        }
        val beforeRelease = m.currentValue

        m.onRelease()   // 松手

        // 松手后不应再有"补一次"的逻辑
        assertEquals(beforeRelease, m.currentValue)
    }

    // ---------- 需求 5：下限为 0，不可为负 ----------

    @Test
    fun `递减到下限0后停止_不出负数`() {
        val m = machine(start = 3, delta = -1, min = 0, max = 100)
        val out = holdPress(m, durationMs = 5_000, intervalMs = 50)
        assertEquals(listOf(2, 1, 0), out)
        assertEquals(0, m.currentValue)
        assertTrue("不应出现负数", out.all { it >= 0 })
    }

    @Test
    fun `下限0是硬边界`() {
        val m = machine(start = 0, delta = -1, min = 0, max = 100)
        val out = holdPress(m, durationMs = 5_000, intervalMs = 50)
        assertTrue(out.isEmpty())
        assertEquals(0, m.currentValue)
    }

    @Test
    fun `上限边界正确`() {
        val m = machine(start = 98, delta = 1, min = 0, max = 100)
        val out = holdPress(m, durationMs = 5_000, intervalMs = 50)
        assertEquals(listOf(99, 100), out)
        assertTrue("不应超过 100", out.all { it <= 100 })
    }

    @Test
    fun `递减连发节奏稳定`() {
        val m = machine(start = 10, delta = -1, min = 0, max = 100)
        val out = mutableListOf<Int>()
        var lastEmit = 0L
        var t = 0L
        while (t <= 1400) {
            m.tick(t)?.let { out += it }
            t += 100
        }
        // start=10，首次 ±1 已由 UI 做过 → 连发从 9 开始
        // t=1000 进入 FAST_REPEAT（不发数），t=1100/1200/1300/1400 各发一个
        assertEquals(listOf(9, 8, 7, 6), out)
    }

    // ---------- 需求 6：快速模式状态标记 ----------

    @Test
    fun `长按前后状态正确切换`() {
        val m = machine(max = 1000)

        // 按下未到阈值
        m.tick(0)
        assertEquals(StepperStateMachine.State.PENDING, m.state)
        assertFalse(m.isFastMode)

        // 到阈值，进入快速模式
        m.tick(1_000)
        assertEquals(StepperStateMachine.State.FAST_REPEAT, m.state)
        assertTrue(m.isFastMode)

        // 起始值已在上限：第一次 tick 就进入 settled（连发无法推进）
        val m2 = machine(start = 100, delta = 1, min = 0, max = 100)
        m2.tick(0)        // PENDING
        m2.tick(1_000)    // 进入 FAST_REPEAT
        m2.tick(1_100)    // 触顶 → SETTLED
        assertTrue(m2.isSettled)
        assertEquals(100, m2.currentValue)
    }

    // ---------- 需求 7：快速连点每次独立 ----------

    @Test
    fun `快速连点每次独立加1不累积加速`() {
        // 每次点击都是完整的独立手势：按下 → tick(0) → onRelease
        val out = mutableListOf<Int>()
        var value = 0

        repeat(5) {
            val m = StepperStateMachine(
                startValue = value + 1,   // UI 层先做了 +1
                delta = 1, min = 0, max = 100,
            )
            // 快速点击：按下后 200ms 就松手（远小于 1000ms 阈值）
            var lastEmit = 0L
            var t = 0L
            while (t <= 200) {
                m.tick(t)
                t += 50
            }
            m.onRelease()
            // 单击结果就是 UI 层的 +1
            out += value + 1
            value += 1
        }

        assertEquals(listOf(1, 2, 3, 4, 5), out)
    }
}
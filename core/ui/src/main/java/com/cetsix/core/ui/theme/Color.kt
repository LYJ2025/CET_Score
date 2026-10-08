package com.cetsix.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 全局色板 —— 低饱和渐变，浅色/深色各一套。
 * 玻璃卡片本身半透明，底色不能太亮，否则模糊效果看不出来。
 */

// ---------- 浅色主题：米白 → 淡粉 ----------
val CreamStart = Color(0xFFF7F4F1)      // 米白
val CreamEnd = Color(0xFFF3E7EC)        // 淡粉
val LightAccent = Color(0xFF7C8AA8)     // 雾霾蓝
val LightAccentSoft = Color(0xFFA8B4C8)
val LightOnSurface = Color(0xFF23262E)
val LightOnSurfaceVariant = Color(0xFF5A5F6B)

// ---------- 深色主题：深蓝紫 → 青灰 ----------
val DeepIndigo = Color(0xFF1A1B2E)      // 深蓝紫
val DeepTealGrey = Color(0xFF1F2A2E)   // 青灰
val DarkAccent = Color(0xFF8FA6C4)      // 冷调蓝，玻璃描边与高亮
val DarkAccentSoft = Color(0xFF5A6B80)
val DarkOnSurface = Color(0xFFE6E8EE)
val DarkOnSurfaceVariant = Color(0xFFA8AEBE)

// ---------- 语义色 ----------
val CorrectGreen = Color(0xFF6BA88B)
val WrongRed = Color(0xFFC98A8A)

/** 分数区间对应的语义色，用于结果页与趋势图 */
fun scoreColor(ratio: Float): Color = when {
    ratio >= 0.75f -> Color(0xFF6BA88B)
    ratio >= 0.60f -> Color(0xFF8FA6C4)
    ratio >= 0.50f -> Color(0xFFC9A87A)
    else -> Color(0xFFC98A8A)
}

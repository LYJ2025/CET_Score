package com.cetsix.core.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

private val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = Color.White,
    primaryContainer = LightAccentSoft,
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF9A8E9E),
    background = CreamStart,
    onBackground = LightOnSurface,
    surface = CreamEnd,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFE8E4E6),
    onSurfaceVariant = LightOnSurfaceVariant,
    error = WrongRed,
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = Color(0xFF10131A),
    primaryContainer = DarkAccentSoft,
    onPrimaryContainer = Color(0xFFE6E8EE),
    secondary = Color(0xFF8E9AA8),
    background = DeepIndigo,
    onBackground = DarkOnSurface,
    surface = DeepTealGrey,
    onSurface = DarkOnSurface,
    surfaceVariant = Color(0xFF2A3140),
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = WrongRed,
)

/** 字体层级：标题大而重，正文清晰，辅助信息小而淡 */
private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 56.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal),
)

/** 间距与圆角，统一在此定义，避免各页面各写各的 */
object Dimens {
    val CornerLarge = 28.dp    // 大卡片
    val CornerMedium = 22.dp   // 中卡片
    val CornerSmall = 16.dp    // 小控件

    val SpaceXS = 4.dp
    val SpaceS = 8.dp
    val SpaceM = 14.dp
    val SpaceL = 20.dp
    val SpaceXL = 28.dp
    val SpaceXXL = 40.dp

    val ScreenPadding = 22.dp  // 屏幕左右留白
}

/** 通过 CompositionLocal 暴露当前是否深色，供玻璃组件决定描边/阴影强度 */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun CetSixTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalIsDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            content = content,
        )
    }
}

/**
 * 应用背景渐变。玻璃卡片需要背后有内容才能看出模糊效果，
 * 所以在真实内容之后铺一层带光斑的渐变。
 */
@Composable
fun appBackgroundBrush(darkTheme: Boolean): Brush {
    val base = if (darkTheme) listOf(DeepIndigo, DeepTealGrey) else listOf(CreamStart, CreamEnd)
    val glow = if (darkTheme) {
        listOf(Color(0x332E4A6B), Color(0x00000000))
    } else {
        listOf(Color(0x33FFFFFF), Color(0x00000000))
    }
    return Brush.linearGradient(base + glow)
}

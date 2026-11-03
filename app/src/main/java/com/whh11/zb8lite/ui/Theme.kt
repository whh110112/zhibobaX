package com.whh11.zb8lite.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 直播吧配色：取 logo 与站点 CSS 的亮蓝 (#2E9FFF) 为主色，
 * 比分用站点同款红 (#E04E4C)。整体比懂球帝更亮、更快节奏。
 */
object Zb8Colors {
    val Blue = Color(0xFF2E9FFF)
    val BlueDeep = Color(0xFF1B6FBF)
    val BlueSoft = Color(0xFF6FBDFF)
    val ScoreRed = Color(0xFFE04E4C)
    val Live = Color(0xFF3DD68C)

    val Bg = Color(0xFF0B0E14)
    val Surface = Color(0xFF141922)
    val SurfaceHigh = Color(0xFF1C2330)
    val Line = Color(0xFF232B39)

    val TextPrimary = Color(0xFFEDF1F7)
    val TextSecondary = Color(0xFF8A94A6)
    val TextTertiary = Color(0xFF5B6577)
}

private val Zb8Scheme = darkColorScheme(
    primary = Zb8Colors.Blue,
    onPrimary = Color.White,
    primaryContainer = Zb8Colors.BlueDeep,
    onPrimaryContainer = Color(0xFFD3E9FF),
    secondary = Zb8Colors.ScoreRed,
    onSecondary = Color.White,
    tertiary = Zb8Colors.Live,
    background = Zb8Colors.Bg,
    onBackground = Zb8Colors.TextPrimary,
    surface = Zb8Colors.Surface,
    onSurface = Zb8Colors.TextPrimary,
    surfaceVariant = Zb8Colors.SurfaceHigh,
    onSurfaceVariant = Zb8Colors.TextSecondary,
    outline = Zb8Colors.Line,
    error = Color(0xFFE5484D)
)

private val Zb8Type = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 13.5.sp, lineHeight = 19.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
)

@Composable
fun Zb8Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Zb8Scheme, typography = Zb8Type, content = content)
}

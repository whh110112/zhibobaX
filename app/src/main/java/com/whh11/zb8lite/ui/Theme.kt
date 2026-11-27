package com.whh11.zb8lite.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.whh11.zb8lite.data.LocalStore
import com.whh11.zb8lite.data.ThemeMode

val LocalIsDark = compositionLocalOf { false }
val LocalIsAmoled = compositionLocalOf { false }

/**
 * 直播吧原味视觉语义 Token：
 * 清爽直播蓝（#1E88E5）+ 直播翡翠绿（#10B981）+ 神评火焰橙红（#FF5722）
 * 白底为主场，深色中性暗岩灰，独立 AMOLED 纯黑模式。
 */
object Zb8Colors {
    val BrandBlue = Color(0xFF1E88E5)
    val BrandBlueDark = Color(0xFF3B9EFF)
    val LiveGreen = Color(0xFF10B981)
    val HotOrange = Color(0xFFFF5722)
    val Red = Color(0xFFE53935)
    val Amber = Color(0xFFF59E0B)

    val Primary: Color
        @Composable get() = if (LocalIsDark.current) BrandBlueDark else BrandBlue

    val PrimaryContainer: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF13253A) else Color(0xFFE8F3FE)

    val Bg: Color
        @Composable get() = when {
            LocalIsDark.current && LocalIsAmoled.current -> Color(0xFF000000)
            LocalIsDark.current -> Color(0xFF101216)
            else -> Color(0xFFF4F5F7)
        }

    val Card: Color
        @Composable get() = when {
            LocalIsDark.current && LocalIsAmoled.current -> Color(0xFF000000)
            LocalIsDark.current -> Color(0xFF181B22)
            else -> Color(0xFFFFFFFF)
        }

    val SurfaceHigh: Color
        @Composable get() = when {
            LocalIsDark.current && LocalIsAmoled.current -> Color(0xFF141414)
            LocalIsDark.current -> Color(0xFF222732)
            else -> Color(0xFFEEF1F5)
        }

    val Divider: Color
        @Composable get() = when {
            LocalIsDark.current && LocalIsAmoled.current -> Color(0xFF1C1C1C)
            LocalIsDark.current -> Color(0xFF262B36)
            else -> Color(0xFFE6E9EF)
        }

    val TextPrimary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFFEDF1F7) else Color(0xFF181C24)

    val TextSecondary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF939DAE) else Color(0xFF64748B)

    val TextTertiary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF646E80) else Color(0xFF94A3B8)
}

private val LightScheme = lightColorScheme(
    primary = Zb8Colors.BrandBlue,
    onPrimary = Color.White,
    background = Color(0xFFF4F5F7),
    onBackground = Color(0xFF181C24),
    surface = Color.White,
    onSurface = Color(0xFF181C24),
    surfaceVariant = Color(0xFFEEF1F5),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE6E9EF),
    error = Zb8Colors.Red
)

private val DarkScheme = darkColorScheme(
    primary = Zb8Colors.BrandBlueDark,
    onPrimary = Color.White,
    background = Color(0xFF101216),
    onBackground = Color(0xFFEDF1F7),
    surface = Color(0xFF181B22),
    onSurface = Color(0xFFEDF1F7),
    surfaceVariant = Color(0xFF222732),
    onSurfaceVariant = Color(0xFF939DAE),
    outline = Color(0xFF262B36),
    error = Color(0xFFF87171)
)

private val AmoledScheme = DarkScheme.copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceVariant = Color(0xFF141414),
    outline = Color(0xFF1C1C1C)
)

private val Zb8Type = Typography(
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = TextStyle(fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.5.sp, lineHeight = 25.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun Zb8Theme(content: @Composable () -> Unit) {
    val mode by LocalStore.themeMode.collectAsState()
    val amoled by LocalStore.amoled.collectAsState()
    val fontScale by LocalStore.fontScale.collectAsState()
    val systemDark = isSystemInDarkTheme()

    val isDark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val scheme = when {
        !isDark -> LightScheme
        amoled -> AmoledScheme
        else -> DarkScheme
    }

    val baseDensity = LocalDensity.current
    CompositionLocalProvider(
        LocalIsDark provides isDark,
        LocalIsAmoled provides (isDark && amoled),
        LocalDensity provides Density(baseDensity.density, baseDensity.fontScale * fontScale)
    ) {
        MaterialTheme(colorScheme = scheme, typography = Zb8Type, content = content)
    }
}

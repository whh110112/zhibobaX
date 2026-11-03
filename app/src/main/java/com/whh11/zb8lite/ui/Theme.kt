package com.whh11.zb8lite.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("白天模式"),
    DARK("夜间模式")
}

object ThemeManager {
    var mode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    fun init(context: Context) {
        val sp = context.getSharedPreferences("zb8_pref", Context.MODE_PRIVATE)
        val saved = sp.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        mode = runCatching { ThemeMode.valueOf(saved) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun set(context: Context, newMode: ThemeMode) {
        mode = newMode
        val sp = context.getSharedPreferences("zb8_pref", Context.MODE_PRIVATE)
        sp.edit().putString("theme_mode", newMode.name).apply()
    }
}

val LocalIsDark = compositionLocalOf { true }

object Zb8Colors {
    val Blue = Color(0xFF0084FF)
    val BlueDeep = Color(0xFF0066CC)
    val BlueSoft = Color(0xFF4DA8FF)
    val ScoreRed = Color(0xFFE04E4C)
    val Live = Color(0xFF2EBD73)

    // 动态属性跟随当前主题
    val Bg: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF0B0E14) else Color(0xFFF4F6F9)

    val Surface: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF141922) else Color(0xFFFFFFFF)

    val SurfaceHigh: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF1C2330) else Color(0xFFEEF2F6)

    val Line: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF232B39) else Color(0xFFE2E8F0)

    val TextPrimary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFFEDF1F7) else Color(0xFF1E293B)

    val TextSecondary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF8A94A6) else Color(0xFF64748B)

    val TextTertiary: Color
        @Composable get() = if (LocalIsDark.current) Color(0xFF5B6577) else Color(0xFF94A3B8)
}

private val DarkScheme = darkColorScheme(
    primary = Zb8Colors.Blue,
    onPrimary = Color.White,
    primaryContainer = Zb8Colors.BlueDeep,
    onPrimaryContainer = Color(0xFFD3E9FF),
    secondary = Zb8Colors.ScoreRed,
    onSecondary = Color.White,
    tertiary = Zb8Colors.Live,
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFEDF1F7),
    surface = Color(0xFF141922),
    onSurface = Color(0xFFEDF1F7),
    surfaceVariant = Color(0xFF1C2330),
    onSurfaceVariant = Color(0xFF8A94A6),
    outline = Color(0xFF232B39),
    error = Color(0xFFE5484D)
)

private val LightScheme = lightColorScheme(
    primary = Zb8Colors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EFFF),
    onPrimaryContainer = Zb8Colors.BlueDeep,
    secondary = Zb8Colors.ScoreRed,
    onSecondary = Color.White,
    tertiary = Zb8Colors.Live,
    background = Color(0xFFF4F6F9),
    onBackground = Color(0xFF1E293B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFEEF2F6),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
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
    val systemDark = isSystemInDarkTheme()
    val isDark = when (ThemeManager.mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val scheme = if (isDark) DarkScheme else LightScheme

    CompositionLocalProvider(LocalIsDark provides isDark) {
        MaterialTheme(colorScheme = scheme, typography = Zb8Type, content = content)
    }
}

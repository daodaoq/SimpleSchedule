package com.example.simpleschedule.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * 浅色主题配色方案
 * 使用莫兰迪低饱和度色系，整体干净柔和
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = MorandiBlue.copy(alpha = 0.2f),
    onPrimaryContainer = PrimaryLight,
    secondary = MorandiSlate,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = MorandiWarmGray.copy(alpha = 0.2f),
    tertiary = MorandiLavender,
    surface = SurfaceLight,
    onSurface = androidx.compose.ui.graphics.Color(0xFF1C1B1F),
    surfaceVariant = BackgroundLight,
    background = BackgroundLight,
    onBackground = androidx.compose.ui.graphics.Color(0xFF1C1B1F)
)

/**
 * 深色主题配色方案
 * 深色背景下保持莫兰迪色系的柔和感
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF1A2A30),
    primaryContainer = MorandiBlue.copy(alpha = 0.3f),
    onPrimaryContainer = PrimaryDark,
    secondary = MorandiSky,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF1A2A30),
    secondaryContainer = MorandiWarmGray.copy(alpha = 0.2f),
    tertiary = MorandiMauve,
    surface = SurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE6E1E5),
    surfaceVariant = BackgroundDark,
    background = BackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE6E1E5)
)

/**
 * SimpleSchedule 全局主题
 *
 * 特性：
 * - 支持动态取色（Android 12+ Material You）
 * - 深浅色模式自动跟随系统
 * - 关闭动态取色时可回退到莫兰迪自定义色板
 *
 * @param darkTheme 是否使用深色主题（默认跟随系统设置）
 * @param dynamicColor 是否启用 Material You 动态取色（默认关闭，保持莫兰迪风格统一）
 * @param content 子 Composable
 */
@Composable
fun SimpleScheduleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,  // 默认关闭动态取色，保持莫兰迪风格一致性
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

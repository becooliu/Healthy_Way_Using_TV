package com.healthyway.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 应用主题配置
 * 【功能】定义统一的色彩方案和主题样式，适用于 Android TV 深色显示
 */

// TV 深色主题色板
private val TvDarkColorScheme = darkColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1565C0),
    onPrimaryContainer = Color(0xFFBBDEFB),
    secondary = Color(0xFF42A5F5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1A237E),
    onSecondaryContainer = Color(0xFF90CAF9),
    background = Color(0xFF121212),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFBDBDBD),
    error = Color(0xFFEF5350),
    onError = Color.White,
    outline = Color(0xFF616161)
)

/**
 * 健康看电视应用主题
 * @param content 界面内容
 */
@Composable
fun HealthyWayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TvDarkColorScheme,
        content = content
    )
}

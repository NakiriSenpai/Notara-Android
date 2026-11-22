package com.notara.player.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

@Composable
fun NotaraTheme(
    seedColor: Color = Color(0xFF3B82F6),
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        val primary = lerp(seedColor, Color.White, 0.28f)
        val surface = lerp(Color(0xFF0E1117), seedColor, 0.06f)
        val surfaceContainer = lerp(Color(0xFF171C26), seedColor, 0.09f)
        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF0B1220),
            primaryContainer = lerp(Color(0xFF1E293B), seedColor, 0.35f),
            onPrimaryContainer = Color(0xFFF1F5F9),
            secondary = Color(0xFFF59E0B),
            onSecondary = Color(0xFF1F1600),
            background = surface,
            onBackground = Color(0xFFF1F5F9),
            surface = surface,
            onSurface = Color(0xFFF1F5F9),
            surfaceVariant = surfaceContainer,
            onSurfaceVariant = Color(0xFF94A3B8)
        )
    } else {
        val primary = lerp(seedColor, Color(0xFF0F172A), 0.18f)
        val surface = lerp(Color(0xFFF8FAFC), seedColor, 0.03f)
        val surfaceContainer = lerp(Color(0xFFEEF2F6), seedColor, 0.06f)
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = lerp(Color.White, seedColor, 0.18f),
            onPrimaryContainer = Color(0xFF0F172A),
            secondary = Color(0xFFD97706),
            onSecondary = Color.White,
            background = surface,
            onBackground = Color(0xFF0F172A),
            surface = surface,
            onSurface = Color(0xFF0F172A),
            surfaceVariant = surfaceContainer,
            onSurfaceVariant = Color(0xFF475569)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

package com.notara.player.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

enum class AppThemePreset(
    val displayName: String,
    val subtitle: String,
    val isDark: Boolean,
    val previewBg: Color,
    val previewAccent: Color
) {
    TOKYO_MIDNIGHT(
        displayName = "Tokyo Midnight",
        subtitle = "Espresso hangat & lampu amber",
        isDark = true,
        previewBg = Color(0xFF171311),
        previewAccent = Color(0xFFE8A864)
    ),
    SEOUL_CREAM(
        displayName = "Seoul Cream",
        subtitle = "Kertas krem, matcha & terakota",
        isDark = false,
        previewBg = Color(0xFFFAF6F0),
        previewAccent = Color(0xFFC87D55)
    ),
    MATCHA_MILK(
        displayName = "Matcha Milk",
        subtitle = "Hijau sage lembut & menenangkan",
        isDark = false,
        previewBg = Color(0xFFF3F7F2),
        previewAccent = Color(0xFF4F7A5B)
    ),
    SAKURA_DUSK(
        displayName = "Sakura Dusk",
        subtitle = "Gelap hangat & aksen sakura",
        isDark = true,
        previewBg = Color(0xFF191317),
        previewAccent = Color(0xFFE29EB0)
    ),
    PURE_OLED(
        displayName = "Pure OLED",
        subtitle = "Hitam pekat hemat baterai",
        isDark = true,
        previewBg = Color(0xFF000000),
        previewAccent = Color(0xFF38BDF8)
    ),
    DYNAMIC_COVER(
        displayName = "Dinamis Sampul",
        subtitle = "Mengikuti warna sampul lagu",
        isDark = true,
        previewBg = Color(0xFF14161F),
        previewAccent = Color(0xFFA78BFA)
    )
}

object ThemePreferences {
    private const val PREFS_NAME = "notara_theme_prefs"
    private const val KEY_THEME = "selected_theme"

    fun loadTheme(context: Context): AppThemePreset {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, AppThemePreset.TOKYO_MIDNIGHT.name)
        return AppThemePreset.entries.firstOrNull { it.name == raw }
            ?: AppThemePreset.TOKYO_MIDNIGHT
    }

    fun saveTheme(context: Context, preset: AppThemePreset) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, preset.name)
            .apply()
    }
}

fun buildNotaraColorScheme(preset: AppThemePreset, dynamicSeed: Color): ColorScheme {
    return when (preset) {
        AppThemePreset.TOKYO_MIDNIGHT -> darkColorScheme(
            primary = Color(0xFFE8A864),
            onPrimary = Color(0xFF23160A),
            primaryContainer = Color(0xFF3A291C),
            onPrimaryContainer = Color(0xFFFFE5C7),
            secondary = Color(0xFFD99AA4),
            onSecondary = Color(0xFF2B1419),
            secondaryContainer = Color(0xFF352622),
            onSecondaryContainer = Color(0xFFF5DADF),
            background = Color(0xFF161311),
            onBackground = Color(0xFFF6EFE6),
            surface = Color(0xFF161311),
            onSurface = Color(0xFFF6EFE6),
            surfaceVariant = Color(0xFF241E1B),
            onSurfaceVariant = Color(0xFFB5A698),
            outline = Color(0xFF3E342E)
        )
        AppThemePreset.SEOUL_CREAM -> lightColorScheme(
            primary = Color(0xFFC86D4B),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFF5E3D6),
            onPrimaryContainer = Color(0xFF3E1E12),
            secondary = Color(0xFF5D8069),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFE3ECE5),
            onSecondaryContainer = Color(0xFF1B3022),
            background = Color(0xFFFAF6F0),
            onBackground = Color(0xFF2C241E),
            surface = Color(0xFFFAF6F0),
            onSurface = Color(0xFF2C241E),
            surfaceVariant = Color(0xFFF0E8DE),
            onSurfaceVariant = Color(0xFF6F6256),
            outline = Color(0xFFD8CCBE)
        )
        AppThemePreset.MATCHA_MILK -> lightColorScheme(
            primary = Color(0xFF4F7A5B),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFDCEADF),
            onPrimaryContainer = Color(0xFF172E1E),
            secondary = Color(0xFFC68B59),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF2E6D8),
            onSecondaryContainer = Color(0xFF36210E),
            background = Color(0xFFF4F7F2),
            onBackground = Color(0xFF222B24),
            surface = Color(0xFFF4F7F2),
            onSurface = Color(0xFF222B24),
            surfaceVariant = Color(0xFFE6EEE4),
            onSurfaceVariant = Color(0xFF58695B),
            outline = Color(0xFFC7D6C5)
        )
        AppThemePreset.SAKURA_DUSK -> darkColorScheme(
            primary = Color(0xFFE5A0B3),
            onPrimary = Color(0xFF2B1119),
            primaryContainer = Color(0xFF3C232B),
            onPrimaryContainer = Color(0xFFFFE2EA),
            secondary = Color(0xFFE6B68A),
            onSecondary = Color(0xFF2B1A0B),
            secondaryContainer = Color(0xFF342725),
            onSecondaryContainer = Color(0xFFFBE7D5),
            background = Color(0xFF191316),
            onBackground = Color(0xFFF8EEF2),
            surface = Color(0xFF191316),
            onSurface = Color(0xFFF8EEF2),
            surfaceVariant = Color(0xFF261D22),
            onSurfaceVariant = Color(0xFFB9A4AE),
            outline = Color(0xFF42333B)
        )
        AppThemePreset.PURE_OLED -> darkColorScheme(
            primary = Color(0xFF38BDF8),
            onPrimary = Color(0xFF041C28),
            primaryContainer = Color(0xFF122836),
            onPrimaryContainer = Color(0xFFD6F1FF),
            secondary = Color(0xFFF59E0B),
            onSecondary = Color(0xFF241600),
            secondaryContainer = Color(0xFF1F1C14),
            onSecondaryContainer = Color(0xFFFFE7B8),
            background = Color(0xFF000000),
            onBackground = Color(0xFFF8FAFC),
            surface = Color(0xFF000000),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF121417),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF262A30)
        )
        AppThemePreset.DYNAMIC_COVER -> {
            val primary = lerp(dynamicSeed, Color.White, 0.30f)
            val bg = lerp(Color(0xFF121114), dynamicSeed, 0.08f)
            val card = lerp(Color(0xFF1E1C22), dynamicSeed, 0.12f)
            darkColorScheme(
                primary = primary,
                onPrimary = Color(0xFF111114),
                primaryContainer = lerp(Color(0xFF292530), dynamicSeed, 0.32f),
                onPrimaryContainer = Color(0xFFF5F3F8),
                secondary = Color(0xFFE8A864),
                onSecondary = Color(0xFF23160A),
                background = bg,
                onBackground = Color(0xFFF5F3F8),
                surface = bg,
                onSurface = Color(0xFFF5F3F8),
                surfaceVariant = card,
                onSurfaceVariant = Color(0xFFAFA8B8),
                outline = Color(0xFF3A3442)
            )
        }
    }
}

@Composable
fun NotaraTheme(
    preset: AppThemePreset = AppThemePreset.TOKYO_MIDNIGHT,
    dynamicSeed: Color = Color(0xFFE8A864),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = buildNotaraColorScheme(preset, dynamicSeed),
        content = content
    )
}

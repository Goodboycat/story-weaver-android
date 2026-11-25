package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.data.ThemePreset
import com.example.data.UserSettings

private val StoryWeaverShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
)

private fun colorSchemeFor(settings: UserSettings): ColorScheme {
    val customAccent = settings.customAccentArgb?.let { Color(it.toInt()) }
    val base = when (settings.themePreset) {
        ThemePreset.LIGHT -> lightColorScheme(
            primary = customAccent ?: LightPrimary,
            onPrimary = if (customAccent != null && customAccent.luminance() > 0.55f) Color(0xFF1A1612) else LightOnPrimary,
            primaryContainer = (customAccent ?: LightPrimary).copy(alpha = 0.14f),
            onPrimaryContainer = customAccent ?: LightPrimary,
            secondary = LightSecondary,
            onSecondary = Color.White,
            background = LightBg,
            onBackground = LightText,
            surface = LightSurface,
            onSurface = LightText,
            surfaceVariant = LightSurfaceVariant,
            onSurfaceVariant = LightTextMuted,
            outline = LightTextMuted.copy(alpha = 0.3f),
        )
        ThemePreset.MIDNIGHT -> darkColorScheme(
            primary = customAccent ?: MidnightPrimary,
            onPrimary = if (customAccent != null && customAccent.luminance() > 0.55f) Color(0xFF0A0D18) else MidnightOnPrimary,
            primaryContainer = (customAccent ?: MidnightPrimary).copy(alpha = 0.22f),
            onPrimaryContainer = customAccent ?: Color(0xFFDDD6FE),
            secondary = MidnightSecondary,
            onSecondary = Color(0xFF042029),
            background = MidnightBg,
            onBackground = MidnightText,
            surface = MidnightSurface,
            onSurface = MidnightText,
            surfaceVariant = MidnightSurfaceVariant,
            onSurfaceVariant = MidnightTextMuted,
            outline = MidnightTextMuted.copy(alpha = 0.28f),
        )
        ThemePreset.FOREST -> darkColorScheme(
            primary = customAccent ?: ForestPrimary,
            onPrimary = if (customAccent != null && customAccent.luminance() > 0.55f) Color(0xFF031B11) else ForestOnPrimary,
            primaryContainer = (customAccent ?: ForestPrimary).copy(alpha = 0.22f),
            onPrimaryContainer = customAccent ?: Color(0xFFA7F3D0),
            secondary = ForestSecondary,
            onSecondary = Color(0xFF211600),
            background = ForestBg,
            onBackground = ForestText,
            surface = ForestSurface,
            onSurface = ForestText,
            surfaceVariant = ForestSurfaceVariant,
            onSurfaceVariant = ForestTextMuted,
            outline = ForestTextMuted.copy(alpha = 0.28f),
        )
        ThemePreset.SUNSET -> darkColorScheme(
            primary = customAccent ?: SunsetPrimary,
            onPrimary = if (customAccent != null && customAccent.luminance() > 0.55f) Color(0xFF200A10) else SunsetOnPrimary,
            primaryContainer = (customAccent ?: SunsetPrimary).copy(alpha = 0.22f),
            onPrimaryContainer = customAccent ?: Color(0xFFFECDD3),
            secondary = SunsetSecondary,
            onSecondary = Color(0xFF240F00),
            background = SunsetBg,
            onBackground = SunsetText,
            surface = SunsetSurface,
            onSurface = SunsetText,
            surfaceVariant = SunsetSurfaceVariant,
            onSurfaceVariant = SunsetTextMuted,
            outline = SunsetTextMuted.copy(alpha = 0.28f),
        )
        ThemePreset.DARK -> darkColorScheme(
            primary = customAccent ?: DarkPrimary,
            onPrimary = if (customAccent != null && customAccent.luminance() > 0.55f) DarkOnPrimary else Color.White,
            primaryContainer = (customAccent ?: DarkPrimary).copy(alpha = 0.20f),
            onPrimaryContainer = customAccent ?: Color(0xFFFDE68A),
            secondary = DarkSecondary,
            onSecondary = Color(0xFF04202D),
            background = DarkBg,
            onBackground = DarkText,
            surface = DarkSurface,
            onSurface = DarkText,
            surfaceVariant = DarkSurfaceVariant,
            onSurfaceVariant = DarkTextMuted,
            outline = DarkTextMuted.copy(alpha = 0.28f),
        )
    }
    return base
}

@Composable
fun StoryWeaverTheme(
    settings: UserSettings = UserSettings(),
    content: @Composable () -> Unit,
) {
    val colorScheme = androidx.compose.runtime.remember(
        settings.themePreset,
        settings.customAccentArgb,
    ) {
        colorSchemeFor(settings)
    }
    val typography = androidx.compose.runtime.remember(settings.textScale) {
        buildStoryWeaverTypography(settings.textScale.factor)
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = StoryWeaverShapes,
        content = content,
    )
}

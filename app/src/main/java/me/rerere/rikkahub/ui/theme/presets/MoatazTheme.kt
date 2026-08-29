package me.rerere.rikkahub.ui.theme.presets

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import me.rerere.rikkahub.ui.theme.PresetTheme

/**
 * Moataz Alaqami product identity.
 * Crisp indigo/azure accents, quiet neutral surfaces and high contrast for long agent sessions.
 */
val MoatazThemePreset by lazy {
    PresetTheme(
        id = "moataz",
        name = { Text("Moataz") },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    primary = Color(0xFF3157D5),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFDCE2FF),
    onPrimaryContainer = Color(0xFF07184F),
    secondary = Color(0xFF536078),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD7E3FF),
    onSecondaryContainer = Color(0xFF101C31),
    tertiary = Color(0xFF006A69),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF9CF1EF),
    onTertiaryContainer = Color(0xFF00201F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF9F9FC),
    onBackground = Color(0xFF1A1B20),
    surface = Color(0xFFF9F9FC),
    onSurface = Color(0xFF1A1B20),
    surfaceVariant = Color(0xFFE2E2EC),
    onSurfaceVariant = Color(0xFF45464F),
    outline = Color(0xFF767680),
    outlineVariant = Color(0xFFC6C6D0),
    inverseSurface = Color(0xFF2F3036),
    inverseOnSurface = Color(0xFFF1F0F7),
    inversePrimary = Color(0xFFB8C3FF),
    surfaceDim = Color(0xFFDAD9E0),
    surfaceBright = Color(0xFFF9F9FC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F3F8),
    surfaceContainer = Color(0xFFEEEEF3),
    surfaceContainerHigh = Color(0xFFE8E7ED),
    surfaceContainerHighest = Color(0xFFE2E2E7),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFFB8C3FF),
    onPrimary = Color(0xFF002788),
    primaryContainer = Color(0xFF173AA7),
    onPrimaryContainer = Color(0xFFDCE2FF),
    secondary = Color(0xFFBBC7E5),
    onSecondary = Color(0xFF253148),
    secondaryContainer = Color(0xFF3B475F),
    onSecondaryContainer = Color(0xFFD7E3FF),
    tertiary = Color(0xFF80D5D3),
    onTertiary = Color(0xFF003736),
    tertiaryContainer = Color(0xFF00504F),
    onTertiaryContainer = Color(0xFF9CF1EF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE3E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE3E2E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC6C6D0),
    outline = Color(0xFF90909A),
    outlineVariant = Color(0xFF45464F),
    inverseSurface = Color(0xFFE3E2E9),
    inverseOnSurface = Color(0xFF2F3036),
    inversePrimary = Color(0xFF3157D5),
    surfaceDim = Color(0xFF111318),
    surfaceBright = Color(0xFF373940),
    surfaceContainerLowest = Color(0xFF0C0E13),
    surfaceContainerLow = Color(0xFF1A1B20),
    surfaceContainer = Color(0xFF1E1F25),
    surfaceContainerHigh = Color(0xFF282A2F),
    surfaceContainerHighest = Color(0xFF33343A),
)

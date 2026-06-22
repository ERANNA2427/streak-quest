package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StreakQuestColorScheme = darkColorScheme(
    primary = PastelPurple,
    secondary = PastelTeal,
    tertiary = PastelPurpleGlow,
    background = SlateBg,
    surface = SlateCard,
    onPrimary = SlateBg,
    onSecondary = SlateBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    surfaceVariant = SlateHeader,
    outline = BorderColor
)

@Composable
fun StreakQuestTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = StreakQuestColorScheme,
        typography = Typography,
        content = content
    )
}

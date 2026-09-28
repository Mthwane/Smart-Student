package com.example.smartstudent.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = StudentBlack,
    onPrimary = StudentBeigeBg,
    secondary = StudentGold,
    onSecondary = StudentWhite,
    tertiary = StudentGreen,
    onTertiary = StudentWhite,
    background = StudentBeigeBg,
    onBackground = StudentBrown800,
    surface = StudentCream,
    onSurface = StudentBrown800,
    surfaceVariant = StudentBeige100,
    onSurfaceVariant = StudentBrown600,
    outline = StudentBeige200,
    error = StudentRed,
    onError = StudentWhite,
    errorContainer = StudentRedLight
)

private val DarkColors = darkColorScheme(
    primary = StudentBeigeBg,
    onPrimary = StudentBlack,
    secondary = StudentGold,
    onSecondary = StudentBlack,
    tertiary = StudentGreen,
    onTertiary = StudentBlack,
    background = StudentBlack,
    onBackground = StudentBeigeBg,
    surface = StudentBrown800,
    onSurface = StudentBeigeBg,
    surfaceVariant = StudentBrown800,
    onSurfaceVariant = StudentTaupe400,
    outline = StudentBrown600,
    error = StudentRed,
    onError = StudentWhite,
    errorContainer = StudentRedLight
)

@Composable
fun SmartStudentTheme(
    // The whole visual design (light-tinted accent cards throughout the app —
    // balance card, allowance card, permission prompts, etc.) was built assuming
    // this light scheme's ambient text colors. Auto-following system dark mode
    // caused real contrast bugs (light text on light-tinted backgrounds), so this
    // always uses the light scheme regardless of the device's dark mode setting.
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colorScheme,
        typography = StudentTypography,
        shapes = StudentShapes,
        content = content
    )
}

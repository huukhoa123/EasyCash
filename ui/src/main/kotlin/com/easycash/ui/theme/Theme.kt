package com.easycash.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    secondary = TealSecondaryDark,
    background = NavyBackgroundDark,
    surface = NavySurfaceDark,
    surfaceVariant = NavySurfaceVariantDark,
    onPrimary = Color(0xFF022C22),
    onSecondary = Color(0xFF042F2E),
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    onSurfaceVariant = TextSecondaryDark,
    error = ExpenseDark,
    onError = Color.Black,
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimaryLight,
    secondary = TealSecondaryLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight,
    onSurfaceVariant = TextSecondaryLight,
    error = ExpenseLight,
    onError = Color.White,
)

@Composable
fun EasyCashTheme(
    darkTheme: Boolean = true, // Default to Dark Premium mode per docs/UI_UX.md
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}

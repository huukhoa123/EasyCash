package com.easycash.ui.theme


import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    primaryContainer = Color(0xFF193E34),
    onPrimaryContainer = Color(0xFFB2F3D7),
    outline = Color(0xFF70827D),
    outlineVariant = Color(0xFF2B3A43),
    secondary = TealSecondaryDark,
    secondaryContainer = Color(0xFF243A3D),
    onSecondaryContainer = Color(0xFFC9EBE8),
    tertiary = Color(0xFFE2BC7E),
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
    primaryContainer = Color(0xFFD4F4E4),
    onPrimaryContainer = Color(0xFF004D39),
    outline = Color(0xFF73857B),
    outlineVariant = Color(0xFFD5E0D8),
    secondary = TealSecondaryLight,
    secondaryContainer = Color(0xFFDDEFEA),
    onSecondaryContainer = Color(0xFF164C46),
    tertiary = Color(0xFF866019),
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

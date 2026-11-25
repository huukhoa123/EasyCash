package com.easycash.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF022B21),
    primaryContainer = BrandPrimaryContainerDark,
    onPrimaryContainer = BrandOnPrimaryContainerDark,
    secondary = LedgerOnMintDark,
    onSecondary = Color(0xFF003731),
    secondaryContainer = Color(0xFF1B3D38),
    onSecondaryContainer = Color(0xFFC7F3EC),
    tertiary = LedgerOnButterDark,
    onTertiary = Color(0xFF402D05),
    background = CanvasDark,
    onBackground = TextInkPrimaryDark,
    surface = SurfaceLedgerDark,
    onSurface = TextInkPrimaryDark,
    surfaceVariant = SurfaceVariantLedgerDark,
    onSurfaceVariant = TextInkSecondaryDark,
    outline = OutlineLedgerDark,
    outlineVariant = OutlineVariantLedgerDark,
    error = ExpenseDark,
    onError = Color(0xFF560004),
)

private val LightColorScheme = lightColorScheme(
    primary = BrandPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = BrandPrimaryContainerLight,
    onPrimaryContainer = BrandOnPrimaryContainerLight,
    secondary = BrandPrimaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4F4EC),
    onSecondaryContainer = BrandOnPrimaryContainerLight,
    tertiary = LedgerOnButterLight,
    onTertiary = Color.White,
    background = CanvasIvoryLight,
    onBackground = TextInkPrimaryLight,
    surface = SurfaceLedgerLight,
    onSurface = TextInkPrimaryLight,
    surfaceVariant = SurfaceVariantLedgerLight,
    onSurfaceVariant = TextInkSecondaryLight,
    outline = OutlineLedgerLight,
    outlineVariant = OutlineVariantLedgerLight,
    error = ExpenseLight,
    onError = Color.White,
)

@Composable
fun EasyCashTheme(
    darkTheme: Boolean = false, // Ivory Canvas Light is default per UI_LEDGER_DIRECTION.md
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

package com.easycash.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

enum class Glyph(val path: String) {
    Home("M3 10 L12 3 L21 10 M5 9 L5 21 L10 21 L10 14 L14 14 L14 21 L19 21 L19 9"),
    Transactions("M4 7 L20 7 M16 3 L20 7 L16 11 M20 17 L4 17 M8 13 L4 17 L8 21"),
    Reports("M4 20 L4 13 M10 20 L10 8 M16 20 L16 4 M22 20 L22 11"),
    Wallet("M4 5 L19 5 L19 8 M4 5 L4 20 L21 20 L21 8 L4 8 M16 12 L21 12 L21 16 L16 16 Z"),
    Settings("M4 6 L20 6 M4 12 L20 12 M4 18 L20 18 M8 3 L8 9 M16 9 L16 15 M10 15 L10 21"),
    Plus("M12 5 L12 19 M5 12 L19 12"),
    Arrow("M5 12 L19 12 M13 6 L19 12 L13 18"),
    Income("M6 18 L18 6 M6 6 L18 6 L18 18"),
    Expense("M6 6 L18 18 M6 18 L18 18 L18 6"),
    Shield("M12 3 L20 6 L20 12 C20 17 16 20 12 22 C8 20 4 17 4 12 L4 6 Z M8 12 L11 15 L16 9"),
    Search("M10 3 A7 7 0 1 0 10 17 A7 7 0 1 0 10 3 M15 15 L21 21"),
    Eye("M2 12 C7 3 17 3 22 12 C17 21 7 21 2 12 M12 9 A3 3 0 1 0 12 15 A3 3 0 1 0 12 9"),
    EyeOff("M2 2 L22 22 M9.88 9.88 A3 3 0 1 0 14.12 14.12 M10.73 5.08 A10.43 10.43 0 0 1 12 5 C17 5 21 9 22 12 C21.49 13.02 20.61 14.28 19.33 15.42 M6.61 6.61 C4.62 7.84 3.14 9.69 2 12 C3 15 7 19 12 19 C13.52 19 14.98 18.61 16.3 17.91"),
    Bank("M3 9 L12 3 L21 9 Z M5 12 L5 18 M10 12 L10 18 M15 12 L15 18 M20 12 L20 18 M3 21 L22 21"),
    Cash("M2 6 H22 V18 H2 Z M12 12 L12 12.01 M6 12 L6 12.01 M18 12 L18 12.01"),
    More("M12 6 L12 6.01 M12 12 L12 12.01 M12 18 L12 18.01"),
    Edit("M17 3 L21 7 L7 21 H3 V17 L17 3 Z"),
    Trash("M3 6 H21 M8 6 V4 H16 V6 M19 6 V20 H5 V6 M10 11 V17 M14 11 V17"),
    Check("M5 13 L9 17 L19 7"),
    Close("M18 6 L6 18 M6 6 L18 18"),
    Tag("M20.59 13.41 L13.42 20.58 C12.64 21.36 11.37 21.36 10.59 20.58 L2 12 V2 H12 L20.59 10.59 C21.37 11.37 21.37 12.63 20.59 13.41 Z M7 7 L7 7.01"),
    Sun("M12 3 V5 M12 19 V21 M5 12 H3 M21 12 H19 M18.36 5.64 L16.95 7.05 M7.05 16.95 L5.64 18.36 M18.36 18.36 L16.95 16.95 M7.05 7.05 L5.64 5.64 M12 8 A4 4 0 1 0 12 16 A4 4 0 1 0 12 8 Z"),
    Moon("M21 12.79 A9 9 0 1 1 11.21 3 A7 7 0 0 0 21 12.79 Z"),
    Download("M12 3 V15 M7 10 L12 15 L17 10 M4 20 H20"),
    Upload("M12 17 V5 M7 10 L12 5 L17 10 M4 20 H20"),
    Document("M14 2 H6 C4.9 2 4 2.9 4 4 V20 C4 21.1 4.9 22 6 22 H18 C19.1 22 20 21.1 20 20 V8 Z M14 2 V8 H20 M16 13 H8 M16 17 H8"),
    Bell("M18 8 A6 6 0 0 0 6 8 C6 15 3 17 3 17 H21 C21 17 18 15 18 8 M10.3 21 A2 2 0 0 0 13.7 21"),
    Repeat("M17 1 L21 5 L17 9 M3 11 V9 C3 6.8 4.8 5 7 5 H21 M7 23 L3 19 L7 15 M21 13 V15 C21 17.2 19.2 19 17 19 H3"),
    Calendar("M19 4 H5 C3.9 4 3 4.9 3 6 V20 C3 21.1 3.9 22 5 22 H19 C20.1 22 21 21.1 21 20 V6 C21 4.9 20.1 4 19 4 Z M16 2 V6 M8 2 V6 M3 10 H21"),
}

@Composable
fun AppIcon(glyph: Glyph, contentDescription: String? = null, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    val vector = remember(glyph) {
        ImageVector.Builder(glyph.name, 24.dp, 24.dp, 24f, 24f)
            .addPath(PathParser().parsePathString(glyph.path).toNodes(), fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            .build()
    }
    Icon(vector, contentDescription, modifier.size(24.dp), tint)
}

@Composable
fun IconTile(glyph: Glyph, tint: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp).background(tint.copy(alpha = 0.10f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        AppIcon(glyph, tint = tint)
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun EmptyPanel(title: String, description: String, glyph: Glyph, action: String? = null, onAction: () -> Unit = {}) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))) {
        Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            IconTile(glyph)
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (action != null) FilledTonalButton(onClick = onAction) { Text(action) }
        }
    }
}

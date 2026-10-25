package com.example.savetool
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val PersianDark = darkColorScheme(
    primary = Color(0xFFFFC857), onPrimary = Color(0xFF2B1F00),
    primaryContainer = Color(0xFF5A4600), onPrimaryContainer = Color(0xFFFFE9A8),
    secondary = Color(0xFF4DD0E1), onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F57), onSecondaryContainer = Color(0xFF9EEFF9),
    tertiary = Color(0xFFCE93D8), onTertiary = Color(0xFF3E1D47),
    tertiaryContainer = Color(0xFF57305F), onTertiaryContainer = Color(0xFFF2C7FA),
    background = Color(0xFF0E1116), onBackground = Color(0xFFE3E5E8),
    surface = Color(0xFF151A21), onSurface = Color(0xFFE3E5E8),
    surfaceVariant = Color(0xFF232A33), onSurfaceVariant = Color(0xFFC1C7CE),
    error = Color(0xFFFF6B6B), onError = Color(0xFF4D0000),
    outline = Color(0xFF4A5360)
)
@Composable
fun SaveEditorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PersianDark, content = content)
}

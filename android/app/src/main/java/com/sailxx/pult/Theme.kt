package com.sailxx.pult

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Премиальная дизайн-система Pult в эстетике Apple × Claude × Okto:
 * Чистый OLED фон (#000000), сдержанные поверхности, четкая геометрия и тактильные радиусы.
 */
object PultColors {
    val background    = Color(0xFF000000) // Pure OLED Black
    val surface       = Color(0xFF0C0E14) // Deep Dark Card
    val surfaceLight  = Color(0xFF141722) // Elevated Surface
    val border        = Color(0xFF1F2432) // Subtle Border
    val accent        = Color(0xFF38BDF8) // Sky Blue Focus
    val success       = Color(0xFF22C55E) // Connected Green
    val danger        = Color(0xFFEF4444) // Mute / Alert Red
    val textPrimary   = Color(0xFFFFFFFF) // High Contrast Text
    val textSecondary = Color(0xFF8B949E) // Muted Info
    val textTertiary  = Color(0xFF94A3B8) // Subtle Captions
}

object PultDimens {
    val radiusCard   = 10.dp
    val radiusButton = 10.dp
    val radiusInner  = 8.dp
    val radiusBadge  = 6.dp
    val headerHeight = 42.dp

    val shapeCard   = RoundedCornerShape(radiusCard)
    val shapeButton = RoundedCornerShape(radiusButton)
    val shapeInner  = RoundedCornerShape(radiusInner)
    val shapeBadge  = RoundedCornerShape(radiusBadge)
}

@Composable
fun PultTheme(content: @Composable () -> Unit) {
    val colorScheme = darkColorScheme(
        background = PultColors.background,
        surface = PultColors.surface,
        surfaceVariant = PultColors.surfaceLight,
        primary = PultColors.accent,
        onBackground = PultColors.textPrimary,
        onSurface = PultColors.textPrimary,
        outline = PultColors.border
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

package com.sailxx.pult

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class PultThemePalette(
    val name: String,
    val backgroundBase: Color,
    val gradientA: Color,
    val gradientB: Color,
    val gradientC: Color,
    val accent: Color,
    val live: Color,
    val surface: Color,
    val surfaceLight: Color,
    val border: Color,
    val textPrimary: Color = Color(0xFFEEF3F1),
    val textSecondary: Color = Color(0xFFA0AFAF),
    val textTertiary: Color = Color(0xFF758585),
    val danger: Color = Color(0xFFEF4444)
)

object PultPalettes {
    val DarkMinimal = PultThemePalette(
        name = "dark",
        backgroundBase = Color(0xFF0C0E12),
        gradientA = Color(0xFF14171F),
        gradientB = Color(0xFF101216),
        gradientC = Color(0xFF181B22),
        accent = Color(0xFF4CC2FF),
        live = Color(0xFF38BDF8),
        surface = Color(0x18FFFFFF),
        surfaceLight = Color(0x28FFFFFF),
        border = Color(0x1FFFFFFF),
        textPrimary = Color(0xFFF5F7FA),
        textSecondary = Color(0xFF9CA3AF),
        textTertiary = Color(0xFF6B7280)
    )

    val Aurora = PultThemePalette(
        name = "aurora",
        backgroundBase = Color(0xFF101719),
        gradientA = Color(0xFF71A78A),
        gradientB = Color(0xFF274268),
        gradientC = Color(0xFF273F45),
        accent = Color(0xFFB4EDCC),
        live = Color(0xFFA3E6BE),
        surface = Color(0xE61B2629),
        surfaceLight = Color(0xFF263438),
        border = Color(0xFF354449)
    )

    val Dusk = PultThemePalette(
        name = "dusk",
        backgroundBase = Color(0xFF171228),
        gradientA = Color(0xFF8B578F),
        gradientB = Color(0xFF3B5A90),
        gradientC = Color(0xFFAF7FA0),
        accent = Color(0xFFE7D4FF),
        live = Color(0xFFD1B3F3),
        surface = Color(0xE6231B38),
        surfaceLight = Color(0xFF31274C),
        border = Color(0xFF463A68)
    )

    val Ember = PultThemePalette(
        name = "ember",
        backgroundBase = Color(0xFF221615),
        gradientA = Color(0xFFA45F43),
        gradientB = Color(0xFF5B3F6C),
        gradientC = Color(0xFFC19E75),
        accent = Color(0xFFF8DCC0),
        live = Color(0xFFEBC4A1),
        surface = Color(0xE62E1E1C),
        surfaceLight = Color(0xFF3D2A28),
        border = Color(0xFF593F3C)
    )

    val Midnight = PultThemePalette(
        name = "midnight",
        backgroundBase = Color(0xFF090E17),
        gradientA = Color(0xFF283852),
        gradientB = Color(0xFF151F35),
        gradientC = Color(0xFF54617A),
        accent = Color(0xFFD0E2FF),
        live = Color(0xFFAFC7E7),
        surface = Color(0xE6131B2A),
        surfaceLight = Color(0xFF1B263B),
        border = Color(0xFF2D3C59)
    )

    val Oled = PultThemePalette(
        name = "oled",
        backgroundBase = Color(0xFF000000),
        gradientA = Color(0xFF0C0E14),
        gradientB = Color(0xFF141722),
        gradientC = Color(0xFF1A1F2C),
        accent = Color(0xFF38BDF8),
        live = Color(0xFF22C55E),
        surface = Color(0xFF0C0E14),
        surfaceLight = Color(0xFF141722),
        border = Color(0xFF1F2432)
    )

    fun get(name: String): PultThemePalette = when (name.lowercase()) {
        "aurora" -> Aurora
        "dusk" -> Dusk
        "ember" -> Ember
        "midnight" -> Midnight
        "oled" -> Oled
        else -> DarkMinimal
    }
}

val LocalPultPalette = staticCompositionLocalOf { PultPalettes.DarkMinimal }

/**
 * Динамический доступ к цветам активной темы Pult.
 * Автоматически адаптируется под выбранную палитру (Aurora, Dusk, Ember, Midnight, OLED).
 */
object PultColors {
    val background: Color @Composable get() = LocalPultPalette.current.backgroundBase
    val surface: Color @Composable get() = LocalPultPalette.current.surface
    val surfaceLight: Color @Composable get() = LocalPultPalette.current.surfaceLight
    val border: Color @Composable get() = LocalPultPalette.current.border
    val accent: Color @Composable get() = LocalPultPalette.current.accent
    val success: Color @Composable get() = LocalPultPalette.current.live
    val danger: Color @Composable get() = LocalPultPalette.current.danger
    val textPrimary: Color @Composable get() = LocalPultPalette.current.textPrimary
    val textSecondary: Color @Composable get() = LocalPultPalette.current.textSecondary
    val textTertiary: Color @Composable get() = LocalPultPalette.current.textTertiary
}

object PultDimens {
    val radiusCard   = 18.dp
    val radiusButton = 12.dp
    val radiusTile   = 14.dp
    val radiusInner  = 10.dp
    val radiusPill   = 24.dp
    val radiusBadge  = 8.dp
    val headerHeight = 40.dp

    val shapeCard   = RoundedCornerShape(radiusCard)
    val shapeButton = RoundedCornerShape(radiusButton)
    val shapeTile   = RoundedCornerShape(radiusTile)
    val shapeInner  = RoundedCornerShape(radiusInner)
    val shapePill   = RoundedCornerShape(radiusPill)
    val shapeBadge  = RoundedCornerShape(radiusBadge)
}

@Composable
fun PultTheme(
    palette: PultThemePalette = PultPalettes.DarkMinimal,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        background = palette.backgroundBase,
        surface = palette.surface,
        surfaceVariant = palette.surfaceLight,
        primary = palette.accent,
        onBackground = palette.textPrimary,
        onSurface = palette.textPrimary,
        outline = palette.border
    )

    CompositionLocalProvider(LocalPultPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

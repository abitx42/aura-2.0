package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Immutable
data class AuraCustomColors(
    val positiveGreen: Color,
    val positiveGreenContainer: Color,
    val negativeRed: Color,
    val negativeRedContainer: Color,
    val badgeGold: Color,
    val badgeGoldContainer: Color,
    val accentBrand: Color,
    val screenBackground: Color,
    val cardBackground: Color,
    val cardBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val bottomNavBackground: Color,
    val bottomNavActivePill: Color
) {
    val gold: Color get() = badgeGold
}

val LocalAuraColors = staticCompositionLocalOf {
    AuraCustomColors(
        positiveGreen = SemanticGreen,
        positiveGreenContainer = SemanticGreenContainer,
        negativeRed = SemanticRed,
        negativeRedContainer = SemanticRedContainer,
        badgeGold = SemanticGold,
        badgeGoldContainer = SemanticGoldContainer,
        accentBrand = RadiantOrange,
        screenBackground = DarkBackground,
        cardBackground = DarkCard,
        cardBorder = DarkCardBorder,
        textPrimary = TextWhitePrimary,
        textSecondary = TextBoneSecondary,
        textMuted = TextMutedSteel,
        bottomNavBackground = DarkSurface,
        bottomNavActivePill = DarkPillActive
    )
}

object AuraTheme {
    val colors: AuraCustomColors
        @Composable
        get() = LocalAuraColors.current
}

@Composable
fun MyApplicationTheme(
    themeMode: String = "DARK",
    themePalette: String = "RADIANT_SUNSET", // Default to Radiant Coral Sunset from reference
    content: @Composable () -> Unit
) {
    val isDark = themeMode != "LIGHT"

    // Backgrounds & Surface scale
    val (bg, surface, card, cardBorder, activePill) = when (themeMode) {
        "AMOLED" -> Quintuple(AmoledBackground, AmoledSurface, AmoledCard, AmoledCardBorder, AmoledPillActive)
        "LIGHT" -> Quintuple(LightBackground, LightSurface, LightCard, LightCardBorder, LightPillActive)
        else -> Quintuple(DarkBackground, DarkSurface, DarkCard, DarkCardBorder, DarkPillActive)
    }

    // Palettes
    val (primary, secondary, tertiary) = when (themePalette) {
        "AXIO_LIME" -> Triple(
            AxioElectricLime,
            SemanticGreen,
            SemanticGold
        )
        "CYAN_GLOW" -> Triple(
            Color(0xFF00E5FF), // Digital neon cyan
            Color(0xFF7C4DFF), // Tech purple
            Color(0xFFFFA726)  // Copper warm
        )
        "EMERALD_GARDEN" -> Triple(
            SemanticGreen,     // Mint Emerald
            Color(0xFF00B0FF), // Ocean Indigo
            SemanticGold       // Golden Plum
        )
        "ROYAL_AMETHYST" -> Triple(
            Color(0xFFBB86FC), // Orchid Purple
            Color(0xFF7C4DFF), // Tech Purple
            Color(0xFF03DAC6)  // Cool Cyan
        )
        "OCEAN_BREEZE" -> Triple(
            Color(0xFF0288D1), // Sky Blue
            Color(0xFF00E676), // Deep Green
            Color(0xFFFFD54F)  // Sand Yellow
        )
        else -> Triple( // "RADIANT_SUNSET" - Default Signature Fintech Coral Orange
            RadiantOrange,
            Color(0xFFFF5252),
            SemanticGold
        )
    }

    // Text hierarchy
    val (textPrimary, textSecondary, textMuted) = if (isDark) {
        Triple(TextWhitePrimary, TextBoneSecondary, TextMutedSteel)
    } else {
        Triple(TextDarkPrimary, TextDarkSecondary, TextDarkMuted)
    }

    // Sync legacy mutable variables for zero regression
    AuraObsidian = bg
    AuraCharcoalBase = surface
    AuraSlateCard = card
    AuraSlateLight = cardBorder
    AuraCyanNeon = primary
    AuraCyanMuted = primary.copy(alpha = 0.7f)
    AuraPurpleAccent = secondary
    AuraCopperWarm = tertiary
    AuraWhiteMedium = textSecondary
    AuraWhiteMuted = textMuted

    // Material 3 ColorScheme
    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.18f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            tertiary = tertiary,
            onTertiary = Color.Black,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = card,
            onSurfaceVariant = textSecondary,
            outline = cardBorder
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primary.copy(alpha = 0.15f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            tertiary = tertiary,
            onTertiary = Color.White,
            background = bg,
            onBackground = textPrimary,
            surface = surface,
            onSurface = textPrimary,
            surfaceVariant = card,
            onSurfaceVariant = textSecondary,
            outline = cardBorder
        )
    }

    val customColors = AuraCustomColors(
        positiveGreen = SemanticGreen,
        positiveGreenContainer = SemanticGreenContainer,
        negativeRed = SemanticRed,
        negativeRedContainer = SemanticRedContainer,
        badgeGold = SemanticGold,
        badgeGoldContainer = SemanticGoldContainer,
        accentBrand = primary,
        screenBackground = bg,
        cardBackground = card,
        cardBorder = cardBorder,
        textPrimary = textPrimary,
        textSecondary = textSecondary,
        textMuted = textMuted,
        bottomNavBackground = surface,
        bottomNavActivePill = activePill
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = bg.toArgb()
            window.navigationBarColor = surface.toArgb()
            val viewCompat = WindowCompat.getInsetsController(window, view)
            viewCompat.isAppearanceLightStatusBars = !isDark
            viewCompat.isAppearanceLightNavigationBars = !isDark
        }
    }

    CompositionLocalProvider(LocalAuraColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

private data class Quintuple<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

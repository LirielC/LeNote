package com.example.lenote.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

object LeNotePalette {
    val WarmPaperBackground = Color(0xFFF7F7F7)
    val CrispIvorySurface = Color(0xFFFFFFFF)
    val PureWhiteSurface = Color(0xFFFFFFFF)
    val LinenSurface = Color(0xFFF1F1F3)
    val StoneSurface = Color(0xFFE8E8EC)
    val HairlineBorder = Color(0xFFE4E2E6)
    val SubtleDivider = Color(0xFFE4E2E6)

    val ObsidianInk = Color(0xFF1C1B1F)
    val CharcoalText = Color(0xFF68666B)
    val MutedStoneText = Color(0xFF858289)

    val AtelierNavy = Color(0xFF315F9B)
    val AtelierNavyLight = Color(0xFF4776B3)
    val AtelierNavyContainer = Color(0xFFE5EEFA)
    val OnAtelierNavyContainer = Color(0xFF17365F)

    val TerracottaAccent = Color(0xFF8C5B4C)
    val TerracottaContainer = Color(0xFFF4E8E4)
    val OnTerracottaContainer = Color(0xFF49271E)

    val AmberGold = Color(0xFF8A641B)
    val AmberGoldContainer = Color(0xFFF7EBCB)

    val ForestSuccess = Color(0xFF2F6848)
    val ForestSuccessContainer = Color(0xFFE1F1E7)

    val CrimsonError = Color(0xFFBA1A1A)
    val CrimsonErrorContainer = Color(0xFFFFDAD6)

    val NocturnalBackground = Color(0xFF101012)
    val NocturnalSurface = Color(0xFF1B1B1E)
    val NocturnalElevated = Color(0xFF242428)
    val NocturnalContainer = Color(0xFF2B2B30)
    val NocturnalBorder = Color(0xFF323238)
    val ParchmentText = Color(0xFFF1F0F4)
    val MutedParchmentText = Color(0xFFAAA7AF)
    val NocturnalNavyAccent = Color(0xFFA8C7FA)
    val NocturnalNavyContainer = Color(0xFF28466F)

    val EditorialCoverPalette = listOf(
        Color(0xFF315F9B),
        Color(0xFF35665A),
        Color(0xFF925044),
        Color(0xFF54565B),
        Color(0xFF8A641B),
        Color(0xFF72526F),
        Color(0xFF44634D),
        Color(0xFF71685E)
    )
}

@Immutable
data class LeNoteExtendedColors(
    val paperBackground: Color,
    val elevatedSurface: Color,
    val tonalSurface: Color,
    val subtleSurface: Color,
    val hairlineBorder: Color,
    val subtleDivider: Color,
    val inkPrimary: Color,
    val inkSecondary: Color,
    val inkMuted: Color,
    val accentPrimary: Color,
    val accentPrimaryContainer: Color,
    val accentWarm: Color,
    val accentWarmContainer: Color,
    val goldStar: Color,
    val success: Color,
    val successContainer: Color,
    val isDark: Boolean
)

val LightExtendedColors = LeNoteExtendedColors(
    paperBackground = LeNotePalette.WarmPaperBackground,
    elevatedSurface = LeNotePalette.PureWhiteSurface,
    tonalSurface = LeNotePalette.LinenSurface,
    subtleSurface = LeNotePalette.StoneSurface,
    hairlineBorder = LeNotePalette.HairlineBorder,
    subtleDivider = LeNotePalette.SubtleDivider,
    inkPrimary = LeNotePalette.ObsidianInk,
    inkSecondary = LeNotePalette.CharcoalText,
    inkMuted = LeNotePalette.MutedStoneText,
    accentPrimary = LeNotePalette.AtelierNavy,
    accentPrimaryContainer = LeNotePalette.AtelierNavyContainer,
    accentWarm = LeNotePalette.TerracottaAccent,
    accentWarmContainer = LeNotePalette.TerracottaContainer,
    goldStar = LeNotePalette.AmberGold,
    success = LeNotePalette.ForestSuccess,
    successContainer = LeNotePalette.ForestSuccessContainer,
    isDark = false
)

val DarkExtendedColors = LeNoteExtendedColors(
    paperBackground = LeNotePalette.NocturnalBackground,
    elevatedSurface = LeNotePalette.NocturnalSurface,
    tonalSurface = LeNotePalette.NocturnalElevated,
    subtleSurface = LeNotePalette.NocturnalContainer,
    hairlineBorder = LeNotePalette.NocturnalBorder,
    subtleDivider = LeNotePalette.NocturnalBorder,
    inkPrimary = LeNotePalette.ParchmentText,
    inkSecondary = LeNotePalette.MutedParchmentText,
    inkMuted = Color(0xFF85818A),
    accentPrimary = LeNotePalette.NocturnalNavyAccent,
    accentPrimaryContainer = LeNotePalette.NocturnalNavyContainer,
    accentWarm = Color(0xFFE2A38F),
    accentWarmContainer = Color(0xFF543127),
    goldStar = Color(0xFFE7C06C),
    success = Color(0xFF9AD4B0),
    successContainer = Color(0xFF254B35),
    isDark = true
)

val LocalLeNoteColors = staticCompositionLocalOf { LightExtendedColors }

val LeNoteLightColorScheme = lightColorScheme(
    primary = LeNotePalette.AtelierNavy,
    onPrimary = Color.White,
    primaryContainer = LeNotePalette.AtelierNavyContainer,
    onPrimaryContainer = LeNotePalette.OnAtelierNavyContainer,
    secondary = LeNotePalette.TerracottaAccent,
    onSecondary = Color.White,
    secondaryContainer = LeNotePalette.TerracottaContainer,
    onSecondaryContainer = LeNotePalette.OnTerracottaContainer,
    tertiary = LeNotePalette.AmberGold,
    onTertiary = Color.White,
    tertiaryContainer = LeNotePalette.AmberGoldContainer,
    onTertiaryContainer = Color(0xFF2D1F00),
    background = LeNotePalette.WarmPaperBackground,
    onBackground = LeNotePalette.ObsidianInk,
    surface = LeNotePalette.CrispIvorySurface,
    onSurface = LeNotePalette.ObsidianInk,
    surfaceVariant = LeNotePalette.LinenSurface,
    onSurfaceVariant = LeNotePalette.CharcoalText,
    surfaceContainerLowest = LeNotePalette.PureWhiteSurface,
    surfaceContainerLow = LeNotePalette.CrispIvorySurface,
    surfaceContainer = LeNotePalette.LinenSurface,
    surfaceContainerHigh = LeNotePalette.StoneSurface,
    surfaceContainerHighest = LeNotePalette.StoneSurface,
    outline = Color(0xFF77767A),
    outlineVariant = LeNotePalette.SubtleDivider,
    error = LeNotePalette.CrimsonError,
    onError = Color.White,
    errorContainer = LeNotePalette.CrimsonErrorContainer,
    onErrorContainer = Color(0xFF410002)
)

val LeNoteDarkColorScheme = darkColorScheme(
    primary = LeNotePalette.NocturnalNavyAccent,
    onPrimary = Color(0xFF0B305F),
    primaryContainer = LeNotePalette.NocturnalNavyContainer,
    onPrimaryContainer = Color(0xFFD8E7FF),
    secondary = Color(0xFFE2A38F),
    onSecondary = Color(0xFF4A261C),
    secondaryContainer = Color(0xFF543127),
    onSecondaryContainer = Color(0xFFFFDBCF),
    tertiary = Color(0xFFE7C06C),
    onTertiary = Color(0xFF3D2E00),
    tertiaryContainer = Color(0xFF574500),
    onTertiaryContainer = Color(0xFFFFE08F),
    background = LeNotePalette.NocturnalBackground,
    onBackground = LeNotePalette.ParchmentText,
    surface = LeNotePalette.NocturnalSurface,
    onSurface = LeNotePalette.ParchmentText,
    surfaceVariant = LeNotePalette.NocturnalElevated,
    onSurfaceVariant = LeNotePalette.MutedParchmentText,
    surfaceContainerLowest = Color(0xFF0B0B0D),
    surfaceContainerLow = LeNotePalette.NocturnalSurface,
    surfaceContainer = LeNotePalette.NocturnalElevated,
    surfaceContainerHigh = LeNotePalette.NocturnalContainer,
    surfaceContainerHighest = Color(0xFF36363C),
    outline = Color(0xFF949197),
    outlineVariant = LeNotePalette.NocturnalBorder,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

package com.example.lenote.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.ui.PhosphorIcons

/**
 * ============================================================================
 * LENOTE EDITORIAL DESIGN SYSTEM ("Warm Paper & Editorial Ink")
 * ============================================================================
 * Direção Visual:
 * - Premium minimalista, sofisticação visual e design editorial de ateliê.
 * - Superfícies inspiradas em papel algodão quente (Warm Alabaster & Linen).
 * - Tipografia com forte hierarquia (Serif para títulos editoriais, Sans para UI,
 *   Monospace para metadados/índices).
 * - Bordas hairline (1.dp) intencionais em vez de excesso de sombras ou gradientes.
 */

object LeNotePalette {
    // Superfícies Light (Atelier Paper)
    val WarmPaperBackground = Color(0xFFF6F4EE)
    val CrispIvorySurface = Color(0xFFFCFBF8)
    val PureWhiteSurface = Color(0xFFFFFFFF)
    val LinenSurface = Color(0xFFEEEAE0)
    val StoneSurface = Color(0xFFE5E0D4)
    val HairlineBorder = Color(0xFFDCD6C8)
    val SubtleDivider = Color(0xFFE6E1D6)

    // Tipografia e Tinta (Light)
    val ObsidianInk = Color(0xFF161514)
    val CharcoalText = Color(0xFF4F4C46)
    val MutedStoneText = Color(0xFF7D786F)

    // Acentos Editoriais
    val AtelierNavy = Color(0xFF1B365D)
    val AtelierNavyLight = Color(0xFF2B4C7E)
    val AtelierNavyContainer = Color(0xFFE4EAF2)
    val OnAtelierNavyContainer = Color(0xFF102340)

    val TerracottaAccent = Color(0xFFB85435)
    val TerracottaContainer = Color(0xFFF7E7E2)
    val OnTerracottaContainer = Color(0xFF5C2210)

    val AmberGold = Color(0xFFC88224)
    val AmberGoldContainer = Color(0xFFFAF0DE)

    val ForestSuccess = Color(0xFF1F5E3B)
    val ForestSuccessContainer = Color(0xFFE3F0E8)

    val CrimsonError = Color(0xFF9E2A2B)
    val CrimsonErrorContainer = Color(0xFFF9E6E6)

    // Superfícies Dark (Nocturnal Studio)
    val NocturnalBackground = Color(0xFF121211)
    val NocturnalSurface = Color(0xFF1B1B19)
    val NocturnalElevated = Color(0xFF242320)
    val NocturnalContainer = Color(0xFF2D2C28)
    val NocturnalBorder = Color(0xFF383631)
    val ParchmentText = Color(0xFFF2EFE9)
    val MutedParchmentText = Color(0xFFA8A39A)
    val NocturnalNavyAccent = Color(0xFF90B4E8)
    val NocturnalNavyContainer = Color(0xFF1E324D)

    // Paleta Curada para Capas de Cadernos (Tons Editoriais Encadernados)
    val EditorialCoverPalette = listOf(
        Color(0xFF1B365D), // Atelier Navy
        Color(0xFF224B45), // Botanical Pine
        Color(0xFF7C3225), // Venetian Terracotta
        Color(0xFF36393B), // Graphite Monolith
        Color(0xFF7D5626), // Raw Umber / Ochre
        Color(0xFF4A314D), // Aubergine Ink
        Color(0xFF2A4736), // British Racing Green
        Color(0xFF5C5346)  // Warm Taupe Linen
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
    inkMuted = Color(0xFF78746C),
    accentPrimary = LeNotePalette.NocturnalNavyAccent,
    accentPrimaryContainer = LeNotePalette.NocturnalNavyContainer,
    accentWarm = Color(0xFFE08868),
    accentWarmContainer = Color(0xFF472317),
    goldStar = Color(0xFFE8A848),
    success = Color(0xFF68C28D),
    successContainer = Color(0xFF183B28),
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
    onTertiaryContainer = Color(0xFF422905),
    background = LeNotePalette.WarmPaperBackground,
    onBackground = LeNotePalette.ObsidianInk,
    surface = LeNotePalette.CrispIvorySurface,
    onSurface = LeNotePalette.ObsidianInk,
    surfaceVariant = LeNotePalette.LinenSurface,
    onSurfaceVariant = LeNotePalette.CharcoalText,
    surfaceContainerLowest = LeNotePalette.PureWhiteSurface,
    surfaceContainerLow = LeNotePalette.CrispIvorySurface,
    surfaceContainer = LeNotePalette.LinenSurface,
    surfaceContainerHigh = LeNotePalette.LinenSurface,
    surfaceContainerHighest = LeNotePalette.StoneSurface,
    outline = LeNotePalette.HairlineBorder,
    outlineVariant = LeNotePalette.SubtleDivider,
    error = LeNotePalette.CrimsonError,
    onError = Color.White,
    errorContainer = LeNotePalette.CrimsonErrorContainer,
    onErrorContainer = LeNotePalette.CrimsonError
)

val LeNoteDarkColorScheme = darkColorScheme(
    primary = LeNotePalette.NocturnalNavyAccent,
    onPrimary = Color(0xFF0A192F),
    primaryContainer = LeNotePalette.NocturnalNavyContainer,
    onPrimaryContainer = Color(0xFFD6E4F8),
    secondary = Color(0xFFE08868),
    onSecondary = Color(0xFF3B180B),
    secondaryContainer = Color(0xFF472317),
    onSecondaryContainer = Color(0xFFFADCD2),
    tertiary = Color(0xFFE8A848),
    onTertiary = Color(0xFF382304),
    tertiaryContainer = Color(0xFF4F3409),
    onTertiaryContainer = Color(0xFFFCE8C8),
    background = LeNotePalette.NocturnalBackground,
    onBackground = LeNotePalette.ParchmentText,
    surface = LeNotePalette.NocturnalSurface,
    onSurface = LeNotePalette.ParchmentText,
    surfaceVariant = LeNotePalette.NocturnalElevated,
    onSurfaceVariant = LeNotePalette.MutedParchmentText,
    surfaceContainerLowest = Color(0xFF0D0D0C),
    surfaceContainerLow = LeNotePalette.NocturnalSurface,
    surfaceContainer = LeNotePalette.NocturnalElevated,
    surfaceContainerHigh = LeNotePalette.NocturnalContainer,
    surfaceContainerHighest = Color(0xFF363430),
    outline = LeNotePalette.NocturnalBorder,
    outlineVariant = Color(0xFF2D2B27),
    error = Color(0xFFE57373),
    onError = Color(0xFF421010),
    errorContainer = Color(0xFF4F1919),
    onErrorContainer = Color(0xFFF9D6D6)
)

/**
 * Tipografia Editorial LeNote:
 * - Títulos de destaque em Serif com espaçamento refinado (aspecto de publicação impressa / caderno literário).
 * - Corpo e controles em Sans-Serif limpa e legível.
 * - Índices, contadores e carimbos de tempo com espaçamento estruturado.
 */
val LeNoteTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 1.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

object LeNoteTheme {
    val colors: LeNoteExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalLeNoteColors.current
}

@Composable
fun LeNoteTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) LeNoteDarkColorScheme else LeNoteLightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLeNoteColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = LeNoteTypography,
            content = content
        )
    }
}

/**
 * ============================================================================
 * REUSABLE EDITORIAL DESIGN SYSTEM COMPONENTS (Rule 8 & Rule 10)
 * ============================================================================
 */

/**
 * Cabeçalho de seção editorial com tipografia small-caps espaçada, linha sutil
 * e contador ou ação opcional à direita.
 */
@Composable
fun EditorialSectionHeader(
    title: String,
    count: Int? = null,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = LeNoteTheme.colors.inkSecondary
            )
            if (count != null) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = LeNoteTheme.colors.tonalSurface,
                    border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = LeNoteTheme.colors.inkPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
            if (subtitle != null) {
                Text(
                    text = "•  $subtitle",
                    style = MaterialTheme.typography.bodySmall,
                    color = LeNoteTheme.colors.inkMuted
                )
            }
        }
        if (trailingContent != null) {
            trailingContent()
        }
    }
}

/**
 * Barra de busca editorial responsiva com borda hairline 1.dp e transição suave de foco.
 */
@Composable
fun EditorialSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val isActive = query.isNotEmpty()
    val borderColor by animateColorAsState(
        targetValue = if (isActive) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.hairlineBorder,
        animationSpec = tween(200),
        label = "SearchBorderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isActive) LeNoteTheme.colors.elevatedSurface else LeNoteTheme.colors.tonalSurface.copy(alpha = 0.65f),
        animationSpec = tween(200),
        label = "SearchBgColor"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                contentDescription = "Buscar",
                tint = if (isActive) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkMuted,
                modifier = Modifier.size(17.dp)
            )
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LeNoteTheme.colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = LeNoteTheme.colors.inkPrimary
                    ),
                    cursorBrush = SolidColor(LeNoteTheme.colors.accentPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = "Limpar busca",
                        tint = LeNoteTheme.colors.inkSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Item de filtro segmentado editorial.
 */
data class EditorialFilterTab(
    val label: String,
    val count: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

/**
 * Seletor segmentado customizado de estética editorial (substitui o TabRow padrão do Material).
 */
@Composable
fun EditorialSegmentedBar(
    tabs: List<EditorialFilterTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = LeNoteTheme.colors.tonalSurface,
        border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedIndex == index
                val bgColor by animateColorAsState(
                    targetValue = if (isSelected) LeNoteTheme.colors.elevatedSurface else Color.Transparent,
                    animationSpec = tween(180),
                    label = "SegmentBg"
                )
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) LeNoteTheme.colors.inkPrimary else LeNoteTheme.colors.inkSecondary,
                    animationSpec = tween(180),
                    label = "SegmentContent"
                )
                val borderStroke = if (isSelected) {
                    BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
                } else null

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .clickable { onSelect(index) },
                    shape = RoundedCornerShape(7.dp),
                    color = bgColor,
                    border = borderStroke
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSelected) tab.selectedIcon else tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) LeNoteTheme.colors.accentPrimary else contentColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 12.sp
                            ),
                            color = contentColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "${tab.count}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkMuted
                        )
                    }
                }
            }
        }
    }
}

/**
 * Botão utilitário compacto em formato de pílula ou quadrado arredondado com microinteração de escala.
 */
@Composable
fun EditorialActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDestructive: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "ChipScale"
    )

    val containerColor = when {
        isDestructive -> MaterialTheme.colorScheme.errorContainer
        isPrimary -> LeNoteTheme.colors.inkPrimary
        else -> LeNoteTheme.colors.elevatedSurface
    }
    val contentColor = when {
        isDestructive -> MaterialTheme.colorScheme.error
        isPrimary -> LeNoteTheme.colors.paperBackground
        else -> LeNoteTheme.colors.inkPrimary
    }
    val border = when {
        isPrimary -> null
        isDestructive -> BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        else -> BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
    }

    Surface(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

/**
 * Estados explícitos de UI (Rule 10): Loading, Error, Empty, Success.
 */
enum class EditorialFeedbackType {
    LOADING,
    SUCCESS,
    ERROR
}

data class EditorialFeedbackBannerState(
    val type: EditorialFeedbackType,
    val title: String,
    val message: String? = null
)

@Composable
fun EditorialStatusBanner(
    state: EditorialFeedbackBannerState,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val bgColor = when (state.type) {
        EditorialFeedbackType.LOADING -> LeNoteTheme.colors.accentPrimaryContainer
        EditorialFeedbackType.SUCCESS -> LeNoteTheme.colors.successContainer
        EditorialFeedbackType.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    val accentColor = when (state.type) {
        EditorialFeedbackType.LOADING -> LeNoteTheme.colors.accentPrimary
        EditorialFeedbackType.SUCCESS -> LeNoteTheme.colors.success
        EditorialFeedbackType.ERROR -> MaterialTheme.colorScheme.error
    }
    val icon = when (state.type) {
        EditorialFeedbackType.LOADING -> null
        EditorialFeedbackType.SUCCESS -> PhosphorIcons.Fill.CheckCircle
        EditorialFeedbackType.ERROR -> PhosphorIcons.Fill.WarningCircle
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = bgColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.type == EditorialFeedbackType.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = LeNoteTheme.colors.inkPrimary
                )
                if (!state.message.isNullOrBlank()) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = LeNoteTheme.colors.inkSecondary
                    )
                }
            }

            if (onDismiss != null && state.type != EditorialFeedbackType.LOADING) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = "Fechar aviso",
                        tint = LeNoteTheme.colors.inkSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Componente reutilizável para Estados Vazios Editoriais (Rule 8 & Rule 10).
 */
@Composable
fun EditorialEmptyState(
    icon: ImageVector,
    overline: String,
    title: String,
    description: String,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = LeNoteTheme.colors.tonalSurface,
            border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LeNoteTheme.colors.inkSecondary,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Text(
            text = overline.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = LeNoteTheme.colors.inkMuted
        )

        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = LeNoteTheme.colors.inkPrimary,
            textAlign = TextAlign.Center
        )

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = LeNoteTheme.colors.inkSecondary,
            textAlign = TextAlign.Center
        )

        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(4.dp))
            EditorialActionChip(
                label = actionLabel,
                icon = actionIcon ?: PhosphorIcons.Regular.Plus,
                onClick = onActionClick,
                isPrimary = true
            )
        }
    }
}

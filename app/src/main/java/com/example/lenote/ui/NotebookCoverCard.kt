package com.example.lenote.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.data.NotebookEntity
import com.example.lenote.ui.theme.LeNotePalette
import com.example.lenote.ui.theme.LeNoteTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal val NotebookCoverColors = LeNotePalette.EditorialCoverPalette

/**
 * Card de Caderno com estética de Monografia / Caderno Encadernado de Ateliê (Hardcover Folio):
 * - Cantos assimétricos de livro encadernado (lombada esquerda 4.dp, corte direito 12.dp);
 * - Lombada em tecido fosco com vinco sutil de encadernação (sem gradientes plásticos exagerados);
 * - Placa tipográfica editorial em papel marfim com título em Serif, índice de seções/páginas e menu discreto;
 * - Microinteração natural de escala ao toque.
 */
@Composable
internal fun NotebookCoverCard(
    notebook: NotebookEntity,
    pageCount: Int,
    sectionCount: Int,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onMoveToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LeNoteTheme.strings
    val coverColor = remember(notebook.colorHex) {
        when (notebook.colorHex.toLong() and 0xFFFFFFFFL) {
            0xFF1976D2L -> Color(0xFF1E3A5F) // Azul Atelier Profundo
            0xFF388E3CL -> Color(0xFF2D4A3E) // Verde Floresta Editorial
            0xFFD32F2FL -> Color(0xFF6B2D2C) // Borgonha Clássico
            0xFF7B1FA2L -> Color(0xFF4A354F) // Ameixa Escuro
            0xFFF57C00L -> Color(0xFF8C4A27) // Terracota Queimado
            0xFF455A64L -> Color(0xFF364147) // Ardósia Grafite
            else -> Color(notebook.colorHex)
        }
    }
    val dateFormat = remember(s.language) { SimpleDateFormat("dd MMM yyyy", s.language.locale) }
    val lastUpdateStr = remember(notebook.updatedAt, s.language) {
        dateFormat.format(Date(notebook.updatedAt)).uppercase(s.language.locale)
    }
    var showMenu by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "BookCoverScale"
    )

    val bookShape = RoundedCornerShape(
        topStart = 4.dp,
        bottomStart = 4.dp,
        topEnd = 12.dp,
        bottomEnd = 12.dp
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .aspectRatio(0.76f)
            .shadow(
                elevation = 2.dp,
                shape = bookShape,
                ambientColor = Color.Black.copy(alpha = 0.15f),
                spotColor = Color.Black.copy(alpha = 0.15f)
            )
            .clip(bookShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = bookShape,
        color = coverColor,
        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.18f))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Lombada Encadernada (Cloth Spine) com vinco sutil de 1.dp
            Row(
                modifier = Modifier
                    .width(14.dp)
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.22f))
            ) {
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.14f))
                )
            }

            // Capa principal do caderno
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Placa Editorial Superior (Ivory Label Plate)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = LeNotePalette.CrispIvorySurface,
                    border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 9.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.cardNotebookOverline,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp
                                ),
                                color = LeNotePalette.MutedStoneText
                            )

                            Box {
                                IconButton(
                                    onClick = { showMenu = true },
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.DotsThreeVertical,
                                        contentDescription = s.menuNotebookOptionsDesc,
                                        tint = LeNotePalette.CharcoalText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                    containerColor = LeNoteTheme.colors.elevatedSurface
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                s.menuExportLenote,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = PhosphorIcons.Regular.Export,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onExport()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                s.menuMoveToTrash,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = PhosphorIcons.Regular.Trash,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        onClick = {
                                            showMenu = false
                                            onMoveToTrash()
                                        }
                                    )
                                }
                            }
                        }

                        Text(
                            text = notebook.title,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 20.sp
                            ),
                            color = LeNotePalette.ObsidianInk,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(2.dp))
                        HorizontalDivider(color = LeNotePalette.HairlineBorder)

                        Text(
                            text = s.cardNotebookStats(sectionCount, pageCount),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = LeNotePalette.CharcoalText
                        )
                    }
                }

                // Rodapé Editorial da Capa (Data + Seta de abertura)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastUpdateStr,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = Color.White.copy(alpha = 0.82f)
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier.size(22.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.CaretRight,
                                contentDescription = s.openNotebookDesc,
                                tint = Color.White.copy(alpha = 0.92f),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

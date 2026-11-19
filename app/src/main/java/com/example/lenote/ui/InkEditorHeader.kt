package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.data.PageEntity
import com.example.lenote.ui.theme.LeNoteTheme

/**
 * Cabeçalho Editorial Compacto (altura 50dp):
 * Esquerda: Voltar para Biblioteca, Gaveta de Cadernos/Páginas, Título editável da folha, Estrela de favorito.
 * Direita: Botão rápido de modelo de folha, Indicador discreto de autosave e Menu "Mais" (⋮).
 */
@Composable
internal fun CompactHeaderBar(
    currentPage: PageEntity?,
    pageIndexLabel: String?,
    isSaving: Boolean,
    saveStatus: String?,
    onNavigateBack: (() -> Unit)?,
    onToggleSidebar: () -> Unit,
    onRenamePageClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSheetOptions: () -> Unit,
    onOpenMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LeNoteTheme.strings
    Column(modifier = modifier.statusBarsPadding()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            color = LeNoteTheme.colors.paperBackground.copy(alpha = 0.96f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lado Esquerdo: Voltar + Gaveta + Título + Favorito
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.CaretLeft,
                                contentDescription = s.backToLibraryDesc,
                                tint = LeNoteTheme.colors.inkPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Botão da Gaveta de Navegação (Sumário / Páginas)
                    Surface(
                        onClick = onToggleSidebar,
                        shape = RoundedCornerShape(8.dp),
                        color = LeNoteTheme.colors.elevatedSurface,
                        border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Notebook,
                                contentDescription = s.notebookIndexDesc,
                                tint = LeNoteTheme.colors.accentPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = pageIndexLabel ?: s.indexDefaultLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = LeNoteTheme.colors.inkPrimary
                            )
                        }
                    }

                    // Título da Página com ação de renomear
                    Row(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onRenamePageClick() }
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currentPage?.title ?: s.defaultPageTitle(1),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 15.sp,
                                lineHeight = 19.sp
                            ),
                            color = LeNoteTheme.colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Icon(
                            imageVector = PhosphorIcons.Regular.PencilSimple,
                            contentDescription = s.renamePageDesc,
                            tint = LeNoteTheme.colors.inkMuted,
                            modifier = Modifier.size(12.dp)
                        )
                    }

                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(28.dp)
                    ) {
                        val isFav = currentPage?.isFavorite == true
                        Icon(
                            imageVector = if (isFav) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                            contentDescription = if (isFav) s.unfavoritePageDesc else s.favoritePageDesc,
                            tint = if (isFav) LeNoteTheme.colors.goldStar else LeNoteTheme.colors.inkMuted,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                // Lado Direito: Status de Salvamento + Papel + Menu Mais (⋮)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = LeNoteTheme.colors.tonalSurface.copy(alpha = 0.7f),
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isSaving) {
                                Text(
                                    text = s.statusSaving,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = LeNoteTheme.colors.inkSecondary
                                )
                            } else {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Check,
                                    contentDescription = null,
                                    tint = LeNoteTheme.colors.success,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = s.statusSaved,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                    color = LeNoteTheme.colors.inkSecondary
                                )
                            }
                        }
                    }

                    // Atalho direto para Modelo/Cor do Papel
                    IconButton(
                        onClick = onOpenSheetOptions,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.SlidersHorizontal,
                            contentDescription = s.sheetTemplateAndColorDesc,
                            tint = LeNoteTheme.colors.inkSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenMoreOptions,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.DotsThreeVertical,
                            contentDescription = s.morePageOptionsDesc,
                            tint = LeNoteTheme.colors.inkPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(color = LeNoteTheme.colors.hairlineBorder)
    }
}

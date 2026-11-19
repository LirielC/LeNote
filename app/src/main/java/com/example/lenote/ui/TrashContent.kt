package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity
import com.example.lenote.ui.theme.EditorialActionChip
import com.example.lenote.ui.theme.EditorialEmptyState
import com.example.lenote.ui.theme.EditorialSectionHeader
import com.example.lenote.ui.theme.LeNoteTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Componente que exibe itens excluídos (Cadernos e Páginas) com opções de Restaurar e Excluir Definitivamente.
 */
@Composable
fun TrashContent(
    deletedNotebooks: List<NotebookEntity>,
    deletedPages: List<PageEntity>,
    onRestoreNotebook: (NotebookEntity) -> Unit,
    onDeleteNotebookPermanently: (NotebookEntity) -> Unit,
    onRestorePage: (PageEntity) -> Unit,
    onDeletePagePermanently: (PageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LeNoteTheme.strings
    val dateFormat = remember(s.language) { SimpleDateFormat("dd/MM/yyyy HH:mm", s.language.locale) }

    if (deletedNotebooks.isEmpty() && deletedPages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EditorialEmptyState(
                icon = PhosphorIcons.Regular.Trash,
                overline = s.emptyTrashOverline,
                title = s.emptyTrashTitle,
                description = s.emptyTrashDescription
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 92.dp)
        ) {
            if (deletedNotebooks.isNotEmpty()) {
                item {
                    EditorialSectionHeader(
                        title = s.sectionDeletedNotebooks,
                        count = deletedNotebooks.size
                    )
                }
                items(deletedNotebooks, key = { "nb_${it.id}" }) { nb ->
                    val delDate = remember(nb.deletedAt, s.language) {
                        nb.deletedAt?.let { dateFormat.format(Date(it)) } ?: s.unknownDate
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = LeNoteTheme.colors.elevatedSurface,
                        border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(Color(nb.colorHex), RoundedCornerShape(3.dp))
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = nb.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = LeNoteTheme.colors.inkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = s.deletedOnDate(delDate),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LeNoteTheme.colors.inkMuted
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EditorialActionChip(
                                    label = s.actionRestore,
                                    icon = PhosphorIcons.Regular.ArrowCounterClockwise,
                                    onClick = { onRestoreNotebook(nb) }
                                )
                                IconButton(
                                    onClick = { onDeleteNotebookPermanently(nb) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Trash,
                                        contentDescription = s.deletePermanentlyDesc,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (deletedPages.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    EditorialSectionHeader(
                        title = s.sectionDeletedPages,
                        count = deletedPages.size
                    )
                }
                items(deletedPages, key = { "pg_${it.id}" }) { page ->
                    val delDate = remember(page.deletedAt, s.language) {
                        page.deletedAt?.let { dateFormat.format(Date(it)) } ?: s.unknownDate
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = LeNoteTheme.colors.elevatedSurface,
                        border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.FileText,
                                    contentDescription = null,
                                    tint = LeNoteTheme.colors.inkSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = page.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = LeNoteTheme.colors.inkPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = s.deletedOnDate(delDate),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = LeNoteTheme.colors.inkMuted
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                EditorialActionChip(
                                    label = s.actionRestore,
                                    icon = PhosphorIcons.Regular.ArrowCounterClockwise,
                                    onClick = { onRestorePage(page) }
                                )
                                IconButton(
                                    onClick = { onDeletePagePermanently(page) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Trash,
                                        contentDescription = s.deletePermanentlyDesc,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

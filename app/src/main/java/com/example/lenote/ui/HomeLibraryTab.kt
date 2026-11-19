package com.example.lenote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity
import com.example.lenote.ui.theme.EditorialEmptyState
import com.example.lenote.ui.theme.EditorialFeedbackBannerState
import com.example.lenote.ui.theme.EditorialFeedbackType
import com.example.lenote.ui.theme.EditorialSectionHeader
import com.example.lenote.ui.theme.LeNoteTheme

@Composable
internal fun HomeLibraryTab(
    model: HomeUiModel,
    state: HomeScreenState,
    viewModel: InkPlaygroundViewModel,
    fileActions: HomeFileActions,
    onOpenNotebook: (NotebookEntity) -> Unit,
    onOpenPage: (PageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredNotebooks = model.filteredNotebooks
    val recentPages = model.recentPages
    val thumbnailVersion = model.thumbnailVersion
    val homeState = state
    val s = LeNoteTheme.strings
    Box(modifier = modifier.fillMaxWidth()) {
                // ABA BIBLIOTECA: Scroll unificado (Continuar Escrevendo + Grade 2+ colunas de Cadernos)
                if (filteredNotebooks.isEmpty() && recentPages.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (homeState.searchQuery.isNotBlank()) {
                            EditorialEmptyState(
                                icon = PhosphorIcons.Regular.MagnifyingGlass,
                                overline = s.emptySearchOverline,
                                title = s.emptySearchTitle(homeState.searchQuery),
                                description = s.emptySearchDescription,
                                actionLabel = s.actionClearSearch,
                                actionIcon = PhosphorIcons.Regular.X,
                                onActionClick = { homeState.searchQuery = "" }
                            )
                        } else {
                            EditorialEmptyState(
                                icon = PhosphorIcons.Regular.Notebook,
                                overline = s.emptyLibraryOverline,
                                title = s.emptyLibraryTitle,
                                description = s.emptyLibraryDescription,
                                actionLabel = s.actionCreateFirstNotebook,
                                actionIcon = PhosphorIcons.Regular.Plus,
                                onActionClick = { homeState.showCreateDialog = true }
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 152.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(top = 2.dp, bottom = 96.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .fillMaxWidth()
                    ) {
                        // Seção 1: Continuar Escrevendo (Carrossel Horizontal de Páginas Recentes)
                        if (recentPages.isNotEmpty()) {
                            item(
                                key = "header_recent_pages",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    EditorialSectionHeader(
                                        title = s.sectionContinueWriting,
                                        count = recentPages.size,
                                        subtitle = s.sectionContinueWritingSubtitle
                                    )

                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(vertical = 2.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(recentPages, key = { "recent_${it.id}" }) { page ->
                                            val parentNb = viewModel.getNotebookForSection(page.sectionId)
                                            RecentPageCard(
                                                page = page,
                                                notebookTitle = parentNb?.title,
                                                notebookColorHex = parentNb?.colorHex,
                                                thumbnailFile = viewModel.getThumbnailFile(page.id),
                                                thumbnailVersion = thumbnailVersion,
                                                onClick = {
                                                    viewModel.selectPage(page)
                                                    onOpenPage(page)
                                                }
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(2.dp))
                                    HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)
                                }
                            }
                        }

                        // Seção 2: Cabeçalho de Cadernos Encadernados
                        item(
                            key = "header_notebooks_grid",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            EditorialSectionHeader(
                                title = s.sectionNotebookCollection,
                                count = filteredNotebooks.size,
                                subtitle = if (homeState.searchQuery.isBlank()) s.sectionMainShelfSubtitle else s.sectionActiveFilterSubtitle
                            )
                        }

                        // Itens da Grade de Cadernos (2 colunas em smartphones, 3–4 em tablets)
                        items(filteredNotebooks, key = { "nb_grid_${it.id}" }) { nb ->
                            NotebookCoverCard(
                                notebook = nb,
                                pageCount = viewModel.getNotebookPageCount(nb.id),
                                sectionCount = viewModel.getNotebookSectionCount(nb.id),
                                onClick = {
                                    viewModel.openNotebook(nb.id)
                                    onOpenNotebook(nb)
                                },
                                onExport = {
                                    fileActions.exportNotebook(nb)
                                },
                                onMoveToTrash = {
                                    viewModel.moveNotebookToTrash(nb.id)
                                    homeState.feedbackBanner = EditorialFeedbackBannerState(
                                        type = EditorialFeedbackType.SUCCESS,
                                        title = s.bannerMovedToTrashTitle,
                                        message = s.bannerMovedToTrashMessage(nb.title)
                                    )
                                }
                            )
                        }
                    }
                }
    }
}

package com.example.lenote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity
import com.example.lenote.ui.theme.EditorialEmptyState
import com.example.lenote.ui.theme.EditorialFeedbackBannerState
import com.example.lenote.ui.theme.EditorialFeedbackType
import com.example.lenote.ui.theme.EditorialFilterTab
import com.example.lenote.ui.theme.EditorialSearchBar
import com.example.lenote.ui.theme.EditorialSectionHeader
import com.example.lenote.ui.theme.EditorialSegmentedBar
import com.example.lenote.ui.theme.EditorialStatusBanner
import com.example.lenote.ui.theme.LeNoteTheme

@Composable
internal fun HomeContent(
    model: HomeUiModel,
    state: HomeScreenState,
    viewModel: InkPlaygroundViewModel,
    fileActions: HomeFileActions,
    onOpenNotebook: (NotebookEntity) -> Unit,
    onOpenPage: (PageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val notebooks = model.notebooks
    val allPages = model.allPages
    val favoritePages = model.favoritePages
    val deletedNotebooks = model.deletedNotebooks
    val deletedPages = model.deletedPages
    val recentPages = model.recentPages
    val filteredNotebooks = model.filteredNotebooks
    val filteredFavorites = model.filteredFavorites
    val thumbnailVersion = model.thumbnailVersion
    val isDarkMode = model.isDarkMode
    val homeState = state
    val s = LeNoteTheme.strings
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = LeNoteTheme.colors.paperBackground,
        floatingActionButton = {
            when (homeState.selectedTab) {
                0 -> {
                // FAB único, elegante e sem colisão horizontal
                FloatingActionButton(
                    onClick = { homeState.showCreateDialog = true },
                    containerColor = LeNoteTheme.colors.accentPrimary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(14.dp),
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 3.dp,
                        pressedElevation = 6.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Plus,
                            contentDescription = s.fabNewNotebook,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = s.fabNewNotebook,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
                }
                2 -> {
                if (deletedNotebooks.isNotEmpty() || deletedPages.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { homeState.showEmptyTrashConfirmDialog = true },
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = Color.White,
                        shape = RoundedCornerShape(14.dp),
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Trash,
                                contentDescription = s.fabEmptyTrash,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = s.fabEmptyTrash,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            // 1. Masthead Editorial (Sobretítulo + Título em Serif + Ações Utilitárias Compactas)
            EditorialMasthead(
                notebookCount = notebooks.size,
                pageCount = allPages.size,
                isDarkMode = isDarkMode,
                onImportPdf = fileActions.importPdf,
                onImportLenote = fileActions.importLenote,
                onOpenLanguagePicker = { homeState.showLanguagePickerDialog = true },
                onToggleTheme = { viewModel.toggleDarkThemeMode() }
            )

            // Banner de Estado Explícito (Loading, Sucesso ou Erro)
            AnimatedVisibility(
                visible = homeState.feedbackBanner != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                homeState.feedbackBanner?.let { banner ->
                EditorialStatusBanner(
                    state = banner,
                    onDismiss = { homeState.feedbackBanner = null }
                )
                }
            }

            // 2. Barra de Busca e Filtro Segmentado Editorial
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorialSearchBar(
                query = homeState.searchQuery,
                onQueryChange = { homeState.searchQuery = it },
                placeholder = s.searchNotebooksPlaceholder
                )

                val trashTotal = deletedNotebooks.size + deletedPages.size
                EditorialSegmentedBar(
                tabs = listOf(
                    EditorialFilterTab(
                        label = s.tabLibrary,
                        count = notebooks.size,
                        icon = PhosphorIcons.Regular.Notebook,
                        selectedIcon = PhosphorIcons.Fill.Notebook
                    ),
                    EditorialFilterTab(
                        label = s.tabFavorites,
                        count = favoritePages.size,
                        icon = PhosphorIcons.Regular.Star,
                        selectedIcon = PhosphorIcons.Fill.Star
                    ),
                    EditorialFilterTab(
                        label = s.tabTrash,
                        count = trashTotal,
                        icon = PhosphorIcons.Regular.Trash,
                        selectedIcon = PhosphorIcons.Fill.Trash
                    )
                ),
                selectedIndex = homeState.selectedTab,
                onSelect = { homeState.selectedTab = it }
                )
            }

            // 3. Conteúdo Principal da Aba Selecionada
            when (homeState.selectedTab) {
                0 -> HomeLibraryTab(
                    model = model,
                    state = state,
                    viewModel = viewModel,
                    fileActions = fileActions,
                    onOpenNotebook = onOpenNotebook,
                    onOpenPage = onOpenPage,
                    modifier = Modifier.weight(1f)
                )

                1 -> {
                    // ABA FAVORITAS: Grade de folhas marcadas com estrela
                    FavoritesContent(
                        favoritePages = filteredFavorites,
                        getNotebookForPage = { viewModel.getNotebookForSection(it.sectionId) },
                        thumbnailVersion = thumbnailVersion,
                        getThumbnailFile = { viewModel.getThumbnailFile(it) },
                        onOpenPage = { page ->
                            viewModel.selectPage(page)
                            onOpenPage(page)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                2 -> {
                    // ABA LIXEIRA: Cadernos e páginas excluídos
                    TrashContent(
                        deletedNotebooks = deletedNotebooks,
                        deletedPages = deletedPages,
                        onRestoreNotebook = { nb ->
                            viewModel.restoreNotebookFromTrash(nb.id)
                            homeState.feedbackBanner = EditorialFeedbackBannerState(
                                type = EditorialFeedbackType.SUCCESS,
                                title = s.bannerNotebookRestoredTitle,
                                message = s.bannerNotebookRestoredMessage(nb.title)
                            )
                        },
                        onDeleteNotebookPermanently = { homeState.notebookToDeletePermanently = it },
                        onRestorePage = { pg ->
                            viewModel.restorePageFromTrash(pg.id)
                            homeState.feedbackBanner = EditorialFeedbackBannerState(
                                type = EditorialFeedbackType.SUCCESS,
                                title = s.bannerPageRestoredTitle,
                                message = s.bannerPageRestoredMessage(pg.title)
                            )
                        },
                        onDeletePagePermanently = { homeState.pageToDeletePermanently = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

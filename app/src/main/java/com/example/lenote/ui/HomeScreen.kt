package com.example.lenote.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity
import com.example.lenote.ui.theme.EditorialFeedbackType
import kotlinx.coroutines.delay

/**
 * Tela Inicial / Atelier Editorial de Cadernos (HomeScreen):
 * - Masthead editorial com sobretítulo ("ATELIER DE NOTAS"), título em Serif, sumário estatístico
 *   e ações utilitárias compactas ("PDF", ".lenote", alternância de tema);
 * - Barra de busca editorial responsiva de largura total;
 * - Filtro segmentado customizado: Biblioteca, Favoritas e Lixeira;
 * - Seção "Continuar Escrevendo" com miniaturas reais de páginas em proporção A4/caderno;
 * - Grade adaptativa 2+ colunas de cadernos encadernados (Monograph Folio) sem excesso de sombras ou gradientes artificiais;
 * - Estados explícitos de Loading, Sucesso, Erro e Vazio (Rule 10);
 * - FAB único, claro e ergonômico ("+ Novo Caderno") respeitando navigationBarsPadding.
 */
@Composable
fun HomeScreen(
    viewModel: InkPlaygroundViewModel,
    onOpenNotebook: (NotebookEntity) -> Unit,
    onOpenPage: (PageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val notebooks by viewModel.notebooks.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val favoritePages by viewModel.favoritePages.collectAsState()
    val deletedNotebooks by viewModel.deletedNotebooks.collectAsState()
    val deletedPages by viewModel.deletedPages.collectAsState()
    val thumbnailVersion by viewModel.thumbnailVersion.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val appLanguage by viewModel.appLanguage.collectAsState()
    val isDarkMode = themeMode == com.example.lenote.data.ThemeMode.DARK
    val homeState = rememberHomeScreenState()


    // Auto-dismiss para banners de sucesso após 4 segundos
    LaunchedEffect(homeState.feedbackBanner) {
        if (homeState.feedbackBanner?.type == EditorialFeedbackType.SUCCESS) {
            delay(4000)
            homeState.feedbackBanner = null
        }
    }

    val fileActions = rememberHomeFileActions(
        viewModel = viewModel,
        onOpenNotebook = onOpenNotebook,
        onFeedback = { homeState.feedbackBanner = it }
    )

    // Páginas recentes ordenadas por data de modificação
    val recentPages = remember(allPages, homeState.searchQuery) {
        val sorted = allPages.sortedByDescending { it.updatedAt }
        if (homeState.searchQuery.isBlank()) {
            sorted.take(6)
        } else {
            val q = homeState.searchQuery.trim().lowercase()
            sorted.filter { it.title.lowercase().contains(q) }
        }
    }

    // Cadernos filtrados por busca
    val filteredNotebooks = remember(notebooks, homeState.searchQuery) {
        if (homeState.searchQuery.isBlank()) notebooks
        else {
            val q = homeState.searchQuery.trim().lowercase()
            notebooks.filter { it.title.lowercase().contains(q) }
        }
    }

    // Páginas favoritas filtradas por busca
    val filteredFavorites = remember(favoritePages, homeState.searchQuery) {
        if (homeState.searchQuery.isBlank()) favoritePages
        else {
            val q = homeState.searchQuery.trim().lowercase()
            favoritePages.filter { it.title.lowercase().contains(q) }
        }
    }

    HomeContent(
        model = HomeUiModel(
            notebooks = notebooks,
            allPages = allPages,
            favoritePages = favoritePages,
            deletedNotebooks = deletedNotebooks,
            deletedPages = deletedPages,
            recentPages = recentPages,
            filteredNotebooks = filteredNotebooks,
            filteredFavorites = filteredFavorites,
            thumbnailVersion = thumbnailVersion,
            isDarkMode = isDarkMode
        ),
        state = homeState,
        viewModel = viewModel,
        fileActions = fileActions,
        onOpenNotebook = onOpenNotebook,
        onOpenPage = onOpenPage,
        modifier = modifier
    )

    HomeDialogHost(
        state = homeState,
        viewModel = viewModel,
        appLanguage = appLanguage,
        notebooks = notebooks,
        onOpenNotebook = onOpenNotebook
    )
}

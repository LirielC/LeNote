package com.example.lenote.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity
import com.example.lenote.ink.PageBackground
import com.example.lenote.ui.theme.EditorialActionChip
import com.example.lenote.ui.theme.EditorialEmptyState
import com.example.lenote.ui.theme.EditorialFeedbackBannerState
import com.example.lenote.ui.theme.EditorialFeedbackType
import com.example.lenote.ui.theme.EditorialFilterTab
import com.example.lenote.ui.theme.EditorialSearchBar
import com.example.lenote.ui.theme.EditorialSectionHeader
import com.example.lenote.ui.theme.EditorialSegmentedBar
import com.example.lenote.ui.theme.EditorialStatusBanner
import com.example.lenote.ui.theme.LeNotePalette
import com.example.lenote.ui.theme.LeNoteTheme
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Paleta curada de capas editoriais para cadernos LeNote.
 */
private val NotebookCoverColors = LeNotePalette.EditorialCoverPalette

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
    val isDarkMode = themeMode == com.example.lenote.data.ThemeMode.DARK

    // 0: Biblioteca, 1: Favoritas, 2: Lixeira
    var selectedTab by remember { mutableIntStateOf(0) }
    var notebookToExport by remember { mutableStateOf<NotebookEntity?>(null) }
    var notebookToDeletePermanently by remember { mutableStateOf<NotebookEntity?>(null) }
    var pageToDeletePermanently by remember { mutableStateOf<PageEntity?>(null) }
    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }

    // Estado explícito para Loading, Sucesso e Erro (Rule 10)
    var feedbackBanner by remember { mutableStateOf<EditorialFeedbackBannerState?>(null) }

    // Auto-dismiss para banners de sucesso após 4 segundos
    LaunchedEffect(feedbackBanner) {
        if (feedbackBanner?.type == EditorialFeedbackType.SUCCESS) {
            delay(4000)
            feedbackBanner = null
        }
    }

    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }

    // Launcher SAF para importar documento PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Documento.pdf"
            val cleanName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
            val pdfsDir = File(context.filesDir, "pdfs").apply { mkdirs() }
            val targetFile = File(pdfsDir, "pdf_${System.currentTimeMillis()}_$cleanName")

            feedbackBanner = EditorialFeedbackBannerState(
                type = EditorialFeedbackType.LOADING,
                title = "Importando documento PDF...",
                message = "Renderizando páginas de '$cleanName' para o seu caderno."
            )

            try {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                val title = cleanName.removeSuffix(".pdf")
                viewModel.importPdfDocument(title, targetFile) { nb ->
                    feedbackBanner = EditorialFeedbackBannerState(
                        type = EditorialFeedbackType.SUCCESS,
                        title = "PDF importado com sucesso",
                        message = "Caderno '${nb.title}' pronto para anotação."
                    )
                    onOpenNotebook(nb)
                }
            } catch (e: Exception) {
                feedbackBanner = EditorialFeedbackBannerState(
                    type = EditorialFeedbackType.ERROR,
                    title = "Falha ao importar PDF",
                    message = e.localizedMessage ?: "Verifique se o arquivo PDF é válido."
                )
            }
        }
    }

    // Launcher SAF para importar pacote .lenote
    val lenoteImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "caderno.lenote"
            val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}_$fileName")

            feedbackBanner = EditorialFeedbackBannerState(
                type = EditorialFeedbackType.LOADING,
                title = "Importando pacote .lenote...",
                message = "Restaurando seções, páginas e traços vetoriais."
            )

            try {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    FileOutputStream(tempFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                viewModel.importNotebookPackage(tempFile) { importedNb ->
                    tempFile.delete()
                    if (importedNb != null) {
                        feedbackBanner = EditorialFeedbackBannerState(
                            type = EditorialFeedbackType.SUCCESS,
                            title = "Caderno importado",
                            message = "'${importedNb.title}' adicionado à biblioteca."
                        )
                        onOpenNotebook(importedNb)
                    } else {
                        feedbackBanner = EditorialFeedbackBannerState(
                            type = EditorialFeedbackType.ERROR,
                            title = "Pacote .lenote inválido",
                            message = "Não foi possível ler a estrutura do arquivo selecionado."
                        )
                    }
                }
            } catch (e: Exception) {
                feedbackBanner = EditorialFeedbackBannerState(
                    type = EditorialFeedbackType.ERROR,
                    title = "Erro na importação",
                    message = e.localizedMessage ?: "Falha de leitura do pacote."
                )
            }
        }
    }

    // Launcher SAF para exportar pacote .lenote
    val lenoteExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val nb = notebookToExport
        if (uri != null && nb != null) {
            feedbackBanner = EditorialFeedbackBannerState(
                type = EditorialFeedbackType.LOADING,
                title = "Exportando '${nb.title}'...",
                message = "Compactando traços, mídias e metadados."
            )
            val tempZip = File(context.cacheDir, "temp_export_${nb.id}.lenote")
            viewModel.exportNotebookPackage(nb.id, tempZip) { success ->
                if (success && tempZip.exists()) {
                    try {
                        tempZip.inputStream().use { inStream ->
                            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        feedbackBanner = EditorialFeedbackBannerState(
                            type = EditorialFeedbackType.SUCCESS,
                            title = "Backup .lenote concluído",
                            message = "O caderno '${nb.title}' foi salvo com sucesso."
                        )
                    } catch (e: Exception) {
                        feedbackBanner = EditorialFeedbackBannerState(
                            type = EditorialFeedbackType.ERROR,
                            title = "Erro ao gravar arquivo",
                            message = e.localizedMessage ?: "Não foi possível salvar no destino."
                        )
                    }
                    tempZip.delete()
                } else {
                    feedbackBanner = EditorialFeedbackBannerState(
                        type = EditorialFeedbackType.ERROR,
                        title = "Falha na exportação",
                        message = "Não foi possível gerar o pacote do caderno."
                    )
                }
            }
        }
        notebookToExport = null
    }

    // Páginas recentes ordenadas por data de modificação
    val recentPages = remember(allPages, searchQuery) {
        val sorted = allPages.sortedByDescending { it.updatedAt }
        if (searchQuery.isBlank()) {
            sorted.take(6)
        } else {
            val q = searchQuery.trim().lowercase()
            sorted.filter { it.title.lowercase().contains(q) }
        }
    }

    // Cadernos filtrados por busca
    val filteredNotebooks = remember(notebooks, searchQuery) {
        if (searchQuery.isBlank()) notebooks
        else {
            val q = searchQuery.trim().lowercase()
            notebooks.filter { it.title.lowercase().contains(q) }
        }
    }

    // Páginas favoritas filtradas por busca
    val filteredFavorites = remember(favoritePages, searchQuery) {
        if (searchQuery.isBlank()) favoritePages
        else {
            val q = searchQuery.trim().lowercase()
            favoritePages.filter { it.title.lowercase().contains(q) }
        }
    }

    // Mapa auxiliar de ID do caderno -> NotebookEntity para exibir contexto nos cartões de página
    val notebookMap = remember(notebooks) {
        notebooks.associateBy { it.id }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        containerColor = LeNoteTheme.colors.paperBackground,
        floatingActionButton = {
            when (selectedTab) {
                0 -> {
                    // FAB único, elegante e sem colisão horizontal
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
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
                                contentDescription = "Novo Caderno",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Novo Caderno",
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
                            onClick = { showEmptyTrashConfirmDialog = true },
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
                                    contentDescription = "Esvaziar Lixeira",
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Esvaziar Lixeira",
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
                onImportPdf = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                onImportLenote = {
                    lenoteImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                },
                onToggleTheme = { viewModel.toggleDarkThemeMode() }
            )

            // Banner de Estado Explícito (Loading, Sucesso ou Erro)
            AnimatedVisibility(
                visible = feedbackBanner != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                feedbackBanner?.let { banner ->
                    EditorialStatusBanner(
                        state = banner,
                        onDismiss = { feedbackBanner = null }
                    )
                }
            }

            // 2. Barra de Busca e Filtro Segmentado Editorial
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorialSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    placeholder = "Buscar cadernos, notas ou tópicos..."
                )

                val trashTotal = deletedNotebooks.size + deletedPages.size
                EditorialSegmentedBar(
                    tabs = listOf(
                        EditorialFilterTab(
                            label = "Biblioteca",
                            count = notebooks.size,
                            icon = PhosphorIcons.Regular.Notebook,
                            selectedIcon = PhosphorIcons.Fill.Notebook
                        ),
                        EditorialFilterTab(
                            label = "Favoritas",
                            count = favoritePages.size,
                            icon = PhosphorIcons.Regular.Star,
                            selectedIcon = PhosphorIcons.Fill.Star
                        ),
                        EditorialFilterTab(
                            label = "Lixeira",
                            count = trashTotal,
                            icon = PhosphorIcons.Regular.Trash,
                            selectedIcon = PhosphorIcons.Fill.Trash
                        )
                    ),
                    selectedIndex = selectedTab,
                    onSelect = { selectedTab = it }
                )
            }

            // 3. Conteúdo Principal da Aba Selecionada
            when (selectedTab) {
                0 -> {
                    // ABA BIBLIOTECA: Scroll unificado (Continuar Escrevendo + Grade 2+ colunas de Cadernos)
                    if (filteredNotebooks.isEmpty() && recentPages.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (searchQuery.isNotBlank()) {
                                EditorialEmptyState(
                                    icon = PhosphorIcons.Regular.MagnifyingGlass,
                                    overline = "Sem resultados",
                                    title = "Nenhum caderno para \"$searchQuery\"",
                                    description = "Verifique a grafia ou limpe o filtro de busca para visualizar toda a sua coleção.",
                                    actionLabel = "Limpar Busca",
                                    actionIcon = PhosphorIcons.Regular.X,
                                    onActionClick = { searchQuery = "" }
                                )
                            } else {
                                EditorialEmptyState(
                                    icon = PhosphorIcons.Regular.Notebook,
                                    overline = "Acervo Vazio",
                                    title = "Comece seu primeiro caderno",
                                    description = "Crie um caderno em branco para manuscritos e esboços ou importe um PDF para leitura e anotação.",
                                    actionLabel = "Criar Primeiro Caderno",
                                    actionIcon = PhosphorIcons.Regular.Plus,
                                    onActionClick = { showCreateDialog = true }
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
                                .weight(1f)
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
                                            title = "Continuar Escrevendo",
                                            count = recentPages.size,
                                            subtitle = "Folhas recentes"
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
                                    title = "Coleção de Cadernos",
                                    count = filteredNotebooks.size,
                                    subtitle = if (searchQuery.isBlank()) "Estante principal" else "Filtro ativo"
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
                                        notebookToExport = nb
                                        val safeTitle = nb.title.replace(Regex("[^a-zA-Z0-9_]"), "_")
                                        lenoteExportLauncher.launch("$safeTitle.lenote")
                                    },
                                    onMoveToTrash = {
                                        viewModel.moveNotebookToTrash(nb.id)
                                        feedbackBanner = EditorialFeedbackBannerState(
                                            type = EditorialFeedbackType.SUCCESS,
                                            title = "Movido para a lixeira",
                                            message = "'${nb.title}' pode ser restaurado na aba Lixeira."
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

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
                            feedbackBanner = EditorialFeedbackBannerState(
                                type = EditorialFeedbackType.SUCCESS,
                                title = "Caderno restaurado",
                                message = "'${nb.title}' voltou para a sua biblioteca."
                            )
                        },
                        onDeleteNotebookPermanently = { notebookToDeletePermanently = it },
                        onRestorePage = { pg ->
                            viewModel.restorePageFromTrash(pg.id)
                            feedbackBanner = EditorialFeedbackBannerState(
                                type = EditorialFeedbackType.SUCCESS,
                                title = "Página restaurada",
                                message = "'${pg.title}' foi restaurada ao caderno."
                            )
                        },
                        onDeletePagePermanently = { pageToDeletePermanently = it },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Diálogo de Criação de Novo Caderno
    if (showCreateDialog) {
        CreateNotebookDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { title, colorArgb ->
                viewModel.createNewNotebook(title, colorArgb)
                showCreateDialog = false
                feedbackBanner = EditorialFeedbackBannerState(
                    type = EditorialFeedbackType.SUCCESS,
                    title = "Caderno criado",
                    message = "Abrindo '$title'..."
                )
                notebooks.find { it.title == title }?.let { nb ->
                    onOpenNotebook(nb)
                } ?: run {
                    viewModel.notebooks.value.firstOrNull()?.let { onOpenNotebook(it) }
                }
            }
        )
    }

    // Diálogos de Confirmação da Lixeira
    if (showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirmDialog = false },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = {
                Text(
                    text = "Esvaziar Lixeira?",
                    style = MaterialTheme.typography.titleLarge,
                    color = LeNoteTheme.colors.inkPrimary
                )
            },
            text = {
                Text(
                    text = "Todos os cadernos, seções, páginas e mídias da lixeira serão excluídos definitivamente do dispositivo. Esta ação não pode ser desfeita.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LeNoteTheme.colors.inkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashConfirmDialog = false
                        feedbackBanner = EditorialFeedbackBannerState(
                            type = EditorialFeedbackType.SUCCESS,
                            title = "Lixeira esvaziada",
                            message = "Todos os itens foram removidos permanentemente."
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Esvaziar Definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirmDialog = false }) {
                    Text("Cancelar", color = LeNoteTheme.colors.inkSecondary)
                }
            }
        )
    }

    if (notebookToDeletePermanently != null) {
        val nb = notebookToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { notebookToDeletePermanently = null },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = {
                Text(
                    text = "Excluir Caderno Definitivamente?",
                    style = MaterialTheme.typography.titleLarge,
                    color = LeNoteTheme.colors.inkPrimary
                )
            },
            text = {
                Text(
                    text = "O caderno '${nb.title}' e todas as suas páginas serão removidos permanentemente do armazenamento.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LeNoteTheme.colors.inkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeleteNotebook(nb.id)
                        notebookToDeletePermanently = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { notebookToDeletePermanently = null }) {
                    Text("Cancelar", color = LeNoteTheme.colors.inkSecondary)
                }
            }
        )
    }

    if (pageToDeletePermanently != null) {
        val p = pageToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { pageToDeletePermanently = null },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = {
                Text(
                    text = "Excluir Página Definitivamente?",
                    style = MaterialTheme.typography.titleLarge,
                    color = LeNoteTheme.colors.inkPrimary
                )
            },
            text = {
                Text(
                    text = "A página '${p.title}' será apagada permanentemente.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LeNoteTheme.colors.inkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeletePage(p.id)
                        pageToDeletePermanently = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { pageToDeletePermanently = null }) {
                    Text("Cancelar", color = LeNoteTheme.colors.inkSecondary)
                }
            }
        )
    }
}

/**
 * Cabeçalho Editorial (Masthead) da HomeScreen:
 * Forte hierarquia tipográfica, sumário do acervo e botões utilitários elegantes no topo.
 */
@Composable
private fun EditorialMasthead(
    notebookCount: Int,
    pageCount: Int,
    isDarkMode: Boolean,
    onImportPdf: () -> Unit,
    onImportLenote: () -> Unit,
    onToggleTheme: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Linha 1: Sobretítulo Editorial + Ações Utilitárias Compactas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(LeNoteTheme.colors.accentWarm, CircleShape)
                )
                Text(
                    text = "ATELIER DE NOTAS",
                    style = MaterialTheme.typography.labelSmall,
                    color = LeNoteTheme.colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorialActionChip(
                    label = "PDF",
                    icon = PhosphorIcons.Regular.FilePdf,
                    onClick = onImportPdf
                )
                EditorialActionChip(
                    label = "Backup",
                    icon = PhosphorIcons.Regular.Export,
                    onClick = onImportLenote
                )
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleTheme() },
                    shape = RoundedCornerShape(8.dp),
                    color = LeNoteTheme.colors.elevatedSurface,
                    border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDarkMode) PhosphorIcons.Regular.Sun else PhosphorIcons.Regular.Moon,
                            contentDescription = if (isDarkMode) "Modo Claro" else "Modo Noturno",
                            tint = LeNoteTheme.colors.inkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Linha 2: Título Editorial Full-Width + Metadados em Monospace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Meus Cadernos",
                style = MaterialTheme.typography.headlineMedium,
                color = LeNoteTheme.colors.inkPrimary,
                maxLines = 1
            )

            Text(
                text = "$notebookCount cadernos  •  $pageCount folhas",
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkMuted,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

/**
 * Card de Caderno com estética de Monografia / Caderno Encadernado de Ateliê (Hardcover Folio):
 * - Cantos assimétricos de livro encadernado (lombada esquerda 4.dp, corte direito 12.dp);
 * - Lombada em tecido fosco com vinco sutil de encadernação (sem gradientes plásticos exagerados);
 * - Placa tipográfica editorial em papel marfim com título em Serif, índice de seções/páginas e menu discreto;
 * - Microinteração natural de escala ao toque.
 */
@Composable
private fun NotebookCoverCard(
    notebook: NotebookEntity,
    pageCount: Int,
    sectionCount: Int,
    onClick: () -> Unit,
    onExport: () -> Unit,
    onMoveToTrash: () -> Unit,
    modifier: Modifier = Modifier
) {
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
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val lastUpdateStr = remember(notebook.updatedAt) {
        dateFormat.format(Date(notebook.updatedAt)).uppercase()
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
                                text = "CADERNO",
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
                                        contentDescription = "Opções do caderno",
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
                                                "Exportar (.lenote)",
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
                                                "Mover para Lixeira",
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
                            text = "$sectionCount seções  •  $pageCount pág.",
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
                                contentDescription = "Abrir caderno",
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

/**
 * Card de Página Recente na seção "Continuar Escrevendo":
 * Apresenta a miniatura real da folha com borda hairline de papel, indicador do caderno
 * de origem, título e horário.
 */
@Composable
private fun RecentPageCard(
    page: PageEntity,
    notebookTitle: String?,
    notebookColorHex: Int?,
    thumbnailFile: File?,
    thumbnailVersion: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbBitmap = remember(page.id, page.updatedAt, thumbnailVersion) {
        if (thumbnailFile != null && thumbnailFile.exists()) {
            runCatching { BitmapFactory.decodeFile(thumbnailFile.absolutePath)?.asImageBitmap() }.getOrNull()
        } else null
    }

    val timeFormat = remember { SimpleDateFormat("dd MMM • HH:mm", Locale.getDefault()) }
    val formattedTime = remember(page.updatedAt) { timeFormat.format(Date(page.updatedAt)) }

    Surface(
        modifier = modifier
            .width(156.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = LeNoteTheme.colors.elevatedSurface,
        border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val paperColor = Color(page.paperColor.toInt())
            val isDarkPaper = com.example.lenote.ink.PaperColors.isDarkPaper(page.paperColor)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(paperColor)
                    .border(
                        width = 1.dp,
                        color = if (isDarkPaper) Color(0xFF334155) else LeNoteTheme.colors.hairlineBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap,
                        contentDescription = page.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    TemplatePreviewCanvas(background = page.pageBackground, isDark = isDarkPaper)
                }

                if (page.isFavorite) {
                    Surface(
                        shape = CircleShape,
                        color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = PhosphorIcons.Fill.Star,
                                contentDescription = "Favorita",
                                tint = LeNoteTheme.colors.goldStar,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!notebookTitle.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (notebookColorHex != null) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(notebookColorHex), CircleShape)
                            )
                        }
                        Text(
                            text = notebookTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = LeNoteTheme.colors.inkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = LeNoteTheme.colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = LeNoteTheme.colors.inkMuted
                )
            }
        }
    }
}

/**
 * Conteúdo da Aba Favoritas (Folhas marcadas com estrela pelo usuário).
 */
@Composable
private fun FavoritesContent(
    favoritePages: List<PageEntity>,
    getNotebookForPage: (PageEntity) -> NotebookEntity?,
    thumbnailVersion: Long,
    getThumbnailFile: (String) -> File?,
    onOpenPage: (PageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (favoritePages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EditorialEmptyState(
                icon = PhosphorIcons.Regular.Star,
                overline = "Coleção de Destaques",
                title = "Nenhuma página favorita",
                description = "Toque no ícone de estrela no topo do canvas ou na barra lateral para fixar suas folhas mais importantes aqui."
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 152.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
            modifier = modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EditorialSectionHeader(
                    title = "Folhas Favoritas",
                    count = favoritePages.size,
                    subtitle = "Acesso rápido"
                )
            }
            items(favoritePages, key = { "fav_${it.id}" }) { page ->
                val parentNb = getNotebookForPage(page)
                RecentPageCard(
                    page = page,
                    notebookTitle = parentNb?.title,
                    notebookColorHex = parentNb?.colorHex,
                    thumbnailFile = getThumbnailFile(page.id),
                    thumbnailVersion = thumbnailVersion,
                    onClick = { onOpenPage(page) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Desenho de fundo em miniatura para folhas que ainda não possuem traços salvos.
 */
@Composable
private fun MiniPageBackgroundCanvas(
    pageBackground: PageBackground,
    paperColor: Long = 0xFFFFFFFF,
    modifier: Modifier = Modifier
) {
    TemplatePreviewCanvas(
        background = pageBackground,
        isDark = com.example.lenote.ink.PaperColors.isDarkPaper(paperColor),
        modifier = modifier
    )
}

/**
 * Diálogo Editorial para criação de novo caderno com título e seletor de cor da capa.
 */
@Composable
private fun CreateNotebookDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, colorArgb: Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(NotebookCoverColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LeNoteTheme.colors.elevatedSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "NOVO VOLUME",
                    style = MaterialTheme.typography.labelMedium,
                    color = LeNoteTheme.colors.inkMuted
                )
                Text(
                    text = "Criar Caderno",
                    style = MaterialTheme.typography.headlineSmall,
                    color = LeNoteTheme.colors.inkPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título do Caderno") },
                    placeholder = { Text("Ex: Arquitetura, Diário de Campo...") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LeNoteTheme.colors.accentPrimary,
                        unfocusedBorderColor = LeNoteTheme.colors.hairlineBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "TONALIDADE DA ENCADERNAÇÃO",
                        style = MaterialTheme.typography.labelMedium,
                        color = LeNoteTheme.colors.inkSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NotebookCoverColors.forEach { color ->
                            val isSelected = selectedColor == color
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = color }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.5.dp, LeNoteTheme.colors.inkPrimary, CircleShape)
                                        } else {
                                            Modifier.border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Check,
                                        contentDescription = "Selecionado",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim().ifEmpty { "Novo Caderno" }
                    val argb = (selectedColor.value shr 32).toInt()
                    onCreate(finalTitle, argb)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LeNoteTheme.colors.accentPrimary,
                    contentColor = Color.White
                )
            ) {
                Text("Criar Caderno")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = LeNoteTheme.colors.inkSecondary)
            }
        }
    )
}

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
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    if (deletedNotebooks.isEmpty() && deletedPages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EditorialEmptyState(
                icon = PhosphorIcons.Regular.Trash,
                overline = "Arquivo de Exclusão",
                title = "A lixeira está vazia",
                description = "Cadernos e páginas removidos ficam arquivados aqui temporariamente antes da exclusão definitiva."
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
                        title = "Cadernos Arquivados na Lixeira",
                        count = deletedNotebooks.size
                    )
                }
                items(deletedNotebooks, key = { "nb_${it.id}" }) { nb ->
                    val delDate = remember(nb.deletedAt) {
                        nb.deletedAt?.let { dateFormat.format(Date(it)) } ?: "Data desconhecida"
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
                                        text = "Excluído em $delDate",
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
                                    label = "Restaurar",
                                    icon = PhosphorIcons.Regular.ArrowCounterClockwise,
                                    onClick = { onRestoreNotebook(nb) }
                                )
                                IconButton(
                                    onClick = { onDeleteNotebookPermanently(nb) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Trash,
                                        contentDescription = "Excluir definitivamente",
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
                        title = "Folhas Soltas na Lixeira",
                        count = deletedPages.size
                    )
                }
                items(deletedPages, key = { "pg_${it.id}" }) { page ->
                    val delDate = remember(page.deletedAt) {
                        page.deletedAt?.let { dateFormat.format(Date(it)) } ?: "Data desconhecida"
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
                                        text = "Excluído em $delDate",
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
                                    label = "Restaurar",
                                    icon = PhosphorIcons.Regular.ArrowCounterClockwise,
                                    onClick = { onRestorePage(page) }
                                )
                                IconButton(
                                    onClick = { onDeletePagePermanently(page) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Trash,
                                        contentDescription = "Excluir definitivamente",
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

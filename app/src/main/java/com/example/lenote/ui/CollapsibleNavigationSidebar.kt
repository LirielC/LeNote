package com.example.lenote.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.lenote.data.SectionEntity
import com.example.lenote.ink.PageBackground
import com.example.lenote.ui.theme.EditorialActionChip
import com.example.lenote.ui.theme.EditorialEmptyState
import com.example.lenote.ui.theme.EditorialSearchBar
import com.example.lenote.ui.theme.LeNoteTheme
import java.io.File
import java.io.FileOutputStream

/**
 * Painel lateral recolhível (`CollapsibleNavigationSidebar`) com estética de Índice / Sumário Editorial:
 * - Superfície em papel aquecido (`paperBackground`) separada por borda hairline vertical;
 * - Respeita `statusBarsPadding()` e `navigationBarsPadding()`;
 * - Barra de busca editorial reutilizável (`EditorialSearchBar`);
 * - Seletor horizontal de Cadernos e Seções com indicadores de cor de capa;
 * - Lista de páginas com miniaturas reais de proporção A4, indicador de página ativa e menu contextual;
 * - Barra de utilidades inferior compacta em uma única linha (Importar PDF, Backup .lenote, Lixeira).
 */
@Composable
fun CollapsibleNavigationSidebar(
    viewModel: InkPlaygroundViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notebooks by viewModel.notebooks.collectAsState()
    val sections by viewModel.sections.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val favoritePages by viewModel.favoritePages.collectAsState()
    val selectedNotebookId by viewModel.selectedNotebookId.collectAsState()
    val selectedSectionId by viewModel.selectedSectionId.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val thumbnailVersion by viewModel.thumbnailVersion.collectAsState()
    val deletedNotebooks by viewModel.deletedNotebooks.collectAsState()
    val deletedPages by viewModel.deletedPages.collectAsState()

    var showTrashDialog by remember { mutableStateOf(false) }
    var showCreatePageTemplateDialog by remember { mutableStateOf(false) }
    var pageToChangeTemplate by remember { mutableStateOf<PageEntity?>(null) }
    var notebookToExport by remember { mutableStateOf<NotebookEntity?>(null) }
    var notebookToDeletePermanently by remember { mutableStateOf<NotebookEntity?>(null) }
    var pageToDeletePermanently by remember { mutableStateOf<PageEntity?>(null) }
    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }

    val s = LeNoteTheme.strings
    val context = LocalContext.current
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var dialogMode by remember { mutableStateOf<SidebarDialogMode?>(null) }
    var dialogInputText by remember { mutableStateOf("") }

    // Launcher SAF para importar documento PDF
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "Documento.pdf"
            val cleanName = if (fileName.endsWith(".pdf", ignoreCase = true)) fileName else "$fileName.pdf"
            val pdfsDir = File(context.filesDir, "pdfs").apply { mkdirs() }
            val targetFile = File(pdfsDir, "pdf_${System.currentTimeMillis()}_$cleanName")

            try {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                val title = cleanName.removeSuffix(".pdf")
                viewModel.importPdfDocument(title, targetFile)
            } catch (_: Exception) {}
        }
    }

    // Launcher SAF para exportar pacote .lenote do caderno ativo
    val lenoteExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val nb = notebookToExport
        if (uri != null && nb != null) {
            val tempZip = File(context.cacheDir, "temp_export_${nb.id}.lenote")
            viewModel.exportNotebookPackage(nb.id, tempZip) { success ->
                if (success && tempZip.exists()) {
                    try {
                        tempZip.inputStream().use { inStream ->
                            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                    } catch (_: Exception) {}
                    tempZip.delete()
                }
            }
        }
        notebookToExport = null
    }

    Row(modifier = modifier.fillMaxHeight()) {
        Surface(
            modifier = Modifier
                .width(324.dp)
                .fillMaxHeight(),
            color = LeNoteTheme.colors.paperBackground,
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Cabeçalho Editorial do Sumário
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = s.sidebarOverline,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                letterSpacing = 1.sp
                            ),
                            color = LeNoteTheme.colors.inkMuted
                        )
                        Text(
                            text = s.sidebarTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = LeNoteTheme.colors.inkPrimary
                        )
                    }
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.X,
                            contentDescription = s.actionClose,
                            tint = LeNoteTheme.colors.inkSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                // Busca Rápida Editorial
                EditorialSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.updateSearchQuery(it) },
                    placeholder = s.sidebarSearchPlaceholder
                )

                if (searchQuery.isNotBlank()) {
                    Text(
                        text = s.sidebarSearchResultsHeader(searchResults.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = LeNoteTheme.colors.inkSecondary
                    )
                    if (searchResults.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            EditorialEmptyState(
                                icon = PhosphorIcons.Regular.MagnifyingGlass,
                                overline = s.sidebarSearchEmptyOverline,
                                title = s.sidebarSearchEmptyTitle,
                                description = s.sidebarSearchEmptyDesc(searchQuery)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(searchResults, key = { it.id }) { page ->
                                PageListItemCard(
                                    page = page,
                                    isSelected = currentPage?.id == page.id,
                                    thumbnailFile = viewModel.getThumbnailFile(page.id),
                                    thumbnailVersion = thumbnailVersion,
                                    onSelect = { viewModel.selectPage(page) },
                                    onToggleFavorite = { viewModel.togglePageFavorite(page.id) },
                                    onRename = {
                                        dialogInputText = page.title
                                        dialogMode = SidebarDialogMode.RenamePage(page)
                                    },
                                    onDuplicate = { viewModel.duplicatePage(page.id) },
                                    onMoveUp = {},
                                    onMoveDown = {},
                                    onChangeTemplate = { pageToChangeTemplate = page },
                                    onDelete = { viewModel.movePageToTrash(page.id) },
                                    showReorderButtons = false
                                )
                            }
                        }
                    }
                } else {
                    // 1. Seção de Cadernos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.sidebarNotebooksHeader(notebooks.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = LeNoteTheme.colors.inkMuted
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val curNb = notebooks.find { it.id == selectedNotebookId }
                                    if (curNb != null) {
                                        dialogInputText = curNb.title
                                        dialogMode = SidebarDialogMode.RenameNotebook(curNb)
                                    }
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.PencilSimple,
                                    contentDescription = s.sidebarRenameNotebookTitle,
                                    tint = LeNoteTheme.colors.inkSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    dialogInputText = ""
                                    dialogMode = SidebarDialogMode.CreateNotebook
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Plus,
                                    contentDescription = s.fabNewNotebook,
                                    tint = LeNoteTheme.colors.accentPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(notebooks, key = { it.id }) { nb ->
                            val isSelected = nb.id == selectedNotebookId
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LeNoteTheme.colors.elevatedSurface else LeNoteTheme.colors.tonalSurface.copy(alpha = 0.6f),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.hairlineBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectNotebook(nb.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(nb.colorHex), CircleShape)
                                    )
                                    Text(
                                        text = nb.title,
                                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp),
                                        color = if (isSelected) LeNoteTheme.colors.inkPrimary else LeNoteTheme.colors.inkSecondary,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                    // 2. Seção de Divisórias / Seções
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.sidebarSectionsHeader(sections.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = LeNoteTheme.colors.inkMuted
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val curSec = sections.find { it.id == selectedSectionId }
                                    if (curSec != null) {
                                        dialogInputText = curSec.title
                                        dialogMode = SidebarDialogMode.RenameSection(curSec)
                                    }
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.PencilSimple,
                                    contentDescription = s.sidebarRenameSectionTitle,
                                    tint = LeNoteTheme.colors.inkSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    dialogInputText = ""
                                    dialogMode = SidebarDialogMode.CreateSection
                                },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.Plus,
                                    contentDescription = s.sidebarNewSectionTitle,
                                    tint = LeNoteTheme.colors.accentPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(sections, key = { it.id }) { sec ->
                            val isSelected = sec.id == selectedSectionId
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LeNoteTheme.colors.accentPrimaryContainer else Color.Transparent,
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) LeNoteTheme.colors.accentPrimary.copy(alpha = 0.4f) else LeNoteTheme.colors.hairlineBorder
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.selectSection(sec.id) }
                            ) {
                                Text(
                                    text = sec.title,
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.5.sp),
                                    color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkSecondary,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                    // 3. Seção de Folhas (Pages) + Filtro de Favoritas + Botão Nova Folha
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (showFavoritesOnly) s.sidebarFavoritesHeader(favoritePages.size) else s.sidebarPagesHeader(pages.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = LeNoteTheme.colors.inkMuted
                            )
                            Surface(
                                onClick = { showFavoritesOnly = !showFavoritesOnly },
                                shape = RoundedCornerShape(6.dp),
                                color = if (showFavoritesOnly) LeNoteTheme.colors.accentWarmContainer else LeNoteTheme.colors.tonalSurface,
                                border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showFavoritesOnly) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                                        contentDescription = s.sidebarFilterStars,
                                        tint = if (showFavoritesOnly) LeNoteTheme.colors.goldStar else LeNoteTheme.colors.inkSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (showFavoritesOnly) s.sidebarFilterAll else s.sidebarFilterStars,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = LeNoteTheme.colors.inkSecondary
                                    )
                                }
                            }
                        }

                        EditorialActionChip(
                            label = s.sidebarNewSheetButton,
                            icon = PhosphorIcons.Regular.Plus,
                            onClick = { showCreatePageTemplateDialog = true },
                            isPrimary = true
                        )
                    }

                    val displayedPages = if (showFavoritesOnly) favoritePages else pages

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(displayedPages, key = { it.id }) { page ->
                            PageListItemCard(
                                page = page,
                                isSelected = currentPage?.id == page.id,
                                thumbnailFile = viewModel.getThumbnailFile(page.id),
                                thumbnailVersion = thumbnailVersion,
                                onSelect = { viewModel.selectPage(page) },
                                onToggleFavorite = { viewModel.togglePageFavorite(page.id) },
                                onRename = {
                                    dialogInputText = page.title
                                    dialogMode = SidebarDialogMode.RenamePage(page)
                                },
                                onDuplicate = { viewModel.duplicatePage(page.id) },
                                onMoveUp = { viewModel.movePageOrder(page.id, moveUp = true) },
                                onMoveDown = { viewModel.movePageOrder(page.id, moveUp = false) },
                                onChangeTemplate = { pageToChangeTemplate = page },
                                onDelete = { viewModel.movePageToTrash(page.id) },
                                showReorderButtons = !showFavoritesOnly
                            )
                        }
                    }
                }

                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                // Barra Utilitária Inferior Compacta em Linha Única
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EditorialActionChip(
                        label = s.actionPdf,
                        icon = PhosphorIcons.Regular.FilePdf,
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.weight(1f)
                    )
                    EditorialActionChip(
                        label = s.actionBackup,
                        icon = PhosphorIcons.Regular.Export,
                        onClick = {
                            val currentNb = notebooks.find { it.id == selectedNotebookId } ?: notebooks.firstOrNull()
                            if (currentNb != null) {
                                notebookToExport = currentNb
                                val safeTitle = currentNb.title.replace(Regex("[^a-zA-Z0-9_]"), "_")
                                lenoteExportLauncher.launch("$safeTitle.lenote")
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    val trashCount = deletedNotebooks.size + deletedPages.size
                    EditorialActionChip(
                        label = s.sidebarTrashButton(trashCount),
                        icon = PhosphorIcons.Regular.Trash,
                        onClick = { showTrashDialog = true },
                        isDestructive = trashCount > 0,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }

        // Borda vertical hairline direita da barra lateral
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(LeNoteTheme.colors.hairlineBorder)
        )
    }

    // Diálogo da Lixeira na Barra Lateral
    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Trash,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = s.sidebarTrashDialogTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = LeNoteTheme.colors.inkPrimary
                        )
                    }
                    if (deletedNotebooks.isNotEmpty() || deletedPages.isNotEmpty()) {
                        TextButton(
                            onClick = { showEmptyTrashConfirmDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text(s.sidebarEmptyAction, fontSize = 12.sp)
                        }
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                ) {
                    TrashContent(
                        deletedNotebooks = deletedNotebooks,
                        deletedPages = deletedPages,
                        onRestoreNotebook = { viewModel.restoreNotebookFromTrash(it.id) },
                        onDeleteNotebookPermanently = { notebookToDeletePermanently = it },
                        onRestorePage = { viewModel.restorePageFromTrash(it.id) },
                        onDeletePagePermanently = { pageToDeletePermanently = it }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTrashDialog = false }) {
                    Text(s.actionClose, color = LeNoteTheme.colors.inkPrimary)
                }
            }
        )
    }

    if (showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirmDialog = false },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = { Text(s.emptyTrashDialogTitle, style = MaterialTheme.typography.titleLarge) },
            text = { Text(s.emptyTrashDialogMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(s.sidebarEmptyAction)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirmDialog = false }) {
                    Text(s.actionCancel)
                }
            }
        )
    }

    if (notebookToDeletePermanently != null) {
        val nb = notebookToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { notebookToDeletePermanently = null },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = { Text(s.deleteNotebookDialogTitle, style = MaterialTheme.typography.titleLarge) },
            text = { Text(s.deleteNotebookDialogMessage(nb.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeleteNotebook(nb.id)
                        notebookToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(s.actionDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { notebookToDeletePermanently = null }) {
                    Text(s.actionCancel)
                }
            }
        )
    }

    if (pageToDeletePermanently != null) {
        val p = pageToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { pageToDeletePermanently = null },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = { Text(s.deletePageDialogTitle, style = MaterialTheme.typography.titleLarge) },
            text = { Text(s.deletePageDialogMessage(p.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeletePage(p.id)
                        pageToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(s.actionDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { pageToDeletePermanently = null }) {
                    Text(s.actionCancel)
                }
            }
        )
    }

    // Diálogo de Seleção de Modelo para Nova Página
    if (showCreatePageTemplateDialog) {
        PageTemplatePickerDialog(
            initialTitle = s.defaultPageTitle(pages.size + 1),
            initialBackground = currentPage?.pageBackground ?: PageBackground.RULED,
            initialPaperColor = currentPage?.paperColor ?: 0xFFFFFFFF,
            isCreateMode = true,
            onConfirm = { newTitle, bg, paperColor ->
                showCreatePageTemplateDialog = false
                viewModel.createNewPage(
                    title = newTitle.ifBlank { s.defaultPageTitle(pages.size + 1) },
                    background = bg,
                    paperColor = paperColor
                )
            },
            onDismiss = { showCreatePageTemplateDialog = false }
        )
    }

    // Diálogo de Alteração de Modelo para Página Existente
    if (pageToChangeTemplate != null) {
        val targetPage = pageToChangeTemplate!!
        PageTemplatePickerDialog(
            initialTitle = targetPage.title,
            initialBackground = targetPage.pageBackground,
            initialPaperColor = targetPage.paperColor,
            isCreateMode = false,
            onConfirm = { _, bg, paperColor ->
                pageToChangeTemplate = null
                viewModel.changePageTemplateFor(targetPage.id, bg, paperColor)
            },
            onDismiss = { pageToChangeTemplate = null }
        )
    }

    val activeDialog = dialogMode
    if (activeDialog != null) {
        val titleText = when (activeDialog) {
            is SidebarDialogMode.CreateNotebook -> s.defaultNewNotebookName
            is SidebarDialogMode.RenameNotebook -> s.sidebarRenameNotebookTitle
            is SidebarDialogMode.CreateSection -> s.sidebarNewSectionTitle
            is SidebarDialogMode.RenameSection -> s.sidebarRenameSectionTitle
            is SidebarDialogMode.RenamePage -> s.renamePageTitle
        }

        AlertDialog(
            onDismissRequest = { dialogMode = null },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge,
                    color = LeNoteTheme.colors.inkPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = dialogInputText,
                    onValueChange = { dialogInputText = it },
                    singleLine = true,
                    placeholder = { Text(s.sidebarEnterNamePlaceholder) },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LeNoteTheme.colors.accentPrimary,
                        unfocusedBorderColor = LeNoteTheme.colors.hairlineBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (activeDialog) {
                            is SidebarDialogMode.CreateNotebook -> viewModel.createNewNotebook(dialogInputText)
                            is SidebarDialogMode.RenameNotebook -> viewModel.renameNotebook(activeDialog.notebook.id, dialogInputText)
                            is SidebarDialogMode.CreateSection -> viewModel.createNewSection(dialogInputText)
                            is SidebarDialogMode.RenameSection -> viewModel.renameSection(activeDialog.section.id, dialogInputText)
                            is SidebarDialogMode.RenamePage -> viewModel.renamePage(activeDialog.page.id, dialogInputText)
                        }
                        dialogMode = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LeNoteTheme.colors.accentPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(s.actionConfirm)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (activeDialog is SidebarDialogMode.RenameNotebook) {
                        TextButton(
                            onClick = {
                                viewModel.moveNotebookToTrash(activeDialog.notebook.id)
                                dialogMode = null
                            }
                        ) {
                            Text(s.menuMoveToTrash, color = MaterialTheme.colorScheme.error)
                        }
                    } else if (activeDialog is SidebarDialogMode.RenameSection) {
                        TextButton(
                            onClick = {
                                viewModel.deleteSection(activeDialog.section.id)
                                dialogMode = null
                            }
                        ) {
                            Text(s.actionDelete, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { dialogMode = null }) {
                        Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
                    }
                }
            }
        )
    }
}

private sealed interface SidebarDialogMode {
    data object CreateNotebook : SidebarDialogMode
    data class RenameNotebook(val notebook: NotebookEntity) : SidebarDialogMode
    data object CreateSection : SidebarDialogMode
    data class RenameSection(val section: SectionEntity) : SidebarDialogMode
    data class RenamePage(val page: PageEntity) : SidebarDialogMode
}

/**
 * Item de página editorial com miniatura realista, título, tipo de folha, favorito
 * e menu contextual "⋮" para ações secundárias.
 */
@Composable
private fun PageListItemCard(
    page: PageEntity,
    isSelected: Boolean,
    thumbnailFile: File?,
    thumbnailVersion: Long,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onChangeTemplate: () -> Unit = {},
    onDelete: () -> Unit,
    showReorderButtons: Boolean
) {
    val s = LeNoteTheme.strings
    var isContextMenuOpen by remember { mutableStateOf(false) }

    val thumbBitmap = remember(page.id, page.updatedAt, thumbnailVersion) {
        if (thumbnailFile != null && thumbnailFile.exists()) {
            runCatching { BitmapFactory.decodeFile(thumbnailFile.absolutePath)?.asImageBitmap() }.getOrNull()
        } else {
            null
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onSelect() },
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) LeNoteTheme.colors.elevatedSurface else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, LeNoteTheme.colors.accentPrimary.copy(alpha = 0.5f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Miniatura da Folha
            PageThumbnailPreview(
                pageBackground = page.pageBackground,
                paperColorLong = page.paperColor,
                bitmap = thumbBitmap,
                modifier = Modifier.size(width = 44.dp, height = 58.dp)
            )

            // 2. Metadados Principais
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = LeNoteTheme.colors.inkPrimary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = s.pageBackgroundLabel(page.pageBackground),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = LeNoteTheme.colors.inkMuted
                )
            }

            // 3. Favorito
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (page.isFavorite) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                    contentDescription = if (page.isFavorite) s.unfavoritePageDesc else s.favoritePageDesc,
                    tint = if (page.isFavorite) LeNoteTheme.colors.goldStar else LeNoteTheme.colors.inkMuted,
                    modifier = Modifier.size(15.dp)
                )
            }

            // 4. Menu Contextual "⋮"
            Box {
                IconButton(
                    onClick = { isContextMenuOpen = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.DotsThreeVertical,
                        contentDescription = s.morePageOptionsDesc,
                        tint = LeNoteTheme.colors.inkSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = isContextMenuOpen,
                    onDismissRequest = { isContextMenuOpen = false },
                    containerColor = LeNoteTheme.colors.elevatedSurface
                ) {
                    DropdownMenuItem(
                        text = { Text(s.actionRenameSheet, style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = PhosphorIcons.Regular.PencilSimple,
                                contentDescription = null,
                                tint = LeNoteTheme.colors.inkSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            isContextMenuOpen = false
                            onRename()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text(s.actionDuplicateSheet, style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Copy,
                                contentDescription = null,
                                tint = LeNoteTheme.colors.inkSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            isContextMenuOpen = false
                            onDuplicate()
                        }
                    )

                    if (showReorderButtons) {
                        DropdownMenuItem(
                            text = { Text(s.actionMoveUp, style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.CaretUp,
                                    contentDescription = null,
                                    tint = LeNoteTheme.colors.inkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                isContextMenuOpen = false
                                onMoveUp()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(s.actionMoveDown, style = MaterialTheme.typography.bodyMedium) },
                            leadingIcon = {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.CaretDown,
                                    contentDescription = null,
                                    tint = LeNoteTheme.colors.inkSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                isContextMenuOpen = false
                                onMoveDown()
                            }
                        )
                    }

                    HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                    DropdownMenuItem(
                        text = { Text(s.actionSheetTemplateTitle, style = MaterialTheme.typography.bodyMedium) },
                        leadingIcon = {
                            Icon(
                                imageVector = PhosphorIcons.Regular.SlidersHorizontal,
                                contentDescription = null,
                                tint = LeNoteTheme.colors.inkSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            isContextMenuOpen = false
                            onChangeTemplate()
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = s.menuMoveToTrash,
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
                            isContextMenuOpen = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Renderização gráfica de miniatura com proporção de folha real.
 */
@Composable
private fun PageThumbnailPreview(
    pageBackground: PageBackground,
    paperColorLong: Long = 0xFFFFFFFF,
    bitmap: androidx.compose.ui.graphics.ImageBitmap?,
    modifier: Modifier = Modifier
) {
    val isDark = com.example.lenote.ink.PaperColors.isDarkPaper(paperColorLong)
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = Color(paperColorLong.toInt()),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF334155) else LeNoteTheme.colors.hairlineBorder)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            TemplatePreviewCanvas(background = pageBackground, isDark = isDark)
        }
    }
}

package com.example.lenote.ui

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import java.io.FileOutputStream
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity
import com.example.lenote.data.SectionEntity
import com.example.lenote.ink.PageBackground
import java.io.File

/**
 * Painel lateral recolhível (`CollapsibleNavigationSidebar`) para tablets:
 * - Largura reduzida para ~330dp, funcionando como um navegador limpo de páginas;
 * - Botões discretos de edição para Cadernos e Seções;
 * - Miniaturas de página realistas (renderizam traços ou o padrão da folha nativo);
 * - Lista de páginas simplificada (thumbnail + título + tipo de folha + favorito);
 * - Ações secundárias (Mover e Excluir) agrupadas no menu contextual "⋯".
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

    Surface(
        modifier = modifier
            .width(330.dp)
            .fillMaxHeight()
            .statusBarsPadding(),
        color = Color(0xFFFDFDFE),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabeçalho do Navegador de Páginas
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
                        imageVector = PhosphorIcons.Fill.Notebook,
                        contentDescription = "Cadernos",
                        tint = Color(0xFF1E3A8A),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Cadernos & Páginas",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = "Fechar painel",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Campo de Busca Rápida de Páginas (Alinhamento Vertical e Contraste Aprimorados)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                        contentDescription = "Buscar",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Buscar páginas...",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { viewModel.updateSearchQuery("") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.X,
                                contentDescription = "Limpar busca",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (searchQuery.isNotBlank()) {
                Text(
                    text = "Resultados (${searchResults.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
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
            } else {
                // 1. Seção de Cadernos (Notebooks)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CADERNOS (${notebooks.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ação de editar discreta (ícone lápis)
                        IconButton(
                            onClick = {
                                val curNb = notebooks.find { it.id == selectedNotebookId }
                                if (curNb != null) {
                                    dialogInputText = curNb.title
                                    dialogMode = SidebarDialogMode.RenameNotebook(curNb)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.PencilSimple,
                                contentDescription = "Renomear caderno",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Ação minimalista: + Caderno
                        IconButton(
                            onClick = {
                                dialogInputText = ""
                                dialogMode = SidebarDialogMode.CreateNotebook
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Plus,
                                contentDescription = "Novo caderno",
                                tint = Color(0xFF1E3A8A),
                                modifier = Modifier.size(16.dp)
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
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectNotebook(nb.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(Color(nb.colorHex), CircleShape)
                                )
                                Text(
                                    text = nb.title,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF334155),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // 2. Seção de Divisórias / Seções (Sections)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SEÇÕES (${sections.size})",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Ação de editar discreta (ícone lápis)
                        IconButton(
                            onClick = {
                                val curSec = sections.find { it.id == selectedSectionId }
                                if (curSec != null) {
                                    dialogInputText = curSec.title
                                    dialogMode = SidebarDialogMode.RenameSection(curSec)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.PencilSimple,
                                contentDescription = "Renomear seção",
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        // Ação minimalista: + Seção
                        IconButton(
                            onClick = {
                                dialogInputText = ""
                                dialogMode = SidebarDialogMode.CreateSection
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Plus,
                                contentDescription = "Nova seção",
                                tint = Color(0xFF1E3A8A),
                                modifier = Modifier.size(16.dp)
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
                            color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { viewModel.selectSection(sec.id) }
                        ) {
                            Text(
                                text = sec.title,
                                fontSize = 11.sp,
                                color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF334155),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // 3. Seção de Páginas (Pages) + Filtro de Favoritas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (showFavoritesOnly) "FAVORITAS (${favoritePages.size})" else "PÁGINAS (${pages.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        TextButton(
                            onClick = { showFavoritesOnly = !showFavoritesOnly },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 1.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Icon(
                                imageVector = if (showFavoritesOnly) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                                contentDescription = "Filtrar favoritas",
                                tint = if (showFavoritesOnly) Color(0xFFD97706) else Color(0xFF64748B),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (showFavoritesOnly) "Todas" else "Favoritas",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    // Ação principal evidente: + Página
                    Button(
                        onClick = { showCreatePageTemplateDialog = true },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E3A8A),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Plus,
                            contentDescription = "Nova página",
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Página", fontSize = 11.sp)
                    }
                }

                val displayedPages = if (showFavoritesOnly) favoritePages else pages

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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

            HorizontalDivider(color = Color(0xFFE2E8F0))

            // Ação de Importar PDF
            OutlinedButton(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.FilePdf,
                    contentDescription = "Importar PDF",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Importar PDF", fontSize = 11.sp)
            }

            // Exportar Caderno Ativo (.lenote)
            OutlinedButton(
                onClick = {
                    val currentNb = notebooks.find { it.id == selectedNotebookId } ?: notebooks.firstOrNull()
                    if (currentNb != null) {
                        notebookToExport = currentNb
                        val safeTitle = currentNb.title.replace(Regex("[^a-zA-Z0-9_]"), "_")
                        lenoteExportLauncher.launch("$safeTitle.lenote")
                    }
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Export,
                    contentDescription = "Exportar caderno",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exportar Caderno (.lenote)", fontSize = 11.sp)
            }

            // Botão Lixeira
            OutlinedButton(
                onClick = { showTrashDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (deletedNotebooks.isNotEmpty() || deletedPages.isNotEmpty()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.Trash,
                    contentDescription = "Lixeira",
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Lixeira (${deletedNotebooks.size + deletedPages.size})",
                    fontSize = 11.sp
                )
            }
        }
    }

    // Diálogos para Criar / Renomear Caderno, Seção ou Página
    // Diálogo da Lixeira na Barra Lateral
    if (showTrashDialog) {
        AlertDialog(
            onDismissRequest = { showTrashDialog = false },
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
                        Icon(imageVector = PhosphorIcons.Regular.Trash, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Text("Lixeira do Aplicativo", fontWeight = FontWeight.SemiBold)
                    }
                    if (deletedNotebooks.isNotEmpty() || deletedPages.isNotEmpty()) {
                        TextButton(
                            onClick = { showEmptyTrashConfirmDialog = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Esvaziar", fontSize = 12.sp)
                        }
                    }
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
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
                    Text("Fechar")
                }
            }
        )
    }

    if (showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEmptyTrashConfirmDialog = false },
            title = { Text("Esvaziar Lixeira?") },
            text = { Text("Todos os cadernos e páginas na lixeira serão excluídos permanentemente.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Esvaziar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (notebookToDeletePermanently != null) {
        val nb = notebookToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { notebookToDeletePermanently = null },
            title = { Text("Excluir Caderno?") },
            text = { Text("O caderno '${nb.title}' será excluído definitivamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeleteNotebook(nb.id)
                        notebookToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { notebookToDeletePermanently = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (pageToDeletePermanently != null) {
        val p = pageToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { pageToDeletePermanently = null },
            title = { Text("Excluir Página?") },
            text = { Text("A página '${p.title}' será excluída definitivamente.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeletePage(p.id)
                        pageToDeletePermanently = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { pageToDeletePermanently = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de Seleção de Modelo para Nova Página
    if (showCreatePageTemplateDialog) {
        PageTemplatePickerDialog(
            initialTitle = "Página ${pages.size + 1}",
            initialBackground = currentPage?.pageBackground ?: PageBackground.RULED,
            initialPaperColor = currentPage?.paperColor ?: 0xFFFFFFFF,
            isCreateMode = true,
            onConfirm = { newTitle, bg, paperColor ->
                showCreatePageTemplateDialog = false
                viewModel.createNewPage(
                    title = newTitle.ifBlank { "Página ${pages.size + 1}" },
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
                viewModel.changePageTemplate(bg, paperColor)
            },
            onDismiss = { pageToChangeTemplate = null }
        )
    }

    val activeDialog = dialogMode
    if (activeDialog != null) {
        val titleText = when (activeDialog) {
            is SidebarDialogMode.CreateNotebook -> "Novo Caderno"
            is SidebarDialogMode.RenameNotebook -> "Renomear Caderno"
            is SidebarDialogMode.CreateSection -> "Nova Seção"
            is SidebarDialogMode.RenameSection -> "Renomear Seção"
            is SidebarDialogMode.RenamePage -> "Renomear Página"
        }

        AlertDialog(
            onDismissRequest = { dialogMode = null },
            title = { Text(titleText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = dialogInputText,
                    onValueChange = { dialogInputText = it },
                    singleLine = true,
                    placeholder = { Text("Digite o nome...") }
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
                    }
                ) {
                    Text("Confirmar")
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
                            Text("Mover para Lixeira", color = MaterialTheme.colorScheme.error)
                        }
                    } else if (activeDialog is SidebarDialogMode.RenameSection) {
                        TextButton(
                            onClick = {
                                viewModel.deleteSection(activeDialog.section.id)
                                dialogMode = null
                            }
                        ) {
                            Text("Excluir", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = { dialogMode = null }) {
                        Text("Cancelar")
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
 * Item de página limpo e elegante com miniatura realista, título, tipo de folha, favorito
 * e menu contextual "⋯" para ações secundárias (Mover e Excluir).
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
    var isContextMenuOpen by remember { mutableStateOf(false) }

    val thumbBitmap = remember(page.id, page.updatedAt, thumbnailVersion) {
        if (thumbnailFile != null && thumbnailFile.exists()) {
            runCatching { BitmapFactory.decodeFile(thumbnailFile.absolutePath)?.asImageBitmap() }.getOrNull()
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelect() },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Miniatura da Página (Thumbnail de alta qualidade com padrão de folha ou traços)
            PageThumbnailPreview(
                pageBackground = page.pageBackground,
                paperColorLong = page.paperColor,
                bitmap = thumbBitmap,
                modifier = Modifier.size(width = 46.dp, height = 62.dp)
            )

            // 2. Metadados Principais (Título + Tipo de Folha)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = page.title,
                    fontSize = 12.sp,
                    color = Color(0xFF0F172A),
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = page.pageBackground.label,
                    fontSize = 10.sp,
                    color = Color(0xFF64748B)
                )
            }

            // 3. Botão de Favorito Direto
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (page.isFavorite) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                    contentDescription = if (page.isFavorite) "Desfavoritar" else "Favoritar",
                    tint = if (page.isFavorite) Color(0xFFD97706) else Color(0xFF94A3B8),
                    modifier = Modifier.size(15.dp)
                )
            }

            // 4. Menu Contextual "⋯" para Ações Secundárias (Renomear, Duplicar, Mover e Excluir)
            Box {
                IconButton(
                    onClick = { isContextMenuOpen = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.DotsThreeVertical,
                        contentDescription = "Mais ações da página",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = isContextMenuOpen,
                    onDismissRequest = { isContextMenuOpen = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Renomear", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = PhosphorIcons.Regular.PencilSimple,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        onClick = {
                            isContextMenuOpen = false
                            onRename()
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("Duplicar", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Copy,
                                contentDescription = null,
                                tint = Color(0xFF475569),
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
                            text = { Text("Mover para cima", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.CaretUp,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                isContextMenuOpen = false
                                onMoveUp()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mover para baixo", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.CaretDown,
                                    contentDescription = null,
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            onClick = {
                                isContextMenuOpen = false
                                onMoveDown()
                            }
                        )
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    DropdownMenuItem(
                        text = { Text("Alterar Modelo de Fundo", fontSize = 12.sp) },
                        leadingIcon = {
                            Icon(
                                imageVector = PhosphorIcons.Regular.SlidersHorizontal,
                                contentDescription = null,
                                tint = Color(0xFF475569),
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
                                text = "Mover para Lixeira",
                                fontSize = 12.sp,
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
 * Renderiza os traços salvos (se disponíveis) ou o padrão da folha (pautada, quadriculada, etc.) nativamente.
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
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)),
        shadowElevation = 1.dp
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

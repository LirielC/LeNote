package com.example.lenote.ui

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity
import com.example.lenote.ink.PageBackground
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Paleta de cores para capas de caderno.
 */
private val NotebookCoverColors = listOf(
    Color(0xFF1E3A8A), // Navy Blue
    Color(0xFF0F766E), // Teal
    Color(0xFF15803D), // Forest Green
    Color(0xFFB45309), // Amber / Leather
    Color(0xFFBE123C), // Crimson
    Color(0xFF6B21A8), // Deep Purple
    Color(0xFF334155), // Slate Graphite
    Color(0xFF475569)  // Classic Grey
)

/**
 * Tela Inicial / Biblioteca de Cadernos (HomeScreen):
 * - Cabeçalho: título "Meus Cadernos", avatar/estatísticas e barra de busca rápida;
 * - Seção "Recentes": carrossel horizontal das últimas 3 páginas editadas com miniatura real;
 * - Grade de Cadernos: LazyVerticalGrid exibindo cadernos estilizados (proporção ~3:4, lombada lateral, capa tonal);
 * - FloatingActionButton (+ Novo Caderno) com diálogo para nome e cor;
 * - Ao tocar em um caderno, navega diretamente para a última página ativa no InkPlaygroundScreen.
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
    val deletedNotebooks by viewModel.deletedNotebooks.collectAsState()
    val deletedPages by viewModel.deletedPages.collectAsState()
    val thumbnailVersion by viewModel.thumbnailVersion.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Cadernos, 1: Lixeira
    var notebookToExport by remember { mutableStateOf<NotebookEntity?>(null) }
    var notebookToDeletePermanently by remember { mutableStateOf<NotebookEntity?>(null) }
    var pageToDeletePermanently by remember { mutableStateOf<PageEntity?>(null) }
    var showEmptyTrashConfirmDialog by remember { mutableStateOf(false) }

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

            try {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                val title = cleanName.removeSuffix(".pdf")
                viewModel.importPdfDocument(title, targetFile) { nb ->
                    onOpenNotebook(nb)
                }
            } catch (_: Exception) {}
        }
    }

    // Launcher SAF para importar pacote .lenote
    val lenoteImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':') ?: "caderno.lenote"
            val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}_$fileName")
            try {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    FileOutputStream(tempFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                viewModel.importNotebookPackage(tempFile) { importedNb ->
                    tempFile.delete()
                    if (importedNb != null) {
                        onOpenNotebook(importedNb)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // Launcher SAF para exportar pacote .lenote
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

    // Páginas recentes: ordenadas por data de modificação recente (top 5)
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab == 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Botão Importar .lenote
                    FloatingActionButton(
                        onClick = { lenoteImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Export,
                                contentDescription = "Importar .lenote",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Importar .lenote",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Botão Importar PDF
                    FloatingActionButton(
                        onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.FilePdf,
                                contentDescription = "Importar PDF",
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Importar PDF",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Botão Novo Caderno
                    FloatingActionButton(
                        onClick = { showCreateDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = CircleShape
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
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
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            } else {
                // Na aba Lixeira: botão para Esvaziar Lixeira se houver itens
                if (deletedNotebooks.isNotEmpty() || deletedPages.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { showEmptyTrashConfirmDialog = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                        shape = CircleShape,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
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
                                fontWeight = FontWeight.SemiBold
                            )
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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Cabeçalho Superior da Biblioteca
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (selectedTab == 0) PhosphorIcons.Regular.Notebook else PhosphorIcons.Regular.Trash,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = if (selectedTab == 0) "Meus Cadernos" else "Lixeira",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = if (selectedTab == 0) "${notebooks.size} caderno(s) • ${allPages.size} página(s)" else "${deletedNotebooks.size} caderno(s) e ${deletedPages.size} página(s) na lixeira",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 50.dp)
                    )
                }

                // Campo de Busca Global na Biblioteca
                Surface(
                    modifier = Modifier
                        .width(320.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
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
                                    text = "Buscar cadernos ou páginas...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
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
                                onClick = { searchQuery = "" },
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
            }

            // Abas de Navegação: Cadernos vs Lixeira
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)) }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = PhosphorIcons.Regular.Notebook, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Cadernos (${notebooks.size})", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(imageVector = PhosphorIcons.Regular.Trash, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Lixeira (${deletedNotebooks.size + deletedPages.size})", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                )
            }

            if (selectedTab == 0) {
            // 2. Seção "Recentes" (Notas editadas recentemente)
            if (recentPages.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENTES",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentPages, key = { it.id }) { page ->
                            RecentPageCard(
                                page = page,
                                thumbnailFile = viewModel.getThumbnailFile(page.id),
                                thumbnailVersion = thumbnailVersion,
                                onClick = {
                                    viewModel.selectPage(page)
                                    onOpenPage(page)
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }

            // 3. Grade de Cadernos (Aspecto visual de cadernos reais)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "TODOS OS CADERNOS (${filteredNotebooks.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (filteredNotebooks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = "Nenhum caderno encontrado",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 200.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredNotebooks, key = { it.id }) { nb ->
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
                                }
                            )
                        }
                    }
                }
            }
            } else {
                // Conteúdo da Aba Lixeira (Cadernos e Páginas Deletados)
                TrashContent(
                    deletedNotebooks = deletedNotebooks,
                    deletedPages = deletedPages,
                    onRestoreNotebook = { viewModel.restoreNotebookFromTrash(it.id) },
                    onDeleteNotebookPermanently = { notebookToDeletePermanently = it },
                    onRestorePage = { viewModel.restorePageFromTrash(it.id) },
                    onDeletePagePermanently = { pageToDeletePermanently = it },
                    modifier = Modifier.weight(1f)
                )
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
            title = { Text("Esvaziar Lixeira?") },
            text = { Text("Todos os cadernos, seções, páginas e mídias da lixeira serão excluídos definitivamente do dispositivo. Esta ação não pode ser desfeita.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrash()
                        showEmptyTrashConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Esvaziar Definitivamente")
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
            title = { Text("Excluir Caderno Definitivamente?") },
            text = { Text("O caderno '${nb.title}' e todas as suas páginas serão removidos permanentemente do armazenamento.") },
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
            title = { Text("Excluir Página Definitivamente?") },
            text = { Text("A página '${p.title}' será apagada permanentemente.") },
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

}

/**
 * Card de Caderno com visual realista: proporção ~3:4, lombada lateral texturizada,
 * cor de capa customizável, etiqueta central elegante e metadados.
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
    val coverColor = Color(notebook.colorHex)
    val dateFormat = remember { SimpleDateFormat("dd MMM, yyyy", Locale.getDefault()) }
    val lastUpdateStr = remember(notebook.updatedAt) {
        dateFormat.format(Date(notebook.updatedAt))
    }
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .aspectRatio(0.75f)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = coverColor)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // Lombada lateral esquerda do caderno (Spine)
            Box(
                modifier = Modifier
                    .width(18.dp)
                    .fillMaxHeight()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.15f),
                                Color.White.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.20f)
                            )
                        )
                    )
            )

            // Capa principal do caderno com etiqueta e detalhes
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Etiqueta central estilizada de caderno com menu ⋯
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.White.copy(alpha = 0.95f),
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = notebook.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "$sectionCount seções • $pageCount páginas",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        Box {
                            IconButton(
                                onClick = { showMenu = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.DotsThreeVertical,
                                    contentDescription = "Opções do caderno",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Exportar (.lenote)") },
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
                                    text = { Text("Mover para Lixeira", color = MaterialTheme.colorScheme.error) },
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
                }

                // Rodapé do Caderno: Data de modificação e ícone discreto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = lastUpdateStr,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                    Icon(
                        imageVector = PhosphorIcons.Regular.CaretRight,
                        contentDescription = "Abrir caderno",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Card de Página Recente com miniatura realista.
 */
@Composable
private fun RecentPageCard(
    page: PageEntity,
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

    val timeFormat = remember { SimpleDateFormat("HH:mm - dd/MM", Locale.getDefault()) }
    val formattedTime = remember(page.updatedAt) { timeFormat.format(Date(page.updatedAt)) }

    Card(
        modifier = modifier
            .width(180.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Miniatura gráfica com textura de folha
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFFFCFCFA),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap,
                        contentDescription = page.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    MiniPageBackgroundCanvas(pageBackground = page.pageBackground)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = page.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val lineColor = Color(0xFF94A3B8).copy(alpha = 0.5f)
        when (pageBackground) {
            PageBackground.BLANK -> {}
            PageBackground.RULED -> {
                val spacing = size.height / 7f
                for (i in 1..6) {
                    val y = i * spacing
                    drawLine(
                        color = lineColor,
                        start = Offset(4f, y),
                        end = Offset(size.width - 4f, y),
                        strokeWidth = 0.8f
                    )
                }
            }
            PageBackground.GRID -> {
                val spacingX = size.width / 6f
                val spacingY = size.height / 7f
                for (i in 1 until 6) {
                    val x = i * spacingX
                    drawLine(color = lineColor.copy(alpha = 0.3f), start = Offset(x, 2f), end = Offset(x, size.height - 2f), strokeWidth = 0.6f)
                }
                for (i in 1 until 7) {
                    val y = i * spacingY
                    drawLine(color = lineColor.copy(alpha = 0.3f), start = Offset(2f, y), end = Offset(size.width - 2f, y), strokeWidth = 0.6f)
                }
            }
            PageBackground.DOTS -> {
                val spacingX = size.width / 5f
                val spacingY = size.height / 7f
                for (i in 1..4) {
                    for (j in 1..6) {
                        drawCircle(color = lineColor, radius = 0.9f, center = Offset(i * spacingX, j * spacingY))
                    }
                }
            }
            PageBackground.MATHEMATICS -> {
                val spacing = size.height / 8f
                for (i in 1..7) {
                    val y = i * spacing
                    val isAxis = i == 4
                    drawLine(color = if (isAxis) Color(0xFF64748B) else lineColor.copy(alpha = 0.3f), start = Offset(2f, y), end = Offset(size.width - 2f, y), strokeWidth = if (isAxis) 1f else 0.5f)
                }
            }
        }
    }
}

/**
 * Diálogo para criação de novo caderno com título e seletor de cor da capa.
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
        title = {
            Text(
                text = "Novo Caderno",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nome do Caderno") },
                    placeholder = { Text("Ex: Física Quântica, Ideias...") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Cor da Capa",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = color }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        } else Modifier
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
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Criar Caderno")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
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
            modifier = modifier
                .fillMaxSize()
                .padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Trash,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Text(
                    text = "A lixeira está vazia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Itens movidos para a lixeira aparecerão aqui",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (deletedNotebooks.isNotEmpty()) {
                item {
                    Text(
                        text = "CADERNOS NA LIXEIRA (${deletedNotebooks.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                items(deletedNotebooks, key = { "nb_${it.id}" }) { nb ->
                    val delDate = remember(nb.deletedAt) {
                        nb.deletedAt?.let { dateFormat.format(Date(it)) } ?: "Data desconhecida"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(Color(nb.colorHex), CircleShape)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = nb.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Excluído em: $delDate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onRestoreNotebook(nb) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = PhosphorIcons.Regular.ArrowCounterClockwise, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Restaurar", fontSize = 12.sp)
                                    }
                                }
                                TextButton(
                                    onClick = { onDeleteNotebookPermanently(nb) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Excluir", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            if (deletedPages.isNotEmpty()) {
                item {
                    Text(
                        text = "PÁGINAS NA LIXEIRA (${deletedPages.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
                items(deletedPages, key = { "pg_${it.id}" }) { page ->
                    val delDate = remember(page.deletedAt) {
                        page.deletedAt?.let { dateFormat.format(Date(it)) } ?: "Data desconhecida"
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = PhosphorIcons.Regular.FileText,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        text = page.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Excluído em: $delDate",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onRestorePage(page) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(imageVector = PhosphorIcons.Regular.ArrowCounterClockwise, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Text("Restaurar", fontSize = 12.sp)
                                    }
                                }
                                TextButton(
                                    onClick = { onDeletePagePermanently(page) },
                                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Text("Excluir", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.example.lenote.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import java.io.FileOutputStream

import android.content.ActivityNotFoundException
import android.widget.FrameLayout
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.ink.authoring.InProgressStrokesView
import com.example.lenote.data.PageEntity
import com.example.lenote.ink.EraserSize
import com.example.lenote.ink.FinishedStrokesView
import com.example.lenote.ink.HighlighterSize
import com.example.lenote.ink.InkingState
import com.example.lenote.ink.InkingTool
import com.example.lenote.ink.JetpackInkEngine
import com.example.lenote.export.ExportResult
import com.example.lenote.export.ExportViewerHelper
import com.example.lenote.export.PageExporter
import com.example.lenote.ink.PageBackground
import com.example.lenote.ink.PenStrokeSize
import com.example.lenote.ink.SmoothingLevel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.util.Locale

/**
 * Tema de cores executivo e limpo (Navy Blue & Slate) para o LeNote.
 * Elimina o roxo padrão do Material 3, garantindo uma estética profissional de caderno/papel.
 */
private val LeNoteLightColorScheme = lightColorScheme(
    primary = Color(0xFF1E3A8A),           // Azul Marinho Executivo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEEF2FF),  // Índigo Suave
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF2563EB),         // Azul Real
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1F5F9),// Slate 100
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFF0D9488),          // Teal sutil
    onTertiary = Color.White,
    surface = Color(0xFFFFFFFF),           // Branco Puro
    onSurface = Color(0xFF0F172A),          // Slate 900
    surfaceVariant = Color(0xFFF8FAFC),    // Slate 50
    onSurfaceVariant = Color(0xFF64748B),  // Slate 500
    outline = Color(0xFFCBD5E1),           // Slate 300
    outlineVariant = Color(0xFFE2E8F0),    // Slate 200 (Bordas neutras)
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    error = Color(0xFFDC2626),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF991B1B)
)

/**
 * Estado da barra de confirmação flutuante de exportação.
 */
data class ExportConfirmationState(
    val exportResult: ExportResult,
    val title: String,
    val subtitle: String,
    val isError: Boolean = false,
    val canOpen: Boolean = true
)

/**
 * Tela principal de anotações do LeNote:
 * - Canvas nativo ocupando a área dominante;
 * - Header compacto (48dp) com indicador de autosave de baixa interferência;
 * - Toolbar flutuante refinada (área de toque 44dp, ícones 24dp, separadores discretos);
 * - Painéis contextuais com amostras visuais de espessura de traço;
 * - Opções da folha com zero truncamento de texto e altura 20% mais compacta;
 * - Confirmação flutuante moderna de exportação (PDF/PNG) com ação direta de Abrir/Ver arquivo.
 */
@Composable
fun InkPlaygroundScreen(
    viewModel: InkPlaygroundViewModel? = null,
    engine: JetpackInkEngine = viewModel?.engine ?: remember { JetpackInkEngine() },
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel?.initializeIfNeeded(context)
    }

    val vmSavedData = viewModel?.savedStrokesData?.collectAsState()
    val vmSaveStatus = viewModel?.saveStatusMessage?.collectAsState()
    val vmIsSaving = viewModel?.isSaving?.collectAsState()
    val vmCurrentPage = viewModel?.currentPage?.collectAsState()
    val vmPages = viewModel?.pages?.collectAsState()
    val vmIsSidebarOpen = viewModel?.isSidebarOpen?.collectAsState()
    val vmIsDiagnosticsVisible = viewModel?.isDiagnosticsVisible?.collectAsState()
    val textBlocks by (viewModel?.textBlocks ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val imageBlocks by (viewModel?.imageBlocks ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val isTextToolActive by (viewModel?.isTextToolActive ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()

    // Photo picker para inserção de imagem na folha
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val imagesDir = File(context.filesDir, "images").apply { mkdirs() }
            val targetFile = File(imagesDir, "img_${System.currentTimeMillis()}.png")
            try {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    FileOutputStream(targetFile).use { outStream ->
                        inStream.copyTo(outStream)
                    }
                }
                viewModel?.addImageBlock(targetFile, 150f, 150f)
            } catch (_: Exception) {}
        }
    }

    var localSavedStrokesData by remember { mutableStateOf<ByteArray?>(null) }
    var localSaveStatusMessage by remember { mutableStateOf<String?>(null) }
    var localDiagnosticsVisible by remember { mutableStateOf(false) }

    val savedStrokesData = vmSavedData?.value ?: localSavedStrokesData
    val saveStatusMessage = vmSaveStatus?.value ?: localSaveStatusMessage
    val isSaving = vmIsSaving?.value ?: false
    val currentPage = vmCurrentPage?.value
    val pages = vmPages?.value ?: emptyList()
    val currentPageIndex = pages.indexOfFirst { it.id == currentPage?.id }
    val canMoveUp = currentPageIndex > 0
    val canMoveDown = currentPageIndex >= 0 && currentPageIndex < pages.size - 1

    val isSidebarOpen = vmIsSidebarOpen?.value ?: false
    val isDiagnosticsVisible = vmIsDiagnosticsVisible?.value ?: localDiagnosticsVisible

    val engineState by engine.state.collectAsState()

    var showRenamePageDialog by remember { mutableStateOf(false) }
    var renamePageInput by remember { mutableStateOf("") }
    var showSheetOptionsModal by remember { mutableStateOf(false) }
    var showTemplatePickerDialog by remember { mutableStateOf(false) }
    var showPageActionsModal by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showDeletePageConfirmDialog by remember { mutableStateOf(false) }

    var exportConfirmation by remember { mutableStateOf<ExportConfirmationState?>(null) }

    LaunchedEffect(exportConfirmation) {
        if (exportConfirmation != null) {
            delay(7500L)
            exportConfirmation = null
        }
    }

    MaterialTheme(colorScheme = LeNoteLightColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // A) CANVAS / FOLHA: Elemento visual dominante ocupando toda a tela
                InkCanvas(
                    engine = engine,
                    modifier = Modifier.fillMaxSize()
                )

                // Camada Interativa de Mídia Rica (Caixas de Texto e Imagens)
                CanvasMediaOverlay(
                    engine = engine,
                    textBlocks = textBlocks,
                    imageBlocks = imageBlocks,
                    isTextToolActive = isTextToolActive,
                    onAddTextBlock = { x, y ->
                        viewModel?.addTextBlock(x, y)
                        viewModel?.setTextToolActive(false)
                    },
                    onUpdateTextBlock = { viewModel?.updateTextBlock(it) },
                    onDeleteTextBlock = { viewModel?.deleteTextBlock(it) },
                    onUpdateImageBlock = { viewModel?.updateImageBlock(it) },
                    onDeleteImageBlock = { viewModel?.deleteImageBlock(it) },
                    modifier = Modifier.fillMaxSize()
                )

                // B) HEADER COMPACTO: Pequeno, discreto (48dp), sem competir com "Página 1"
                CompactHeaderBar(
                    currentPage = currentPage,
                    isSaving = isSaving,
                    saveStatus = saveStatusMessage,
                    onNavigateBack = {
                        viewModel?.flushCurrentPageSave()
                        onNavigateBack?.invoke()
                    },
                    onToggleSidebar = { viewModel?.toggleSidebar() },
                    onRenamePageClick = {
                        renamePageInput = currentPage?.title ?: "Página 1"
                        showRenamePageDialog = true
                    },
                    onToggleFavorite = { viewModel?.toggleCurrentPageFavorite() },
                    onOpenMoreOptions = { showPageActionsModal = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )

                // C) TOOLBAR FLUTUANTE COMPACTA & PAINÉIS CONTEXTUAIS
                ToolbarOverlayStateHost(
                    engine = engine,
                    viewModel = viewModel,
                    onInsertImage = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    onSave = { strokeCount ->
                        if (viewModel != null) {
                            viewModel.saveCurrentStrokes()
                        } else {
                            val bytes = engine.saveStrokes()
                            localSavedStrokesData = bytes
                            localSaveStatusMessage = "Salvo: ${bytes.size} bytes ($strokeCount traços)"
                        }
                    },
                    onLoad = {
                        if (viewModel != null) {
                            viewModel.loadSavedStrokes()
                        } else {
                            savedStrokesData?.let { bytes ->
                                engine.loadStrokes(bytes)
                                localSaveStatusMessage = "Carregado com sucesso!"
                            } ?: run {
                                localSaveStatusMessage = "Nenhum traço salvo em memória!"
                            }
                        }
                    },
                    canLoad = savedStrokesData != null,
                    onPromptClear = { showClearConfirmDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 58.dp)
                )

                // D) TELEMETRIA & DIAGNÓSTICO (Opcional, ativável em Mais Opções -> Diagnósticos)
                if (isDiagnosticsVisible) {
                    DiagnosticsOverlayStateHost(
                        engine = engine,
                        saveStatus = saveStatusMessage,
                        onClose = {
                            if (viewModel != null) {
                                viewModel.toggleDiagnosticsOverlay()
                            } else {
                                localDiagnosticsVisible = false
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }

                // E) PAINEL LATERAL RECOLHÍVEL (Cadernos -> Seções -> Páginas)
                if (viewModel != null) {
                    AnimatedVisibility(
                        visible = isSidebarOpen,
                        enter = slideInHorizontally(initialOffsetX = { -it }),
                        exit = slideOutHorizontally(targetOffsetX = { -it }),
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        CollapsibleNavigationSidebar(
                            viewModel = viewModel,
                            onClose = { viewModel.setSidebarOpen(false) }
                        )
                    }
                }

                // F) CONFIRMAÇÃO FLUTUANTE DE EXPORTAÇÃO (Com ação direta "Abrir PDF" ou "Ver imagem")
                AnimatedVisibility(
                    visible = exportConfirmation != null,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 24.dp)
                ) {
                    exportConfirmation?.let { confState ->
                        ExportConfirmationBanner(
                            state = confState,
                            onOpen = {
                                ExportViewerHelper.openExportedFile(context, confState.exportResult)
                                    .onSuccess {
                                        exportConfirmation = null
                                    }
                                    .onFailure { ex ->
                                        val appType = if (confState.exportResult.mimeType == "application/pdf") "PDF" else "imagem"
                                        if (ex is ActivityNotFoundException) {
                                            exportConfirmation = confState.copy(
                                                title = "Não foi encontrado um aplicativo para abrir $appType.",
                                                subtitle = confState.exportResult.displayPath,
                                                isError = true,
                                                canOpen = false
                                            )
                                        } else {
                                            val docType = if (confState.exportResult.mimeType == "application/pdf") "PDF" else "Imagem"
                                            exportConfirmation = confState.copy(
                                                title = "$docType salvo com sucesso, mas não foi possível abrir o arquivo.",
                                                subtitle = confState.exportResult.displayPath,
                                                isError = true,
                                                canOpen = false
                                            )
                                        }
                                    }
                            },
                            onDismiss = { exportConfirmation = null }
                        )
                    }
                }

                // G) INDICADOR E CONTROLE DISCRETO DE ZOOM (Canto inferior direito)
                ZoomIndicatorControl(
                    zoomLevel = engineState.zoomLevel,
                    onZoomIn = { engine.zoomIn() },
                    onZoomOut = { engine.zoomOut() },
                    onResetZoom = { engine.resetZoom() },
                    onFitToPage = { engine.fitToPage() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }
        }
    }

    // Diálogo de Renomear Página
    if (showRenamePageDialog) {
        AlertDialog(
            onDismissRequest = { showRenamePageDialog = false },
            title = {
                Text(
                    text = "Renomear Página",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                OutlinedTextField(
                    value = renamePageInput,
                    onValueChange = { renamePageInput = it },
                    singleLine = true,
                    placeholder = { Text("Título da página") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (viewModel != null) {
                            viewModel.renameCurrentPage(renamePageInput)
                        }
                        showRenamePageDialog = false
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenamePageDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Confirmação para Ação Destrutiva "Limpar Página"
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = PhosphorIcons.Regular.Trash,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Limpar página?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Todos os traços desta página serão apagados. A ação pode ser revertida usando o botão Desfazer.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        engine.clear()
                        showClearConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Limpar tudo")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Confirmação para Ação Destrutiva "Excluir Página"
    if (showDeletePageConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeletePageConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = PhosphorIcons.Regular.Trash,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Excluir página?",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Text(
                    text = "Esta página e todos os seus traços serão excluídos permanentemente.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentPage?.let { page ->
                            viewModel?.deletePage(page.id) ?: engine.clear()
                        }
                        showDeletePageConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Excluir")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePageConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // 1. Diálogo dedicado: Opções da Folha (Tipo de papel e Suavização)
    if (showSheetOptionsModal) {
        val currentState by engine.state.collectAsState()
        SheetOptionsDialog(
            state = currentState,
            onPageBackgroundSelect = { engine.setPageBackground(it) },
            onSmoothingSelect = { engine.setSmoothingLevel(it) },
            onDismiss = { showSheetOptionsModal = false }
        )
    }

    // 2. Diálogo dedicado: Ações da Página (Menu "⋮" do Header)
    if (showPageActionsModal) {
        PageActionsDialog(
            currentPage = currentPage,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onRenamePage = {
                renamePageInput = currentPage?.title ?: "Página 1"
                showRenamePageDialog = true
                showPageActionsModal = false
            },
            onDuplicatePage = {
                viewModel?.duplicateCurrentPage()
                showPageActionsModal = false
            },
            onMoveUp = {
                currentPage?.let { page ->
                    viewModel?.movePageOrder(page.id, moveUp = true)
                }
            },
            onMoveDown = {
                currentPage?.let { page ->
                    viewModel?.movePageOrder(page.id, moveUp = false)
                }
            },
            onToggleFavorite = {
                viewModel?.toggleCurrentPageFavorite()
            },
            onOpenSheetOptions = {
                showPageActionsModal = false
                showTemplatePickerDialog = true
            },
            onExportPdf = {
                showPageActionsModal = false
                if (viewModel != null) {
                    viewModel.exportCurrentPageToPdf { res ->
                        exportConfirmation = if (res.isSuccess) {
                            ExportConfirmationState(
                                exportResult = res,
                                title = "PDF exportado com sucesso",
                                subtitle = res.displayPath,
                                isError = false,
                                canOpen = true
                            )
                        } else {
                            ExportConfirmationState(
                                exportResult = res,
                                title = "Não foi possível exportar o PDF.",
                                subtitle = res.errorMessage ?: "Erro ao gravar o arquivo.",
                                isError = true,
                                canOpen = false
                            )
                        }
                    }
                } else {
                    val pageTitle = currentPage?.title ?: "Pagina 1"
                    val bg = engine.state.value.pageBackground
                    val strokes = engine.getFinishedStrokesSnapshot()
                    try {
                        val res = PageExporter.exportPageToPdf(context, pageTitle, bg, strokes)
                        exportConfirmation = ExportConfirmationState(
                            exportResult = res,
                            title = "PDF exportado com sucesso",
                            subtitle = res.displayPath,
                            isError = false,
                            canOpen = true
                        )
                    } catch (e: Exception) {
                        val dummy = File(context.cacheDir, "err.pdf")
                        exportConfirmation = ExportConfirmationState(
                            exportResult = ExportResult(dummy, null, "", isSuccess = false),
                            title = "Não foi possível exportar o PDF.",
                            subtitle = e.message ?: "Erro ao gravar o arquivo.",
                            isError = true,
                            canOpen = false
                        )
                    }
                }
            },
            onExportPng = {
                showPageActionsModal = false
                if (viewModel != null) {
                    viewModel.exportCurrentPageToPng { res ->
                        exportConfirmation = if (res.isSuccess) {
                            ExportConfirmationState(
                                exportResult = res,
                                title = "Imagem exportada com sucesso",
                                subtitle = res.displayPath,
                                isError = false,
                                canOpen = true
                            )
                        } else {
                            ExportConfirmationState(
                                exportResult = res,
                                title = "Não foi possível exportar a imagem.",
                                subtitle = res.errorMessage ?: "Erro ao gravar o arquivo.",
                                isError = true,
                                canOpen = false
                            )
                        }
                    }
                } else {
                    val pageTitle = currentPage?.title ?: "Pagina 1"
                    val bg = engine.state.value.pageBackground
                    val strokes = engine.getFinishedStrokesSnapshot()
                    try {
                        val res = PageExporter.exportPageToPng(context, pageTitle, bg, strokes)
                        exportConfirmation = ExportConfirmationState(
                            exportResult = res,
                            title = "Imagem exportada com sucesso",
                            subtitle = res.displayPath,
                            isError = false,
                            canOpen = true
                        )
                    } catch (e: Exception) {
                        val dummy = File(context.cacheDir, "err.png")
                        exportConfirmation = ExportConfirmationState(
                            exportResult = ExportResult(dummy, null, "", isSuccess = false),
                            title = "Não foi possível exportar a imagem.",
                            subtitle = e.message ?: "Erro ao gravar o arquivo.",
                            isError = true,
                            canOpen = false
                        )
                    }
                }
            },
            onDeletePage = {
                showPageActionsModal = false
                showDeletePageConfirmDialog = true
            },
            onPromptClear = {
                showPageActionsModal = false
                showClearConfirmDialog = true
            },
            isDiagnosticsVisible = isDiagnosticsVisible,
            onToggleDiagnostics = {
                if (viewModel != null) {
                    viewModel.toggleDiagnosticsOverlay()
                } else {
                    localDiagnosticsVisible = !localDiagnosticsVisible
                }
            },
            onDismiss = { showPageActionsModal = false }
        )
    }
}

/**
 * Cabeçalho compacto e discreto (altura ~48dp).
 * Esquerda: Botão "Cadernos", Título da página, Estrela de favorito.
 * Direita: Indicador discreto de autosave e menu "Mais" (⋯).
 */
@Composable
private fun CompactHeaderBar(
    currentPage: PageEntity?,
    isSaving: Boolean,
    saveStatus: String?,
    onNavigateBack: (() -> Unit)?,
    onToggleSidebar: () -> Unit,
    onRenamePageClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenMoreOptions: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .statusBarsPadding()
            .height(52.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lado Esquerdo: Cadernos + Título da Página + Favorito
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onNavigateBack != null) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.CaretLeft,
                            contentDescription = "Voltar para Biblioteca",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    onClick = onToggleSidebar,
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Notebook,
                            contentDescription = "Cadernos",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Cadernos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(18.dp)
                        .background(Color(0xFFE2E8F0))
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onRenamePageClick() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = currentPage?.title ?: "Página 1",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = PhosphorIcons.Regular.PencilSimple,
                        contentDescription = "Renomear",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(13.dp)
                    )
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (currentPage?.isFavorite == true) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                        contentDescription = if (currentPage?.isFavorite == true) "Desfavoritar" else "Favoritar",
                        tint = if (currentPage?.isFavorite == true) Color(0xFFD97706) else Color(0xFFCBD5E1),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            // Lado Direito: Indicador Discreto de Autosave + Menu Mais (⋯)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isSaving) {
                    Text(
                        text = "Salvando...",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF94A3B8)
                    )
                } else if (!saveStatus.isNullOrBlank() && saveStatus != "Salvo") {
                    Text(
                        text = saveStatus,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF1E3A8A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Check,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Salvo",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                IconButton(
                    onClick = onOpenMoreOptions,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.DotsThreeVertical,
                        contentDescription = "Mais opções",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsOverlayStateHost(
    engine: JetpackInkEngine,
    saveStatus: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by engine.state.collectAsState()
    DiagnosticsOverlay(
        state = state,
        saveStatus = saveStatus,
        onClose = onClose,
        modifier = modifier
    )
}

@Composable
private fun ToolbarOverlayStateHost(
    engine: JetpackInkEngine,
    viewModel: InkPlaygroundViewModel? = null,
    onInsertImage: () -> Unit = {},
    onSave: (Int) -> Unit,
    onLoad: () -> Unit,
    canLoad: Boolean,
    onPromptClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val toolbarStateFlow = remember(engine) {
        engine.state.distinctUntilChanged { old, new ->
            old.currentTool == new.currentTool &&
                old.eraserSize == new.eraserSize &&
                old.pageBackground == new.pageBackground &&
                old.smoothingLevel == new.smoothingLevel &&
                old.canUndo == new.canUndo &&
                old.canRedo == new.canRedo &&
                old.strokeCount == new.strokeCount &&
                old.selectedStrokeCount == new.selectedStrokeCount &&
                old.isStylusButtonEraserActive == new.isStylusButtonEraserActive
        }
    }
    val state by toolbarStateFlow.collectAsState(initial = engine.state.value)

    val isTextActive by (viewModel?.isTextToolActive ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()

    ToolbarOverlay(
        state = state,
        onToolSelect = {
            viewModel?.setTextToolActive(false)
            engine.setTool(it)
        },
        isTextActive = isTextActive,
        onToggleText = {
            val next = !isTextActive
            viewModel?.setTextToolActive(next)
        },
        onInsertImage = { onInsertImage() },
        onEraserSizeSelect = { engine.setEraserSize(it) },
        onPageBackgroundSelect = { engine.setPageBackground(it) },
        onSmoothingSelect = { engine.setSmoothingLevel(it) },
        onUndo = { engine.undo() },
        onRedo = { engine.redo() },
        onClear = onPromptClear,
        onDeleteSelection = { engine.deleteSelectedStrokes() },
        onDuplicateSelection = { engine.duplicateSelectedStrokes() },
        onRecolorSelection = { engine.recolorSelectedStrokes(it) },
        onClearSelection = { engine.clearSelection() },
        onSave = { onSave(state.strokeCount) },
        onLoad = onLoad,
        canLoad = canLoad,
        modifier = modifier
    )
}

/**
 * Área de visualização nativa de traços (Jetpack Ink).
 */
@Composable
fun InkCanvas(
    engine: JetpackInkEngine,
    modifier: Modifier = Modifier
) {
    DisposableEffect(engine) {
        onDispose {
            engine.detachView()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            val root = FrameLayout(context)

            val finishedView = FinishedStrokesView(context).apply {
                tag = "FinishedStrokesView"
            }
            val inProgressView = InProgressStrokesView(context).apply {
                tag = "InProgressStrokesView"
                eagerInit()
            }

            root.addView(
                finishedView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            root.addView(
                inProgressView,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            )

            engine.attachView(inProgressView)
            engine.attachFinishedView(finishedView)

            root.setOnTouchListener { _, event ->
                engine.onTouchEvent(event)
            }
            root.setOnHoverListener { _, event ->
                engine.onHoverEvent(event)
            }

            root
        }
    )
}

/**
 * Painel de Diagnósticos e Telemetria de Entrada.
 */
@Composable
fun DiagnosticsOverlay(
    state: InkingState,
    saveStatus: String?,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.widthIn(max = 420.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8FAFC)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Bug,
                        contentDescription = null,
                        tint = Color(0xFF1E3A8A),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Telemetria Jetpack Ink",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = "Fechar",
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Gesto: ${state.gestureState.name} | Botão Stylus Borracha: ${if (state.isStylusButtonEraserActive) "ATIVO" else "OFF"}",
                fontSize = 11.sp,
                color = Color(0xFF475569)
            )
            Text(
                text = "ACTION: ${state.lastActionName} | pointerCount: ${state.activePointerCount} | FLAG_CANCELED: ${state.isFlagCanceled}",
                fontSize = 11.sp
            )
            Text(
                text = "Decisão: ${state.lastDecisionSummary}",
                fontSize = 11.sp
            )

            if (state.pointerDiagnostics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Ponteiros Ativos:",
                    fontSize = 11.sp,
                    color = Color(0xFF1E3A8A)
                )
                state.pointerDiagnostics.forEach { ptr ->
                    Text(
                        text = String.format(
                            Locale.US,
                            " • [idx=%d|id=%d] %s | P=%.3f | (%.0f, %.0f) -> %s",
                            ptr.pointerIndex,
                            ptr.pointerId,
                            ptr.toolTypeName,
                            ptr.pressure,
                            ptr.x,
                            ptr.y,
                            ptr.decision
                        ),
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(
                    Locale.US,
                    "Traços: %d (Sel: %d) | Folha: %s | Borracha: %s | Suavização: %s",
                    state.strokeCount,
                    state.selectedStrokeCount,
                    state.pageBackground.label,
                    state.eraserSize.name,
                    state.smoothingLevel.label
                ),
                fontSize = 11.sp
            )
            Text(
                text = String.format(
                    Locale.US,
                    "Zoom: %.2fx | Pan: (%.0f, %.0f)",
                    state.zoomLevel,
                    state.panOffsetX,
                    state.panOffsetY
                ),
                fontSize = 11.sp
            )

            if (!saveStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Status: $saveStatus",
                    fontSize = 10.sp,
                    color = Color(0xFF0D9488)
                )
            }
        }
    }
}

private enum class ContextualPanelType {
    NONE,
    PEN,
    HIGHLIGHTER,
    ERASER
}

/**
 * Toolbar Flutuante Compacta:
 * - Área de toque de 44x44dp;
 * - Ícones aumentados para 24dp;
 * - Espaçamento refinado entre ferramentas e separadores discretos;
 * - Altura mantida compacta (~48dp).
 */
@Composable
fun ToolbarOverlay(
    state: InkingState,
    onToolSelect: (InkingTool) -> Unit,
    onEraserSizeSelect: (EraserSize) -> Unit,
    onPageBackgroundSelect: (PageBackground) -> Unit,
    onSmoothingSelect: (SmoothingLevel) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onDeleteSelection: () -> Unit = {},
    onDuplicateSelection: () -> Unit = {},
    onRecolorSelection: (Int) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onSave: () -> Unit,
    onLoad: () -> Unit,
    canLoad: Boolean,
    isTextActive: Boolean = false,
    onToggleText: () -> Unit = {},
    onInsertImage: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val penColors = listOf(
        Color(0xFF000000), // Preto
        Color(0xFF1E88E5), // Azul
        Color(0xFFE53935), // Vermelho
        Color(0xFF43A047), // Verde
        Color(0xFF8E24AA), // Roxo
        Color(0xFFFB8C00)  // Laranja
    )

    val highlighterColors = listOf(
        Color(0x66FDD835), // Amarelo
        Color(0x664CAF50), // Verde
        Color(0x6629B6F6), // Azul
        Color(0x66EC407A), // Rosa
        Color(0x66FFA726), // Laranja
        Color(0x66AB47BC)  // Roxo
    )

    var lastPenColorInt by remember {
        val initialColor = (state.currentTool as? InkingTool.Pen)?.colorInt ?: 0xFF000000.toInt()
        mutableIntStateOf(initialColor)
    }
    var lastPenSize by remember {
        val initialSize = (state.currentTool as? InkingTool.Pen)?.size ?: PenStrokeSize.MEDIUM.sizeWorld
        mutableFloatStateOf(initialSize)
    }

    var lastHighlighterColorInt by remember {
        val initialColor = (state.currentTool as? InkingTool.Highlighter)?.colorInt ?: 0x66FDD835.toInt()
        mutableIntStateOf(initialColor)
    }
    var lastHighlighterSize by remember {
        val initialSize = (state.currentTool as? InkingTool.Highlighter)?.size ?: HighlighterSize.MEDIUM.sizeWorld
        mutableFloatStateOf(initialSize)
    }

    var activeContextualPanel by remember { mutableStateOf(ContextualPanelType.NONE) }

    val isPenActive = state.currentTool is InkingTool.Pen && !state.isStylusButtonEraserActive
    val isHighlighterActive = state.currentTool is InkingTool.Highlighter && !state.isStylusButtonEraserActive
    val isEraserActive = state.currentTool is InkingTool.Eraser || state.isStylusButtonEraserActive
    val isLassoActive = state.currentTool is InkingTool.Lasso && !state.isStylusButtonEraserActive

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Dock Principal da Toolbar Flutuante (Estilo Floating Island)
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
            shadowElevation = 6.dp,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Grupo 1: Ferramentas Principais de Desenho
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Caneta
                    ToolbarToolButton(
                        iconRegular = PhosphorIcons.Regular.Pen,
                        iconFill = PhosphorIcons.Fill.Pen,
                        contentDescription = "Caneta",
                        isSelected = isPenActive,
                        badgeColor = Color(lastPenColorInt),
                        onClick = {
                            if (isPenActive) {
                                activeContextualPanel = if (activeContextualPanel == ContextualPanelType.PEN) {
                                    ContextualPanelType.NONE
                                } else {
                                    ContextualPanelType.PEN
                                }
                            } else {
                                onToolSelect(InkingTool.Pen(colorInt = lastPenColorInt, size = lastPenSize))
                                activeContextualPanel = ContextualPanelType.PEN
                            }
                        }
                    )

                    // Marca-texto
                    ToolbarToolButton(
                        iconRegular = PhosphorIcons.Regular.HighlighterCircle,
                        iconFill = PhosphorIcons.Fill.HighlighterCircle,
                        contentDescription = "Marca-texto",
                        isSelected = isHighlighterActive,
                        badgeColor = Color(lastHighlighterColorInt),
                        onClick = {
                            if (isHighlighterActive) {
                                activeContextualPanel = if (activeContextualPanel == ContextualPanelType.HIGHLIGHTER) {
                                    ContextualPanelType.NONE
                                } else {
                                    ContextualPanelType.HIGHLIGHTER
                                }
                            } else {
                                onToolSelect(InkingTool.Highlighter(colorInt = lastHighlighterColorInt, size = lastHighlighterSize))
                                activeContextualPanel = ContextualPanelType.HIGHLIGHTER
                            }
                        }
                    )

                    // Borracha
                    ToolbarToolButton(
                        iconRegular = PhosphorIcons.Regular.Eraser,
                        iconFill = PhosphorIcons.Fill.Eraser,
                        contentDescription = "Borracha",
                        isSelected = isEraserActive,
                        onClick = {
                            if (isEraserActive) {
                                activeContextualPanel = if (activeContextualPanel == ContextualPanelType.ERASER) {
                                    ContextualPanelType.NONE
                                } else {
                                    ContextualPanelType.ERASER
                                }
                            } else {
                                onToolSelect(InkingTool.Eraser(state.eraserSize))
                                activeContextualPanel = ContextualPanelType.ERASER
                            }
                        }
                    )

                    // Laço
                    ToolbarToolButton(
                        iconRegular = PhosphorIcons.Regular.Lasso,
                        iconFill = PhosphorIcons.Fill.Lasso,
                        contentDescription = "Laço",
                        isSelected = isLassoActive,
                        onClick = {
                            onToolSelect(InkingTool.Lasso)
                            activeContextualPanel = ContextualPanelType.NONE
                        }
                    )

                    // Caixa de Texto
                    ToolbarToolButton(
                        iconRegular = PhosphorIcons.Regular.TextT,
                        iconFill = PhosphorIcons.Regular.TextT,
                        contentDescription = "Texto",
                        isSelected = isTextActive,
                        onClick = onToggleText
                    )

                    // Inserir Imagem
                    ToolbarToolButton(
                        iconRegular = PhosphorIcons.Regular.Image,
                        iconFill = PhosphorIcons.Regular.Image,
                        contentDescription = "Inserir Imagem",
                        isSelected = false,
                        onClick = onInsertImage
                    )
                }

                // Divisor Vertical 1
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .width(1.dp)
                        .height(20.dp)
                        .background(Color(0xFFE2E8F0))
                )

                // Grupo 2: Ações de Histórico (Desfazer / Refazer)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onUndo,
                        enabled = state.canUndo,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ArrowCounterClockwise,
                            contentDescription = "Desfazer",
                            tint = if (state.canUndo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = onRedo,
                        enabled = state.canRedo,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ArrowClockwise,
                            contentDescription = "Refazer",
                            tint = if (state.canRedo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Painel Contextual 1: Caneta (Cores + Amostras Visuais de Espessura)
        AnimatedVisibility(
            visible = isPenActive && activeContextualPanel == ContextualPanelType.PEN,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Paleta de Cores da Caneta
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        penColors.forEach { c ->
                            val colorArgb = c.toArgb()
                            val isSelected = (state.currentTool as? InkingTool.Pen)?.colorInt == colorArgb
                            ColorPickerDot(
                                color = c,
                                isSelected = isSelected,
                                onClick = {
                                    lastPenColorInt = colorArgb
                                    val currentPen = state.currentTool as? InkingTool.Pen
                                    onToolSelect(currentPen?.copy(colorInt = colorArgb) ?: InkingTool.Pen(colorInt = colorArgb, size = lastPenSize))
                                }
                            )
                        }
                    }

                    // Amostras Visuais de Espessura da Caneta (Linha real com ponta arredondada)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentSize = (state.currentTool as? InkingTool.Pen)?.size ?: lastPenSize
                        PenStrokeSize.entries.forEach { sz ->
                            val isSelected = kotlin.math.abs(currentSize - sz.sizeWorld) < 0.5f
                            StrokeSizeChip(
                                label = sz.label,
                                strokeWidthPx = sz.sizeWorld * 1.4f,
                                strokeColor = Color(lastPenColorInt),
                                isSelected = isSelected,
                                onClick = {
                                    lastPenSize = sz.sizeWorld
                                    val currentPen = state.currentTool as? InkingTool.Pen
                                    onToolSelect(currentPen?.copy(size = sz.sizeWorld) ?: InkingTool.Pen(colorInt = lastPenColorInt, size = sz.sizeWorld))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Painel Contextual 2: Marca-texto (Cores + Amostras Visuais de Espessura)
        AnimatedVisibility(
            visible = isHighlighterActive && activeContextualPanel == ContextualPanelType.HIGHLIGHTER,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Paleta de Cores do Marca-texto
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        highlighterColors.forEach { c ->
                            val colorArgb = c.toArgb()
                            val isSelected = (state.currentTool as? InkingTool.Highlighter)?.colorInt == colorArgb
                            ColorPickerDot(
                                color = c,
                                isSelected = isSelected,
                                onClick = {
                                    lastHighlighterColorInt = colorArgb
                                    val currentHl = state.currentTool as? InkingTool.Highlighter
                                    onToolSelect(currentHl?.copy(colorInt = colorArgb) ?: InkingTool.Highlighter(colorInt = colorArgb, size = lastHighlighterSize))
                                }
                            )
                        }
                    }

                    // Amostras Visuais de Espessura do Marca-texto
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val currentSize = (state.currentTool as? InkingTool.Highlighter)?.size ?: lastHighlighterSize
                        HighlighterSize.entries.forEach { sz ->
                            val isSelected = kotlin.math.abs(currentSize - sz.sizeWorld) < 0.5f
                            StrokeSizeChip(
                                label = sz.label,
                                strokeWidthPx = sz.sizeWorld * 0.7f,
                                strokeColor = Color(lastHighlighterColorInt),
                                isSelected = isSelected,
                                onClick = {
                                    lastHighlighterSize = sz.sizeWorld
                                    val currentHl = state.currentTool as? InkingTool.Highlighter
                                    onToolSelect(currentHl?.copy(size = sz.sizeWorld) ?: InkingTool.Highlighter(colorInt = lastHighlighterColorInt, size = sz.sizeWorld))
                                }
                            )
                        }
                    }
                }
            }
        }

        // Painel Contextual 3: Borracha (Tamanho Pequeno / Médio / Grande com visual circular)
        AnimatedVisibility(
            visible = isEraserActive && activeContextualPanel == ContextualPanelType.ERASER,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EraserSize.entries.forEach { sizeOption ->
                        val isSelected = state.eraserSize == sizeOption
                        val previewRadius = when (sizeOption) {
                            EraserSize.SMALL -> 8.dp
                            EraserSize.MEDIUM -> 14.dp
                            EraserSize.LARGE -> 20.dp
                        }
                        Surface(
                            onClick = { onEraserSizeSelect(sizeOption) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF2563EB)) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(previewRadius)
                                        .background(
                                            if (isSelected) Color(0xFF1E3A8A) else Color(0xFF64748B),
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = sizeOption.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF334155)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Painel Contextual 4: Ações de Seleção do Laço
        AnimatedVisibility(
            visible = isLassoActive,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f))
            ) {
                if (state.selectedStrokeCount > 0) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${state.selectedStrokeCount} selecionado(s):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E3A8A)
                        )

                        // Duplicar
                        IconButton(
                            onClick = onDuplicateSelection,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Copy,
                                contentDescription = "Duplicar",
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Recolorir
                        penColors.forEach { c ->
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(c, CircleShape)
                                    .border(1.dp, Color(0xFFCBD5E1), CircleShape)
                                    .clickable { onRecolorSelection(c.toArgb()) }
                            )
                        }

                        // Excluir Seleção
                        IconButton(
                            onClick = onDeleteSelection,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Trash,
                                contentDescription = "Excluir traços selecionados",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Desmarcar
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.X,
                                contentDescription = "Desmarcar",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Lasso,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Circule os traços com a Stylus para selecionar",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Botão individual para uma ferramenta na toolbar flutuante com área de toque de 44x44dp.
 */
@Composable
private fun ToolbarToolButton(
    iconRegular: ImageVector,
    iconFill: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    badgeColor: Color? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isSelected) iconFill else iconRegular,
                contentDescription = contentDescription,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )

            // Indicador sutil de cor ativa (pontinho na base do ícone)
            if (badgeColor != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp)
                        .size(5.dp)
                        .background(badgeColor, CircleShape)
                        .border(0.5.dp, Color.White, CircleShape)
                )
            }
        }
    }
}

/**
 * Ponto seletor de cor circular.
 */
@Composable
private fun ColorPickerDot(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = PhosphorIcons.Regular.Check,
                contentDescription = null,
                tint = if (color.toArgb() == 0xFF000000.toInt()) Color.White else Color.Black,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Amostra visual de espessura de traço (com traço suave desenhado em tempo real).
 */
@Composable
private fun StrokeSizeChip(
    label: String,
    strokeWidthPx: Float,
    strokeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF2563EB)) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.height(34.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 24.dp, height = 14.dp)) {
                drawLine(
                    color = strokeColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = strokeWidthPx.coerceIn(1.5f, 12f),
                    cap = StrokeCap.Round
                )
            }
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF334155)
            )
        }
    }
}

/**
 * Diálogo focado exclusivamente nas opções da folha:
 * - Tipo de papel (Branca, Pautada, Quadriculada, Pontilhada, Matemática);
 * - Nível de suavização pós-traço (Off, Low, Medium).
 */
@Composable
private fun SheetOptionsDialog(
    state: InkingState,
    onPageBackgroundSelect: (PageBackground) -> Unit,
    onSmoothingSelect: (SmoothingLevel) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Cabeçalho do Diálogo
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
                            imageVector = PhosphorIcons.Regular.SlidersHorizontal,
                            contentDescription = null,
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Opções da Folha",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.X,
                            contentDescription = "Fechar",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Seção 1: Tipo de Folha (Miniaturas Visuais sem truncamento de texto)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "TIPO DE PAPEL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PageBackground.entries.forEach { bg ->
                            PageBackgroundThumbnail(
                                bg = bg,
                                isSelected = state.pageBackground == bg,
                                onClick = { onPageBackgroundSelect(bg) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Seção 2: Suavização do Traço
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SUAVIZAÇÃO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmoothingLevel.entries.forEach { level ->
                            val isSelected = state.smoothingLevel == level
                            Surface(
                                onClick = { onSmoothingSelect(level) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                                border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF2563EB)) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = level.label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF334155)
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

/**
 * Diálogo de ações da página (Menu "⋯" / Mais Opções):
 * - GERENCIAMENTO: Nova página, Duplicar página, Renomear página, Favoritar;
 * - CONFIGURAÇÕES: Opções da folha;
 * - EXPORTAÇÃO: Exportar como PDF, Exportar como PNG;
 * - AVANÇADO (exibido apenas em build de desenvolvimento): Diagnósticos & Depuração;
 * - AÇÃO DESTRUTIVA: Limpar página (com confirmação obrigatória).
 */
@Composable
private fun PageActionsDialog(
    currentPage: PageEntity?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRenamePage: () -> Unit,
    onDuplicatePage: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSheetOptions: () -> Unit,
    onExportPdf: () -> Unit,
    onExportPng: () -> Unit,
    onDeletePage: () -> Unit,
    onPromptClear: () -> Unit,
    isDiagnosticsVisible: Boolean,
    onToggleDiagnostics: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Cabeçalho
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Ações da Página",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = currentPage?.title ?: "Página Atual",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.X,
                            contentDescription = "Fechar",
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // 1. GERENCIAMENTO DA PÁGINA
                Text(
                    text = "PÁGINA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.PencilSimple,
                    title = "Renomear página",
                    onClick = onRenamePage
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Copy,
                    title = "Duplicar página",
                    subtitle = "Criar cópia idêntica da página",
                    onClick = onDuplicatePage
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.CaretUp,
                    title = "Mover para cima",
                    enabled = canMoveUp,
                    onClick = onMoveUp
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.CaretDown,
                    title = "Mover para baixo",
                    enabled = canMoveDown,
                    onClick = onMoveDown
                )
                PageActionRow(
                    icon = if (currentPage?.isFavorite == true) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                    iconTint = if (currentPage?.isFavorite == true) Color(0xFFD97706) else null,
                    title = if (currentPage?.isFavorite == true) "Remover dos favoritos" else "Adicionar aos favoritos",
                    onClick = onToggleFavorite
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // 2. CONFIGURAÇÕES DA FOLHA
                Text(
                    text = "FOLHA",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.SlidersHorizontal,
                    title = "Opções da folha",
                    subtitle = "Tipo de papel e suavização",
                    onClick = onOpenSheetOptions
                )

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // 3. EXPORTAÇÃO
                Text(
                    text = "EXPORTAÇÃO",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.FilePdf,
                    title = "Exportar PDF",
                    subtitle = "Documento em alta resolução",
                    onClick = onExportPdf
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Image,
                    title = "Exportar Imagem (PNG)",
                    subtitle = "Imagem nítida da folha",
                    onClick = onExportPng
                )

                // 4. AVANÇADO (Exibido apenas em Debug Builds)
                if (com.example.lenote.BuildConfig.DEBUG) {
                    HorizontalDivider(color = Color(0xFFE2E8F0))
                    Text(
                        text = "AVANÇADO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                    PageActionRow(
                        icon = PhosphorIcons.Regular.Bug,
                        title = "Diagnósticos & Depuração",
                        subtitle = if (isDiagnosticsVisible) "Ativo na tela" else "Inativo",
                        onClick = onToggleDiagnostics
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // 5. AÇÕES DESTRUTIVAS
                Text(
                    text = "AÇÕES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Eraser,
                    title = "Limpar página",
                    subtitle = "Apaga todos os traços desta página",
                    isDestructive = true,
                    onClick = onPromptClear
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Trash,
                    title = "Excluir página",
                    subtitle = "Exclui permanentemente esta página",
                    isDestructive = true,
                    onClick = onDeletePage
                )
            }
        }
    }
}

@Composable
private fun PageActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color? = null,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !enabled -> Color(0xFFCBD5E1)
                    isDestructive -> MaterialTheme.colorScheme.error
                    iconTint != null -> iconTint
                    else -> Color(0xFF334155)
                },
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        !enabled -> Color(0xFF94A3B8)
                        isDestructive -> MaterialTheme.colorScheme.error
                        else -> Color(0xFF0F172A)
                    }
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = if (!enabled) Color(0xFFCBD5E1) else Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

/**
 * Indicador discreto de zoom no canto inferior com popover expansível:
 * - Em repouso: cápsula pequena com porcentagem atual (ex.: 100%);
 * - Ao tocar: expande com botões [ − ] 100% [ + ] e [ Ajustar à página ].
 */
@Composable
private fun ZoomIndicatorControl(
    zoomLevel: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onFitToPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 3.dp
    ) {
        if (!isExpanded) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { isExpanded = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${kotlin.math.round(zoomLevel * 100).toInt()}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Minus,
                        contentDescription = "Diminuir zoom",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable {
                            onResetZoom()
                            isExpanded = false
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${kotlin.math.round(zoomLevel * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E3A8A)
                    )
                }

                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Plus,
                        contentDescription = "Aumentar zoom",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(Color(0xFFE2E8F0))
                )

                IconButton(
                    onClick = {
                        onFitToPage()
                        isExpanded = false
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.CornersOut,
                        contentDescription = "Ajustar à página",
                        tint = Color(0xFF334155),
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = { isExpanded = false },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = "Fechar controle de zoom",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Miniatura do padrão de folha (Branca, Pautada, Quadriculada, Pontilhada, Matemática).
 * Espaçamento e tipografia calibrados para eliminar qualquer truncamento de texto.
 */
@Composable
private fun PageBackgroundThumbnail(
    bg: PageBackground,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        border = if (isSelected) BorderStroke(1.5.dp, Color(0xFF2563EB)) else BorderStroke(1.dp, Color(0xFFE2E8F0)),
        color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color(0xFFFCFCFA),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                TemplatePreviewCanvas(background = bg, isDark = false)
            }
            Text(
                text = bg.label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF334155),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * Card flutuante e discreto de confirmação pós-exportação (PDF ou PNG).
 * Exibe status amigável, caminho onde o arquivo foi salvo no dispositivo
 * e botão de ação para abrir diretamente no visualizador do Android via URI segura.
 * Usa exclusivamente Phosphor Icons (FilePdf, Image, ArrowSquareOut, CheckCircle, WarningCircle).
 */
@Composable
private fun ExportConfirmationBanner(
    state: ExportConfirmationState,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .widthIn(min = 340.dp, max = 560.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFFFFF),
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, if (state.isError) Color(0xFFFCA5A5) else Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Ícone contextual Phosphor
            val iconVector = when {
                state.isError -> PhosphorIcons.Regular.WarningCircle
                state.exportResult.mimeType == "application/pdf" -> PhosphorIcons.Regular.FilePdf
                else -> PhosphorIcons.Regular.Image
            }
            val iconTint = when {
                state.isError -> Color(0xFFDC2626) // Vermelho de erro
                state.exportResult.mimeType == "application/pdf" -> Color(0xFFDC2626) // PDF vermelho característico
                else -> Color(0xFF2563EB) // Azul real para PNG
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.isError) Color(0xFFFEE2E2) else Color(0xFFF1F5F9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Textos Informativos: Título + Localização
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = state.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                if (state.subtitle.isNotBlank()) {
                    Text(
                        text = state.subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF64748B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Ação de Abertura ("Abrir PDF" ou "Ver imagem")
            if (state.canOpen) {
                val actionLabel = if (state.exportResult.mimeType == "application/pdf") "Abrir PDF" else "Ver imagem"
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1E3A8A),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.ArrowSquareOut,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = actionLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Botão Fechar discreto
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.X,
                    contentDescription = "Fechar",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

package com.example.lenote.ui

import android.content.ActivityNotFoundException
import android.widget.FrameLayout
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.example.lenote.export.ExportResult
import com.example.lenote.export.ExportViewerHelper
import com.example.lenote.export.PageExporter
import com.example.lenote.ink.EraserSize
import com.example.lenote.ink.FinishedStrokesView
import com.example.lenote.ink.HighlighterSize
import com.example.lenote.ink.InkingState
import com.example.lenote.ink.InkingTool
import com.example.lenote.ink.JetpackInkEngine
import com.example.lenote.ink.PageBackground
import com.example.lenote.ink.PenStrokeSize
import com.example.lenote.ink.SmoothingLevel
import com.example.lenote.ui.theme.LeNotePalette
import com.example.lenote.ui.theme.LeNoteTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

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
 * Tela principal de anotações (Studio Canvas) do LeNote:
 * - Canvas nativo ocupando a área dominante;
 * - Header Editorial compacto (50dp) em papel marfim com borda hairline inferior e indicador de salvamento;
 * - Dock Flutuante de Ferramentas responsivo (com scroll horizontal suave em telas estreitas para nunca cortar Undo/Redo);
 * - Painéis contextuais com amostras visuais reais de espessura de traço e cores editoriais;
 * - Controles inferiores respeitando `navigationBarsPadding()` (zero sobreposição com a barra do sistema).
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
    val themeMode by (viewModel?.themeMode ?: kotlinx.coroutines.flow.MutableStateFlow(com.example.lenote.data.ThemeMode.LIGHT)).collectAsState()
    val appLanguage by (viewModel?.appLanguage ?: kotlinx.coroutines.flow.MutableStateFlow(com.example.lenote.data.AppLanguage.SYSTEM)).collectAsState()
    val isDarkMode = themeMode == com.example.lenote.data.ThemeMode.DARK
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

    LeNoteTheme(darkTheme = isDarkMode, appLanguage = appLanguage) {
        val s = LeNoteTheme.strings
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = LeNoteTheme.colors.paperBackground
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

                // B) HEADER EDITORIAL COMPACTO (50dp)
                CompactHeaderBar(
                    currentPage = currentPage,
                    pageIndexLabel = if (currentPageIndex >= 0 && pages.isNotEmpty()) "${currentPageIndex + 1}/${pages.size}" else null,
                    isSaving = isSaving,
                    saveStatus = saveStatusMessage,
                    onNavigateBack = {
                        viewModel?.flushCurrentPageSave()
                        onNavigateBack?.invoke()
                    },
                    onToggleSidebar = { viewModel?.toggleSidebar() },
                    onRenamePageClick = {
                        renamePageInput = currentPage?.title ?: s.defaultPageTitle(1)
                        showRenamePageDialog = true
                    },
                    onToggleFavorite = { viewModel?.toggleCurrentPageFavorite() },
                    onOpenSheetOptions = { showTemplatePickerDialog = true },
                    onOpenMoreOptions = { showPageActionsModal = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                )

                // C) DOCK FLUTUANTE DE FERRAMENTAS & PAINÉIS CONTEXTUAIS
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
                            localSaveStatusMessage = "${s.statusSaved}: ${bytes.size} bytes ($strokeCount)"
                        }
                    },
                    onLoad = {
                        if (viewModel != null) {
                            viewModel.loadSavedStrokes()
                        } else {
                            savedStrokesData?.let { bytes ->
                                engine.loadStrokes(bytes)
                                localSaveStatusMessage = s.statusSaved
                            }
                        }
                    },
                    canLoad = savedStrokesData != null,
                    onPromptClear = { showClearConfirmDialog = true },
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 56.dp, start = 12.dp, end = 12.dp)
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
                            .navigationBarsPadding()
                            .padding(16.dp)
                    )
                }

                // E) PAINEL LATERAL RECOLHÍVEL COM SCRIM EDITORIAL (Cadernos -> Seções -> Páginas)
                if (viewModel != null) {
                    AnimatedVisibility(
                        visible = isSidebarOpen,
                        enter = fadeIn(tween(200)),
                        exit = fadeOut(tween(180)),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.28f))
                                .clickable(
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null,
                                    onClick = { viewModel.setSidebarOpen(false) }
                                )
                        )
                    }
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
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp)
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
                                        val isPdf = confState.exportResult.mimeType == "application/pdf"
                                        val appType = if (isPdf) "PDF" else "PNG"
                                        if (ex is ActivityNotFoundException) {
                                            exportConfirmation = confState.copy(
                                                title = s.exportNoAppFoundError(appType),
                                                subtitle = confState.exportResult.displayPath,
                                                isError = true,
                                                canOpen = false
                                            )
                                        } else {
                                            exportConfirmation = confState.copy(
                                                title = s.exportOpenFailedError(appType),
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

                // G) INDICADOR E CONTROLE DISCRETO DE ZOOM (Canto inferior direito, acima da barra de navegação)
                ZoomIndicatorControl(
                    zoomLevel = engineState.zoomLevel,
                    onZoomIn = { engine.zoomIn() },
                    onZoomOut = { engine.zoomOut() },
                    onResetZoom = { engine.resetZoom() },
                    onFitToPage = { engine.fitToPage() },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = 16.dp, bottom = 14.dp)
                )
            }
        }

        // Diálogo de Renomear Página
        if (showRenamePageDialog) {
            AlertDialog(
                onDismissRequest = { showRenamePageDialog = false },
                containerColor = LeNoteTheme.colors.elevatedSurface,
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = s.currentSheetOverline,
                            style = MaterialTheme.typography.labelMedium,
                            color = LeNoteTheme.colors.inkMuted
                        )
                        Text(
                            text = s.renamePageTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = LeNoteTheme.colors.inkPrimary
                        )
                    }
                },
                text = {
                    OutlinedTextField(
                        value = renamePageInput,
                        onValueChange = { renamePageInput = it },
                        singleLine = true,
                        placeholder = { Text(s.pageTitlePlaceholder) },
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
                            if (viewModel != null) {
                                viewModel.renameCurrentPage(renamePageInput)
                            }
                            showRenamePageDialog = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LeNoteTheme.colors.accentPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(s.actionSave)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRenamePageDialog = false }) {
                        Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
                    }
                }
            )
        }

        // Confirmação para Ação Destrutiva "Limpar Página"
        if (showClearConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmDialog = false },
                containerColor = LeNoteTheme.colors.elevatedSurface,
                icon = {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Eraser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(26.dp)
                    )
                },
                title = {
                    Text(
                        text = s.clearStrokesDialogTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = LeNoteTheme.colors.inkPrimary
                    )
                },
                text = {
                    Text(
                        text = s.clearStrokesDialogMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LeNoteTheme.colors.inkSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            engine.clear()
                            showClearConfirmDialog = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(s.actionClearSheet)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirmDialog = false }) {
                        Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
                    }
                }
            )
        }

        // Confirmação para Ação Destrutiva "Excluir Página"
        if (showDeletePageConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeletePageConfirmDialog = false },
                containerColor = LeNoteTheme.colors.elevatedSurface,
                icon = {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Trash,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(26.dp)
                    )
                },
                title = {
                    Text(
                        text = s.movePageToTrashDialogTitle,
                        style = MaterialTheme.typography.titleLarge,
                        color = LeNoteTheme.colors.inkPrimary
                    )
                },
                text = {
                    Text(
                        text = s.movePageToTrashDialogMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = LeNoteTheme.colors.inkSecondary
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
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(s.menuMoveToTrash)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeletePageConfirmDialog = false }) {
                        Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
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
                    renamePageInput = currentPage?.title ?: s.defaultPageTitle(1)
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
                                    title = s.exportPdfSuccessTitle,
                                    subtitle = res.displayPath,
                                    isError = false,
                                    canOpen = true
                                )
                            } else {
                                ExportConfirmationState(
                                    exportResult = res,
                                    title = s.exportPdfErrorTitle,
                                    subtitle = res.errorMessage ?: "",
                                    isError = true,
                                    canOpen = false
                                )
                            }
                        }
                    } else {
                        val pageTitle = currentPage?.title ?: s.defaultPageTitle(1)
                        val bg = engine.state.value.pageBackground
                        val strokes = engine.getFinishedStrokesSnapshot()
                        try {
                            val res = PageExporter.exportPageToPdf(context, pageTitle, bg, strokes)
                            exportConfirmation = ExportConfirmationState(
                                exportResult = res,
                                title = s.exportPdfSuccessTitle,
                                subtitle = res.displayPath,
                                isError = false,
                                canOpen = true
                            )
                        } catch (e: Exception) {
                            val dummy = File(context.cacheDir, "err.pdf")
                            exportConfirmation = ExportConfirmationState(
                                exportResult = ExportResult(dummy, null, "", isSuccess = false),
                                title = s.exportPdfErrorTitle,
                                subtitle = e.message ?: "",
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
                                    title = s.exportPngSuccessTitle,
                                    subtitle = res.displayPath,
                                    isError = false,
                                    canOpen = true
                                )
                            } else {
                                ExportConfirmationState(
                                    exportResult = res,
                                    title = s.exportPngErrorTitle,
                                    subtitle = res.errorMessage ?: "",
                                    isError = true,
                                    canOpen = false
                                )
                            }
                        }
                    } else {
                        val pageTitle = currentPage?.title ?: s.defaultPageTitle(1)
                        val bg = engine.state.value.pageBackground
                        val strokes = engine.getFinishedStrokesSnapshot()
                        try {
                            val res = PageExporter.exportPageToPng(context, pageTitle, bg, strokes)
                            exportConfirmation = ExportConfirmationState(
                                exportResult = res,
                                title = s.exportPngSuccessTitle,
                                subtitle = res.displayPath,
                                isError = false,
                                canOpen = true
                            )
                        } catch (e: Exception) {
                            val dummy = File(context.cacheDir, "err.png")
                            exportConfirmation = ExportConfirmationState(
                                exportResult = ExportResult(dummy, null, "", isSuccess = false),
                                title = s.exportPngErrorTitle,
                                subtitle = e.message ?: "",
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

        // Diálogo de Seleção de Modelo e Cor de Folha (Opções da Folha)
        if (showTemplatePickerDialog) {
            val cur = currentPage
            PageTemplatePickerDialog(
                initialTitle = cur?.title ?: s.defaultPageTitle(1),
                initialBackground = cur?.pageBackground ?: engineState.pageBackground,
                initialPaperColor = cur?.paperColor ?: 0xFFFFFFFF,
                isCreateMode = false,
                onConfirm = { _, bg, paperColor ->
                    showTemplatePickerDialog = false
                    if (viewModel != null) {
                        viewModel.changePageTemplate(bg, paperColor)
                    } else {
                        engine.setPageBackground(bg, paperColor)
                    }
                },
                onDismiss = { showTemplatePickerDialog = false }
            )
        }
    }
}

/**
 * Cabeçalho Editorial Compacto (altura 50dp):
 * Esquerda: Voltar para Biblioteca, Gaveta de Cadernos/Páginas, Título editável da folha, Estrela de favorito.
 * Direita: Botão rápido de modelo de folha, Indicador discreto de autosave e Menu "Mais" (⋮).
 */
@Composable
private fun CompactHeaderBar(
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
                old.isStylusButtonEraserActive == new.isStylusButtonEraserActive &&
                old.isTouchInkingEnabled == new.isTouchInkingEnabled
        }
    }
    val scope = rememberCoroutineScope()
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
        onBeautifySelection = {
            scope.launch {
                viewModel?.beautifySelectedStrokesWithFont()
            }
        },
        onAlignBaseline = {
            scope.launch {
                viewModel?.alignSelectedStrokesToBaseline()
            }
        },
        onSave = { onSave(state.strokeCount) },
        onLoad = onLoad,
        canLoad = canLoad,
        onToggleTouchInking = {
            if (viewModel != null) {
                viewModel.toggleTouchInking()
            } else {
                engine.setTouchInkingEnabled(!state.isTouchInkingEnabled)
            }
        },
        modifier = modifier
    )
}

/**
 * Área de visualização nativa de traços (Jetpack Ink).
 * Mantém as tags "FinishedStrokesView" e "InProgressStrokesView" para compatibilidade com testes instrumentados.
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
    Surface(
        modifier = modifier.widthIn(max = 400.dp),
        color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
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
                        tint = LeNoteTheme.colors.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Telemetria Jetpack Ink",
                        style = MaterialTheme.typography.titleSmall,
                        color = LeNoteTheme.colors.inkPrimary
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = "Fechar",
                        tint = LeNoteTheme.colors.inkSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Gesto: ${state.gestureState.name} | Stylus Eraser: ${if (state.isStylusButtonEraserActive) "ATIVO" else "OFF"}",
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkSecondary
            )
            Text(
                text = "ACTION: ${state.lastActionName} | ptrs: ${state.activePointerCount} | CANCELED: ${state.isFlagCanceled}",
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkSecondary
            )
            Text(
                text = "Decisão: ${state.lastDecisionSummary}",
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkPrimary
            )

            if (state.pointerDiagnostics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                state.pointerDiagnostics.forEach { ptr ->
                    Text(
                        text = String.format(
                            Locale.US,
                            "• [%d|%d] %s | P=%.2f | (%.0f, %.0f) -> %s",
                            ptr.pointerIndex,
                            ptr.pointerId,
                            ptr.toolTypeName,
                            ptr.pressure,
                            ptr.x,
                            ptr.y,
                            ptr.decision
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = LeNoteTheme.colors.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(
                    Locale.US,
                    "Traços: %d (Sel: %d) | Folha: %s | Zoom: %.2fx",
                    state.strokeCount,
                    state.selectedStrokeCount,
                    state.pageBackground.label,
                    state.zoomLevel
                ),
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkSecondary
            )

            if (!saveStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Status: $saveStatus",
                    style = MaterialTheme.typography.labelSmall,
                    color = LeNoteTheme.colors.success
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
 * Dock Flutuante Editorial de Ferramentas (ToolbarOverlay):
 * - Superfície em papel marfim puro (`elevatedSurface`) com borda hairline 1.dp (`hairlineBorder`),
 *   eliminando qualquer tom roxo/lavanda padrão do Material 3;
 * - Responsivo: utiliza `horizontalScroll` para que em smartphones compactos todos os 9 controles
 *   (incluindo Desfazer e Refazer) fiquem 100% visíveis e acessíveis sem corte nas bordas;
 * - Botões com microinteração suave de cor e indicador de cor da tinta.
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
    onBeautifySelection: () -> Unit = {},
    onAlignBaseline: () -> Unit = {},
    onSave: () -> Unit,
    onLoad: () -> Unit,
    canLoad: Boolean,
    isTextActive: Boolean = false,
    onToggleText: () -> Unit = {},
    onInsertImage: () -> Unit = {},
    onToggleTouchInking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val s = LeNoteTheme.strings
    val penColors = listOf(
        Color(0xFF141413), // Tinta Obsidiana
        Color(0xFFFFFFFF), // Tinta Branca (Giz / Folha Escura)
        Color(0xFF1B365D), // Azul Atelier
        Color(0xFFB8332A), // Carmim Editorial
        Color(0xFF1F5E3B), // Verde Botânico
        Color(0xFF5B3278), // Ameixa
        Color(0xFFC86D24)  // Ocre Queimado
    )

    val highlighterColors = listOf(
        Color(0x66FDD835), // Amarelo Solar
        Color(0x664CAF50), // Verde Menta
        Color(0x6629B6F6), // Azul Céu
        Color(0x66EC407A), // Rosa Coral
        Color(0x66FFA726), // Âmbar
        Color(0x66AB47BC)  // Lavanda
    )

    var lastPenColorInt by remember {
        val initialColor = (state.currentTool as? InkingTool.Pen)?.colorInt ?: 0xFF141413.toInt()
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

    val isPenActive = state.currentTool is InkingTool.Pen && !state.isStylusButtonEraserActive && !isTextActive
    val isHighlighterActive = state.currentTool is InkingTool.Highlighter && !state.isStylusButtonEraserActive && !isTextActive
    val isEraserActive = (state.currentTool is InkingTool.Eraser || state.isStylusButtonEraserActive) && !isTextActive
    val isLassoActive = state.currentTool is InkingTool.Lasso && !state.isStylusButtonEraserActive && !isTextActive

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dock Principal da Toolbar Flutuante (Estilo Atelier Floating Pill)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.97f),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Caneta
                ToolbarToolButton(
                    iconRegular = PhosphorIcons.Regular.Pen,
                    iconFill = PhosphorIcons.Fill.Pen,
                    contentDescription = s.toolPen,
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
                    contentDescription = s.toolHighlighter,
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
                    contentDescription = s.toolEraser,
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
                    contentDescription = s.toolLasso,
                    isSelected = isLassoActive,
                    onClick = {
                        onToolSelect(InkingTool.Lasso)
                        activeContextualPanel = ContextualPanelType.NONE
                    }
                )

                // Divisor Vertical Sutil 1
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(1.dp)
                        .height(18.dp)
                        .background(LeNoteTheme.colors.hairlineBorder)
                )

                // Caixa de Texto
                ToolbarToolButton(
                    iconRegular = PhosphorIcons.Regular.TextT,
                    iconFill = PhosphorIcons.Regular.TextT,
                    contentDescription = s.toolTextBox,
                    isSelected = isTextActive,
                    onClick = {
                        activeContextualPanel = ContextualPanelType.NONE
                        onToggleText()
                    }
                )

                // Inserir Imagem
                ToolbarToolButton(
                    iconRegular = PhosphorIcons.Regular.Image,
                    iconFill = PhosphorIcons.Fill.Image,
                    contentDescription = s.toolInsertImage,
                    isSelected = false,
                    onClick = {
                        activeContextualPanel = ContextualPanelType.NONE
                        onInsertImage()
                    }
                )

                // Desenhar com o Dedo (Touch Inking Toggle)
                ToolbarToolButton(
                    iconRegular = PhosphorIcons.Regular.HandPointing,
                    iconFill = PhosphorIcons.Fill.HandPointing,
                    contentDescription = s.toolTouchInking,
                    isSelected = state.isTouchInkingEnabled,
                    onClick = onToggleTouchInking
                )

                // Divisor Vertical Sutil 2
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .width(1.dp)
                        .height(18.dp)
                        .background(LeNoteTheme.colors.hairlineBorder)
                )

                // Desfazer
                IconButton(
                    onClick = onUndo,
                    enabled = state.canUndo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.ArrowCounterClockwise,
                        contentDescription = s.toolUndo,
                        tint = if (state.canUndo) LeNoteTheme.colors.inkPrimary else LeNoteTheme.colors.inkMuted.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Refazer
                IconButton(
                    onClick = onRedo,
                    enabled = state.canRedo,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.ArrowClockwise,
                        contentDescription = s.toolRedo,
                        tint = if (state.canRedo) LeNoteTheme.colors.inkPrimary else LeNoteTheme.colors.inkMuted.copy(alpha = 0.35f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Painel Contextual 1: Caneta (Cores + Amostras Visuais de Espessura)
        AnimatedVisibility(
            visible = isPenActive && activeContextualPanel == ContextualPanelType.PEN,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder),
                color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.98f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Paleta de Cores da Caneta
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                    // Amostras Visuais de Espessura da Caneta
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val currentSize = (state.currentTool as? InkingTool.Pen)?.size ?: lastPenSize
                        PenStrokeSize.entries.forEach { sz ->
                            val isSelected = kotlin.math.abs(currentSize - sz.sizeWorld) < 0.5f
                            StrokeSizeChip(
                                label = s.penStrokeSizeLabel(sz),
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
            Surface(
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder),
                color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.98f)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val currentSize = (state.currentTool as? InkingTool.Highlighter)?.size ?: lastHighlighterSize
                        HighlighterSize.entries.forEach { sz ->
                            val isSelected = kotlin.math.abs(currentSize - sz.sizeWorld) < 0.5f
                            StrokeSizeChip(
                                label = s.highlighterSizeLabel(sz),
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

        // Painel Contextual 3: Borracha
        AnimatedVisibility(
            visible = isEraserActive && activeContextualPanel == ContextualPanelType.ERASER,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder),
                color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.98f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EraserSize.entries.forEach { sizeOption ->
                        val isSelected = state.eraserSize == sizeOption
                        val previewRadius = when (sizeOption) {
                            EraserSize.SMALL -> 7.dp
                            EraserSize.MEDIUM -> 12.dp
                            EraserSize.LARGE -> 17.dp
                        }
                        Surface(
                            onClick = { onEraserSizeSelect(sizeOption) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) LeNoteTheme.colors.accentPrimaryContainer else LeNoteTheme.colors.tonalSurface,
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.hairlineBorder
                            ),
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
                                            if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkSecondary,
                                            CircleShape
                                        )
                                )
                                Text(
                                    text = s.eraserSizeLabel(sizeOption),
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) LeNoteTheme.colors.inkPrimary else LeNoteTheme.colors.inkSecondary
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
            Surface(
                shape = RoundedCornerShape(14.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder),
                color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.98f)
            ) {
                if (state.selectedStrokeCount > 0) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = s.lassoSelectedCount(state.selectedStrokeCount),
                            style = MaterialTheme.typography.labelSmall,
                            color = LeNoteTheme.colors.accentPrimary
                        )

                        // Embelezar Caligrafia
                        IconButton(
                            onClick = onBeautifySelection,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Sparkle,
                                contentDescription = s.lassoBeautifyDesc,
                                tint = LeNoteTheme.colors.accentWarm,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Alinhar Linha de Base
                        IconButton(
                            onClick = onAlignBaseline,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.ListDashes,
                                contentDescription = s.lassoAlignBaselineDesc,
                                tint = LeNoteTheme.colors.accentPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Duplicar
                        IconButton(
                            onClick = onDuplicateSelection,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Copy,
                                contentDescription = s.lassoDuplicateDesc,
                                tint = LeNoteTheme.colors.inkPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Recolorir
                        penColors.forEach { c ->
                            val isLightRecolor = (c.red * 0.299f + c.green * 0.587f + c.blue * 0.114f) > 0.78f
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(c, CircleShape)
                                    .border(
                                        1.dp,
                                        if (isLightRecolor) LeNoteTheme.colors.inkSecondary.copy(alpha = 0.6f) else LeNoteTheme.colors.hairlineBorder,
                                        CircleShape
                                    )
                                    .clickable { onRecolorSelection(c.toArgb()) }
                            )
                        }

                        // Excluir Seleção
                        IconButton(
                            onClick = onDeleteSelection,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.Trash,
                                contentDescription = s.lassoDeleteSelectionDesc,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Desmarcar
                        IconButton(
                            onClick = onClearSelection,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.X,
                                contentDescription = s.lassoClearSelectionDesc,
                                tint = LeNoteTheme.colors.inkSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Lasso,
                            contentDescription = null,
                            tint = LeNoteTheme.colors.accentPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = s.lassoHint,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = LeNoteTheme.colors.inkSecondary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Botão individual para uma ferramenta na toolbar flutuante com transição suave de cor.
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
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) LeNoteTheme.colors.accentPrimaryContainer else Color.Transparent,
        animationSpec = tween(160),
        label = "ToolBtnBg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkSecondary,
        animationSpec = tween(160),
        label = "ToolBtnIcon"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = if (isSelected) BorderStroke(1.dp, LeNoteTheme.colors.accentPrimary.copy(alpha = 0.25f)) else null,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isSelected) iconFill else iconRegular,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )

            // Indicador sutil de cor ativa na base do botão
            if (badgeColor != null) {
                val isLightBadge = (badgeColor.red * 0.299f + badgeColor.green * 0.587f + badgeColor.blue * 0.114f) > 0.78f
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .size(5.dp)
                        .background(badgeColor, CircleShape)
                        .border(
                            width = 0.75.dp,
                            color = if (isLightBadge) LeNoteTheme.colors.inkSecondary else Color.White,
                            shape = CircleShape
                        )
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
    val isLightColor = (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f) > 0.78f
    val defaultBorderColor = if (isLightColor) {
        LeNoteTheme.colors.inkSecondary.copy(alpha = 0.55f)
    } else {
        LeNoteTheme.colors.hairlineBorder
    }
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) LeNoteTheme.colors.accentPrimary else defaultBorderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = PhosphorIcons.Regular.Check,
                contentDescription = null,
                tint = if (isLightColor) Color(0xFF141413) else Color.White,
                modifier = Modifier.size(13.dp)
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
    val isLightStroke = (strokeColor.red * 0.299f + strokeColor.green * 0.587f + strokeColor.blue * 0.114f) > 0.78f
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) LeNoteTheme.colors.accentPrimaryContainer else LeNoteTheme.colors.tonalSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.hairlineBorder
        ),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 20.dp, height = 12.dp)) {
                val clampedWidth = strokeWidthPx.coerceIn(1.5f, 11f)
                if (isLightStroke) {
                    drawLine(
                        color = Color(0xFF141413).copy(alpha = 0.45f),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = clampedWidth + 2.2f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(
                    color = strokeColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = clampedWidth,
                    cap = StrokeCap.Round
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) LeNoteTheme.colors.inkPrimary else LeNoteTheme.colors.inkSecondary
            )
        }
    }
}

/**
 * Diálogo focado nas opções rápidas da folha.
 */
@Composable
private fun SheetOptionsDialog(
    state: InkingState,
    onPageBackgroundSelect: (PageBackground) -> Unit,
    onSmoothingSelect: (SmoothingLevel) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LeNoteTheme.strings
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = LeNoteTheme.colors.elevatedSurface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                            tint = LeNoteTheme.colors.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = s.sheetOptionsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = LeNoteTheme.colors.inkPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.X,
                            contentDescription = s.actionClose,
                            tint = LeNoteTheme.colors.inkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = s.strokeSmoothingLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = LeNoteTheme.colors.inkMuted
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
                                color = if (isSelected) LeNoteTheme.colors.accentPrimaryContainer else LeNoteTheme.colors.tonalSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.hairlineBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = level.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) LeNoteTheme.colors.inkPrimary else LeNoteTheme.colors.inkSecondary
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
 * Diálogo Editorial de Ações da Página (Menu "⋮" do Header):
 * - GERENCIAMENTO: Renomear, Duplicar, Reordenar, Favoritar;
 * - CONFIGURAÇÕES: Modelo e Cor da Folha;
 * - EXPORTAÇÃO: Exportar PDF, Exportar PNG;
 * - AVANÇADO: Diagnósticos & Depuração;
 * - AÇÕES DESTRUTIVAS: Limpar traços, Mover para lixeira.
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
    val s = LeNoteTheme.strings
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = LeNoteTheme.colors.elevatedSurface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = s.pageOptionsOverline,
                            style = MaterialTheme.typography.labelMedium,
                            color = LeNoteTheme.colors.inkMuted
                        )
                        Text(
                            text = currentPage?.title ?: s.currentSheetFallback,
                            style = MaterialTheme.typography.headlineSmall,
                            color = LeNoteTheme.colors.inkPrimary,
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
                            contentDescription = s.actionClose,
                            tint = LeNoteTheme.colors.inkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                // 1. GERENCIAMENTO DA PÁGINA
                Text(
                    text = s.sectionOrganization,
                    style = MaterialTheme.typography.labelMedium,
                    color = LeNoteTheme.colors.inkMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.PencilSimple,
                    title = s.actionRenameSheet,
                    onClick = onRenamePage
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Copy,
                    title = s.actionDuplicateSheet,
                    subtitle = s.actionDuplicateSheetSubtitle,
                    onClick = onDuplicatePage
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.CaretUp,
                    title = s.actionMoveUp,
                    enabled = canMoveUp,
                    onClick = onMoveUp
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.CaretDown,
                    title = s.actionMoveDown,
                    enabled = canMoveDown,
                    onClick = onMoveDown
                )
                PageActionRow(
                    icon = if (currentPage?.isFavorite == true) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                    iconTint = if (currentPage?.isFavorite == true) LeNoteTheme.colors.goldStar else null,
                    title = if (currentPage?.isFavorite == true) s.actionRemoveFavorite else s.actionAddFavorite,
                    onClick = onToggleFavorite
                )

                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                // 2. CONFIGURAÇÕES DA FOLHA
                Text(
                    text = s.sectionPaperAndRuling,
                    style = MaterialTheme.typography.labelMedium,
                    color = LeNoteTheme.colors.inkMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.SlidersHorizontal,
                    title = s.actionSheetTemplateTitle,
                    subtitle = s.actionSheetTemplateSubtitle,
                    onClick = onOpenSheetOptions
                )

                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                // 3. EXPORTAÇÃO
                Text(
                    text = s.sectionExportAndShare,
                    style = MaterialTheme.typography.labelMedium,
                    color = LeNoteTheme.colors.inkMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.FilePdf,
                    title = s.actionExportPdfTitle,
                    subtitle = s.actionExportPdfSubtitle,
                    onClick = onExportPdf
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Image,
                    title = s.actionExportPngTitle,
                    subtitle = s.actionExportPngSubtitle,
                    onClick = onExportPng
                )

                if (com.example.lenote.BuildConfig.DEBUG) {
                    HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)
                    Text(
                        text = s.sectionEngineering,
                        style = MaterialTheme.typography.labelMedium,
                        color = LeNoteTheme.colors.inkMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    PageActionRow(
                        icon = PhosphorIcons.Regular.Bug,
                        title = s.actionTelemetryTitle,
                        subtitle = if (isDiagnosticsVisible) s.telemetryVisibleSubtitle else s.telemetryHiddenSubtitle,
                        onClick = onToggleDiagnostics
                    )
                }

                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                // 5. AÇÕES DESTRUTIVAS
                Text(
                    text = s.sectionDangerZone,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Eraser,
                    title = s.actionClearAllStrokesTitle,
                    subtitle = s.actionClearAllStrokesSubtitle,
                    isDestructive = true,
                    onClick = onPromptClear
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Trash,
                    title = s.actionMoveSheetToTrashTitle,
                    subtitle = s.actionMoveSheetToTrashSubtitle,
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
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !enabled -> LeNoteTheme.colors.inkMuted.copy(alpha = 0.4f)
                    isDestructive -> MaterialTheme.colorScheme.error
                    iconTint != null -> iconTint
                    else -> LeNoteTheme.colors.inkSecondary
                },
                modifier = Modifier.size(19.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = when {
                        !enabled -> LeNoteTheme.colors.inkMuted.copy(alpha = 0.45f)
                        isDestructive -> MaterialTheme.colorScheme.error
                        else -> LeNoteTheme.colors.inkPrimary
                    }
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (!enabled) LeNoteTheme.colors.inkMuted.copy(alpha = 0.35f) else LeNoteTheme.colors.inkMuted
                    )
                }
            }
        }
    }
}

/**
 * Indicador e Controle Editorial de Zoom no canto inferior direito:
 * - Em repouso: cápsula de alto contraste com tipografia monoespaçada (ex.: "100%");
 * - Ao tocar: expande controles [ − ] 100% [ + ] e [ Ajustar à página ].
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
    val s = LeNoteTheme.strings
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder),
        shadowElevation = 3.dp
    ) {
        if (!isExpanded) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { isExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                    contentDescription = null,
                    tint = LeNoteTheme.colors.inkSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${kotlin.math.round(zoomLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = LeNoteTheme.colors.inkPrimary
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Minus,
                        contentDescription = s.zoomOutDesc,
                        tint = LeNoteTheme.colors.inkPrimary,
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
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = LeNoteTheme.colors.accentPrimary
                    )
                }

                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Plus,
                        contentDescription = s.zoomInDesc,
                        tint = LeNoteTheme.colors.inkPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(LeNoteTheme.colors.hairlineBorder)
                )

                IconButton(
                    onClick = {
                        onFitToPage()
                        isExpanded = false
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.CornersOut,
                        contentDescription = s.zoomFitToPageDesc,
                        tint = LeNoteTheme.colors.inkPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = { isExpanded = false },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = s.zoomCloseDesc,
                        tint = LeNoteTheme.colors.inkMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Card flutuante de confirmação pós-exportação (PDF ou PNG).
 */
@Composable
private fun ExportConfirmationBanner(
    state: ExportConfirmationState,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LeNoteTheme.strings
    Surface(
        modifier = modifier
            .widthIn(min = 320.dp, max = 540.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = LeNoteTheme.colors.elevatedSurface,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (state.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else LeNoteTheme.colors.hairlineBorder
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val iconVector = when {
                state.isError -> PhosphorIcons.Regular.WarningCircle
                state.exportResult.mimeType == "application/pdf" -> PhosphorIcons.Regular.FilePdf
                else -> PhosphorIcons.Regular.Image
            }
            val iconTint = when {
                state.isError -> MaterialTheme.colorScheme.error
                state.exportResult.mimeType == "application/pdf" -> LeNotePalette.TerracottaAccent
                else -> LeNoteTheme.colors.accentPrimary
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.isError) MaterialTheme.colorScheme.errorContainer else LeNoteTheme.colors.tonalSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = LeNoteTheme.colors.inkPrimary
                )
                if (state.subtitle.isNotBlank()) {
                    Text(
                        text = state.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = LeNoteTheme.colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (state.canOpen) {
                val actionLabel = if (state.exportResult.mimeType == "application/pdf") s.exportOpenPdfButton else s.exportViewImageButton
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LeNoteTheme.colors.accentPrimary,
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
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = Color.White
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.X,
                    contentDescription = s.actionClose,
                    tint = LeNoteTheme.colors.inkMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

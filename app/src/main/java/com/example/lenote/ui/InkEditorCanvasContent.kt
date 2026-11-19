package com.example.lenote.ui

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.lenote.export.ExportViewerHelper
import com.example.lenote.export.GeminiShareHelper
import com.example.lenote.export.GeminiShareOutcome
import com.example.lenote.ink.JetpackInkEngine
import com.example.lenote.ui.theme.LeNoteTheme

@Composable
internal fun InkEditorCanvasContent(
    uiState: InkEditorCanvasUiState,
    dialogs: InkEditorDialogState,
    viewModel: InkPlaygroundViewModel?,
    engine: JetpackInkEngine,
    insertImage: () -> Unit,
    onNavigateBack: (() -> Unit)?,
    onCanvasSizeChanged: (IntSize) -> Unit,
    onLocalSave: (ByteArray, String) -> Unit,
    onLocalLoad: (String) -> Unit,
    onCloseLocalDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val s = LeNoteTheme.strings
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LeNoteTheme.colors.paperBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged(onCanvasSizeChanged)
        ) {
            // A) CANVAS / FOLHA: Elemento visual dominante ocupando toda a tela
            InkCanvas(
                engine = engine,
                modifier = Modifier.fillMaxSize()
            )

            // Camada Interativa de Mídia Rica (Caixas de Texto e Imagens)
            CanvasMediaOverlay(
                engine = engine,
                textBlocks = uiState.textBlocks,
                imageBlocks = uiState.imageBlocks,
                isTextToolActive = uiState.isTextToolActive,
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
                currentPage = uiState.currentPage,
                pageIndexLabel = if (uiState.currentPageIndex >= 0 && uiState.pageCount > 0) "${uiState.currentPageIndex + 1}/${uiState.pageCount}" else null,
                isSaving = uiState.isSaving,
                saveStatus = uiState.saveStatus,
                onNavigateBack = {
                    viewModel?.flushCurrentPageSave()
                    onNavigateBack?.invoke()
                },
                onToggleSidebar = { viewModel?.toggleSidebar() },
                onRenamePageClick = {
                    dialogs.renamePageInput = uiState.currentPage?.title ?: s.defaultPageTitle(1)
                    dialogs.showRenamePageDialog = true
                },
                onToggleFavorite = { viewModel?.toggleCurrentPageFavorite() },
                onOpenSheetOptions = { dialogs.showTemplatePickerDialog = true },
                onOpenMoreOptions = { dialogs.showPageActionsModal = true },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )

            // C) DOCK FLUTUANTE DE FERRAMENTAS & PAINÉIS CONTEXTUAIS
            ToolbarOverlayStateHost(
                engine = engine,
                viewModel = viewModel,
                onInsertImage = insertImage,
                onSave = { strokeCount ->
                    if (viewModel != null) {
                        viewModel.saveCurrentStrokes()
                    } else {
                        val bytes = engine.saveStrokes()
                        onLocalSave(bytes, "${s.statusSaved}: ${bytes.size} bytes ($strokeCount)")
                    }
                },
                onLoad = {
                    if (viewModel != null) {
                        viewModel.loadSavedStrokes()
                    } else {
                        uiState.savedStrokesData?.let { bytes ->
                            engine.loadStrokes(bytes)
                            onLocalLoad(s.statusSaved)
                        }
                    }
                },
                canLoad = uiState.savedStrokesData != null,
                onPromptClear = { dialogs.showClearConfirmDialog = true },
                onSendSelectionToGemini = { action ->
                    val handleOutcome: (GeminiShareOutcome) -> Unit = { outcome ->
                        if (!outcome.isSuccess) {
                            Toast.makeText(
                                context,
                                outcome.errorMessage ?: s.geminiShareErrorMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (outcome.promptCopiedToClipboard) {
                            Toast.makeText(
                                context,
                                s.geminiPromptCopiedMessage,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    if (viewModel != null) {
                        viewModel.shareSelectionToGemini(
                            activityContext = context,
                            action = action,
                            strings = s,
                            onComplete = handleOutcome
                        )
                    } else {
                        val selectedStrokes = engine.getSelectedStrokesSnapshot()
                        val bounds = engine.state.value.selectionBoundsWorld
                        if (selectedStrokes.isNotEmpty() && bounds != null) {
                            val paperColor = uiState.currentPage?.paperColor ?: 0xFFFFFFFF
                            val pngFile = GeminiShareHelper.renderSelectionCropToPngFile(
                                context = context,
                                bounds = bounds,
                                selectedStrokes = selectedStrokes,
                                background = engine.state.value.pageBackground,
                                isDarkMode = paperColor == 0xFF1E293B,
                                paperColor = paperColor,
                                pdfPath = uiState.currentPage?.pdfPath,
                                pdfPageIndex = uiState.currentPage?.pdfPageIndex,
                                textBlocks = uiState.textBlocks,
                                imageBlocks = uiState.imageBlocks
                            )
                            val prompt = GeminiShareHelper.buildPrompt(action, s, isFullPage = false)
                            handleOutcome(
                                GeminiShareHelper.shareImageToGemini(
                                    context = context,
                                    imageFile = pngFile,
                                    prompt = prompt,
                                    chooserTitle = s.geminiChooserTitle
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp, start = 12.dp, end = 12.dp)
            )

            // D) TELEMETRIA & DIAGNÓSTICO (Opcional, ativável em Mais Opções -> Diagnósticos)
            if (uiState.isDiagnosticsVisible) {
                DiagnosticsOverlayStateHost(
                    engine = engine,
                    saveStatus = uiState.saveStatus,
                    onClose = {
                        if (viewModel != null) {
                            viewModel.toggleDiagnosticsOverlay()
                        } else {
                            onCloseLocalDiagnostics()
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
                    visible = uiState.isSidebarOpen,
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
                    visible = uiState.isSidebarOpen,
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
                visible = dialogs.exportConfirmation != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 16.dp)
            ) {
                dialogs.exportConfirmation?.let { confState ->
                    ExportConfirmationBanner(
                        state = confState,
                        onOpen = {
                            ExportViewerHelper.openExportedFile(context, confState.exportResult)
                                .onSuccess {
                                    dialogs.exportConfirmation = null
                                }
                                .onFailure { ex ->
                                    val isPdf = confState.exportResult.mimeType == "application/pdf"
                                    val appType = if (isPdf) "PDF" else "PNG"
                                    if (ex is ActivityNotFoundException) {
                                        dialogs.exportConfirmation = confState.copy(
                                            title = s.exportNoAppFoundError(appType),
                                            subtitle = confState.exportResult.displayPath,
                                            isError = true,
                                            canOpen = false
                                        )
                                    } else {
                                        dialogs.exportConfirmation = confState.copy(
                                            title = s.exportOpenFailedError(appType),
                                            subtitle = confState.exportResult.displayPath,
                                            isError = true,
                                            canOpen = false
                                        )
                                    }
                                }
                        },
                        onDismiss = { dialogs.exportConfirmation = null }
                    )
                }
            }

            // G) INDICADOR E CONTROLE DISCRETO DE ZOOM (Canto inferior direito, acima da barra de navegação)
            ZoomIndicatorControl(
                zoomLevel = uiState.zoomLevel,
                isNavigationLocked = uiState.isNavigationLocked,
                onZoomIn = { engine.zoomIn() },
                onZoomOut = { engine.zoomOut() },
                onResetZoom = { engine.resetZoom() },
                onFitToPage = { engine.fitToPage() },
                onToggleNavigationLock = { engine.setNavigationLocked(!uiState.isNavigationLocked) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 14.dp)
            )
        }
    }
}

package com.example.lenote.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import com.example.lenote.data.PageEntity
import com.example.lenote.export.ExportResult
import com.example.lenote.export.PageExporter
import com.example.lenote.ink.JetpackInkEngine
import com.example.lenote.ui.theme.LeNoteTheme
import java.io.File

@Composable
internal fun InkEditorPageActionsDialogs(
    state: InkEditorDialogState,
    viewModel: InkPlaygroundViewModel?,
    engine: JetpackInkEngine,
    currentPage: PageEntity?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isDiagnosticsVisible: Boolean,
    onToggleDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val s = LeNoteTheme.strings
    // 1. Diálogo dedicado: Opções da Folha (Tipo de papel e Suavização)
    if (state.showSheetOptionsModal) {
        val currentState by engine.state.collectAsState()
        SheetOptionsDialog(
            state = currentState,
            onPageBackgroundSelect = { engine.setPageBackground(it) },
            onSmoothingSelect = { engine.setSmoothingLevel(it) },
            onDismiss = { state.showSheetOptionsModal = false }
        )
    }

    // 2. Diálogo dedicado: Ações da Página (Menu "⋮" do Header)
    if (state.showPageActionsModal) {
        PageActionsDialog(
            currentPage = currentPage,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onRenamePage = {
                state.renamePageInput = currentPage?.title ?: s.defaultPageTitle(1)
                state.showRenamePageDialog = true
                state.showPageActionsModal = false
            },
            onDuplicatePage = {
                viewModel?.duplicateCurrentPage()
                state.showPageActionsModal = false
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
                state.showPageActionsModal = false
                state.showTemplatePickerDialog = true
            },
            onExportPdf = {
                state.showPageActionsModal = false
                if (viewModel != null) {
                    viewModel.exportCurrentPageToPdf { res ->
                        state.exportConfirmation = if (res.isSuccess) {
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
                        state.exportConfirmation = ExportConfirmationState(
                            exportResult = res,
                            title = s.exportPdfSuccessTitle,
                            subtitle = res.displayPath,
                            isError = false,
                            canOpen = true
                        )
                    } catch (e: Exception) {
                        val dummy = File(context.cacheDir, "err.pdf")
                        state.exportConfirmation = ExportConfirmationState(
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
                state.showPageActionsModal = false
                if (viewModel != null) {
                    viewModel.exportCurrentPageToPng { res ->
                        state.exportConfirmation = if (res.isSuccess) {
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
                        state.exportConfirmation = ExportConfirmationState(
                            exportResult = res,
                            title = s.exportPngSuccessTitle,
                            subtitle = res.displayPath,
                            isError = false,
                            canOpen = true
                        )
                    } catch (e: Exception) {
                        val dummy = File(context.cacheDir, "err.png")
                        state.exportConfirmation = ExportConfirmationState(
                            exportResult = ExportResult(dummy, null, "", isSuccess = false),
                            title = s.exportPngErrorTitle,
                            subtitle = e.message ?: "",
                            isError = true,
                            canOpen = false
                        )
                    }
                }
            },
            onSendPageToGemini = {
                state.showPageActionsModal = false
                state.showPageGeminiDialog = true
            },
            onDeletePage = {
                state.showPageActionsModal = false
                state.showDeletePageConfirmDialog = true
            },
            onPromptClear = {
                state.showPageActionsModal = false
                state.showClearConfirmDialog = true
            },
            isDiagnosticsVisible = isDiagnosticsVisible,
            onToggleDiagnostics = onToggleDiagnostics,
            onDismiss = { state.showPageActionsModal = false }
        )
    }
}

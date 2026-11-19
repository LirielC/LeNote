package com.example.lenote.ui

import androidx.compose.runtime.Composable
import com.example.lenote.data.CanvasImageBlock
import com.example.lenote.data.CanvasTextBlock
import com.example.lenote.data.PageEntity
import com.example.lenote.ink.InkingState
import com.example.lenote.ink.JetpackInkEngine

@Composable
internal fun InkEditorDialogsHost(
    state: InkEditorDialogState,
    viewModel: InkPlaygroundViewModel?,
    engine: JetpackInkEngine,
    currentPage: PageEntity?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    isDiagnosticsVisible: Boolean,
    engineState: InkingState,
    textBlocks: List<CanvasTextBlock>,
    imageBlocks: List<CanvasImageBlock>,
    onToggleDiagnostics: () -> Unit
) {
    InkEditorConfirmationDialogs(
        state = state,
        viewModel = viewModel,
        engine = engine,
        currentPage = currentPage
    )
    InkEditorPageActionsDialogs(
        state = state,
        viewModel = viewModel,
        engine = engine,
        currentPage = currentPage,
        canMoveUp = canMoveUp,
        canMoveDown = canMoveDown,
        isDiagnosticsVisible = isDiagnosticsVisible,
        onToggleDiagnostics = onToggleDiagnostics
    )
    InkEditorGeminiDialogs(
        state = state,
        viewModel = viewModel,
        engine = engine,
        currentPage = currentPage,
        engineState = engineState,
        textBlocks = textBlocks,
        imageBlocks = imageBlocks
    )
}

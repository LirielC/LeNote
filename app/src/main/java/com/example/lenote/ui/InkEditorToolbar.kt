package com.example.lenote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lenote.export.GeminiPromptAction
import com.example.lenote.ink.EraserSize
import com.example.lenote.ink.InkingState
import com.example.lenote.ink.InkingTool

@Composable
fun ToolbarOverlay(
    state: InkingState,
    onToolSelect: (InkingTool) -> Unit,
    onEraserSizeSelect: (EraserSize) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onDeleteSelection: () -> Unit = {},
    onDuplicateSelection: () -> Unit = {},
    onRecolorSelection: (Int) -> Unit = {},
    onClearSelection: () -> Unit = {},
    onBeautifySelection: () -> Unit = {},
    onAlignBaseline: () -> Unit = {},
    onSendSelectionToGemini: (GeminiPromptAction) -> Unit = {},
    isTextActive: Boolean = false,
    onToggleText: () -> Unit = {},
    onInsertImage: () -> Unit = {},
    onToggleTouchInking: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val toolbarState = rememberInkToolbarState(state)
    val isPenActive = state.currentTool is InkingTool.Pen &&
        !state.isStylusButtonEraserActive && !isTextActive
    val isHighlighterActive = state.currentTool is InkingTool.Highlighter &&
        !state.isStylusButtonEraserActive && !isTextActive
    val isEraserActive = (state.currentTool is InkingTool.Eraser ||
        state.isStylusButtonEraserActive) && !isTextActive
    val isLassoActive = state.currentTool is InkingTool.Lasso &&
        !state.isStylusButtonEraserActive && !isTextActive

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InkToolbarDock(
            state = state,
            toolbarState = toolbarState,
            isPenActive = isPenActive,
            isHighlighterActive = isHighlighterActive,
            isEraserActive = isEraserActive,
            isLassoActive = isLassoActive,
            isTextActive = isTextActive,
            onToolSelect = onToolSelect,
            onToggleText = onToggleText,
            onInsertImage = onInsertImage,
            onToggleTouchInking = onToggleTouchInking,
            onUndo = onUndo,
            onRedo = onRedo
        )
        InkPenOptionsPanel(
            visible = isPenActive && toolbarState.activePanel == ContextualPanelType.PEN,
            state = state,
            toolbarState = toolbarState,
            onToolSelect = onToolSelect
        )
        InkHighlighterOptionsPanel(
            visible = isHighlighterActive &&
                toolbarState.activePanel == ContextualPanelType.HIGHLIGHTER,
            state = state,
            toolbarState = toolbarState,
            onToolSelect = onToolSelect
        )
        InkEraserOptionsPanel(
            visible = isEraserActive && toolbarState.activePanel == ContextualPanelType.ERASER,
            state = state,
            onEraserSizeSelect = onEraserSizeSelect
        )
        InkSelectionOptionsPanel(
            visible = isLassoActive,
            state = state,
            onDeleteSelection = onDeleteSelection,
            onDuplicateSelection = onDuplicateSelection,
            onRecolorSelection = onRecolorSelection,
            onClearSelection = onClearSelection,
            onBeautifySelection = onBeautifySelection,
            onAlignBaseline = onAlignBaseline,
            onSendSelectionToGemini = onSendSelectionToGemini
        )
    }
}

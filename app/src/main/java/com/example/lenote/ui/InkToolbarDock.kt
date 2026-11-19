package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lenote.ink.InkingState
import com.example.lenote.ink.InkingTool
import com.example.lenote.ui.theme.LeNoteTheme

@Composable
internal fun InkToolbarDock(
    state: InkingState,
    toolbarState: InkToolbarState,
    isPenActive: Boolean,
    isHighlighterActive: Boolean,
    isEraserActive: Boolean,
    isLassoActive: Boolean,
    isTextActive: Boolean,
    onToolSelect: (InkingTool) -> Unit,
    onToggleText: () -> Unit,
    onInsertImage: () -> Unit,
    onToggleTouchInking: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    val s = LeNoteTheme.strings
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
                badgeColor = Color(toolbarState.penColor),
                onClick = {
                    if (isPenActive) {
                        toolbarState.activePanel = if (toolbarState.activePanel == ContextualPanelType.PEN) {
                            ContextualPanelType.NONE
                        } else {
                            ContextualPanelType.PEN
                        }
                    } else {
                        onToolSelect(InkingTool.Pen(colorInt = toolbarState.penColor, size = toolbarState.penSize))
                        toolbarState.activePanel = ContextualPanelType.PEN
                    }
                }
            )

            // Marca-texto
            ToolbarToolButton(
                iconRegular = PhosphorIcons.Regular.HighlighterCircle,
                iconFill = PhosphorIcons.Fill.HighlighterCircle,
                contentDescription = s.toolHighlighter,
                isSelected = isHighlighterActive,
                badgeColor = Color(toolbarState.highlighterColor),
                onClick = {
                    if (isHighlighterActive) {
                        toolbarState.activePanel = if (toolbarState.activePanel == ContextualPanelType.HIGHLIGHTER) {
                            ContextualPanelType.NONE
                        } else {
                            ContextualPanelType.HIGHLIGHTER
                        }
                    } else {
                        onToolSelect(InkingTool.Highlighter(colorInt = toolbarState.highlighterColor, size = toolbarState.highlighterSize))
                        toolbarState.activePanel = ContextualPanelType.HIGHLIGHTER
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
                        toolbarState.activePanel = if (toolbarState.activePanel == ContextualPanelType.ERASER) {
                            ContextualPanelType.NONE
                        } else {
                            ContextualPanelType.ERASER
                        }
                    } else {
                        onToolSelect(InkingTool.Eraser(state.eraserSize))
                        toolbarState.activePanel = ContextualPanelType.ERASER
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
                    toolbarState.activePanel = ContextualPanelType.NONE
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
                    toolbarState.activePanel = ContextualPanelType.NONE
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
                    toolbarState.activePanel = ContextualPanelType.NONE
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
}

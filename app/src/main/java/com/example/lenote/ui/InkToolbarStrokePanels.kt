package com.example.lenote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.lenote.ink.HighlighterSize
import com.example.lenote.ink.InkingState
import com.example.lenote.ink.InkingTool
import com.example.lenote.ink.PenStrokeSize
import com.example.lenote.ui.theme.LeNoteTheme

@Composable
internal fun InkPenOptionsPanel(
    visible: Boolean,
    state: InkingState,
    toolbarState: InkToolbarState,
    onToolSelect: (InkingTool) -> Unit
) {
    val s = LeNoteTheme.strings
    // Painel Contextual 1: Caneta (Cores + Amostras Visuais de Espessura)
    AnimatedVisibility(
        visible = visible,
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
                    inkPenColors.forEach { c ->
                        val colorArgb = c.toArgb()
                        val isSelected = (state.currentTool as? InkingTool.Pen)?.colorInt == colorArgb
                        ColorPickerDot(
                            color = c,
                            isSelected = isSelected,
                            onClick = {
                                toolbarState.penColor = colorArgb
                                val currentPen = state.currentTool as? InkingTool.Pen
                                onToolSelect(currentPen?.copy(colorInt = colorArgb) ?: InkingTool.Pen(colorInt = colorArgb, size = toolbarState.penSize))
                            }
                        )
                    }
                }

                // Amostras Visuais de Espessura da Caneta
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentSize = (state.currentTool as? InkingTool.Pen)?.size ?: toolbarState.penSize
                    PenStrokeSize.entries.forEach { sz ->
                        val isSelected = kotlin.math.abs(currentSize - sz.sizeWorld) < 0.5f
                        StrokeSizeChip(
                            label = s.penStrokeSizeLabel(sz),
                            strokeWidthPx = sz.sizeWorld * 1.4f,
                            strokeColor = Color(toolbarState.penColor),
                            isSelected = isSelected,
                            onClick = {
                                toolbarState.penSize = sz.sizeWorld
                                val currentPen = state.currentTool as? InkingTool.Pen
                                onToolSelect(currentPen?.copy(size = sz.sizeWorld) ?: InkingTool.Pen(colorInt = toolbarState.penColor, size = sz.sizeWorld))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun InkHighlighterOptionsPanel(
    visible: Boolean,
    state: InkingState,
    toolbarState: InkToolbarState,
    onToolSelect: (InkingTool) -> Unit
) {
    val s = LeNoteTheme.strings
    // Painel Contextual 2: Marca-texto (Cores + Amostras Visuais de Espessura)
    AnimatedVisibility(
        visible = visible,
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
                    inkHighlighterColors.forEach { c ->
                        val colorArgb = c.toArgb()
                        val isSelected = (state.currentTool as? InkingTool.Highlighter)?.colorInt == colorArgb
                        ColorPickerDot(
                            color = c,
                            isSelected = isSelected,
                            onClick = {
                                toolbarState.highlighterColor = colorArgb
                                val currentHl = state.currentTool as? InkingTool.Highlighter
                                onToolSelect(currentHl?.copy(colorInt = colorArgb) ?: InkingTool.Highlighter(colorInt = colorArgb, size = toolbarState.highlighterSize))
                            }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val currentSize = (state.currentTool as? InkingTool.Highlighter)?.size ?: toolbarState.highlighterSize
                    HighlighterSize.entries.forEach { sz ->
                        val isSelected = kotlin.math.abs(currentSize - sz.sizeWorld) < 0.5f
                        StrokeSizeChip(
                            label = s.highlighterSizeLabel(sz),
                            strokeWidthPx = sz.sizeWorld * 0.7f,
                            strokeColor = Color(toolbarState.highlighterColor),
                            isSelected = isSelected,
                            onClick = {
                                toolbarState.highlighterSize = sz.sizeWorld
                                val currentHl = state.currentTool as? InkingTool.Highlighter
                                onToolSelect(currentHl?.copy(size = sz.sizeWorld) ?: InkingTool.Highlighter(colorInt = toolbarState.highlighterColor, size = sz.sizeWorld))
                            }
                        )
                    }
                }
            }
        }
    }
}

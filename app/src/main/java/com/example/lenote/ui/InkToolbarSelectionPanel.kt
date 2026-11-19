package com.example.lenote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.export.GeminiPromptAction
import com.example.lenote.ink.InkingState
import com.example.lenote.ui.theme.LeNoteTheme

@Composable
internal fun InkSelectionOptionsPanel(
    visible: Boolean,
    state: InkingState,
    onDeleteSelection: () -> Unit,
    onDuplicateSelection: () -> Unit,
    onRecolorSelection: (Int) -> Unit,
    onClearSelection: () -> Unit,
    onBeautifySelection: () -> Unit,
    onAlignBaseline: () -> Unit,
    onSendSelectionToGemini: (GeminiPromptAction) -> Unit
) {
    val s = LeNoteTheme.strings
    // Painel Contextual 4: Ações de Seleção do Laço
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
                    LassoGeminiActionMenu(onAction = onSendSelectionToGemini)

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
                    inkPenColors.forEach { c ->
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

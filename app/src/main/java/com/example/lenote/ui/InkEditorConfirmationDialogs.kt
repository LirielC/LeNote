package com.example.lenote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lenote.data.PageEntity
import com.example.lenote.ink.JetpackInkEngine
import com.example.lenote.ui.theme.LeNoteTheme

@Composable
internal fun InkEditorConfirmationDialogs(
    state: InkEditorDialogState,
    viewModel: InkPlaygroundViewModel?,
    engine: JetpackInkEngine,
    currentPage: PageEntity?
) {
    val s = LeNoteTheme.strings
    // Diálogo de Renomear Página
    if (state.showRenamePageDialog) {
        AlertDialog(
            onDismissRequest = { state.showRenamePageDialog = false },
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
                    value = state.renamePageInput,
                    onValueChange = { state.renamePageInput = it },
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
                            viewModel.renameCurrentPage(state.renamePageInput)
                        }
                        state.showRenamePageDialog = false
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
                TextButton(onClick = { state.showRenamePageDialog = false }) {
                    Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
                }
            }
        )
    }

    // Confirmação para Ação Destrutiva "Limpar Página"
    if (state.showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { state.showClearConfirmDialog = false },
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
                        state.showClearConfirmDialog = false
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
                TextButton(onClick = { state.showClearConfirmDialog = false }) {
                    Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
                }
            }
        )
    }

    // Confirmação para Ação Destrutiva "Excluir Página"
    if (state.showDeletePageConfirmDialog) {
        AlertDialog(
            onDismissRequest = { state.showDeletePageConfirmDialog = false },
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
                        state.showDeletePageConfirmDialog = false
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
                TextButton(onClick = { state.showDeletePageConfirmDialog = false }) {
                    Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
                }
            }
        )
    }
}

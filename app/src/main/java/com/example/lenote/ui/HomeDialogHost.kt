package com.example.lenote.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lenote.data.AppLanguage
import com.example.lenote.data.NotebookEntity
import com.example.lenote.ui.theme.EditorialFeedbackBannerState
import com.example.lenote.ui.theme.EditorialFeedbackType
import com.example.lenote.ui.theme.LeNoteTheme

@Composable
internal fun HomeDialogHost(
    state: HomeScreenState,
    viewModel: InkPlaygroundViewModel,
    appLanguage: AppLanguage,
    notebooks: List<NotebookEntity>,
    onOpenNotebook: (NotebookEntity) -> Unit
) {
    val s = LeNoteTheme.strings
    // Diálogo de Escolha de Idioma
    if (state.showLanguagePickerDialog) {
        LanguagePickerDialog(
            currentPreference = appLanguage,
            onSelectLanguage = { lang ->
                viewModel.setAppLanguage(lang)
                state.showLanguagePickerDialog = false
            },
            onDismiss = { state.showLanguagePickerDialog = false }
        )
    }

    // Diálogo de Criação de Novo Caderno
    if (state.showCreateDialog) {
        CreateNotebookDialog(
            onDismiss = { state.showCreateDialog = false },
            onCreate = { title, colorArgb ->
                viewModel.createNewNotebook(title, colorArgb)
                state.showCreateDialog = false
                state.feedbackBanner = EditorialFeedbackBannerState(
                    type = EditorialFeedbackType.SUCCESS,
                    title = s.bannerNotebookCreatedTitle,
                    message = s.bannerNotebookCreatedMessage(title)
                )
                notebooks.find { it.title == title }?.let { nb ->
                    onOpenNotebook(nb)
                } ?: run {
                    viewModel.notebooks.value.firstOrNull()?.let { onOpenNotebook(it) }
                }
            }
        )
    }

    // Diálogos de Confirmação da Lixeira
    if (state.showEmptyTrashConfirmDialog) {
        AlertDialog(
            onDismissRequest = { state.showEmptyTrashConfirmDialog = false },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = {
                Text(
                    text = s.emptyTrashDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = LeNoteTheme.colors.inkPrimary
                )
            },
            text = {
                Text(
                    text = s.emptyTrashDialogMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LeNoteTheme.colors.inkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.emptyTrash()
                        state.showEmptyTrashConfirmDialog = false
                        state.feedbackBanner = EditorialFeedbackBannerState(
                            type = EditorialFeedbackType.SUCCESS,
                            title = s.bannerTrashEmptiedTitle,
                            message = s.bannerTrashEmptiedMessage
                        )
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(s.actionEmptyPermanently)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.showEmptyTrashConfirmDialog = false }) {
                    Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
                }
            }
        )
    }

    if (state.notebookToDeletePermanently != null) {
        val nb = state.notebookToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { state.notebookToDeletePermanently = null },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = {
                Text(
                    text = s.deleteNotebookDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = LeNoteTheme.colors.inkPrimary
                )
            },
            text = {
                Text(
                    text = s.deleteNotebookDialogMessage(nb.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LeNoteTheme.colors.inkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeleteNotebook(nb.id)
                        state.notebookToDeletePermanently = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(s.actionDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.notebookToDeletePermanently = null }) {
                    Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
                }
            }
        )
    }

    if (state.pageToDeletePermanently != null) {
        val p = state.pageToDeletePermanently!!
        AlertDialog(
            onDismissRequest = { state.pageToDeletePermanently = null },
            containerColor = LeNoteTheme.colors.elevatedSurface,
            title = {
                Text(
                    text = s.deletePageDialogTitle,
                    style = MaterialTheme.typography.titleLarge,
                    color = LeNoteTheme.colors.inkPrimary
                )
            },
            text = {
                Text(
                    text = s.deletePageDialogMessage(p.title),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LeNoteTheme.colors.inkSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.permanentlyDeletePage(p.id)
                        state.pageToDeletePermanently = null
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(s.actionDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { state.pageToDeletePermanently = null }) {
                    Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
                }
            }
        )
    }
}

package com.example.lenote.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.lenote.data.NotebookEntity
import com.example.lenote.ui.theme.EditorialFeedbackBannerState
import com.example.lenote.ui.theme.EditorialFeedbackType
import com.example.lenote.ui.theme.LeNoteTheme
import java.io.File
import java.io.FileOutputStream

internal data class HomeFileActions(
    val importPdf: () -> Unit,
    val importLenote: () -> Unit,
    val exportNotebook: (NotebookEntity) -> Unit
)

@Composable
internal fun rememberHomeFileActions(
    viewModel: InkPlaygroundViewModel,
    onOpenNotebook: (NotebookEntity) -> Unit,
    onFeedback: (EditorialFeedbackBannerState) -> Unit
): HomeFileActions {
    val context = LocalContext.current
    val s = LeNoteTheme.strings
    var notebookToExport by remember { mutableStateOf<NotebookEntity?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
                ?: "Documento.pdf"
            val cleanName = if (fileName.endsWith(".pdf", ignoreCase = true)) {
                fileName
            } else {
                "$fileName.pdf"
            }
            val targetFile = File(
                File(context.filesDir, "pdfs").apply { mkdirs() },
                "pdf_${System.currentTimeMillis()}_$cleanName"
            )
            onFeedback(
                EditorialFeedbackBannerState(
                    EditorialFeedbackType.LOADING,
                    s.bannerImportingPdfTitle,
                    s.bannerImportingPdfMessage(cleanName)
                )
            )
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use(input::copyTo)
                }
                viewModel.importPdfDocument(cleanName.removeSuffix(".pdf"), targetFile) { notebook ->
                    onFeedback(
                        EditorialFeedbackBannerState(
                            EditorialFeedbackType.SUCCESS,
                            s.bannerPdfImportedTitle,
                            s.bannerPdfImportedMessage(notebook.title)
                        )
                    )
                    onOpenNotebook(notebook)
                }
            } catch (exception: Exception) {
                onFeedback(
                    EditorialFeedbackBannerState(
                        EditorialFeedbackType.ERROR,
                        s.bannerPdfImportFailedTitle,
                        exception.localizedMessage ?: s.bannerPdfImportFailedDefaultMsg
                    )
                )
            }
        }
    }

    val lenotePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val fileName = uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
                ?: "caderno.lenote"
            val tempFile = File(context.cacheDir, "import_${System.currentTimeMillis()}_$fileName")
            onFeedback(
                EditorialFeedbackBannerState(
                    EditorialFeedbackType.LOADING,
                    s.bannerImportingLenoteTitle,
                    s.bannerImportingLenoteMessage
                )
            )
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use(input::copyTo)
                }
                viewModel.importNotebookPackage(tempFile) { notebook ->
                    tempFile.delete()
                    if (notebook != null) {
                        onFeedback(
                            EditorialFeedbackBannerState(
                                EditorialFeedbackType.SUCCESS,
                                s.bannerLenoteImportedTitle,
                                s.bannerLenoteImportedMessage(notebook.title)
                            )
                        )
                        onOpenNotebook(notebook)
                    } else {
                        onFeedback(
                            EditorialFeedbackBannerState(
                                EditorialFeedbackType.ERROR,
                                s.bannerLenoteInvalidTitle,
                                s.bannerLenoteInvalidMessage
                            )
                        )
                    }
                }
            } catch (exception: Exception) {
                onFeedback(
                    EditorialFeedbackBannerState(
                        EditorialFeedbackType.ERROR,
                        s.bannerImportErrorTitle,
                        exception.localizedMessage ?: s.bannerImportErrorDefaultMsg
                    )
                )
            }
        }
    }

    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val notebook = notebookToExport
        if (uri != null && notebook != null) {
            onFeedback(
                EditorialFeedbackBannerState(
                    EditorialFeedbackType.LOADING,
                    s.bannerExportingNotebookTitle(notebook.title),
                    s.bannerExportingNotebookMessage
                )
            )
            val tempZip = File(context.cacheDir, "temp_export_${notebook.id}.lenote")
            viewModel.exportNotebookPackage(notebook.id, tempZip) { success ->
                if (success && tempZip.exists()) {
                    try {
                        tempZip.inputStream().use { input ->
                            context.contentResolver.openOutputStream(uri)?.use(input::copyTo)
                        }
                        onFeedback(
                            EditorialFeedbackBannerState(
                                EditorialFeedbackType.SUCCESS,
                                s.bannerExportSuccessTitle,
                                s.bannerExportSuccessMessage(notebook.title)
                            )
                        )
                    } catch (exception: Exception) {
                        onFeedback(
                            EditorialFeedbackBannerState(
                                EditorialFeedbackType.ERROR,
                                s.bannerExportWriteErrorTitle,
                                exception.localizedMessage ?: s.bannerExportFailedMessage
                            )
                        )
                    }
                    tempZip.delete()
                } else {
                    onFeedback(
                        EditorialFeedbackBannerState(
                            EditorialFeedbackType.ERROR,
                            s.bannerExportFailedTitle,
                            s.bannerExportFailedMessage
                        )
                    )
                }
            }
        }
        notebookToExport = null
    }

    return HomeFileActions(
        importPdf = { pdfPicker.launch(arrayOf("application/pdf")) },
        importLenote = {
            lenotePicker.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
        },
        exportNotebook = { notebook ->
            notebookToExport = notebook
            val safeTitle = notebook.title.replace(Regex("[^a-zA-Z0-9_]"), "_")
            exportPicker.launch("$safeTitle.lenote")
        }
    )
}

package com.example.lenote.ui

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.lenote.data.CanvasImageBlock
import com.example.lenote.data.CanvasTextBlock
import com.example.lenote.data.PageEntity
import com.example.lenote.export.GeminiShareHelper
import com.example.lenote.export.GeminiShareOutcome
import com.example.lenote.ink.InkingState
import com.example.lenote.ink.JetpackInkEngine
import com.example.lenote.ui.theme.LeNoteTheme

@Composable
internal fun InkEditorGeminiDialogs(
    state: InkEditorDialogState,
    viewModel: InkPlaygroundViewModel?,
    engine: JetpackInkEngine,
    currentPage: PageEntity?,
    engineState: InkingState,
    textBlocks: List<CanvasTextBlock>,
    imageBlocks: List<CanvasImageBlock>
) {
    val context = LocalContext.current
    val s = LeNoteTheme.strings
    // Diálogo de Ações Rápidas para Enviar Página Inteira ao Gemini
    if (state.showPageGeminiDialog) {
        GeminiActionPickerDialog(
            title = s.geminiMenuPageTitle,
            onSelectAction = { action ->
                state.showPageGeminiDialog = false
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
                    viewModel.shareCurrentPageToGemini(
                        activityContext = context,
                        action = action,
                        strings = s,
                        onComplete = handleOutcome
                    )
                } else {
                    val paperColor = currentPage?.paperColor ?: 0xFFFFFFFF
                    val pngFile = GeminiShareHelper.renderFullPageToPngFile(
                        context = context,
                        background = engine.state.value.pageBackground,
                        strokes = engine.getFinishedStrokesSnapshot(),
                        isDarkMode = paperColor == 0xFF1E293B,
                        paperColor = paperColor,
                        pdfPath = currentPage?.pdfPath,
                        pdfPageIndex = currentPage?.pdfPageIndex,
                        textBlocks = textBlocks,
                        imageBlocks = imageBlocks
                    )
                    val prompt = GeminiShareHelper.buildPrompt(action, s, isFullPage = true)
                    handleOutcome(
                        GeminiShareHelper.shareImageToGemini(
                            context = context,
                            imageFile = pngFile,
                            prompt = prompt,
                            chooserTitle = s.geminiChooserTitle
                        )
                    )
                }
            },
            onDismiss = { state.showPageGeminiDialog = false }
        )
    }

    // Diálogo de Seleção de Modelo e Cor de Folha (Opções da Folha)
    if (state.showTemplatePickerDialog) {
        val cur = currentPage
        PageTemplatePickerDialog(
            initialTitle = cur?.title ?: s.defaultPageTitle(1),
            initialBackground = cur?.pageBackground ?: engineState.pageBackground,
            initialPaperColor = cur?.paperColor ?: 0xFFFFFFFF,
            isCreateMode = false,
            onConfirm = { _, bg, paperColor ->
                state.showTemplatePickerDialog = false
                if (viewModel != null) {
                    viewModel.changePageTemplate(bg, paperColor)
                } else {
                    engine.setPageBackground(bg, paperColor)
                }
            },
            onDismiss = { state.showTemplatePickerDialog = false }
        )
    }
}

package com.example.lenote.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.example.lenote.export.ExportResult
import com.example.lenote.ink.JetpackInkEngine
import com.example.lenote.ui.theme.LeNoteTheme
import kotlinx.coroutines.delay

/**
 * Estado da barra de confirmação flutuante de exportação.
 */
data class ExportConfirmationState(
    val exportResult: ExportResult,
    val title: String,
    val subtitle: String,
    val isError: Boolean = false,
    val canOpen: Boolean = true
)

internal fun visibleImageOrigin(
    viewportSize: IntSize,
    zoom: Float,
    panX: Float,
    panY: Float,
    imageWidth: Float = 300f,
    imageHeight: Float = imageWidth
): Offset = Offset(
    x = (viewportSize.width / 2f - panX) / zoom - imageWidth / 2f,
    y = (viewportSize.height / 2f - panY) / zoom - imageHeight / 2f
)

/**
 * Tela principal de anotações (Studio Canvas) do LeNote:
 * - Canvas nativo ocupando a área dominante;
 * - Header Editorial compacto (50dp) em papel marfim com borda hairline inferior e indicador de salvamento;
 * - Dock Flutuante de Ferramentas responsivo (com scroll horizontal suave em telas estreitas para nunca cortar Undo/Redo);
 * - Painéis contextuais com amostras visuais reais de espessura de traço e cores editoriais;
 * - Controles inferiores respeitando `navigationBarsPadding()` (zero sobreposição com a barra do sistema).
 */
@Composable
fun InkPlaygroundScreen(
    viewModel: InkPlaygroundViewModel? = null,
    engine: JetpackInkEngine = viewModel?.engine ?: remember { JetpackInkEngine() },
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    LaunchedEffect(viewModel) {
        viewModel?.initializeIfNeeded(context)
    }

    val vmSavedData = viewModel?.savedStrokesData?.collectAsState()
    val vmSaveStatus = viewModel?.saveStatusMessage?.collectAsState()
    val vmIsSaving = viewModel?.isSaving?.collectAsState()
    val vmCurrentPage = viewModel?.currentPage?.collectAsState()
    val vmPages = viewModel?.pages?.collectAsState()
    val vmIsSidebarOpen = viewModel?.isSidebarOpen?.collectAsState()
    val vmIsDiagnosticsVisible = viewModel?.isDiagnosticsVisible?.collectAsState()
    val themeMode by (viewModel?.themeMode ?: kotlinx.coroutines.flow.MutableStateFlow(com.example.lenote.data.ThemeMode.LIGHT)).collectAsState()
    val appLanguage by (viewModel?.appLanguage ?: kotlinx.coroutines.flow.MutableStateFlow(com.example.lenote.data.AppLanguage.SYSTEM)).collectAsState()
    val isDarkMode = themeMode == com.example.lenote.data.ThemeMode.DARK
    val textBlocks by (viewModel?.textBlocks ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val imageBlocks by (viewModel?.imageBlocks ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val isTextToolActive by (viewModel?.isTextToolActive ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()
    val engineState by engine.state.collectAsState()
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    val insertImage = rememberImageImportAction(viewModel, canvasSize, engineState)

    var localSavedStrokesData by remember { mutableStateOf<ByteArray?>(null) }
    var localSaveStatusMessage by remember { mutableStateOf<String?>(null) }
    var localDiagnosticsVisible by remember { mutableStateOf(false) }

    val savedStrokesData = vmSavedData?.value ?: localSavedStrokesData
    val saveStatusMessage = vmSaveStatus?.value ?: localSaveStatusMessage
    val isSaving = vmIsSaving?.value ?: false
    val currentPage = vmCurrentPage?.value
    val pages = vmPages?.value ?: emptyList()
    val currentPageIndex = pages.indexOfFirst { it.id == currentPage?.id }
    val canMoveUp = currentPageIndex > 0
    val canMoveDown = currentPageIndex >= 0 && currentPageIndex < pages.size - 1

    val isSidebarOpen = vmIsSidebarOpen?.value ?: false
    val isDiagnosticsVisible = vmIsDiagnosticsVisible?.value ?: localDiagnosticsVisible

    val dialogs = rememberInkEditorDialogState()


    LaunchedEffect(dialogs.exportConfirmation) {
        if (dialogs.exportConfirmation != null) {
            delay(7500L)
            dialogs.exportConfirmation = null
        }
    }

    LeNoteTheme(darkTheme = isDarkMode, appLanguage = appLanguage) {
        val s = LeNoteTheme.strings
        InkEditorCanvasContent(
            uiState = InkEditorCanvasUiState(
                currentPage = currentPage,
                currentPageIndex = currentPageIndex,
                pageCount = pages.size,
                isSaving = isSaving,
                saveStatus = saveStatusMessage,
                savedStrokesData = savedStrokesData,
                isSidebarOpen = isSidebarOpen,
                isDiagnosticsVisible = isDiagnosticsVisible,
                isTextToolActive = isTextToolActive,
                zoomLevel = engineState.zoomLevel,
                isNavigationLocked = engineState.isNavigationLocked,
                textBlocks = textBlocks,
                imageBlocks = imageBlocks
            ),
            dialogs = dialogs,
            viewModel = viewModel,
            engine = engine,
            insertImage = insertImage,
            onNavigateBack = onNavigateBack,
            onCanvasSizeChanged = { canvasSize = it },
            onLocalSave = { bytes, message ->
                localSavedStrokesData = bytes
                localSaveStatusMessage = message
            },
            onLocalLoad = { localSaveStatusMessage = it },
            onCloseLocalDiagnostics = { localDiagnosticsVisible = false }
        )

        InkEditorDialogsHost(
            state = dialogs,
            viewModel = viewModel,
            engine = engine,
            currentPage = currentPage,
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            isDiagnosticsVisible = isDiagnosticsVisible,
            engineState = engineState,
            textBlocks = textBlocks,
            imageBlocks = imageBlocks,
            onToggleDiagnostics = {
                if (viewModel != null) {
                    viewModel.toggleDiagnosticsOverlay()
                } else {
                    localDiagnosticsVisible = !localDiagnosticsVisible
                }
            }
        )
    }
}

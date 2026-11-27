package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.os.Debug
import com.example.lenote.export.GeminiPromptAction
import com.example.lenote.ink.InkingState
import com.example.lenote.ink.JetpackInkEngine
import com.example.lenote.ink.InkPerformanceSnapshot
import com.example.lenote.ui.theme.LeNoteTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
internal fun DiagnosticsOverlayStateHost(
    engine: JetpackInkEngine,
    saveStatus: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by engine.state.collectAsState()
    var performance by remember(engine) { mutableStateOf(engine.performanceMonitor.snapshot()) }
    var memoryStats by remember { mutableStateOf(readMemoryStats()) }
    LaunchedEffect(engine) {
        while (true) {
            delay(500L)
            performance = engine.performanceMonitor.snapshot()
            memoryStats = readMemoryStats()
        }
    }
    DiagnosticsOverlay(
        state = state,
        performance = performance,
        memoryStats = memoryStats,
        saveStatus = saveStatus,
        onClose = onClose,
        modifier = modifier
    )
}

private fun readMemoryStats(): String {
    val runtime = Runtime.getRuntime()
    val javaUsed = runtime.totalMemory() - runtime.freeMemory()
    val gcCount = Debug.getRuntimeStat("art.gc.gc-count")?.toLongOrNull() ?: 0L
    val gcTimeMs = Debug.getRuntimeStat("art.gc.gc-time")?.toLongOrNull() ?: 0L
    val nativeUsed = Debug.getNativeHeapAllocatedSize()
    return String.format(
        Locale.US,
        "Heap Java %.1f MiB | nativa %.1f MiB | GC %d / %d ms",
        javaUsed / 1_048_576.0,
        nativeUsed / 1_048_576.0,
        gcCount,
        gcTimeMs
    )
}

@Composable
internal fun ToolbarOverlayStateHost(
    engine: JetpackInkEngine,
    viewModel: InkPlaygroundViewModel? = null,
    onInsertImage: () -> Unit = {},
    onSave: (Int) -> Unit,
    onLoad: () -> Unit,
    canLoad: Boolean,
    onPromptClear: () -> Unit,
    onSendSelectionToGemini: (GeminiPromptAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val toolbarStateFlow = remember(engine) {
        engine.state.distinctUntilChanged { old, new ->
            old.currentTool == new.currentTool &&
                old.eraserSize == new.eraserSize &&
                old.pageBackground == new.pageBackground &&
                old.smoothingLevel == new.smoothingLevel &&
                old.canUndo == new.canUndo &&
                old.canRedo == new.canRedo &&
                old.strokeCount == new.strokeCount &&
                old.selectedStrokeCount == new.selectedStrokeCount &&
                old.isStylusButtonEraserActive == new.isStylusButtonEraserActive &&
                old.isTouchInkingEnabled == new.isTouchInkingEnabled
        }
    }
    val scope = rememberCoroutineScope()
    val state by toolbarStateFlow.collectAsState(initial = InkingState())

    val isTextActive by (viewModel?.isTextToolActive ?: kotlinx.coroutines.flow.MutableStateFlow(false)).collectAsState()

    ToolbarOverlay(
        state = state,
        onToolSelect = {
            viewModel?.setTextToolActive(false)
            engine.setTool(it)
        },
        isTextActive = isTextActive,
        onToggleText = {
            val next = !isTextActive
            viewModel?.setTextToolActive(next)
        },
        onInsertImage = { onInsertImage() },
        onEraserSizeSelect = { engine.setEraserSize(it) },
        onUndo = { engine.undo() },
        onRedo = { engine.redo() },
        onDeleteSelection = { engine.deleteSelectedStrokes() },
        onDuplicateSelection = { engine.duplicateSelectedStrokes() },
        onRecolorSelection = { engine.recolorSelectedStrokes(it) },
        onClearSelection = { engine.clearSelection() },
        onBeautifySelection = {
            scope.launch {
                viewModel?.beautifySelectedStrokesWithFont()
            }
        },
        onAlignBaseline = {
            scope.launch {
                viewModel?.alignSelectedStrokesToBaseline()
            }
        },
        onSendSelectionToGemini = onSendSelectionToGemini,
        onToggleTouchInking = {
            if (viewModel != null) {
                viewModel.toggleTouchInking()
            } else {
                engine.setTouchInkingEnabled(!state.isTouchInkingEnabled)
            }
        },
        modifier = modifier
    )
}



/**
 * Painel de Diagnósticos e Telemetria de Entrada.
 */
@Composable
fun DiagnosticsOverlay(
    state: InkingState,
    performance: InkPerformanceSnapshot,
    memoryStats: String,
    saveStatus: String?,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.widthIn(max = 400.dp),
        color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Bug,
                        contentDescription = null,
                        tint = LeNoteTheme.colors.accentPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Telemetria Jetpack Ink",
                        style = MaterialTheme.typography.titleSmall,
                        color = LeNoteTheme.colors.inkPrimary
                    )
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = "Fechar",
                        tint = LeNoteTheme.colors.inkSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Gesto: ${state.gestureState.name} | Stylus Eraser: ${if (state.isStylusButtonEraserActive) "ATIVO" else "OFF"}",
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkSecondary
            )
            Text(
                text = "ACTION: ${state.lastActionName} | ptrs: ${state.activePointerCount} | CANCELED: ${state.isFlagCanceled}",
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkSecondary
            )
            Text(
                text = "Decisão: ${state.lastDecisionSummary}",
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkPrimary
            )

            Text(
                text = String.format(
                    Locale.US,
                    "Input %d | P50/P95/P99 %.2f/%.2f/%.2f ms | intervalo P50 %.2f ms | históricos %d",
                    performance.inputSamples,
                    performance.inputP50Millis,
                    performance.inputP95Millis,
                    performance.inputP99Millis,
                    performance.eventIntervalP50Millis,
                    performance.historicalSamples
                ),
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkSecondary
            )
            Text(
                text = memoryStats,
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkSecondary
            )
            Text(
                text = String.format(
                    Locale.US,
                    "Frames %d | P50/P95/P99 %.2f/%.2f/%.2f ms | >16,67 ms %d",
                    performance.frameSamples,
                    performance.frameP50Millis,
                    performance.frameP95Millis,
                    performance.frameP99Millis,
                    performance.framesOverBudget
                ),
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkSecondary
            )

            if (state.pointerDiagnostics.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                state.pointerDiagnostics.forEach { ptr ->
                    Text(
                        text = String.format(
                            Locale.US,
                            "• [%d|%d] %s | P=%.2f tilt=%.2f ori=%.2f | (%.0f, %.0f) -> %s",
                            ptr.pointerIndex,
                            ptr.pointerId,
                            ptr.toolTypeName,
                            ptr.pressure,
                            ptr.tilt,
                            ptr.orientation,
                            ptr.x,
                            ptr.y,
                            ptr.decision
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = LeNoteTheme.colors.inkMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = String.format(
                    Locale.US,
                    "Traços: %d (Sel: %d) | Folha: %s | Zoom: %.2fx",
                    state.strokeCount,
                    state.selectedStrokeCount,
                    state.pageBackground.label,
                    state.zoomLevel
                ),
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkSecondary
            )

            if (!saveStatus.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Status: $saveStatus",
                    style = MaterialTheme.typography.labelSmall,
                    color = LeNoteTheme.colors.success
                )
            }
        }
    }
}

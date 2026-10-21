package com.example.lenote.ink

import android.view.MotionEvent
import android.view.View
import kotlinx.coroutines.flow.StateFlow

enum class PenStrokeSize(val label: String, val sizeWorld: Float) {
    FINE("Fina", 2.5f),
    MEDIUM("Média", 5.0f),
    THICK("Grossa", 9.0f),
    EXTRA_THICK("Extra", 14.0f)
}

enum class HighlighterSize(val label: String, val sizeWorld: Float) {
    NARROW("Fino", 16f),
    MEDIUM("Médio", 25f),
    WIDE("Largo", 38f)
}

sealed interface InkingTool {
    data class Pen(
        val colorInt: Int = 0xFF000000.toInt(),
        val size: Float = PenStrokeSize.MEDIUM.sizeWorld
    ) : InkingTool

    data class Highlighter(
        val colorInt: Int = 0x66FFFF00.toInt(),
        val size: Float = HighlighterSize.MEDIUM.sizeWorld
    ) : InkingTool

    data class Eraser(
        val eraserSize: EraserSize = EraserSize.MEDIUM
    ) : InkingTool {
        companion object {
            val Default = Eraser(EraserSize.MEDIUM)
        }
    }

    data object Lasso : InkingTool
}

/**
 * Representa a telemetria detalhada de um ponteiro individual em um MotionEvent.
 */
data class PointerTelemetry(
    val pointerId: Int,
    val pointerIndex: Int,
    val toolType: Int,
    val toolTypeName: String,
    val x: Float,
    val y: Float,
    val pressure: Float,
    val decision: String
)

/**
 * Estado atual da máquina de gestos e rejeição de palma/dedo.
 */
enum class GestureInputState {
    IDLE,
    SINGLE_FINGER_IGNORED,
    SINGLE_FINGER_PAN,
    TWO_FINGER_PAN_ZOOM,
    STYLUS_DRAWING,
    STYLUS_ERASING,
    STYLUS_LASSO_SELECTING,
    STYLUS_LASSO_MOVING,
    CANCELED
}

data class InkingState(
    val stylusPressure: Float = 0f,
    val lastPointerType: Int = MotionEvent.TOOL_TYPE_UNKNOWN,
    val lastCoordinates: Pair<Float, Float> = Pair(0f, 0f),
    val strokeCount: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val currentTool: InkingTool = InkingTool.Pen(),
    val eraserSize: EraserSize = EraserSize.MEDIUM,
    val isEraserIndicatorVisible: Boolean = false,
    val eraserIndicatorX: Float = 0f,
    val eraserIndicatorY: Float = 0f,
    val eraserIndicatorRadiusPx: Float = EraserSize.MEDIUM.worldRadius,
    val isHoverPreviewVisible: Boolean = false,
    val hoverPreviewX: Float = 0f,
    val hoverPreviewY: Float = 0f,
    val hoverPreviewRadiusPx: Float = 5f,
    val hoverPreviewColorInt: Int = 0xFF000000.toInt(),
    val isStylusButtonEraserActive: Boolean = false,
    val selectedStrokeCount: Int = 0,
    val selectionBoundsWorld: SelectionBoundsBox? = null,
    val pageBackground: PageBackground = PageBackground.BLANK,
    val smoothingLevel: SmoothingLevel = SmoothingLevel.LOW,
    val zoomLevel: Float = 1.0f,
    val panOffsetX: Float = 0f,
    val panOffsetY: Float = 0f,
    val contentVersion: Long = 0L,
    // Telemetria avançada para diagnóstico de Stylus, Palma e Dedo
    val lastActionName: String = "NONE",
    val activePointerCount: Int = 0,
    val isFlagCanceled: Boolean = false,
    val gestureState: GestureInputState = GestureInputState.IDLE,
    val pointerDiagnostics: List<PointerTelemetry> = emptyList(),
    val lastDecisionSummary: String = "Aguardando input..."
)

interface InkingEngine {
    val state: StateFlow<InkingState>

    fun attachView(view: View)
    fun detachView()
    fun onTouchEvent(event: MotionEvent): Boolean
    fun onHoverEvent(event: MotionEvent): Boolean
    fun setTool(tool: InkingTool)
    fun setEraserSize(size: EraserSize)
    fun setPageBackground(background: PageBackground)
    fun setSmoothingLevel(level: SmoothingLevel)
    fun setCameraTransform(zoom: Float, panX: Float, panY: Float)
    fun deleteSelectedStrokes()
    fun duplicateSelectedStrokes()
    fun recolorSelectedStrokes(newColorInt: Int)
    fun clearSelection()
    fun undo()
    fun redo()
    fun clear()
    fun saveStrokes(): ByteArray
    fun loadStrokes(data: ByteArray)
    fun zoomIn() {}
    fun zoomOut() {}
    fun resetZoom() {}
    fun fitToPage() {}
}

package com.example.lenote.ink



import android.graphics.Matrix

import android.graphics.RectF

import android.view.MotionEvent

import android.view.ScaleGestureDetector

import android.view.View

import androidx.ink.authoring.InProgressStrokeId

import androidx.ink.authoring.InProgressStrokesFinishedListener

import androidx.ink.authoring.InProgressStrokesView

import androidx.ink.brush.Brush

import androidx.ink.brush.StockBrushes

import androidx.ink.geometry.AffineTransform

import androidx.ink.geometry.Box

import androidx.ink.geometry.ImmutableBox

import androidx.ink.geometry.ImmutableVec

import androidx.ink.geometry.Intersection

import androidx.ink.storage.StrokeInputBatchSerialization

import androidx.ink.strokes.ImmutableStrokeInputBatch

import androidx.ink.strokes.Stroke

import kotlinx.coroutines.flow.MutableStateFlow

import kotlinx.coroutines.flow.StateFlow

import kotlinx.coroutines.flow.asStateFlow

import kotlinx.coroutines.flow.update

import java.io.ByteArrayInputStream

import java.io.ByteArrayOutputStream

import java.io.DataInputStream

import java.io.DataOutputStream

import kotlin.math.hypot



/**

 * Representa uma operação atômica no histórico de Undo/Redo.

 */

sealed interface InkHistoryOperation {

    data class AddStroke(val stroke: Stroke) : InkHistoryOperation

    data class EraseStrokes(

        val beforeStrokes: List<Stroke>,

        val afterStrokes: List<Stroke>

    ) : InkHistoryOperation

    data class TransformStrokes(

        val beforeStrokes: List<Stroke>,

        val afterStrokes: List<Stroke>

    ) : InkHistoryOperation

    data class ReplaceStroke(

        val originalStroke: Stroke,

        val snappedStroke: Stroke

    ) : InkHistoryOperation

}



/**

 * Métricas de profiling da borracha parcial para diagnóstico de performance em tempo real.

 */

data class EraserProfilingStats(

    val moveEventsProcessed: Int = 0,

    val totalStrokesExamined: Int = 0,

    val candidateStrokesPassedBBox: Int = 0,

    val strokesActuallyModified: Int = 0,

    val totalHitTestNanos: Long = 0L,

    val totalRebuildNanos: Long = 0L,

    val viewInvalidatesTriggered: Int = 0

)



class JetpackInkEngine : InkingEngine {



    private val _state = MutableStateFlow(InkingState())

    override val state: StateFlow<InkingState> = _state.asStateFlow()



    private var inProgressStrokesView: InProgressStrokesView? = null

    private var finishedStrokesView: FinishedStrokesView? = null



    // Coleção canônica de strokes finalizados

    private val finishedStrokes = mutableListOf<Stroke>()

    private var contentVersionCounter: Long = 0L



    // Pilhas ilimitadas de Undo/Redo baseadas em operações (desenho, borracha parcial/total e transformações de laço)

    private val undoStack = mutableListOf<InkHistoryOperation>()

    private val redoStack = mutableListOf<InkHistoryOperation>()



    // Snapshot LAZY do estado dos strokes no início de um gesto contínuo da borracha:

    // só é copiado na PRIMEIRA vez que um stroke realmente sofre modificação durante o gesto!

    private var currentEraserGestureInitialStrokes: List<Stroke>? = null

    private var currentEraserGestureModified: Boolean = false



    // Estruturas leves para corte incremental em tempo real durante ACTION_MOVE da borracha:

    // - gestureHiddenStrokes: conjunto (por identidade) de strokes originais já atingidos neste gesto

    // - gestureLiveSplitMap: mapeia cada stroke original atingido para suas LiveSampleChains sobreviventes atuais

    // Dessa forma, NENHUM objeto Stroke/PartitionedMesh nativo é reconstruído durante ACTION_MOVE!

    // Todos os Strokes resultantes são construídos UMA ÚNICA VEZ no ACTION_UP!

    private val gestureHiddenStrokes: MutableSet<Stroke> =

        java.util.Collections.newSetFromMap( java.util.IdentityHashMap() )

    private val gestureLiveSplitMap = java.util.IdentityHashMap<Stroke, List<LiveSampleChain>>()

    private val gestureFlatPreviewChains = ArrayList<LiveSampleChain>()



    // Mapeamento de ponteiros STYLUS ativos para o ID do stroke em progresso

    private val activeStrokeIds = mutableMapOf<Int, InProgressStrokeId>()



    // Ponteiros ativos de borracha (STYLUS/ERASER) e sua última posição conhecida em WORLD coordinates

    private val activeEraserPointerIds = mutableSetOf<Int>()

    private val lastEraserWorldPos = mutableMapOf<Int, Pair<Float, Float>>()



    // Estado da ferramenta de Seleção por Laço (Lasso)

    private val selectedStrokes: MutableSet<Stroke> =

        java.util.Collections.newSetFromMap(java.util.IdentityHashMap())

    private var currentSelectionBoundsWorld: SelectionBoundsBox? = null

    private val lassoPolygonWorldPoints = ArrayList<Pair<Float, Float>>()

    private var activeLassoPointerId: Int = -1

    private var isLassoMovingSelection: Boolean = false

    private var lassoMoveStartWorldX: Float = 0f

    private var lassoMoveStartWorldY: Float = 0f

    private var lassoMoveDeltaWorldX: Float = 0f

    private var lassoMoveDeltaWorldY: Float = 0f

    private val selectedStrokeInitialChains = ArrayList<Pair<Stroke, LiveSampleChain>>()



    // Array reutilizável para conversão Screen -> World sem alocar floatArrayOf no hot path

    private val scratchPoint = FloatArray(2)



    // Controle de throttling da telemetria Compose durante ACTION_MOVE contínuo (~15Hz)

    private var lastMoveTelemetryUptimeMs: Long = 0L



    // Métricas acumuladas do último gesto de borracha (para profiling e testes de regressão)

    var lastEraserProfilingStats: EraserProfilingStats = EraserProfilingStats()

        private set

    private var currentMoveEvents = 0

    private var currentStrokesExamined = 0

    private var currentCandidateStrokes = 0

    private var currentStrokesModified = 0

    private var currentHitTestNanos = 0L

    private var currentRebuildNanos = 0L

    private var currentInvalidates = 0



    // Transformação Mundo -> View (Tela) e sua inversa View (Tela) -> Mundo

    private val worldToViewMatrix = Matrix()

    private val viewToWorldMatrix = Matrix()

    private val identityMatrix = Matrix()



    private var currentScale = 1.0f

    private var translationX = 0f

    private var translationY = 0f



    // Estado de Pan/Zoom exclusivamente para 2 dedos (TOOL_TYPE_FINGER)

    private var isTwoFingerPanZoomActive = false

    private var lastPanCenterX = 0f

    private var lastPanCenterY = 0f

    private var lastFingerSpan = 0f



    // Mecânica Draw & Hold para Shape Snapping

    private val activeStrokePoints = mutableMapOf<Int, MutableList<Point2D>>()

    private val activeStrokeHoldTimers = mutableMapOf<Int, Long>()

    private val activeStrokeLastStationaryPos = mutableMapOf<Int, Point2D>()

    private val activeStrokeStationaryStartTime = mutableMapOf<Int, Long>()

    private val activeStrokeSnappedPending = mutableSetOf<Int>()

    var onShapeSnapped: (() -> Unit)? = null



    // Histórico de gestos de dedos para detecção de 2-finger tap (Undo) e 3-finger tap (Redo)

    private var multiFingerTapDownTime: Long = 0L

    private var multiFingerMaxCount: Int = 0

    private var multiFingerMovedTooFar: Boolean = false

    private var multiFingerInitialX: Float = 0f

    private var multiFingerInitialY: Float = 0f

    var onUndoGestureTriggered: (() -> Unit)? = null

    var onRedoGestureTriggered: (() -> Unit)? = null



    // Navegação fluida com 1 dedo (Single-finger pan vertical/horizontal)

    private var isSingleFingerPanActive = false

    private var singleFingerDownX = 0f

    private var singleFingerDownY = 0f

    private var singleFingerLastX = 0f

    private var singleFingerLastY = 0f

    private var isSingleFingerDragging = false



    // Famílias de pincéis padrão do Jetpack Ink

    private val penBrushFamily = StockBrushes.pressurePen()

    private val highlighterBrushFamily = StockBrushes.highlighter()



    // Manter compatibilidade opcional caso o ScaleGestureDetector ainda seja consultado

    private var scaleDetector: ScaleGestureDetector? = null



    private val strokesFinishedListener = object : InProgressStrokesFinishedListener {

        override fun onStrokesFinished(strokes: Map<InProgressStrokeId, Stroke>) {

            if (strokes.isEmpty()) return



            // CRÍTICO: Remove imediatamente os strokes do cache interno do InProgressStrokesView

            // para evitar renderização duplicada (InProgressStrokesView + FinishedStrokesView)

            // e evitar que strokes fiquem presos na camada do InProgressStrokesView impedindo o Undo/Eraser.

            inProgressStrokesView?.removeFinishedStrokes(strokes.keys)



            val currentSmoothing = _state.value.smoothingLevel

            for ((_, rawStroke) in strokes) {

                var strokeToInsert = StrokeSmoother.smoothStroke(rawStroke, currentSmoothing)

                var wasSnapped = false



                val snappedPointerId = activeStrokeSnappedPending.firstOrNull()

                if (snappedPointerId != null) {

                    activeStrokeSnappedPending.remove(snappedPointerId)

                    val rawPoints = activeStrokePoints.remove(snappedPointerId) ?: emptyList()

                    val shape = ShapeSnappingHelper.classifyShape(rawPoints)

                    if (shape != null) {

                        val snapped = ShapeSnappingHelper.createSnappedStroke(shape, strokeToInsert.brush)

                        undoStack.add(InkHistoryOperation.ReplaceStroke(originalStroke = strokeToInsert, snappedStroke = snapped))

                        strokeToInsert = snapped

                        wasSnapped = true

                    }

                }



                PartialStrokeEraser.prewarmStroke(strokeToInsert)

                finishedStrokes.add(strokeToInsert)

                if (!wasSnapped) {

                    undoStack.add(InkHistoryOperation.AddStroke(strokeToInsert))

                }

            }

            activeStrokePoints.clear()

            activeStrokeLastStationaryPos.clear()

            activeStrokeStationaryStartTime.clear()

            redoStack.clear()

            contentVersionCounter++

            syncStateAndRender()

        }

    }



    override fun attachView(view: View) {

        if (view is InProgressStrokesView) {

            view.removeFinishedStrokesListener(strokesFinishedListener)

            inProgressStrokesView = view

            view.addFinishedStrokesListener(strokesFinishedListener)

        }

        updateTransformMatrices()

    }



    fun attachFinishedView(view: FinishedStrokesView) {

        finishedStrokesView = view

        view.updateTransform(worldToViewMatrix)

        view.updatePageBackground(_state.value.pageBackground)

        val currentState = _state.value

        view.updateEraserIndicator(

            visible = currentState.isEraserIndicatorVisible,

            screenX = currentState.eraserIndicatorX,

            screenY = currentState.eraserIndicatorY,

            screenRadius = currentState.eraserIndicatorRadiusPx

        )

        view.updateStrokes(finishedStrokes)

        view.updateLassoOverlay(lassoPolygonWorldPoints, currentSelectionBoundsWorld)

    }



    override fun detachView() {

        inProgressStrokesView?.removeFinishedStrokesListener(strokesFinishedListener)

        inProgressStrokesView = null

        finishedStrokesView = null

        scaleDetector = null

        activeStrokeIds.clear()

        activeEraserPointerIds.clear()

        lastEraserWorldPos.clear()

        gestureHiddenStrokes.clear()

        gestureLiveSplitMap.clear()

        gestureFlatPreviewChains.clear()

        activeLassoPointerId = -1

        isLassoMovingSelection = false

        lassoPolygonWorldPoints.clear()

        isTwoFingerPanZoomActive = false

        hideEraserIndicator()

    }



    override fun setTool(tool: InkingTool) {

        if (tool !is InkingTool.Lasso && selectedStrokes.isNotEmpty()) {

            clearSelectionInternal()

        }

        val updatedEraserSize = if (tool is InkingTool.Eraser) tool.eraserSize else _state.value.eraserSize

        val keepIndicator = tool is InkingTool.Eraser && _state.value.isEraserIndicatorVisible

        val screenRadius = updatedEraserSize.worldRadius * currentScale



        _state.update {

            it.copy(

                currentTool = tool,

                eraserSize = updatedEraserSize,

                isEraserIndicatorVisible = keepIndicator,

                eraserIndicatorRadiusPx = screenRadius

            )

        }

        finishedStrokesView?.updateEraserIndicator(

            visible = keepIndicator,

            screenX = _state.value.eraserIndicatorX,

            screenY = _state.value.eraserIndicatorY,

            screenRadius = screenRadius

        )

    }



    override fun setEraserSize(size: EraserSize) {

        val currentTool = _state.value.currentTool

        val updatedTool = if (currentTool is InkingTool.Eraser) {

            InkingTool.Eraser(size)

        } else {

            currentTool

        }

        val screenRadius = size.worldRadius * currentScale

        _state.update {

            it.copy(

                currentTool = updatedTool,

                eraserSize = size,

                eraserIndicatorRadiusPx = screenRadius

            )

        }

        finishedStrokesView?.updateEraserIndicator(

            visible = _state.value.isEraserIndicatorVisible,

            screenX = _state.value.eraserIndicatorX,

            screenY = _state.value.eraserIndicatorY,

            screenRadius = screenRadius

        )

    }



    fun setPdfBackgroundBitmap(bitmap: android.graphics.Bitmap?) {
        finishedStrokesView?.setPdfBackgroundBitmap(bitmap)
    }

    override fun setPageBackground(background: PageBackground) {

        if (_state.value.pageBackground != background) {

            contentVersionCounter++

        }

        _state.update {

            it.copy(

                pageBackground = background,

                contentVersion = contentVersionCounter

            )

        }

        finishedStrokesView?.updatePageBackground(background)

    }



    override fun setSmoothingLevel(level: SmoothingLevel) {

        _state.update { it.copy(smoothingLevel = level) }

    }



    override fun setCameraTransform(zoom: Float, panX: Float, panY: Float) {

        currentScale = zoom.coerceIn(0.2f, 5.0f)

        translationX = panX

        translationY = panY

        updateTransformMatrices()

    }



    override fun zoomIn() {

        val viewW = finishedStrokesView?.width?.toFloat() ?: inProgressStrokesView?.width?.toFloat() ?: 0f

        val viewH = finishedStrokesView?.height?.toFloat() ?: inProgressStrokesView?.height?.toFloat() ?: 0f

        val cx = if (viewW > 0f) viewW / 2f else 600f

        val cy = if (viewH > 0f) viewH / 2f else 800f



        val newScale = (currentScale + 0.25f).coerceIn(0.2f, 5.0f)

        val effectiveFactor = newScale / currentScale

        currentScale = newScale



        translationX = cx - (cx - translationX) * effectiveFactor

        translationY = cy - (cy - translationY) * effectiveFactor

        applyPanBounds()

        updateTransformMatrices()

    }



    override fun zoomOut() {

        val viewW = finishedStrokesView?.width?.toFloat() ?: inProgressStrokesView?.width?.toFloat() ?: 0f

        val viewH = finishedStrokesView?.height?.toFloat() ?: inProgressStrokesView?.height?.toFloat() ?: 0f

        val cx = if (viewW > 0f) viewW / 2f else 600f

        val cy = if (viewH > 0f) viewH / 2f else 800f



        val newScale = (currentScale - 0.25f).coerceIn(0.2f, 5.0f)

        val effectiveFactor = newScale / currentScale

        currentScale = newScale



        translationX = cx - (cx - translationX) * effectiveFactor

        translationY = cy - (cy - translationY) * effectiveFactor

        applyPanBounds()

        updateTransformMatrices()

    }



    override fun resetZoom() {

        val viewW = finishedStrokesView?.width?.toFloat() ?: inProgressStrokesView?.width?.toFloat() ?: 0f

        val viewH = finishedStrokesView?.height?.toFloat() ?: inProgressStrokesView?.height?.toFloat() ?: 0f

        val cx = if (viewW > 0f) viewW / 2f else 600f

        val cy = if (viewH > 0f) viewH / 2f else 800f



        val newScale = 1.0f

        val effectiveFactor = newScale / currentScale

        currentScale = newScale



        translationX = cx - (cx - translationX) * effectiveFactor

        translationY = cy - (cy - translationY) * effectiveFactor

        applyPanBounds()

        updateTransformMatrices()

    }



    override fun fitToPage() {

        val viewW = finishedStrokesView?.width?.toFloat() ?: inProgressStrokesView?.width?.toFloat() ?: 0f

        val viewH = finishedStrokesView?.height?.toFloat() ?: inProgressStrokesView?.height?.toFloat() ?: 0f

        if (viewW <= 0f || viewH <= 0f) {

            resetZoom()

            return

        }

        val pageWidth = 1200f

        val pageHeight = 1600f

        val scaleX = (viewW - 80f) / pageWidth

        val scaleY = (viewH - 80f) / pageHeight

        val targetScale = minOf(scaleX, scaleY).coerceIn(0.2f, 3.0f)



        currentScale = targetScale

        translationX = (viewW - pageWidth * currentScale) / 2f

        translationY = (viewH - pageHeight * currentScale) / 2f

        applyPanBounds()

        updateTransformMatrices()

    }



    /**

     * Retorna uma cópia imutável dos strokes finalizados atualmente presentes no motor.

     */

    fun getFinishedStrokesSnapshot(): List<Stroke> = finishedStrokes.toList()
    fun getSelectedStrokesSnapshot(): List<Stroke> = selectedStrokes.toList()



    /**

     * Atualiza o indicador circular da borracha diretamente na [FinishedStrokesView] (caminho rápido)

     * e sincroniza o estado do indicador apenas quando a visibilidade/raio mudar ou para inspeção.

     */

    private fun showEraserIndicatorAtScreen(

        screenX: Float,

        screenY: Float,

        syncComposeState: Boolean = true

    ) {

        val screenRadius = _state.value.eraserSize.worldRadius * currentScale

        if (syncComposeState || !_state.value.isEraserIndicatorVisible) {

            _state.update {

                it.copy(

                    isEraserIndicatorVisible = true,

                    eraserIndicatorX = screenX,

                    eraserIndicatorY = screenY,

                    eraserIndicatorRadiusPx = screenRadius

                )

            }

        }

        finishedStrokesView?.updateEraserIndicator(

            visible = true,

            screenX = screenX,

            screenY = screenY,

            screenRadius = screenRadius

        )

    }



    private fun hideEraserIndicator() {

        val screenRadius = _state.value.eraserSize.worldRadius * currentScale

        _state.update {

            it.copy(

                isEraserIndicatorVisible = false,

                eraserIndicatorRadiusPx = screenRadius

            )

        }

        finishedStrokesView?.updateEraserIndicator(

            visible = false,

            screenX = _state.value.eraserIndicatorX,

            screenY = _state.value.eraserIndicatorY,

            screenRadius = screenRadius

        )

    }



    private fun isStylusButtonPressed(event: MotionEvent): Boolean {

        return (event.buttonState and MotionEvent.BUTTON_STYLUS_PRIMARY) != 0

    }



    fun setSearchHighlights(boxes: List<RectF>) {

        finishedStrokesView?.setSearchHighlights(boxes)

    }



    fun clearSearchHighlights() {

        finishedStrokesView?.clearSearchHighlights()

    }



    fun setDarkMode(isDark: Boolean) {

        finishedStrokesView?.setDarkMode(isDark)

    }



    private fun showHoverPreviewAtScreen(

        screenX: Float,

        screenY: Float,

        worldSize: Float,

        colorInt: Int

    ) {

        val radiusPx = (worldSize * currentScale * 0.5f).coerceAtLeast(3.0f)

        _state.update {

            it.copy(

                isHoverPreviewVisible = true,

                hoverPreviewX = screenX,

                hoverPreviewY = screenY,

                hoverPreviewRadiusPx = radiusPx,

                hoverPreviewColorInt = colorInt

            )

        }

        finishedStrokesView?.updateHoverPreview(

            visible = true,

            screenX = screenX,

            screenY = screenY,

            screenRadius = radiusPx,

            colorInt = colorInt

        )

    }



    private fun hideHoverPreview() {

        if (_state.value.isHoverPreviewVisible) {

            _state.update { it.copy(isHoverPreviewVisible = false) }

        }

        finishedStrokesView?.updateHoverPreview(

            visible = false,

            screenX = 0f,

            screenY = 0f,

            screenRadius = 0f,

            colorInt = 0

        )

    }



    override fun onHoverEvent(event: MotionEvent): Boolean {

        val actionIndex = event.actionIndex

        val toolType = event.getToolType(actionIndex)

        if (!isStylusToolType(toolType)) {

            return false

        }



        val stylusBtn = isStylusButtonPressed(event)

        val isEraserMode = _state.value.currentTool is InkingTool.Eraser ||

            toolType == MotionEvent.TOOL_TYPE_ERASER ||

            stylusBtn



        if (!isEraserMode) {

            if (_state.value.isEraserIndicatorVisible || _state.value.isStylusButtonEraserActive) {

                hideEraserIndicator()

                _state.update { it.copy(isStylusButtonEraserActive = false) }

            }



            // Stylus Hover Preview para Caneta e Marca-texto

            val currentTool = _state.value.currentTool

            val (toolSize, toolColor) = when (currentTool) {

                is InkingTool.Pen -> Pair(currentTool.size, currentTool.colorInt)

                is InkingTool.Highlighter -> Pair(currentTool.size, currentTool.colorInt)

                else -> Pair(0f, 0)

            }



            if (toolSize > 0f) {

                val hx = event.getX(actionIndex)

                val hy = event.getY(actionIndex)

                when (event.actionMasked) {

                    MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {

                        showHoverPreviewAtScreen(hx, hy, toolSize, toolColor)

                        return true

                    }

                    MotionEvent.ACTION_HOVER_EXIT -> {

                        hideHoverPreview()

                        return true

                    }

                }

            } else {

                hideHoverPreview()

            }

            return false

        } else {

            hideHoverPreview()

        }



        if (_state.value.isStylusButtonEraserActive != stylusBtn) {

            _state.update { it.copy(isStylusButtonEraserActive = stylusBtn) }

        }



        when (event.actionMasked) {

            MotionEvent.ACTION_HOVER_ENTER -> {

                showEraserIndicatorAtScreen(

                    screenX = event.getX(actionIndex),

                    screenY = event.getY(actionIndex),

                    syncComposeState = true

                )

                return true

            }

            MotionEvent.ACTION_HOVER_MOVE -> {

                showEraserIndicatorAtScreen(

                    screenX = event.getX(actionIndex),

                    screenY = event.getY(actionIndex),

                    syncComposeState = false

                )

                return true

            }

            MotionEvent.ACTION_HOVER_EXIT -> {

                hideEraserIndicator()

                if (_state.value.isStylusButtonEraserActive) {

                    _state.update { it.copy(isStylusButtonEraserActive = false) }

                }

                return true

            }

        }

        return false

    }



    private fun createBrushForCurrentTool(): Brush {

        return when (val tool = _state.value.currentTool) {

            is InkingTool.Pen -> {

                Brush.createWithColorIntArgb(penBrushFamily, tool.colorInt, tool.size, 0.1f)

            }

            is InkingTool.Highlighter -> {

                Brush.createWithColorIntArgb(highlighterBrushFamily, tool.colorInt, tool.size, 0.1f)

            }

            is InkingTool.Eraser, is InkingTool.Lasso -> {

                Brush.createWithColorIntArgb(penBrushFamily, 0xFF000000.toInt(), 10f, 0.1f)

            }

        }

    }



    /**

     * Verifica se o toolType corresponde a uma caneta Stylus autorizada a desenhar/apagar.

     * Dedos (TOOL_TYPE_FINGER) e palma nunca são autorizados a criar tinta.

     */

    fun isStylusToolType(toolType: Int): Boolean {

        return toolType == MotionEvent.TOOL_TYPE_STYLUS ||

            toolType == MotionEvent.TOOL_TYPE_ERASER

    }



    override fun onTouchEvent(event: MotionEvent): Boolean {

        val actionMasked = event.actionMasked

        val actionIndex = event.actionIndex

        val actionPointerId = event.getPointerId(actionIndex)

        val actionToolType = event.getToolType(actionIndex)

        val isFlagCanceled = (event.flags and MotionEvent.FLAG_CANCELED) != 0

        val stylusBtnPressed = isStylusButtonPressed(event)



        // Solicita unbuffered dispatch para menor latência na camada wet (InProgressStrokesView) ao desenhar.

        // Para a Borracha ou Laço (que renderizam na FinishedStrokesView sincronizada com VSYNC), mantemos o batching

        // nativo por frame do Choreographer para evitar múltiplos redraws por frame a 240Hz.

        val isEraserOrLassoAction = _state.value.currentTool is InkingTool.Eraser ||

            _state.value.currentTool is InkingTool.Lasso ||

            actionToolType == MotionEvent.TOOL_TYPE_ERASER ||

            stylusBtnPressed

        if (actionMasked == MotionEvent.ACTION_DOWN && isStylusToolType(actionToolType) && !isEraserOrLassoAction) {

            inProgressStrokesView?.requestUnbufferedDispatch(event)

        }



        // Inspeção de todos os ponteiros presentes neste evento

        var hasStylusInEvent = false

        var fingerCount = 0

        var fingerIdx0 = -1

        var fingerIdx1 = -1

        for (i in 0 until event.pointerCount) {

            val tType = event.getToolType(i)

            if (isStylusToolType(tType)) {

                hasStylusInEvent = true

            } else if (tType == MotionEvent.TOOL_TYPE_FINGER) {

                if (fingerCount == 0) fingerIdx0 = i

                else if (fingerCount == 1) fingerIdx1 = i

                fingerCount++

            }

        }



        val isStylusCurrentlyActive = hasStylusInEvent ||

            activeStrokeIds.isNotEmpty() ||

            activeEraserPointerIds.isNotEmpty() ||

            activeLassoPointerId != -1



        // 1. Tratamento prioritário de ACTION_CANCEL (ex: rejeição de palma pelo sistema Android)

        if (actionMasked == MotionEvent.ACTION_CANCEL) {

            cancelAllActiveStrokes(event)

            discardActiveEraserGesture()

            discardActiveLassoGesture()

            hideEraserIndicator()

            isTwoFingerPanZoomActive = false

            isSingleFingerPanActive = false

            isSingleFingerDragging = false

            updateTelemetry(

                event = event,

                gestureState = GestureInputState.CANCELED,

                summary = "ACTION_CANCEL (flagCanceled=$isFlagCanceled): inputs temporários descartados",

                forceUpdate = true

            )

            return true

        }



        // 2. Se a Stylus está ativa ou presente no evento, ela tem prioridade absoluta.

        // Qualquer Pan/Zoom de dedos é encerrado imediatamente sem cancelar o traço da Stylus.

        // Isso garante Palm Rejection total: a palma apoiada na tela jamais moverá a página enquanto a stylus atuar.

        if (isStylusCurrentlyActive) {

            if (isTwoFingerPanZoomActive) {

                isTwoFingerPanZoomActive = false

            }

            if (isSingleFingerPanActive) {

                isSingleFingerPanActive = false

                isSingleFingerDragging = false

            }

            return handleStylusPipeline(

                event = event,

                actionMasked = actionMasked,

                actionIndex = actionIndex,

                actionPointerId = actionPointerId,

                actionToolType = actionToolType,

                isFlagCanceled = isFlagCanceled

            )

        }



        // 3. Nenhum ponteiro Stylus ativo: rastrear contagem para multi-finger taps (2 dedos = Undo, 3 dedos = Redo)

        if (actionMasked == MotionEvent.ACTION_DOWN) {

            multiFingerTapDownTime = System.currentTimeMillis()

            multiFingerMaxCount = fingerCount

            multiFingerMovedTooFar = false

            multiFingerInitialX = event.x

            multiFingerInitialY = event.y

        } else if (actionMasked == MotionEvent.ACTION_POINTER_DOWN) {

            if (fingerCount > multiFingerMaxCount) {

                multiFingerMaxCount = fingerCount

            }

        }



        if (fingerCount >= 2) {

            isSingleFingerPanActive = false

            isSingleFingerDragging = false



            val x0 = event.getX(fingerIdx0)

            val y0 = event.getY(fingerIdx0)

            val x1 = event.getX(fingerIdx1)

            val y1 = event.getY(fingerIdx1)



            val centerX = (x0 + x1) / 2f

            val centerY = (y0 + y1) / 2f

            val span = hypot((x1 - x0).toDouble(), (y1 - y0).toDouble()).toFloat()



            when (actionMasked) {

                MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_DOWN -> {

                    isTwoFingerPanZoomActive = true

                    lastPanCenterX = centerX

                    lastPanCenterY = centerY

                    lastFingerSpan = span

                }

                MotionEvent.ACTION_MOVE -> {

                    if (hypot((centerX - multiFingerInitialX).toDouble(), (centerY - multiFingerInitialY).toDouble()) > 24.0) {

                        multiFingerMovedTooFar = true

                    }

                    if (!isTwoFingerPanZoomActive) {

                        isTwoFingerPanZoomActive = true

                        lastPanCenterX = centerX

                        lastPanCenterY = centerY

                        lastFingerSpan = span

                    } else {

                        // Calcula Pan com 2 dedos

                        val dx = centerX - lastPanCenterX

                        val dy = centerY - lastPanCenterY

                        translationX += dx

                        translationY += dy



                        // Calcula Pinch-to-Zoom ao redor do centro entre os 2 dedos

                        if (lastFingerSpan > 10f && span > 10f) {

                            val rawScaleFactor = span / lastFingerSpan

                            val newScale = (currentScale * rawScaleFactor).coerceIn(0.2f, 5.0f)

                            val effectiveFactor = newScale / currentScale

                            currentScale = newScale



                            translationX = centerX - (centerX - translationX) * effectiveFactor

                            translationY = centerY - (centerY - translationY) * effectiveFactor

                        }



                        lastPanCenterX = centerX

                        lastPanCenterY = centerY

                        lastFingerSpan = span

                        applyPanBounds()

                        updateTransformMatrices()

                    }

                }

                MotionEvent.ACTION_POINTER_UP -> {

                    isTwoFingerPanZoomActive = false

                    // Transição suave para o dedo restante se houver movimento contínuo

                    val remainingIdx = if (actionIndex == fingerIdx0) fingerIdx1 else fingerIdx0

                    if (remainingIdx in 0 until event.pointerCount) {

                        singleFingerLastX = event.getX(remainingIdx)

                        singleFingerLastY = event.getY(remainingIdx)

                        singleFingerDownX = singleFingerLastX

                        singleFingerDownY = singleFingerLastY

                        isSingleFingerPanActive = true

                        isSingleFingerDragging = false

                    }

                }

            }



            updateTelemetry(

                event = event,

                gestureState = GestureInputState.TWO_FINGER_PAN_ZOOM,

                summary = "Pan/Zoom com 2 dedos ativo (dedos não geram traço)",

                forceUpdate = actionMasked != MotionEvent.ACTION_MOVE

            )

            return true

        }



        // 4. Apenas 1 dedo (fingerCount == 1): Navegação fluida vertical/horizontal (FINGER = navegação, STYLUS = tinta)

        if (fingerCount == 1 && fingerIdx0 in 0 until event.pointerCount) {

            val fx = event.getX(fingerIdx0)

            val fy = event.getY(fingerIdx0)



            when (actionMasked) {

                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {

                    isTwoFingerPanZoomActive = false

                    isSingleFingerPanActive = true

                    isSingleFingerDragging = false

                    singleFingerDownX = fx

                    singleFingerDownY = fy

                    singleFingerLastX = fx

                    singleFingerLastY = fy

                }

                MotionEvent.ACTION_MOVE -> {

                    isTwoFingerPanZoomActive = false

                    if (!isSingleFingerPanActive) {

                        isSingleFingerPanActive = true

                        isSingleFingerDragging = false

                        singleFingerDownX = fx

                        singleFingerDownY = fy

                        singleFingerLastX = fx

                        singleFingerLastY = fy

                    } else {

                        val dx = fx - singleFingerLastX

                        val dy = fy - singleFingerLastY

                        if (!isSingleFingerDragging) {

                            val dist = hypot((fx - singleFingerDownX).toDouble(), (fy - singleFingerDownY).toDouble()).toFloat()

                            if (dist > 8f) { // Touch slop de 8px para evitar jitter ao apoiar a mão

                                isSingleFingerDragging = true

                            }

                        }

                        if (isSingleFingerDragging) {

                            translationX += dx

                            translationY += dy

                            applyPanBounds()

                            updateTransformMatrices()

                            singleFingerLastX = fx

                            singleFingerLastY = fy

                        }

                    }

                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {

                    isSingleFingerPanActive = false

                    isSingleFingerDragging = false

                    isTwoFingerPanZoomActive = false

                }

            }



            updateTelemetry(

                event = event,

                gestureState = if (isSingleFingerDragging) GestureInputState.SINGLE_FINGER_PAN else GestureInputState.SINGLE_FINGER_IGNORED,

                summary = if (isSingleFingerDragging) "Navegação por toque (1 dedo move a folha)" else "Toque de dedo (dedo não cria traço)",

                forceUpdate = actionMasked != MotionEvent.ACTION_MOVE

            )

            return true

        }



        // 5. Fim de gesto de toque: verificar se foi um multi-finger tap rápido (Undo com 2 dedos, Redo com 3 dedos)

        if (actionMasked == MotionEvent.ACTION_UP || actionMasked == MotionEvent.ACTION_CANCEL) {

            val tapDuration = System.currentTimeMillis() - multiFingerTapDownTime

            if (actionMasked == MotionEvent.ACTION_UP && !multiFingerMovedTooFar && tapDuration in 35..450) {

                if (multiFingerMaxCount == 2) {

                    undo()

                    onUndoGestureTriggered?.invoke()

                } else if (multiFingerMaxCount == 3) {

                    redo()

                    onRedoGestureTriggered?.invoke()

                }

            }

            isTwoFingerPanZoomActive = false

            isSingleFingerPanActive = false

            isSingleFingerDragging = false

            multiFingerMaxCount = 0

        } else if (actionMasked == MotionEvent.ACTION_POINTER_UP) {

            isTwoFingerPanZoomActive = false

        }



        updateTelemetry(

            event = event,

            gestureState = GestureInputState.IDLE,

            summary = "Gesto finalizado",

            forceUpdate = true

        )

        return true

    }



    /**

     * Mantém limites confortáveis de navegação para que o documento nunca seja perdido fora da tela.

     * O documento pode ser navegado de ponta a ponta na vertical e na horizontal.

     */

    private fun applyPanBounds() {

        val viewW = finishedStrokesView?.width?.toFloat() ?: inProgressStrokesView?.width?.toFloat() ?: 0f

        val viewH = finishedStrokesView?.height?.toFloat() ?: inProgressStrokesView?.height?.toFloat() ?: 0f

        if (viewW <= 0f || viewH <= 0f) return



        var docLeft = 0f

        var docTop = 0f

        var docRight = 1200f

        var docBottom = 1600f



        for (stroke in finishedStrokes) {

            val b = PartialStrokeEraser.getOrComputeBounds(stroke) ?: continue

            if (b.xMin < docLeft) docLeft = b.xMin

            if (b.yMin < docTop) docTop = b.yMin

            if (b.xMax > docRight) docRight = b.xMax

            if (b.yMax > docBottom) docBottom = b.yMax

        }



        val margin = 120f

        val minTransX = -(docRight * currentScale - margin)

        val maxTransX = viewW - (docLeft * currentScale + margin)

        val minTransY = -(docBottom * currentScale - margin)

        val maxTransY = viewH - (docTop * currentScale + margin)



        if (minTransX <= maxTransX) {

            translationX = translationX.coerceIn(minTransX, maxTransX)

        }

        if (minTransY <= maxTransY) {

            translationY = translationY.coerceIn(minTransY, maxTransY)

        }

    }



    private fun screenToWorldX(screenX: Float, screenY: Float): Float {

        scratchPoint[0] = screenX

        scratchPoint[1] = screenY

        viewToWorldMatrix.mapPoints(scratchPoint)

        return scratchPoint[0]

    }



    private fun handleStylusPipeline(

        event: MotionEvent,

        actionMasked: Int,

        actionIndex: Int,

        actionPointerId: Int,

        actionToolType: Int,

        isFlagCanceled: Boolean

    ): Boolean {

        val strokeView = inProgressStrokesView

        val stylusBtnPressed = isStylusButtonPressed(event)

        if (_state.value.isStylusButtonEraserActive != stylusBtnPressed) {

            _state.update { it.copy(isStylusButtonEraserActive = stylusBtnPressed) }

        }



        when (actionMasked) {

            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {

                if (!isStylusToolType(actionToolType)) {

                    updateTelemetry(

                        event = event,

                        gestureState = currentStylusGestureState(),

                        summary = "Ponteiro ID=$actionPointerId (${toolTypeToName(actionToolType)}) ignorado no ${actionToName(actionMasked)}",

                        forceUpdate = true

                    )

                    return true

                }



                val isEraserMode = _state.value.currentTool is InkingTool.Eraser ||

                    actionToolType == MotionEvent.TOOL_TYPE_ERASER ||

                    stylusBtnPressed



                val isLassoMode = !isEraserMode && _state.value.currentTool is InkingTool.Lasso



                if (isEraserMode) {

                    if (selectedStrokes.isNotEmpty()) {

                        clearSelectionInternal()

                    }

                    if (activeEraserPointerIds.isEmpty()) {

                        currentEraserGestureInitialStrokes = null

                        currentEraserGestureModified = false

                        gestureHiddenStrokes.clear()

                        gestureLiveSplitMap.clear()

                        gestureFlatPreviewChains.clear()

                        currentMoveEvents = 0

                        currentStrokesExamined = 0

                        currentCandidateStrokes = 0

                        currentStrokesModified = 0

                        currentHitTestNanos = 0L

                        currentRebuildNanos = 0L

                        currentInvalidates = 0

                    }

                    activeEraserPointerIds.add(actionPointerId)

                    val sx = event.getX(actionIndex)

                    val sy = event.getY(actionIndex)

                    val wx = screenToWorldX(sx, sy)

                    val wy = scratchPoint[1]

                    lastEraserWorldPos[actionPointerId] = Pair(wx, wy)



                    showEraserIndicatorAtScreen(sx, sy, syncComposeState = true)

                    eraseWorldSegmentLive(wx, wy, wx, wy)



                    updateTelemetry(

                        event = event,

                        gestureState = GestureInputState.STYLUS_ERASING,

                        summary = "Stylus ID=$actionPointerId iniciou Borracha (${_state.value.eraserSize.name}, btn=$stylusBtnPressed)",

                        forceUpdate = true

                    )

                } else if (isLassoMode) {

                    if (_state.value.isEraserIndicatorVisible) {

                        hideEraserIndicator()

                    }

                    activeLassoPointerId = actionPointerId

                    val sx = event.getX(actionIndex)

                    val sy = event.getY(actionIndex)

                    val wx = screenToWorldX(sx, sy)

                    val wy = scratchPoint[1]



                    val selBounds = currentSelectionBoundsWorld

                    if (selectedStrokes.isNotEmpty() && selBounds != null && selBounds.containsWorldPoint(wx, wy, 14f)) {

                        // Inicia arraste da seleção existente!

                        isLassoMovingSelection = true

                        lassoMoveStartWorldX = wx

                        lassoMoveStartWorldY = wy

                        lassoMoveDeltaWorldX = 0f

                        lassoMoveDeltaWorldY = 0f

                        lassoPolygonWorldPoints.clear()

                        selectedStrokeInitialChains.clear()

                        gestureHiddenStrokes.clear()

                        gestureFlatPreviewChains.clear()



                        for (s in selectedStrokes) {

                            gestureHiddenStrokes.add(s)

                            val chain = PartialStrokeEraser.strokeToInitialLiveChain(s, 0f)

                            if (chain != null) {

                                selectedStrokeInitialChains.add(Pair(s, chain))

                                gestureFlatPreviewChains.add(chain)

                            }

                        }

                        finishedStrokesView?.updateLiveEraserPreview(gestureHiddenStrokes, gestureFlatPreviewChains)

                        finishedStrokesView?.updateLassoOverlay(emptyList(), selBounds)

                        updateTelemetry(

                            event = event,

                            gestureState = GestureInputState.STYLUS_LASSO_MOVING,

                            summary = "Stylus iniciou translação de ${selectedStrokes.size} stroke(s) selecionados",

                            forceUpdate = true

                        )

                    } else {

                        // Inicia desenho de novo polígono de Laço

                        clearSelectionInternal()

                        isLassoMovingSelection = false

                        lassoPolygonWorldPoints.clear()

                        lassoPolygonWorldPoints.add(Pair(wx, wy))

                        finishedStrokesView?.updateLassoOverlay(lassoPolygonWorldPoints, null)

                        updateTelemetry(

                            event = event,

                            gestureState = GestureInputState.STYLUS_LASSO_SELECTING,

                            summary = "Stylus iniciou seleção por Laço",

                            forceUpdate = true

                        )

                    }

                } else {

                    if (_state.value.isEraserIndicatorVisible) {

                        hideEraserIndicator()

                    }

                    if (selectedStrokes.isNotEmpty()) {

                        clearSelectionInternal()

                    }

                    if (strokeView != null) {

                        val brush = createBrushForCurrentTool()

                        val strokeId = strokeView.startStroke(

                            event = event,

                            pointerId = actionPointerId,

                            brush = brush,

                            motionEventToWorldTransform = viewToWorldMatrix,

                            strokeToWorldTransform = identityMatrix

                        )

                        activeStrokeIds[actionPointerId] = strokeId

                        val sx = event.getX(actionIndex)

                        val sy = event.getY(actionIndex)

                        val wx = screenToWorldX(sx, sy)

                        val wy = scratchPoint[1]

                        val pt = Point2D(wx, wy)

                        activeStrokePoints[actionPointerId] = mutableListOf(pt)

                        val now = System.currentTimeMillis()

                        activeStrokeLastStationaryPos[actionPointerId] = pt

                        activeStrokeStationaryStartTime[actionPointerId] = now

                        activeStrokeSnappedPending.remove(actionPointerId)

                    }

                    updateTelemetry(

                        event = event,

                        gestureState = GestureInputState.STYLUS_DRAWING,

                        summary = "Stylus ID=$actionPointerId aceita para tinta (startStroke)",

                        forceUpdate = true

                    )

                }

                return true

            }



            MotionEvent.ACTION_MOVE -> {

                for (i in 0 until event.pointerCount) {

                    val pId = event.getPointerId(i)



                    val strokeId = activeStrokeIds[pId]

                    if (strokeId != null && strokeView != null) {

                        strokeView.addToStroke(event, pId, strokeId)

                        val sx = event.getX(i)

                        val sy = event.getY(i)

                        val wx = screenToWorldX(sx, sy)

                        val wy = scratchPoint[1]

                        val curPt = Point2D(wx, wy)

                        val pts = activeStrokePoints[pId]

                        if (pts != null) {

                            pts.add(curPt)

                            val lastStat = activeStrokeLastStationaryPos[pId] ?: curPt

                            val dist = hypot((curPt.x - lastStat.x).toDouble(), (curPt.y - lastStat.y).toDouble()).toFloat() * currentScale

                            val now = System.currentTimeMillis()

                            if (dist > 8f) {

                                activeStrokeLastStationaryPos[pId] = curPt

                                activeStrokeStationaryStartTime[pId] = now

                            } else {

                                val statStart = activeStrokeStationaryStartTime[pId] ?: now

                                if (now - statStart in 400L..5000L && !activeStrokeSnappedPending.contains(pId)) {

                                    activeStrokeSnappedPending.add(pId)

                                    onShapeSnapped?.invoke()

                                }

                            }

                        }

                    } else if (activeEraserPointerIds.contains(pId)) {

                        currentMoveEvents++

                        val sx = event.getX(i)

                        val sy = event.getY(i)



                        // A) Atualiza indicador visual da borracha diretamente na View (sem recompor Compose!)

                        showEraserIndicatorAtScreen(sx, sy, syncComposeState = false)



                        // B) Converte trajetória para WORLD coordinates e processa o segmento varrido de forma leve

                        val prevWorld = lastEraserWorldPos[pId]

                        val curWorldX = screenToWorldX(sx, sy)

                        val curWorldY = scratchPoint[1]



                        if (prevWorld != null) {

                            val historySize = event.historySize

                            if (historySize >= 2) {

                                val midH = historySize / 2

                                val midSx = event.getHistoricalX(i, midH)

                                val midSy = event.getHistoricalY(i, midH)

                                val midWx = screenToWorldX(midSx, midSy)

                                val midWy = scratchPoint[1]

                                val hit1 = eraseWorldSegmentLive(prevWorld.first, prevWorld.second, midWx, midWy, flushPreview = false)

                                val hit2 = eraseWorldSegmentLive(midWx, midWy, curWorldX, curWorldY, flushPreview = false)

                                if (hit1 || hit2) {

                                    flushLiveEraserPreviewToView()

                                }

                            } else {

                                eraseWorldSegmentLive(prevWorld.first, prevWorld.second, curWorldX, curWorldY, flushPreview = true)

                            }

                        } else {

                            eraseWorldSegmentLive(curWorldX, curWorldY, curWorldX, curWorldY, flushPreview = true)

                        }



                        lastEraserWorldPos[pId] = Pair(curWorldX, curWorldY)

                    } else if (pId == activeLassoPointerId) {

                        val sx = event.getX(i)

                        val sy = event.getY(i)

                        val wx = screenToWorldX(sx, sy)

                        val wy = scratchPoint[1]



                        if (isLassoMovingSelection) {

                            lassoMoveDeltaWorldX = wx - lassoMoveStartWorldX

                            lassoMoveDeltaWorldY = wy - lassoMoveStartWorldY

                            gestureFlatPreviewChains.clear()

                            for ((_, baseChain) in selectedStrokeInitialChains) {

                                val movedPts = baseChain.points.map { pt ->

                                    pt.copy(

                                        x = pt.x + lassoMoveDeltaWorldX,

                                        y = pt.y + lassoMoveDeltaWorldY

                                    )

                                }

                                val movedChain = PartialStrokeEraser.createLiveChain(

                                    baseChain.brush,

                                    baseChain.strokeHalfWidth,

                                    movedPts

                                )

                                if (movedChain != null) {

                                    gestureFlatPreviewChains.add(movedChain)

                                }

                            }

                            val baseBounds = currentSelectionBoundsWorld

                            val movedBounds = baseBounds?.offset(lassoMoveDeltaWorldX, lassoMoveDeltaWorldY)

                            finishedStrokesView?.updateLiveEraserPreview(gestureHiddenStrokes, gestureFlatPreviewChains)

                            finishedStrokesView?.updateLassoOverlay(emptyList(), movedBounds)

                        } else {

                            val lastPt = lassoPolygonWorldPoints.lastOrNull()

                            if (lastPt == null || hypot((wx - lastPt.first).toDouble(), (wy - lastPt.second).toDouble()) >= 3.0) {

                                lassoPolygonWorldPoints.add(Pair(wx, wy))

                                finishedStrokesView?.updateLassoOverlay(lassoPolygonWorldPoints, null)

                            }

                        }

                    }

                }



                updateTelemetry(

                    event = event,

                    gestureState = currentStylusGestureState(),

                    summary = "Stylus MOVE processado (${activeStrokeIds.size} stroke(s), dedos ignorados)",

                    forceUpdate = false

                )

                return true

            }



            MotionEvent.ACTION_POINTER_UP -> {

                val strokeId = activeStrokeIds.remove(actionPointerId)

                if (strokeId != null) {

                    if (isFlagCanceled) {

                        strokeView?.cancelStroke(strokeId, event)

                        updateTelemetry(

                            event = event,

                            gestureState = currentStylusGestureState(),

                            summary = "Ponteiro ID=$actionPointerId cancelado via FLAG_CANCELED em ACTION_POINTER_UP",

                            forceUpdate = true

                        )

                    } else {

                        strokeView?.finishStroke(event, actionPointerId, strokeId)

                        updateTelemetry(

                            event = event,

                            gestureState = currentStylusGestureState(),

                            summary = "Stylus ID=$actionPointerId finalizou stroke (ACTION_POINTER_UP)",

                            forceUpdate = true

                        )

                    }

                } else if (activeEraserPointerIds.remove(actionPointerId)) {

                    val prevWorld = lastEraserWorldPos.remove(actionPointerId)

                    if (isFlagCanceled) {

                        discardActiveEraserGesture()

                    } else {

                        val sx = event.getX(actionIndex)

                        val sy = event.getY(actionIndex)

                        val wx = screenToWorldX(sx, sy)

                        val wy = scratchPoint[1]

                        if (prevWorld != null) {

                            eraseWorldSegmentLive(prevWorld.first, prevWorld.second, wx, wy)

                        } else {

                            eraseWorldSegmentLive(wx, wy, wx, wy)

                        }

                        if (activeEraserPointerIds.isEmpty()) {

                            finalizeEraserGestureIfNeeded()

                        }

                    }

                    if (activeEraserPointerIds.isEmpty()) {

                        hideEraserIndicator()

                    }

                    updateTelemetry(

                        event = event,

                        gestureState = currentStylusGestureState(),

                        summary = "Stylus Borracha ID=$actionPointerId levantou (flagCanceled=$isFlagCanceled)",

                        forceUpdate = true

                    )

                } else if (actionPointerId == activeLassoPointerId) {

                    if (isFlagCanceled) {

                        discardActiveLassoGesture()

                    } else {

                        finalizeLassoGesture()

                    }

                    updateTelemetry(

                        event = event,

                        gestureState = currentStylusGestureState(),

                        summary = "Stylus Laço ID=$actionPointerId levantou",

                        forceUpdate = true

                    )

                } else {

                    updateTelemetry(

                        event = event,

                        gestureState = currentStylusGestureState(),

                        summary = "Dedo/Palma ID=$actionPointerId levantou (FLAG_CANCELED=$isFlagCanceled) — Stylus continua intacta",

                        forceUpdate = true

                    )

                }

                return true

            }



            MotionEvent.ACTION_UP -> {

                val strokeId = activeStrokeIds.remove(actionPointerId)

                if (strokeId != null) {

                    if (isFlagCanceled) {

                        strokeView?.cancelStroke(strokeId, event)

                    } else {

                        strokeView?.finishStroke(event, actionPointerId, strokeId)

                    }

                }

                if (activeStrokeIds.isNotEmpty()) {

                    for ((pId, orphanId) in activeStrokeIds) {

                        if (isFlagCanceled) {

                            strokeView?.cancelStroke(orphanId, event)

                        } else {

                            strokeView?.finishStroke(event, pId, orphanId)

                        }

                    }

                    activeStrokeIds.clear()

                }



                if (activeEraserPointerIds.remove(actionPointerId) || activeEraserPointerIds.isNotEmpty()) {

                    val prevWorld = lastEraserWorldPos.remove(actionPointerId)

                    activeEraserPointerIds.clear()

                    lastEraserWorldPos.clear()

                    if (isFlagCanceled) {

                        discardActiveEraserGesture()

                    } else {

                        val sx = event.getX(actionIndex)

                        val sy = event.getY(actionIndex)

                        val wx = screenToWorldX(sx, sy)

                        val wy = scratchPoint[1]

                        if (prevWorld != null) {

                            eraseWorldSegmentLive(prevWorld.first, prevWorld.second, wx, wy)

                        } else {

                            eraseWorldSegmentLive(wx, wy, wx, wy)

                        }

                        finalizeEraserGestureIfNeeded()

                    }

                    hideEraserIndicator()

                }



                if (activeLassoPointerId != -1) {

                    if (isFlagCanceled) {

                        discardActiveLassoGesture()

                    } else {

                        finalizeLassoGesture()

                    }

                }



                updateTelemetry(

                    event = event,

                    gestureState = GestureInputState.IDLE,

                    summary = "Stylus ACTION_UP concluído (flagCanceled=$isFlagCanceled)",

                    forceUpdate = true

                )

                return true

            }

        }



        return true

    }



    private fun currentStylusGestureState(): GestureInputState {

        return when {

            activeStrokeIds.isNotEmpty() -> GestureInputState.STYLUS_DRAWING

            activeEraserPointerIds.isNotEmpty() -> GestureInputState.STYLUS_ERASING

            activeLassoPointerId != -1 && isLassoMovingSelection -> GestureInputState.STYLUS_LASSO_MOVING

            activeLassoPointerId != -1 -> GestureInputState.STYLUS_LASSO_SELECTING

            else -> GestureInputState.IDLE

        }

    }



    private fun cancelAllActiveStrokes(event: MotionEvent) {

        val strokeView = inProgressStrokesView

        if (strokeView != null) {

            for ((_, strokeId) in activeStrokeIds) {

                strokeView.cancelStroke(strokeId, event)

            }

        }

        activeStrokeIds.clear()

    }



    private fun discardActiveEraserGesture() {

        finishedStrokesView?.clearLiveEraserPreview()

        gestureHiddenStrokes.clear()

        gestureLiveSplitMap.clear()

        gestureFlatPreviewChains.clear()

        currentEraserGestureInitialStrokes = null

        currentEraserGestureModified = false

        activeEraserPointerIds.clear()

        lastEraserWorldPos.clear()

    }



    private fun discardActiveLassoGesture() {

        activeLassoPointerId = -1

        isLassoMovingSelection = false

        lassoPolygonWorldPoints.clear()

        selectedStrokeInitialChains.clear()

        gestureHiddenStrokes.clear()

        gestureFlatPreviewChains.clear()

        finishedStrokesView?.clearLiveEraserPreview()

        finishedStrokesView?.updateLassoOverlay(emptyList(), currentSelectionBoundsWorld)

    }



    private fun finalizeLassoGesture() {

        activeLassoPointerId = -1

        if (isLassoMovingSelection) {

            isLassoMovingSelection = false

            val dx = lassoMoveDeltaWorldX

            val dy = lassoMoveDeltaWorldY

            selectedStrokeInitialChains.clear()

            gestureHiddenStrokes.clear()

            gestureFlatPreviewChains.clear()

            finishedStrokesView?.clearLiveEraserPreview()



            if (hypot(dx.toDouble(), dy.toDouble()) > 0.5 && selectedStrokes.isNotEmpty()) {

                val beforeSnapshot = ArrayList(finishedStrokes)

                val newSelectedSet: MutableSet<Stroke> =

                    java.util.Collections.newSetFromMap(java.util.IdentityHashMap())

                val updatedList = ArrayList<Stroke>(finishedStrokes.size)



                for (s in finishedStrokes) {

                    if (selectedStrokes.contains(s)) {

                        val moved = LassoSelectionHelper.translateStroke(s, dx, dy)

                        updatedList.add(moved)

                        newSelectedSet.add(moved)

                    } else {

                        updatedList.add(s)

                    }

                }



                finishedStrokes.clear()

                finishedStrokes.addAll(updatedList)

                selectedStrokes.clear()

                selectedStrokes.addAll(newSelectedSet)

                currentSelectionBoundsWorld = LassoSelectionHelper.computeCombinedBounds(selectedStrokes)



                undoStack.add(

                    InkHistoryOperation.TransformStrokes(

                        beforeStrokes = beforeSnapshot,

                        afterStrokes = ArrayList(finishedStrokes)

                    )

                )

                redoStack.clear()

                contentVersionCounter++

                syncStateAndRender()

            } else {

                finishedStrokesView?.updateLassoOverlay(emptyList(), currentSelectionBoundsWorld)

            }

        } else {

            val hits = LassoSelectionHelper.findStrokesInsidePolygon(finishedStrokes, lassoPolygonWorldPoints)

            lassoPolygonWorldPoints.clear()

            selectedStrokes.clear()

            selectedStrokes.addAll(hits)

            currentSelectionBoundsWorld = LassoSelectionHelper.computeCombinedBounds(selectedStrokes)

            finishedStrokesView?.updateLassoOverlay(emptyList(), currentSelectionBoundsWorld)

            _state.update {

                it.copy(

                    selectedStrokeCount = selectedStrokes.size,

                    selectionBoundsWorld = currentSelectionBoundsWorld

                )

            }

        }

    }



    private fun clearSelectionInternal() {

        selectedStrokes.clear()

        currentSelectionBoundsWorld = null

        lassoPolygonWorldPoints.clear()

        finishedStrokesView?.updateLassoOverlay(emptyList(), null)

        _state.update {

            it.copy(

                selectedStrokeCount = 0,

                selectionBoundsWorld = null

            )

        }

    }



    override fun clearSelection() {

        clearSelectionInternal()

    }



    override fun deleteSelectedStrokes() {

        if (selectedStrokes.isEmpty()) return

        val beforeSnapshot = ArrayList(finishedStrokes)

        finishedStrokes.removeAll { selectedStrokes.contains(it) }

        undoStack.add(

            InkHistoryOperation.TransformStrokes(

                beforeStrokes = beforeSnapshot,

                afterStrokes = ArrayList(finishedStrokes)

            )

        )

        redoStack.clear()

        contentVersionCounter++

        clearSelectionInternal()

        syncStateAndRender()

    }



    override fun duplicateSelectedStrokes() {

        duplicateSelectedStrokesByOffset(28f, 28f)

    }



    fun duplicateSelectedStrokesByOffset(offsetXWorld: Float, offsetYWorld: Float) {

        if (selectedStrokes.isEmpty()) return

        val beforeSnapshot = ArrayList(finishedStrokes)

        val duplicatedSet: MutableSet<Stroke> =

            java.util.Collections.newSetFromMap(java.util.IdentityHashMap())



        for (s in finishedStrokes.toList()) {

            if (selectedStrokes.contains(s)) {

                val copy = LassoSelectionHelper.translateStroke(s, offsetXWorld, offsetYWorld)

                finishedStrokes.add(copy)

                duplicatedSet.add(copy)

            }

        }



        selectedStrokes.clear()

        selectedStrokes.addAll(duplicatedSet)

        currentSelectionBoundsWorld = LassoSelectionHelper.computeCombinedBounds(selectedStrokes)



        undoStack.add(

            InkHistoryOperation.TransformStrokes(

                beforeStrokes = beforeSnapshot,

                afterStrokes = ArrayList(finishedStrokes)

            )

        )

        redoStack.clear()

        contentVersionCounter++

        syncStateAndRender()

    }



    override fun recolorSelectedStrokes(newColorInt: Int) {

        if (selectedStrokes.isEmpty()) return

        val beforeSnapshot = ArrayList(finishedStrokes)

        val newSelectedSet: MutableSet<Stroke> =

            java.util.Collections.newSetFromMap(java.util.IdentityHashMap())

        val updatedList = ArrayList<Stroke>(finishedStrokes.size)



        for (s in finishedStrokes) {

            if (selectedStrokes.contains(s)) {

                val recolored = LassoSelectionHelper.recolorStroke(s, newColorInt)

                updatedList.add(recolored)

                newSelectedSet.add(recolored)

            } else {

                updatedList.add(s)

            }

        }



        finishedStrokes.clear()

        finishedStrokes.addAll(updatedList)

        selectedStrokes.clear()

        selectedStrokes.addAll(newSelectedSet)

        currentSelectionBoundsWorld = LassoSelectionHelper.computeCombinedBounds(selectedStrokes)



        undoStack.add(

            InkHistoryOperation.TransformStrokes(

                beforeStrokes = beforeSnapshot,

                afterStrokes = ArrayList(finishedStrokes)

            )

        )

        redoStack.clear()

        contentVersionCounter++

        syncStateAndRender()

    }



    private fun updateTelemetry(

        event: MotionEvent,

        gestureState: GestureInputState,

        summary: String,

        forceUpdate: Boolean

    ) {

        // Durante ACTION_MOVE contínuo (forceUpdate == false), se o estado do gesto não mudou,

        // retorna imediatamente com ZERO alocações e ZERO recomposições do Jetpack Compose!

        if (!forceUpdate && _state.value.gestureState == gestureState) {

            return

        }

        lastMoveTelemetryUptimeMs = event.eventTime



        val actionIndex = event.actionIndex

        val primaryPressure = event.getPressure(actionIndex)

        val primaryToolType = event.getToolType(actionIndex)

        val primaryX = event.getX(actionIndex)

        val primaryY = event.getY(actionIndex)

        val isFlagCanceled = (event.flags and MotionEvent.FLAG_CANCELED) != 0



        val pointers = ArrayList<PointerTelemetry>(event.pointerCount)

        for (i in 0 until event.pointerCount) {

            val pId = event.getPointerId(i)

            val tType = event.getToolType(i)

            val decision = when {

                activeStrokeIds.containsKey(pId) -> "ACEITO (Tinta)"

                activeEraserPointerIds.contains(pId) -> "ACEITO (Borracha)"

                pId == activeLassoPointerId -> "ACEITO (Laço)"

                isStylusToolType(tType) -> "ACEITO (Stylus)"

                gestureState == GestureInputState.TWO_FINGER_PAN_ZOOM && tType == MotionEvent.TOOL_TYPE_FINGER -> "PAN/ZOOM (Ignorado p/ Tinta)"

                else -> "IGNORADO (Rejeição Dedo/Palma)"

            }

            pointers.add(

                PointerTelemetry(

                    pointerId = pId,

                    pointerIndex = i,

                    toolType = tType,

                    toolTypeName = toolTypeToName(tType),

                    x = event.getX(i),

                    y = event.getY(i),

                    pressure = event.getPressure(i),

                    decision = decision

                )

            )

        }



        _state.update {

            it.copy(

                stylusPressure = primaryPressure,

                lastPointerType = primaryToolType,

                lastCoordinates = Pair(primaryX, primaryY),

                lastActionName = actionToName(event.actionMasked),

                activePointerCount = event.pointerCount,

                isFlagCanceled = isFlagCanceled,

                gestureState = gestureState,

                pointerDiagnostics = pointers,

                lastDecisionSummary = summary

            )

        }

    }



    private fun actionToName(actionMasked: Int): String {

        return when (actionMasked) {

            MotionEvent.ACTION_DOWN -> "ACTION_DOWN"

            MotionEvent.ACTION_POINTER_DOWN -> "ACTION_POINTER_DOWN"

            MotionEvent.ACTION_MOVE -> "ACTION_MOVE"

            MotionEvent.ACTION_POINTER_UP -> "ACTION_POINTER_UP"

            MotionEvent.ACTION_UP -> "ACTION_UP"

            MotionEvent.ACTION_CANCEL -> "ACTION_CANCEL"

            else -> "ACTION_$actionMasked"

        }

    }



    private fun toolTypeToName(toolType: Int): String {

        return when (toolType) {

            MotionEvent.TOOL_TYPE_STYLUS -> "STYLUS"

            MotionEvent.TOOL_TYPE_ERASER -> "ERASER"

            MotionEvent.TOOL_TYPE_FINGER -> "FINGER"

            MotionEvent.TOOL_TYPE_MOUSE -> "MOUSE"

            else -> "UNKNOWN($toolType)"

        }

    }



    private fun flushLiveEraserPreviewToView() {

        gestureFlatPreviewChains.clear()

        for (chains in gestureLiveSplitMap.values) {

            gestureFlatPreviewChains.addAll(chains)

        }

        currentInvalidates++

        finishedStrokesView?.updateLiveEraserPreview(gestureHiddenStrokes, gestureFlatPreviewChains)

    }



    /**

     * Aplica o segmento varrido da borracha em coordenadas do mundo `(x0, y0) -> (x1, y1)`

     * em tempo real (`ACTION_DOWN` / `ACTION_MOVE`):

     *

     * - **NÃO reconstrói objetos `Stroke` nem `PartitionedMesh` durante o movimento!**

     * - Faz hit-testing espacial AABB O(1) contra os strokes ainda intocados e contra as

     *   `LiveSampleChain` dos strokes já atingidos no mesmo gesto.

     * - Se houver impacto, corta apenas as listas de pontos (`LiveSampleChain`) em memória (< 0.05ms)

     *   e atualiza o preview em `finishedStrokesView`.

     * - A materialização definitiva dos novos `Stroke` e o registro atômico no Undo/Redo ocorrem

     *   exclusivamente no `ACTION_UP` ([finalizeEraserGestureIfNeeded]).

     */

    private fun eraseWorldSegmentLive(

        x0: Float,

        y0: Float,

        x1: Float,

        y1: Float,

        flushPreview: Boolean = true

    ): Boolean {

        if (finishedStrokes.isEmpty()) return false



        val radiusInWorld = _state.value.eraserSize.worldRadius

        val tStartHit = System.nanoTime()



        val count = finishedStrokes.size

        currentStrokesExamined += count



        var anyHitInStep = false



        // 1. Verifica os strokes originais que já foram atingidos anteriormente neste mesmo gesto

        // (eles estão em gestureLiveSplitMap e possuem sub-cadeias vivas)

        if (gestureLiveSplitMap.isNotEmpty()) {

            for (entry in gestureLiveSplitMap.entries) {

                val chains = entry.value

                if (chains.isEmpty()) continue



                var entryModified = false

                var nextChainsForStroke: ArrayList<LiveSampleChain>? = null



                for (cIdx in chains.indices) {

                    val chain = chains[cIdx]

                    if (!PartialStrokeEraser.isChainCandidateForSegment(chain, x0, y0, x1, y1, radiusInWorld)) {

                        nextChainsForStroke?.add(chain)

                        continue

                    }



                    currentCandidateStrokes++

                    val (hit, clipped) = PartialStrokeEraser.clipLiveChainBySegment(

                        chain = chain,

                        x0 = x0,

                        y0 = y0,

                        x1 = x1,

                        y1 = y1,

                        eraserWorldRadius = radiusInWorld

                    )

                    if (hit) {

                        entryModified = true

                        if (nextChainsForStroke == null) {

                            nextChainsForStroke = ArrayList(chains.size + 2)

                            for (prev in 0 until cIdx) {

                                nextChainsForStroke.add(chains[prev])

                            }

                        }

                        nextChainsForStroke.addAll(clipped)

                    } else {

                        nextChainsForStroke?.add(chain)

                    }

                }



                if (entryModified && nextChainsForStroke != null) {

                    entry.setValue(nextChainsForStroke)

                    anyHitInStep = true

                }

            }

        }



        // 2. Verifica os strokes originais que ainda NÃO foram atingidos neste gesto

        for (i in 0 until count) {

            val stroke = finishedStrokes[i]

            if (gestureHiddenStrokes.contains(stroke)) continue



            if (!PartialStrokeEraser.isCandidateForSegment(stroke, x0, y0, x1, y1, radiusInWorld)) {

                continue

            }



            currentCandidateStrokes++

            val initialChain = PartialStrokeEraser.strokeToInitialLiveChain(stroke, radiusInWorld) ?: continue

            val (hit, survivingChains) = PartialStrokeEraser.clipLiveChainBySegment(

                chain = initialChain,

                x0 = x0,

                y0 = y0,

                x1 = x1,

                y1 = y1,

                eraserWorldRadius = radiusInWorld

            )



            if (hit) {

                if (!currentEraserGestureModified) {

                    currentEraserGestureInitialStrokes = ArrayList(finishedStrokes)

                    currentEraserGestureModified = true

                }

                gestureHiddenStrokes.add(stroke)

                gestureLiveSplitMap[stroke] = survivingChains

                currentStrokesModified++

                anyHitInStep = true

            }

        }



        currentHitTestNanos += (System.nanoTime() - tStartHit)



        if (anyHitInStep && flushPreview) {

            flushLiveEraserPreviewToView()

        }

        return anyHitInStep

    }



    /**

     * Chamado no `ACTION_UP` da borracha:

     * Materializa definitivamente apenas os strokes atingidos em novos objetos [Stroke] do Jetpack Ink,

     * atualiza `finishedStrokes`, cria UMA operação atômica de Undo e limpa o Redo.

     */

    private fun finalizeEraserGestureIfNeeded() {

        val initialSnapshot = currentEraserGestureInitialStrokes

        if (currentEraserGestureModified && initialSnapshot != null) {

            val tRebuildStart = System.nanoTime()

            val finalStrokes = ArrayList<Stroke>(finishedStrokes.size + gestureFlatPreviewChains.size)

            for (i in 0 until finishedStrokes.size) {

                val s = finishedStrokes[i]

                val splitChains = gestureLiveSplitMap[s]

                if (splitChains != null) {

                    finalStrokes.addAll(PartialStrokeEraser.materializeLiveChains(splitChains))

                } else {

                    finalStrokes.add(s)

                }

            }

            currentRebuildNanos += (System.nanoTime() - tRebuildStart)



            finishedStrokes.clear()

            finishedStrokes.addAll(finalStrokes)



            undoStack.add(

                InkHistoryOperation.EraseStrokes(

                    beforeStrokes = initialSnapshot,

                    afterStrokes = ArrayList(finishedStrokes)

                )

            )

            redoStack.clear()

            contentVersionCounter++

            finishedStrokesView?.clearLiveEraserPreview()

            syncStateAndRender()

        } else {

            finishedStrokesView?.clearLiveEraserPreview()

        }



        lastEraserProfilingStats = EraserProfilingStats(

            moveEventsProcessed = currentMoveEvents,

            totalStrokesExamined = currentStrokesExamined,

            candidateStrokesPassedBBox = currentCandidateStrokes,

            strokesActuallyModified = currentStrokesModified,

            totalHitTestNanos = currentHitTestNanos,

            totalRebuildNanos = currentRebuildNanos,

            viewInvalidatesTriggered = currentInvalidates

        )



        gestureHiddenStrokes.clear()

        gestureLiveSplitMap.clear()

        gestureFlatPreviewChains.clear()

        currentEraserGestureInitialStrokes = null

        currentEraserGestureModified = false

    }



    private fun updateTransformMatrices() {

        worldToViewMatrix.reset()

        worldToViewMatrix.postScale(currentScale, currentScale)

        worldToViewMatrix.postTranslate(translationX, translationY)



        worldToViewMatrix.invert(viewToWorldMatrix)



        finishedStrokesView?.updateTransform(worldToViewMatrix)

        val screenRadius = _state.value.eraserSize.worldRadius * currentScale

        finishedStrokesView?.updateEraserIndicator(

            visible = _state.value.isEraserIndicatorVisible,

            screenX = _state.value.eraserIndicatorX,

            screenY = _state.value.eraserIndicatorY,

            screenRadius = screenRadius

        )



        _state.update {

            it.copy(

                zoomLevel = currentScale,

                panOffsetX = translationX,

                panOffsetY = translationY,

                eraserIndicatorRadiusPx = screenRadius

            )

        }

    }



    override fun undo() {

        if (undoStack.isEmpty()) return

        clearSelectionInternal()



        when (val op = undoStack.removeAt(undoStack.lastIndex)) {

            is InkHistoryOperation.AddStroke -> {

                val idx = finishedStrokes.lastIndexOf(op.stroke)

                if (idx != -1) {

                    finishedStrokes.removeAt(idx)

                }

                redoStack.add(op)

            }

            is InkHistoryOperation.EraseStrokes -> {

                finishedStrokes.clear()

                finishedStrokes.addAll(op.beforeStrokes)

                redoStack.add(op)

            }

            is InkHistoryOperation.TransformStrokes -> {

                finishedStrokes.clear()

                finishedStrokes.addAll(op.beforeStrokes)

                redoStack.add(op)

            }

            is InkHistoryOperation.ReplaceStroke -> {

                val idx = finishedStrokes.indexOf(op.snappedStroke)

                if (idx != -1) {

                    finishedStrokes[idx] = op.originalStroke

                } else {

                    finishedStrokes.remove(op.snappedStroke)

                    finishedStrokes.add(op.originalStroke)

                }

                redoStack.add(op)

            }

        }



        contentVersionCounter++

        syncStateAndRender()

    }



    override fun redo() {

        if (redoStack.isEmpty()) return

        clearSelectionInternal()



        when (val op = redoStack.removeAt(redoStack.lastIndex)) {

            is InkHistoryOperation.AddStroke -> {

                finishedStrokes.add(op.stroke)

                undoStack.add(op)

            }

            is InkHistoryOperation.EraseStrokes -> {

                finishedStrokes.clear()

                finishedStrokes.addAll(op.afterStrokes)

                undoStack.add(op)

            }

            is InkHistoryOperation.TransformStrokes -> {

                finishedStrokes.clear()

                finishedStrokes.addAll(op.afterStrokes)

                undoStack.add(op)

            }

            is InkHistoryOperation.ReplaceStroke -> {

                val idx = finishedStrokes.indexOf(op.originalStroke)

                if (idx != -1) {

                    finishedStrokes[idx] = op.snappedStroke

                } else {

                    finishedStrokes.remove(op.originalStroke)

                    finishedStrokes.add(op.snappedStroke)

                }

                undoStack.add(op)

            }

        }



        contentVersionCounter++

        syncStateAndRender()

    }



    override fun clear() {

        if (finishedStrokes.isEmpty() && undoStack.isEmpty() && redoStack.isEmpty()) return

        inProgressStrokesView?.cancelUnfinishedStrokes()

        activeStrokeIds.clear()

        activeEraserPointerIds.clear()

        lastEraserWorldPos.clear()

        currentEraserGestureInitialStrokes = null

        currentEraserGestureModified = false

        clearSelectionInternal()

        finishedStrokes.clear()

        undoStack.clear()

        redoStack.clear()

        contentVersionCounter++

        syncStateAndRender()

    }



    private fun syncStateAndRender() {

        _state.update {

            it.copy(

                strokeCount = finishedStrokes.size,

                canUndo = undoStack.isNotEmpty(),

                canRedo = redoStack.isNotEmpty(),

                selectedStrokeCount = selectedStrokes.size,

                contentVersion = contentVersionCounter

            )

        }

        finishedStrokesView?.updateStrokes(finishedStrokes)

        finishedStrokesView?.updateLassoOverlay(lassoPolygonWorldPoints, currentSelectionBoundsWorld)

    }



    override fun saveStrokes(): ByteArray {

        val byteOut = ByteArrayOutputStream()

        val dataOut = DataOutputStream(byteOut)



        dataOut.writeInt(finishedStrokes.size)



        for (stroke in finishedStrokes) {

            val brush = stroke.brush

            val isHighlighter = brush.family == highlighterBrushFamily

            dataOut.writeBoolean(isHighlighter)

            dataOut.writeInt(brush.colorIntArgb)

            dataOut.writeFloat(brush.size)



            val inputBatch = stroke.inputs

            val batchBytesStream = ByteArrayOutputStream()

            StrokeInputBatchSerialization.encode(inputBatch, batchBytesStream)

            val batchBytes = batchBytesStream.toByteArray()



            dataOut.writeInt(batchBytes.size)

            dataOut.write(batchBytes)

        }



        dataOut.flush()

        return byteOut.toByteArray()

    }



    override fun loadStrokes(data: ByteArray) {

        if (data.isEmpty()) {

            clear()

            return

        }



        try {

            val byteIn = ByteArrayInputStream(data)

            val dataIn = DataInputStream(byteIn)



            val count = dataIn.readInt()

            val loadedStrokes = mutableListOf<Stroke>()

            val loadedHistory = mutableListOf<InkHistoryOperation>()



            for (i in 0 until count) {

                val isHighlighter = dataIn.readBoolean()

                val colorInt = dataIn.readInt()

                val size = dataIn.readFloat()



                val batchLength = dataIn.readInt()

                val batchBytes = ByteArray(batchLength)

                dataIn.readFully(batchBytes)



                val inputBatchStream = ByteArrayInputStream(batchBytes)

                val inputBatch: ImmutableStrokeInputBatch = StrokeInputBatchSerialization.decode(inputBatchStream)



                val family = if (isHighlighter) highlighterBrushFamily else penBrushFamily

                val brush = Brush.createWithColorIntArgb(family, colorInt, size, 0.1f)

                val stroke = Stroke(brush, inputBatch)

                PartialStrokeEraser.prewarmStroke(stroke)

                loadedStrokes.add(stroke)

                loadedHistory.add(InkHistoryOperation.AddStroke(stroke))

            }



            inProgressStrokesView?.cancelUnfinishedStrokes()

            activeStrokeIds.clear()

            activeEraserPointerIds.clear()

            currentEraserGestureInitialStrokes = null

            currentEraserGestureModified = false

            clearSelectionInternal()



            finishedStrokes.clear()

            finishedStrokes.addAll(loadedStrokes)



            undoStack.clear()

            undoStack.addAll(loadedHistory)

            redoStack.clear()



            contentVersionCounter++

            syncStateAndRender()

        } catch (e: Exception) {

            e.printStackTrace()

        }

    }

}


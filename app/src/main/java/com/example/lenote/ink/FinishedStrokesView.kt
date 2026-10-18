package com.example.lenote.ink

import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * View customizada responsável por renderizar:
 * 1. O padrão de folha ([PageBackground]) no mesmo sistema de coordenadas do mundo (world space)
 *    que os strokes, acompanhando Pan, Zoom e Rotação sem ser um [Stroke];
 * 2. Os strokes finalizados existentes na coleção atual do motor;
 * 3. O indicador visual circular da borracha (EraserIndicator) em coordenadas de tela (screen space),
 *    refletindo o raio efetivo de apagamento ajustado pelo Zoom sem gerar tinta nem persistência.
 */
class FinishedStrokesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val renderer: CanvasStrokeRenderer = CanvasStrokeRenderer.create(false)
    private val strokes = mutableListOf<Stroke>()
    private val worldToViewMatrix = Matrix()
    private val viewToWorldMatrix = Matrix()
    private val matrixValues = FloatArray(9)

    private var pageBackground: PageBackground = PageBackground.BLANK

    private var eraserIndicatorVisible: Boolean = false
    private var eraserIndicatorScreenX: Float = 0f
    private var eraserIndicatorScreenY: Float = 0f
    private var eraserIndicatorScreenRadius: Float = EraserSize.MEDIUM.worldRadius

    // Paints reutilizáveis para o PageBackground (evita alocações em onDraw)
    private val paperBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = 0xFFFCFCFA.toInt()
    }

    private val ruledLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFBBDEFB.toInt() // Azul suave clássico de caderno
    }

    private val marginLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFFFCDD2.toInt() // Margem vertical avermelhada suave
    }

    private val gridMinorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFE3EAF2.toInt()
    }

    private val gridMajorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFFB0C4DE.toInt()
    }

    private val mathAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF78909C.toInt()
    }

    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFB0BEC5.toInt()
    }

    // Paints reutilizáveis para o indicador circular da borracha
    private val eraserFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x28E53935 // Vermelho translúcido sutil
    }

    private val eraserStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xCCE53935.toInt()
        strokeWidth = 2.5f
    }

    private val eraserInnerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x99FFFFFF.toInt()
        strokeWidth = 1.5f
    }

    // Paints reutilizáveis para a ferramenta de Seleção por Laço (Lasso)
    private val lassoFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x1A1E88E5 // Azul translúcido suave
    }

    private val lassoStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xDD1E88E5.toInt()
        strokeWidth = 2.5f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }

    private val selectionBoxFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0x141565C0
    }

    private val selectionBoxStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xEE1565C0.toInt()
        strokeWidth = 2.2f
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 6f), 0f)
    }

    private val selectionHandlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFF1565C0.toInt()
    }

    private val lassoWorldPath = android.graphics.Path()
    private val lassoScreenPath = android.graphics.Path()
    private var hasLassoPolygon: Boolean = false
    private var selectionBoundsWorld: SelectionBoundsBox? = null
    private val selectionRectPts = FloatArray(4)

    // Estado opcional de preview em tempo real durante ACTION_MOVE da borracha parcial:
    // - hiddenStrokes: strokes originais que foram atingidos neste gesto (ocultos enquanto o gesto dura)
    // - liveChains: sub-cadeias sobreviventes cortadas em tempo real (sem construir Stroke/PartitionedMesh a cada frame!)
    private var hiddenPreviewStrokes: Set<Stroke>? = null
    private val livePreviewChains = mutableListOf<LiveSampleChain>()
    private val liveChainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    // Cache em Bitmap da camada estática (PageBackground + Strokes finalizados não ocultos):
    // Evita redesenhar centenas de Strokes via JNI e o fundo de folha a cada pixel movido pela borracha!
    private var staticCacheBitmap: android.graphics.Bitmap? = null
    private var staticCacheCanvas: Canvas? = null
    private var staticCacheDirty: Boolean = true
    private val cachedHiddenStrokes: MutableSet<Stroke> =
        java.util.Collections.newSetFromMap(java.util.IdentityHashMap())
    private var hasDirtyWorldRect: Boolean = false
    private var dirtyWorldMinX: Float = Float.POSITIVE_INFINITY
    private var dirtyWorldMinY: Float = Float.POSITIVE_INFINITY
    private var dirtyWorldMaxX: Float = Float.NEGATIVE_INFINITY
    private var dirtyWorldMaxY: Float = Float.NEGATIVE_INFINITY

    // Contadores de diagnóstico de performance de renderização
    var staticCacheRebuildCount: Int = 0
        private set
    var staticCachePartialPatchCount: Int = 0
        private set
    var fastBitmapBlitFrameCount: Int = 0
        private set

    // Array reutilizável em onDraw para calcular os limites visíveis do mundo sem alocações
    private val viewportCorners = FloatArray(4)

    private fun resetDirtyWorldRect() {
        hasDirtyWorldRect = false
        dirtyWorldMinX = Float.POSITIVE_INFINITY
        dirtyWorldMinY = Float.POSITIVE_INFINITY
        dirtyWorldMaxX = Float.NEGATIVE_INFINITY
        dirtyWorldMaxY = Float.NEGATIVE_INFINITY
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        staticCacheDirty = true
    }

    fun updateStrokes(newStrokes: List<Stroke>) {
        hiddenPreviewStrokes = null
        cachedHiddenStrokes.clear()
        resetDirtyWorldRect()
        livePreviewChains.clear()
        strokes.clear()
        strokes.addAll(newStrokes)
        staticCacheDirty = true
        invalidate()
    }

    internal fun updateLiveEraserPreview(
        hiddenStrokes: Set<Stroke>,
        survivingChains: List<LiveSampleChain>
    ) {
        hiddenPreviewStrokes = hiddenStrokes
        if (!staticCacheDirty && hiddenStrokes.size != cachedHiddenStrokes.size) {
            for (s in hiddenStrokes) {
                if (cachedHiddenStrokes.add(s)) {
                    val b = PartialStrokeEraser.getOrComputeBounds(s)
                    if (b != null) {
                        val pad = b.strokeHalfWidth + 6f
                        dirtyWorldMinX = min(dirtyWorldMinX, b.xMin - pad)
                        dirtyWorldMinY = min(dirtyWorldMinY, b.yMin - pad)
                        dirtyWorldMaxX = max(dirtyWorldMaxX, b.xMax + pad)
                        dirtyWorldMaxY = max(dirtyWorldMaxY, b.yMax + pad)
                        hasDirtyWorldRect = true
                    } else {
                        staticCacheDirty = true
                    }
                }
            }
        }
        livePreviewChains.clear()
        livePreviewChains.addAll(survivingChains)
        invalidate()
    }

    internal fun clearLiveEraserPreview() {
        if (hiddenPreviewStrokes != null || livePreviewChains.isNotEmpty()) {
            if (cachedHiddenStrokes.isNotEmpty()) {
                staticCacheDirty = true
            }
            hiddenPreviewStrokes = null
            cachedHiddenStrokes.clear()
            resetDirtyWorldRect()
            livePreviewChains.clear()
            invalidate()
        }
    }

    fun updateLassoOverlay(
        lassoPointsWorld: List<Pair<Float, Float>>,
        boundsWorld: SelectionBoundsBox?
    ) {
        lassoWorldPath.reset()
        if (lassoPointsWorld.size >= 2) {
            lassoWorldPath.moveTo(lassoPointsWorld[0].first, lassoPointsWorld[0].second)
            for (i in 1 until lassoPointsWorld.size) {
                lassoWorldPath.lineTo(lassoPointsWorld[i].first, lassoPointsWorld[i].second)
            }
            lassoWorldPath.close()
            hasLassoPolygon = true
        } else {
            hasLassoPolygon = false
        }
        selectionBoundsWorld = boundsWorld
        invalidate()
    }

    fun updateTransform(matrix: Matrix) {
        worldToViewMatrix.set(matrix)
        worldToViewMatrix.invert(viewToWorldMatrix)
        staticCacheDirty = true
        invalidate()
    }

    fun updatePageBackground(background: PageBackground) {
        if (pageBackground != background) {
            pageBackground = background
            staticCacheDirty = true
            invalidate()
        }
    }

    fun updateEraserIndicator(
        visible: Boolean,
        screenX: Float,
        screenY: Float,
        screenRadius: Float
    ) {
        val changed = eraserIndicatorVisible != visible ||
            eraserIndicatorScreenX != screenX ||
            eraserIndicatorScreenY != screenY ||
            eraserIndicatorScreenRadius != screenRadius

        eraserIndicatorVisible = visible
        eraserIndicatorScreenX = screenX
        eraserIndicatorScreenY = screenY
        eraserIndicatorScreenRadius = screenRadius

        if (changed) {
            invalidate()
        }
    }

    fun getRenderedStrokeCount(): Int {
        val hidden = hiddenPreviewStrokes
        return if (hidden == null || hidden.isEmpty()) {
            strokes.size
        } else {
            (strokes.size - hidden.size) + livePreviewChains.size
        }
    }

    fun getRenderedStrokes(): List<Stroke> = strokes.toList()

    fun getCurrentTransformMatrix(): Matrix = Matrix(worldToViewMatrix)

    fun getCurrentPageBackground(): PageBackground = pageBackground

    fun isEraserIndicatorShowing(): Boolean = eraserIndicatorVisible

    fun getEraserIndicatorScreenPosition(): Pair<Float, Float> =
        Pair(eraserIndicatorScreenX, eraserIndicatorScreenY)

    fun getEraserIndicatorScreenRadius(): Float = eraserIndicatorScreenRadius

    private fun renderStaticLayerToCanvas(targetCanvas: Canvas, viewW: Int, viewH: Int) {
        targetCanvas.drawRect(0f, 0f, viewW.toFloat(), viewH.toFloat(), paperBackgroundPaint)

        val hasValidViewport = viewW > 0 && viewH > 0
        var worldLeft = Float.NEGATIVE_INFINITY
        var worldTop = Float.NEGATIVE_INFINITY
        var worldRight = Float.POSITIVE_INFINITY
        var worldBottom = Float.POSITIVE_INFINITY

        if (hasValidViewport) {
            viewportCorners[0] = 0f
            viewportCorners[1] = 0f
            viewportCorners[2] = viewW.toFloat()
            viewportCorners[3] = viewH.toFloat()
            viewToWorldMatrix.mapPoints(viewportCorners)
            worldLeft = min(viewportCorners[0], viewportCorners[2])
            worldTop = min(viewportCorners[1], viewportCorners[3])
            worldRight = max(viewportCorners[0], viewportCorners[2])
            worldBottom = max(viewportCorners[1], viewportCorners[3])
        }

        val hidden = hiddenPreviewStrokes
        val saveCount = targetCanvas.save()
        try {
            targetCanvas.concat(worldToViewMatrix)
            drawPageBackgroundInWorldSpace(targetCanvas, worldLeft, worldTop, worldRight, worldBottom)

            for (i in 0 until strokes.size) {
                val stroke = strokes[i]
                if (hidden != null && hidden.contains(stroke)) {
                    continue
                }
                if (hasValidViewport) {
                    val b = PartialStrokeEraser.getOrComputeBounds(stroke)
                    if (b != null) {
                        val margin = b.strokeHalfWidth + 4f
                        if (b.xMax + margin < worldLeft ||
                            b.xMin - margin > worldRight ||
                            b.yMax + margin < worldTop ||
                            b.yMin - margin > worldBottom
                        ) {
                            continue
                        }
                    }
                }
                renderer.draw(targetCanvas, stroke, worldToViewMatrix)
            }
        } finally {
            targetCanvas.restoreToCount(saveCount)
        }
    }

    /**
     * Atualiza incrementalmente APENAS a sub-região (`dirtyWorldRect`) do `staticCacheBitmap`
     * correspondente ao(s) stroke(s) recém-atingido(s) pela borracha, evitando redesenhar a tela
     * inteira e os demais strokes distantes durante o movimento contínuo da borracha!
     */
    private fun patchStaticLayerDirtyWorldRect(
        targetCanvas: Canvas,
        viewW: Int,
        viewH: Int,
        wLeft: Float,
        wTop: Float,
        wRight: Float,
        wBottom: Float
    ) {
        viewportCorners[0] = wLeft
        viewportCorners[1] = wTop
        viewportCorners[2] = wRight
        viewportCorners[3] = wBottom
        worldToViewMatrix.mapPoints(viewportCorners)

        val sLeft = floor(min(viewportCorners[0], viewportCorners[2])).coerceIn(0f, viewW.toFloat())
        val sTop = floor(min(viewportCorners[1], viewportCorners[3])).coerceIn(0f, viewH.toFloat())
        val sRight = ceil(max(viewportCorners[0], viewportCorners[2])).coerceIn(0f, viewW.toFloat())
        val sBottom = ceil(max(viewportCorners[1], viewportCorners[3])).coerceIn(0f, viewH.toFloat())

        if (sRight <= sLeft || sBottom <= sTop) return

        val hidden = hiddenPreviewStrokes
        val saveCount = targetCanvas.save()
        try {
            targetCanvas.clipRect(sLeft, sTop, sRight, sBottom)
            targetCanvas.drawRect(sLeft, sTop, sRight, sBottom, paperBackgroundPaint)
            targetCanvas.concat(worldToViewMatrix)

            drawPageBackgroundInWorldSpace(targetCanvas, wLeft, wTop, wRight, wBottom)

            for (i in 0 until strokes.size) {
                val stroke = strokes[i]
                if (hidden != null && hidden.contains(stroke)) {
                    continue
                }
                val b = PartialStrokeEraser.getOrComputeBounds(stroke)
                if (b != null) {
                    val margin = b.strokeHalfWidth + 4f
                    if (b.xMax + margin < wLeft ||
                        b.xMin - margin > wRight ||
                        b.yMax + margin < wTop ||
                        b.yMin - margin > wBottom
                    ) {
                        continue
                    }
                }
                renderer.draw(targetCanvas, stroke, worldToViewMatrix)
            }
        } finally {
            targetCanvas.restoreToCount(saveCount)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewW = width
        val viewH = height

        // 1. Camada Estática (PageBackground + Strokes finalizados não atingidos) via Bitmap Cache
        if (viewW > 0 && viewH > 0) {
            var bmp = staticCacheBitmap
            var bmpCanvas = staticCacheCanvas
            if (bmp == null || bmp.width != viewW || bmp.height != viewH) {
                bmp?.recycle()
                bmp = android.graphics.Bitmap.createBitmap(viewW, viewH, android.graphics.Bitmap.Config.ARGB_8888)
                bmpCanvas = Canvas(bmp)
                staticCacheBitmap = bmp
                staticCacheCanvas = bmpCanvas
                staticCacheDirty = true
            }

            if (staticCacheDirty && bmpCanvas != null) {
                renderStaticLayerToCanvas(bmpCanvas, viewW, viewH)
                cachedHiddenStrokes.clear()
                hiddenPreviewStrokes?.let { cachedHiddenStrokes.addAll(it) }
                resetDirtyWorldRect()
                staticCacheDirty = false
                staticCacheRebuildCount++
            } else if (hasDirtyWorldRect && bmpCanvas != null) {
                patchStaticLayerDirtyWorldRect(
                    bmpCanvas,
                    viewW,
                    viewH,
                    dirtyWorldMinX,
                    dirtyWorldMinY,
                    dirtyWorldMaxX,
                    dirtyWorldMaxY
                )
                resetDirtyWorldRect()
                staticCachePartialPatchCount++
                fastBitmapBlitFrameCount++
            } else {
                fastBitmapBlitFrameCount++
            }

            canvas.drawBitmap(bmp, 0f, 0f, null)
        } else {
            renderStaticLayerToCanvas(canvas, canvas.width, canvas.height)
        }

        // 2. Camada Dinâmica do Gesto da Borracha (apenas os fragmentos dos strokes em corte ativo)
        if (livePreviewChains.isNotEmpty()) {
            val worldSaveCount = canvas.save()
            try {
                canvas.concat(worldToViewMatrix)
                for (i in 0 until livePreviewChains.size) {
                    val chain = livePreviewChains[i]
                    if (chain.points.size < 2) continue
                    liveChainPaint.color = chain.brush.colorIntArgb
                    liveChainPaint.strokeWidth = chain.brush.size
                    canvas.drawPath(chain.renderPath, liveChainPaint)
                }
            } finally {
                canvas.restoreToCount(worldSaveCount)
            }
        }

        // 3. Camada de Seleção por Laço (Lasso Polygon + Bounding Box de Strokes Selecionados)
        if (hasLassoPolygon) {
            lassoWorldPath.transform(worldToViewMatrix, lassoScreenPath)
            canvas.drawPath(lassoScreenPath, lassoFillPaint)
            canvas.drawPath(lassoScreenPath, lassoStrokePaint)
        }

        val selBounds = selectionBoundsWorld
        if (selBounds != null) {
            selectionRectPts[0] = selBounds.xMin - 6f
            selectionRectPts[1] = selBounds.yMin - 6f
            selectionRectPts[2] = selBounds.xMax + 6f
            selectionRectPts[3] = selBounds.yMax + 6f
            worldToViewMatrix.mapPoints(selectionRectPts)

            val left = min(selectionRectPts[0], selectionRectPts[2])
            val top = min(selectionRectPts[1], selectionRectPts[3])
            val right = max(selectionRectPts[0], selectionRectPts[2])
            val bottom = max(selectionRectPts[1], selectionRectPts[3])

            canvas.drawRect(left, top, right, bottom, selectionBoxFillPaint)
            canvas.drawRect(left, top, right, bottom, selectionBoxStrokePaint)

            val handleR = 5.5f
            canvas.drawCircle(left, top, handleR, selectionHandlePaint)
            canvas.drawCircle(right, top, handleR, selectionHandlePaint)
            canvas.drawCircle(left, bottom, handleR, selectionHandlePaint)
            canvas.drawCircle(right, bottom, handleR, selectionHandlePaint)
        }

        // 4. Camada Screen Space: Indicador visual circular da Borracha (acima dos traços, sem virar Stroke)
        if (eraserIndicatorVisible && eraserIndicatorScreenRadius > 0f) {
            canvas.drawCircle(
                eraserIndicatorScreenX,
                eraserIndicatorScreenY,
                eraserIndicatorScreenRadius,
                eraserFillPaint
            )
            if (eraserIndicatorScreenRadius > 2f) {
                canvas.drawCircle(
                    eraserIndicatorScreenX,
                    eraserIndicatorScreenY,
                    eraserIndicatorScreenRadius - 1.2f,
                    eraserInnerStrokePaint
                )
            }
            canvas.drawCircle(
                eraserIndicatorScreenX,
                eraserIndicatorScreenY,
                eraserIndicatorScreenRadius,
                eraserStrokePaint
            )
        }
    }

    private fun drawPageBackgroundInWorldSpace(
        canvas: Canvas,
        worldLeft: Float,
        worldTop: Float,
        worldRight: Float,
        worldBottom: Float
    ) {
        if (pageBackground == PageBackground.BLANK || width <= 0 || height <= 0) {
            return
        }

        worldToViewMatrix.getValues(matrixValues)
        val scale = max(0.1f, matrixValues[Matrix.MSCALE_X])

        // Mantém espessura nítida na tela independentemente do zoom
        val thinStrokeWorld = 1.2f / scale
        val mediumStrokeWorld = 2.0f / scale
        val axisStrokeWorld = 2.6f / scale

        when (pageBackground) {
            PageBackground.BLANK -> Unit

            PageBackground.RULED -> {
                val lineSpacing = 48f
                ruledLinePaint.strokeWidth = thinStrokeWorld
                marginLinePaint.strokeWidth = mediumStrokeWorld

                val startY = floor(worldTop / lineSpacing) * lineSpacing
                var y = startY
                while (y <= worldBottom) {
                    canvas.drawLine(worldLeft, y, worldRight, y, ruledLinePaint)
                    y += lineSpacing
                }

                // Linha de margem vertical fixa no espaço do mundo (x = 96f)
                val marginX = 96f
                if (marginX in worldLeft..worldRight) {
                    canvas.drawLine(marginX, worldTop, marginX, worldBottom, marginLinePaint)
                }
            }

            PageBackground.GRID -> {
                val gridStep = 40f
                gridMinorPaint.strokeWidth = thinStrokeWorld

                val startX = floor(worldLeft / gridStep) * gridStep
                var x = startX
                while (x <= worldRight) {
                    canvas.drawLine(x, worldTop, x, worldBottom, gridMinorPaint)
                    x += gridStep
                }

                val startY = floor(worldTop / gridStep) * gridStep
                var y = startY
                while (y <= worldBottom) {
                    canvas.drawLine(worldLeft, y, worldRight, y, gridMinorPaint)
                    y += gridStep
                }
            }

            PageBackground.DOTS -> {
                val dotStep = 40f
                val dotRadiusWorld = (2.2f / scale).coerceIn(0.8f, 3.5f)

                val startX = floor(worldLeft / dotStep) * dotStep
                val startY = floor(worldTop / dotStep) * dotStep

                var x = startX
                while (x <= worldRight) {
                    var y = startY
                    while (y <= worldBottom) {
                        canvas.drawCircle(x, y, dotRadiusWorld, dotPaint)
                        y += dotStep
                    }
                    x += dotStep
                }
            }

            PageBackground.MATHEMATICS -> {
                // Papel milimetrado / matemático: subdivisões finas (24f), linhas principais a cada 5 blocos (120f) e eixos (0,0)
                val minorStep = 24f
                val majorMultiple = 5
                gridMinorPaint.strokeWidth = thinStrokeWorld
                gridMajorPaint.strokeWidth = mediumStrokeWorld
                mathAxisPaint.strokeWidth = axisStrokeWorld

                val firstCol = floor(worldLeft / minorStep).toInt()
                val lastCol = ceil(worldRight / minorStep).toInt()
                for (col in firstCol..lastCol) {
                    val x = col * minorStep
                    val paint = when {
                        col == 0 -> mathAxisPaint
                        col % majorMultiple == 0 -> gridMajorPaint
                        else -> gridMinorPaint
                    }
                    canvas.drawLine(x, worldTop, x, worldBottom, paint)
                }

                val firstRow = floor(worldTop / minorStep).toInt()
                val lastRow = ceil(worldBottom / minorStep).toInt()
                for (row in firstRow..lastRow) {
                    val y = row * minorStep
                    val paint = when {
                        row == 0 -> mathAxisPaint
                        row % majorMultiple == 0 -> gridMajorPaint
                        else -> gridMinorPaint
                    }
                    canvas.drawLine(worldLeft, y, worldRight, y, paint)
                }
            }
        }
    }
}

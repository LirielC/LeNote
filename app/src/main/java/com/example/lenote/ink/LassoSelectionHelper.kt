package com.example.lenote.ink

import androidx.ink.brush.Brush
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import kotlin.math.max
import kotlin.math.min

/**
 * Caixa delimitadora em coordenadas do mundo (world space) para o conjunto de strokes
 * selecionados pela ferramenta de Laço ([InkingTool.Lasso]).
 */
data class SelectionBoundsBox(
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float
) {
    fun containsWorldPoint(x: Float, y: Float, padding: Float = 12f): Boolean {
        return x >= (xMin - padding) && x <= (xMax + padding) &&
            y >= (yMin - padding) && y <= (yMax + padding)
    }

    fun offset(dx: Float, dy: Float): SelectionBoundsBox {
        return SelectionBoundsBox(
            xMin = xMin + dx,
            yMin = yMin + dy,
            xMax = xMax + dx,
            yMax = yMax + dy
        )
    }
}

/**
 * Utilitário geométrico para a Ferramenta de Seleção por Laço (**Lasso Tool**):
 * - Seleção de strokes contidos em polígono fechado (AABB pre-filter + Ray-Casting);
 * - Movimentação (translação) em lote preservando atributos físicos da Stylus;
 * - Troca de cor em lote.
 */
object LassoSelectionHelper {

    /**
     * Identifica todos os [Stroke]s de [strokes] que estão parcial ou totalmente dentro do polígono
     * fechado [polygonWorldPoints] (pelo menos 1 ponto amostrado dentro do polígono, ou >30% para evitar
     * toques acidentais de borda).
     */
    fun findStrokesInsidePolygon(
        strokes: List<Stroke>,
        polygonWorldPoints: List<Pair<Float, Float>>
    ): List<Stroke> {
        if (strokes.isEmpty() || polygonWorldPoints.size < 3) return emptyList()

        var polyMinX = Float.POSITIVE_INFINITY
        var polyMinY = Float.POSITIVE_INFINITY
        var polyMaxX = Float.NEGATIVE_INFINITY
        var polyMaxY = Float.NEGATIVE_INFINITY

        for (i in polygonWorldPoints.indices) {
            val pt = polygonWorldPoints[i]
            if (pt.first < polyMinX) polyMinX = pt.first
            if (pt.second < polyMinY) polyMinY = pt.second
            if (pt.first > polyMaxX) polyMaxX = pt.first
            if (pt.second > polyMaxY) polyMaxY = pt.second
        }

        val selected = ArrayList<Stroke>()
        val scratch = StrokeInput()

        for (sIdx in strokes.indices) {
            val stroke = strokes[sIdx]
            val b = PartialStrokeEraser.getOrComputeBounds(stroke)
            if (b != null) {
                if (b.xMax < polyMinX || b.xMin > polyMaxX || b.yMax < polyMinY || b.yMin > polyMaxY) {
                    continue
                }
            }

            val batch = stroke.inputs
            val totalPts = batch.size
            if (totalPts == 0) continue

            var insideCount = 0
            for (i in 0 until totalPts) {
                batch.populate(i, scratch)
                val px = scratch.x
                val py = scratch.y
                if (px in polyMinX..polyMaxX && py in polyMinY..polyMaxY) {
                    if (isPointInPolygon(px, py, polygonWorldPoints)) {
                        insideCount++
                    }
                }
            }

            // Seleciona o stroke se ao menos metade de seus pontos ou pelo menos 2 pontos estiverem dentro do laço
            val requiredInside = min(2, totalPts)
            if (insideCount >= requiredInside) {
                selected.add(stroke)
            }
        }

        return selected
    }

    /**
     * Teste clássico de Ponto-no-Polígono via Ray-Casting horizontal.
     */
    fun isPointInPolygon(
        px: Float,
        py: Float,
        polygon: List<Pair<Float, Float>>
    ): Boolean {
        val n = polygon.size
        if (n < 3) return false
        var inside = false
        var j = n - 1
        for (i in 0 until n) {
            val xi = polygon[i].first
            val yi = polygon[i].second
            val xj = polygon[j].first
            val yj = polygon[j].second

            val intersects = ((yi > py) != (yj > py)) &&
                (px < (xj - xi) * (py - yi) / ((yj - yi).let { if ( kotlin.math.abs(it) < 1e-6f ) 1e-6f else it }) + xi)
            if (intersects) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Calcula a caixa delimitadora combinada ([SelectionBoundsBox]) para uma coleção de strokes selecionados.
     */
    fun computeCombinedBounds(strokes: Collection<Stroke>): SelectionBoundsBox? {
        if (strokes.isEmpty()) return null
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var anyValid = false

        for (stroke in strokes) {
            val b = PartialStrokeEraser.getOrComputeBounds(stroke) ?: continue
            val pad = b.strokeHalfWidth
            minX = min(minX, b.xMin - pad)
            minY = min(minY, b.yMin - pad)
            maxX = max(maxX, b.xMax + pad)
            maxY = max(maxY, b.yMax + pad)
            anyValid = true
        }

        return if (anyValid) SelectionBoundsBox(minX, minY, maxX, maxY) else null
    }

    /**
     * Translada um [Stroke] por `(dx, dy)` em coordenadas do mundo preservando todos os atributos
     * físicos da Stylus (pressão, inclinação, orientação, tempo e pincel).
     */
    fun translateStroke(stroke: Stroke, dx: Float, dy: Float): Stroke {
        if (kotlin.math.abs(dx) < 1e-4f && kotlin.math.abs(dy) < 1e-4f) return stroke
        val oldBatch = stroke.inputs
        val count = oldBatch.size
        if (count == 0) return stroke

        val newBatch = MutableStrokeInputBatch()
        val scratch = StrokeInput()
        for (i in 0 until count) {
            oldBatch.populate(i, scratch)
            val shifted = StrokeInput.create(
                x = scratch.x + dx,
                y = scratch.y + dy,
                elapsedTimeMillis = scratch.elapsedTimeMillis,
                toolType = scratch.toolType,
                strokeUnitLengthCm = scratch.strokeUnitLengthCm,
                pressure = if (scratch.hasPressure) scratch.pressure else StrokeInput.NO_PRESSURE,
                tiltRadians = if (scratch.hasTilt) scratch.tiltRadians else StrokeInput.NO_TILT,
                orientationRadians = if (scratch.hasOrientation) scratch.orientationRadians else StrokeInput.NO_ORIENTATION
            )
            newBatch.add(shifted)
        }
        val movedStroke = Stroke(stroke.brush, newBatch.toImmutable())
        PartialStrokeEraser.prewarmStroke(movedStroke)
        return movedStroke
    }

    /**
     * Cria um novo [Stroke] mantendo exatamente a mesma geometria/inputs, mas alterando a cor do [Brush].
     */
    fun recolorStroke(stroke: Stroke, newColorInt: Int): Stroke {
        val oldBrush = stroke.brush
        if (oldBrush.colorIntArgb == newColorInt) return stroke
        val newBrush = Brush.createWithColorIntArgb(
            oldBrush.family,
            newColorInt,
            oldBrush.size,
            oldBrush.epsilon
        )
        val recolored = Stroke(newBrush, stroke.inputs)
        PartialStrokeEraser.prewarmStroke(recolored)
        return recolored
    }
}

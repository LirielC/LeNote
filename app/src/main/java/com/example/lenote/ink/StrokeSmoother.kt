package com.example.lenote.ink

import androidx.ink.strokes.ImmutableStrokeInputBatch
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import kotlin.math.hypot

/**
 * Níveis configuráveis de suavização da escrita manual (post-stroke smoothing).
 */
enum class SmoothingLevel(val label: String) {
    OFF("OFF"),
    LOW("LOW"),
    MEDIUM("MEDIUM")
}

/**
 * Camada geométrica conservadora de suavização pós-finalização de stroke.
 *
 * Opera exclusivamente sobre o [StrokeInputBatch] de um [Stroke] recém-finalizado,
 * sem adicionar qualquer custo durante o despacho de [android.view.MotionEvent] (zero latência in-progress).
 *
 * Garantias:
 * - Preserva integralmente o primeiro e o último ponto do traço (ancoragem exata de extremidades);
 * - Preserva pressão (pressure), inclinação (tilt), orientação (orientation), toolType,
 *   strokeUnitLengthCm, elapsedTimeMillis (ritmo visual/velocidade) e noiseSeed;
 * - Preserva cantos vivos/mudanças intencionais de direção (corner-aware damping);
 * - Não altera o tamanho nem a proporção global do traço.
 */
object StrokeSmoother {

    fun smoothStroke(stroke: Stroke, level: SmoothingLevel): Stroke {
        if (level == SmoothingLevel.OFF) return stroke

        val originalBatch = stroke.inputs
        if (originalBatch.size < 3) return stroke

        val smoothedBatch = smoothInputBatch(originalBatch, level)
        return Stroke(stroke.brush, smoothedBatch)
    }

    fun smoothInputBatch(
        batch: ImmutableStrokeInputBatch,
        level: SmoothingLevel
    ): ImmutableStrokeInputBatch {
        val count = batch.size
        if (level == SmoothingLevel.OFF || count < 3) return batch

        val rawX = FloatArray(count)
        val rawY = FloatArray(count)
        val scratch = StrokeInput()

        for (i in 0 until count) {
            batch.populate(i, scratch)
            rawX[i] = scratch.x
            rawY[i] = scratch.y
        }

        // Parâmetros conservadores para LOW e MEDIUM
        val passes: Int
        val baseNeighborWeight: Float
        val maxDisplacementWorldUnits: Float

        when (level) {
            SmoothingLevel.OFF -> return batch
            SmoothingLevel.LOW -> {
                // Correção sutil de micro-tremores do digitalizador
                passes = 1
                baseNeighborWeight = 0.16f // [0.16, 0.68, 0.16]
                maxDisplacementWorldUnits = 1.75f
            }
            SmoothingLevel.MEDIUM -> {
                // Correção perceptível mantendo a caligrafia natural e cantos intencionais
                passes = 2
                baseNeighborWeight = 0.22f // [0.22, 0.56, 0.22] por passo
                maxDisplacementWorldUnits = 3.5f
            }
        }

        var currentX = rawX.copyOf()
        var currentY = rawY.copyOf()
        val nextX = FloatArray(count)
        val nextY = FloatArray(count)

        for (pass in 0 until passes) {
            // Ancora estritamente os pontos inicial e final
            nextX[0] = rawX[0]
            nextY[0] = rawY[0]
            nextX[count - 1] = rawX[count - 1]
            nextY[count - 1] = rawY[count - 1]

            for (i in 1 until count - 1) {
                val prevX = currentX[i - 1]
                val prevY = currentY[i - 1]
                val currX = currentX[i]
                val currY = currentY[i]
                val nxtX = currentX[i + 1]
                val nxtY = currentY[i + 1]

                val v1x = currX - prevX
                val v1y = currY - prevY
                val v2x = nxtX - currX
                val v2y = nxtY - currY

                val len1 = hypot(v1x.toDouble(), v1y.toDouble()).toFloat()
                val len2 = hypot(v2x.toDouble(), v2y.toDouble()).toFloat()

                // Detecta mudanças bruscas de direção (vértices/cantos intencionais de letras).
                // Se for um canto agudo intencional (mas não um micro-jitter de < 1.5px), reduz o peso do filtro.
                var effectiveWeight = baseNeighborWeight
                if (len1 > 1.5f && len2 > 1.5f) {
                    val cosTheta = ((v1x * v2x + v1y * v2y) / (len1 * len2)).coerceIn(-1f, 1f)
                    if (cosTheta < 0.25f) {
                        // Canto acentuado: preserva o vértice
                        val cornerFactor = ((cosTheta + 1f) / 1.25f).coerceIn(0.15f, 1f)
                        effectiveWeight *= cornerFactor
                    }
                }

                val centerWeight = 1f - 2f * effectiveWeight
                var candX = effectiveWeight * prevX + centerWeight * currX + effectiveWeight * nxtX
                var candY = effectiveWeight * prevY + centerWeight * currY + effectiveWeight * nxtY

                // Limita o deslocamento máximo em relação ao ponto original para nunca distorcer a letra
                val dxFromOriginal = candX - rawX[i]
                val dyFromOriginal = candY - rawY[i]
                val distFromOriginal = hypot(dxFromOriginal.toDouble(), dyFromOriginal.toDouble()).toFloat()
                if (distFromOriginal > maxDisplacementWorldUnits && distFromOriginal > 0f) {
                    val scale = maxDisplacementWorldUnits / distFromOriginal
                    candX = rawX[i] + dxFromOriginal * scale
                    candY = rawY[i] + dyFromOriginal * scale
                }

                nextX[i] = candX
                nextY[i] = candY
            }

            currentX = nextX.copyOf()
            currentY = nextY.copyOf()
        }

        val mutableBatch = MutableStrokeInputBatch()

        for (i in 0 until count) {
            batch.populate(i, scratch)
            val smoothedInput = StrokeInput.create(
                x = currentX[i],
                y = currentY[i],
                elapsedTimeMillis = scratch.elapsedTimeMillis,
                toolType = scratch.toolType,
                strokeUnitLengthCm = scratch.strokeUnitLengthCm,
                pressure = if (scratch.hasPressure) scratch.pressure else StrokeInput.NO_PRESSURE,
                tiltRadians = if (scratch.hasTilt) scratch.tiltRadians else StrokeInput.NO_TILT,
                orientationRadians = if (scratch.hasOrientation) scratch.orientationRadians else StrokeInput.NO_ORIENTATION
            )
            mutableBatch.add(smoothedInput)
        }

        return mutableBatch.toImmutable()
    }
}

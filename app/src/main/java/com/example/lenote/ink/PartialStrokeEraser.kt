package com.example.lenote.ink

import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.geometry.Box
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import java.util.IdentityHashMap
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import kotlin.math.sqrt

/**
 * Resultado da operação de apagamento parcial (partial stroke erasing) sobre um único [Stroke].
 */
sealed interface PartialEraseResult {
    /** A borracha não atingiu nenhum ponto ou segmento real deste stroke. */
    data object Untouched : PartialEraseResult

    /** O stroke foi atingido pela borracha e substituído por [remainingStrokes] (vazio se apagado por completo). */
    data class Modified(val remainingStrokes: List<Stroke>) : PartialEraseResult
}

/**
 * Ponto leve em coordenadas do mundo (world space) com atributos físicos do [StrokeInput].
 */
internal data class SamplePoint(
    val x: Float,
    val y: Float,
    val elapsedTimeMillis: Long,
    val toolType: InputToolType,
    val strokeUnitLengthCm: Float,
    val hasPressure: Boolean,
    val pressure: Float,
    val hasTilt: Boolean,
    val tiltRadians: Float,
    val hasOrientation: Boolean,
    val orientationRadians: Float
)

/**
 * Bounding box leve em coordenadas do mundo (world space) pré-calculada uma única vez por [Stroke],
 * eliminando alocações e chamadas repetidas a `stroke.shape.computeBoundingBox()` no hot path.
 */
internal data class CachedStrokeBounds(
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float,
    val strokeHalfWidth: Float
)

/**
 * Representa uma sub-cadeia viva de um [Stroke] que foi atingido pela borracha durante o gesto
 * em andamento (`ACTION_MOVE`).
 *
 * Durante o movimento da borracha:
 * - As cadeias vivas ([LiveSampleChain]) são cortadas incrementalmente em memória (< 0.05ms) e
 *   renderizadas diretamente no `FinishedStrokesView` sem chamar `Stroke(brush, batch)` (que
 *   reconstrói a malha `PartitionedMesh` via JNI).
 * - No `ACTION_UP`, cada [LiveSampleChain] sobrevivente é convertida **uma única vez** em um
 *   objeto [Stroke] real do Jetpack Ink!
 */
internal class LiveSampleChain(
    val brush: Brush,
    val strokeHalfWidth: Float,
    val points: List<SamplePoint>,
    val xMin: Float,
    val yMin: Float,
    val xMax: Float,
    val yMax: Float,
    val renderPath: android.graphics.Path
)

/**
 * Motor geométrico de alta performance para apagamento parcial de strokes (Partial Stroke Eraser)
 * utilizando exclusivamente estruturas oficiais do Jetpack Ink ([Stroke], [StrokeInput],
 * [MutableStrokeInputBatch], [Box]).
 *
 * Separação arquitetural para Zero-Jank no Lenovo Xiaoxin Pad 2022 + Lenovo Precision Pen 2:
 * - **Durante `ACTION_MOVE` (Tempo Real)**:
 *   Usa [clipLiveChainBySegment] com corte analítico de cápsula em passagem única O(N) para cortar
 *   apenas as cadeias de [SamplePoint] dos strokes candidatos atingidos em coordenadas do mundo (`WORLD`),
 *   atualizando o preview visual em tempo real com zero chamadas ao construtor pesado `Stroke(brush, batch)`.
 * - **No `ACTION_UP` (Finalização Definitiva)**:
 *   Converte as [LiveSampleChain] sobreviventes em instâncias definitivas de [Stroke] do Jetpack Ink
 *   via [materializeLiveChains] exatamente uma vez por gesto, registrando uma única operação
 *   atômica de Undo/Redo.
 */
object PartialStrokeEraser {

    private val boundsCache = IdentityHashMap<Stroke, CachedStrokeBounds>()
    private val initialChainCache = IdentityHashMap<Stroke, LiveSampleChain>()

    @Synchronized
    internal fun getOrComputeBounds(stroke: Stroke): CachedStrokeBounds? {
        boundsCache[stroke]?.let { return it }
        val box: Box = stroke.shape.computeBoundingBox() ?: return null
        val halfWidth = (stroke.brush.size * 0.45f).coerceAtLeast(1f)
        val cached = CachedStrokeBounds(
            xMin = box.xMin,
            yMin = box.yMin,
            xMax = box.xMax,
            yMax = box.yMax,
            strokeHalfWidth = halfWidth
        )
        boundsCache[stroke] = cached
        return cached
    }

    /**
     * Pré-aquece tanto o bounding box quanto a [LiveSampleChain] base de um [Stroke] assim que ele
     * é criado, garantindo zero custo de extração JNI durante o `ACTION_MOVE` da borracha.
     */
    @Synchronized
    internal fun prewarmStroke(stroke: Stroke) {
        val b = getOrComputeBounds(stroke) ?: return
        if (initialChainCache.containsKey(stroke)) return
        val rawSamples = extractSamples(stroke)
        if (rawSamples.isEmpty()) return
        val samples = densifySamplesIfNeeded(rawSamples, 5.0f)
        val chain = createLiveChain(stroke.brush, b.strokeHalfWidth, samples)
        if (chain != null) {
            initialChainCache[stroke] = chain
        }
    }

    /**
     * Teste rápido de sobreposição AABB (O(1), zero alocações) entre a caixa varrida pelo segmento
     * da borracha `(x0, y0) -> (x1, y1)` com raio `eraserWorldRadius` e o bounding box em cache do [stroke].
     */
    fun isCandidateForSegment(
        stroke: Stroke,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        eraserWorldRadius: Float
    ): Boolean {
        val b = getOrComputeBounds(stroke) ?: return false
        val effR = (eraserWorldRadius + b.strokeHalfWidth).coerceAtLeast(2f)
        val segMinX = min(x0, x1) - effR
        val segMaxX = max(x0, x1) + effR
        val segMinY = min(y0, y1) - effR
        val segMaxY = max(y0, y1) + effR
        return segMinX <= b.xMax && segMaxX >= b.xMin && segMinY <= b.yMax && segMaxY >= b.yMin
    }

    internal fun isChainCandidateForSegment(
        chain: LiveSampleChain,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        eraserWorldRadius: Float
    ): Boolean {
        val effR = (eraserWorldRadius + chain.strokeHalfWidth).coerceAtLeast(2f)
        val segMinX = min(x0, x1) - effR
        val segMaxX = max(x0, x1) + effR
        val segMinY = min(y0, y1) - effR
        val segMaxY = max(y0, y1) + effR
        return segMinX <= chain.xMax && segMaxX >= chain.xMin && segMinY <= chain.yMax && segMaxY >= chain.yMin
    }

    /**
     * Obtém (do cache pré-aquecido ou computando uma única vez) a [LiveSampleChain] inicial de um [Stroke]
     * para ser cortada incrementalmente durante `ACTION_MOVE` sem reconstruir `PartitionedMesh`.
     */
    @Synchronized
    internal fun strokeToInitialLiveChain(stroke: Stroke, eraserWorldRadius: Float): LiveSampleChain? {
        initialChainCache[stroke]?.let { return it }
        val b = getOrComputeBounds(stroke) ?: return null
        val rawSamples = extractSamples(stroke)
        if (rawSamples.isEmpty()) return null
        val maxStepLength = max(3.0f, min(eraserWorldRadius * 0.45f, 6.0f))
        val samples = densifySamplesIfNeeded(rawSamples, maxStepLength)
        val chain = createLiveChain(stroke.brush, b.strokeHalfWidth, samples)
        if (chain != null) {
            initialChainCache[stroke] = chain
        }
        return chain
    }

    internal fun createLiveChain(
        brush: Brush,
        strokeHalfWidth: Float,
        points: List<SamplePoint>
    ): LiveSampleChain? {
        if (points.isEmpty()) return null
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        val path = android.graphics.Path()
        val first = points[0]
        path.moveTo(first.x, first.y)
        minX = first.x
        minY = first.y
        maxX = first.x
        maxY = first.y

        for (i in 1 until points.size) {
            val p = points[i]
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
            path.lineTo(p.x, p.y)
        }
        return LiveSampleChain(
            brush = brush,
            strokeHalfWidth = strokeHalfWidth,
            points = points,
            xMin = minX,
            yMin = minY,
            xMax = maxX,
            yMax = maxY,
            renderPath = path
        )
    }

    /**
     * Corta uma [LiveSampleChain] contra a cápsula varrida `(x0, y0) -> (x1, y1)` em coordenadas
     * do mundo (`WORLD`) em **uma única passagem linear O(N)**, sem loop multi-círculo e sem
     * instanciar nenhum objeto [Stroke] nativo!
     *
     * @return `Pair(anyHit, resultingChains)` onde `anyHit` indica se o segmento realmente atingiu a cadeia.
     */
    internal fun clipLiveChainBySegment(
        chain: LiveSampleChain,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        eraserWorldRadius: Float
    ): Pair<Boolean, List<LiveSampleChain>> {
        if (!isChainCandidateForSegment(chain, x0, y0, x1, y1, eraserWorldRadius)) {
            return Pair(false, listOf(chain))
        }

        val effectiveRadius = (eraserWorldRadius + chain.strokeHalfWidth).coerceAtLeast(2f)
        val samples = chain.points
        if (samples.isEmpty()) return Pair(false, emptyList())

        val (hit, splitLists) = clipSampleChainByCapsule(
            samples = samples,
            x0 = x0,
            y0 = y0,
            x1 = x1,
            y1 = y1,
            effectiveRadius = effectiveRadius
        )

        if (!hit) {
            return Pair(false, listOf(chain))
        }

        val outChains = ArrayList<LiveSampleChain>(splitLists.size)
        for (i in splitLists.indices) {
            val pts = splitLists[i]
            if (isChainLongEnough(pts)) {
                val live = createLiveChain(chain.brush, chain.strokeHalfWidth, pts)
                if (live != null) {
                    outChains.add(live)
                }
            }
        }
        return Pair(true, outChains)
    }

    /**
     * Converte uma lista de [LiveSampleChain] em objetos [Stroke] definitivos do Jetpack Ink.
     * Chamado **apenas uma vez** no `ACTION_UP` da borracha!
     */
    internal fun materializeLiveChains(chains: List<LiveSampleChain>): List<Stroke> {
        if (chains.isEmpty()) return emptyList()
        val out = ArrayList<Stroke>(chains.size)
        for (i in chains.indices) {
            val chain = chains[i]
            val stroke = buildStrokeFromChain(chain.brush, chain.points)
            if (stroke != null) {
                getOrComputeBounds(stroke)
                initialChainCache[stroke] = chain
                out.add(stroke)
            }
        }
        return out
    }

    /**
     * Aplica um único círculo de borracha no espaço do mundo `(eraserWorldX, eraserWorldY)` com raio
     * `eraserWorldRadius` sobre um [stroke].
     */
    fun eraseCircleFromStroke(
        stroke: Stroke,
        eraserWorldX: Float,
        eraserWorldY: Float,
        eraserWorldRadius: Float
    ): PartialEraseResult {
        return eraseSegmentFromStroke(
            stroke = stroke,
            x0 = eraserWorldX,
            y0 = eraserWorldY,
            x1 = eraserWorldX,
            y1 = eraserWorldY,
            eraserWorldRadius = eraserWorldRadius
        )
    }

    /**
     * Aplica um segmento varrido (cápsula) de borracha no espaço do mundo de `(x0, y0)` até `(x1, y1)`
     * com raio `eraserWorldRadius` sobre um [stroke].
     */
    fun eraseSegmentFromStroke(
        stroke: Stroke,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        eraserWorldRadius: Float
    ): PartialEraseResult {
        if (!isCandidateForSegment(stroke, x0, y0, x1, y1, eraserWorldRadius)) {
            return PartialEraseResult.Untouched
        }
        val initialChain = strokeToInitialLiveChain(stroke, eraserWorldRadius)
            ?: return PartialEraseResult.Untouched
        val (hit, survivingChains) = clipLiveChainBySegment(initialChain, x0, y0, x1, y1, eraserWorldRadius)
        if (!hit) {
            return PartialEraseResult.Untouched
        }
        return PartialEraseResult.Modified(materializeLiveChains(survivingChains))
    }

    /**
     * Corta uma única cadeia de [SamplePoint] contra a cápsula varrida `(x0, y0) -> (x1, y1)` de raio
     * [effectiveRadius] em **uma única passagem linear O(N)**, com descarte rápido por AABB por segmento
     * e sem criar objetos [Stroke] intermediários.
     */
    internal fun clipSampleChainByCapsule(
        samples: List<SamplePoint>,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        effectiveRadius: Float
    ): Pair<Boolean, List<List<SamplePoint>>> {
        val count = samples.size
        if (count == 0) return Pair(false, emptyList())
        val innerRadius = (effectiveRadius - 0.05f).coerceAtLeast(0.5f)
        val innerRSq = innerRadius * innerRadius

        if (count == 1) {
            val p = samples[0]
            val distSq = distanceToSegmentSquared(p.x, p.y, x0, y0, x1, y1)
            return if (distSq <= innerRSq) {
                Pair(true, emptyList())
            } else {
                Pair(false, listOf(samples))
            }
        }

        val capMinX = min(x0, x1) - effectiveRadius
        val capMaxX = max(x0, x1) + effectiveRadius
        val capMinY = min(y0, y1) - effectiveRadius
        val capMaxY = max(y0, y1) + effectiveRadius

        fun isStrictlyInside(p: SamplePoint): Boolean {
            if (p.x < capMinX || p.x > capMaxX || p.y < capMinY || p.y > capMaxY) {
                return false
            }
            return distanceToSegmentSquared(p.x, p.y, x0, y0, x1, y1) < innerRSq
        }

        var anyHit = false
        var subStrokePointLists: ArrayList<MutableList<SamplePoint>>? = null
        var currentChain: ArrayList<SamplePoint>? = null

        var a = samples[0]
        var aIn = isStrictlyInside(a)
        if (aIn) {
            anyHit = true
            subStrokePointLists = ArrayList(2)
            currentChain = ArrayList(count)
        }

        fun ensureMaterializedAtFirstHit(upToInclusiveIndex: Int): ArrayList<SamplePoint> {
            var chain = currentChain
            if (chain == null) {
                subStrokePointLists = ArrayList(2)
                chain = ArrayList(count)
                for (k in 0..upToInclusiveIndex) {
                    appendDistinct(chain, samples[k])
                }
                currentChain = chain
            }
            return chain
        }

        for (i in 0 until count - 1) {
            val b = samples[i + 1]

            // Fast-path AABB: se 'a' já estava fora e a caixa do segmento A->B nem encosta na caixa da cápsula,
            // então 'b' também está fora e o segmento não intercepta a cápsula (zero matemática pesada e zero alocação se !anyHit!).
            if (!aIn) {
                val segMaxX = if (a.x > b.x) a.x else b.x
                val segMinX = if (a.x < b.x) a.x else b.x
                val segMaxY = if (a.y > b.y) a.y else b.y
                val segMinY = if (a.y < b.y) a.y else b.y
                if (segMaxX < capMinX || segMinX > capMaxX || segMaxY < capMinY || segMinY > capMaxY) {
                    if (anyHit) {
                        appendDistinct(currentChain!!, b)
                    }
                    a = b
                    aIn = false
                    continue
                }
            }

            val bIn = isStrictlyInside(b)

            if (!aIn && !bIn) {
                val innerInterval = intersectSegmentWithCapsule(
                    ax = a.x, ay = a.y, bx = b.x, by = b.y,
                    x0 = x0, y0 = y0, x1 = x1, y1 = y1,
                    r = innerRadius
                )
                if (innerInterval != null && (innerInterval.second - innerInterval.first) > 0.02f) {
                    val outerInterval = intersectSegmentWithCapsule(
                        ax = a.x, ay = a.y, bx = b.x, by = b.y,
                        x0 = x0, y0 = y0, x1 = x1, y1 = y1,
                        r = effectiveRadius
                    ) ?: innerInterval
                    val chainBeforeHit = ensureMaterializedAtFirstHit(i)
                    anyHit = true
                    val tEnter = outerInterval.first
                    val tExit = outerInterval.second
                    if (tEnter > 0.02f) {
                        val enterPt = interpolateSample(a, b, tEnter)
                        appendDistinct(chainBeforeHit, enterPt)
                    }
                    if (chainBeforeHit.isNotEmpty()) {
                        subStrokePointLists!!.add(chainBeforeHit)
                        currentChain = ArrayList()
                    }
                    if (tExit < 0.98f) {
                        val exitPt = interpolateSample(a, b, tExit)
                        appendDistinct(currentChain!!, exitPt)
                    }
                    appendDistinct(currentChain!!, b)
                } else if (anyHit) {
                    appendDistinct(currentChain!!, b)
                }
            } else if (!aIn && bIn) {
                val chainBeforeHit = ensureMaterializedAtFirstHit(i)
                anyHit = true
                val interval = intersectSegmentWithCapsule(
                    ax = a.x, ay = a.y, bx = b.x, by = b.y,
                    x0 = x0, y0 = y0, x1 = x1, y1 = y1,
                    r = effectiveRadius
                )
                val tEnter = interval?.first ?: 0.5f
                if (tEnter > 0.02f) {
                    val enterPt = interpolateSample(a, b, tEnter)
                    appendDistinct(chainBeforeHit, enterPt)
                }
                if (chainBeforeHit.isNotEmpty()) {
                    subStrokePointLists!!.add(chainBeforeHit)
                    currentChain = ArrayList()
                }
            } else if (aIn && !bIn) {
                anyHit = true
                val interval = intersectSegmentWithCapsule(
                    ax = a.x, ay = a.y, bx = b.x, by = b.y,
                    x0 = x0, y0 = y0, x1 = x1, y1 = y1,
                    r = effectiveRadius
                )
                val tExit = interval?.second ?: 0.5f
                if (tExit < 0.98f) {
                    val exitPt = interpolateSample(a, b, tExit)
                    appendDistinct(currentChain!!, exitPt)
                }
                appendDistinct(currentChain!!, b)
            } else {
                anyHit = true
            }

            a = b
            aIn = bIn
        }

        if (!anyHit) {
            return Pair(false, emptyList())
        }

        val finalChain = currentChain
        val outLists = subStrokePointLists!!
        if (finalChain != null && finalChain.isNotEmpty()) {
            outLists.add(finalChain)
        }

        return Pair(true, outLists)
    }

    /**
     * Calcula analiticamente o intervalo paramétrico `[tEnter, tExit]` em `[0, 1]` onde o segmento
     * `A(ax, ay) -> B(bx, by)` intercepta a cápsula definida pelo segmento `(x0, y0) -> (x1, y1)` e raio [r].
     */
    private fun intersectSegmentWithCapsule(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        r: Float
    ): Pair<Float, Float>? {
        val ux = x1 - x0
        val uy = y1 - y0
        val lenSq = ux * ux + uy * uy
        if (lenSq < 1e-5f) {
            return intersectSegmentWithCircle(ax, ay, bx, by, x0, y0, r)
        }

        var minEnter = 2f
        var maxExit = -1f

        val cap0 = intersectSegmentWithCircle(ax, ay, bx, by, x0, y0, r)
        if (cap0 != null) {
            if (cap0.first < minEnter) minEnter = cap0.first
            if (cap0.second > maxExit) maxExit = cap0.second
        }

        val cap1 = intersectSegmentWithCircle(ax, ay, bx, by, x1, y1, r)
        if (cap1 != null) {
            if (cap1.first < minEnter) minEnter = cap1.first
            if (cap1.second > maxExit) maxExit = cap1.second
        }

        val len = sqrt(lenSq.toDouble()).toFloat()
        val dirX = ux / len
        val dirY = uy / len
        val normX = -dirY
        val normY = dirX

        val dax = ax - x0
        val day = ay - y0
        val sdx = bx - ax
        val sdy = by - ay

        // Coordenada longitudinal u(t) = u0 + t * du em [0, len]
        val u0 = dax * dirX + day * dirY
        val du = sdx * dirX + sdy * dirY

        // Coordenada transversal v(t) = v0 + t * dv em [-r, +r]
        val v0 = dax * normX + day * normY
        val dv = sdx * normX + sdy * normY

        var tMinCyl = 0f
        var tMaxCyl = 1f
        var cylValid = true

        if (kotlin.math.abs(du) < 1e-6f) {
            if (u0 < 0f || u0 > len) cylValid = false
        } else {
            val tu1 = (0f - u0) / du
            val tu2 = (len - u0) / du
            val uEnter = min(tu1, tu2)
            val uExit = max(tu1, tu2)
            tMinCyl = max(tMinCyl, uEnter)
            tMaxCyl = min(tMaxCyl, uExit)
            if (tMinCyl > tMaxCyl) cylValid = false
        }

        if (cylValid) {
            if (kotlin.math.abs(dv) < 1e-6f) {
                if (v0 < -r || v0 > r) cylValid = false
            } else {
                val tv1 = (-r - v0) / dv
                val tv2 = (r - v0) / dv
                val vEnter = min(tv1, tv2)
                val vExit = max(tv1, tv2)
                tMinCyl = max(tMinCyl, vEnter)
                tMaxCyl = min(tMaxCyl, vExit)
                if (tMinCyl > tMaxCyl) cylValid = false
            }
        }

        if (cylValid && tMinCyl <= tMaxCyl) {
            if (tMinCyl < minEnter) minEnter = tMinCyl
            if (tMaxCyl > maxExit) maxExit = tMaxCyl
        }

        if (minEnter > maxExit) return null
        return Pair(minEnter.coerceIn(0f, 1f), maxExit.coerceIn(0f, 1f))
    }

    private fun isChainLongEnough(chain: List<SamplePoint>): Boolean {
        if (chain.size < 2) return false
        var totalLen = 0f
        for (i in 0 until chain.size - 1) {
            val p0 = chain[i]
            val p1 = chain[i + 1]
            totalLen += hypot((p1.x - p0.x).toDouble(), (p1.y - p0.y).toDouble()).toFloat()
            if (totalLen >= 1.5f) return true
        }
        return false
    }

    private fun distanceToSegmentSquared(
        px: Float,
        py: Float,
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float
    ): Float {
        val dx = x1 - x0
        val dy = y1 - y0
        val lenSq = dx * dx + dy * dy
        if (lenSq < 1e-6f) {
            val ex = px - x0
            val ey = py - y0
            return ex * ex + ey * ey
        }
        val t = (((px - x0) * dx + (py - y0) * dy) / lenSq).coerceIn(0f, 1f)
        val projX = x0 + t * dx
        val projY = y0 + t * dy
        val ex = px - projX
        val ey = py - projY
        return ex * ex + ey * ey
    }

    private fun extractSamples(stroke: Stroke): List<SamplePoint> {
        val batch = stroke.inputs
        val size = batch.size
        if (size == 0) return emptyList()

        val list = ArrayList<SamplePoint>(size)
        val scratch = StrokeInput()
        for (i in 0 until size) {
            batch.populate(i, scratch)
            list.add(
                SamplePoint(
                    x = scratch.x,
                    y = scratch.y,
                    elapsedTimeMillis = scratch.elapsedTimeMillis,
                    toolType = scratch.toolType,
                    strokeUnitLengthCm = scratch.strokeUnitLengthCm,
                    hasPressure = scratch.hasPressure,
                    pressure = scratch.pressure,
                    hasTilt = scratch.hasTilt,
                    tiltRadians = scratch.tiltRadians,
                    hasOrientation = scratch.hasOrientation,
                    orientationRadians = scratch.orientationRadians
                )
            )
        }
        return list
    }

    private fun densifySamplesIfNeeded(
        samples: List<SamplePoint>,
        maxStepLength: Float
    ): List<SamplePoint> {
        if (samples.size < 2) return samples

        var needsDensification = false
        val maxStepSq = maxStepLength * maxStepLength
        for (i in 0 until samples.size - 1) {
            val a = samples[i]
            val b = samples[i + 1]
            val dx = b.x - a.x
            val dy = b.y - a.y
            if (dx * dx + dy * dy > maxStepSq) {
                needsDensification = true
                break
            }
        }
        if (!needsDensification) return samples

        val result = ArrayList<SamplePoint>(samples.size * 2)
        result.add(samples[0])

        for (i in 0 until samples.size - 1) {
            val a = samples[i]
            val b = samples[i + 1]
            val dist = hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
            if (dist > maxStepLength) {
                val steps = ceil(dist / maxStepLength).toInt().coerceIn(2, 64)
                for (s in 1 until steps) {
                    val t = s.toFloat() / steps.toFloat()
                    result.add(interpolateSample(a, b, t))
                }
            }
            result.add(b)
        }
        return result
    }

    private fun interpolateSample(a: SamplePoint, b: SamplePoint, t: Float): SamplePoint {
        val clampedT = t.coerceIn(0f, 1f)
        val x = a.x + (b.x - a.x) * clampedT
        val y = a.y + (b.y - a.y) * clampedT
        val time = a.elapsedTimeMillis + ((b.elapsedTimeMillis - a.elapsedTimeMillis) * clampedT).roundToLong()
        val pressure = if (a.hasPressure && b.hasPressure) {
            a.pressure + (b.pressure - a.pressure) * clampedT
        } else {
            a.pressure
        }
        val tilt = if (a.hasTilt && b.hasTilt) {
            a.tiltRadians + (b.tiltRadians - a.tiltRadians) * clampedT
        } else {
            a.tiltRadians
        }
        val orientation = if (a.hasOrientation && b.hasOrientation) {
            a.orientationRadians + (b.orientationRadians - a.orientationRadians) * clampedT
        } else {
            a.orientationRadians
        }
        return SamplePoint(
            x = x,
            y = y,
            elapsedTimeMillis = time,
            toolType = a.toolType,
            strokeUnitLengthCm = a.strokeUnitLengthCm,
            hasPressure = a.hasPressure,
            pressure = pressure,
            hasTilt = a.hasTilt,
            tiltRadians = tilt,
            hasOrientation = a.hasOrientation,
            orientationRadians = orientation
        )
    }

    private fun appendDistinct(chain: MutableList<SamplePoint>, candidate: SamplePoint) {
        if (chain.isEmpty()) {
            chain.add(candidate)
            return
        }
        val last = chain.last()
        val dx = candidate.x - last.x
        val dy = candidate.y - last.y
        if (dx * dx + dy * dy >= 0.0625f) { // dist >= 0.25f
            chain.add(candidate)
        }
    }

    /**
     * Calcula o intervalo paramétrico `[tEnter, tExit]` em `[0, 1]` onde o segmento `A -> B`
     * está dentro do círculo de centro `(cx, cy)` e raio `r`.
     * Retorna `null` se o segmento não intersectar o círculo.
     */
    private fun intersectSegmentWithCircle(
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        cx: Float,
        cy: Float,
        r: Float
    ): Pair<Float, Float>? {
        val dx = bx - ax
        val dy = by - ay
        val fx = ax - cx
        val fy = ay - cy

        val aCoeff = dx * dx + dy * dy
        if (aCoeff < 1e-6f) {
            return if (fx * fx + fy * fy <= r * r) Pair(0f, 1f) else null
        }

        val bCoeff = 2f * (fx * dx + fy * dy)
        val cCoeff = (fx * fx + fy * fy) - r * r

        val discriminant = bCoeff * bCoeff - 4f * aCoeff * cCoeff
        if (discriminant < 0f) return null

        val sqrtDisc = sqrt(discriminant.toDouble()).toFloat()
        val t1 = (-bCoeff - sqrtDisc) / (2f * aCoeff)
        val t2 = (-bCoeff + sqrtDisc) / (2f * aCoeff)

        if (t2 < 0f || t1 > 1f) return null

        val enter = t1.coerceIn(0f, 1f)
        val exit = t2.coerceIn(0f, 1f)
        return if (enter <= exit) Pair(enter, exit) else null
    }

    private fun buildStrokeFromChain(
        brush: Brush,
        chain: List<SamplePoint>
    ): Stroke? {
        if (!isChainLongEnough(chain)) return null

        val batch = MutableStrokeInputBatch()
        val baseTime = chain.first().elapsedTimeMillis
        var prevTime = -1L
        var prevX = Float.NaN
        var prevY = Float.NaN

        for (pt in chain) {
            if (!prevX.isNaN()) {
                val dx = pt.x - prevX
                val dy = pt.y - prevY
                if (dx * dx + dy * dy < 0.01f) continue // dist < 0.1f
            }

            val rawRelTime = (pt.elapsedTimeMillis - baseTime).coerceAtLeast(0L)
            val safeTime = if (prevTime < 0L) {
                0L
            } else if (rawRelTime <= prevTime) {
                prevTime + 1L
            } else {
                rawRelTime
            }

            try {
                val input = StrokeInput.create(
                    x = pt.x,
                    y = pt.y,
                    elapsedTimeMillis = safeTime,
                    toolType = pt.toolType,
                    strokeUnitLengthCm = pt.strokeUnitLengthCm,
                    pressure = if (pt.hasPressure) pt.pressure else StrokeInput.NO_PRESSURE,
                    tiltRadians = if (pt.hasTilt) pt.tiltRadians else StrokeInput.NO_TILT,
                    orientationRadians = if (pt.hasOrientation) pt.orientationRadians else StrokeInput.NO_ORIENTATION
                )
                batch.add(input)
                prevTime = safeTime
                prevX = pt.x
                prevY = pt.y
            } catch (_: Exception) {
                // Ignora ponto duplicado caso ocorra arredondamento numérico extremo
            }
        }

        if (batch.size < 2) return null
        return Stroke(brush, batch.toImmutable())
    }
}

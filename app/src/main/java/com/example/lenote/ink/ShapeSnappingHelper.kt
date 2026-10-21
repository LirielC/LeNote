package com.example.lenote.ink

import androidx.ink.brush.Brush
import androidx.ink.brush.InputToolType
import androidx.ink.strokes.MutableStrokeInputBatch
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Ponto 2D simples para cálculos geométricos.
 */
data class Point2D(val x: Float, val y: Float)

/**
 * Tipos de formas reconhecidas pelo classificador geométrico.
 */
sealed interface RecognizedShape {
    data class Line(val start: Point2D, val end: Point2D) : RecognizedShape
    data class Circle(val center: Point2D, val radius: Float) : RecognizedShape
    data class Ellipse(val center: Point2D, val radiusX: Float, val radiusY: Float) : RecognizedShape
    data class Rectangle(val p1: Point2D, val p2: Point2D, val p3: Point2D, val p4: Point2D) : RecognizedShape
    data class Triangle(val p1: Point2D, val p2: Point2D, val p3: Point2D) : RecognizedShape
}

/**
 * Utilitário de Geometria Computacional pura em Kotlin para Reconhecimento e Alinhamento de Formas (Shape Snapping).
 * - Algoritmo Ramer-Douglas-Peucker (RDP) para simplificação poligonal;
 * - Análise de circularidade (Isoperimetric Quotient) para círculos e elipses;
 * - Detecção de ângulos e vértices para triângulos e retângulos/quadrados.
 */
object ShapeSnappingHelper {

    /**
     * Tenta reconhecer uma forma geométrica a partir de uma lista de pontos brutos (x, y) do traço.
     * Retorna a [RecognizedShape] identificada ou null se o traço for forma livre arbitrária.
     */
    fun classifyShape(points: List<Point2D>): RecognizedShape? {
        if (points.size < 4) return null

        val start = points.first()
        val end = points.last()
        val totalLength = computePathLength(points)
        if (totalLength < 25f) return null

        val chordDist = distance(start, end)
        val isClosed = chordDist < 45f || (chordDist / totalLength) < 0.18f

        if (!isClosed) {
            // Candidato a Linha Reta: desvio perpendicular máximo da corda start->end
            val maxPerpDist = maxPerpendicularDistance(points, start, end)
            if (maxPerpDist < 18f || (maxPerpDist / totalLength) < 0.08f) {
                return RecognizedShape.Line(start, end)
            }
        }

        // Se fechado (ou quase fechado), pode ser Polígono (Triângulo/Retângulo) ou Círculo/Elipse
        if (isClosed) {
            val closedPoints = if (chordDist > 0.01f) points + start else points

            // 1. Teste de Círculo primeiro com critérios rigorosos de curvatura contínua
            val (center, avgRadius) = fitCircle(points)
            val maxRadiusDiff = points.maxOf { abs(distance(it, center) - avgRadius) }
            val radiusVarianceRatio = if (avgRadius > 0.01f) maxRadiusDiff / avgRadius else 1.0f

            val area = abs(computePolygonArea(closedPoints))
            val perimeter = totalLength
            val circularity = if (perimeter > 0f) (4f * PI.toFloat() * area) / (perimeter * perimeter) else 0f

            // Um círculo verdadeiro tem raio praticamente constante ao redor do centro (variância < 16%)
            // e circularidade próxima de 1.0 (ex: 0.82 .. 1.22)
            if (avgRadius > 10f && radiusVarianceRatio < 0.16f && circularity in 0.80f..1.22f) {
                return RecognizedShape.Circle(center, avgRadius)
            }

            // 2. Simplificação poligonal via RDP para Triângulos e Retângulos
            val epsilon = (totalLength * 0.05f).coerceIn(10f, 35f)
            val simplified = ramerDouglasPeucker(closedPoints, epsilon)
            val vertices = if (simplified.size > 1 && distance(simplified.first(), simplified.last()) < 20f) {
                simplified.dropLast(1)
            } else {
                simplified
            }

            // Triângulo: exatamente 3 vértices nítidos
            if (vertices.size == 3) {
                return RecognizedShape.Triangle(vertices[0], vertices[1], vertices[2])
            }

            // Retângulo / Quadrado: exatamente 4 vértices nítidos
            if (vertices.size == 4) {
                val isRectangular = checkRightAngles(vertices)
                if (isRectangular) {
                    return alignRectangle(vertices)
                }
                return RecognizedShape.Rectangle(vertices[0], vertices[1], vertices[2], vertices[3])
            }

            // Círculo com tolerância um pouco mais relaxada caso não seja quadrado
            if (avgRadius > 10f && radiusVarianceRatio < 0.24f && circularity in 0.75f..1.28f) {
                return RecognizedShape.Circle(center, avgRadius)
            }
        }

        // Simplificação aberta com RDP para detectar linhas retas parciais
        val rdpLine = ramerDouglasPeucker(points, (totalLength * 0.05f).coerceAtLeast(10f))
        if (rdpLine.size == 2) {
            return RecognizedShape.Line(rdpLine[0], rdpLine[1])
        }

        return null
    }

    /**
     * Converte a forma reconhecida em um [Stroke] nativo do Jetpack Ink preservando o Brush.
     */
    fun createSnappedStroke(shape: RecognizedShape, brush: Brush): Stroke {
        val polyPoints = when (shape) {
            is RecognizedShape.Line -> listOf(shape.start, shape.end)
            is RecognizedShape.Circle -> generateCirclePoints(shape.center, shape.radius, 48)
            is RecognizedShape.Ellipse -> generateEllipsePoints(shape.center, shape.radiusX, shape.radiusY, 48)
            is RecognizedShape.Rectangle -> listOf(shape.p1, shape.p2, shape.p3, shape.p4, shape.p1)
            is RecognizedShape.Triangle -> listOf(shape.p1, shape.p2, shape.p3, shape.p1)
        }

        val batch = MutableStrokeInputBatch()
        val scratch = StrokeInput()
        val stepMs = 5L
        for (i in polyPoints.indices) {
            val pt = polyPoints[i]
            scratch.update(
                x = pt.x,
                y = pt.y,
                elapsedTimeMillis = i * stepMs,
                toolType = InputToolType.STYLUS,
                strokeUnitLengthCm = 0f,
                pressure = 0.5f,
                tiltRadians = 0f,
                orientationRadians = 0f
            )
            batch.add(scratch)
        }
        return Stroke(brush, batch.toImmutable())
    }

    fun computePathLength(points: List<Point2D>): Float {
        var len = 0f
        for (i in 0 until points.size - 1) {
            len += distance(points[i], points[i + 1])
        }
        return len
    }

    fun distance(p1: Point2D, p2: Point2D): Float =
        hypot((p2.x - p1.x).toDouble(), (p2.y - p1.y).toDouble()).toFloat()

    private fun maxPerpendicularDistance(points: List<Point2D>, lineStart: Point2D, lineEnd: Point2D): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lineLen = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (lineLen < 0.001f) return 0f

        var maxDist = 0f
        for (p in points) {
            val dist = abs(dy * p.x - dx * p.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x) / lineLen
            if (dist > maxDist) maxDist = dist
        }
        return maxDist
    }

    private fun computePolygonArea(points: List<Point2D>): Float {
        var area = 0f
        for (i in points.indices) {
            val j = (i + 1) % points.size
            area += points[i].x * points[j].y
            area -= points[j].x * points[i].y
        }
        return area / 2f
    }

    private fun fitCircle(points: List<Point2D>): Pair<Point2D, Float> {
        val avgX = points.map { it.x }.average().toFloat()
        val avgY = points.map { it.y }.average().toFloat()
        val center = Point2D(avgX, avgY)
        val avgRadius = points.map { distance(it, center) }.average().toFloat()
        return Pair(center, avgRadius)
    }

    fun ramerDouglasPeucker(points: List<Point2D>, epsilon: Float): List<Point2D> {
        if (points.size < 3) return points

        var maxDist = 0f
        var maxIndex = 0
        val start = points.first()
        val end = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], start, end)
            if (dist > maxDist) {
                maxDist = dist
                maxIndex = i
            }
        }

        return if (maxDist > epsilon) {
            val left = ramerDouglasPeucker(points.subList(0, maxIndex + 1), epsilon)
            val right = ramerDouglasPeucker(points.subList(maxIndex, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }

    private fun perpendicularDistance(p: Point2D, lineStart: Point2D, lineEnd: Point2D): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val lineLen = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (lineLen < 0.001f) return distance(p, lineStart)
        return abs(dy * p.x - dx * p.y + lineEnd.x * lineStart.y - lineEnd.y * lineStart.x) / lineLen
    }

    private fun checkRightAngles(v: List<Point2D>): Boolean {
        if (v.size != 4) return false
        for (i in 0..3) {
            val pPrev = v[(i + 3) % 4]
            val pCur = v[i]
            val pNext = v[(i + 1) % 4]
            val angle = angleDegrees(pPrev, pCur, pNext)
            if (abs(angle - 90f) > 30f) return false
        }
        return true
    }

    private fun angleDegrees(p1: Point2D, p2: Point2D, p3: Point2D): Float {
        val v1x = p1.x - p2.x
        val v1y = p1.y - p2.y
        val v2x = p3.x - p2.x
        val v2y = p3.y - p2.y
        val dot = v1x * v2x + v1y * v2y
        val m1 = hypot(v1x.toDouble(), v1y.toDouble()).toFloat()
        val m2 = hypot(v2x.toDouble(), v2y.toDouble()).toFloat()
        if (m1 * m2 < 0.001f) return 90f
        val cosTheta = (dot / (m1 * m2)).coerceIn(-1f, 1f)
        return (kotlin.math.acos(cosTheta) * (180f / PI.toFloat()))
    }

    private fun alignRectangle(v: List<Point2D>): RecognizedShape.Rectangle {
        val minX = v.minOf { it.x }
        val maxX = v.maxOf { it.x }
        val minY = v.minOf { it.y }
        val maxY = v.maxOf { it.y }

        return RecognizedShape.Rectangle(
            p1 = Point2D(minX, minY),
            p2 = Point2D(maxX, minY),
            p3 = Point2D(maxX, maxY),
            p4 = Point2D(minX, maxY)
        )
    }

    private fun generateCirclePoints(center: Point2D, radius: Float, count: Int): List<Point2D> {
        val list = ArrayList<Point2D>(count + 1)
        for (i in 0..count) {
            val angle = (2 * PI * i / count).toFloat()
            list.add(Point2D(center.x + radius * cos(angle), center.y + radius * sin(angle)))
        }
        return list
    }

    private fun generateEllipsePoints(center: Point2D, rx: Float, ry: Float, count: Int): List<Point2D> {
        val list = ArrayList<Point2D>(count + 1)
        for (i in 0..count) {
            val angle = (2 * PI * i / count).toFloat()
            list.add(Point2D(center.x + rx * cos(angle), center.y + ry * sin(angle)))
        }
        return list
    }
}

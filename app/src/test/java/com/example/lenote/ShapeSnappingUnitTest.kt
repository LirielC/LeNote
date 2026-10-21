package com.example.lenote

import com.example.lenote.ink.Point2D
import com.example.lenote.ink.RecognizedShape
import com.example.lenote.ink.ShapeSnappingHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ShapeSnappingUnitTest {

    @Test
    fun testStraightLineRecognition() {
        val linePoints = (0..20).map { i ->
            Point2D(100f + i * 10f, 200f + (if (i % 2 == 0) 1f else -1f)) // reta horizontal com leve ruído
        }
        val shape = ShapeSnappingHelper.classifyShape(linePoints)
        assertNotNull("Deveria reconhecer como reta", shape)
        assertTrue(shape is RecognizedShape.Line)
        val line = shape as RecognizedShape.Line
        assertEquals(100f, line.start.x, 2f)
        assertEquals(300f, line.end.x, 2f)
    }

    @Test
    fun testCircleRecognition() {
        val center = Point2D(250f, 250f)
        val radius = 80f
        val circlePoints = (0..36).map { i ->
            val angle = (2 * PI * i / 36).toFloat()
            Point2D(center.x + radius * cos(angle), center.y + radius * sin(angle))
        }
        val shape = ShapeSnappingHelper.classifyShape(circlePoints)
        assertNotNull("Deveria reconhecer como círculo", shape)
        assertTrue(shape is RecognizedShape.Circle)
        val circle = shape as RecognizedShape.Circle
        assertEquals(center.x, circle.center.x, 5f)
        assertEquals(center.y, circle.center.y, 5f)
        assertEquals(radius, circle.radius, 5f)
    }

    @Test
    fun testTriangleRecognition() {
        val p1 = Point2D(200f, 100f)
        val p2 = Point2D(100f, 300f)
        val p3 = Point2D(300f, 300f)

        val points = mutableListOf<Point2D>()
        // Aresta 1
        for (i in 0..10) points.add(Point2D(p1.x + (p2.x - p1.x) * i / 10f, p1.y + (p2.y - p1.y) * i / 10f))
        // Aresta 2
        for (i in 1..10) points.add(Point2D(p2.x + (p3.x - p2.x) * i / 10f, p2.y + (p3.y - p2.y) * i / 10f))
        // Aresta 3
        for (i in 1..10) points.add(Point2D(p3.x + (p1.x - p3.x) * i / 10f, p3.y + (p1.y - p3.y) * i / 10f))

        val shape = ShapeSnappingHelper.classifyShape(points)
        assertNotNull("Deveria reconhecer como triângulo", shape)
        assertTrue(shape is RecognizedShape.Triangle)
    }

    @Test
    fun testRectangleRecognition() {
        val points = mutableListOf<Point2D>()
        // Topo
        for (x in 100..300 step 20) points.add(Point2D(x.toFloat(), 100f))
        // Direita
        for (y in 100..250 step 20) points.add(Point2D(300f, y.toFloat()))
        // Fundo
        for (x in 300 downTo 100 step 20) points.add(Point2D(x.toFloat(), 250f))
        // Esquerda
        for (y in 250 downTo 100 step 20) points.add(Point2D(100f, y.toFloat()))

        val shape = ShapeSnappingHelper.classifyShape(points)
        assertNotNull("Deveria reconhecer como retângulo", shape)
        assertTrue(shape is RecognizedShape.Rectangle)
    }
}

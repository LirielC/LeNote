package com.example.lenote

import androidx.ink.brush.InputToolType
import com.example.lenote.data.PageStrokeStorage
import com.example.lenote.ink.LassoSelectionHelper
import com.example.lenote.ink.PartialStrokeEraser
import com.example.lenote.ink.SamplePoint
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class Phase2To5UnitTest {

    @Test
    fun pageStrokeStorage_atomicSaveLoadAndDelete_worksCorrectly() {
        val tempDir = Files.createTempDirectory("lenote_test_storage").toFile()
        try {
            val storage = PageStrokeStorage(tempDir)
            val pageId = "page_unit_123"
            val sampleBytes = byteArrayOf(1, 2, 3, 4, 5, 42, 99)

            assertFalse(storage.getStrokeFile(pageId).exists())
            storage.savePageStrokesAtomic(pageId, sampleBytes)
            assertTrue(storage.getStrokeFile(pageId).exists())

            val loaded = storage.loadPageStrokes(pageId)
            assertArrayEquals(sampleBytes, loaded)

            storage.deletePageFiles(pageId)
            assertFalse(storage.getStrokeFile(pageId).exists())
            assertEquals(0, storage.loadPageStrokes(pageId).size)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun lassoSelectionHelper_rayCastingPointInPolygon_detectsInsideAndOutside() {
        val squarePolygon = listOf(
            Pair(100f, 100f),
            Pair(300f, 100f),
            Pair(300f, 300f),
            Pair(100f, 300f)
        )

        assertTrue(LassoSelectionHelper.isPointInPolygon(200f, 200f, squarePolygon))
        assertTrue(LassoSelectionHelper.isPointInPolygon(150f, 250f, squarePolygon))
        assertFalse(LassoSelectionHelper.isPointInPolygon(50f, 200f, squarePolygon))
        assertFalse(LassoSelectionHelper.isPointInPolygon(350f, 200f, squarePolygon))
    }

    @Test
    fun partialStrokeEraser_analyticalCapsuleClip_splitsSampleChainInSinglePass() {
        val points = (0..20).map { i ->
            SamplePoint(
                x = 100f + i * 10f, // 100..300
                y = 200f,
                elapsedTimeMillis = i * 5L,
                toolType = InputToolType.STYLUS,
                strokeUnitLengthCm = 0f,
                hasPressure = true,
                pressure = 0.6f,
                hasTilt = false,
                tiltRadians = 0f,
                hasOrientation = false,
                orientationRadians = 0f
            )
        }

        // Corta verticalmente em x = 200 com raio efetivo 18
        val (modified, chains) = PartialStrokeEraser.clipSampleChainByCapsule(
            samples = points,
            x0 = 200f,
            y0 = 150f,
            x1 = 200f,
            y1 = 250f,
            effectiveRadius = 18f
        )

        assertTrue(modified)
        assertEquals(2, chains.size)
        assertTrue(chains[0].last().x <= 182.1f)
        assertTrue(chains[1].first().x >= 217.9f)
    }

    @Test
    fun pageExporter_sanitizeFileName_and_generateExportFileName_formatsCorrectly() {
        val sanitized = com.example.lenote.export.PageExporter.sanitizeFileName("Página 1")
        assertEquals("Pagina_1", sanitized)

        val pngName = com.example.lenote.export.PageExporter.generateExportFileName("Página 1", "png")
        assertTrue(pngName.startsWith("LeNote_Pagina_1_"))
        assertTrue(pngName.endsWith(".png"))

        val pdfName = com.example.lenote.export.PageExporter.generateExportFileName("Página 1", "pdf")
        assertTrue(pdfName.startsWith("LeNote_Pagina_1_"))
        assertTrue(pdfName.endsWith(".pdf"))
    }

    @Test
    fun pageExporter_computePageWorldViewport_standardBounds() {
        val defaultViewport = com.example.lenote.export.PageExporter.computePageWorldViewport(emptyList())
        assertEquals(0f, defaultViewport[0], 0.001f)
        assertEquals(0f, defaultViewport[1], 0.001f)
        assertEquals(1200f, defaultViewport[2], 0.001f)
        assertEquals(1600f, defaultViewport[3], 0.001f)
    }

    @Test
    fun inkingEngine_gestureInputState_hasSingleFingerPan() {
        val state = com.example.lenote.ink.GestureInputState.SINGLE_FINGER_PAN
        assertEquals("SINGLE_FINGER_PAN", state.name)
    }

    @Test
    fun exportResult_properties_forPdfAndPng_correctMimeTypeAndDisplayPath() {
        val tempFilePdf = java.io.File.createTempFile("test_export", ".pdf")
        try {
            val pdfResult = com.example.lenote.export.ExportResult(
                localFile = tempFilePdf,
                uri = null,
                fileName = "LeNote_Pagina_1.pdf"
            )
            assertEquals("application/pdf", pdfResult.mimeType)
            assertEquals("Download/LeNote/LeNote_Pagina_1.pdf", pdfResult.displayPath)
            assertEquals("PDF exportado com sucesso", pdfResult.userMessage)
            assertTrue(pdfResult.isSuccess)
        } finally {
            tempFilePdf.delete()
        }

        val tempFilePng = java.io.File.createTempFile("test_export", ".png")
        try {
            val pngResult = com.example.lenote.export.ExportResult(
                localFile = tempFilePng,
                uri = null,
                fileName = "LeNote_Pagina_1.png"
            )
            assertEquals("image/png", pngResult.mimeType)
            assertEquals("Pictures/LeNote/LeNote_Pagina_1.png", pngResult.displayPath)
            assertEquals("Imagem exportada com sucesso", pngResult.userMessage)
            assertTrue(pngResult.isSuccess)
        } finally {
            tempFilePng.delete()
        }
    }
}

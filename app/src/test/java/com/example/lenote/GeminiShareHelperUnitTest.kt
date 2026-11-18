package com.example.lenote

import com.example.lenote.export.GeminiPromptAction
import com.example.lenote.export.GeminiShareHelper
import com.example.lenote.ink.SelectionBoundsBox
import com.example.lenote.ui.i18n.ResolvedLanguage
import com.example.lenote.ui.i18n.stringsForLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiShareHelperUnitTest {

    @Test
    fun buildPrompt_returnsNullForFreeAndLocalizedPromptsForOtherActions() {
        for (lang in ResolvedLanguage.entries) {
            val s = stringsForLanguage(lang)
            assertNull(GeminiShareHelper.buildPrompt(GeminiPromptAction.FREE, s, isFullPage = false))
            assertNull(GeminiShareHelper.buildPrompt(GeminiPromptAction.FREE, s, isFullPage = true))

            for (action in listOf(GeminiPromptAction.EXPLAIN, GeminiPromptAction.SOLVE, GeminiPromptAction.SUMMARIZE)) {
                val selPrompt = GeminiShareHelper.buildPrompt(action, s, isFullPage = false)
                val pagePrompt = GeminiShareHelper.buildPrompt(action, s, isFullPage = true)
                assertNotNull(selPrompt)
                assertNotNull(pagePrompt)
                assertTrue(selPrompt!!.isNotBlank())
                assertTrue(pagePrompt!!.isNotBlank())
                assertNotEquals(selPrompt, pagePrompt)
            }
        }
    }

    @Test
    fun computeSelectionCropViewport_addsSymmetricPaddingAroundBounds() {
        val bounds = SelectionBoundsBox(xMin = 100f, yMin = 200f, xMax = 400f, yMax = 500f)
        val vp = GeminiShareHelper.computeSelectionCropViewport(bounds)

        assertEquals(4, vp.size)
        assertTrue(vp[0] < bounds.xMin)
        assertTrue(vp[1] < bounds.yMin)
        assertTrue(vp[2] > bounds.xMax)
        assertTrue(vp[3] > bounds.yMax)

        val padLeft = bounds.xMin - vp[0]
        val padRight = vp[2] - bounds.xMax
        val padTop = bounds.yMin - vp[1]
        val padBottom = vp[3] - bounds.yMax
        assertEquals(padLeft, padRight, 0.01f)
        assertEquals(padTop, padBottom, 0.01f)
        assertTrue(padLeft >= 36f)
    }

    @Test
    fun computeCropBitmapSize_preservesAspectRatioAndClampsWithinBounds() {
        val (w1, h1) = GeminiShareHelper.computeCropBitmapSize(600f, 300f)
        assertEquals(1440, w1)
        assertEquals(720, h1)

        val (w2, h2) = GeminiShareHelper.computeCropBitmapSize(20f, 10f)
        assertTrue(w2 in 128..2048)
        assertTrue(h2 in 128..2048)
        assertTrue(w2 > h2)

        val (w3, h3) = GeminiShareHelper.computeCropBitmapSize(5000f, 100f)
        assertTrue(w3 in 128..2048)
        assertTrue(h3 in 128..2048)
    }
}

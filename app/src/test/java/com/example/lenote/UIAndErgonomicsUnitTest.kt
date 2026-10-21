package com.example.lenote

import com.example.lenote.data.AppPreferences
import com.example.lenote.data.ThemeMode
import com.example.lenote.data.ToolbarDockEdge
import com.example.lenote.ink.ColorInversionHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class UIAndErgonomicsUnitTest {

    @Test
    fun colorInversionHelper_invertsBlackToLightGray() {
        val pureBlack = 0xFF000000.toInt()
        val inverted = ColorInversionHelper.invertColorForDarkMode(pureBlack)

        // Deve ser claro (RGB >= 200)
        val r = (inverted shr 16) and 0xFF
        val g = (inverted shr 8) and 0xFF
        val b = inverted and 0xFF

        assertTrue(r >= 200)
        assertTrue(g >= 200)
        assertTrue(b >= 200)
    }

    @Test
    fun colorInversionHelper_preservesVibrantColors() {
        val vibrantRed = 0xFFE53935.toInt()
        val result = ColorInversionHelper.invertColorForDarkMode(vibrantRed)

        val r = (result shr 16) and 0xFF
        val g = (result shr 8) and 0xFF

        assertTrue(r > 200)
        assertTrue(g < 100)
    }

    @Test
    fun colorInversionHelper_paperBackgroundColor_differsForDarkAndLight() {
        val lightBg = ColorInversionHelper.getPaperBackgroundColor(false)
        val darkBg = ColorInversionHelper.getPaperBackgroundColor(true)

        assertNotEquals(lightBg, darkBg)
        assertEquals(0xFF121418.toInt(), darkBg)
    }
}

package com.example.lenote

import com.example.lenote.ink.EraserSize
import com.example.lenote.ink.PageBackground
import com.example.lenote.ink.SmoothingLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmoothingConfigurationUnitTest {
    @Test
    fun defaultSmoothingLevel_isLow() {
        val defaultState = com.example.lenote.ink.InkingState()
        assertEquals(SmoothingLevel.LOW, defaultState.smoothingLevel)
        assertEquals(listOf(SmoothingLevel.OFF, SmoothingLevel.LOW, SmoothingLevel.MEDIUM), SmoothingLevel.entries)
    }

    @Test
    fun eraserSize_and_pageBackground_defaultsAndOrdering() {
        val defaultState = com.example.lenote.ink.InkingState()
        assertEquals(EraserSize.MEDIUM, defaultState.eraserSize)
        assertEquals(PageBackground.BLANK, defaultState.pageBackground)
        assertTrue(EraserSize.SMALL.worldRadius < EraserSize.MEDIUM.worldRadius)
        assertTrue(EraserSize.MEDIUM.worldRadius < EraserSize.LARGE.worldRadius)
        assertEquals(
            listOf(
                PageBackground.BLANK,
                PageBackground.RULED,
                PageBackground.GRID,
                PageBackground.DOTS,
                PageBackground.MATHEMATICS
            ),
            PageBackground.entries
        )
    }
}

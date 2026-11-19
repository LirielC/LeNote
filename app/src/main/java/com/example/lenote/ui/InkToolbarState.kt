package com.example.lenote.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import com.example.lenote.ink.HighlighterSize
import com.example.lenote.ink.InkingState
import com.example.lenote.ink.InkingTool
import com.example.lenote.ink.PenStrokeSize

internal enum class ContextualPanelType {
    NONE,
    PEN,
    HIGHLIGHTER,
    ERASER
}

@Stable
internal class InkToolbarState(
    initialPenColor: Int,
    initialPenSize: Float,
    initialHighlighterColor: Int,
    initialHighlighterSize: Float
) {
    var penColor by mutableIntStateOf(initialPenColor)
    var penSize by mutableFloatStateOf(initialPenSize)
    var highlighterColor by mutableIntStateOf(initialHighlighterColor)
    var highlighterSize by mutableFloatStateOf(initialHighlighterSize)
    var activePanel by mutableStateOf(ContextualPanelType.NONE)
}

@Composable
internal fun rememberInkToolbarState(state: InkingState): InkToolbarState = remember {
    InkToolbarState(
        initialPenColor = (state.currentTool as? InkingTool.Pen)?.colorInt
            ?: 0xFF141413.toInt(),
        initialPenSize = (state.currentTool as? InkingTool.Pen)?.size
            ?: PenStrokeSize.MEDIUM.sizeWorld,
        initialHighlighterColor = (state.currentTool as? InkingTool.Highlighter)?.colorInt
            ?: 0x66FDD835,
        initialHighlighterSize = (state.currentTool as? InkingTool.Highlighter)?.size
            ?: HighlighterSize.MEDIUM.sizeWorld
    )
}

internal val inkPenColors = listOf(
    Color(0xFF141413),
    Color(0xFFFFFFFF),
    Color(0xFF1B365D),
    Color(0xFFB8332A),
    Color(0xFF1F5E3B),
    Color(0xFF5B3278),
    Color(0xFFC86D24)
)

internal val inkHighlighterColors = listOf(
    Color(0x66FDD835),
    Color(0x664CAF50),
    Color(0x6629B6F6),
    Color(0x66EC407A),
    Color(0x66FFA726),
    Color(0x66AB47BC)
)

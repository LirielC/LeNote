package com.example.lenote.ui

import com.example.lenote.data.CanvasImageBlock
import com.example.lenote.data.CanvasTextBlock
import com.example.lenote.data.PageEntity

internal data class InkEditorCanvasUiState(
    val currentPage: PageEntity?,
    val currentPageIndex: Int,
    val pageCount: Int,
    val isSaving: Boolean,
    val saveStatus: String?,
    val savedStrokesData: ByteArray?,
    val isSidebarOpen: Boolean,
    val isDiagnosticsVisible: Boolean,
    val isTextToolActive: Boolean,
    val zoomLevel: Float,
    val isNavigationLocked: Boolean,
    val textBlocks: List<CanvasTextBlock>,
    val imageBlocks: List<CanvasImageBlock>
)

package com.example.lenote.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Stable
internal class InkEditorDialogState {
    var showRenamePageDialog by mutableStateOf(false)
    var renamePageInput by mutableStateOf("")
    var showSheetOptionsModal by mutableStateOf(false)
    var showTemplatePickerDialog by mutableStateOf(false)
    var showPageActionsModal by mutableStateOf(false)
    var showPageGeminiDialog by mutableStateOf(false)
    var showClearConfirmDialog by mutableStateOf(false)
    var showDeletePageConfirmDialog by mutableStateOf(false)
    var exportConfirmation by mutableStateOf<ExportConfirmationState?>(null)
}

@Composable
internal fun rememberInkEditorDialogState(): InkEditorDialogState = remember {
    InkEditorDialogState()
}

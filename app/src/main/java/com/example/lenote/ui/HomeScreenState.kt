package com.example.lenote.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity
import com.example.lenote.ui.theme.EditorialFeedbackBannerState

@Stable
internal class HomeScreenState {
    var selectedTab by mutableIntStateOf(0)
    var notebookToDeletePermanently by mutableStateOf<NotebookEntity?>(null)
    var pageToDeletePermanently by mutableStateOf<PageEntity?>(null)
    var showEmptyTrashConfirmDialog by mutableStateOf(false)
    var showLanguagePickerDialog by mutableStateOf(false)
    var feedbackBanner by mutableStateOf<EditorialFeedbackBannerState?>(null)
    var searchQuery by mutableStateOf("")
    var showCreateDialog by mutableStateOf(false)
}

@Composable
internal fun rememberHomeScreenState(): HomeScreenState = remember { HomeScreenState() }

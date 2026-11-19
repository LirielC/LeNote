package com.example.lenote.ui

import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity

internal data class HomeUiModel(
    val notebooks: List<NotebookEntity>,
    val allPages: List<PageEntity>,
    val favoritePages: List<PageEntity>,
    val deletedNotebooks: List<NotebookEntity>,
    val deletedPages: List<PageEntity>,
    val recentPages: List<PageEntity>,
    val filteredNotebooks: List<NotebookEntity>,
    val filteredFavorites: List<PageEntity>,
    val thumbnailVersion: Long,
    val isDarkMode: Boolean
)

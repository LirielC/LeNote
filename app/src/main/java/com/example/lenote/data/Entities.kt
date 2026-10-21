package com.example.lenote.data

import com.example.lenote.ink.PageBackground

/**
 * Representa um Caderno (Notebook) na hierarquia estilo Microsoft OneNote.
 */
data class NotebookEntity(
    val id: String,
    val title: String,
    val colorHex: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Int,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

/**
 * Representa uma Seção / Aba (Section) pertencente a um [NotebookEntity].
 */
data class SectionEntity(
    val id: String,
    val notebookId: String,
    val title: String,
    val colorHex: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Int
)

/**
 * Representa uma Página (Page) pertencente a uma [SectionEntity], armazenando seus metadados,
 * tipo de folha, estado de câmera (Pan/Zoom) e caminho do arquivo binário `.ink` de strokes.
 */
data class PageEntity(
    val id: String,
    val sectionId: String,
    val title: String,
    val pageBackground: PageBackground = PageBackground.BLANK,
    val paperColor: Long = 0xFFFFFFFF,
    val zoom: Float = 1.0f,
    val panX: Float = 0f,
    val panY: Float = 0f,
    val strokesFilePath: String,
    val thumbnailPath: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long,
    val sortOrder: Int,
    val pdfPath: String? = null,
    val pdfPageIndex: Int? = null,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null
)

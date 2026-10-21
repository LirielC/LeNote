package com.example.lenote.data

import android.graphics.Bitmap
import com.example.lenote.ink.PageBackground
import kotlinx.coroutines.flow.Flow
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Repositório central do LeNote unificando [LeNoteDatabase] e [PageStrokeStorage].
 */
class NoteRepository(
    val database: LeNoteDatabase,
    val strokeStorage: PageStrokeStorage
) {

    fun observeNotebooks(): Flow<List<NotebookEntity>> = database.observeNotebooks()
    fun observeDeletedNotebooks(): Flow<List<NotebookEntity>> = database.observeDeletedNotebooks()
    fun observeSections(notebookId: String): Flow<List<SectionEntity>> = database.observeSectionsByNotebook(notebookId)
    fun observePages(sectionId: String): Flow<List<PageEntity>> = database.observePagesBySection(sectionId)
    fun observeAllPages(): Flow<List<PageEntity>> = database.observeAllPages()
    fun observeFavoritePages(): Flow<List<PageEntity>> = database.observeFavoritePages()
    fun observeDeletedPages(): Flow<List<PageEntity>> = database.observeDeletedPages()
    fun searchPages(query: String): Flow<List<PageEntity>> = database.searchPages(query)
    fun indexPageHandwriting(pageId: String, text: String, boxesJson: String) = database.indexPageHandwriting(pageId, text, boxesJson)
    fun searchHandwriting(query: String): List<HandwritingSearchResult> = database.searchHandwriting(query)

    /**
     * Garante que exista ao menos 1 Caderno, 1 Seção e 1 Página no primeiro uso do app,
     * retornando a página inicial pronta para o editor.
     */
    @Synchronized
    fun ensureDefaultHierarchy(): Triple<NotebookEntity, SectionEntity, PageEntity> {
        var notebooks = database.getAllNotebooks()
        val firstNotebook = if (notebooks.isEmpty()) {
            createNotebook(title = "Meu Caderno", colorHex = 0xFF1976D2.toInt())
        } else {
            notebooks.first()
        }

        val sections = database.getSectionsByNotebook(firstNotebook.id)
        val firstSection = if (sections.isEmpty()) {
            createSection(notebookId = firstNotebook.id, title = "Notas Rápidas", colorHex = 0xFF00897B.toInt())
        } else {
            sections.first()
        }

        val pages = database.getPagesBySection(firstSection.id)
        val firstPage = if (pages.isEmpty()) {
            createPage(
                sectionId = firstSection.id,
                title = "Página 1",
                pageBackground = PageBackground.RULED
            )
        } else {
            pages.first()
        }

        return Triple(firstNotebook, firstSection, firstPage)
    }

    @Synchronized
    fun createNotebook(title: String, colorHex: Int = 0xFF1976D2.toInt()): NotebookEntity {
        val now = System.currentTimeMillis()
        val nextOrder = (database.getAllNotebooks().maxOfOrNull { it.sortOrder } ?: -1) + 1
        val notebook = NotebookEntity(
            id = UUID.randomUUID().toString(),
            title = title.trim().ifEmpty { "Novo Caderno" },
            colorHex = colorHex,
            createdAt = now,
            updatedAt = now,
            sortOrder = nextOrder
        )
        database.upsertNotebook(notebook)
        return notebook
    }

    @Synchronized
    fun renameNotebook(notebookId: String, newTitle: String) {
        val current = database.getNotebookById(notebookId) ?: return
        database.upsertNotebook(
            current.copy(
                title = newTitle.trim().ifEmpty { current.title },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    @Synchronized
    fun updateNotebookColor(notebookId: String, newColorHex: Int) {
        val current = database.getNotebookById(notebookId) ?: return
        database.upsertNotebook(
            current.copy(
                colorHex = newColorHex,
                updatedAt = System.currentTimeMillis()
            )
        )
    }


    // --- Sistema de Lixeira (Soft Delete & Restauração) ---

    @Synchronized
    fun moveToTrash(notebookId: String) {
        val nb = database.getNotebookById(notebookId) ?: return
        val now = System.currentTimeMillis()
        database.upsertNotebook(nb.copy(isDeleted = true, deletedAt = now))

        // Marca todas as páginas do caderno como excluídas também
        val sections = database.getSectionsByNotebook(notebookId)
        for (sec in sections) {
            val pages = database.getPagesBySection(sec.id)
            for (p in pages) {
                database.upsertPage(p.copy(isDeleted = true, deletedAt = now))
            }
        }
    }

    @Synchronized
    fun restoreFromTrash(notebookId: String) {
        val nb = database.getNotebookById(notebookId) ?: return
        database.upsertNotebook(nb.copy(isDeleted = false, deletedAt = null))

        val sections = database.getSectionsByNotebook(notebookId)
        for (sec in sections) {
            val pages = database.getAllPagesInDb().filter { it.sectionId == sec.id }
            for (p in pages) {
                database.upsertPage(p.copy(isDeleted = false, deletedAt = null))
            }
        }
    }

    @Synchronized
    fun movePageToTrash(pageId: String) {
        val page = database.getPageById(pageId) ?: return
        val now = System.currentTimeMillis()
        database.upsertPage(page.copy(isDeleted = true, deletedAt = now))
    }

    @Synchronized
    fun restorePageFromTrash(pageId: String) {
        val page = database.getPageById(pageId) ?: return
        database.upsertPage(page.copy(isDeleted = false, deletedAt = null))

        // Se o caderno pai estiver na lixeira, restaura também
        val sec = database.getSectionById(page.sectionId)
        if (sec != null) {
            val nb = database.getNotebookById(sec.notebookId)
            if (nb != null && nb.isDeleted) {
                database.upsertNotebook(nb.copy(isDeleted = false, deletedAt = null))
            }
        }
    }

    @Synchronized
    fun permanentlyDeleteNotebook(notebookId: String) {
        deleteNotebook(notebookId)
    }

    @Synchronized
    fun permanentlyDeletePage(pageId: String) {
        deletePage(pageId)
    }

    @Synchronized
    fun emptyTrash() {
        val deletedNotebooks = database.getDeletedNotebooks()
        for (nb in deletedNotebooks) {
            deleteNotebook(nb.id)
        }
        val deletedPages = database.getDeletedPages()
        for (p in deletedPages) {
            deletePage(p.id)
        }
    }

    @Synchronized
    fun deleteNotebook(notebookId: String) {
        val sections = database.getSectionsByNotebook(notebookId)
        for (sec in sections) {
            val pages = database.getAllPagesInDb().filter { it.sectionId == sec.id }
            for (page in pages) {
                strokeStorage.deletePageFiles(page.id)
            }
        }
        database.deleteNotebook(notebookId)
    }

    @Synchronized
    fun createSection(notebookId: String, title: String, colorHex: Int = 0xFF00897B.toInt()): SectionEntity {
        val now = System.currentTimeMillis()
        val nextOrder = (database.getSectionsByNotebook(notebookId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        val section = SectionEntity(
            id = UUID.randomUUID().toString(),
            notebookId = notebookId,
            title = title.trim().ifEmpty { "Nova Seção" },
            colorHex = colorHex,
            createdAt = now,
            updatedAt = now,
            sortOrder = nextOrder
        )
        database.upsertSection(section)
        return section
    }

    @Synchronized
    fun renameSection(sectionId: String, newTitle: String) {
        val current = database.getSectionById(sectionId) ?: return
        database.upsertSection(
            current.copy(
                title = newTitle.trim().ifEmpty { current.title },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    @Synchronized
    fun updateSectionColor(sectionId: String, colorHex: Int) {
        val current = database.getSectionById(sectionId) ?: return
        database.upsertSection(
            current.copy(
                colorHex = colorHex,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    @Synchronized
    fun deleteSection(sectionId: String) {
        val pages = database.getPagesBySection(sectionId)
        for (page in pages) {
            strokeStorage.deletePageFiles(page.id)
        }
        database.deleteSection(sectionId)
    }

    @Synchronized
    fun createPage(
        sectionId: String,
        title: String,
        pageBackground: PageBackground = PageBackground.BLANK,
        paperColor: Long = 0xFFFFFFFF,
        pdfPath: String? = null,
        pdfPageIndex: Int? = null
    ): PageEntity {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val nextOrder = (database.getPagesBySection(sectionId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        val strokeFilePath = strokeStorage.getStrokeFile(id).absolutePath
        val page = PageEntity(
            id = id,
            sectionId = sectionId,
            title = title.trim().ifEmpty { "Nova Página" },
            pageBackground = pageBackground,
            paperColor = paperColor,
            zoom = 1.0f,
            panX = 0f,
            panY = 0f,
            strokesFilePath = strokeFilePath,
            thumbnailPath = null,
            isFavorite = false,
            createdAt = now,
            updatedAt = now,
            sortOrder = nextOrder,
            pdfPath = pdfPath,
            pdfPageIndex = pdfPageIndex
        )
        database.upsertPage(page)
        return page
    }

    @Synchronized
    fun updatePageTemplate(pageId: String, background: PageBackground, paperColor: Long) {
        val current = database.getPageById(pageId) ?: return
        database.upsertPage(
            current.copy(
                pageBackground = background,
                paperColor = paperColor,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Cria um novo caderno a partir de um arquivo PDF importado, gerando uma página para cada página do PDF.
     */
    @Synchronized
    fun importPdfDocument(
        title: String,
        pdfFile: File,
        pageCount: Int,
        colorHex: Int = 0xFFBE123C.toInt()
    ): Triple<NotebookEntity, SectionEntity, List<PageEntity>> {
        val notebook = createNotebook(title = title.ifBlank { pdfFile.nameWithoutExtension }, colorHex = colorHex)
        val section = createSection(notebookId = notebook.id, title = "Documento")
        val pages = mutableListOf<PageEntity>()

        val totalPages = if (pageCount > 0) pageCount else 1
        for (pageIdx in 0 until totalPages) {
            val pageTitle = "Página ${pageIdx + 1}"
            val page = createPage(
                sectionId = section.id,
                title = pageTitle,
                pageBackground = PageBackground.BLANK,
                pdfPath = pdfFile.absolutePath,
                pdfPageIndex = pageIdx
            )
            pages.add(page)
        }
        return Triple(notebook, section, pages)
    }

    @Synchronized
    fun renamePage(pageId: String, newTitle: String) {
        val current = database.getPageById(pageId) ?: return
        database.upsertPage(
            current.copy(
                title = newTitle.trim().ifEmpty { current.title },
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    @Synchronized
    fun togglePageFavorite(pageId: String): Boolean {
        val current = database.getPageById(pageId) ?: return false
        val nextFav = !current.isFavorite
        database.upsertPage(
            current.copy(
                isFavorite = nextFav,
                updatedAt = System.currentTimeMillis()
            )
        )
        return nextFav
    }

    @Synchronized
    fun movePageOrder(pageId: String, moveUp: Boolean) {
        val page = database.getPageById(pageId) ?: return
        val siblings = database.getPagesBySection(page.sectionId).toMutableList()
        val idx = siblings.indexOfFirst { it.id == pageId }
        if (idx == -1) return
        val targetIdx = if (moveUp) idx - 1 else idx + 1
        if (targetIdx !in siblings.indices) return

        val other = siblings[targetIdx]
        val now = System.currentTimeMillis()
        database.upsertPage(page.copy(sortOrder = other.sortOrder, updatedAt = now))
        database.upsertPage(other.copy(sortOrder = page.sortOrder, updatedAt = now))
    }

    @Synchronized
    fun reorderPages(sectionId: String, fromIndex: Int, toIndex: Int) {
        val siblings = database.getPagesBySection(sectionId).toMutableList()
        if (fromIndex !in siblings.indices || toIndex !in siblings.indices || fromIndex == toIndex) return
        val item = siblings.removeAt(fromIndex)
        siblings.add(toIndex, item)
        val now = System.currentTimeMillis()
        siblings.forEachIndexed { index, p ->
            if (p.sortOrder != index) {
                database.upsertPage(p.copy(sortOrder = index, updatedAt = now))
            }
        }
    }


    // --- Gerenciamento de Mídia Rica (Texto e Imagem) ---

    @Synchronized
    fun getTextBlocks(pageId: String): List<CanvasTextBlock> = database.getTextBlocksByPage(pageId)

    @Synchronized
    fun getImageBlocks(pageId: String): List<CanvasImageBlock> = database.getImageBlocksByPage(pageId)

    @Synchronized
    fun upsertTextBlock(block: CanvasTextBlock) = database.upsertTextBlock(block)

    @Synchronized
    fun deleteTextBlock(id: String) = database.deleteTextBlock(id)

    @Synchronized
    fun upsertImageBlock(block: CanvasImageBlock) = database.upsertImageBlock(block)

    @Synchronized
    fun deleteImageBlock(id: String) = database.deleteImageBlock(id)

    @Synchronized
    fun deletePage(pageId: String) {
        strokeStorage.deletePageFiles(pageId)
        database.deletePage(pageId)
    }

    @Synchronized
    fun duplicatePage(pageId: String): PageEntity? {
        val original = database.getPageById(pageId) ?: return null
        val now = System.currentTimeMillis()
        val newId = UUID.randomUUID().toString()
        val nextOrder = (database.getPagesBySection(original.sectionId).maxOfOrNull { it.sortOrder } ?: -1) + 1
        val strokeFilePath = strokeStorage.getStrokeFile(newId).absolutePath
        val strokeBytes = strokeStorage.loadPageStrokes(pageId)
        if (strokeBytes.isNotEmpty()) {
            strokeStorage.savePageStrokesAtomic(newId, strokeBytes)
        }
        val newPage = original.copy(
            id = newId,
            title = "${original.title} (Cópia)",
            strokesFilePath = strokeFilePath,
            thumbnailPath = null,
            isFavorite = false,
            createdAt = now,
            updatedAt = now,
            sortOrder = nextOrder
        )
        database.upsertPage(newPage)
        return newPage
    }

    /**
     * Salva o conteúdo binário dos strokes, estado da folha/câmera e miniatura opcional da página.
     */
    @Synchronized
    fun savePageContent(
        pageId: String,
        strokesBytes: ByteArray,
        pageBackground: PageBackground,
        zoom: Float,
        panX: Float,
        panY: Float,
        thumbnailBitmap: Bitmap? = null,
        paperColor: Long? = null
    ): PageEntity? {
        val current = database.getPageById(pageId) ?: return null
        val savedPath = strokeStorage.savePageStrokesAtomic(pageId, strokesBytes)
        val thumbPath = if (thumbnailBitmap != null) {
            strokeStorage.savePageThumbnailAtomic(pageId, thumbnailBitmap)
        } else {
            current.thumbnailPath
        }
        val updated = current.copy(
            pageBackground = pageBackground,
            paperColor = paperColor ?: current.paperColor,
            zoom = zoom,
            panX = panX,
            panY = panY,
            strokesFilePath = savedPath,
            thumbnailPath = thumbPath,
            updatedAt = System.currentTimeMillis()
        )
        database.upsertPage(updated)
        return updated
    }

    @Synchronized
    fun savePageStrokes(pageId: String, strokesBytes: ByteArray) {
        strokeStorage.savePageStrokesAtomic(pageId, strokesBytes)
    }

    @Synchronized
    fun loadPageStrokes(pageId: String): ByteArray {
        return strokeStorage.loadPageStrokes(pageId)
    }

    /**
     * Exporta todo o banco de dados (Cadernos, Seções, Páginas) e todos os arquivos binários `.ink`
     * em um único arquivo pacote `.lenote` (formato ZIP com manifesto binário estruturado).
     */
    @Synchronized
    fun exportBackupLenotePackage(targetFile: File): File {
        targetFile.parentFile?.mkdirs()
        val notebooks = database.getAllNotebooks()
        val sections = database.getAllSections()
        val pages = database.getAllPages()

        ZipOutputStream(FileOutputStream(targetFile)).use { zos ->
            // 1. Entrada de manifesto contendo todos os metadados
            zos.putNextEntry(ZipEntry("manifest.bin"))
            val manifestBytes = ByteArrayOutputStream().also { bos ->
                val out = DataOutputStream(bos)
                out.writeInt(1) // Versão do pacote .lenote

                out.writeInt(notebooks.size)
                for (nb in notebooks) {
                    out.writeUTF(nb.id)
                    out.writeUTF(nb.title)
                    out.writeInt(nb.colorHex)
                    out.writeLong(nb.createdAt)
                    out.writeLong(nb.updatedAt)
                    out.writeInt(nb.sortOrder)
                }

                out.writeInt(sections.size)
                for (sec in sections) {
                    out.writeUTF(sec.id)
                    out.writeUTF(sec.notebookId)
                    out.writeUTF(sec.title)
                    out.writeInt(sec.colorHex)
                    out.writeLong(sec.createdAt)
                    out.writeLong(sec.updatedAt)
                    out.writeInt(sec.sortOrder)
                }

                out.writeInt(pages.size)
                for (pg in pages) {
                    out.writeUTF(pg.id)
                    out.writeUTF(pg.sectionId)
                    out.writeUTF(pg.title)
                    out.writeUTF(pg.pageBackground.name)
                    out.writeFloat(pg.zoom)
                    out.writeFloat(pg.panX)
                    out.writeFloat(pg.panY)
                    out.writeBoolean(pg.isFavorite)
                    out.writeLong(pg.createdAt)
                    out.writeLong(pg.updatedAt)
                    out.writeInt(pg.sortOrder)
                }
                out.flush()
            }.toByteArray()
            zos.write(manifestBytes)
            zos.closeEntry()

            // 2. Entradas para cada arquivo .ink existente
            for (pg in pages) {
                val strokeBytes = strokeStorage.loadPageStrokes(pg.id)
                if (strokeBytes.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("pages/${pg.id}.ink"))
                    zos.write(strokeBytes)
                    zos.closeEntry()
                }
            }
        }
        return targetFile
    }

    /**
     * Importa um pacote `.lenote` (ZIP) restaurando cadernos, seções, páginas e arquivos `.ink`.
     */
    @Synchronized
    fun importBackupLenotePackage(sourceFile: File): Int {
        if (!sourceFile.exists()) return 0
        var manifestData: ByteArray? = null
        val strokeBlobs = HashMap<String, ByteArray>()

        ZipInputStream(FileInputStream(sourceFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                val bytes = zis.readBytes()
                if (name == "manifest.bin") {
                    manifestData = bytes
                } else if (name.startsWith("pages/") && name.endsWith(".ink")) {
                    val pageId = name.removePrefix("pages/").removeSuffix(".ink")
                    strokeBlobs[pageId] = bytes
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val manifest = manifestData ?: return 0
        val din = DataInputStream(ByteArrayInputStream(manifest))
        val version = din.readInt()
        if (version < 1) return 0

        val nbCount = din.readInt()
        for (i in 0 until nbCount) {
            val nb = NotebookEntity(
                id = din.readUTF(),
                title = din.readUTF(),
                colorHex = din.readInt(),
                createdAt = din.readLong(),
                updatedAt = din.readLong(),
                sortOrder = din.readInt()
            )
            database.upsertNotebook(nb)
        }

        val secCount = din.readInt()
        for (i in 0 until secCount) {
            val sec = SectionEntity(
                id = din.readUTF(),
                notebookId = din.readUTF(),
                title = din.readUTF(),
                colorHex = din.readInt(),
                createdAt = din.readLong(),
                updatedAt = din.readLong(),
                sortOrder = din.readInt()
            )
            database.upsertSection(sec)
        }

        val pageCount = din.readInt()
        for (i in 0 until pageCount) {
            val id = din.readUTF()
            val sectionId = din.readUTF()
            val title = din.readUTF()
            val bgName = din.readUTF()
            val zoom = din.readFloat()
            val panX = din.readFloat()
            val panY = din.readFloat()
            val isFavorite = din.readBoolean()
            val createdAt = din.readLong()
            val updatedAt = din.readLong()
            val sortOrder = din.readInt()

            val bg = try {
                PageBackground.valueOf(bgName)
            } catch (_: Exception) {
                PageBackground.BLANK
            }

            val blob = strokeBlobs[id] ?: ByteArray(0)
            val savedPath = strokeStorage.savePageStrokesAtomic(id, blob)

            val pg = PageEntity(
                id = id,
                sectionId = sectionId,
                title = title,
                pageBackground = bg,
                zoom = zoom,
                panX = panX,
                panY = panY,
                strokesFilePath = savedPath,
                thumbnailPath = null,
                isFavorite = isFavorite,
                createdAt = createdAt,
                updatedAt = updatedAt,
                sortOrder = sortOrder
            )
            database.upsertPage(pg)
        }

        return pageCount
    }
}

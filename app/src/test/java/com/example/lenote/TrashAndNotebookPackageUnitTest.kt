package com.example.lenote

import com.example.lenote.data.CanvasImageBlock
import com.example.lenote.data.CanvasTextBlock
import com.example.lenote.data.LeNoteDatabase
import com.example.lenote.data.NoteRepository
import com.example.lenote.data.NotebookPackageExporter
import com.example.lenote.data.PageStrokeStorage
import com.example.lenote.ink.PageBackground
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Testes unitários para:
 * 1. Sistema de Lixeira (Soft Delete, Restauração e Exclusão Definitiva);
 * 2. Exportação e Importação de Cadernos no pacote .lenote (ZIP com metadata, strokes e mídias).
 */
class TrashAndNotebookPackageUnitTest {

    @Test
    fun trashSystem_notebookSoftDeleteAndRestore_movesAndRestoresCascadingPages() {
        val db = LeNoteDatabase.createInMemory()
        val tempDir = Files.createTempDirectory("lenote_trash_test").toFile()
        val storage = PageStrokeStorage(tempDir)
        val repo = NoteRepository(db, storage)

        val nb = repo.createNotebook("Caderno de Teste")
        val sec = repo.createSection(nb.id, "Seção 1")
        val p1 = repo.createPage(sec.id, "Página 1", PageBackground.RULED)
        val p2 = repo.createPage(sec.id, "Página 2", PageBackground.GRID)

        assertEquals(1, repo.database.getAllNotebooks().size)
        assertEquals(2, repo.database.getAllPages().size)

        // 1. Move caderno para a lixeira
        repo.moveToTrash(nb.id)

        // Consultas ativas filtram itens na lixeira
        val activeNotebooks = repo.database.getAllNotebooks()
        val activePages = repo.database.getAllPages()
        val deletedNotebooks = repo.database.getAllNotebooksInDb().filter { it.isDeleted }
        val deletedPages = repo.database.getAllPagesInDb().filter { it.isDeleted }

        assertEquals(0, activeNotebooks.size)
        assertEquals(0, activePages.size)
        assertEquals(1, deletedNotebooks.size)
        assertEquals(2, deletedPages.size)
        assertTrue(deletedNotebooks[0].isDeleted)
        assertNotNull(deletedNotebooks[0].deletedAt)

        // 2. Restaura o caderno da lixeira
        repo.restoreFromTrash(nb.id)

        val restoredNotebooks = repo.database.getAllNotebooks()
        val restoredPages = repo.database.getAllPages()
        assertEquals(1, restoredNotebooks.size)
        assertEquals(2, restoredPages.size)
        assertFalse(restoredNotebooks[0].isDeleted)
        assertNull(restoredNotebooks[0].deletedAt)
        assertFalse(restoredPages[0].isDeleted)
        assertNull(restoredPages[0].deletedAt)
    }

    @Test
    fun trashSystem_pageSoftDeleteAndRestore_worksIndependently() {
        val db = LeNoteDatabase.createInMemory()
        val tempDir = Files.createTempDirectory("lenote_page_trash_test").toFile()
        val storage = PageStrokeStorage(tempDir)
        val repo = NoteRepository(db, storage)

        val nb = repo.createNotebook("Caderno Misto")
        val sec = repo.createSection(nb.id, "Geral")
        val p1 = repo.createPage(sec.id, "Página Ativa", PageBackground.RULED)
        val p2 = repo.createPage(sec.id, "Página para Lixeira", PageBackground.BLANK)

        assertEquals(2, repo.database.getPagesBySection(sec.id).size)

        // Move apenas p2 para a lixeira
        repo.movePageToTrash(p2.id)

        val activeInSec = repo.database.getPagesBySection(sec.id)
        assertEquals(1, activeInSec.size)
        assertEquals(p1.id, activeInSec[0].id)

        val allPagesInDb = repo.database.getAllPagesInDb()
        val delPage = allPagesInDb.find { it.id == p2.id }
        assertNotNull(delPage)
        assertTrue(delPage!!.isDeleted)

        // Restaura p2
        repo.restorePageFromTrash(p2.id)
        val restoredInSec = repo.database.getPagesBySection(sec.id)
        assertEquals(2, restoredInSec.size)
    }

    @Test
    fun trashSystem_emptyTrash_removesDeletedRecordsAndAssociatedFiles() {
        val db = LeNoteDatabase.createInMemory()
        val tempDir = Files.createTempDirectory("lenote_empty_trash_test").toFile()
        val storage = PageStrokeStorage(tempDir)
        val repo = NoteRepository(db, storage)

        val nb = repo.createNotebook("Caderno Deletado")
        val sec = repo.createSection(nb.id, "Seção 1")
        val page = repo.createPage(sec.id, "Página Deletada", PageBackground.RULED)

        // Salva traços simulados na página
        repo.savePageStrokes(page.id, byteArrayOf(10, 20, 30, 40))
        val strokeFile = storage.getStrokeFile(page.id)
        assertTrue(strokeFile.exists())

        // Move para a lixeira e esvazia
        repo.moveToTrash(nb.id)
        repo.emptyTrash()

        assertEquals(0, repo.database.getAllNotebooksInDb().size)
        assertEquals(0, repo.database.getAllPagesInDb().size)
        assertFalse(strokeFile.exists())
    }

    @Test
    fun notebookPackageExporter_exportAndImport_restoresCompleteHierarchyStrokesAndMedia() = runBlocking {
        val db1 = LeNoteDatabase.createInMemory()
        val tempDir1 = Files.createTempDirectory("lenote_export_source").toFile()
        val storage1 = PageStrokeStorage(tempDir1)
        val repo1 = NoteRepository(db1, storage1)

        val nb = repo1.createNotebook("Física Quântica", 0xFF1E3A8A.toInt())
        val sec1 = repo1.createSection(nb.id, "Termodinâmica")
        val page1 = repo1.createPage(sec1.id, "Entropia e Leis", PageBackground.MATHEMATICS)

        // Salva traços
        val sampleStrokes = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        repo1.savePageStrokes(page1.id, sampleStrokes)

        // Adiciona bloco de texto
        val textBlock = CanvasTextBlock(
            id = "tb_1",
            pageId = page1.id,
            text = "dS >= dQ/T",
            x = 120f,
            y = 240f,
            width = 250f,
            height = 80f,
            textColor = 0xFF000000.toInt(),
            fontSize = 20f,
            zIndex = 1f
        )
        repo1.upsertTextBlock(textBlock)

        // Adiciona bloco de imagem
        val imagesDir = File(tempDir1, "images").apply { mkdirs() }
        val imgFile = File(imagesDir, "diagram.png")
        imgFile.writeBytes(byteArrayOf(99, 98, 97, 96))

        val imgBlock = CanvasImageBlock(
            id = "ib_1",
            pageId = page1.id,
            imagePath = imgFile.absolutePath,
            x = 50f,
            y = 50f,
            width = 200f,
            height = 200f,
            rotationDegrees = 0f,
            zIndex = 2f
        )
        repo1.upsertImageBlock(imgBlock)

        // 1. Exporta o pacote .lenote
        val packageFile = File(tempDir1, "fisica_quantica.lenote")
        val exportSuccess = NotebookPackageExporter.exportNotebookPackage(nb.id, repo1, packageFile)
        assertTrue("Exportação do pacote deve retornar true", exportSuccess)
        assertTrue("Arquivo .lenote gerado deve existir e ter tamanho > 0", packageFile.exists() && packageFile.length() > 0)

        // 2. Importa em um banco e diretório novos (dispositivo receptor)
        val db2 = LeNoteDatabase.createInMemory()
        val tempDir2 = Files.createTempDirectory("lenote_import_dest").toFile()
        val storage2 = PageStrokeStorage(tempDir2)
        val repo2 = NoteRepository(db2, storage2)

        val importedNb = NotebookPackageExporter.importNotebookPackage(packageFile, repo2, tempDir2)
        assertNotNull("Importação deve retornar o caderno criado", importedNb)
        assertEquals("Física Quântica", importedNb!!.title)
        assertEquals(0xFF1E3A8A.toInt(), importedNb.colorHex)

        // Verifica seções restauradas
        val importedSections = repo2.database.getSectionsByNotebook(importedNb.id)
        assertEquals(1, importedSections.size)
        assertEquals("Termodinâmica", importedSections[0].title)

        // Verifica páginas restauradas
        val importedPages = repo2.database.getPagesBySection(importedSections[0].id)
        assertEquals(1, importedPages.size)
        val impPage = importedPages[0]
        assertEquals("Entropia e Leis", impPage.title)
        assertEquals(PageBackground.MATHEMATICS, impPage.pageBackground)

        // Verifica traços restaurados
        val restoredStrokes = repo2.loadPageStrokes(impPage.id)
        assertTrue("Traços da página importada devem ser idênticos", sampleStrokes.contentEquals(restoredStrokes))

        // Verifica blocos de texto
        val restoredTextBlocks = repo2.getTextBlocks(impPage.id)
        assertEquals(1, restoredTextBlocks.size)
        assertEquals("dS >= dQ/T", restoredTextBlocks[0].text)

        // Verifica blocos de imagem
        val restoredImageBlocks = repo2.getImageBlocks(impPage.id)
        assertEquals(1, restoredImageBlocks.size)
        val restoredImg = restoredImageBlocks[0]
        val restoredFile = File(restoredImg.imagePath)
        assertTrue("Arquivo de imagem deve ter sido descompactado no diretório interno", restoredFile.exists())
        assertEquals(4, restoredFile.length())
    }
}

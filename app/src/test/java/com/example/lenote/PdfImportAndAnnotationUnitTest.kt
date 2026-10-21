package com.example.lenote

import com.example.lenote.data.LeNoteDatabase
import com.example.lenote.data.NoteRepository
import com.example.lenote.data.PageStrokeStorage
import com.example.lenote.ink.PageBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PdfImportAndAnnotationUnitTest {

    @Test
    fun databaseAndRepository_createsAndPersistsPdfMetadataCorrectly() {
        val db = LeNoteDatabase.createInMemory()
        val tempDir = Files.createTempDirectory("lenote_pdf_test_storage").toFile()
        val storage = PageStrokeStorage(tempDir)
        val repo = NoteRepository(db, storage)

        val nb = repo.createNotebook(title = "Caderno de Artigos")
        val sec = repo.createSection(notebookId = nb.id, title = "Leitura")

        // 1. Cria página normal (sem PDF)
        val normalPage = repo.createPage(
            sectionId = sec.id,
            title = "Página Normal",
            pageBackground = PageBackground.RULED
        )
        assertNull(normalPage.pdfPath)
        assertNull(normalPage.pdfPageIndex)

        val retrievedNormal = repo.database.getPageById(normalPage.id)
        assertNotNull(retrievedNormal)
        assertNull(retrievedNormal?.pdfPath)
        assertNull(retrievedNormal?.pdfPageIndex)

        // 2. Cria página anotada com PDF
        val mockPdfFile = File(tempDir, "sample_document.pdf")
        mockPdfFile.writeText("%PDF-1.4 Mock PDF Content")

        val pdfPage = repo.createPage(
            sectionId = sec.id,
            title = "Slide 1",
            pageBackground = PageBackground.BLANK,
            pdfPath = mockPdfFile.absolutePath,
            pdfPageIndex = 0
        )
        assertEquals(mockPdfFile.absolutePath, pdfPage.pdfPath)
        assertEquals(0, pdfPage.pdfPageIndex)

        val retrievedPdfPage = repo.database.getPageById(pdfPage.id)
        assertNotNull(retrievedPdfPage)
        assertEquals(mockPdfFile.absolutePath, retrievedPdfPage?.pdfPath)
        assertEquals(0, retrievedPdfPage?.pdfPageIndex)
        assertEquals(PageBackground.BLANK, retrievedPdfPage?.pageBackground)

        // 3. Atualização / Upsert de página preservando os metadados de PDF
        repo.renamePage(pdfPage.id, "Slide 1 - Introdução")
        val renamed = repo.database.getPageById(pdfPage.id)
        assertEquals("Slide 1 - Introdução", renamed?.title)
        assertEquals(mockPdfFile.absolutePath, renamed?.pdfPath)
        assertEquals(0, renamed?.pdfPageIndex)
    }

    @Test
    fun importPdfDocument_createsNotebookWithAllPages() {
        val db = LeNoteDatabase.createInMemory()
        val tempDir = Files.createTempDirectory("lenote_pdf_import_test").toFile()
        val storage = PageStrokeStorage(tempDir)
        val repo = NoteRepository(db, storage)

        val mockPdfFile = File(tempDir, "Apostila_Calculo.pdf")
        mockPdfFile.writeText("%PDF-1.4 Mock Book Content")

        val pageCount = 5
        val (notebook, section, pages) = repo.importPdfDocument(
            title = "Apostila de Cálculo",
            pdfFile = mockPdfFile,
            pageCount = pageCount
        )

        assertEquals("Apostila de Cálculo", notebook.title)
        assertEquals(notebook.id, section.notebookId)
        assertEquals("Documento", section.title)
        assertEquals(pageCount, pages.size)

        for (i in 0 until pageCount) {
            val page = pages[i]
            assertEquals("Página ${i + 1}", page.title)
            assertEquals(mockPdfFile.absolutePath, page.pdfPath)
            assertEquals(i, page.pdfPageIndex)
            assertEquals(PageBackground.BLANK, page.pageBackground)

            // Verifica se está persistida no DAO/Database
            val dbPage = repo.database.getPageById(page.id)
            assertNotNull(dbPage)
            assertEquals(i, dbPage?.pdfPageIndex)
            assertEquals(mockPdfFile.absolutePath, dbPage?.pdfPath)
        }

        // Teste de exclusão em cascata: excluir o caderno deve limpar todas as páginas
        repo.deleteNotebook(notebook.id)
        val pagesAfterDelete = repo.database.getPagesBySection(section.id)
        assertTrue(pagesAfterDelete.isEmpty())
    }
}

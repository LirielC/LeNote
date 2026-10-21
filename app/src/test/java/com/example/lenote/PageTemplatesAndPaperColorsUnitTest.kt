package com.example.lenote

import com.example.lenote.data.LeNoteDatabase
import com.example.lenote.data.NoteRepository
import com.example.lenote.data.PageStrokeStorage
import com.example.lenote.export.PageExporter
import com.example.lenote.ink.PageBackground
import com.example.lenote.ink.PaperColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

/**
 * Testes unitários para:
 * 1. Novos modelos de página pedagógicos e técnicos (CORNELL, WEEKLY_PLANNER, ENGINEERING_GRAPH, MUSIC_STAFF);
 * 2. Cores de papel personalizáveis (Legal Yellow, Sépia Cream, OLED Black, etc.);
 * 3. Persistência de `pageBackground` e `paperColor` no banco SQLite e repositório;
 * 4. Renderização com cor de papel personalizada na exportação de imagem.
 */
class PageTemplatesAndPaperColorsUnitTest {

    @Test
    fun pageTemplates_allEnumValuesAvailable() {
        val expectedTemplates = listOf(
            PageBackground.BLANK,
            PageBackground.RULED,
            PageBackground.GRID,
            PageBackground.DOTS,
            PageBackground.MATHEMATICS,
            PageBackground.CORNELL,
            PageBackground.WEEKLY_PLANNER,
            PageBackground.ENGINEERING_GRAPH,
            PageBackground.MUSIC_STAFF
        )
        assertEquals(9, PageBackground.entries.size)
        assertTrue(PageBackground.entries.containsAll(expectedTemplates))
    }

    @Test
    fun databaseAndRepository_persistsPageBackgroundAndPaperColor() {
        val db = LeNoteDatabase.createInMemory()
        val tempDir = Files.createTempDirectory("lenote_template_test").toFile()
        val storage = PageStrokeStorage(tempDir)
        val repo = NoteRepository(db, storage)

        val nb = repo.createNotebook("Caderno de Engenharia")
        val sec = repo.createSection(nb.id, "Projetos")

        // 1. Cria página com modelo Cornell e papel Amarelo Legal
        val page1 = repo.createPage(
            sectionId = sec.id,
            title = "Notas de Aula Cornell",
            pageBackground = PageBackground.CORNELL,
            paperColor = PaperColors.LEGAL_YELLOW
        )
        assertNotNull(page1)
        assertEquals(PageBackground.CORNELL, page1.pageBackground)
        assertEquals(PaperColors.LEGAL_YELLOW, page1.paperColor)

        // 2. Consulta no repositório e verifica recuperação fiel
        val retrieved1 = repo.database.getPageById(page1.id)
        assertNotNull(retrieved1)
        assertEquals(PageBackground.CORNELL, retrieved1!!.pageBackground)
        assertEquals(PaperColors.LEGAL_YELLOW, retrieved1.paperColor)

        // 3. Altera o modelo para Papel de Engenharia com cor Sépia
        repo.updatePageTemplate(
            pageId = page1.id,
            background = PageBackground.ENGINEERING_GRAPH,
            paperColor = PaperColors.SEPIA_CREAM
        )

        val updated1 = repo.database.getPageById(page1.id)
        assertNotNull(updated1)
        assertEquals(PageBackground.ENGINEERING_GRAPH, updated1!!.pageBackground)
        assertEquals(PaperColors.SEPIA_CREAM, updated1.paperColor)

        // 4. Cria página com partitura musical e fundo OLED Black
        val page2 = repo.createPage(
            sectionId = sec.id,
            title = "Composição Musical",
            pageBackground = PageBackground.MUSIC_STAFF,
            paperColor = PaperColors.OLED_BLACK
        )
        assertNotNull(page2)
        assertEquals(PageBackground.MUSIC_STAFF, page2.pageBackground)
        assertEquals(PaperColors.OLED_BLACK, page2.paperColor)

        val retrieved2 = repo.database.getPageById(page2.id)
        assertNotNull(retrieved2)
        assertEquals(PageBackground.MUSIC_STAFF, retrieved2!!.pageBackground)
        assertEquals(PaperColors.OLED_BLACK, retrieved2.paperColor)
    }

    @Test
    fun paperColors_darkPaperDetection_worksAccurately() {
        assertTrue(PaperColors.isDarkPaper(PaperColors.OLED_BLACK))
        assertTrue(PaperColors.isDarkPaper(0xFF000000))
        assertTrue(PaperColors.isDarkPaper(0xFF1F2937)) // Dark Slate

        assertTrue(!PaperColors.isDarkPaper(PaperColors.CLASSIC_WHITE))
        assertTrue(!PaperColors.isDarkPaper(PaperColors.LEGAL_YELLOW))
        assertTrue(!PaperColors.isDarkPaper(PaperColors.SEPIA_CREAM))
    }

    @Test
    fun pageExporter_respectsPaperColorInRepositoryLifecycle() {
        val db = LeNoteDatabase.createInMemory()
        val tempDir = Files.createTempDirectory("lenote_export_test").toFile()
        val storage = PageStrokeStorage(tempDir)
        val repo = NoteRepository(db, storage)

        val nb = repo.createNotebook("Caderno Export")
        val sec = repo.createSection(nb.id, "Seção 1")
        val page = repo.createPage(
            sectionId = sec.id,
            title = "Página Cornell Amarela",
            pageBackground = PageBackground.CORNELL,
            paperColor = PaperColors.LEGAL_YELLOW
        )

        assertEquals(PaperColors.LEGAL_YELLOW, page.paperColor)
        assertEquals(PageBackground.CORNELL, page.pageBackground)

        // Simula atualização dinâmica do modelo e cor da página
        repo.updatePageTemplate(page.id, PageBackground.WEEKLY_PLANNER, PaperColors.OLED_BLACK)
        val updated = repo.database.getPageById(page.id)
        assertNotNull(updated)
        assertEquals(PageBackground.WEEKLY_PLANNER, updated!!.pageBackground)
        assertEquals(PaperColors.OLED_BLACK, updated.paperColor)
        assertTrue(PaperColors.isDarkPaper(updated.paperColor))
    }
}

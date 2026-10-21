package com.example.lenote

import com.example.lenote.data.LeNoteDatabase
import com.example.lenote.data.NoteRepository
import com.example.lenote.ink.PageBackground
import com.example.lenote.data.PageStrokeStorage
import com.example.lenote.data.RecognizedWordBox
import com.example.lenote.data.WordBoxJsonSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DigitalInkAndFtsUnitTest {

    @Test
    fun wordBoxJsonSerializer_serializesAndDeserializesCorrectly() {
        val originalBoxes = listOf(
            RecognizedWordBox(word = "LeNote", left = 10f, top = 20f, right = 110f, bottom = 60f),
            RecognizedWordBox(word = "Android", left = 120f, top = 22f, right = 230f, bottom = 62f)
        )

        val json = WordBoxJsonSerializer.serialize(originalBoxes)
        assertNotNull(json)
        assertTrue(json.contains("LeNote"))
        assertTrue(json.contains("Android"))

        val deserialized = WordBoxJsonSerializer.deserialize(json)
        assertEquals(2, deserialized.size)
        assertEquals("LeNote", deserialized[0].word)
        assertEquals(10f, deserialized[0].left, 0.001f)
        assertEquals(20f, deserialized[0].top, 0.001f)
        assertEquals(110f, deserialized[0].right, 0.001f)
        assertEquals(60f, deserialized[0].bottom, 0.001f)

        assertEquals("Android", deserialized[1].word)
        assertEquals(120f, deserialized[1].left, 0.001f)
    }

    @Test
    fun databaseAndRepository_indexesHandwritingAndSearchesCorrectly() {
        val db = LeNoteDatabase.createInMemory()
        val tempDir = Files.createTempDirectory("lenote_test_storage").toFile()
        val storage = PageStrokeStorage(tempDir)
        val repo = NoteRepository(db, storage)

        val nb = repo.createNotebook(title = "Caderno de Estudos")
        val sec = repo.createSection(notebookId = nb.id, title = "Anotações")
        val page1 = repo.createPage(sectionId = sec.id, title = "Aula 1", pageBackground = PageBackground.RULED)
        val page2 = repo.createPage(sectionId = sec.id, title = "Aula 2", pageBackground = PageBackground.BLANK)

        val boxesP1 = listOf(
            RecognizedWordBox(word = "Algoritmos", left = 50f, top = 100f, right = 180f, bottom = 140f),
            RecognizedWordBox(word = "Grafos", left = 190f, top = 100f, right = 280f, bottom = 140f)
        )
        val boxesP2 = listOf(
            RecognizedWordBox(word = "Banco", left = 60f, top = 110f, right = 150f, bottom = 150f),
            RecognizedWordBox(word = "SQLite", left = 160f, top = 110f, right = 260f, bottom = 150f)
        )

        repo.indexPageHandwriting(page1.id, "Algoritmos Grafos", WordBoxJsonSerializer.serialize(boxesP1))
        repo.indexPageHandwriting(page2.id, "Banco SQLite", WordBoxJsonSerializer.serialize(boxesP2))

        // Busca por termo na página 1
        val results1 = repo.searchHandwriting("Grafos")
        assertEquals(1, results1.size)
        assertEquals(page1.id, results1[0].page.id)
        assertEquals(1, results1[0].matchedWordBoxes.size)
        assertEquals("Grafos", results1[0].matchedWordBoxes[0].word)
        assertEquals(190f, results1[0].matchedWordBoxes[0].left, 0.001f)

        // Busca case-insensitive e parcial por "sql"
        val results2 = repo.searchHandwriting("sql")
        assertEquals(1, results2.size)
        assertEquals(page2.id, results2[0].page.id)
        assertEquals("SQLite", results2[0].matchedWordBoxes[0].word)

        // Busca sem correspondência
        val emptyResults = repo.searchHandwriting("Inexistente")
        assertTrue(emptyResults.isEmpty())

        // Exclusão da página deve remover do índice
        repo.deletePage(page1.id)
        val resultsAfterDelete = repo.searchHandwriting("Grafos")
        assertTrue(resultsAfterDelete.isEmpty())
    }
}

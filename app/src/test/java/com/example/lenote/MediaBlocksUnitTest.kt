package com.example.lenote

import com.example.lenote.data.CanvasImageBlock
import com.example.lenote.data.CanvasTextBlock
import com.example.lenote.data.LeNoteDatabase
import com.example.lenote.data.NoteRepository
import com.example.lenote.data.PageStrokeStorage
import com.example.lenote.ink.PageBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MediaBlocksUnitTest {

    @Test
    fun databaseAndRepository_persistsAndRetrievesTextAndImageBlocks() {
        val db = LeNoteDatabase.createInMemory()
        val tempDir = Files.createTempDirectory("lenote_media_test").toFile()
        val storage = PageStrokeStorage(tempDir)
        val repo = NoteRepository(db, storage)

        val nb = repo.createNotebook("Caderno com Mídia")
        val sec = repo.createSection(nb.id, "Seção 1")
        val page = repo.createPage(sec.id, "Página com Mídia", PageBackground.BLANK)

        // 1. Testa Bloco de Texto
        val textBlock = CanvasTextBlock(
            id = "text_1",
            pageId = page.id,
            text = "Anotação importante sobre a aula",
            x = 100f,
            y = 150f,
            width = 300f,
            height = 80f,
            textColor = 0xFF1E3A8A.toInt(),
            fontSize = 18f,
            zIndex = 1f
        )
        repo.upsertTextBlock(textBlock)

        val retrievedTextBlocks = repo.getTextBlocks(page.id)
        assertEquals(1, retrievedTextBlocks.size)
        val t = retrievedTextBlocks[0]
        assertEquals("text_1", t.id)
        assertEquals(page.id, t.pageId)
        assertEquals("Anotação importante sobre a aula", t.text)
        assertEquals(100f, t.x, 0.001f)
        assertEquals(150f, t.y, 0.001f)
        assertEquals(300f, t.width, 0.001f)
        assertEquals(80f, t.height, 0.001f)
        assertEquals(18f, t.fontSize, 0.001f)

        // 2. Testa Bloco de Imagem
        val mockImageFile = File(tempDir, "diagram.png")
        mockImageFile.writeBytes(byteArrayOf(1, 2, 3, 4))

        val imageBlock = CanvasImageBlock(
            id = "img_1",
            pageId = page.id,
            imagePath = mockImageFile.absolutePath,
            x = 200f,
            y = 300f,
            width = 400f,
            height = 250f,
            rotationDegrees = 15f,
            zIndex = 0.5f
        )
        repo.upsertImageBlock(imageBlock)

        val retrievedImageBlocks = repo.getImageBlocks(page.id)
        assertEquals(1, retrievedImageBlocks.size)
        val img = retrievedImageBlocks[0]
        assertEquals("img_1", img.id)
        assertEquals(page.id, img.pageId)
        assertEquals(mockImageFile.absolutePath, img.imagePath)
        assertEquals(200f, img.x, 0.001f)
        assertEquals(300f, img.y, 0.001f)
        assertEquals(400f, img.width, 0.001f)
        assertEquals(250f, img.height, 0.001f)
        assertEquals(15f, img.rotationDegrees, 0.001f)

        // 3. Atualização de Texto e Imagem
        val updatedText = textBlock.copy(text = "Texto alterado com sucesso", width = 350f)
        repo.upsertTextBlock(updatedText)
        val updatedList = repo.getTextBlocks(page.id)
        assertEquals(1, updatedList.size)
        assertEquals("Texto alterado com sucesso", updatedList[0].text)
        assertEquals(350f, updatedList[0].width, 0.001f)

        val updatedImg = imageBlock.copy(rotationDegrees = 45f)
        repo.upsertImageBlock(updatedImg)
        val updatedImgList = repo.getImageBlocks(page.id)
        assertEquals(45f, updatedImgList[0].rotationDegrees, 0.001f)

        // 4. Exclusão individual de blocos
        repo.deleteTextBlock("text_1")
        assertTrue(repo.getTextBlocks(page.id).isEmpty())

        repo.deleteImageBlock("img_1")
        assertTrue(repo.getImageBlocks(page.id).isEmpty())

        // 5. Exclusão em cascata ao remover a página
        repo.upsertTextBlock(textBlock)
        repo.upsertImageBlock(imageBlock)
        assertEquals(1, repo.getTextBlocks(page.id).size)
        assertEquals(1, repo.getImageBlocks(page.id).size)

        repo.deletePage(page.id)
        assertTrue(repo.getTextBlocks(page.id).isEmpty())
        assertTrue(repo.getImageBlocks(page.id).isEmpty())
    }
}

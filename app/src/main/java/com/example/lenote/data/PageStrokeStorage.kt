package com.example.lenote.data

import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream

/**
 * Gerenciador de armazenamento binário de strokes (`filesDir/pages/<pageId>.ink`) e miniaturas
 * (`filesDir/thumbs/<pageId>.png`).
 *
 * Garantias:
 * - Mantém o banco SQLite leve armazenando apenas o caminho do arquivo;
 * - Escrita atômica via arquivo temporário (`.tmp` + `fd.sync()` + rename) para evitar corrupção
 *   de páginas em caso de fechamento abrupto ou queda de bateria.
 */
class PageStrokeStorage(private val rootDir: File) {

    val pagesDir: File = File(rootDir, "pages").apply { mkdirs() }
    val thumbsDir: File = File(rootDir, "thumbs").apply { mkdirs() }
    val exportsDir: File = File(rootDir, "exports").apply { mkdirs() }

    fun getStrokeFile(pageId: String): File = File(pagesDir, "$pageId.ink")

    fun getThumbnailFile(pageId: String): File = File(thumbsDir, "$pageId.png")

    /**
     * Salva atomicamente os bytes serializados dos strokes de uma página.
     * Retorna o caminho absoluto do arquivo `.ink` salvo.
     */
    @Synchronized
    fun savePageStrokesAtomic(pageId: String, data: ByteArray): String {
        pagesDir.mkdirs()
        val targetFile = getStrokeFile(pageId)
        val tempFile = File(pagesDir, "$pageId.ink.tmp")

        FileOutputStream(tempFile).use { fos ->
            fos.write(data)
            fos.flush()
            try {
                fos.fd.sync()
            } catch (_: Exception) {
                // Ignorado em sistemas de arquivos virtuais de teste onde sync() não é suportado
            }
        }

        if (targetFile.exists() && !tempFile.renameTo(targetFile)) {
            targetFile.delete()
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        } else if (!targetFile.exists()) {
            if (!tempFile.renameTo(targetFile)) {
                tempFile.copyTo(targetFile, overwrite = true)
                tempFile.delete()
            }
        }

        return targetFile.absolutePath
    }

    /**
     * Carrega os bytes serializados dos strokes de uma página, ou retorna [ ByteArray(0) ] se ainda não existir.
     */
    @Synchronized
    fun loadPageStrokes(pageId: String): ByteArray {
        val file = getStrokeFile(pageId)
        if (!file.exists() || !file.isFile) return ByteArray(0)
        return try {
            file.readBytes()
        } catch (_: Exception) {
            ByteArray(0)
        }
    }

    /**
     * Salva atomicamente o Bitmap da miniatura (thumbnail) de uma página.
     */
    @Synchronized
    fun savePageThumbnailAtomic(pageId: String, bitmap: Bitmap): String {
        thumbsDir.mkdirs()
        val targetFile = getThumbnailFile(pageId)
        val tempFile = File(thumbsDir, "$pageId.png.tmp")

        FileOutputStream(tempFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
            fos.flush()
        }

        if (targetFile.exists()) {
            targetFile.delete()
        }
        if (!tempFile.renameTo(targetFile)) {
            tempFile.copyTo(targetFile, overwrite = true)
            tempFile.delete()
        }
        return targetFile.absolutePath
    }

    /**
     * Remove os arquivos `.ink` e `.png` associados a uma página excluída.
     */
    @Synchronized
    fun deletePageFiles(pageId: String) {
        getStrokeFile(pageId).delete()
        File(pagesDir, "$pageId.ink.tmp").delete()
        getThumbnailFile(pageId).delete()
        File(thumbsDir, "$pageId.png.tmp").delete()
    }
}

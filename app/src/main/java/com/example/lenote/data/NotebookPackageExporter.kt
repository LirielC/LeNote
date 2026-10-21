package com.example.lenote.data

import com.example.lenote.ink.PageBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Utilitário responsável por exportar e importar cadernos no formato oficial do LeNote (`.lenote`).
 *
 * Estrutura do arquivo ZIP (.lenote):
 * - `metadata.json`: Informações completas do caderno, seções, páginas, caixas de texto e referências a imagens/PDFs.
 * - `strokes/{pageId}.ink`: Dados binários de traços do Jetpack Ink de cada página.
 * - `media/images/{filename}`: Arquivos de imagem inseridos nas páginas.
 * - `media/pdfs/{filename}`: Arquivos de PDF associados ao documento.
 */
object NotebookPackageExporter {

    /**
     * Exporta um caderno completo para o formato `.lenote`.
     */
    suspend fun exportNotebookPackage(
        notebookId: String,
        repository: NoteRepository,
        targetZipFile: File
    ): Boolean = withContext(Dispatchers.IO) {
        val nb = repository.database.getNotebookById(notebookId) ?: return@withContext false
        val sections = repository.database.getSectionsByNotebook(notebookId)
        val pages = sections.flatMap { repository.database.getAllPagesInDb().filter { p -> p.sectionId == it.id } }

        try {
            targetZipFile.parentFile?.mkdirs()
            ZipOutputStream(FileOutputStream(targetZipFile)).use { zos ->
                // 1. Constrói o metadata.json
                val rootJson = JSONObject().apply {
                    put("version", 2)
                    put("app", "LeNote")
                    put("timestamp", System.currentTimeMillis())

                    val nbJson = JSONObject().apply {
                        put("id", nb.id)
                        put("title", nb.title)
                        put("colorHex", nb.colorHex)
                        put("createdAt", nb.createdAt)
                        put("updatedAt", nb.updatedAt)
                        put("sortOrder", nb.sortOrder)
                    }
                    put("notebook", nbJson)

                    val secArray = JSONArray()
                    for (sec in sections) {
                        secArray.put(JSONObject().apply {
                            put("id", sec.id)
                            put("notebookId", sec.notebookId)
                            put("title", sec.title)
                            put("colorHex", sec.colorHex)
                            put("createdAt", sec.createdAt)
                            put("updatedAt", sec.updatedAt)
                            put("sortOrder", sec.sortOrder)
                        })
                    }
                    put("sections", secArray)

                    val pageArray = JSONArray()
                    for (p in pages) {
                        val pJson = JSONObject().apply {
                            put("id", p.id)
                            put("sectionId", p.sectionId)
                            put("title", p.title)
                            put("pageBackground", p.pageBackground.name)
                            put("zoom", p.zoom.toDouble())
                            put("panX", p.panX.toDouble())
                            put("panY", p.panY.toDouble())
                            put("isFavorite", p.isFavorite)
                            put("createdAt", p.createdAt)
                            put("updatedAt", p.updatedAt)
                            put("sortOrder", p.sortOrder)
                            if (p.pdfPath != null) {
                                put("pdfFileName", File(p.pdfPath).name)
                                put("pdfPageIndex", p.pdfPageIndex ?: 0)
                            }
                        }

                        // Inclui blocos de texto
                        val textBlocks = repository.getTextBlocks(p.id)
                        val tbArray = JSONArray()
                        for (tb in textBlocks) {
                            tbArray.put(JSONObject().apply {
                                put("id", tb.id)
                                put("text", tb.text)
                                put("x", tb.x.toDouble())
                                put("y", tb.y.toDouble())
                                put("width", tb.width.toDouble())
                                put("height", tb.height.toDouble())
                                put("textColor", tb.textColor)
                                put("fontSize", tb.fontSize.toDouble())
                                put("zIndex", tb.zIndex.toDouble())
                            })
                        }
                        pJson.put("textBlocks", tbArray)

                        // Inclui blocos de imagem
                        val imageBlocks = repository.getImageBlocks(p.id)
                        val ibArray = JSONArray()
                        for (ib in imageBlocks) {
                            ibArray.put(JSONObject().apply {
                                put("id", ib.id)
                                put("imageFileName", File(ib.imagePath).name)
                                put("x", ib.x.toDouble())
                                put("y", ib.y.toDouble())
                                put("width", ib.width.toDouble())
                                put("height", ib.height.toDouble())
                                put("rotationDegrees", ib.rotationDegrees.toDouble())
                                put("zIndex", ib.zIndex.toDouble())
                            })
                        }
                        pJson.put("imageBlocks", ibArray)

                        pageArray.put(pJson)
                    }
                    put("pages", pageArray)
                }

                // Grava metadata.json
                zos.putNextEntry(ZipEntry("metadata.json"))
                zos.write(rootJson.toString(2).toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // 2. Grava os strokes de cada página
                for (p in pages) {
                    val strokesBytes = repository.loadPageStrokes(p.id)
                    if (strokesBytes.isNotEmpty()) {
                        zos.putNextEntry(ZipEntry("strokes/${p.id}.ink"))
                        zos.write(strokesBytes)
                        zos.closeEntry()
                    }
                }

                // 3. Grava arquivos de imagem incorporados
                val addedImages = mutableSetOf<String>()
                for (p in pages) {
                    val imageBlocks = repository.getImageBlocks(p.id)
                    for (ib in imageBlocks) {
                        val imgFile = File(ib.imagePath)
                        if (imgFile.exists() && addedImages.add(imgFile.name)) {
                            zos.putNextEntry(ZipEntry("media/images/${imgFile.name}"))
                            imgFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }

                // 4. Grava arquivos de PDF incorporados
                val addedPdfs = mutableSetOf<String>()
                for (p in pages) {
                    if (p.pdfPath != null) {
                        val pdfFile = File(p.pdfPath)
                        if (pdfFile.exists() && addedPdfs.add(pdfFile.name)) {
                            zos.putNextEntry(ZipEntry("media/pdfs/${pdfFile.name}"))
                            pdfFile.inputStream().use { it.copyTo(zos) }
                            zos.closeEntry()
                        }
                    }
                }
            }
            true
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }
    }

    /**
     * Importa e descompacta um pacote `.lenote`, restaurando cadernos, seções, páginas, mídias e strokes.
     * Gera novos UUIDs para evitar conflitos de chave primária.
     */
    suspend fun importNotebookPackage(
        packageFile: File,
        repository: NoteRepository,
        internalFilesDir: File
    ): NotebookEntity? = withContext(Dispatchers.IO) {
        if (!packageFile.exists()) return@withContext null

        var metadataStr: String? = null
        val strokeData = mutableMapOf<String, ByteArray>()
        val imagesDir = File(internalFilesDir, "images").apply { mkdirs() }
        val pdfsDir = File(internalFilesDir, "pdfs").apply { mkdirs() }
        val restoredImages = mutableMapOf<String, String>()
        val restoredPdfs = mutableMapOf<String, String>()

        try {
            ZipInputStream(FileInputStream(packageFile)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "metadata.json" -> {
                            metadataStr = zis.readBytes().toString(Charsets.UTF_8)
                        }
                        name.startsWith("strokes/") && name.endsWith(".ink") -> {
                            val oldPageId = name.removePrefix("strokes/").removeSuffix(".ink")
                            strokeData[oldPageId] = zis.readBytes()
                        }
                        name.startsWith("media/images/") -> {
                            val fileName = name.removePrefix("media/images/")
                            val target = File(imagesDir, "imported_${System.currentTimeMillis()}_$fileName")
                            target.outputStream().use { zis.copyTo(it) }
                            restoredImages[fileName] = target.absolutePath
                        }
                        name.startsWith("media/pdfs/") -> {
                            val fileName = name.removePrefix("media/pdfs/")
                            val target = File(pdfsDir, "imported_${System.currentTimeMillis()}_$fileName")
                            target.outputStream().use { zis.copyTo(it) }
                            restoredPdfs[fileName] = target.absolutePath
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            val meta = metadataStr?.let { JSONObject(it) } ?: return@withContext null
            val nbObj = meta.getJSONObject("notebook")
            val newNbId = UUID.randomUUID().toString()
            val now = System.currentTimeMillis()

            val notebook = NotebookEntity(
                id = newNbId,
                title = nbObj.optString("title", "Caderno Restaurado"),
                colorHex = nbObj.optInt("colorHex", 0xFF1E3A8A.toInt()),
                createdAt = now,
                updatedAt = now,
                sortOrder = (repository.database.getAllNotebooks().maxOfOrNull { it.sortOrder } ?: -1) + 1
            )
            repository.database.upsertNotebook(notebook)

            // Mapeamento de IDs antigos para novos IDs
            val sectionIdMap = mutableMapOf<String, String>()
            val secArray = meta.getJSONArray("sections")
            for (i in 0 until secArray.length()) {
                val secObj = secArray.getJSONObject(i)
                val oldSecId = secObj.getString("id")
                val newSecId = UUID.randomUUID().toString()
                sectionIdMap[oldSecId] = newSecId

                val section = SectionEntity(
                    id = newSecId,
                    notebookId = newNbId,
                    title = secObj.optString("title", "Seção"),
                    colorHex = secObj.optInt("colorHex", 0xFF00897B.toInt()),
                    createdAt = now,
                    updatedAt = now,
                    sortOrder = secObj.optInt("sortOrder", i)
                )
                repository.database.upsertSection(section)
            }

            val pageArray = meta.getJSONArray("pages")
            for (i in 0 until pageArray.length()) {
                val pObj = pageArray.getJSONObject(i)
                val oldPageId = pObj.getString("id")
                val oldSecId = pObj.getString("sectionId")
                val targetSecId = sectionIdMap[oldSecId] ?: continue

                val newPageId = UUID.randomUUID().toString()
                val strokeFilePath = repository.strokeStorage.getStrokeFile(newPageId).absolutePath

                val pdfName = pObj.optString("pdfFileName", "")
                val pdfPath = if (pdfName.isNotEmpty()) restoredPdfs[pdfName] else null
                val pdfIdx = if (pdfPath != null) pObj.optInt("pdfPageIndex", 0) else null

                val page = PageEntity(
                    id = newPageId,
                    sectionId = targetSecId,
                    title = pObj.optString("title", "Página ${i + 1}"),
                    pageBackground = runCatching { PageBackground.valueOf(pObj.getString("pageBackground")) }.getOrDefault(PageBackground.BLANK),
                    zoom = pObj.optDouble("zoom", 1.0).toFloat(),
                    panX = pObj.optDouble("panX", 0.0).toFloat(),
                    panY = pObj.optDouble("panY", 0.0).toFloat(),
                    strokesFilePath = strokeFilePath,
                    isFavorite = pObj.optBoolean("isFavorite", false),
                    createdAt = now,
                    updatedAt = now,
                    sortOrder = pObj.optInt("sortOrder", i),
                    pdfPath = pdfPath,
                    pdfPageIndex = pdfIdx
                )
                repository.database.upsertPage(page)

                // Restaura strokes da página
                val strokes = strokeData[oldPageId]
                if (strokes != null && strokes.isNotEmpty()) {
                    repository.savePageStrokes(newPageId, strokes)
                }

                // Restaura blocos de texto
                val tbArray = pObj.optJSONArray("textBlocks")
                if (tbArray != null) {
                    for (j in 0 until tbArray.length()) {
                        val tbObj = tbArray.getJSONObject(j)
                        val tb = CanvasTextBlock(
                            id = UUID.randomUUID().toString(),
                            pageId = newPageId,
                            text = tbObj.getString("text"),
                            x = tbObj.getDouble("x").toFloat(),
                            y = tbObj.getDouble("y").toFloat(),
                            width = tbObj.optDouble("width", 240.0).toFloat(),
                            height = tbObj.optDouble("height", 60.0).toFloat(),
                            textColor = tbObj.optInt("textColor", 0xFF0F172A.toInt()),
                            fontSize = tbObj.optDouble("fontSize", 16.0).toFloat(),
                            zIndex = tbObj.optDouble("zIndex", 1.0).toFloat()
                        )
                        repository.upsertTextBlock(tb)
                    }
                }

                // Restaura blocos de imagem
                val ibArray = pObj.optJSONArray("imageBlocks")
                if (ibArray != null) {
                    for (j in 0 until ibArray.length()) {
                        val ibObj = ibArray.getJSONObject(j)
                        val imgName = ibObj.getString("imageFileName")
                        val imgPath = restoredImages[imgName] ?: continue
                        val ib = CanvasImageBlock(
                            id = UUID.randomUUID().toString(),
                            pageId = newPageId,
                            imagePath = imgPath,
                            x = ibObj.getDouble("x").toFloat(),
                            y = ibObj.getDouble("y").toFloat(),
                            width = ibObj.optDouble("width", 300.0).toFloat(),
                            height = ibObj.optDouble("height", 300.0).toFloat(),
                            rotationDegrees = ibObj.optDouble("rotationDegrees", 0.0).toFloat(),
                            zIndex = ibObj.optDouble("zIndex", 0.0).toFloat()
                        )
                        repository.upsertImageBlock(ib)
                    }
                }
            }

            notebook
        } catch (_: Throwable) {
            null
        }
    }
}

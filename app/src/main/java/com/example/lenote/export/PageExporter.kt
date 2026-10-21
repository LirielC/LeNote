package com.example.lenote.export

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.graphics.BitmapFactory
import com.example.lenote.data.CanvasTextBlock
import com.example.lenote.data.CanvasImageBlock

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.ink.rendering.android.canvas.CanvasStrokeRenderer
import androidx.ink.strokes.Stroke
import com.example.lenote.data.PageEntity
import com.example.lenote.ink.PageBackground
import com.example.lenote.ink.PartialStrokeEraser
import java.io.File
import java.io.FileOutputStream
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Resultado de exportação de página/documento.
 * Herda de [java.io.File] para manter 100% de compatibilidade com chamadas que esperam um [File],
 * enquanto provê metadados modernos como [uri] do MediaStore/FileProvider, [fileName], [mimeType],
 * [displayPath] e [userMessage] amigável.
 */
open class ExportResult(
    val localFile: File,
    val uri: Uri?,
    val fileName: String,
    val mimeType: String = if (fileName.endsWith(".pdf", ignoreCase = true)) "application/pdf" else "image/png",
    val displayPath: String = if (fileName.endsWith(".pdf", ignoreCase = true)) "Download/LeNote/$fileName" else "Pictures/LeNote/$fileName",
    val userMessage: String = if (fileName.endsWith(".pdf", ignoreCase = true)) "PDF exportado com sucesso" else "Imagem exportada com sucesso",
    val isSuccess: Boolean = true,
    val errorMessage: String? = null
) : File(localFile.absolutePath)

/**
 * Utilitário seguro para abertura de arquivos exportados através de [android.content.Intent.ACTION_VIEW]
 * usando Content URIs (MediaStore ou FileProvider), sem expor file:// URIs.
 */
object ExportViewerHelper {

    /**
     * Tenta abrir o arquivo exportado usando o visualizador padrão do sistema Android.
     * Retorna [Result.success] se a Activity foi iniciada com sucesso,
     * ou [Result.failure] com [android.content.ActivityNotFoundException] se nenhum visualizador estiver instalado.
     */
    fun openExportedFile(context: Context, exportResult: ExportResult): Result<Unit> {
        val urisToTry = mutableListOf<Uri>()

        // 1. Tenta a URI pública do MediaStore (se disponível)
        exportResult.uri?.let { urisToTry.add(it) }

        // 2. Fallback para URI segura via FileProvider
        try {
            val fpUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                exportResult.localFile
            )
            if (!urisToTry.contains(fpUri)) {
                urisToTry.add(fpUri)
            }
        } catch (_: Throwable) {}

        if (urisToTry.isEmpty()) {
            return Result.failure(IllegalStateException("URI segura não disponível para abertura."))
        }

        var lastError: Throwable? = null
        for (uri in urisToTry) {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, exportResult.mimeType)
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
                return Result.success(Unit)
            } catch (e: android.content.ActivityNotFoundException) {
                val appType = if (exportResult.mimeType == "application/pdf") "PDF" else "imagem"
                return Result.failure(android.content.ActivityNotFoundException("Não foi encontrado um aplicativo para abrir $appType."))
            } catch (e: Throwable) {
                lastError = e
            }
        }

        return Result.failure(lastError ?: RuntimeException("Não foi possível abrir o arquivo."))
    }
}

/**
 * Motor de exportação vetorial/bitmap para páginas do LeNote:
 * - Geração de Miniaturas (`Thumbnail`) rápidas em Bitmap;
 * - Exportação de Página para imagem PNG de alta resolução via MediaStore (Pictures/LeNote);
 * - Exportação de Página ou Seção inteira para documento PDF (`PdfDocument`) via MediaStore (Download/LeNote).
 */
object PageExporter {

    private val renderer: CanvasStrokeRenderer by lazy { CanvasStrokeRenderer.create(false) }

    private val paperBackgroundPaint by lazy {
        Paint().apply {
            style = Paint.Style.FILL
            color = 0xFFFCFCFA.toInt()
        }
    }

    private val ruledLinePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFBBDEFB.toInt()
        }
    }

    private val marginLinePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFFFCDD2.toInt()
        }
    }

    private val gridMinorPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFE3EAF2.toInt()
        }
    }

    private val gridMajorPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFFB0C4DE.toInt()
        }
    }

    private val mathAxisPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = 0xFF78909C.toInt()
        }
    }

    private val dotPaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFFB0BEC5.toInt()
        }
    }

    private val titlePaint by lazy {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = 0xFF37474F.toInt()
            textSize = 20f
            isFakeBoldText = true
        }
    }

    /**
     * Calcula a região de mundo ocupada pelos strokes ou usa uma folha padrão (1200x1600) se vazia.
     */
    fun computePageWorldViewport(
        strokes: List<Stroke>,
        minWidth: Float = 1200f,
        minHeight: Float = 1600f,
        padding: Float = 64f
    ): FloatArray {
        if (strokes.isEmpty()) {
            return floatArrayOf(0f, 0f, minWidth, minHeight)
        }
        var xMin = Float.POSITIVE_INFINITY
        var yMin = Float.POSITIVE_INFINITY
        var xMax = Float.NEGATIVE_INFINITY
        var yMax = Float.NEGATIVE_INFINITY

        for (s in strokes) {
            val b = PartialStrokeEraser.getOrComputeBounds(s) ?: continue
            val m = b.strokeHalfWidth + 4f
            xMin = min(xMin, b.xMin - m)
            yMin = min(yMin, b.yMin - m)
            xMax = max(xMax, b.xMax + m)
            yMax = max(yMax, b.yMax + m)
        }

        if (!xMin.isFinite() || !yMin.isFinite()) {
            return floatArrayOf(0f, 0f, minWidth, minHeight)
        }

        val left = min(0f, xMin - padding)
        val top = min(0f, yMin - padding)
        val right = max(left + minWidth, xMax + padding)
        val bottom = max(top + minHeight, yMax + padding)
        return floatArrayOf(left, top, right, bottom)
    }

    /**
     * Renderiza uma página (fundo + strokes) em um [Canvas] de destino com dimensões `(targetWidth, targetHeight)`.
     */
    fun renderPageToCanvas(
        targetCanvas: Canvas,
        targetWidth: Int,
        targetHeight: Int,
        background: PageBackground,
        strokes: List<Stroke>,
        headerTitle: String? = null,
        isDarkMode: Boolean = false,
        pdfPath: String? = null,
        pdfPageIndex: Int? = null,
        textBlocks: List<CanvasTextBlock> = emptyList(),
        imageBlocks: List<CanvasImageBlock> = emptyList()
    ) {
        val paperBgColor = if (isDarkMode) {
            com.example.lenote.ink.ColorInversionHelper.getPaperBackgroundColor(true)
        } else {
            paperBackgroundPaint.color
        }
        val bgPaint = Paint().apply {
            style = Paint.Style.FILL
            color = paperBgColor
        }
        targetCanvas.drawRect(0f, 0f, targetWidth.toFloat(), targetHeight.toFloat(), bgPaint)

        val bounds = computePageWorldViewport(strokes)
        val worldLeft = bounds[0]
        val worldTop = bounds[1]
        val worldW = max(1f, bounds[2] - worldLeft)
        val worldH = max(1f, bounds[3] - worldTop)

        val scale = min(targetWidth.toFloat() / worldW, targetHeight.toFloat() / worldH)
        val offsetX = (targetWidth - worldW * scale) * 0.5f - worldLeft * scale
        val offsetY = (targetHeight - worldH * scale) * 0.5f - worldTop * scale

        val transform = Matrix().apply {
            postScale(scale, scale)
            postTranslate(offsetX, offsetY)
        }

        val saveCount = targetCanvas.save()
        try {
            targetCanvas.concat(transform)
            var drewPdf = false
            if (!pdfPath.isNullOrBlank() && pdfPageIndex != null && File(pdfPath).exists()) {
                val pdfBmp = com.example.lenote.pdf.PdfPageRenderer.getCachedBitmap(pdfPath, pdfPageIndex, 2.0f)
                    ?: kotlinx.coroutines.runBlocking {
                        com.example.lenote.pdf.PdfPageRenderer.renderPage(pdfPath, pdfPageIndex, 2.0f)
                    }
                if (pdfBmp != null && !pdfBmp.isRecycled) {
                    val srcRect = android.graphics.Rect(0, 0, pdfBmp.width, pdfBmp.height)
                    val dstRect = android.graphics.RectF(0f, 0f, 1200f, 1600f)
                    targetCanvas.drawBitmap(pdfBmp, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))
                    drewPdf = true
                }
            }

            if (!drewPdf) {
                drawBackgroundInWorld(
                    canvas = targetCanvas,
                    background = background,
                    worldLeft = worldLeft,
                    worldTop = worldTop,
                    worldRight = worldLeft + worldW,
                    worldBottom = worldTop + worldH,
                    scale = scale,
                    isDarkMode = isDarkMode
                )
            }

            // 1. Renderiza Imagens no Canvas
            for (img in imageBlocks.sortedBy { it.zIndex }) {
                val file = File(img.imagePath)
                if (file.exists()) {
                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                    if (bmp != null) {
                        val imgSave = targetCanvas.save()
                        try {
                            targetCanvas.translate(img.x + img.width / 2f, img.y + img.height / 2f)
                            targetCanvas.rotate(img.rotationDegrees)
                            val srcR = android.graphics.Rect(0, 0, bmp.width, bmp.height)
                            val dstR = android.graphics.RectF(-img.width / 2f, -img.height / 2f, img.width / 2f, img.height / 2f)
                            targetCanvas.drawBitmap(bmp, srcR, dstR, Paint(Paint.FILTER_BITMAP_FLAG))
                        } finally {
                            targetCanvas.restoreToCount(imgSave)
                            bmp.recycle()
                        }
                    }
                }
            }

            // 2. Renderiza Caixas de Texto com StaticLayout
            for (tb in textBlocks.sortedBy { it.zIndex }) {
                if (tb.text.isNotBlank()) {
                    val txtPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = tb.textColor
                        textSize = tb.fontSize
                    }
                    val blockW = max(10, tb.width.toInt())
                    val layout = StaticLayout.Builder.obtain(tb.text, 0, tb.text.length, txtPaint, blockW)
                        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                        .setIncludePad(false)
                        .build()

                    val txtSave = targetCanvas.save()
                    try {
                        targetCanvas.translate(tb.x, tb.y)
                        layout.draw(targetCanvas)
                    } finally {
                        targetCanvas.restoreToCount(txtSave)
                    }
                }
            }

            // 3. Renderiza traços de tinta sobrepostos
            for (stroke in strokes) {
                renderer.draw(targetCanvas, stroke, transform)
            }
        } finally {
            targetCanvas.restoreToCount(saveCount)
        }

        if (!headerTitle.isNullOrBlank()) {
            titlePaint.textSize = max(12f, targetWidth * 0.018f)
            targetCanvas.drawText(headerTitle, 24f, titlePaint.textSize + 16f, titlePaint)
        }
    }

    /**
     * Gera um Bitmap leve de miniatura (ex: 240x320) para exibição rápida na barra lateral.
     */
    fun generateThumbnailBitmap(
        background: PageBackground,
        strokes: List<Stroke>,
        width: Int = 240,
        height: Int = 320,
        pdfPath: String? = null,
        pdfPageIndex: Int? = null
    ): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        renderPageToCanvas(
            targetCanvas = canvas,
            targetWidth = width,
            targetHeight = height,
            background = background,
            strokes = strokes,
            headerTitle = null,
            pdfPath = pdfPath,
            pdfPageIndex = pdfPageIndex
        )
        return bmp
    }

    /**
     * Gera um nome de arquivo seguro e formatado para exportação.
     * Exemplo: `LeNote_Pagina_1_2026-10-18.png`
     */
    fun generateExportFileName(pageTitle: String, extension: String): String {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val safeTitle = sanitizeFileName(pageTitle.ifBlank { "Pagina_1" })
        return "LeNote_${safeTitle}_${dateStr}.${extension.lowercase()}"
    }

    /**
     * Sanitiza o título do documento para uso seguro no sistema de arquivos.
     * Normaliza acentuações (ex: "Página 1" -> "Pagina_1").
     */
    fun sanitizeFileName(raw: String): String {
        val normalized = Normalizer.normalize(raw, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return normalized.trim()
            .replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .take(40)
            .ifBlank { "Pagina_1" }
    }

    /**
     * Salva o Bitmap de exportação no [MediaStore] sob o diretório `Pictures/LeNote`.
     */
    private fun saveToMediaStoreImages(
        context: Context,
        fileName: String,
        bitmap: Bitmap
    ): Uri? {
        val resolver = context.contentResolver ?: return null
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/LeNote")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = try {
            resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        } catch (_: Throwable) {
            null
        } ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { outStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                outStream.flush()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (_: Throwable) {
            try {
                resolver.delete(uri, null, null)
            } catch (_: Throwable) {}
            null
        }
    }

    /**
     * Salva o documento PDF no [MediaStore] sob o diretório `Download/LeNote`.
     */
    private fun saveToMediaStoreDownloads(
        context: Context,
        fileName: String,
        pdfDoc: PdfDocument
    ): Uri? {
        val resolver = context.contentResolver ?: return null
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/LeNote")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val targetCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = try {
            resolver.insert(targetCollection, values)
        } catch (_: Throwable) {
            null
        } ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { outStream ->
                pdfDoc.writeTo(outStream)
                outStream.flush()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (_: Throwable) {
            try {
                resolver.delete(uri, null, null)
            } catch (_: Throwable) {}
            null
        }
    }

    /**
     * Exporta uma página para imagem PNG de alta definição via MediaStore (`Pictures/LeNote`)
     * com cópia local em `filesDir/exports`.
     * Não inclui nenhum controle de UI, toolbar ou header (apenas a folha e seus strokes).
     * Independente do zoom e pan atuais da tela.
     */
    fun exportPageToPng(
        context: Context,
        pageTitle: String,
        background: PageBackground,
        strokes: List<Stroke>,
        width: Int = 1600,
        height: Int = 2133,
        isDarkMode: Boolean = false,
        pdfPath: String? = null,
        pdfPageIndex: Int? = null,
        textBlocks: List<CanvasTextBlock> = emptyList(),
        imageBlocks: List<CanvasImageBlock> = emptyList()
    ): ExportResult {
        val fileName = generateExportFileName(pageTitle, "png")
        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val localFile = File(exportsDir, fileName)

        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        renderPageToCanvas(
            targetCanvas = canvas,
            targetWidth = width,
            targetHeight = height,
            background = background,
            strokes = strokes,
            headerTitle = null,
            isDarkMode = isDarkMode,
            pdfPath = pdfPath,
            pdfPageIndex = pdfPageIndex,
            textBlocks = textBlocks,
            imageBlocks = imageBlocks
        )

        // Grava no arquivo local (garantindo acesso offline e compatibilidade com testes)
        FileOutputStream(localFile).use { fos ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
        }

        // Grava no armazenamento público do usuário (Pictures/LeNote)
        val mediaUri = saveToMediaStoreImages(context, fileName, bmp)
        bmp.recycle()

        val userMessage = if (mediaUri != null) {
            "✓ PNG salvo em Imagens/LeNote: $fileName"
        } else {
            "✓ PNG salvo: $fileName"
        }

        return ExportResult(
            localFile = localFile,
            uri = mediaUri,
            fileName = fileName,
            userMessage = userMessage
        )
    }

    /**
     * Exporta uma única página para um arquivo `.pdf` via MediaStore (`Download/LeNote`).
     * Não inclui nenhum controle de UI, toolbar ou header.
     * Independente do zoom e pan atuais da tela.
     */
    fun exportPageToPdf(
        context: Context,
        pageTitle: String,
        background: PageBackground,
        strokes: List<Stroke>,
        pageWidthPt: Int = 1200,
        pageHeightPt: Int = 1600,
        isDarkMode: Boolean = false,
        pdfPath: String? = null,
        pdfPageIndex: Int? = null,
        textBlocks: List<CanvasTextBlock> = emptyList(),
        imageBlocks: List<CanvasImageBlock> = emptyList()
    ): ExportResult {
        val fileName = generateExportFileName(pageTitle, "pdf")
        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val localFile = File(exportsDir, fileName)

        val pdfDoc = PdfDocument()
        val mediaUri: Uri?
        try {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, 1).create()
            val pdfPage = pdfDoc.startPage(pageInfo)
            renderPageToCanvas(
                targetCanvas = pdfPage.canvas,
                targetWidth = pageWidthPt,
                targetHeight = pageHeightPt,
                background = background,
                strokes = strokes,
                headerTitle = null,
                isDarkMode = isDarkMode,
                pdfPath = pdfPath,
                pdfPageIndex = pdfPageIndex,
                textBlocks = textBlocks,
                imageBlocks = imageBlocks
            )
            pdfDoc.finishPage(pdfPage)

            FileOutputStream(localFile).use { fos ->
                pdfDoc.writeTo(fos)
                fos.flush()
            }

            mediaUri = saveToMediaStoreDownloads(context, fileName, pdfDoc)
        } finally {
            pdfDoc.close()
        }

        val userMessage = if (mediaUri != null) {
            "✓ PDF salvo em Downloads/LeNote: $fileName"
        } else {
            "✓ PDF salvo: $fileName"
        }

        return ExportResult(
            localFile = localFile,
            uri = mediaUri,
            fileName = fileName,
            userMessage = userMessage
        )
    }

    /**
     * Exporta múltiplas páginas de uma Seção para um único documento `.pdf` multipágina via MediaStore.
     */
    fun exportSectionToPdf(
        context: Context,
        sectionTitle: String,
        pagesWithStrokes: List<Pair<PageEntity, List<Stroke>>>,
        pageWidthPt: Int = 1200,
        pageHeightPt: Int = 1600,
        isDarkMode: Boolean = false
    ): ExportResult {
        val fileName = generateExportFileName(sectionTitle, "pdf")
        val exportsDir = File(context.filesDir, "exports").apply { mkdirs() }
        val localFile = File(exportsDir, fileName)

        val pdfDoc = PdfDocument()
        val mediaUri: Uri?
        try {
            pagesWithStrokes.forEachIndexed { index, (pageEntity, strokes) ->
                val bg = pageEntity.pageBackground
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidthPt, pageHeightPt, index + 1).create()
                val pdfPage = pdfDoc.startPage(pageInfo)
                renderPageToCanvas(
                    targetCanvas = pdfPage.canvas,
                    targetWidth = pageWidthPt,
                    targetHeight = pageHeightPt,
                    background = bg,
                    strokes = strokes,
                    headerTitle = null,
                    isDarkMode = isDarkMode,
                    pdfPath = pageEntity.pdfPath,
                    pdfPageIndex = pageEntity.pdfPageIndex
                )
                pdfDoc.finishPage(pdfPage)
            }

            FileOutputStream(localFile).use { fos ->
                pdfDoc.writeTo(fos)
                fos.flush()
            }

            mediaUri = saveToMediaStoreDownloads(context, fileName, pdfDoc)
        } finally {
            pdfDoc.close()
        }

        val userMessage = if (mediaUri != null) {
            "✓ PDF salvo em Downloads/LeNote: $fileName"
        } else {
            "✓ PDF salvo: $fileName"
        }

        return ExportResult(
            localFile = localFile,
            uri = mediaUri,
            fileName = fileName,
            userMessage = userMessage
        )
    }


    /**
     * Exporta todas as páginas de um Caderno para um único documento `.pdf` multipágina via MediaStore.
     * Preserva as páginas originais de PDF com anotações sobrepostas em alta fidelidade.
     */
    fun exportNotebookToPdf(
        context: Context,
        notebookTitle: String,
        pagesWithStrokes: List<Pair<PageEntity, List<Stroke>>>,
        pageWidthPt: Int = 1200,
        pageHeightPt: Int = 1600,
        isDarkMode: Boolean = false
    ): ExportResult {
        return exportSectionToPdf(
            context = context,
            sectionTitle = notebookTitle,
            pagesWithStrokes = pagesWithStrokes,
            pageWidthPt = pageWidthPt,
            pageHeightPt = pageHeightPt,
            isDarkMode = isDarkMode
        )
    }

    private fun drawBackgroundInWorld(
        canvas: Canvas,
        background: PageBackground,
        worldLeft: Float,
        worldTop: Float,
        worldRight: Float,
        worldBottom: Float,
        scale: Float,
        isDarkMode: Boolean = false
    ) {
        if (background == PageBackground.BLANK) return

        val safeScale = max(0.1f, scale)
        val thinStrokeWorld = 1.2f / safeScale
        val mediumStrokeWorld = 2.0f / safeScale
        val axisStrokeWorld = 2.6f / safeScale

        if (isDarkMode) {
            ruledLinePaint.color = 0xFF2A3644.toInt()
            marginLinePaint.color = 0xFF5C2D32.toInt()
            gridMinorPaint.color = 0xFF1E2836.toInt()
            gridMajorPaint.color = 0xFF33445C.toInt()
            mathAxisPaint.color = 0xFF4F698A.toInt()
            dotPaint.color = 0xFF475569.toInt()
        } else {
            ruledLinePaint.color = 0xFFBBDEFB.toInt()
            marginLinePaint.color = 0xFFFFCDD2.toInt()
            gridMinorPaint.color = 0xFFE3EAF2.toInt()
            gridMajorPaint.color = 0xFFB0C4DE.toInt()
            mathAxisPaint.color = 0xFF78909C.toInt()
            dotPaint.color = 0xFFB0BEC5.toInt()
        }

        when (background) {
            PageBackground.BLANK -> Unit
            PageBackground.RULED -> {
                val lineSpacing = 48f
                ruledLinePaint.strokeWidth = thinStrokeWorld
                marginLinePaint.strokeWidth = mediumStrokeWorld

                var y = floor(worldTop / lineSpacing) * lineSpacing
                while (y <= worldBottom) {
                    canvas.drawLine(worldLeft, y, worldRight, y, ruledLinePaint)
                    y += lineSpacing
                }

                val marginX = 96f
                if (marginX in worldLeft..worldRight) {
                    canvas.drawLine(marginX, worldTop, marginX, worldBottom, marginLinePaint)
                }
            }
            PageBackground.GRID -> {
                val gridStep = 40f
                gridMinorPaint.strokeWidth = thinStrokeWorld

                var x = floor(worldLeft / gridStep) * gridStep
                while (x <= worldRight) {
                    canvas.drawLine(x, worldTop, x, worldBottom, gridMinorPaint)
                    x += gridStep
                }

                var y = floor(worldTop / gridStep) * gridStep
                while (y <= worldBottom) {
                    canvas.drawLine(worldLeft, y, worldRight, y, gridMinorPaint)
                    y += gridStep
                }
            }
            PageBackground.DOTS -> {
                val dotStep = 40f
                val dotRadiusWorld = (2.2f / safeScale).coerceIn(0.8f, 3.5f)
                var x = floor(worldLeft / dotStep) * dotStep
                while (x <= worldRight) {
                    var y = floor(worldTop / dotStep) * dotStep
                    while (y <= worldBottom) {
                        canvas.drawCircle(x, y, dotRadiusWorld, dotPaint)
                        y += dotStep
                    }
                    x += dotStep
                }
            }
            PageBackground.MATHEMATICS -> {
                val minorStep = 24f
                val majorMultiple = 5
                gridMinorPaint.strokeWidth = thinStrokeWorld
                gridMajorPaint.strokeWidth = mediumStrokeWorld
                mathAxisPaint.strokeWidth = axisStrokeWorld

                val firstCol = floor(worldLeft / minorStep).toInt()
                val lastCol = ceil(worldRight / minorStep).toInt()
                for (col in firstCol..lastCol) {
                    val x = col * minorStep
                    val paint = when {
                        col == 0 -> mathAxisPaint
                        col % majorMultiple == 0 -> gridMajorPaint
                        else -> gridMinorPaint
                    }
                    canvas.drawLine(x, worldTop, x, worldBottom, paint)
                }

                val firstRow = floor(worldTop / minorStep).toInt()
                val lastRow = ceil(worldBottom / minorStep).toInt()
                for (row in firstRow..lastRow) {
                    val y = row * minorStep
                    val paint = when {
                        row == 0 -> mathAxisPaint
                        row % majorMultiple == 0 -> gridMajorPaint
                        else -> gridMinorPaint
                    }
                    canvas.drawLine(worldLeft, y, worldRight, y, paint)
                }
            }
        }
    }
}

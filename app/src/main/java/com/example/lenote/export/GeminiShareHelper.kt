package com.example.lenote.export

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.FileProvider
import androidx.ink.strokes.Stroke
import com.example.lenote.data.CanvasImageBlock
import com.example.lenote.data.CanvasTextBlock
import com.example.lenote.ink.PageBackground
import com.example.lenote.ink.SelectionBoundsBox
import com.example.lenote.ui.i18n.LeNoteStrings
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Ações rápidas disponíveis ao enviar uma seleção do Laço ou uma página inteira para o Gemini.
 */
enum class GeminiPromptAction {
    EXPLAIN,
    SOLVE,
    SUMMARIZE,
    FREE
}

/**
 * Resultado da tentativa de compartilhamento com o app do Gemini (ou seletor do sistema).
 */
data class GeminiShareOutcome(
    val isSuccess: Boolean,
    val launchedDirectlyInGemini: Boolean = false,
    val promptCopiedToClipboard: Boolean = false,
    val errorMessage: String? = null
)

/**
 * Utilitário 100% local e sem API keys para recortar traços selecionados (ou renderizar a folha
 * inteira) em PNG nítido no cache temporário e enviar diretamente para o app do Gemini instalado
 * no dispositivo (usando a conta pessoal do próprio usuário), com fallback automático para o
 * seletor de compartilhamento do Android.
 */
object GeminiShareHelper {

    private const val SHARE_CACHE_DIR = "gemini_shares"
    private const val GEMINI_APP_PACKAGE = "com.google.android.apps.bard"
    private const val MIN_CROP_EDGE_PX = 512
    private const val TARGET_CROP_LONG_EDGE_PX = 1440
    private const val MAX_CROP_EDGE_PX = 2048

    /**
     * Monta o prompt localizado (PT / EN / ES) correspondente à ação escolhida pelo usuário.
     * Retorna `null` para [GeminiPromptAction.FREE], enviando apenas a imagem.
     */
    fun buildPrompt(
        action: GeminiPromptAction,
        strings: LeNoteStrings,
        isFullPage: Boolean
    ): String? {
        return when (action) {
            GeminiPromptAction.EXPLAIN ->
                if (isFullPage) strings.geminiPromptExplainPage else strings.geminiPromptExplainSelection
            GeminiPromptAction.SOLVE ->
                if (isFullPage) strings.geminiPromptSolvePage else strings.geminiPromptSolveSelection
            GeminiPromptAction.SUMMARIZE ->
                if (isFullPage) strings.geminiPromptSummarizePage else strings.geminiPromptSummarizeSelection
            GeminiPromptAction.FREE -> null
        }
    }

    /**
     * Calcula o retângulo em coordenadas do mundo `[left, top, right, bottom]` para o recorte
     * do Laço com uma margem confortável ao redor dos traços selecionados.
     */
    fun computeSelectionCropViewport(bounds: SelectionBoundsBox): FloatArray {
        val rawW = max(1f, bounds.xMax - bounds.xMin)
        val rawH = max(1f, bounds.yMax - bounds.yMin)
        val padding = max(36f, min(rawW, rawH) * 0.14f).coerceAtMost(120f)
        return floatArrayOf(
            bounds.xMin - padding,
            bounds.yMin - padding,
            bounds.xMax + padding,
            bounds.yMax + padding
        )
    }

    /**
     * Calcula dimensões de saída em pixels `(width, height)` preservando a proporção do recorte
     * e garantindo alta nitidez para leitura multimodal pelo Gemini.
     */
    fun computeCropBitmapSize(worldWidth: Float, worldHeight: Float): Pair<Int, Int> {
        val safeW = max(1f, worldWidth)
        val safeH = max(1f, worldHeight)
        val longEdge = max(safeW, safeH)
        val desiredScale = TARGET_CROP_LONG_EDGE_PX / longEdge
        var outW = (safeW * desiredScale).roundToInt()
        var outH = (safeH * desiredScale).roundToInt()

        val maxDim = max(outW, outH)
        if (maxDim > MAX_CROP_EDGE_PX) {
            val factor = MAX_CROP_EDGE_PX.toFloat() / maxDim.toFloat()
            outW = (outW * factor).roundToInt()
            outH = (outH * factor).roundToInt()
        }

        // Garante largura/altura mínimas razoáveis para recortes muito finos (ex: 1 linha de equação)
        val minAllowed = min(MIN_CROP_EDGE_PX, MAX_CROP_EDGE_PX)
        if (max(outW, outH) < minAllowed) {
            val factor = minAllowed.toFloat() / max(1, max(outW, outH)).toFloat()
            outW = (outW * factor).roundToInt()
            outH = (outH * factor).roundToInt()
        }

        return Pair(outW.coerceIn(128, MAX_CROP_EDGE_PX), outH.coerceIn(128, MAX_CROP_EDGE_PX))
    }

    /**
     * Renderiza apenas a área selecionada pelo Laço (incluindo fundo/PDF/mídia sob o recorte e
     * os traços selecionados) em um arquivo PNG temporário no cache do app.
     */
    fun renderSelectionCropToPngFile(
        context: Context,
        bounds: SelectionBoundsBox,
        selectedStrokes: List<Stroke>,
        background: PageBackground,
        isDarkMode: Boolean = false,
        paperColor: Long = 0xFFFFFFFF,
        pdfPath: String? = null,
        pdfPageIndex: Int? = null,
        textBlocks: List<CanvasTextBlock> = emptyList(),
        imageBlocks: List<CanvasImageBlock> = emptyList()
    ): File {
        val viewport = computeSelectionCropViewport(bounds)
        val worldW = max(1f, viewport[2] - viewport[0])
        val worldH = max(1f, viewport[3] - viewport[1])
        val (widthPx, heightPx) = computeCropBitmapSize(worldW, worldH)

        val bmp = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bmp)
            PageExporter.renderPageToCanvas(
                targetCanvas = canvas,
                targetWidth = widthPx,
                targetHeight = heightPx,
                background = background,
                strokes = selectedStrokes,
                headerTitle = null,
                isDarkMode = isDarkMode,
                paperColor = paperColor,
                pdfPath = pdfPath,
                pdfPageIndex = pdfPageIndex,
                textBlocks = textBlocks,
                imageBlocks = imageBlocks,
                customWorldViewport = viewport
            )
            return writeTempSharePng(context, bmp, "lenote_gemini_selection")
        } finally {
            bmp.recycle()
        }
    }

    /**
     * Renderiza a página inteira em um arquivo PNG temporário de alta resolução no cache do app.
     */
    fun renderFullPageToPngFile(
        context: Context,
        background: PageBackground,
        strokes: List<Stroke>,
        isDarkMode: Boolean = false,
        paperColor: Long = 0xFFFFFFFF,
        pdfPath: String? = null,
        pdfPageIndex: Int? = null,
        textBlocks: List<CanvasTextBlock> = emptyList(),
        imageBlocks: List<CanvasImageBlock> = emptyList(),
        width: Int = 1600,
        height: Int = 2133
    ): File {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bmp)
            PageExporter.renderPageToCanvas(
                targetCanvas = canvas,
                targetWidth = width,
                targetHeight = height,
                background = background,
                strokes = strokes,
                headerTitle = null,
                isDarkMode = isDarkMode,
                paperColor = paperColor,
                pdfPath = pdfPath,
                pdfPageIndex = pdfPageIndex,
                textBlocks = textBlocks,
                imageBlocks = imageBlocks
            )
            return writeTempSharePng(context, bmp, "lenote_gemini_page")
        } finally {
            bmp.recycle()
        }
    }

    private fun writeTempSharePng(context: Context, bitmap: Bitmap, prefix: String): File {
        val dir = File(context.cacheDir, SHARE_CACHE_DIR).apply { mkdirs() }
        // Remove recortes temporários antigos para não acumular espaço em cache
        dir.listFiles()?.forEach { oldFile ->
            runCatching { oldFile.delete() }
        }
        val outFile = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { fos ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            fos.flush()
        }
        return outFile
    }

    /**
     * Compartilha o arquivo PNG via [FileProvider] diretamente com o app oficial do Gemini
     * (`com.google.android.apps.bard`), usando o seletor nativo do Android como fallback.
     *
     * Quando [prompt] não é nulo, também copia o texto para a área de transferência para garantir
     * praticidade caso o app destino ignore `EXTRA_TEXT` ao receber uma imagem.
     */
    fun shareImageToGemini(
        context: Context,
        imageFile: File,
        prompt: String?,
        chooserTitle: String
    ): GeminiShareOutcome {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )

            var copiedToClipboard = false
            if (!prompt.isNullOrBlank()) {
                runCatching {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("LeNote Gemini Prompt", prompt))
                    copiedToClipboard = true
                }
            }

            val baseIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("LeNote", uri)
                if (!prompt.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, prompt)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            // 1. Tenta abrir diretamente no app oficial do Gemini
            try {
                val directIntent = Intent(baseIntent).setPackage(GEMINI_APP_PACKAGE)
                context.startActivity(directIntent)
                return GeminiShareOutcome(
                    isSuccess = true,
                    launchedDirectlyInGemini = true,
                    promptCopiedToClipboard = copiedToClipboard
                )
            } catch (_: ActivityNotFoundException) {
                // Gemini não instalado ou não aceita share direto — segue para o seletor do sistema
            }

            // 2. Fallback: Seletor nativo de compartilhamento do Android
            val chooser = Intent.createChooser(baseIntent, chooserTitle).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
            GeminiShareOutcome(
                isSuccess = true,
                launchedDirectlyInGemini = false,
                promptCopiedToClipboard = copiedToClipboard
            )
        } catch (e: Throwable) {
            GeminiShareOutcome(
                isSuccess = false,
                errorMessage = e.message
            )
        }
    }
}

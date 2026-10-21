package com.example.lenote.ink

import android.content.Context
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import com.example.lenote.data.RecognizedWordBox
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognition
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModel
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognitionModelIdentifier
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizer
import com.google.mlkit.vision.digitalink.recognition.DigitalInkRecognizerOptions
import com.google.mlkit.vision.digitalink.recognition.Ink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.math.min

/**
 * Resultado completo do reconhecimento de escrita digital contendo o texto reconhecido
 * e caixas delimitadoras aproximadas por palavra calculadas a partir dos traços.
 */
data class DigitalInkRecognitionResult(
    val fullText: String,
    val wordBoxes: List<RecognizedWordBox>
)

/**
 * Utilitário e gerenciador assíncrono para Google ML Kit Digital Ink Recognition.
 * - Gerencia o ciclo de vida do modelo offline (padrão pt-BR ou locale do sistema);
 * - Converte instâncias de [androidx.ink.strokes.Stroke] em [com.google.mlkit.vision.digitalink.recognition.Ink];
 * - Executa inferência assíncrona fora da thread principal (`Dispatchers.Default`).
 */
class DigitalInkManager(
    private val context: Context,
    private val languageTag: String = resolveDefaultLanguageTag()
) {

    private val modelManager = RemoteModelManager.getInstance()
    private var model: DigitalInkRecognitionModel? = null
    private var recognizer: DigitalInkRecognizer? = null
    private var isModelDownloaded: Boolean = false

    companion object {
        fun resolveDefaultLanguageTag(): String {
            val systemLocale = Locale.getDefault()
            val tag = systemLocale.toLanguageTag()
            // Tenta o locale do sistema; se não for suportado pelo ML Kit, recorre a pt-BR ou en-US
            if (DigitalInkRecognitionModelIdentifier.fromLanguageTag(tag) != null) {
                return tag
            }
            if (DigitalInkRecognitionModelIdentifier.fromLanguageTag("pt-BR") != null) {
                return "pt-BR"
            }
            return "en-US"
        }

        /**
         * Converte uma lista de [androidx.ink.strokes.Stroke] do Jetpack Ink para o formato [Ink] do ML Kit.
         */
        fun convertStrokesToMlKitInk(strokes: List<Stroke>): Ink {
            val inkBuilder = Ink.builder()
            for (stroke in strokes) {
                val strokeBuilder = Ink.Stroke.builder()
                val inputs = stroke.inputs
                val inputCount = inputs.size
                if (inputCount == 0) continue

                val tempInput = StrokeInput()
                for (i in 0 until inputCount) {
                    inputs.populate(i, tempInput)
                    strokeBuilder.addPoint(
                        Ink.Point.create(
                            tempInput.x,
                            tempInput.y,
                            tempInput.elapsedTimeMillis
                        )
                    )
                }
                inkBuilder.addStroke(strokeBuilder.build())
            }
            return inkBuilder.build()
        }
    }

    /**
     * Garante de forma assíncrona que o modelo de idioma do ML Kit esteja baixado e inicializado.
     */
    suspend fun ensureModelDownloaded(): Boolean = withContext(Dispatchers.IO) {
        val identifier = DigitalInkRecognitionModelIdentifier.fromLanguageTag(languageTag)
            ?: DigitalInkRecognitionModelIdentifier.fromLanguageTag("pt-BR")
            ?: DigitalInkRecognitionModelIdentifier.fromLanguageTag("en-US")
            ?: return@withContext false

        val currentModel = DigitalInkRecognitionModel.builder(identifier).build()
        this@DigitalInkManager.model = currentModel

        val isDownloaded = suspendCancellableCoroutine<Boolean> { cont ->
            modelManager.isModelDownloaded(currentModel)
                .addOnSuccessListener { downloaded ->
                    if (cont.isActive) cont.resume(downloaded)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(false)
                }
        }

        if (isDownloaded) {
            isModelDownloaded = true
            initRecognizer(currentModel)
            return@withContext true
        }

        // Se ainda não foi baixado, realiza o download
        val downloadSuccess = suspendCancellableCoroutine<Boolean> { cont ->
            val conditions = DownloadConditions.Builder().build()
            modelManager.download(currentModel, conditions)
                .addOnSuccessListener {
                    if (cont.isActive) cont.resume(true)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(false)
                }
        }

        if (downloadSuccess) {
            isModelDownloaded = true
            initRecognizer(currentModel)
        }
        downloadSuccess
    }

    private fun initRecognizer(model: DigitalInkRecognitionModel) {
        if (recognizer == null) {
            val options = DigitalInkRecognizerOptions.builder(model).build()
            recognizer = DigitalInkRecognition.getClient(options)
        }
    }

    /**
     * Reconhece o texto desenhado a partir dos traços fornecidos.
     * Retorna o texto e as caixas delimitadoras estimadas por palavra.
     */
    suspend fun recognizeStrokes(strokes: List<Stroke>): Result<DigitalInkRecognitionResult> = withContext(Dispatchers.Default) {
        if (strokes.isEmpty()) {
            return@withContext Result.success(DigitalInkRecognitionResult("", emptyList()))
        }

        val ready = ensureModelDownloaded()
        val currentRecognizer = recognizer
        if (!ready || currentRecognizer == null) {
            return@withContext Result.failure(IllegalStateException("Modelo Digital Ink indisponível ou download falhou."))
        }

        val ink = convertStrokesToMlKitInk(strokes)

        val mlKitResult = suspendCancellableCoroutine<String?> { cont ->
            currentRecognizer.recognize(ink)
                .addOnSuccessListener { result ->
                    val text = result.candidates.firstOrNull()?.text ?: ""
                    if (cont.isActive) cont.resume(text)
                }
                .addOnFailureListener { ex ->
                    if (cont.isActive) cont.resumeWith(Result.failure(ex))
                }
        }

        val fullText = mlKitResult?.trim() ?: ""
        if (fullText.isEmpty()) {
            return@withContext Result.success(DigitalInkRecognitionResult("", emptyList()))
        }

        // Calcula a bounding box global dos traços fornecidos
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        val temp = StrokeInput()
        for (s in strokes) {
            val count = s.inputs.size
            for (i in 0 until count) {
                s.inputs.populate(i, temp)
                minX = min(minX, temp.x)
                minY = min(minY, temp.y)
                maxX = max(maxX, temp.x)
                maxY = max(maxY, temp.y)
            }
        }

        if (minX == Float.MAX_VALUE) {
            minX = 0f
            maxX = 100f
            minY = 0f
            maxY = 40f
        }

        // Divide o texto reconhecido em palavras e distribui caixas delimitadoras proporcionais
        val words = fullText.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val wordBoxes = mutableListOf<RecognizedWordBox>()

        if (words.size <= 1) {
            wordBoxes.add(
                RecognizedWordBox(
                    word = fullText,
                    left = minX,
                    top = minY,
                    right = maxX,
                    bottom = maxY
                )
            )
        } else {
            val totalChars = words.sumOf { it.length }.coerceAtLeast(1)
            val totalWidth = (maxX - minX).coerceAtLeast(10f)
            val gapWidth = (totalWidth * 0.05f) / (words.size - 1)
            val usableWidth = totalWidth - (gapWidth * (words.size - 1))

            var currentX = minX
            for (w in words) {
                val wWidth = (w.length.toFloat() / totalChars) * usableWidth
                wordBoxes.add(
                    RecognizedWordBox(
                        word = w,
                        left = currentX,
                        top = minY,
                        right = currentX + wWidth,
                        bottom = maxY
                    )
                )
                currentX += wWidth + gapWidth
            }
        }

        Result.success(DigitalInkRecognitionResult(fullText, wordBoxes))
    }

    fun close() {
        recognizer?.close()
        recognizer = null
    }
}

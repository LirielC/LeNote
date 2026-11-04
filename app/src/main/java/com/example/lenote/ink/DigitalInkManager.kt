package com.example.lenote.ink

import android.content.Context
import androidx.ink.strokes.Stroke
import androidx.ink.strokes.StrokeInput
import com.example.lenote.data.RecognizedWordBox
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

/**
 * Resultado completo da análise geométrica de escrita digital contendo o texto reconhecido
 * (quando disponível) e as caixas delimitadoras por grupo de traços/palavras calculadas localmente.
 */
data class DigitalInkRecognitionResult(
    val fullText: String,
    val wordBoxes: List<RecognizedWordBox>
)

/**
 * Gerenciador 100% FOSS e offline de análise geométrica de traços manuscritos.
 * Opera exclusivamente com geometria local dos [Stroke]s do Jetpack Ink, sem dependências
 * proprietárias de rede, GMS, Firebase ou ML Kit.
 */
class DigitalInkManager(
    @Suppress("UNUSED_PARAMETER") private val context: Context,
    private val languageTag: String = resolveDefaultLanguageTag()
) {

    companion object {
        fun resolveDefaultLanguageTag(): String {
            val tag = Locale.getDefault().toLanguageTag()
            return tag.ifBlank { "pt-BR" }
        }
    }

    suspend fun ensureModelDownloaded(): Boolean = withContext(Dispatchers.IO) {
        true
    }

    /**
     * Analisa geometricamente os traços fornecidos e agrupa-os em caixas delimitadoras de palavras
     * com base na proximidade horizontal/vertical, permitindo alinhamento de linha de base (Baseline Snap)
     * e refinamento caligráfico 100% offline.
     */
    suspend fun recognizeStrokes(strokes: List<Stroke>): Result<DigitalInkRecognitionResult> =
        withContext(Dispatchers.Default) {
            if (strokes.isEmpty()) {
                return@withContext Result.success(DigitalInkRecognitionResult("", emptyList()))
            }

            data class StrokeBounds(
                val left: Float,
                val top: Float,
                val right: Float,
                val bottom: Float
            )

            val strokeBoundsList = mutableListOf<StrokeBounds>()
            val scratch = StrokeInput()

            for (s in strokes) {
                val count = s.inputs.size
                if (count == 0) continue
                var sMinX = Float.MAX_VALUE
                var sMinY = Float.MAX_VALUE
                var sMaxX = -Float.MAX_VALUE
                var sMaxY = -Float.MAX_VALUE
                for (i in 0 until count) {
                    s.inputs.populate(i, scratch)
                    sMinX = min(sMinX, scratch.x)
                    sMinY = min(sMinY, scratch.y)
                    sMaxX = max(sMaxX, scratch.x)
                    sMaxY = max(sMaxY, scratch.y)
                }
                if (sMinX <= sMaxX && sMinY <= sMaxY) {
                    strokeBoundsList.add(StrokeBounds(sMinX, sMinY, sMaxX, sMaxY))
                }
            }

            if (strokeBoundsList.isEmpty()) {
                return@withContext Result.success(DigitalInkRecognitionResult("", emptyList()))
            }

            // Ordena da esquerda para a direita e agrupa traços próximos em caixas de palavras
            val sorted = strokeBoundsList.sortedBy { it.left }
            val wordBoxes = mutableListOf<RecognizedWordBox>()
            var curLeft = sorted[0].left
            var curTop = sorted[0].top
            var curRight = sorted[0].right
            var curBottom = sorted[0].bottom
            var wordIndex = 1

            val wordGapThreshold = 28f
            for (i in 1 until sorted.size) {
                val b = sorted[i]
                if (b.left - curRight <= wordGapThreshold) {
                    curLeft = min(curLeft, b.left)
                    curTop = min(curTop, b.top)
                    curRight = max(curRight, b.right)
                    curBottom = max(curBottom, b.bottom)
                } else {
                    wordBoxes.add(
                        RecognizedWordBox(
                            word = "w$wordIndex",
                            left = curLeft,
                            top = curTop,
                            right = curRight,
                            bottom = curBottom
                        )
                    )
                    wordIndex++
                    curLeft = b.left
                    curTop = b.top
                    curRight = b.right
                    curBottom = b.bottom
                }
            }
            wordBoxes.add(
                RecognizedWordBox(
                    word = "w$wordIndex",
                    left = curLeft,
                    top = curTop,
                    right = curRight,
                    bottom = curBottom
                )
            )

            Result.success(DigitalInkRecognitionResult("", wordBoxes))
        }

    fun close() {
        // Sem recursos nativos proprietários para liberar
    }
}

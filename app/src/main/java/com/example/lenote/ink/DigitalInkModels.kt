package com.example.lenote.data

import android.graphics.RectF

/**
 * Representa um termo/palavra reconhecida e sua caixa delimitadora no canvas.
 */
data class RecognizedWordBox(
    val word: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun toRectF(): RectF = RectF(left, top, right, bottom)
}

/**
 * Resultado de busca FTS5 incluindo a página correspondente e as caixas para realce.
 */
data class HandwritingSearchResult(
    val page: PageEntity,
    val matchedWordBoxes: List<RecognizedWordBox>
)

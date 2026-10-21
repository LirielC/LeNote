package com.example.lenote.ink

import kotlin.math.max
import kotlin.math.min

/**
 * Utilitário puro em Kotlin para inversão inteligente de cores no modo escuro.
 * Preserva o contraste no fundo escuro mantendo cores vibrantes e legibilidade.
 * Não depende do android.graphics.Color nativo para permitir execução pura em JVM unit tests.
 */
object ColorInversionHelper {

    /**
     * Ajusta uma cor ARGB para visualização em fundo escuro:
     * - Cores pretas ou de tom muito escuro são invertidas para tons claros/brancos.
     * - Cores vibrantes (vermelho, azul, verde, etc.) e marca-textos mantêm seu matiz/saturação com leve boost de luminância se necessário.
     * - Cores muito claras no modo claro continuam claras.
     */
    fun invertColorForDarkMode(colorInt: Int): Int {
        val a = (colorInt ushr 24) and 0xFF
        val r = (colorInt ushr 16) and 0xFF
        val g = (colorInt ushr 8) and 0xFF
        val b = colorInt and 0xFF

        val rNorm = r / 255f
        val gNorm = g / 255f
        val bNorm = b / 255f

        // Calcula luminância relativa (padrão ITU-R BT.709)
        val luminance = 0.2126f * rNorm + 0.7152f * gNorm + 0.0722f * bNorm

        val maxC = max(rNorm, max(gNorm, bNorm))
        val minC = min(rNorm, min(gNorm, bNorm))
        val delta = maxC - minC
        val saturation = if (maxC == 0f) 0f else delta / maxC
        val value = maxC

        // Se for essencialmente preto ou cinza escuro neutro (baixa saturação e baixa luminância)
        if (saturation < 0.20f && luminance < 0.35f) {
            // Mapeia preto puro para cinza muito claro / branco quase puro para não cansar a vista (#F1F5F9)
            val invertedGray = ((1f - luminance) * 255).toInt().coerceIn(210, 245)
            return (a shl 24) or (invertedGray shl 16) or (invertedGray shl 8) or invertedGray
        }

        // Se for uma cor com saturação, mas muito escura em valor (ex: azul marinho muito escuro)
        if (value < 0.45f && value > 0f) {
            val boostRatio = 0.85f / value
            val newR = (rNorm * boostRatio * 255).toInt().coerceIn(0, 255)
            val newG = (gNorm * boostRatio * 255).toInt().coerceIn(0, 255)
            val newB = (bNorm * boostRatio * 255).toInt().coerceIn(0, 255)
            return (a shl 24) or (newR shl 16) or (newG shl 8) or newB
        }

        // Cores vibrantes ou marca-textos: mantém a cor original
        return colorInt
    }

    /**
     * Retorna a cor de papel apropriada para o modo escuro ou claro.
     */
    fun getPaperBackgroundColor(isDarkMode: Boolean): Int {
        return if (isDarkMode) 0xFF121418.toInt() else 0xFFFCFCFA.toInt()
    }
}

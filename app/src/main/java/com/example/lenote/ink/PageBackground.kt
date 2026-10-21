package com.example.lenote.ink

/**
 * Tipos de folha (background no espaço do mundo) para o editor de escrita manual e exportação.
 */
enum class PageBackground(val label: String) {
    BLANK("Branca"),
    RULED("Pautada"),
    GRID("Quadriculada"),
    DOTS("Pontilhada"),
    MATHEMATICS("Matemática"),
    CORNELL("Método Cornell"),
    WEEKLY_PLANNER("Planner Semanal"),
    ENGINEERING_GRAPH("Papel Engenharia"),
    MUSIC_STAFF("Partitura Musical")
}

/**
 * Cores de papel padrão suportadas pelo LeNote.
 */
object PaperColors {
    const val CLASSIC_WHITE = 0xFFFFFFFF
    const val LEGAL_YELLOW = 0xFFFFF9D2
    const val SEPIA_CREAM = 0xFFF8F3E6
    const val OLED_BLACK = 0xFF121212

    val DefaultOptions = listOf(
        PaperColorOption("Branco", CLASSIC_WHITE, 0xFF0F172A),
        PaperColorOption("Amarelo Legal", LEGAL_YELLOW, 0xFF1E3A8A),
        PaperColorOption("Sépia Suave", SEPIA_CREAM, 0xFF2D241E),
        PaperColorOption("Grafite OLED", OLED_BLACK, 0xFFF1F5F9)
    )

    fun isDarkPaper(colorLong: Long): Boolean {
        val colorInt = colorLong.toInt()
        val r = (colorInt shr 16) and 0xFF
        val g = (colorInt shr 8) and 0xFF
        val b = colorInt and 0xFF
        // Luminância relativa perceptiva
        val luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        return luminance < 0.4
    }
}

data class PaperColorOption(
    val name: String,
    val colorLong: Long,
    val textColorLong: Long
)

/**
 * Tamanhos configuráveis para a ferramenta Borracha (Eraser).
 */
enum class EraserSize(val label: String, val worldRadius: Float) {
    SMALL("P", 12f),
    MEDIUM("M", 24f),
    LARGE("G", 42f)
}

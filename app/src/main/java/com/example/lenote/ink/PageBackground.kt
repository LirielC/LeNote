package com.example.lenote.ink

/**
 * Tipos de folha (background no espaço do mundo) para o editor de escrita manual.
 */
enum class PageBackground(val label: String) {
    BLANK("Branca"),
    RULED("Pautada"),
    GRID("Quadriculada"),
    DOTS("Pontilhada"),
    MATHEMATICS("Matemática")
}

/**
 * Tamanhos configuráveis para a ferramenta Borracha (Eraser).
 *
 * [worldRadius] define o raio base no espaço do mundo (world space).
 * Em coordenadas de tela, o raio visual/efetivo acompanha o nível de zoom (`worldRadius * zoomLevel`),
 * garantindo correspondência exata entre o indicador circular e a área real de intersecção.
 */
enum class EraserSize(val label: String, val worldRadius: Float) {
    SMALL("P", 12f),
    MEDIUM("M", 24f),
    LARGE("G", 42f)
}

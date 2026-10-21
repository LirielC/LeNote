package com.example.lenote.data

/**
 * Representa um bloco de texto editável posicionado no espaço de mundo (canvas).
 */
data class CanvasTextBlock(
    val id: String,
    val pageId: String,
    val text: String,
    val x: Float,
    val y: Float,
    val width: Float = 250f,
    val height: Float = 60f,
    val textColor: Int = 0xFF0F172A.toInt(),
    val fontSize: Float = 16f,
    val zIndex: Float = 1f
)

/**
 * Representa um bloco de imagem posicionado no espaço de mundo (canvas) com suporte a rotação e redimensionamento.
 */
data class CanvasImageBlock(
    val id: String,
    val pageId: String,
    val imagePath: String,
    val x: Float,
    val y: Float,
    val width: Float = 300f,
    val height: Float = 300f,
    val rotationDegrees: Float = 0f,
    val zIndex: Float = 0f
)

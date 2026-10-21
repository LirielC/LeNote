package com.example.lenote.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.data.CanvasImageBlock
import com.example.lenote.data.CanvasTextBlock
import com.example.lenote.ink.JetpackInkEngine
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Camada de Sobreposição do Compose para Caixas de Texto e Imagens Interativas.
 * Converte coordenadas do mundo (world coordinates) para a tela usando a matriz de câmera da [JetpackInkEngine].
 */
@Composable
fun CanvasMediaOverlay(
    engine: JetpackInkEngine,
    textBlocks: List<CanvasTextBlock>,
    imageBlocks: List<CanvasImageBlock>,
    isTextToolActive: Boolean,
    onAddTextBlock: (worldX: Float, worldY: Float) -> Unit,
    onUpdateTextBlock: (CanvasTextBlock) -> Unit,
    onDeleteTextBlock: (String) -> Unit,
    onUpdateImageBlock: (CanvasImageBlock) -> Unit,
    onDeleteImageBlock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val engineState = engine.state.value
    val zoom = engineState.zoomLevel
    val panX = engineState.panOffsetX
    val panY = engineState.panOffsetY

    var selectedImageId by remember { mutableStateOf<String?>(null) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(isTextToolActive, zoom, panX, panY) {
                if (isTextToolActive) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val worldX = (offset.x - panX) / zoom
                            val worldY = (offset.y - panY) / zoom
                            onAddTextBlock(worldX, worldY)
                        },
                        onDrag = { _, _ -> }
                    )
                }
            }
    ) {
        // 1. Renderiza Imagens Interativas
        for (img in imageBlocks.sortedBy { it.zIndex }) {
            val isSelected = selectedImageId == img.id
            val screenX = img.x * zoom + panX
            val screenY = img.y * zoom + panY
            val screenW = img.width * zoom
            val screenH = img.height * zoom

            InteractiveImageItem(
                imageBlock = img,
                screenX = screenX,
                screenY = screenY,
                screenW = screenW,
                screenH = screenH,
                zoom = zoom,
                isSelected = isSelected,
                onSelect = {
                    selectedImageId = img.id
                    selectedTextId = null
                },
                onUpdate = onUpdateImageBlock,
                onDelete = { onDeleteImageBlock(img.id) }
            )
        }

        // 2. Renderiza Caixas de Texto Interativas
        for (tb in textBlocks.sortedBy { it.zIndex }) {
            val isSelected = selectedTextId == tb.id
            val screenX = tb.x * zoom + panX
            val screenY = tb.y * zoom + panY
            val screenW = max(100f, tb.width * zoom)

            InteractiveTextItem(
                textBlock = tb,
                screenX = screenX,
                screenY = screenY,
                screenW = screenW,
                zoom = zoom,
                isSelected = isSelected,
                onSelect = {
                    selectedTextId = tb.id
                    selectedImageId = null
                },
                onUpdate = onUpdateTextBlock,
                onDelete = { onDeleteTextBlock(tb.id) }
            )
        }
    }
}

/**
 * Item interativo de imagem com rotação, alças de redimensionamento e profundidade.
 */
@Composable
private fun InteractiveImageItem(
    imageBlock: CanvasImageBlock,
    screenX: Float,
    screenY: Float,
    screenW: Float,
    screenH: Float,
    zoom: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdate: (CanvasImageBlock) -> Unit,
    onDelete: () -> Unit
) {
    val bitmap = remember(imageBlock.imagePath) {
        val file = File(imageBlock.imagePath)
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        } else null
    } ?: return

    var currentX by remember(imageBlock.x) { mutableFloatStateOf(imageBlock.x) }
    var currentY by remember(imageBlock.y) { mutableFloatStateOf(imageBlock.y) }
    var currentW by remember(imageBlock.width) { mutableFloatStateOf(imageBlock.width) }
    var currentH by remember(imageBlock.height) { mutableFloatStateOf(imageBlock.height) }
    var currentRotation by remember(imageBlock.rotationDegrees) { mutableFloatStateOf(imageBlock.rotationDegrees) }

    Box(
        modifier = Modifier
            .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
            .size(width = max(24f, screenW).dp, height = max(24f, screenH).dp)
            .rotate(currentRotation)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, Color(0xFF2563EB), RoundedCornerShape(8.dp))
                } else Modifier
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onSelect() },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        currentX += dragAmount.x / zoom
                        currentY += dragAmount.y / zoom
                        onUpdate(imageBlock.copy(x = currentX, y = currentY))
                    }
                )
            }
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Imagem inserida",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize()
        )

        // Controles de Manipulação quando selecionada
        if (isSelected) {
            // Barra de Ações Superiores (Excluir, Trazer para Frente, Girar)
            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-36).dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rotação suave em passos de 15 graus
                    IconButton(
                        onClick = {
                            currentRotation = (currentRotation + 15f) % 360f
                            onUpdate(imageBlock.copy(rotationDegrees = currentRotation))
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.ArrowClockwise,
                            contentDescription = "Girar",
                            tint = Color(0xFF1E3A8A),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // Excluir
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Trash,
                            contentDescription = "Excluir Imagem",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Alça de Redimensionamento no Canto Inferior Direito
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .background(Color(0xFF2563EB), CircleShape)
                    .pointerInput(zoom) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val dw = dragAmount.x / zoom
                            val dh = dragAmount.y / zoom
                            currentW = max(60f, currentW + dw)
                            currentH = max(60f, currentH + dh)
                            onUpdate(imageBlock.copy(width = currentW, height = currentH))
                        }
                    }
            )
        }
    }
}

/**
 * Item interativo de caixa de texto editável com arrasto e redimensionamento de largura.
 */
@Composable
private fun InteractiveTextItem(
    textBlock: CanvasTextBlock,
    screenX: Float,
    screenY: Float,
    screenW: Float,
    zoom: Float,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onUpdate: (CanvasTextBlock) -> Unit,
    onDelete: () -> Unit
) {
    var textValue by remember(textBlock.text) { mutableStateOf(textBlock.text) }
    var currentX by remember(textBlock.x) { mutableFloatStateOf(textBlock.x) }
    var currentY by remember(textBlock.y) { mutableFloatStateOf(textBlock.y) }
    var currentW by remember(textBlock.width) { mutableFloatStateOf(textBlock.width) }

    Box(
        modifier = Modifier
            .offset { IntOffset(screenX.roundToInt(), screenY.roundToInt()) }
            .width(screenW.dp)
            .then(
                if (isSelected) {
                    Modifier
                        .border(1.5.dp, Color(0xFF2563EB), RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .padding(6.dp)
    ) {
        Column {
            if (isSelected) {
                // Barra de Ações Superior (Arrastar e Excluir)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Alça de arraste
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .size(24.dp)
                            .pointerInput(zoom) {
                                detectDragGestures(
                                    onDragStart = { onSelect() },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        currentX += dragAmount.x / zoom
                                        currentY += dragAmount.y / zoom
                                        onUpdate(textBlock.copy(x = currentX, y = currentY))
                                    }
                                )
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = PhosphorIcons.Regular.DotsSixVertical,
                                contentDescription = "Arrastar",
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    // Botão Excluir
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.Trash,
                            contentDescription = "Excluir",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            BasicTextField(
                value = textValue,
                onValueChange = {
                    textValue = it
                    onUpdate(textBlock.copy(text = it))
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = (textBlock.fontSize * zoom).coerceAtLeast(12f).sp,
                    color = Color(textBlock.textColor)
                ),
                cursorBrush = SolidColor(Color(0xFF2563EB)),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { onSelect() },
                            onDrag = { _, _ -> }
                        )
                    }
            )
        }
    }
}

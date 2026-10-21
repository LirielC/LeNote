package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.lenote.ink.PageBackground
import com.example.lenote.ink.PaperColors

/**
 * Diálogo moderno para seleção de Modelo de Página (Page Template) e Cor do Papel.
 *
 * Pode ser utilizado tanto para:
 * 1. Criar uma nova página com modelo e cor personalizados;
 * 2. Alterar o modelo e a cor de uma página já existente sem perder traços.
 */
@Composable
fun PageTemplatePickerDialog(
    initialTitle: String = "",
    initialBackground: PageBackground = PageBackground.RULED,
    initialPaperColor: Long = PaperColors.CLASSIC_WHITE,
    isCreateMode: Boolean = true,
    onConfirm: (title: String, background: PageBackground, paperColor: Long) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var selectedBackground by remember { mutableStateOf(initialBackground) }
    var selectedPaperColor by remember { mutableStateOf(initialPaperColor) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabeçalho
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isCreateMode) "Nova Página" else "Modelo de Página",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isCreateMode) "Escolha o modelo e a cor da folha" else "Altere o fundo e a cor da folha atual",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.X,
                            contentDescription = "Fechar",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Campo de Título da Página (visível apenas no modo de criação)
                if (isCreateMode) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Nome da Página") },
                        placeholder = { Text("Ex: Resumo da Aula, Análise...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Seção 1: Cor do Papel
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "COR DO PAPEL",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PaperColors.DefaultOptions.forEach { option ->
                            val isSelected = selectedPaperColor == option.colorLong
                            val paperCardColor = Color(option.colorLong.toInt())

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedPaperColor = option.colorLong }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(paperCardColor)
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = PhosphorIcons.Regular.Check,
                                            contentDescription = "Selecionado",
                                            tint = if (PaperColors.isDarkPaper(option.colorLong)) Color.White else Color(0xFF1E3A8A),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = option.name,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Seção 2: Grade de Modelos Pedagógicos e Técnicos
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "MODELO DE FUNDO",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        items(PageBackground.entries) { bg ->
                            val isSelected = selectedBackground == bg
                            TemplateCardItem(
                                background = bg,
                                paperColor = selectedPaperColor,
                                isSelected = isSelected,
                                onClick = { selectedBackground = bg }
                            )
                        }
                    }
                }

                // Rodapé com botões de Ação
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(title.trim(), selectedBackground, selectedPaperColor)
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(if (isCreateMode) "Criar Página" else "Salvar Alterações")
                    }
                }
            }
        }
    }
}

/**
 * Item individual de modelo de página com miniatura vetorial procedural.
 */
@Composable
private fun TemplateCardItem(
    background: PageBackground,
    paperColor: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = Color(paperColor.toInt())
    val isDark = PaperColors.isDarkPaper(paperColor)
    val strokeColor = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(if (isSelected) 2.5.dp else 1.dp, strokeColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f),
                shape = RoundedCornerShape(6.dp),
                color = bgColor,
                border = BorderStroke(0.8f.dp, if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1))
            ) {
                TemplatePreviewCanvas(background = background, isDark = isDark)
            }

            Text(
                text = background.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Renderizador de miniatura vetorial para os 9 modelos de folha.
 */
@Composable
fun TemplatePreviewCanvas(
    background: PageBackground,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val lineColor = if (isDark) Color(0xFF64748B).copy(alpha = 0.6f) else Color(0xFF94A3B8).copy(alpha = 0.6f)
        val majorLineColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        val marginColor = if (isDark) Color(0xFFEF5350).copy(alpha = 0.7f) else Color(0xFFE57373).copy(alpha = 0.8f)

        val w = size.width
        val h = size.height

        when (background) {
            PageBackground.BLANK -> {}

            PageBackground.RULED -> {
                val spacing = h / 6f
                for (i in 1..5) {
                    val y = i * spacing
                    drawLine(color = lineColor, start = Offset(4f, y), end = Offset(w - 4f, y), strokeWidth = 0.9f)
                }
                drawLine(color = marginColor, start = Offset(w * 0.2f, 2f), end = Offset(w * 0.2f, h - 2f), strokeWidth = 1.2f)
            }

            PageBackground.GRID -> {
                val sx = w / 5f
                val sy = h / 6f
                for (i in 1 until 5) {
                    drawLine(color = lineColor.copy(alpha = 0.4f), start = Offset(i * sx, 2f), end = Offset(i * sx, h - 2f), strokeWidth = 0.8f)
                }
                for (i in 1 until 6) {
                    drawLine(color = lineColor.copy(alpha = 0.4f), start = Offset(2f, i * sy), end = Offset(w - 2f, i * sy), strokeWidth = 0.8f)
                }
            }

            PageBackground.DOTS -> {
                val sx = w / 4.5f
                val sy = h / 5.5f
                for (i in 1..4) {
                    for (j in 1..5) {
                        drawCircle(color = lineColor, radius = 1.1f, center = Offset(i * sx, j * sy))
                    }
                }
            }

            PageBackground.MATHEMATICS -> {
                val sx = w / 7f
                val sy = h / 7f
                for (i in 1 until 7) {
                    val isAxis = i == 3
                    drawLine(color = if (isAxis) majorLineColor else lineColor.copy(alpha = 0.35f), start = Offset(i * sx, 2f), end = Offset(i * sx, h - 2f), strokeWidth = if (isAxis) 1.2f else 0.6f)
                }
                for (i in 1 until 7) {
                    val isAxis = i == 3
                    drawLine(color = if (isAxis) majorLineColor else lineColor.copy(alpha = 0.35f), start = Offset(2f, i * sy), end = Offset(w - 2f, i * sy), strokeWidth = if (isAxis) 1.2f else 0.6f)
                }
            }

            PageBackground.CORNELL -> {
                val headerY = h * 0.18f
                val cueX = w * 0.28f
                val summaryY = h * 0.82f

                // Cabeçalho e Sumário
                drawLine(color = majorLineColor, start = Offset(2f, headerY), end = Offset(w - 2f, headerY), strokeWidth = 1.2f)
                drawLine(color = majorLineColor, start = Offset(2f, summaryY), end = Offset(w - 2f, summaryY), strokeWidth = 1.2f)

                // Linha de Cues
                drawLine(color = majorLineColor, start = Offset(cueX, headerY), end = Offset(cueX, summaryY), strokeWidth = 1.2f)

                // Pautas sutis nas notas
                val noteSpacing = (summaryY - headerY) / 5f
                for (i in 1..4) {
                    val y = headerY + i * noteSpacing
                    drawLine(color = lineColor.copy(alpha = 0.4f), start = Offset(cueX + 2f, y), end = Offset(w - 4f, y), strokeWidth = 0.7f)
                }
            }

            PageBackground.WEEKLY_PLANNER -> {
                val headerY = h * 0.2f
                drawLine(color = majorLineColor, start = Offset(2f, headerY), end = Offset(w - 2f, headerY), strokeWidth = 1.2f)

                val colW = w / 5f
                for (i in 1 until 5) {
                    drawLine(color = majorLineColor, start = Offset(i * colW, 2f), end = Offset(i * colW, h - 2f), strokeWidth = 1f)
                }
                val rowH = (h - headerY) / 3f
                for (j in 1..2) {
                    drawLine(color = lineColor.copy(alpha = 0.35f), start = Offset(2f, headerY + j * rowH), end = Offset(w - 2f, headerY + j * rowH), strokeWidth = 0.7f)
                }
            }

            PageBackground.ENGINEERING_GRAPH -> {
                val sx = w / 10f
                val sy = h / 10f
                for (i in 1 until 10) {
                    val isMajor = i % 5 == 0
                    drawLine(color = if (isMajor) majorLineColor else lineColor.copy(alpha = 0.3f), start = Offset(i * sx, 2f), end = Offset(i * sx, h - 2f), strokeWidth = if (isMajor) 1f else 0.5f)
                }
                for (i in 1 until 10) {
                    val isMajor = i % 5 == 0
                    drawLine(color = if (isMajor) majorLineColor else lineColor.copy(alpha = 0.3f), start = Offset(2f, i * sy), end = Offset(w - 2f, i * sy), strokeWidth = if (isMajor) 1f else 0.5f)
                }
            }

            PageBackground.MUSIC_STAFF -> {
                val groupGap = h * 0.28f
                for (g in 0..1) {
                    val startY = h * 0.2f + g * groupGap
                    val lineGap = 4.5f
                    for (l in 0..4) {
                        val y = startY + l * lineGap
                        drawLine(color = majorLineColor, start = Offset(4f, y), end = Offset(w - 4f, y), strokeWidth = 0.9f)
                    }
                }
            }
        }
    }
}

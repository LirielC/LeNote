package com.example.lenote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.ui.theme.LeNoteTheme

/**
 * Botão individual para uma ferramenta na toolbar flutuante com transição suave de cor.
 */
@Composable
internal fun ToolbarToolButton(
    iconRegular: ImageVector,
    iconFill: ImageVector,
    contentDescription: String,
    isSelected: Boolean,
    badgeColor: Color? = null,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) LeNoteTheme.colors.accentPrimaryContainer else Color.Transparent,
        animationSpec = tween(160),
        label = "ToolBtnBg"
    )
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkSecondary,
        animationSpec = tween(160),
        label = "ToolBtnIcon"
    )

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = bgColor,
        border = if (isSelected) BorderStroke(1.dp, LeNoteTheme.colors.accentPrimary.copy(alpha = 0.25f)) else null,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (isSelected) iconFill else iconRegular,
                contentDescription = contentDescription,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )

            // Indicador sutil de cor ativa na base do botão
            if (badgeColor != null) {
                val isLightBadge = (badgeColor.red * 0.299f + badgeColor.green * 0.587f + badgeColor.blue * 0.114f) > 0.78f
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 3.dp)
                        .size(5.dp)
                        .background(badgeColor, CircleShape)
                        .border(
                            width = 0.75.dp,
                            color = if (isLightBadge) LeNoteTheme.colors.inkSecondary else Color.White,
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

/**
 * Ponto seletor de cor circular.
 */
@Composable
internal fun ColorPickerDot(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isLightColor = (color.red * 0.299f + color.green * 0.587f + color.blue * 0.114f) > 0.78f
    val defaultBorderColor = if (isLightColor) {
        LeNoteTheme.colors.inkSecondary.copy(alpha = 0.55f)
    } else {
        LeNoteTheme.colors.hairlineBorder
    }
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) LeNoteTheme.colors.accentPrimary else defaultBorderColor,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = PhosphorIcons.Regular.Check,
                contentDescription = null,
                tint = if (isLightColor) Color(0xFF141413) else Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Amostra visual de espessura de traço (com traço suave desenhado em tempo real).
 */
@Composable
internal fun StrokeSizeChip(
    label: String,
    strokeWidthPx: Float,
    strokeColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isLightStroke = (strokeColor.red * 0.299f + strokeColor.green * 0.587f + strokeColor.blue * 0.114f) > 0.78f
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) LeNoteTheme.colors.accentPrimaryContainer else LeNoteTheme.colors.tonalSurface,
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.hairlineBorder
        ),
        modifier = Modifier.height(32.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Canvas(modifier = Modifier.size(width = 20.dp, height = 12.dp)) {
                val clampedWidth = strokeWidthPx.coerceIn(1.5f, 11f)
                if (isLightStroke) {
                    drawLine(
                        color = Color(0xFF141413).copy(alpha = 0.45f),
                        start = Offset(0f, size.height / 2f),
                        end = Offset(size.width, size.height / 2f),
                        strokeWidth = clampedWidth + 2.2f,
                        cap = StrokeCap.Round
                    )
                }
                drawLine(
                    color = strokeColor,
                    start = Offset(0f, size.height / 2f),
                    end = Offset(size.width, size.height / 2f),
                    strokeWidth = clampedWidth,
                    cap = StrokeCap.Round
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) LeNoteTheme.colors.inkPrimary else LeNoteTheme.colors.inkSecondary
            )
        }
    }
}

package com.example.lenote.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.ink.EraserSize
import com.example.lenote.ink.InkingState
import com.example.lenote.ui.theme.LeNoteTheme

@Composable
internal fun InkEraserOptionsPanel(
    visible: Boolean,
    state: InkingState,
    onEraserSizeSelect: (EraserSize) -> Unit
) {
    val s = LeNoteTheme.strings
    // Painel Contextual 3: Borracha
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder),
            color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.98f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EraserSize.entries.forEach { sizeOption ->
                    val isSelected = state.eraserSize == sizeOption
                    val previewRadius = when (sizeOption) {
                        EraserSize.SMALL -> 7.dp
                        EraserSize.MEDIUM -> 12.dp
                        EraserSize.LARGE -> 17.dp
                    }
                    Surface(
                        onClick = { onEraserSizeSelect(sizeOption) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) LeNoteTheme.colors.accentPrimaryContainer else LeNoteTheme.colors.tonalSurface,
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.hairlineBorder
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(previewRadius)
                                    .background(
                                        if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkSecondary,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = s.eraserSizeLabel(sizeOption),
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 11.sp),
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (isSelected) LeNoteTheme.colors.inkPrimary else LeNoteTheme.colors.inkSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

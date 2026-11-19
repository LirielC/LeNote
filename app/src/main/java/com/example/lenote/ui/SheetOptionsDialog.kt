package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lenote.ink.InkingState
import com.example.lenote.ink.PageBackground
import com.example.lenote.ink.SmoothingLevel
import com.example.lenote.ui.theme.LeNoteTheme

/**
 * Diálogo focado nas opções rápidas da folha.
 */
@Composable
internal fun SheetOptionsDialog(
    state: InkingState,
    onPageBackgroundSelect: (PageBackground) -> Unit,
    onSmoothingSelect: (SmoothingLevel) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LeNoteTheme.strings
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = LeNoteTheme.colors.elevatedSurface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.SlidersHorizontal,
                            contentDescription = null,
                            tint = LeNoteTheme.colors.accentPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = s.sheetOptionsTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = LeNoteTheme.colors.inkPrimary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = PhosphorIcons.Regular.X,
                            contentDescription = s.actionClose,
                            tint = LeNoteTheme.colors.inkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = s.strokeSmoothingLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = LeNoteTheme.colors.inkMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SmoothingLevel.entries.forEach { level ->
                            val isSelected = state.smoothingLevel == level
                            Surface(
                                onClick = { onSmoothingSelect(level) },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) LeNoteTheme.colors.accentPrimaryContainer else LeNoteTheme.colors.tonalSurface,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.hairlineBorder
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = level.label,
                                        style = MaterialTheme.typography.labelLarge,
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
    }
}

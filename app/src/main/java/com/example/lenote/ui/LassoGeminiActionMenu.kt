package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.lenote.export.GeminiPromptAction
import com.example.lenote.ui.theme.LeNoteTheme

private data class GeminiSelectionAction(
    val action: GeminiPromptAction,
    val icon: ImageVector,
    val title: String,
    val subtitle: String
)

@Composable
internal fun LassoGeminiActionMenu(onAction: (GeminiPromptAction) -> Unit) {
    val s = LeNoteTheme.strings
    var expanded by remember { mutableStateOf(false) }
    val actions = listOf(
        GeminiSelectionAction(
            GeminiPromptAction.EXPLAIN,
            PhosphorIcons.Regular.Sparkle,
            s.geminiActionExplainTitle,
            s.geminiActionExplainSubtitle
        ),
        GeminiSelectionAction(
            GeminiPromptAction.SOLVE,
            PhosphorIcons.Regular.CheckCircle,
            s.geminiActionSolveTitle,
            s.geminiActionSolveSubtitle
        ),
        GeminiSelectionAction(
            GeminiPromptAction.SUMMARIZE,
            PhosphorIcons.Regular.ListDashes,
            s.geminiActionSummarizeTitle,
            s.geminiActionSummarizeSubtitle
        ),
        GeminiSelectionAction(
            GeminiPromptAction.FREE,
            PhosphorIcons.Regular.ArrowSquareOut,
            s.geminiActionFreeTitle,
            s.geminiActionFreeSubtitle
        )
    )

    Box {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(9.dp),
            color = LeNoteTheme.colors.accentPrimary.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, LeNoteTheme.colors.accentPrimary.copy(alpha = 0.35f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Fill.Sparkle,
                    contentDescription = s.lassoGeminiDesc,
                    tint = LeNoteTheme.colors.accentPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = s.lassoGeminiLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = LeNoteTheme.colors.accentPrimary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = LeNoteTheme.colors.elevatedSurface
        ) {
            Text(
                text = s.geminiMenuOverline,
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkMuted,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
            actions.forEach { item ->
                if (item.action == GeminiPromptAction.FREE) {
                    HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)
                }
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = LeNoteTheme.colors.inkPrimary
                            )
                            Text(
                                text = item.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = LeNoteTheme.colors.inkSecondary
                            )
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = if (item.action == GeminiPromptAction.FREE) {
                                LeNoteTheme.colors.inkSecondary
                            } else {
                                LeNoteTheme.colors.accentPrimary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        expanded = false
                        onAction(item.action)
                    }
                )
            }
        }
    }
}

package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.lenote.export.GeminiPromptAction
import com.example.lenote.ui.theme.LeNoteTheme

/**
 * Mini-menu editorial para escolher a ação ao enviar a página inteira para o app do Gemini.
 */
@Composable
internal fun GeminiActionPickerDialog(
    title: String,
    onSelectAction: (GeminiPromptAction) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LeNoteTheme.strings
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = LeNoteTheme.colors.elevatedSurface,
            shadowElevation = 8.dp,
            border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = s.geminiMenuOverline,
                            style = MaterialTheme.typography.labelMedium,
                            color = LeNoteTheme.colors.accentPrimary
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
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

                Spacer(modifier = Modifier.height(2.dp))
                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                PageActionRow(
                    icon = PhosphorIcons.Regular.Sparkle,
                    iconTint = LeNoteTheme.colors.accentPrimary,
                    title = s.geminiActionExplainTitle,
                    subtitle = s.geminiActionExplainSubtitle,
                    onClick = { onSelectAction(GeminiPromptAction.EXPLAIN) }
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.CheckCircle,
                    iconTint = LeNoteTheme.colors.accentPrimary,
                    title = s.geminiActionSolveTitle,
                    subtitle = s.geminiActionSolveSubtitle,
                    onClick = { onSelectAction(GeminiPromptAction.SOLVE) }
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.ListDashes,
                    iconTint = LeNoteTheme.colors.accentPrimary,
                    title = s.geminiActionSummarizeTitle,
                    subtitle = s.geminiActionSummarizeSubtitle,
                    onClick = { onSelectAction(GeminiPromptAction.SUMMARIZE) }
                )
                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)
                PageActionRow(
                    icon = PhosphorIcons.Regular.ArrowSquareOut,
                    title = s.geminiActionFreeTitle,
                    subtitle = s.geminiActionFreeSubtitle,
                    onClick = { onSelectAction(GeminiPromptAction.FREE) }
                )
            }
        }
    }
}

@Composable
internal fun PageActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color? = null,
    isDestructive: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !enabled -> LeNoteTheme.colors.inkMuted.copy(alpha = 0.4f)
                    isDestructive -> MaterialTheme.colorScheme.error
                    iconTint != null -> iconTint
                    else -> LeNoteTheme.colors.inkSecondary
                },
                modifier = Modifier.size(19.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = when {
                        !enabled -> LeNoteTheme.colors.inkMuted.copy(alpha = 0.45f)
                        isDestructive -> MaterialTheme.colorScheme.error
                        else -> LeNoteTheme.colors.inkPrimary
                    }
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = if (!enabled) LeNoteTheme.colors.inkMuted.copy(alpha = 0.35f) else LeNoteTheme.colors.inkMuted
                    )
                }
            }
        }
    }
}

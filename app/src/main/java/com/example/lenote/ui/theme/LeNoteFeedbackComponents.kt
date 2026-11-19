package com.example.lenote.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.lenote.ui.PhosphorIcons

@Composable
fun EditorialActionChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDestructive: Boolean = false
) {
    val containerColor = when {
        isDestructive -> MaterialTheme.colorScheme.errorContainer
        isPrimary -> LeNoteTheme.colors.accentPrimary
        else -> LeNoteTheme.colors.tonalSurface
    }
    val contentColor = when {
        isDestructive -> MaterialTheme.colorScheme.onErrorContainer
        isPrimary -> Color.White
        else -> LeNoteTheme.colors.inkPrimary
    }

    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

enum class EditorialFeedbackType {
    LOADING,
    SUCCESS,
    ERROR
}

data class EditorialFeedbackBannerState(
    val type: EditorialFeedbackType,
    val title: String,
    val message: String? = null
)

@Composable
fun EditorialStatusBanner(
    state: EditorialFeedbackBannerState,
    onDismiss: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val containerColor = when (state.type) {
        EditorialFeedbackType.LOADING -> LeNoteTheme.colors.accentPrimaryContainer
        EditorialFeedbackType.SUCCESS -> LeNoteTheme.colors.successContainer
        EditorialFeedbackType.ERROR -> MaterialTheme.colorScheme.errorContainer
    }
    val accentColor = when (state.type) {
        EditorialFeedbackType.LOADING -> LeNoteTheme.colors.accentPrimary
        EditorialFeedbackType.SUCCESS -> LeNoteTheme.colors.success
        EditorialFeedbackType.ERROR -> MaterialTheme.colorScheme.error
    }
    val icon = when (state.type) {
        EditorialFeedbackType.LOADING -> null
        EditorialFeedbackType.SUCCESS -> PhosphorIcons.Fill.CheckCircle
        EditorialFeedbackType.ERROR -> PhosphorIcons.Fill.WarningCircle
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.type == EditorialFeedbackType.LOADING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = accentColor
                )
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = LeNoteTheme.colors.inkPrimary
                )
                state.message?.takeIf { it.isNotBlank() }?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = LeNoteTheme.colors.inkSecondary
                    )
                }
            }
            if (onDismiss != null && state.type != EditorialFeedbackType.LOADING) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = "Fechar aviso",
                        tint = LeNoteTheme.colors.inkSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun EditorialEmptyState(
    icon: ImageVector,
    overline: String,
    title: String,
    description: String,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LeNoteTheme.colors.tonalSurface,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = LeNoteTheme.colors.inkSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Text(
            text = overline,
            style = MaterialTheme.typography.labelMedium,
            color = LeNoteTheme.colors.inkMuted
        )
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = LeNoteTheme.colors.inkPrimary,
            textAlign = TextAlign.Center
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = LeNoteTheme.colors.inkSecondary,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(4.dp))
            EditorialActionChip(
                label = actionLabel,
                icon = actionIcon ?: PhosphorIcons.Regular.Plus,
                onClick = onActionClick,
                isPrimary = true
            )
        }
    }
}

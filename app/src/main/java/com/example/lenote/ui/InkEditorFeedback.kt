package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.ui.theme.LeNotePalette
import com.example.lenote.ui.theme.LeNoteTheme

/**
 * Indicador e Controle Editorial de Zoom no canto inferior direito:
 * - Em repouso: cápsula de alto contraste com tipografia monoespaçada (ex.: "100%");
 * - Ao tocar: expande controles [ − ] 100% [ + ] e [ Ajustar à página ].
 */
@Composable
internal fun ZoomIndicatorControl(
    zoomLevel: Float,
    isNavigationLocked: Boolean,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onFitToPage: () -> Unit,
    onToggleNavigationLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LeNoteTheme.strings
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder),
        shadowElevation = 3.dp
    ) {
        if (!isExpanded) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { isExpanded = true }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    imageVector = if (isNavigationLocked) PhosphorIcons.Fill.Hand else PhosphorIcons.Regular.MagnifyingGlass,
                    contentDescription = if (isNavigationLocked) s.zoomUnlockPageDesc else null,
                    tint = if (isNavigationLocked) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "${kotlin.math.round(zoomLevel * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = LeNoteTheme.colors.inkPrimary
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = onZoomOut,
                    enabled = !isNavigationLocked,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Minus,
                        contentDescription = s.zoomOutDesc,
                        tint = LeNoteTheme.colors.inkPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .clickable(enabled = !isNavigationLocked) {
                            onResetZoom()
                            isExpanded = false
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${kotlin.math.round(zoomLevel * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        color = LeNoteTheme.colors.accentPrimary
                    )
                }

                IconButton(
                    onClick = onZoomIn,
                    enabled = !isNavigationLocked,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.Plus,
                        contentDescription = s.zoomInDesc,
                        tint = LeNoteTheme.colors.inkPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(16.dp)
                        .background(LeNoteTheme.colors.hairlineBorder)
                )

                IconButton(
                    onClick = {
                        onFitToPage()
                        isExpanded = false
                    },
                    enabled = !isNavigationLocked,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.CornersOut,
                        contentDescription = s.zoomFitToPageDesc,
                        tint = LeNoteTheme.colors.inkPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = onToggleNavigationLock,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = if (isNavigationLocked) PhosphorIcons.Fill.Hand else PhosphorIcons.Regular.Hand,
                        contentDescription = if (isNavigationLocked) s.zoomUnlockPageDesc else s.zoomLockPageDesc,
                        tint = if (isNavigationLocked) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = { isExpanded = false },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = s.zoomCloseDesc,
                        tint = LeNoteTheme.colors.inkMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Card flutuante de confirmação pós-exportação (PDF ou PNG).
 */
@Composable
internal fun ExportConfirmationBanner(
    state: ExportConfirmationState,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LeNoteTheme.strings
    Surface(
        modifier = modifier
            .widthIn(min = 320.dp, max = 540.dp)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        color = LeNoteTheme.colors.elevatedSurface,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (state.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.4f) else LeNoteTheme.colors.hairlineBorder
        )
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val iconVector = when {
                state.isError -> PhosphorIcons.Regular.WarningCircle
                state.exportResult.mimeType == "application/pdf" -> PhosphorIcons.Regular.FilePdf
                else -> PhosphorIcons.Regular.Image
            }
            val iconTint = when {
                state.isError -> MaterialTheme.colorScheme.error
                state.exportResult.mimeType == "application/pdf" -> LeNotePalette.TerracottaAccent
                else -> LeNoteTheme.colors.accentPrimary
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (state.isError) MaterialTheme.colorScheme.errorContainer else LeNoteTheme.colors.tonalSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = LeNoteTheme.colors.inkPrimary
                )
                if (state.subtitle.isNotBlank()) {
                    Text(
                        text = state.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = LeNoteTheme.colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (state.canOpen) {
                val actionLabel = if (state.exportResult.mimeType == "application/pdf") s.exportOpenPdfButton else s.exportViewImageButton
                Button(
                    onClick = onOpen,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = LeNoteTheme.colors.accentPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.ArrowSquareOut,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.sp),
                        color = Color.White
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    imageVector = PhosphorIcons.Regular.X,
                    contentDescription = s.actionClose,
                    tint = LeNoteTheme.colors.inkMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

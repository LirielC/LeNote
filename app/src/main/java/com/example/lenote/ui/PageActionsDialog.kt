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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.lenote.data.PageEntity
import com.example.lenote.ui.theme.LeNoteTheme

/**
 * Diálogo Editorial de Ações da Página (Menu "⋮" do Header):
 * - GERENCIAMENTO: Renomear, Duplicar, Reordenar, Favoritar;
 * - CONFIGURAÇÕES: Modelo e Cor da Folha;
 * - EXPORTAÇÃO: Exportar PDF, Exportar PNG;
 * - AVANÇADO: Diagnósticos & Depuração;
 * - AÇÕES DESTRUTIVAS: Limpar traços, Mover para lixeira.
 */
@Composable
internal fun PageActionsDialog(
    currentPage: PageEntity?,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onRenamePage: () -> Unit,
    onDuplicatePage: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSheetOptions: () -> Unit,
    onExportPdf: () -> Unit,
    onExportPng: () -> Unit,
    onSendPageToGemini: () -> Unit,
    onDeletePage: () -> Unit,
    onPromptClear: () -> Unit,
    isDiagnosticsVisible: Boolean,
    onToggleDiagnostics: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LeNoteTheme.strings
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 420.dp)
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
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = s.pageOptionsOverline,
                            style = MaterialTheme.typography.labelMedium,
                            color = LeNoteTheme.colors.inkMuted
                        )
                        Text(
                            text = currentPage?.title ?: s.currentSheetFallback,
                            style = MaterialTheme.typography.headlineSmall,
                            color = LeNoteTheme.colors.inkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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

                // 1. GERENCIAMENTO DA PÁGINA
                Text(
                    text = s.sectionOrganization,
                    style = MaterialTheme.typography.labelMedium,
                    color = LeNoteTheme.colors.inkMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.PencilSimple,
                    title = s.actionRenameSheet,
                    onClick = onRenamePage
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Copy,
                    title = s.actionDuplicateSheet,
                    subtitle = s.actionDuplicateSheetSubtitle,
                    onClick = onDuplicatePage
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.CaretUp,
                    title = s.actionMoveUp,
                    enabled = canMoveUp,
                    onClick = onMoveUp
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.CaretDown,
                    title = s.actionMoveDown,
                    enabled = canMoveDown,
                    onClick = onMoveDown
                )
                PageActionRow(
                    icon = if (currentPage?.isFavorite == true) PhosphorIcons.Fill.Star else PhosphorIcons.Regular.Star,
                    iconTint = if (currentPage?.isFavorite == true) LeNoteTheme.colors.goldStar else null,
                    title = if (currentPage?.isFavorite == true) s.actionRemoveFavorite else s.actionAddFavorite,
                    onClick = onToggleFavorite
                )

                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                // 2. CONFIGURAÇÕES DA FOLHA
                Text(
                    text = s.sectionPaperAndRuling,
                    style = MaterialTheme.typography.labelMedium,
                    color = LeNoteTheme.colors.inkMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.SlidersHorizontal,
                    title = s.actionSheetTemplateTitle,
                    subtitle = s.actionSheetTemplateSubtitle,
                    onClick = onOpenSheetOptions
                )

                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                // 3. EXPORTAÇÃO
                Text(
                    text = s.sectionExportAndShare,
                    style = MaterialTheme.typography.labelMedium,
                    color = LeNoteTheme.colors.inkMuted,
                    modifier = Modifier.padding(top = 4.dp)
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.FilePdf,
                    title = s.actionExportPdfTitle,
                    subtitle = s.actionExportPdfSubtitle,
                    onClick = onExportPdf
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Image,
                    title = s.actionExportPngTitle,
                    subtitle = s.actionExportPngSubtitle,
                    onClick = onExportPng
                )
                PageActionRow(
                    icon = PhosphorIcons.Fill.Sparkle,
                    iconTint = LeNoteTheme.colors.accentPrimary,
                    title = s.actionSendPageToGeminiTitle,
                    subtitle = s.actionSendPageToGeminiSubtitle,
                    onClick = onSendPageToGemini
                )

                if (com.example.lenote.BuildConfig.DEBUG) {
                    HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)
                    Text(
                        text = s.sectionEngineering,
                        style = MaterialTheme.typography.labelMedium,
                        color = LeNoteTheme.colors.inkMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    PageActionRow(
                        icon = PhosphorIcons.Regular.Bug,
                        title = s.actionTelemetryTitle,
                        subtitle = if (isDiagnosticsVisible) s.telemetryVisibleSubtitle else s.telemetryHiddenSubtitle,
                        onClick = onToggleDiagnostics
                    )
                }

                HorizontalDivider(color = LeNoteTheme.colors.subtleDivider)

                // 5. AÇÕES DESTRUTIVAS
                Text(
                    text = s.sectionDangerZone,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Eraser,
                    title = s.actionClearAllStrokesTitle,
                    subtitle = s.actionClearAllStrokesSubtitle,
                    isDestructive = true,
                    onClick = onPromptClear
                )
                PageActionRow(
                    icon = PhosphorIcons.Regular.Trash,
                    title = s.actionMoveSheetToTrashTitle,
                    subtitle = s.actionMoveSheetToTrashSubtitle,
                    isDestructive = true,
                    onClick = onDeletePage
                )
            }
        }
    }
}

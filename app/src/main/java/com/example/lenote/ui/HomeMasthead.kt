package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.ui.theme.EditorialActionChip
import com.example.lenote.ui.theme.LeNoteTheme

/**
 * Cabeçalho Editorial (Masthead) da HomeScreen:
 * Forte hierarquia tipográfica, sumário do acervo e botões utilitários elegantes no topo.
 */
@Composable
internal fun EditorialMasthead(
    notebookCount: Int,
    pageCount: Int,
    isDarkMode: Boolean,
    onImportPdf: () -> Unit,
    onImportLenote: () -> Unit,
    onOpenLanguagePicker: () -> Unit,
    onToggleTheme: () -> Unit
) {
    val s = LeNoteTheme.strings
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Linha 1: Sobretítulo Editorial + Ações Utilitárias Compactas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(LeNoteTheme.colors.accentWarm, CircleShape)
                )
                Text(
                    text = s.homeOverline,
                    style = MaterialTheme.typography.labelSmall,
                    color = LeNoteTheme.colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorialActionChip(
                    label = s.actionPdf,
                    icon = PhosphorIcons.Regular.FilePdf,
                    onClick = onImportPdf
                )
                EditorialActionChip(
                    label = s.actionBackup,
                    icon = PhosphorIcons.Regular.Export,
                    onClick = onImportLenote
                )
                // Seletor de Idioma (PT / EN / ES)
                EditorialActionChip(
                    label = s.language.badgeCode,
                    icon = PhosphorIcons.Regular.Globe,
                    onClick = onOpenLanguagePicker
                )
                Surface(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleTheme() },
                    shape = RoundedCornerShape(8.dp),
                    color = LeNoteTheme.colors.elevatedSurface,
                    border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isDarkMode) PhosphorIcons.Regular.Sun else PhosphorIcons.Regular.Moon,
                            contentDescription = if (isDarkMode) s.lightModeDesc else s.darkModeDesc,
                            tint = LeNoteTheme.colors.inkSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Linha 2: Título Editorial Full-Width + Metadados em Monospace
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = s.homeTitle,
                style = MaterialTheme.typography.headlineMedium,
                color = LeNoteTheme.colors.inkPrimary,
                maxLines = 1
            )

            Text(
                text = s.homeStatsSummary(notebookCount, pageCount),
                style = MaterialTheme.typography.labelSmall,
                color = LeNoteTheme.colors.inkMuted,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

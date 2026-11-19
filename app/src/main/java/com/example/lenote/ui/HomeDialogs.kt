package com.example.lenote.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.data.AppLanguage
import com.example.lenote.ui.i18n.resolveAppLanguage
import com.example.lenote.ui.theme.LeNotePalette
import com.example.lenote.ui.theme.LeNoteTheme

/**
 * Diálogo Editorial para criação de novo caderno com título e seletor de cor da capa.
 */
@Composable
internal fun CreateNotebookDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String, colorArgb: Int) -> Unit
) {
    val s = LeNoteTheme.strings
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(NotebookCoverColors.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LeNoteTheme.colors.elevatedSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = s.createNotebookOverline,
                    style = MaterialTheme.typography.labelMedium,
                    color = LeNoteTheme.colors.inkMuted
                )
                Text(
                    text = s.createNotebookTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = LeNoteTheme.colors.inkPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(s.notebookTitleLabel) },
                    placeholder = { Text(s.notebookTitlePlaceholder) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LeNoteTheme.colors.accentPrimary,
                        unfocusedBorderColor = LeNoteTheme.colors.hairlineBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = s.notebookBindingColorLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = LeNoteTheme.colors.inkSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NotebookCoverColors.forEach { color ->
                            val isSelected = selectedColor == color
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .clickable { selectedColor = color }
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.5.dp, LeNoteTheme.colors.inkPrimary, CircleShape)
                                        } else {
                                            Modifier.border(1.dp, Color.Black.copy(alpha = 0.2f), CircleShape)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = PhosphorIcons.Regular.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.trim().ifEmpty { s.defaultNewNotebookName }
                    val argb = (selectedColor.value shr 32).toInt()
                    onCreate(finalTitle, argb)
                },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LeNoteTheme.colors.accentPrimary,
                    contentColor = Color.White
                )
            ) {
                Text(s.actionCreateNotebook)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.actionCancel, color = LeNoteTheme.colors.inkSecondary)
            }
        }
    )
}

/**
 * Diálogo Editorial para seleção de Idioma do Aplicativo (Automático Sistema Android, Português, English, Español).
 */
@Composable
internal fun LanguagePickerDialog(
    currentPreference: AppLanguage,
    onSelectLanguage: (AppLanguage) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LeNoteTheme.strings
    val options = listOf(
        Triple(AppLanguage.SYSTEM, s.languageSystemOption, s.languageSystemSubtitle(s.language)),
        Triple(AppLanguage.PT, s.languagePortuguese, "PT-BR / PT"),
        Triple(AppLanguage.EN, s.languageEnglish, "EN-US / EN"),
        Triple(AppLanguage.ES, s.languageSpanish, "ES-ES / ES")
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LeNoteTheme.colors.elevatedSurface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = s.languagePickerOverline,
                    style = MaterialTheme.typography.labelMedium,
                    color = LeNoteTheme.colors.inkMuted
                )
                Text(
                    text = s.languagePickerTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = LeNoteTheme.colors.inkPrimary
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (lang, label, subtitle) ->
                    val isSelected = currentPreference == lang
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelectLanguage(lang) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            LeNoteTheme.colors.accentPrimaryContainer.copy(alpha = 0.55f)
                        } else {
                            LeNoteTheme.colors.tonalSurface.copy(alpha = 0.45f)
                        },
                        border = BorderStroke(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.hairlineBorder
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (isSelected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkPrimary
                                )
                                Text(
                                    text = subtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = LeNoteTheme.colors.inkMuted
                                )
                            }

                            if (isSelected) {
                                Surface(
                                    shape = CircleShape,
                                    color = LeNoteTheme.colors.accentPrimary,
                                    modifier = Modifier.size(22.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = PhosphorIcons.Regular.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(s.actionClose, color = LeNoteTheme.colors.accentPrimary)
            }
        }
    )
}

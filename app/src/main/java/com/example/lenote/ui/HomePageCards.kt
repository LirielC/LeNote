package com.example.lenote.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lenote.data.NotebookEntity
import com.example.lenote.data.PageEntity
import com.example.lenote.ink.PageBackground
import com.example.lenote.ui.theme.EditorialEmptyState
import com.example.lenote.ui.theme.EditorialSectionHeader
import com.example.lenote.ui.theme.LeNoteTheme
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card de Página Recente na seção "Continuar Escrevendo":
 * Apresenta a miniatura real da folha com borda hairline de papel, indicador do caderno
 * de origem, título e horário.
 */
@Composable
internal fun RecentPageCard(
    page: PageEntity,
    notebookTitle: String?,
    notebookColorHex: Int?,
    thumbnailFile: File?,
    thumbnailVersion: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LeNoteTheme.strings
    val thumbBitmap = remember(page.id, page.updatedAt, thumbnailVersion) {
        if (thumbnailFile != null && thumbnailFile.exists()) {
            runCatching { BitmapFactory.decodeFile(thumbnailFile.absolutePath)?.asImageBitmap() }.getOrNull()
        } else null
    }

    val timeFormat = remember(s.language) { SimpleDateFormat("dd MMM • HH:mm", s.language.locale) }
    val formattedTime = remember(page.updatedAt, s.language) { timeFormat.format(Date(page.updatedAt)) }

    Surface(
        modifier = modifier
            .width(156.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = LeNoteTheme.colors.elevatedSurface,
        border = BorderStroke(1.dp, LeNoteTheme.colors.hairlineBorder)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            val paperColor = Color(page.paperColor.toInt())
            val isDarkPaper = com.example.lenote.ink.PaperColors.isDarkPaper(page.paperColor)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(paperColor)
                    .border(
                        width = 1.dp,
                        color = if (isDarkPaper) Color(0xFF334155) else LeNoteTheme.colors.hairlineBorder,
                        shape = RoundedCornerShape(6.dp)
                    )
            ) {
                if (thumbBitmap != null) {
                    Image(
                        bitmap = thumbBitmap,
                        contentDescription = page.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    TemplatePreviewCanvas(background = page.pageBackground, isDark = isDarkPaper)
                }

                if (page.isFavorite) {
                    Surface(
                        shape = CircleShape,
                        color = LeNoteTheme.colors.elevatedSurface.copy(alpha = 0.9f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(5.dp)
                            .size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = PhosphorIcons.Fill.Star,
                                contentDescription = s.favoritePageDesc,
                                tint = LeNoteTheme.colors.goldStar,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (!notebookTitle.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (notebookColorHex != null) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(Color(notebookColorHex), CircleShape)
                            )
                        }
                        Text(
                            text = notebookTitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = LeNoteTheme.colors.inkMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = page.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = LeNoteTheme.colors.inkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                    color = LeNoteTheme.colors.inkMuted
                )
            }
        }
    }
}

/**
 * Conteúdo da Aba Favoritas (Folhas marcadas com estrela pelo usuário).
 */
@Composable
internal fun FavoritesContent(
    favoritePages: List<PageEntity>,
    getNotebookForPage: (PageEntity) -> NotebookEntity?,
    thumbnailVersion: Long,
    getThumbnailFile: (String) -> File?,
    onOpenPage: (PageEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val s = LeNoteTheme.strings
    if (favoritePages.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            EditorialEmptyState(
                icon = PhosphorIcons.Regular.Star,
                overline = s.emptyFavoritesOverline,
                title = s.emptyFavoritesTitle,
                description = s.emptyFavoritesDescription
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 152.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 88.dp),
            modifier = modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EditorialSectionHeader(
                    title = s.sectionFavoritePages,
                    count = favoritePages.size,
                    subtitle = s.sectionFavoritePagesSubtitle
                )
            }
            items(favoritePages, key = { "fav_${it.id}" }) { page ->
                val parentNb = getNotebookForPage(page)
                RecentPageCard(
                    page = page,
                    notebookTitle = parentNb?.title,
                    notebookColorHex = parentNb?.colorHex,
                    thumbnailFile = getThumbnailFile(page.id),
                    thumbnailVersion = thumbnailVersion,
                    onClick = { onOpenPage(page) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Desenho de fundo em miniatura para folhas que ainda não possuem traços salvos.
 */
@Composable
internal fun MiniPageBackgroundCanvas(
    pageBackground: PageBackground,
    paperColor: Long = 0xFFFFFFFF,
    modifier: Modifier = Modifier
) {
    TemplatePreviewCanvas(
        background = pageBackground,
        isDark = com.example.lenote.ink.PaperColors.isDarkPaper(paperColor),
        modifier = modifier
    )
}

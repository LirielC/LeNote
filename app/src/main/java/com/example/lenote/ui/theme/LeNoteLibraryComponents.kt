package com.example.lenote.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.lenote.ui.PhosphorIcons

@Composable
fun EditorialSectionHeader(
    title: String,
    count: Int? = null,
    subtitle: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = LeNoteTheme.colors.inkPrimary
            )
            if (count != null) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = LeNoteTheme.colors.inkMuted
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = LeNoteTheme.colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailingContent?.invoke()
    }
}

@Composable
fun EditorialSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val isActive = query.isNotEmpty()
    val containerColor by animateColorAsState(
        targetValue = if (isActive) LeNoteTheme.colors.elevatedSurface else LeNoteTheme.colors.tonalSurface,
        animationSpec = tween(160),
        label = "SearchContainer"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = PhosphorIcons.Regular.MagnifyingGlass,
                contentDescription = null,
                tint = LeNoteTheme.colors.inkSecondary,
                modifier = Modifier.size(22.dp)
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (query.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = LeNoteTheme.colors.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = LeNoteTheme.colors.inkPrimary
                    ),
                    cursorBrush = SolidColor(LeNoteTheme.colors.accentPrimary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = PhosphorIcons.Regular.X,
                        contentDescription = "Limpar busca",
                        tint = LeNoteTheme.colors.inkSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

data class EditorialFilterTab(
    val label: String,
    val count: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

@Composable
fun EditorialSegmentedBar(
    tabs: List<EditorialFilterTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = selectedIndex == index
            val containerColor by animateColorAsState(
                targetValue = if (selected) LeNoteTheme.colors.accentPrimaryContainer else Color.Transparent,
                animationSpec = tween(160),
                label = "FilterContainer"
            )
            val contentColor by animateColorAsState(
                targetValue = if (selected) LeNoteTheme.colors.accentPrimary else LeNoteTheme.colors.inkSecondary,
                animationSpec = tween(160),
                label = "FilterContent"
            )
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { onSelect(index) },
                shape = RoundedCornerShape(12.dp),
                color = containerColor
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        color = contentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = tab.count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.75f)
                    )
                }
            }
        }
    }
}

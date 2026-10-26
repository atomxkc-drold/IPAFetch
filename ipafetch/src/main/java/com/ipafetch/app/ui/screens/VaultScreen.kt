package com.ipafetch.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipafetch.app.data.SwipeAction
import com.ipafetch.app.domain.EntryType
import com.ipafetch.app.domain.VaultEntry
import com.ipafetch.app.ui.brand.BrandCatalog
import com.ipafetch.app.ui.components.AppBackground
import com.ipafetch.app.ui.components.BrandTile
import com.ipafetch.app.ui.components.ChipRow
import com.ipafetch.app.ui.components.EmptyState
import com.ipafetch.app.ui.components.SearchPill
import com.ipafetch.app.ui.components.ScreenHeader
import com.ipafetch.app.ui.components.TagPill
import com.ipafetch.app.ui.theme.IpafetchTheme
import com.ipafetch.app.ui.theme.rowShape
import kotlin.math.roundToInt

private const val FILTER_ALL = "All"

@Composable
fun VaultScreen(
    entries: List<VaultEntry>,
    onOpenEntry: (Long) -> Unit,
    onAdd: () -> Unit,
    onToggleFavorite: (Long) -> Unit,
    onCopy: (VaultEntry) -> Unit,
    onDelete: (VaultEntry) -> Unit,
    initialQuery: String = "",
    bottomBar: @Composable () -> Unit
) {
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    var filter by remember { mutableStateOf(FILTER_ALL) }

    val filtered = entries
        .filter { filter == FILTER_ALL || it.type == EntryType.from(filter).key }
        .filter {
            it.matches(query) ||
                (query.isNotBlank() && BrandCatalog.by(it.brandId)?.name?.contains(query, ignoreCase = true) == true)
        }
        .sortedWith(
            compareByDescending<VaultEntry> { it.favorite }.thenByDescending { it.updatedAt }
        )

    val total = entries.size
    val subtitle = when {
        total == 0 -> "Nothing saved yet"
        total == 1 -> "1 item · encrypted locally"
        else -> "$total items · ${entries.map { it.category }.distinct().size} categories"
    }

    AppBackground(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(bottom = 124.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                ScreenHeader(title = "Vault", subtitle = subtitle)
            }
            item {
                Column(Modifier.padding(horizontal = 20.dp)) {
                    SearchPill(value = query, onValueChange = { query = it })
                    Spacer(Modifier.height(12.dp))
                    ChipRow(
                        options = listOf(FILTER_ALL) + EntryType.editorOrder.map { it.label },
                        selected = filter,
                        label = { it },
                        onSelect = { filter = it }
                    )
                }
            }
            if (filtered.isEmpty()) {
                item {
                    EmptyState(
                        icon = Icons.Rounded.Key,
                        title = if (query.isBlank()) "Your vault is empty" else "No matches",
                        text = if (query.isBlank()) {
                            "Add a token, API key, JSON file or the link where you generate it."
                        } else {
                            "Try a different word, or clear the search."
                        },
                        action = if (query.isBlank()) {
                            { com.ipafetch.app.ui.components.AddPillButton("Add your first item", onAdd) }
                        } else null
                    )
                }
            } else {
                items(filtered, key = { it.id }) { entry ->
                    EntryRow(
                        entry = entry,
                        onOpen = { onOpenEntry(entry.id) },
                        onCopy = { onCopy(entry) },
                        onDelete = { onDelete(entry) },
                        onToggleFavorite = { onToggleFavorite(entry.id) }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 18.dp)
        ) { bottomBar() }
    }
}

@Composable
private fun EntryRow(
    entry: VaultEntry,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val colors = IpafetchTheme.colors
    val settings = IpafetchTheme.settings
    val density = LocalDensity.current
    val threshold = with(density) { 92.dp.toPx() }
    val rightAction = SwipeAction.from(settings.leftSwipe)
    val leftAction = SwipeAction.from(settings.rightSwipe)

    var drag by remember(entry.id) { mutableStateOf(0f) }
    val offset by animateFloatAsState(targetValue = drag, animationSpec = tween(200), label = "row-swipe")

    val brand = BrandCatalog.by(entry.brandId)
    val type = EntryType.from(entry.type)
    val shape = rowShape(settings.cornerScale)
    val glass = settings.progressiveBlur && !settings.reduceEffects

    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                offset > 6f && leftAction != SwipeAction.NONE -> SwipeHint(leftAction, alignEnd = false)
                offset < -6f && rightAction != SwipeAction.NONE -> {
                    Spacer(Modifier.weight(1f))
                    SwipeHint(rightAction, alignEnd = true)
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offset.roundToInt(), 0) }
                .clip(shape)
                .background(if (glass) colors.glassRow else colors.row)
                .then(if (glass) Modifier.border(1.dp, colors.glassSheen, shape) else Modifier)
                .pointerInput(entry.id, leftAction, rightAction) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                drag > threshold -> performSwipe(leftAction, onCopy, onDelete, onOpen)
                                drag < -threshold -> performSwipe(rightAction, onCopy, onDelete, onOpen)
                            }
                            drag = 0f
                        },
                        onDragCancel = { drag = 0f },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            drag = (drag + amount).coerceIn(-280f, 280f)
                        }
                    )
                }
                .clickable(onClick = onOpen)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .heightIn(min = 52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandTile(brandId = entry.brandId, label = entry.title, size = 42.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.title.ifBlank { "Untitled" },
                        color = colors.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (entry.favorite) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        brand?.name ?: entry.category.takeIf { it.isNotBlank() },
                        type.label,
                        entry.account.takeIf { it.isNotBlank() }
                    ).joinToString(" · "),
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (entry.isExpired()) {
                TagPill("Expired", color = colors.accent.copy(alpha = 0.18f))
                Spacer(Modifier.width(6.dp))
            } else if (entry.expiresSoon()) {
                TagPill("Expiring", color = colors.accent.copy(alpha = 0.14f))
                Spacer(Modifier.width(6.dp))
            }
            CopyButton(onClick = onCopy)
        }
    }
}

@Composable
private fun CopyButton(onClick: () -> Unit) {
    val colors = IpafetchTheme.colors
    val highlight = IpafetchTheme.settings.highlightTargets
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(if (highlight) colors.rowStrong else colors.row.copy(alpha = 0f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.ContentCopy,
            contentDescription = "Copy value",
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun SwipeHint(action: SwipeAction, alignEnd: Boolean) {
    val colors = IpafetchTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
    ) {
        if (!alignEnd) {
            Icon(
                imageVector = if (action == SwipeAction.DELETE) Icons.Rounded.Delete else Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(action.label, color = colors.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        if (alignEnd) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = if (action == SwipeAction.DELETE) Icons.Rounded.Delete else Icons.Rounded.ContentCopy,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun performSwipe(
    action: SwipeAction,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    when (action) {
        SwipeAction.COPY, SwipeAction.REVEAL -> onCopy()
        SwipeAction.DELETE -> onDelete()
        SwipeAction.OPEN, SwipeAction.EDIT -> onOpen()
        SwipeAction.NONE -> Unit
    }
}

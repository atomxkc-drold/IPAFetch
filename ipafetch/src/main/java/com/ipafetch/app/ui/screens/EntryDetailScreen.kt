package com.ipafetch.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipafetch.app.domain.EntryType
import com.ipafetch.app.domain.VaultEntry
import com.ipafetch.app.ui.brand.BrandCatalog
import com.ipafetch.app.ui.components.AppBackground
import com.ipafetch.app.ui.components.BrandTile
import com.ipafetch.app.ui.components.PrimaryButton
import com.ipafetch.app.ui.components.RoundIconButton
import com.ipafetch.app.ui.components.SettingsRow
import com.ipafetch.app.ui.components.TagPill
import com.ipafetch.app.ui.theme.IpafetchTheme
import com.ipafetch.app.ui.theme.rowShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun EntryDetailScreen(
    entry: VaultEntry,
    secretPlain: () -> String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val colors = IpafetchTheme.colors
    val type = EntryType.from(entry.type)
    val brand = BrandCatalog.by(entry.brandId)
    var revealed by remember(entry.id) { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val plaintext = remember(entry.id, entry.secret) { secretPlain() }

    AppBackground(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 40.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Rounded.ArrowBack, "Back", onBack)
                Spacer(Modifier.weight(1f))
                RoundIconButton(
                    icon = if (entry.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = "Favourite",
                    onClick = onToggleFavorite,
                    tint = colors.accent
                )
                RoundIconButton(Icons.Rounded.Edit, "Edit entry", onEdit, tint = colors.accent)
                RoundIconButton(Icons.Rounded.Delete, "Delete entry", { confirmDelete = true })
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandTile(entry.brandId, entry.title, size = 60.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.title.ifBlank { "Untitled" },
                        color = colors.textPrimary,
                        fontSize = 24.sp,
                        lineHeight = 29.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        listOfNotNull(brand?.name, type.label, entry.category.takeIf { it.isNotBlank() })
                            .joinToString(" · "),
                        color = colors.textSecondary,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            ValueCard(
                type = type,
                value = plaintext,
                revealed = revealed,
                onReveal = { revealed = !revealed },
                onCopy = onCopy
            )

            Spacer(Modifier.height(10.dp))
            if (entry.generateUrl.isNotBlank()) {
                SettingsRow(
                    title = "Generate at ${brand?.name ?: "provider"}",
                    supporting = entry.generateUrl,
                    leading = { Icon(Icons.Rounded.OpenInNew, null, tint = colors.accent, modifier = Modifier.size(20.dp)) },
                    onClick = { onOpenUrl(entry.generateUrl) }
                )
                Spacer(Modifier.height(8.dp))
            }
            if (entry.docsUrl.isNotBlank()) {
                SettingsRow(
                    title = "Documentation",
                    supporting = entry.docsUrl,
                    leading = { Icon(Icons.Rounded.Link, null, tint = colors.accent, modifier = Modifier.size(20.dp)) },
                    onClick = { onOpenUrl(entry.docsUrl) }
                )
                Spacer(Modifier.height(8.dp))
            }
            if (type == EntryType.LINK && plaintext.isNotBlank()) {
                SettingsRow(
                    title = "Open stored link",
                    supporting = plaintext,
                    leading = { Icon(Icons.Rounded.Link, null, tint = colors.accent, modifier = Modifier.size(20.dp)) },
                    onClick = { onOpenUrl(plaintext) }
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(14.dp))
            SectionTitle("Usage")
            SettingsRow(
                title = entry.usage.ifBlank { "No usage notes yet" },
                supporting = if (entry.usage.isBlank()) "Edit this entry to describe how the key is used." else null
            )

            Spacer(Modifier.height(16.dp))
            SectionTitle("Details")
            MetaRow("Account", entry.account.ifBlank { "—" })
            Spacer(Modifier.height(8.dp))
            MetaRow("Environment", entry.environment.ifBlank { "Not set" })
            Spacer(Modifier.height(8.dp))
            MetaRow("Expires", expiryLabel(entry))
            Spacer(Modifier.height(8.dp))
            MetaRow("Copied", if (entry.copyCount == 0) "Never" else "${entry.copyCount}× · last ${relative(entry.lastCopiedAt)}")
            Spacer(Modifier.height(8.dp))
            MetaRow("Updated", relative(entry.updatedAt))

            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    entry.tags.forEach { TagPill(it) }
                }
            }

            Spacer(Modifier.height(20.dp))
            PrimaryButton(label = "Edit entry", onClick = onEdit)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this entry?") },
            text = { Text("${entry.title.ifBlank { "This entry" }} will be removed from the vault. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            },
            containerColor = colors.rowStrong
        )
    }
}

@Composable
private fun ValueCard(
    type: EntryType,
    value: String,
    revealed: Boolean,
    onReveal: () -> Unit,
    onCopy: () -> Unit
) {
    val colors = IpafetchTheme.colors
    val shape = rowShape(IpafetchTheme.settings.cornerScale)
    val isJson = type == EntryType.JSON
    val jsonValid = remember(value, isJson) { if (!isJson) null else validateJson(value) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.row)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = when (type) {
                    EntryType.JSON -> "JSON value"
                    EntryType.LINK -> "Stored link"
                    EntryType.NOTE -> "Note"
                    else -> "Secret value"
                },
                color = colors.textSecondary,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            if (jsonValid != null) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (jsonValid) "Valid JSON" else "Invalid JSON",
                    color = colors.textSecondary,
                    fontSize = 11.sp
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        val masked = if (value.length <= 8) "••••••••" else value.take(4) + "••••••••••" + value.takeLast(4)
        SelectionContainer(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Text(
                text = when {
                    revealed || type == EntryType.LINK || type == EntryType.NOTE || type == EntryType.JSON -> value.ifBlank { "—" }
                    else -> masked
                },
                color = colors.textPrimary,
                fontSize = if (isJson) 12.sp else 15.sp,
                fontFamily = if (isJson) FontFamily.Monospace else FontFamily.Monospace,
                lineHeight = if (isJson) 18.sp else 22.sp
            )
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (type != EntryType.JSON && type != EntryType.NOTE) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(colors.rowInset)
                        .clickable(onClick = onReveal)
                        .padding(vertical = 11.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(if (revealed) "Hide" else "Reveal", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable(onClick = onCopy)
                    .padding(vertical = 11.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    tint = colors.onAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("Copy", color = colors.onAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = IpafetchTheme.colors.textSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
    )
}

@Composable
private fun MetaRow(label: String, value: String) {
    val colors = IpafetchTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape(IpafetchTheme.settings.cornerScale))
            .background(colors.row)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = colors.textPrimary, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun validateJson(value: String): Boolean {
    if (value.isBlank()) return false
    return runCatching {
        if (value.trimStart().startsWith("[")) JSONArray(value) else JSONObject(value)
        true
    }.getOrDefault(false)
}

private fun expiryLabel(entry: VaultEntry): String {
    val expires = entry.expiresAt ?: return "Never"
    val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(expires))
    return when {
        entry.isExpired() -> "Expired $date"
        entry.expiresSoon() -> "Expires $date"
        else -> date
    }
}

private fun relative(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "—"
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60} h ago"
        else -> "${minutes / (60 * 24)} d ago"
    }
}

package com.ipafetch.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipafetch.app.crypto.CryptoManager
import com.ipafetch.app.domain.EntryCategories
import com.ipafetch.app.domain.EntryType
import com.ipafetch.app.domain.VaultEntry
import com.ipafetch.app.ui.brand.Brand
import com.ipafetch.app.ui.brand.BrandCatalog
import com.ipafetch.app.ui.components.AppBackground
import com.ipafetch.app.ui.components.BrandTile
import com.ipafetch.app.ui.components.ChipRow
import com.ipafetch.app.ui.components.DropdownRow
import com.ipafetch.app.ui.components.ErrorLine
import com.ipafetch.app.ui.components.ExpressiveSegmented
import com.ipafetch.app.ui.components.ExpressiveTextField
import com.ipafetch.app.ui.components.PrimaryButton
import com.ipafetch.app.ui.components.RoundIconButton
import com.ipafetch.app.ui.components.SettingsRow
import com.ipafetch.app.ui.components.TrailingValue
import com.ipafetch.app.ui.theme.IpafetchTheme

import com.ipafetch.app.ui.theme.rowShape
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Add / edit sheet. Brand selection drives the default console link and the
 * type, so a new entry takes about three taps.
 */
@Composable
fun AddEditScreen(
    existing: VaultEntry?,
    initialBrandId: String? = null,
    onSave: (VaultEntry, String?) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val colors = IpafetchTheme.colors
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var brandId by remember(existing?.id) { mutableStateOf(existing?.brandId ?: initialBrandId ?: "") }
    var type by remember(existing?.id) { mutableStateOf(EntryType.from(existing?.type)) }
    var account by remember(existing?.id) { mutableStateOf(existing?.account ?: "") }
    var secret by remember(existing?.id) { mutableStateOf(existing?.secret ?: "") }
    var generateUrl by remember(existing?.id) {
        mutableStateOf(existing?.generateUrl ?: BrandCatalog.consoleFor(initialBrandId ?: "") ?: "")
    }
    var docsUrl by remember(existing?.id) { mutableStateOf(existing?.docsUrl ?: "") }
    var usage by remember(existing?.id) { mutableStateOf(existing?.usage ?: "") }
    var category by remember(existing?.id) {
        mutableStateOf(existing?.category ?: BrandCatalog.by(initialBrandId ?: "")?.category ?: EntryCategories.OTHER)
    }
    var environment by remember(existing?.id) { mutableStateOf(existing?.environment ?: "") }
    var tagsText by remember(existing?.id) { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }
    var expiresAt by remember(existing?.id) { mutableStateOf(existing?.expiresAt) }
    var favorite by remember(existing?.id) { mutableStateOf(existing?.favorite ?: false) }
    var error by remember { mutableStateOf<String?>(null) }
    var pickerOpen by remember { mutableStateOf(existing == null && initialBrandId == null) }
    var query by remember { mutableStateOf("") }

    val brands = remember(query) { BrandCatalog.search(query).take(24) }
    val isNew = existing == null

    AppBackground(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoundIconButton(Icons.Rounded.ArrowBack, "Back", onCancel)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isNew) "New entry" else "Edit entry",
                        color = colors.textPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    RoundIconButton(
                        icon = if (favorite) Icons.Rounded.Star else Icons.Rounded.Star,
                        contentDescription = "Favourite",
                        onClick = { favorite = !favorite },
                        tint = if (favorite) colors.accent else colors.textSecondary
                    )
                }
            }

            item {
                ExpressiveSegmented(
                    options = EntryType.editorOrder,
                    selected = type,
                    label = { it.label },
                    onSelect = { type = it },
                    icon = { typeIcon(it) }
                )
            }

            item {
                ExpressiveTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title",
                    placeholder = "e.g. OpenAI production key"
                )
            }

            item {
                Column {
                    Text(
                        "Provider",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )
                    SettingsRow(
                        leading = { BrandTile(brandId, title.ifBlank { "Custom" }, size = 34.dp) },
                        title = BrandCatalog.by(brandId)?.name ?: "Custom provider",
                        supporting = BrandCatalog.by(brandId)?.category ?: "No logo · monogram tile",
                        trailing = {
                            Text(
                                if (pickerOpen) "Close" else "Change",
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = { pickerOpen = !pickerOpen }
                    )
                    if (pickerOpen) {
                        Spacer(Modifier.height(10.dp))
                        ExpressiveTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = "Search 50+ providers",
                            minHeight = 48
                        )
                        Spacer(Modifier.height(10.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(brands, key = { it.id }) { brand ->
                                BrandPickerTile(
                                    brand = brand,
                                    selected = brand.id == brandId,
                                    onClick = {
                                        brandId = brand.id
                                        if (title.isBlank()) title = brand.name
                                        if (generateUrl.isBlank()) generateUrl = brand.consoleUrl
                                        if (category == EntryCategories.OTHER) category = brand.category
                                        pickerOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            item {
                ExpressiveTextField(
                    value = secret,
                    onValueChange = { if (it.length < 8000) secret = it },
                    label = when (type) {
                        EntryType.JSON -> "JSON value"
                        EntryType.LINK -> "Link"
                        EntryType.NOTE -> "Note"
                        else -> "Secret value"
                    },
                    placeholder = when (type) {
                        EntryType.JSON -> "{ \"type\": \"service_account\", … }"
                        EntryType.LINK -> "https://…"
                        else -> "Paste the key or token"
                    },
                    isSecret = type != EntryType.JSON && type != EntryType.NOTE,
                    monospace = true,
                    singleLine = false,
                    minHeight = 96,
                    trailing = if (type == EntryType.TOKEN || type == EntryType.API) {
                        {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { secret = CryptoManager.generateToken() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Refresh,
                                    contentDescription = "Generate random token",
                                    tint = colors.accent,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    } else null
                )
            }

            item {
                ExpressiveTextField(
                    value = generateUrl,
                    onValueChange = { generateUrl = it },
                    label = "Where to generate it",
                    placeholder = "https://console…/api-keys",
                    keyboardType = KeyboardType.Uri
                )
            }

            item {
                ExpressiveTextField(
                    value = docsUrl,
                    onValueChange = { docsUrl = it },
                    label = "Documentation (optional)",
                    placeholder = "https://docs…",
                    keyboardType = KeyboardType.Uri
                )
            }

            item {
                ExpressiveTextField(
                    value = usage,
                    onValueChange = { usage = it },
                    label = "Usage (if provided)",
                    placeholder = "How and where this key is used",
                    singleLine = false,
                    minHeight = 80
                )
            }

            item {
                ExpressiveTextField(
                    value = account,
                    onValueChange = { account = it },
                    label = "Account / project (optional)",
                    placeholder = "you@example.com or org name"
                )
            }

            item {
                Column {
                    Text(
                        "Category",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )
                    ChipRow(
                        options = EntryCategories.ALL,
                        selected = category,
                        label = { it },
                        onSelect = { category = it }
                    )
                }
            }

            item {
                DropdownRow(
                    label = "Environment",
                    value = environment.ifBlank { "Not set" },
                    options = listOf("Not set", "Production", "Staging", "Development", "Local"),
                    onSelect = { environment = if (it == "Not set") "" else it }
                )
            }

            item {
                SettingsRow(
                    title = "Expiry",
                    supporting = expiryText(expiresAt),
                    leading = { Icon(Icons.Rounded.CalendarMonth, null, tint = colors.accent, modifier = Modifier.size(20.dp)) },
                    trailing = {
                        if (expiresAt != null) {
                            RoundIconButton(Icons.Rounded.Close, "Clear expiry", { expiresAt = null }, size = 32)
                        } else {
                            Text("Set", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    },
                    onClick = {
                        expiresAt = if (expiresAt == null) {
                            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 90) }.timeInMillis
                        } else {
                            val calendar = Calendar.getInstance().apply { timeInMillis = expiresAt!!; add(Calendar.DAY_OF_YEAR, 30) }
                            calendar.timeInMillis
                        }
                    }
                )
            }

            item {
                ExpressiveTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = "Tags (comma separated)",
                    placeholder = "prod, billing"
                )
            }

            item {
                ErrorLine(error)
                Spacer(Modifier.height(6.dp))
                PrimaryButton(
                    label = if (isNew) "Save to vault" else "Save changes",
                    onClick = {
                        val problem = when {
                            title.isBlank() -> "Give the entry a title"
                            type == EntryType.JSON && secret.isNotBlank() && !isValidJson(secret) -> "That JSON does not parse"
                            else -> null
                        }
                        error = problem
                        if (problem == null) {
                            val entry = (existing ?: VaultEntry()).copy(
                                title = title.trim(),
                                brandId = brandId,
                                type = type.key,
                                account = account.trim(),
                                generateUrl = generateUrl.trim(),
                                docsUrl = docsUrl.trim(),
                                usage = usage.trim(),
                                category = category,
                                tags = tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                                environment = environment,
                                expiresAt = expiresAt,
                                favorite = favorite,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(entry, secret.takeIf { it.isNotBlank() })
                        }
                    }
                )
                Spacer(Modifier.height(10.dp))
                if (onDelete != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(colors.row)
                            .clickable(onClick = onDelete),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Delete, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Delete entry", color = colors.textSecondary, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandPickerTile(brand: Brand, selected: Boolean, onClick: () -> Unit) {
    val colors = IpafetchTheme.colors
    val shape = rowShape(1f)
    Column(
        modifier = Modifier
            .width(74.dp)
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.row)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        BrandTile(brand.id, brand.name, size = 34.dp)
        Spacer(Modifier.height(6.dp))
        Text(
            brand.name,
            color = if (selected) colors.accent else colors.textSecondary,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun typeIcon(type: EntryType) = when (type) {
    EntryType.TOKEN -> Icons.Rounded.Key
    EntryType.API -> Icons.Rounded.Code
    EntryType.JSON -> Icons.Rounded.Code
    EntryType.LINK -> Icons.Rounded.Link
    EntryType.SECRET -> Icons.Rounded.Key
    EntryType.NOTE -> Icons.Rounded.ContentCopy
}

private fun expiryText(expiresAt: Long?): String =
    if (expiresAt == null) "Never expires · tap to set 90 days"
    else SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(expiresAt)) + " · tap to extend 30 days"

private fun isValidJson(value: String): Boolean = runCatching {
    if (value.trimStart().startsWith("[")) org.json.JSONArray(value) else org.json.JSONObject(value)
    true
}.getOrDefault(false)

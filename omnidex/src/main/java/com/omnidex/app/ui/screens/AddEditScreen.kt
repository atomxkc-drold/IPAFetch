package com.omnidex.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidex.app.crypto.CryptoManager
import com.omnidex.app.domain.AiCatalog
import com.omnidex.app.domain.CustomLogos
import com.omnidex.app.domain.EntryCategories
import com.omnidex.app.domain.EntryType
import com.omnidex.app.domain.PermissionCatalog
import com.omnidex.app.domain.PermissionGrant
import com.omnidex.app.domain.ServiceProfiles
import com.omnidex.app.domain.VaultEntry
import com.omnidex.app.ui.brand.Brand
import com.omnidex.app.ui.brand.BrandCatalog
import com.omnidex.app.ui.components.AppBackground
import com.omnidex.app.ui.components.BrandTile
import com.omnidex.app.ui.components.ChipRow
import com.omnidex.app.ui.components.DropdownRow
import com.omnidex.app.ui.components.ErrorLine
import com.omnidex.app.ui.components.ExpressiveTextField
import com.omnidex.app.ui.components.PrimaryButton
import com.omnidex.app.ui.components.RoundIconButton
import com.omnidex.app.ui.components.SettingsRow
import com.omnidex.app.domain.TypeFields
import com.omnidex.app.ui.screens.TypePillRow
import com.omnidex.app.ui.theme.OmniDexTheme
import com.omnidex.app.ui.theme.FavoriteGold
import com.omnidex.app.ui.theme.rowShape
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val expiryChoices = listOf(
    "Never" to 0,
    "7 days" to 7,
    "30 days" to 30,
    "90 days" to 90,
    "1 year" to 365
)

/**
 * Add / edit sheet. Provider drives the console link, category, token variants
 * and the permission vocabulary; tags and permissions open as bottom sheets.
 */
@Composable
fun AddEditScreen(
    existing: VaultEntry?,
    initialBrandId: String? = null,
    knownTags: List<String> = emptyList(),
    existingExtras: Map<String, String> = emptyMap(),
    onSave: (VaultEntry, String?, Map<String, String>) -> Unit,
    onCancel: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val colors = OmniDexTheme.colors
    val context = LocalContext.current
    var usedByApps by remember(existing?.id) { mutableStateOf(existing?.usedByApps ?: emptyList()) }
    var showApps by remember { mutableStateOf(false) }
    var title by remember(existing?.id) { mutableStateOf(existing?.title ?: "") }
    var brandId by remember(existing?.id) { mutableStateOf(existing?.brandId ?: initialBrandId ?: "") }
    val logoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            CustomLogos.save(context, uri)?.let { saved -> brandId = saved }
        }
    }
    var type by remember(existing?.id) { mutableStateOf(EntryType.from(existing?.type)) }
    var variant by remember(existing?.id) {
        mutableStateOf(
            existing?.variant
                ?: ServiceProfiles.of(initialBrandId ?: existing?.brandId ?: "", existing?.category ?: EntryCategories.OTHER)
                    .variants.firstOrNull()
                ?: ""
        )
    }
    var account by remember(existing?.id) { mutableStateOf(existing?.account ?: "") }
    var usedBy by remember(existing?.id) { mutableStateOf(existing?.usedBy ?: "") }
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
    var tags by remember(existing?.id) { mutableStateOf(existing?.tags ?: emptyList()) }
    var permissions by remember(existing?.id) { mutableStateOf(existing?.permissions ?: emptyList()) }
    var extras by remember(existing?.id) { mutableStateOf(existingExtras) }
    var expiresAt by remember(existing?.id) { mutableStateOf(existing?.expiresAt) }
    var favorite by remember(existing?.id) { mutableStateOf(existing?.favorite ?: false) }

    var error by remember { mutableStateOf<String?>(null) }
    var pickerOpen by remember { mutableStateOf(existing == null && initialBrandId == null) }
    var showTags by remember { mutableStateOf(false) }
    var showPermissions by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    val brands = remember(query) { BrandCatalog.search(query).take(24) }
    val profile = remember(brandId, category) { ServiceProfiles.of(brandId, category) }
    val variants = profile.variants
    val extraFields = remember(profile, variant) { profile.fieldsFor(variant) }
    val isNew = existing == null
    val permissionDefs = remember(brandId) { PermissionCatalog.forBrand(brandId) }
    val lockedCount = remember(permissions, brandId) {
        permissions.count { PermissionCatalog.def(brandId, it.name)?.readOnly == true }
    }

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
                        icon = Icons.Rounded.Star,
                        contentDescription = "Favourite",
                        onClick = { favorite = !favorite },
                        tint = if (favorite) FavoriteGold else colors.textSecondary
                    )
                }
            }

            item {
                TypePillRow(
                    options = EntryType.editorOrder,
                    selected = type,
                    onSelect = { type = it }
                )
            }

            item {
                ExpressiveTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Title",
                    placeholder = "e.g. GitHub deploy token"
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
                            item {
                                Column(
                                    modifier = Modifier
                                        .width(74.dp)
                                        .clip(rowShape(1f))
                                        .background(
                                            if (brandId.isBlank() || CustomLogos.isCustom(brandId)) colors.accentSoft
                                            else colors.row
                                        )
                                        .clickable { brandId = "" }
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    BrandTile("", "Custom", size = 34.dp)
                                    Spacer(Modifier.height(6.dp))
                                    Text("Custom", color = colors.textSecondary, fontSize = 10.sp)
                                }
                            }
                            items(brands, key = { it.id }) { brand ->
                                BrandPickerTile(
                                    brand = brand,
                                    selected = brand.id == brandId,
                                    onClick = {
                                        brandId = brand.id
                                        if (title.isBlank()) title = brand.name
                                        if (generateUrl.isBlank()) generateUrl = brand.consoleUrl
                                        if (category == EntryCategories.OTHER) category = brand.category
                                        val nextVariants = ServiceProfiles.of(brand.id, brand.category).variants
                                        variant = nextVariants.firstOrNull() ?: ""
                                        pickerOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (brandId.isBlank() || CustomLogos.isCustom(brandId)) {
                item {
                    SettingsRow(
                        title = "Provider logo",
                        supporting = if (CustomLogos.isCustom(brandId)) {
                            "Custom image uploaded"
                        } else {
                            "Upload your own image for this provider"
                        },
                        leading = { BrandTile(brandId, title.ifBlank { "Custom" }, size = 30.dp) },
                        trailing = {
                            Text(
                                if (CustomLogos.isCustom(brandId)) "Replace" else "Upload",
                                color = colors.accent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        onClick = { logoLauncher.launch("image/*") }
                    )
                }

            // Type-specific fields (SSH host/port, ADB package, module repo, etc.)
            val typeFields = remember(type) { TypeFields.forType(type) }
            if (typeFields.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            "${type.label} details",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                        items(typeFields, key = { it.key }) { field ->
                            ExpressiveTextField(
                                value = extras[field.key] ?: "",
                                onValueChange = { value ->
                                    extras = if (value.isBlank()) extras - field.key else extras + (field.key to value)
                                },
                                label = field.label,
                                placeholder = field.placeholder,
                                isSecret = field.secret
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
            if (variants.isNotEmpty()) {
                item {
                    Column {
                        Text(
                            "Token type · ${BrandCatalog.by(brandId)?.name ?: "custom"}",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                        )
                        ExpressiveSegmented(
                            options = variants,
                            selected = variant.ifBlank { variants.first() },
                            label = { it },
                            onSelect = { chosen ->
                                variant = chosen
                                if (chosen == "OAuth app") {
                                    if (type == EntryType.TOKEN) type = EntryType.SECRET
                                }
                            }
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            profile.hintFor(variant) ?: "Choose how this credential is issued.",
                            color = colors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }

            if (extraFields.isNotEmpty()) {
                items(extraFields, key = { it.key }) { field ->
                    ExpressiveTextField(
                        value = extras[field.key] ?: "",
                        onValueChange = { value ->
                            extras = if (value.isBlank()) extras - field.key else extras + (field.key to value)
                        },
                        label = field.label,
                        placeholder = field.placeholder,
                        isSecret = field.secret
                    )
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
                    trailing = if (type == EntryType.TOKEN || type == EntryType.API || type == EntryType.SECRET) {
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
                    label = "Where it was generated",
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

            if (AiCatalog.hasInfo(brandId)) {
                item {
                    AiInfoBlock(
                        brandId = brandId,
                        selectedModel = extras["model"],
                        onSelectModel = { model ->
                            extras = if (extras["model"] == model) extras - "model" else extras + ("model" to model)
                        }
                    )
                }
            }

            item {
                SettingsRow(
                    title = "Used by apps",
                    supporting = if (usedByApps.isEmpty()) {
                        "No apps linked · tap to choose installed apps"
                    } else {
                        "${usedByApps.size} app${if (usedByApps.size == 1) "" else "s"} linked"
                    },
                    leading = { Icon(Icons.Rounded.PhoneAndroid, null, tint = colors.accent, modifier = Modifier.size(20.dp)) },
                    onClick = { showApps = true }
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
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Expires",
                            color = colors.textSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 6.dp).weight(1f)
                        )
                        Text(
                            expirySummary(expiresAt),
                            color = colors.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    ChipRow(
                        options = expiryChoices.map { it.first },
                        selected = expiryChoices.firstOrNull {
                            it.second > 0 && matchesExpiry(expiresAt, it.second)
                        }?.first ?: if (expiresAt == null) "Never" else "",
                        label = { it },
                        onSelect = { label ->
                            val days = expiryChoices.firstOrNull { it.first == label }?.second ?: 0
                            expiresAt = if (days == 0) null else daysFromNow(days)
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    SettingsRow(
                        title = "Pick an exact date",
                        supporting = "Opens a calendar",
                        leading = {
                            Icon(
                                Icons.Rounded.CalendarMonth,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = { showDate = true }
                    )
                }
            }

            item {
                SettingsRow(
                    title = "Permissions",
                    supporting = when {
                        permissions.isEmpty() -> "None selected · tap to choose"
                        lockedCount > 0 -> "${permissions.size} selected · $lockedCount read-only by provider"
                        else -> "${permissions.size} selected · tap to edit"
                    },
                    leading = {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { showPermissions = true }
                )
            }

            item {
                SettingsRow(
                    title = "Tags",
                    supporting = tags.joinToString(", ").ifBlank { "No tags yet · tap to add" },
                    leading = {
                        Icon(
                            Icons.Rounded.LocalOffer,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = { showTags = true }
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
                            type == EntryType.JSON && secret.isNotBlank() && !isValidJson(secret) ->
                                "That JSON does not parse"
                            else -> null
                        }
                        error = problem
                        if (problem == null) {
                            val entry = (existing ?: VaultEntry()).copy(
                                title = title.trim(),
                                brandId = brandId,
                                variant = variant,
                                type = type.key,
                                account = account.trim(),
                                usedBy = usedBy.trim(),
                                generateUrl = generateUrl.trim(),
                                docsUrl = docsUrl.trim(),
                                usage = usage.trim(),
                                category = category,
                                tags = tags,
                                permissions = permissions,
                                usedByApps = usedByApps,
                                environment = environment,
                                expiresAt = expiresAt,
                                favorite = favorite,
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(entry, secret.takeIf { it.isNotBlank() }, extras.filterValues { it.isNotBlank() })
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

    if (showApps) {
        AppPickerSheet(
            selected = usedByApps,
            onDismiss = { showApps = false },
            onChange = { usedByApps = it }
        )
    }
    if (showTags) {
        TagsSheet(
            current = tags,
            known = (knownTags + tags).distinct(),
            onDismiss = { showTags = false },
            onChange = { tags = it }
        )
    }
    if (showPermissions) {
        PermissionsSheet(
            brandId = brandId,
            grants = permissions,
            onDismiss = { showPermissions = false },
            onChange = { permissions = it }
        )
    }
    if (showDate) {
        ExpiryPickerDialog(
            initial = expiresAt,
            onDismiss = { showDate = false },
            onPick = { expiresAt = it }
        )
    }
}

@Composable
private fun BrandPickerTile(brand: Brand, selected: Boolean, onClick: () -> Unit) {
    val colors = OmniDexTheme.colors
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

private fun daysFromNow(days: Int): Long =
    Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, days) }.timeInMillis

private fun matchesExpiry(expiresAt: Long?, days: Int): Boolean {
    val target = expiresAt ?: return false
    val diffDays = (target - System.currentTimeMillis()) / (24 * 60 * 60 * 1000)
    return kotlin.math.abs(diffDays - days) <= 1
}

private fun expirySummary(expiresAt: Long?): String {
    if (expiresAt == null) return "Never"
    val date = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(expiresAt))
    return if (expiresAt < System.currentTimeMillis()) "$date · expired" else date
}

private fun isValidJson(value: String): Boolean = runCatching {
    if (value.trimStart().startsWith("[")) org.json.JSONArray(value) else org.json.JSONObject(value)
    true
}.getOrDefault(false)

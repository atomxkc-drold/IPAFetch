package com.omnidex.app.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocalOffer
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidex.app.domain.AiCatalog
import com.omnidex.app.domain.AppCatalog
import com.omnidex.app.domain.EntryType
import com.omnidex.app.ui.components.AppIcon
import com.omnidex.app.ui.components.ExpressiveTextField
import com.omnidex.app.ui.components.TagPill
import com.omnidex.app.ui.theme.OmniDexTheme
import com.omnidex.app.ui.theme.rowShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    selected: List<String>,
    onDismiss: () -> Unit,
    onChange: (List<String>) -> Unit
) {
    val colors = OmniDexTheme.colors
    val context = LocalContext.current
    val apps = remember { AppCatalog.installedApps(context) }
    var query by remember { mutableStateOf("") }
    val filtered = remember(query, apps) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, true) || it.packageName.contains(query, true) }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.canvas
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Text("Used by", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${selected.size} selected · pick every app that holds this credential",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(12.dp))
            ExpressiveTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search installed apps",
                minHeight = 48
            )
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val isSelected = selected.contains(app.packageName)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape(1f))
                            .background(if (isSelected) colors.accentSoft else colors.row)
                            .clickable {
                                onChange(
                                    if (isSelected) selected - app.packageName
                                    else selected + app.packageName
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(app.packageName, size = 28.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, color = colors.textPrimary, fontSize = 14.sp)
                            Text(app.packageName, color = colors.textSecondary, fontSize = 10.sp)
                        }
                        if (isSelected) {
                            Icon(
                                Check,
                                contentDescription = "Selected",
                                tint = colors.accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done", color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun TypePillRow(
    options: List<EntryType>,
    selected: EntryType,
    onSelect: (EntryType) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = OmniDexTheme.colors
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(options, key = { it.key }) { entryType ->
            val isSelected = entryType == selected
            val icon = typeIcon(entryType)
            Box(
                modifier = Modifier
                    .wrapContentWidth()
                    .clip(CircleShape)
                    .background(if (isSelected) colors.accent else colors.row)
                    .then(if (isSelected) Modifier.border(1.dp, colors.accent, CircleShape) else Modifier)
                    .clickable { onSelect(entryType) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) colors.onAccent else colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        entryType.label,
                        color = if (isSelected) colors.onAccent else colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun typeIcon(type: EntryType) = when (type) {
    EntryType.TOKEN -> Icons.Rounded.Key
    EntryType.API -> Icons.Rounded.Code
    EntryType.JSON -> Icons.Rounded.Code
    EntryType.LINK -> Icons.Rounded.Link
    EntryType.SECRET -> Icons.Rounded.Key
    EntryType.TEXT -> Edit
    EntryType.MODULE -> Icons.Rounded.LocalOffer
    EntryType.ADB -> Icons.Rounded.PhoneAndroid
    EntryType.SSH -> Icons.Rounded.Link
    EntryType.COLOR -> FormatSize
    EntryType.NUMBER -> FormatSize
    EntryType.NOTE -> Icons.Rounded.ContentCopy
    else -> Info
}

/**
 * Read-only AI metadata: modality, base URL and the model ids this provider
 * exposes. Selecting a model stores it in the entry's encrypted extras, which
 * is also what a future connection test will call.
 */
@Composable
fun AiInfoBlock(
    brandId: String,
    selectedModel: String?,
    onSelectModel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val info = AiCatalog.of(brandId) ?: return
    val colors = OmniDexTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape(OmniDexTheme.settings.cornerScale))
            .background(colors.row)
            .padding(16.dp)
    ) {
        Text("Model family", color = colors.textSecondary, fontSize = 11.sp)
        Text(info.modality, color = colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(10.dp))

        Text("Base URL", color = colors.textSecondary, fontSize = 11.sp)
        Text(
            info.baseUrl,
            color = colors.textPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace
        )
        if (info.notes.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(info.notes, color = colors.textSecondary, fontSize = 11.sp)
        }

        Spacer(Modifier.height(12.dp))
        Text("Base models · tap to set default", color = colors.textSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        WrapRow {
            info.models.forEach { model ->
                val chosen = model == selectedModel
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(if (chosen) colors.accent else colors.rowInset)
                        .border(
                            1.dp,
                            if (chosen) colors.accent else colors.hairline,
                            CircleShape
                        )
                        .clickable { onSelectModel(model) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        model,
                        color = if (chosen) colors.onAccent else colors.textPrimary,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
        if (selectedModel != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Default: $selectedModel",
                color = colors.textSecondary,
                fontSize = 11.sp
            )
        }
    }
}

/** Read-only pills for the apps that use this credential. */
@Composable
fun UsedByPills(packages: List<String>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val colors = OmniDexTheme.colors
    if (packages.isEmpty()) return
    WrapRow {
        packages.forEach { pkg ->
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(colors.rowInset)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIcon(pkg, size = 16.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    AppCatalog.labelFor(context, pkg),
                    color = colors.textPrimary,
                    fontSize = 11.sp
                )
            }
        }
    }
}

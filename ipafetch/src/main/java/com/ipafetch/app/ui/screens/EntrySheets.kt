package com.ipafetch.app.ui.screens

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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.ipafetch.app.domain.PermissionAccess
import com.ipafetch.app.domain.PermissionCatalog
import com.ipafetch.app.domain.PermissionGrant
import com.ipafetch.app.ui.components.ExpressiveTextField
import com.ipafetch.app.ui.theme.IpafetchTheme
import kotlin.math.absoluteValue

/** Bright, stable colours so each permission keeps an identity of its own. */
private val pillPalette = listOf(
    Color(0xFFE8593F), Color(0xFF7B5BB8), Color(0xFF11867B),
    Color(0xFFC2185B), Color(0xFF2D6CDF), Color(0xFFB26A00), Color(0xFF00838F)
)

private fun pillColor(name: String): Color =
    pillPalette[name.hashCode().absoluteValue % pillPalette.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsSheet(
    current: List<String>,
    known: List<String>,
    onDismiss: () -> Unit,
    onChange: (List<String>) -> Unit
) {
    val colors = IpafetchTheme.colors
    var draft by remember { mutableStateOf("") }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.canvas
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Tags", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Used to group entries and find them later.",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    ExpressiveTextField(
                        value = draft,
                        onValueChange = { draft = it },
                        placeholder = "New tag",
                        minHeight = 48
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                        .clickable {
                            val tag = draft.trim()
                            if (tag.isNotEmpty() && !current.any { it.equals(tag, true) }) {
                                onChange(current + tag)
                            }
                            draft = ""
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add tag", tint = colors.onAccent)
                }
            }

            if (current.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text("On this entry", color = colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                WrapRow {
                    current.forEach { tag ->
                        RemovablePill(tag) { onChange(current - tag) }
                    }
                }
            }

            val suggestions = known.filterNot { n -> current.any { it.equals(n, true) } }
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(18.dp))
                Text("Used before", color = colors.textSecondary, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                WrapRow {
                    suggestions.forEach { tag ->
                        Box(
                            Modifier
                                .clip(CircleShape)
                                .background(colors.row)
                                .clickable { onChange(current + tag) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(tag, color = colors.accent, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done", color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun RemovablePill(label: String, onRemove: () -> Unit) {
    val colors = IpafetchTheme.colors
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(colors.accentSoft)
            .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.textPrimary, fontSize = 12.sp)
        Spacer(Modifier.width(4.dp))
        Box(
            Modifier
                .size(20.dp)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Remove $label", tint = colors.textSecondary, modifier = Modifier.size(13.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSheet(
    brandId: String,
    grants: List<PermissionGrant>,
    onDismiss: () -> Unit,
    onChange: (List<PermissionGrant>) -> Unit
) {
    val colors = IpafetchTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val groups = remember(brandId) { PermissionCatalog.groups(brandId) }

    fun accessOf(name: String): String =
        grants.firstOrNull { it.name == name }?.access ?: PermissionAccess.OFF

    fun cycle(name: String, readOnly: Boolean) {
        val next = when (accessOf(name)) {
            PermissionAccess.OFF -> PermissionGrant(name, PermissionAccess.READ)
            PermissionAccess.READ ->
                if (readOnly) null else PermissionGrant(name, PermissionAccess.WRITE)
            else -> null
        }
        val without = grants.filterNot { it.name == name }
        onChange(if (next == null) without else without + next)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.canvas
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Permissions", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (brandId == "github") {
                    "Tap a permission to cycle read → write → off. GitHub only issues read for the locked ones."
                } else {
                    "Tap a scope to cycle read → write → off."
                },
                color = colors.textSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )

            groups.forEach { (group, defs) ->
                Spacer(Modifier.height(16.dp))
                Text(group.uppercase(), color = colors.textSecondary, fontSize = 10.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                WrapRow {
                    defs.forEach { def ->
                        val access = accessOf(def.name)
                        PermissionPill(
                            name = def.name,
                            access = access,
                            readOnly = def.readOnly,
                            onClick = { cycle(def.name, def.readOnly) }
                        )
                    }
                }
            }

            if (grants.any { g -> PermissionCatalog.def(brandId, g.name)?.readOnly == true }) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Locked permissions stay read-only because the provider does not offer write access.",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Done", color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PermissionPill(
    name: String,
    access: String,
    readOnly: Boolean,
    onClick: () -> Unit
) {
    val colors = IpafetchTheme.colors
    val tint = pillColor(name)
    val selected = access != PermissionAccess.OFF
    val isWrite = access == PermissionAccess.WRITE
    val background = when {
        isWrite -> tint
        selected -> tint.copy(alpha = 0.18f)
        else -> colors.row
    }
    val contentColor = when {
        isWrite -> Color.White
        selected -> tint
        else -> colors.textSecondary
    }
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .then(if (selected) Modifier.border(1.dp, tint, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (readOnly) {
            Icon(
                Icons.Rounded.Lock,
                contentDescription = "Read only",
                tint = contentColor.copy(alpha = 0.8f),
                modifier = Modifier.size(11.dp)
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(name, color = contentColor, fontSize = 12.sp, fontWeight = if (isWrite) FontWeight.SemiBold else FontWeight.Normal)
        if (selected) {
            Spacer(Modifier.width(6.dp))
            Text(
                if (isWrite) "rw" else "r",
                color = contentColor.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/** Read-only pills used on the detail screen. */
@Composable
fun PermissionSummary(brandId: String, grants: List<PermissionGrant>) {
    if (grants.isEmpty()) return
    WrapRow {
        grants.forEach { grant ->
            val def = PermissionCatalog.def(brandId, grant.name)
            PermissionPill(
                name = grant.name,
                access = grant.access,
                readOnly = def?.readOnly == true,
                onClick = {}
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpiryPickerDialog(
    initial: Long?,
    onDismiss: () -> Unit,
    onPick: (Long?) -> Unit
) {
    val colors = IpafetchTheme.colors
    val state = rememberDatePickerState(initialSelectedDateMillis = initial)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onPick(state.selectedDateMillis)
                onDismiss()
            }) { Text("Set date") }
        },
        dismissButton = {
            TextButton(onClick = {
                onPick(null)
                onDismiss()
            }) { Text("Clear") }
        },
        colors = androidx.compose.material3.DatePickerDefaults.colors(
            containerColor = colors.rowStrong,
            titleContentColor = colors.textPrimary,
            headlineContentColor = colors.textPrimary,
            weekdayContentColor = colors.textSecondary,
            subheadContentColor = colors.textSecondary,
            navigationContentColor = colors.textSecondary,
            selectedDayContainerColor = colors.accent,
            selectedDayContentColor = colors.onAccent,
            todayContentColor = colors.accent,
            todayDateBorderColor = colors.accent
        )
    ) {
        DatePicker(state = state)
    }
}

/** Simple wrapping row so chips flow instead of overflowing. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WrapRow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) { content() }
}

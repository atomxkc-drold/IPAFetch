package com.omnidex.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Timer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.omnidex.app.domain.AppCatalog
import com.omnidex.app.domain.VaultEntry
import com.omnidex.app.ui.theme.OmniDexTheme
import com.omnidex.app.ui.theme.rowShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSheet(
    entries: List<VaultEntry>,
    onDismiss: () -> Unit,
    onStartSession: (scopeLabel: String, ttlMinutes: Int, entryIds: List<Long>, targetPackage: String?) -> Unit
) {
    val colors = OmniDexTheme.colors
    val context = LocalContext.current
    val apps = remember { AppCatalog.installedApps(context) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var scopeLabel by remember { mutableStateOf("ci") }
    var ttlMinutes by remember { mutableStateOf(30) }
    var selectedEntryIds by remember { mutableStateOf(setOf<Long>()) }
    var selectedTarget by remember { mutableStateOf<String?>(null) }
    
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Rounded.Timer, contentDescription = null, tint = colors.accent)
                Text("Ephemeral Session", color = colors.textPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Time-limited credential injection",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(Modifier.height(16.dp))
            
            Text("Scope", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("ci", "review", "debug", "deploy").forEach { scope ->
                    TagPill(
                        text = scope,
                        selected = scopeLabel == scope,
                        onClick = { scopeLabel = scope }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            
            Text("TTL: $ttlMinutes minutes", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            // Simple slider replacement with buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(5, 15, 30, 60, 120, 1440).forEach { ttl ->
                    TagPill(
                        text = if (ttl >= 60) "${ttl/60}h" else "${ttl}m",
                        selected = ttlMinutes == ttl,
                        onClick = { ttlMinutes = ttl }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            
            Text("Select entries (${selectedEntryIds.size} max 10)", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(entries.take(50)) { entry ->
                    val isSelected = selectedEntryIds.contains(entry.id)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape(1f))
                            .background(if (isSelected) colors.accentSoft else colors.row)
                            .clickable {
                                if (isSelected) {
                                    selectedEntryIds = selectedEntryIds - entry.id
                                } else if (selectedEntryIds.size < 10) {
                                    selectedEntryIds = selectedEntryIds + entry.id
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.title, color = colors.textPrimary, fontSize = 13.sp)
                            Text(entry.type, color = colors.textSecondary, fontSize = 10.sp)
                        }
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            
            Text("Target app (optional)", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.heightIn(max = 120.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(apps.take(20)) { app ->
                    val isSelected = selectedTarget == app.packageName
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape(1f))
                            .background(if (isSelected) colors.accentSoft else colors.row)
                            .clickable { selectedTarget = if (isSelected) null else app.packageName }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIcon(app.packageName, size = 24.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(app.label, color = colors.textPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            
            PrimaryButton(
                text = "Start Session",
                onClick = {
                    if (selectedEntryIds.isNotEmpty()) {
                        onStartSession(scopeLabel, ttlMinutes, selectedEntryIds.toList(), selectedTarget)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

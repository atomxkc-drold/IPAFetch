package com.ipafetch.app.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipafetch.app.crypto.ExportManager
import com.ipafetch.app.ui.components.AppBackground
import com.ipafetch.app.ui.components.ErrorLine
import com.ipafetch.app.ui.components.ExpressiveTextField
import com.ipafetch.app.ui.components.PrimaryButton
import com.ipafetch.app.ui.components.RoundIconButton
import com.ipafetch.app.ui.components.SecondaryButton
import com.ipafetch.app.ui.theme.IpafetchTheme
import com.ipafetch.app.ui.theme.rowShape

@Composable
fun ExportScreen(
    vaultJson: String,
    onBack: () -> Unit
) {
    val colors = IpafetchTheme.colors
    val context = LocalContext.current
    var passphrase by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val payload = result
        if (uri != null && payload != null) {
            val wrote = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(payload.toByteArray(Charsets.UTF_8))
                }
            }.isSuccess
            notice = if (wrote) "Saved to the folder you picked" else "Could not write that file"
        }
    }

    AppBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Rounded.ArrowBack, "Back", onBack)
                Spacer(Modifier.width(6.dp))
                Text("Export vault", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "The backup is encrypted with a passphrase you choose here. Store it separately from the file.",
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(16.dp))
            ExpressiveTextField(
                value = passphrase,
                onValueChange = { passphrase = it; error = null },
                label = "Backup passphrase",
                placeholder = "At least 8 characters",
                isSecret = true
            )
            Spacer(Modifier.height(6.dp))
            ErrorLine(error)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                label = "Encrypt and prepare",
                onClick = {
                    error = runCatching { ExportManager.export(vaultJson, passphrase) }
                        .onSuccess { result = it }
                        .exceptionOrNull()?.message
                }
            )
            result?.let { envelope ->
                Spacer(Modifier.height(18.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(rowShape(IpafetchTheme.settings.cornerScale))
                        .background(colors.row)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Lock, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Encrypted payload", color = colors.textSecondary, fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    SelectionContainer {
                        Text(
                            envelope,
                            color = colors.textPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.heightIn(max = 220.dp)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(colors.row)
                            .clickable {
                                val clipboard = context.getSystemService(android.content.ClipboardManager::class.java)
                                clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("IPAFetch backup", envelope))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.ContentCopy, null, tint = colors.accent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy", color = colors.accent, fontSize = 14.sp)
                        }
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(colors.accent)
                            .clickable {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, envelope)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share backup"))
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Share, null, tint = colors.onAccent, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Share", color = colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SecondaryButton(
                    label = "Save to folder…",
                    onClick = { saveLauncher.launch("ipafetch-backup.json") }
                )
                if (notice != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(notice!!, color = colors.accent, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ImportScreen(
    onBack: () -> Unit,
    onImport: (String, String) -> String?
) {
    val colors = IpafetchTheme.colors
    val context = LocalContext.current
    var payload by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val openLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            payload = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull().orEmpty()
            message = null
            error = null
        }
    }

    AppBackground(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RoundIconButton(Icons.Rounded.ArrowBack, "Back", onBack)
                Spacer(Modifier.width(6.dp))
                Text("Import vault", color = colors.textPrimary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "Paste an exported payload and its passphrase. Importing replaces the current vault.",
                color = colors.textSecondary,
                fontSize = 13.sp,
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(16.dp))
            SecondaryButton(
                label = "Open a backup file…",
                onClick = { openLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
            )
            Spacer(Modifier.height(10.dp))
            ExpressiveTextField(
                value = payload,
                onValueChange = { payload = it; error = null; message = null },
                label = "Encrypted payload",
                placeholder = "{\"v\":2,…}",
                monospace = true,
                singleLine = false,
                minHeight = 110
            )
            Spacer(Modifier.height(12.dp))
            ExpressiveTextField(
                value = passphrase,
                onValueChange = { passphrase = it; error = null; message = null },
                label = "Backup passphrase",
                isSecret = true
            )
            Spacer(Modifier.height(6.dp))
            ErrorLine(error)
            if (message != null) {
                Text(message!!, color = colors.accent, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.height(12.dp))
            PrimaryButton(
                label = "Decrypt and import",
                onClick = {
                    val problem = onImport(payload, passphrase)
                    if (problem == null) {
                        message = "Vault imported"
                    } else {
                        error = problem
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton(label = "Cancel", onClick = onBack)
            Spacer(Modifier.height(20.dp))
        }
    }
}

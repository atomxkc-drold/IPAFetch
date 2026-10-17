package com.ipafetch.app.ui

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ipafetch.app.domain.KeyEntry
import com.ipafetch.app.security.ClipboardGuard
import com.ipafetch.app.ui.theme.ApiVaultTheme
import com.ipafetch.app.ui.viewmodels.MainViewModel
import androidx.compose.material3.icons.filled.ContentCopy
import androidx.compose.material3.icons.filled.FileDownload

@Composable
fun ExportScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    ApiVaultTheme {
        val context = LocalContext.current

        val coroutineScope = rememberCoroutineScope()
        var passphrase by remember { mutableStateOf("") }
        var exported by remember { mutableStateOf<String?>(null) }
        var showingEntries by remember { mutableStateOf(false) }

        val repository = viewModel.getRepository()
        val entries by repository.getAll().collectAsStateWithLifecycle()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            TopAppBar(
                title = { Text("Export") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = androidx.compose.material3.icons.filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centeredTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            Text("Export passphrase", style = MaterialTheme.typography.labelMedium)

            androidx.compose.material3.OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    if (passphrase.isBlank()) {
                        Toast.makeText(context, "Enter an export passphrase", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    coroutineScope.launch {
                        try {
                            exported = viewModel.exportAll(passphrase.toCharArray())
                        } catch (e: Exception) {
                            Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Export vault")
            }

            exported?.let { blob ->
                androidx.compose.material3.Spacer(modifier = Modifier.height(16.dp))

                Text("Encrypted backup", style = MaterialTheme.typography.labelMedium)

                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(cm.newTextClip("IPAFetch export", blob))
                        Toast.makeText(context, "Export copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Copy encrypted export")
                }

                TextButton(onClick = { showingEntries = !showingEntries }) {
                    Text(if (showingEntries) "Hide entries" else "Show entries being exported")
                }

                if (showingEntries) {
                    androidx.compose.material3.Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                    ) {
                        items(entries) { entry ->
                            ListItem(
                                headlineText = { Text(entry.name) },
                                supportingText = { Text("${entry.service} • ${entry.category}") }
                            )
                        }
                    }
                }
            }

            TextButton(onClick = onBack) {
                Text("Cancel")
            }
        }
    }
}

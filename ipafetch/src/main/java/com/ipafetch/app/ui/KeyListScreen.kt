package com.ipafetch.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipafetch.app.domain.KeyEntry
import com.ipafetch.app.security.ClipboardGuard
import com.ipafetch.app.ui.theme.ApiVaultTheme
import com.ipafetch.app.ui.viewmodels.MainViewModel
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Add
import androidx.compose.material3.icons.filled.ContentCopy

@Composable
fun KeyListScreen(
    viewModel: MainViewModel,
    onAdd: () -> Unit,
    onEdit: (KeyEntry) -> Unit
) {
    ApiVaultTheme {
        val context = LocalContext.current
        val repository = viewModel.getRepository()
        val entries by repository.getAll().collectAsStateWithLifecycle()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("ApiVault") },
                    colors = TopAppBarDefaults.centeredTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())
                )
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onAdd) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add key")
                }
            }
        ) { padding ->
            if (entries.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(32.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No keys yet", style = MaterialTheme.typography.headlineMedium)
                    Text("Tap + to add your first API key", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entries) { entry ->
                        KeyCard(
                            entry = entry,
                            onEdit = { onEdit(entry) },
                            onCopy = {
                                val plain = viewModel.decryptValue(entry.encryptedValue)
                                ClipboardGuard.copy(context, entry.name, plain)
                                android.widget.Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyCard(
    entry: KeyEntry,
    onEdit: () -> Unit,
    onCopy: () -> Unit
) {
    Card(onClick = onEdit) {
        ListItem(
            headlineText = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingText = {
                Text(
                    "${entry.service} • ${entry.category}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            trailingContent = {
                IconButton(onClick = onCopy) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy")
                }
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

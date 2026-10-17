package com.ipafetch.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ipafetch.app.crypto.CryptoManager
import com.ipafetch.app.domain.Category
import com.ipafetch.app.domain.KeyEntry
import com.ipafetch.app.ui.theme.ApiVaultTheme
import java.time.Instant
import java.time.temporal.ChronoUnit

@Composable
fun AddEditScreen(
    existing: KeyEntry?,
    onSave: (KeyEntry, String?) -> Unit,
    onCancel: () -> Unit
) {
    ApiVaultTheme {
        val context = LocalContext.current

        var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "") }
        var service by remember(existing?.id) { mutableStateOf(existing?.service ?: "") }
        var category by remember(existing?.id) { mutableStateOf(existing?.category ?: Category.OTHER) }
        var username by remember(existing?.id) { mutableStateOf(existing?.username ?: "") }
        var endpoint by remember(existing?.id) { mutableStateOf(existing?.endpoint ?: "") }
        var notes by remember(existing?.id) { mutableStateOf(existing?.notes ?: "") }
        var secret by remember(existing?.id) { mutableStateOf("") }
        var expiresAt by remember(existing?.id) { mutableStateOf(existing?.expiresAt) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(if (existing == null) "Add key" else "Edit key") },
                    colors = TopAppBarDefaults.centeredTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = service, onValueChange = { service = it },
                    label = { Text("Service") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelMedium)
                Category.ALL.forEach { c ->
                    OutlinedButton(
                        onClick = { category = c },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (c == category) MaterialTheme.colorScheme.primaryContainer else null,
                            contentColor = if (c == category) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        ),
                        border = null,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(c)
                    }
                }

                OutlinedTextField(
                    value = username, onValueChange = { username = it },
                    label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = endpoint, onValueChange = { endpoint = it },
                    label = { Text("Endpoint / URL") }, singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                if (existing == null) {
                    OutlinedTextField(
                        value = secret, onValueChange = { secret = it },
                        label = { Text("API key") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedButton(
                    onClick = {
                        secret = CryptoManager.generateToken()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate token")
                }

                OutlinedButton(
                    onClick = {
                        expiresAt = Instant.now().plus(90, ChronoUnit.DAYS).toEpochMilli()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set expiry (90 days)")
                }

                Button(
                    onClick = {
                        if (name.isBlank() || service.isBlank()) {
                            Toast.makeText(context, "Name and service required", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (existing == null && secret.isBlank()) {
                            Toast.makeText(context, "Enter a key value", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val entry = (existing ?: KeyEntry()).copy(
                            name = name, service = service, category = category,
                            username = username, endpoint = endpoint, notes = notes,
                            expiresAt = expiresAt,
                            updatedAt = System.currentTimeMillis()
                        )
                        onSave(entry, secret)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save")
                }
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

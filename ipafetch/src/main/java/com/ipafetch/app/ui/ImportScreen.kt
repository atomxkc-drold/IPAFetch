package com.ipafetch.app.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Spacer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ipafetch.app.crypto.ExportManager
import com.ipafetch.app.ui.theme.ApiVaultTheme
import com.ipafetch.app.ui.viewmodels.MainViewModel
import androidx.compose.material3.icons.filled.ArrowBack
import androidx.compose.material3.icons.filled.FileUpload
import kotlinx.coroutines.launch

@Composable
fun ImportScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    ApiVaultTheme {
        val context = LocalContext.current

        var passphrase by remember { mutableStateOf("") }
        var importText by remember { mutableStateOf("") }
        var parsedCount by remember { mutableStateOf<Int?>(null) }

        val coroutineScope = rememberCoroutineScope()
        val repository = viewModel.getRepository()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            TopAppBar(
                title = { Text("Import") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centeredTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Import passphrase", style = MaterialTheme.typography.labelMedium)

            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Passphrase") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Paste encrypted export", style = MaterialTheme.typography.labelMedium)

            OutlinedTextField(
                value = importText,
                onValueChange = { importText = it },
                label = { Text("Encrypted JSON") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = cm.primaryClip
                        val text = clip?.getItemAt(0)?.text ?: ""
                        importText = text.toString()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Paste from clipboard")
                }

                Button(
                    onClick = {
                        try {
                            val parsed = ExportManager.import(
                                importText,
                                passphrase.toCharArray()
                            ) { repository.encryptValue(it) }
                            parsedCount = parsed.size
                        } catch (e: Exception) {
                            Toast.makeText(context, "Import parse failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Preview import")
                }
            }

            parsedCount?.let { count ->
                Spacer(modifier = Modifier.height(16.dp))
                Text("Ready to import $count entries", style = MaterialTheme.typography.labelMedium)
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                viewModel.importAll(importText, passphrase.toCharArray())
                                Toast.makeText(context, "Imported $count entries", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm import")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onBack) {
                Text("Cancel")
            }
        }
    }
}

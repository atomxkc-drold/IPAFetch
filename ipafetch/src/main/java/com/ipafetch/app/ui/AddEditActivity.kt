package com.ipafetch.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModels
import com.ipafetch.app.ui.theme.ApiVaultTheme
import com.ipafetch.app.ui.viewmodels.AddEditViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AddEditActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val entryId = intent.getLongExtra("entry_id", 0L)
        val viewModel by viewModels<AddEditViewModel>()

        setContent {
            ApiVaultTheme {
                val entry by viewModel.entry.collectAsStateWithLifecycle()

                LaunchedEffect(entryId) {
                    viewModel.loadEntry(entryId)
                }

                AddEditScreen(
                    existing = entry,
                    onSave = { e, secret -> viewModel.save(e, secret); finish() },
                    onCancel = { finish() }
                )
            }
        }
    }
}

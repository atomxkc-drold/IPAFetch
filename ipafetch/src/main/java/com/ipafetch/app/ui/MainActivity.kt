package com.ipafetch.app.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Alignment
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipafetch.app.data.SecurePrefs
import com.ipafetch.app.ui.biometric.BiometricGate
import com.ipafetch.app.ui.theme.ApiVaultTheme
import com.ipafetch.app.ui.viewmodels.MainViewModel
import android.view.Window

class MainActivity : ComponentActivity() {

    private var gate: BiometricGate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel = MainViewModel(application)

        fun promptBiometric() {
            gate?.shutdown()
            gate = BiometricGate(
                activity = this,
                onUnlocked = { viewModel.onMasterKeyReady(it) },
                onFailed = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() },
                onError = { msg -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show() }
            )
            gate?.prompt()
        }

        fun startAddEdit(entryId: Long = 0L) {
            startActivity(Intent(this, AddEditActivity::class.java).apply {
                if (entryId != 0L) putExtra("entry_id", entryId)
            })
        }

        if (SecurePrefs.isSetupDone(application)) {
            viewModel.requestUnlock()
        }

        setContent {
            ApiVaultTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                var navScreen by remember { mutableStateOf<NavScreen?>(null) }

                LaunchedEffect(state) {
                    if (state is MainViewModel.State.Locked) promptBiometric()
                }

                when (state) {
                    is MainViewModel.State.Setup -> {
                        SetupScreen(
                            onSetupComplete = { viewModel.setupMasterKey(it) }
                        )
                    }
                    is MainViewModel.State.Locked -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Unlock required", style = MaterialTheme.typography.headlineMedium)
                                Button(onClick = { viewModel.requestUnlock() }) {
                                    Text("Retry unlock")
                                }
                            }
                        }
                    }
                    is MainViewModel.State.Unlocked -> {
                        when (navScreen) {
                            null -> KeyListScreen(
                                viewModel = viewModel,
                                onAdd = { startAddEdit(0L) },
                                onEdit = { startAddEdit(it.id) },
                                onExport = { navScreen = NavScreen.Export },
                                onImport = { navScreen = NavScreen.Import }
                            )
                            is NavScreen.Export -> ExportScreen(
                                viewModel = viewModel,
                                onBack = { navScreen = null }
                            )
                            is NavScreen.Import -> ImportScreen(
                                viewModel = viewModel,
                                onBack = { navScreen = null }
                            )
                        }
                    }
                    is MainViewModel.State.Error -> {
                        Scaffold(modifier = Modifier.fillMaxSize()) { _ ->
                            Text(
                                "Error: ${(state as MainViewModel.State.Error).message}",
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.setFlags(
                Window.FLAG_SECURE,
                Window.FLAG_SECURE
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        gate?.shutdown()
    }
}

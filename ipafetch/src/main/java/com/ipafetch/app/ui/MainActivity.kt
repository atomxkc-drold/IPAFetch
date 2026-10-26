package com.ipafetch.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipafetch.app.crypto.ExportManager
import com.ipafetch.app.data.AppSettings
import com.ipafetch.app.domain.VaultEntry
import com.ipafetch.app.ui.SetupScreen
import com.ipafetch.app.ui.UnlockScreen
import com.ipafetch.app.ui.biometric.BiometricGate
import com.ipafetch.app.ui.components.BottomPillNav
import com.ipafetch.app.ui.components.NavItem
import com.ipafetch.app.ui.screens.AddEditScreen
import com.ipafetch.app.ui.screens.CategoriesScreen
import com.ipafetch.app.ui.screens.EntryDetailScreen
import com.ipafetch.app.ui.screens.ExportScreen
import com.ipafetch.app.ui.screens.ImportScreen
import com.ipafetch.app.ui.screens.SettingsScreen
import com.ipafetch.app.ui.screens.VaultScreen
import com.ipafetch.app.ui.theme.IpafetchTheme
import com.ipafetch.app.ui.viewmodels.VaultViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: VaultViewModel by viewModels()
    private var gate: BiometricGate? = null
    private var backgroundedAt = 0L
    private var biometricPrompted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        makeSystemBarsTransparent()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            LaunchedEffect(settings.allowScreenshots) {
                applyScreenSecurity(settings.allowScreenshots)
            }
            IpafetchTheme(settings) {
                val lockState by viewModel.lockState.collectAsStateWithLifecycle()
                AppRoot(
                    viewModel = viewModel,
                    settings = settings,
                    lockState = lockState,
                    onRequestBiometric = { promptBiometric(settings) },
                    onOpenExternal = { url -> openExternally(url) }
                )
            }
        }
    }

    private fun promptBiometric(settings: AppSettings) {
        if (!settings.biometric || biometricPrompted) return
        biometricPrompted = true
        val next = BiometricGate(
            activity = this,
            onUnlocked = { key -> viewModel.unlockWithBiometricKey(key) },
            onFailed = { message -> toast(message) },
            onUnavailable = { message -> toast(message) }
        )
        gate?.shutdown()
        gate = next
        if (next.canAuthenticate()) next.prompt() else toast("Set up a screen lock to use biometrics")
    }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    /** Bars stay transparent so the app gradient runs edge to edge. */
    private fun makeSystemBarsTransparent() {
        @Suppress("DEPRECATION")
        run {
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    private fun applyScreenSecurity(allowScreenshots: Boolean) {
        if (allowScreenshots) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private fun openExternally(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { toast("No app can open that link") }
    }

    override fun onStart() {
        super.onStart()
        biometricPrompted = false
        val timeoutMs = viewModel.settings.value.autoLockSeconds * 1000L
        val now = SystemClock.elapsedRealtime()
        if (backgroundedAt > 0 && now - backgroundedAt >= timeoutMs) viewModel.lock()
        backgroundedAt = 0L
    }

    override fun onStop() {
        backgroundedAt = SystemClock.elapsedRealtime()
        if (viewModel.settings.value.autoLockSeconds == 0) viewModel.lock()
        super.onStop()
    }

    override fun onDestroy() {
        gate?.shutdown()
        gate = null
        super.onDestroy()
    }

    @Composable
    private fun AppRoot(
        viewModel: VaultViewModel,
        settings: AppSettings,
        lockState: VaultViewModel.LockState,
        onRequestBiometric: () -> Unit,
        onOpenExternal: (String) -> Unit
    ) {
        var screen by remember { mutableStateOf<Screen>(Screen.Vault) }
        var webUrl by remember { mutableStateOf<String?>(null) }
        var vaultQuery by remember { mutableStateOf("") }
        var confirmReset by remember { mutableStateOf(false) }
        var setupError by remember { mutableStateOf<String?>(null) }
        var unlockError by remember { mutableStateOf<String?>(null) }

        val entries by viewModel.entries.collectAsStateWithLifecycle()

        fun copyEntry(entry: VaultEntry) {
            viewModel.copyText(entry.id, viewModel.decrypt(entry.secret), entry.title)
            toast(if (settings.clipboardClear) "Copied · clears in 45s" else "Copied")
        }

        fun handleOpen(url: String) {
            if (settings.showSourceInApp) webUrl = url else onOpenExternal(url)
        }

        BackHandler(enabled = webUrl != null || screen != Screen.Vault) {
            if (webUrl != null) webUrl = null else screen = Screen.Vault
        }

        LaunchedEffect(lockState) {
            if (lockState == VaultViewModel.LockState.LOCKED) {
                screen = Screen.Vault
                webUrl = null
            }
        }

        when (lockState) {
            VaultViewModel.LockState.SETUP -> SetupScreen(
                onCreate = { password -> setupError = viewModel.setupVault(password) },
                error = setupError
            )

            VaultViewModel.LockState.LOCKED -> {
                LaunchedEffect(settings.biometric) {
                    if (settings.biometric) onRequestBiometric()
                }
                UnlockScreen(
                    error = unlockError,
                    biometricAvailable = settings.biometric,
                    onSubmitPassword = { password ->
                        unlockError = viewModel.unlockWithPassword(password)
                    },
                    onBiometric = onRequestBiometric
                )
            }

            VaultViewModel.LockState.UNLOCKED -> {
                val bottomBar: @Composable () -> Unit = {
                    BottomPillNav(
                        items = navItems,
                        selectedKey = when (screen) {
                            is Screen.Categories -> "categories"
                            is Screen.Settings, is Screen.Export, is Screen.Import -> "settings"
                            else -> "vault"
                        },
                        onSelect = { item ->
                            screen = when (item.key) {
                                "vault" -> Screen.Vault
                                "add" -> Screen.Add(null)
                                "categories" -> Screen.Categories
                                else -> Screen.Settings
                            }
                        }
                    )
                }

                when (val current = screen) {
                    Screen.Vault -> VaultScreen(
                        entries = entries,
                        onOpenEntry = { screen = Screen.Detail(it) },
                        onAdd = { screen = Screen.Add(null) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onCopy = { entry -> copyEntry(entry) },
                        onDelete = { entry ->
                            viewModel.deleteEntry(entry.id)
                            toast("Deleted ${entry.title}")
                        },
                        initialQuery = vaultQuery,
                        bottomBar = bottomBar
                    )

                    Screen.Categories -> CategoriesScreen(
                        entries = entries,
                        onOpenCategory = { category ->
                            vaultQuery = category
                            screen = Screen.Vault
                        },
                        onOpenProvider = { name ->
                            vaultQuery = name
                            screen = Screen.Vault
                        },
                        bottomBar = bottomBar
                    )

                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        entryCount = entries.size,
                        onUpdate = viewModel::updateSettings,
                        onExport = { screen = Screen.Export },
                        onImport = { screen = Screen.Import },
                        onReset = { confirmReset = true },
                        bottomBar = bottomBar
                    )

                    is Screen.Add -> AddEditScreen(
                        existing = null,
                        initialBrandId = current.brandId,
                        onSave = { entry, secret ->
                            viewModel.saveEntry(entry, secret)
                            screen = Screen.Vault
                            toast("Saved to vault")
                        },
                        onCancel = { screen = Screen.Vault }
                    )

                    is Screen.Edit -> {
                        val entry = entries.firstOrNull { it.id == current.id }
                        if (entry == null) {
                            LaunchedEffect(current.id) { screen = Screen.Vault }
                        } else {
                            AddEditScreen(
                                existing = entry,
                                onSave = { updated, secret ->
                                    val saved = viewModel.saveEntry(updated, secret)
                                    screen = saved?.let { Screen.Detail(it.id) } ?: Screen.Vault
                                    toast("Changes saved")
                                },
                                onCancel = { screen = Screen.Detail(entry.id) },
                                onDelete = {
                                    viewModel.deleteEntry(entry.id)
                                    screen = Screen.Vault
                                    toast("Deleted")
                                }
                            )
                        }
                    }

                    is Screen.Detail -> {
                        val entry = entries.firstOrNull { it.id == current.id }
                        if (entry == null) {
                            LaunchedEffect(current.id) { screen = Screen.Vault }
                        } else {
                            EntryDetailScreen(
                                entry = entry,
                                secretPlain = { viewModel.decrypt(entry.secret) },
                                onBack = { screen = Screen.Vault },
                                onEdit = { screen = Screen.Edit(entry.id) },
                                onCopy = { copyEntry(entry) },
                                onToggleFavorite = { viewModel.toggleFavorite(entry.id) },
                                onDelete = {
                                    viewModel.deleteEntry(entry.id)
                                    screen = Screen.Vault
                                    toast("Deleted")
                                },
                                onOpenUrl = { handleOpen(it) }
                            )
                        }
                    }

                    Screen.Export -> {
                        val vaultJson = remember { viewModel.exportAll() }
                        ExportScreen(vaultJson = vaultJson, onBack = { screen = Screen.Settings })
                    }

                    Screen.Import -> ImportScreen(
                        onBack = { screen = Screen.Settings },
                        onImport = { payload, passphrase ->
                            val decrypted = ExportManager.import(payload.trim(), passphrase)
                            if (decrypted == null) "Wrong passphrase or damaged payload"
                            else viewModel.importFromJson(decrypted)
                        }
                    )
                }

                webUrl?.let { url ->
                    WebOverlay(url = url, onClose = { webUrl = null })
                }
            }
        }

        if (confirmReset) {
            AlertDialog(
                onDismissRequest = { confirmReset = false },
                title = { Text("Reset the vault?") },
                text = { Text("Every entry, the master key and all vault preferences will be deleted. This cannot be undone.") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmReset = false
                        viewModel.resetVault()
                        screen = Screen.Vault
                    }) { Text("Reset") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
                }
            )
        }
    }

    @Composable
    private fun WebOverlay(url: String, onClose: () -> Unit) {
        Box(Modifier.fillMaxSize()) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = false
                        setBackgroundColor(android.graphics.Color.WHITE)
                        loadUrl(url)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(androidx.compose.ui.graphics.Color.White)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = androidx.compose.ui.graphics.Color.Black)
                Spacer(Modifier.width(6.dp))
                Text("Close", color = androidx.compose.ui.graphics.Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    private val navItems: List<NavItem> = listOf(
        NavItem("vault", "Vault", Icons.Rounded.GridView),
        NavItem("add", "Add", Icons.Rounded.Add, isAction = true),
        NavItem("categories", "Categories", Icons.Rounded.Widgets),
        NavItem("settings", "Settings", Icons.Rounded.Settings)
    )
}

sealed interface Screen {
    data object Vault : Screen
    data object Categories : Screen
    data object Settings : Screen
    data object Export : Screen
    data object Import : Screen
    data class Add(val brandId: String?) : Screen
    data class Detail(val id: Long) : Screen
    data class Edit(val id: Long) : Screen
}

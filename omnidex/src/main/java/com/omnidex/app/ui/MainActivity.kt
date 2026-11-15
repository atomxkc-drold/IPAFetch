package com.omnidex.app

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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.omnidex.app.crypto.ExportManager
import com.omnidex.app.data.AppSettings
import com.omnidex.app.data.SecurePrefs
import com.omnidex.app.domain.VaultEntry
import com.omnidex.app.ui.SetupScreen
import com.omnidex.app.ui.UnlockScreen
import com.omnidex.app.ui.biometric.BiometricGate
import com.omnidex.app.ui.components.BottomPillNav
import com.omnidex.app.ui.components.NavItem
import com.omnidex.app.ui.screens.AddEditScreen
import com.omnidex.app.ui.screens.CanaryEventScreen
import com.omnidex.app.ui.screens.CategoriesScreen
import com.omnidex.app.ui.screens.CompositeDetailScreen
import com.omnidex.app.ui.screens.EntryDetailScreen
import com.omnidex.app.ui.screens.ExportScreen
import com.omnidex.app.ui.screens.HealthReportScreen
import com.omnidex.app.ui.screens.HistoryScreen
import com.omnidex.app.ui.screens.ImportScreen
import com.omnidex.app.ui.screens.MeshScreen
import com.omnidex.app.ui.screens.SettingsScreen
import com.omnidex.app.ui.screens.TransferScreen
import com.omnidex.app.ui.screens.VaultScreen
import com.omnidex.app.ui.theme.OmniDexTheme
import com.omnidex.app.ui.viewmodels.VaultViewModel

class MainActivity : FragmentActivity() {

    private val viewModel: VaultViewModel by viewModels()
    private var gate: BiometricGate? = null
    private var backgroundedAt = 0L
    private var biometricPrompted = false
    private var biometricTesting = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        makeSystemBarsTransparent()

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            LaunchedEffect(settings.allowScreenshots) {
                applyScreenSecurity(settings.allowScreenshots)
            }
            OmniDexTheme(settings) {
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

    private fun promptBiometric(settings: AppSettings, force: Boolean = false) {
        if (!settings.biometric && !force) return
        if (biometricPrompted && !force) return
        biometricPrompted = true
        biometricTesting = force
        val next = BiometricGate(
            activity = this,
            onUnlocked = { key ->
                viewModel.unlockWithBiometricKey(key)
                if (biometricTesting) toast("Biometric unlock works")
                biometricTesting = false
            },
            onError = { message ->
                biometricPrompted = false
                toast(if (biometricTesting) "Biometric test: $message" else message)
                biometricTesting = false
            }
        )
        gate?.shutdown()
        gate = next
        next.authenticate()
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
        gate?.shutdown()
        gate = null
        biometricPrompted = false
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
        // Back stack: every push keeps the page underneath, so a back gesture
        // always returns to where the user came from instead of resetting.
        val backStack = remember { mutableStateListOf<Screen>(Screen.Vault) }
        val screen = backStack.last()
        var goingBack by remember { mutableStateOf(false) }

        fun navigate(to: Screen) {
            if (to == screen) return
            goingBack = false
            backStack.add(to)
        }

        fun resetTo(to: Screen, animateBack: Boolean = false) {
            goingBack = animateBack
            backStack.clear()
            backStack.add(to)
        }

        fun goBack(): Boolean {
            if (backStack.size <= 1) return false
            goingBack = true
            backStack.removeAt(backStack.lastIndex)
            return true
        }

        /** Back to the previous page, falling back to [fallback] on a fresh stack. */
        fun backTo(fallback: Screen) {
            if (!goBack()) resetTo(fallback, animateBack = true)
        }
        var webUrl by remember { mutableStateOf<String?>(null) }
        var vaultQuery by remember { mutableStateOf("") }
        var confirmReset by remember { mutableStateOf(false) }
        var setupError by remember { mutableStateOf<String?>(null) }
        var unlockError by remember { mutableStateOf<String?>(null) }

        val entries by viewModel.entries.collectAsStateWithLifecycle()

        fun copyEntry(entry: VaultEntry) {
            if (entry.burnAfterRead && entry.isConsumed) {
                toast("This secret was consumed")
                return
            }
            val toCopy = if (entry.burnAfterRead && !entry.isConsumed) {
                val consumed = viewModel.consumeOnce(entry.id)
                viewModel.decrypt(consumed?.secret ?: entry.secret)
            } else {
                viewModel.decrypt(entry.secret)
            }
            viewModel.copyText(entry.id, toCopy, entry.title)
            toast(if (settings.clipboardClear) "Copied · clears in 45s" else "Copied")
        }

        fun handleOpen(url: String) {
            if (settings.showSourceInApp) webUrl = url else onOpenExternal(url)
        }

        BackHandler(enabled = webUrl != null || backStack.size > 1) {
            if (webUrl != null) webUrl = null else goBack()
        }

        LaunchedEffect(lockState) {
            if (lockState == VaultViewModel.LockState.LOCKED) {
                resetTo(Screen.Vault)
                webUrl = null
            }
        }

        when (lockState) {
            VaultViewModel.LockState.SETUP -> SetupScreen(
                onCreate = { password -> setupError = viewModel.setupVault(password) },
                error = setupError
            )

            VaultViewModel.LockState.LOCKED -> {
                val biometricReady = settings.biometric &&
                    remember(lockState) { SecurePrefs.getWrappedMasterKey(this@MainActivity) != null }
                LaunchedEffect(biometricReady) {
                    if (biometricReady) onRequestBiometric()
                }
                LifecycleEventEffect(Lifecycle.Event.ON_START) {
                    if (biometricReady) onRequestBiometric()
                }
                UnlockScreen(
                    error = unlockError,
                    biometricAvailable = biometricReady,
                    biometricNotice = if (settings.biometric && !biometricReady) {
                        "Unlock with your master password once to enable biometrics."
                    } else null,
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
                            is Screen.Settings, is Screen.Export, is Screen.Import, Screen.Health -> "settings"
                            else -> "vault"
                        },
                        onSelect = { item ->
                            resetTo(when (item.key) {
                                "vault" -> Screen.Vault
                                "add" -> Screen.Add(null)
                                "categories" -> Screen.Categories
                                else -> Screen.Settings
                            })
                        }
                    )
                }

                val projectionEntries by viewModel.projectionEntries.collectAsState()
                var showSessionSheet by remember { mutableStateOf(false) }

                val tabs = listOf(Screen.Vault, Screen.Categories, Screen.Settings)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(screen) {
                            // A sideways swipe anywhere that is not already a
                            // horizontal scroller flips between the main tabs.
                            val threshold = 90.dp.toPx()
                            var total = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { total = 0f },
                                onHorizontalDrag = { _, delta -> total += delta },
                                onDragEnd = {
                                    val index = tabs.indexOfFirst { it == screen }
                                    if (index >= 0 && kotlin.math.abs(total) > threshold) {
                                        val next = if (total < 0f) index + 1 else index - 1
                                        if (next in tabs.indices) {
                                            resetTo(tabs[next], animateBack = next < index)
                                        }
                                    }
                                    total = 0f
                                },
                                onDragCancel = { total = 0f }
                            )
                        }
                ) {
                AnimatedContent(
                    targetState = screen,
                    modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        // Push slides in from the right, back slides the other way.
                        val direction = if (goingBack) -1 else 1
                        (slideInHorizontally(tween(260)) { width -> width * direction } +
                            fadeIn(tween(180))) togetherWith
                            (slideOutHorizontally(tween(260)) { width -> -width * direction / 3 } +
                                fadeOut(tween(180)))
                    },
                    label = "screen"
                ) { current ->
                when (current) {
                    Screen.Vault -> VaultScreen(
                        entries = entries,
                        onOpenEntry = { navigate(Screen.Detail(it)) },
                        onAdd = { navigate(Screen.Add(null)) },
                        onToggleFavorite = viewModel::toggleFavorite,
                        onCopy = { entry -> copyEntry(entry) },
                        onDelete = { entry ->
                            viewModel.deleteEntry(entry.id)
                            toast("Deleted ${entry.title}")
                        },
                        initialQuery = vaultQuery,
                        bottomBar = bottomBar,
                        projectionEntries = projectionEntries,
                        onRefreshProjection = { viewModel.refreshProjection(this@MainActivity) },
                        onDismissProjection = { viewModel.dismissProjection() },
                        onOpenProjection = { entry -> navigate(Screen.Detail(entry.id)) },
                        showSessionSheet = showSessionSheet,
                        onShowSessionSheet = { showSessionSheet = it },
                        onStartSession = { scope, ttl, ids, target ->
                            val err = viewModel.startSession(scope, ttl, ids, target)
                            if (err != null) toast(err) else toast("Session started")
                        }
                    )

                    Screen.Categories -> CategoriesScreen(
                        entries = entries,
                        onOpenCategory = { category ->
                            vaultQuery = category
                            resetTo(Screen.Vault, animateBack = true)
                        },
                        onOpenProvider = { name ->
                            vaultQuery = name
                            resetTo(Screen.Vault, animateBack = true)
                        },
                        bottomBar = bottomBar
                    )

                    Screen.Settings -> SettingsScreen(
                        settings = settings,
                        entryCount = entries.size,
                        onUpdate = viewModel::updateSettings,
                        onExport = { navigate(Screen.Export) },
                        onImport = { navigate(Screen.Import) },
                        onReset = { confirmReset = true },
                        onClearTags = {
                            viewModel.updateSettings { it.copy(knownTags = emptyList()) }
                            toast("Tag history cleared")
                        },
                        onResetAppearance = {
                            viewModel.updateSettings {
                                it.copy(
                                    accent = com.omnidex.app.ui.theme.AccentPalette.BLUE.key,
                                    paletteStyle = com.omnidex.app.ui.theme.PaletteStyle.TONAL.key,
                                    surfaceShading = 1.4f,
                                    gradientBackground = true,
                                    progressiveBlur = true,
                                    matchIconColors = true,
                                    reduceEffects = false,
                                    cornerScale = 1.5f,
                                    uiScale = 0.9f,
                                    font = "system"
                                )
                            }
                            toast("Appearance reset")
                        },
                        onTestBiometrics = { promptBiometric(settings, force = true) },
                        onRepairBiometrics = { toast(viewModel.repairBiometric()) },
                        onOpenHealth = { navigate(Screen.Health) },
                        onOpenMesh = { navigate(Screen.Mesh) },
                        onOpenTransfer = { navigate(Screen.Transfer) },
                        onOpenCanary = { navigate(Screen.CanaryEvents) },
                        bottomBar = bottomBar
                    )

                    is Screen.Add -> AddEditScreen(
                        existing = null,
                        initialBrandId = current.brandId,
                        knownTags = settings.knownTags,
                        onSave = { entry, secret, extras ->
                            viewModel.saveEntry(entry, secret, extras)
                            resetTo(Screen.Vault, animateBack = true)
                            toast("Saved to vault")
                        },
                        onCancel = { backTo(Screen.Vault) }
                    )

                    is Screen.Edit -> {
                        val entry = entries.firstOrNull { it.id == current.id }
                        if (entry == null) {
                            LaunchedEffect(current.id) { resetTo(Screen.Vault, animateBack = true) }
                        } else {
                            AddEditScreen(
                                existing = entry,
                                knownTags = settings.knownTags,
                                existingExtras = remember(entry.id) { viewModel.extrasOf(entry) },
                                onSave = { updated, secret, extras ->
                                    viewModel.saveEntry(updated, secret, extras)
                                    if (!goBack()) resetTo(Screen.Detail(updated.id), animateBack = true)
                                    toast("Changes saved")
                                },
                                onCancel = { backTo(Screen.Detail(entry.id)) },
                                onDelete = {
                                    viewModel.deleteEntry(entry.id)
                                    backTo(Screen.Vault)
                                    toast("Deleted")
                                }
                            )
                        }
                    }

                    is Screen.Detail -> {
                        val entry = entries.firstOrNull { it.id == current.id }
                        if (entry == null) {
                            LaunchedEffect(current.id) { resetTo(Screen.Vault, animateBack = true) }
                        } else {
                            EntryDetailScreen(
                                entry = entry,
                                secretPlain = { viewModel.decrypt(entry.secret) },
                                onBack = { backTo(Screen.Vault) },
                                onEdit = { navigate(Screen.Edit(entry.id)) },
                                onCopy = { copyEntry(entry) },
                                onToggleFavorite = { viewModel.toggleFavorite(entry.id) },
                                onDelete = {
                                    viewModel.deleteEntry(entry.id)
                                    backTo(Screen.Vault)
                                    toast("Deleted")
                                },
                                onRegenerate = {
                                    val fresh = com.omnidex.app.crypto.CryptoManager.generateToken()
                                    viewModel.saveEntry(
                                        entry.copy(updatedAt = System.currentTimeMillis()),
                                        fresh,
                                        null
                                    )
                                    toast("New value generated · update it at the provider")
                                },
                                extras = remember(entry.id) { viewModel.extrasOf(entry) },
                                onOpenUrl = { handleOpen(it) }
                            )
                        }
                    }

                    Screen.Export -> {
                        val vaultJson = remember { viewModel.exportAll() }
                        ExportScreen(vaultJson = vaultJson, onBack = { backTo(Screen.Settings) })
                    }

                    Screen.Import -> ImportScreen(
                        onBack = { backTo(Screen.Settings) },
                        onImport = { payload, passphrase ->
                            val decrypted = ExportManager.import(payload.trim(), passphrase)
                            if (decrypted == null) "Wrong passphrase or damaged payload"
                            else viewModel.importFromJson(decrypted)
                        }
                    )

                    Screen.Health -> HealthReportScreen(
                        entries = entries,
                        onBack = { backTo(Screen.Settings) },
                        onEntryClick = { entry -> navigate(Screen.Detail(entry.id)) }
                    )

                    Screen.Mesh -> MeshScreen(
                        entries = entries,
                        onBack = { backTo(Screen.Vault) },
                        onEntryClick = { id -> navigate(Screen.Detail(id)) }
                    )

                    Screen.Transfer -> TransferScreen(
                        onBack = { backTo(Screen.Settings) }
                    )

                    Screen.CanaryEvents -> CanaryEventScreen(
                        onBack = { backTo(Screen.Settings) }
                    )

                    is Screen.History -> HistoryScreen(
                        entry = entries.firstOrNull { it.id == current.entryId },
                        onBack = { backTo(Screen.Vault) },
                        onRollback = { _ -> }
                    )

                    is Screen.Composite -> CompositeDetailScreen(
                        entry = entries.firstOrNull { it.id == current.entryId },
                        onBack = { backTo(Screen.Vault) },
                        onGenerate = { }
                    )

                    Screen.Policy -> HealthReportScreen(
                        entries = entries,
                        onBack = { backTo(Screen.Settings) },
                        onEntryClick = { entry -> navigate(Screen.Detail(entry.id)) }
                    )
                }
                }

                webUrl?.let { url ->
                    WebOverlay(url = url, onClose = { webUrl = null })
                }
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
                        resetTo(Screen.Vault)
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
    data object Health : Screen
    data object Mesh : Screen
    data object Policy : Screen
    data object Transfer : Screen
    data class History(val entryId: Long) : Screen
    data class Composite(val entryId: Long) : Screen
    data object CanaryEvents : Screen
    data class Add(val brandId: String?) : Screen
    data class Detail(val id: Long) : Screen
    data class Edit(val id: Long) : Screen
}

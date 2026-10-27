package com.ipafetch.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ipafetch.app.Session
import com.ipafetch.app.crypto.CryptoManager
import com.ipafetch.app.crypto.KeyStoreManager
import com.ipafetch.app.crypto.PasswordVerifier
import com.ipafetch.app.data.AppSettings
import com.ipafetch.app.data.SecurePrefs
import com.ipafetch.app.data.SettingsRepository
import com.ipafetch.app.data.VaultRepository
import com.ipafetch.app.domain.VaultEntry
import com.ipafetch.app.security.AuditLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(ExperimentalCoroutinesApi::class)
class VaultViewModel(app: Application) : AndroidViewModel(app) {

    private val appContext = app.applicationContext
    private val settingsRepo = SettingsRepository.get(appContext)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val settings: StateFlow<AppSettings> = settingsRepo.settings

    /** Setup → first run; Locked → vault exists but the key is not in memory. */
    enum class LockState { SETUP, LOCKED, UNLOCKED }

    private val _lockState = MutableStateFlow(
        if (SecurePrefs.isSetupDone(appContext)) LockState.LOCKED else LockState.SETUP
    )
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private val repository = MutableStateFlow<VaultRepository?>(null)

    val entries: StateFlow<List<VaultEntry>> = repository
        .flatMapLatest { repo -> repo?.entries ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // A process restart clears the session; make sure the state agrees.
        if (!Session.isUnlocked && _lockState.value == LockState.UNLOCKED) {
            _lockState.value = LockState.LOCKED
        }
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsRepo.update(transform)

    // ---------------------------------------------------------------- unlock

    fun setupVault(password: String): String? {
        if (password.length < 8) return "Use at least 8 characters"
        return runCatching {
            val salt = CryptoManager.generateSalt()
            val key = CryptoManager.deriveMasterKey(password.toCharArray(), salt)
            SecurePrefs.saveSalt(appContext, salt)
            SecurePrefs.saveVerifier(appContext, PasswordVerifier.create(key))
            runCatching { SecurePrefs.saveWrappedMasterKey(appContext, KeyStoreManager.wrapMasterKey(key)) }
            SecurePrefs.markSetupDone(appContext, true)
            attachSession(key)
            AuditLogger.log(appContext, "setup")
            null
        }.getOrElse { it.message ?: "Could not create the vault" }
    }

    /** Returns null on success, or a message to show. */
    fun unlockWithPassword(password: String): String? {
        val salt = SecurePrefs.getSalt(appContext) ?: return "Vault salt is missing — reset required"
        val key = CryptoManager.deriveMasterKey(password.toCharArray(), salt)
        val verifier = SecurePrefs.getVerifier(appContext)
        if (verifier == null) {
            return "This vault predates password checks. Unlock with biometrics once to enable it."
        }
        if (!PasswordVerifier.matches(key, verifier)) {
            AuditLogger.log(appContext, "unlock_failed")
            return "Incorrect master password"
        }
        healWrappedKey(key)
        attachSession(key)
        return null
    }

    /**
     * Older vaults (or a failed Keystore wrap at setup) have no biometric blob.
     * Re-wrap now that we hold the derived key again, so biometrics starts
     * working on the next launch instead of failing with a cipher error.
     */
    private fun healWrappedKey(key: ByteArray) {
        val existing = SecurePrefs.getWrappedMasterKey(appContext)
        if (existing != null && KeyStoreManager.canUnwrap(existing)) return
        // Missing, or sealed by a Keystore key we can no longer use (older
        // auth-bound alias): re-wrap now that the password proved the key.
        runCatching {
            SecurePrefs.saveWrappedMasterKey(appContext, KeyStoreManager.wrapMasterKey(key))
            KeyStoreManager.deleteLegacyKeys()
        }
    }

    fun unlockWithBiometricKey(key: ByteArray) {
        if (SecurePrefs.getVerifier(appContext) == null) {
            runCatching { SecurePrefs.saveVerifier(appContext, PasswordVerifier.create(key)) }
        }
        healWrappedKey(key)
        attachSession(key)
    }

    private fun attachSession(key: ByteArray) {
        val repo = VaultRepository(appContext, key)
        Session.set(key, repo)
        repository.value = repo
        _lockState.value = LockState.UNLOCKED
    }

    fun lock() {
        if (_lockState.value == LockState.UNLOCKED) {
            Session.clear()
            repository.value = null
            _lockState.value = LockState.LOCKED
        }
    }

    fun resetVault() {
        SecurePrefs.reset(appContext)
        Session.clear()
        repository.value = null
        _lockState.value = LockState.SETUP
        AuditLogger.log(appContext, "reset")
    }

    // ------------------------------------------------------------- entries

    private fun repo(): VaultRepository? = Session.repository

    fun decrypt(value: String): String = repo()?.decryptValue(value) ?: ""

    fun encrypt(value: String): String = repo()?.encryptValue(value) ?: ""

    fun entryById(id: Long): VaultEntry? = repo()?.getById(id)

    /** Decrypts the provider-specific extra fields for an entry. */
    fun extrasOf(entry: VaultEntry): Map<String, String> {
        if (entry.extrasBlob.isBlank()) return emptyMap()
        val r = repo() ?: return emptyMap()
        val plain = r.decryptValue(entry.extrasBlob).ifBlank { return emptyMap() }
        return runCatching { json.decodeFromString<Map<String, String>>(plain) }.getOrDefault(emptyMap())
    }

    fun saveEntry(
        entry: VaultEntry,
        plaintextSecret: String?,
        extras: Map<String, String>? = null
    ): VaultEntry? {
        val r = repo() ?: return null
        val secretBlob = when {
            !plaintextSecret.isNullOrBlank() -> r.encryptValue(plaintextSecret)
            else -> entry.secret
        }
        val extrasBlob = when {
            extras == null -> entry.extrasBlob
            extras.isEmpty() -> ""
            else -> r.encryptValue(json.encodeToString(extras))
        }
        val saved = r.upsert(entry.copy(secret = secretBlob, extrasBlob = extrasBlob))
        AuditLogger.log(appContext, if (entry.id == 0L) "entry_created" else "entry_updated", saved.title)
        // Remember tag vocabulary so future entries can reuse it in one tap.
        if (saved.tags.isNotEmpty()) {
            settingsRepo.update { current ->
                current.copy(knownTags = (saved.tags + current.knownTags).distinct().take(48))
            }
        }
        return saved
    }

    fun deleteEntry(id: Long) {
        val entry = repo()?.getById(id)
        if (entry != null && com.ipafetch.app.domain.CustomLogos.isCustom(entry.brandId)) {
            com.ipafetch.app.domain.CustomLogos.delete(appContext, entry.brandId)
        }
        repo()?.delete(id)
        AuditLogger.log(appContext, "entry_deleted", entry?.title ?: id.toString())
    }

    fun toggleFavorite(id: Long) {
        val r = repo() ?: return
        val entry = r.getById(id) ?: return
        r.setFavorite(id, !entry.favorite)
    }

    fun recordCopy(id: Long) {
        repo()?.recordCopy(id)
    }

    fun copyText(id: Long, plaintext: String, label: String) {
        val resolved = if (plaintext.isBlank()) decrypt(entryById(id)?.secret.orEmpty()) else plaintext
        com.ipafetch.app.security.ClipboardGuard.copy(
            context = appContext,
            label = label,
            value = resolved,
            clearAfterMs = if (settingsRepo.current.clipboardClear) 45_000L else Long.MAX_VALUE
        )
        recordCopy(id)
    }

    fun exportAll(): String = repo()?.exportJson() ?: "[]"

    fun importJson(entries: List<VaultEntry>) {
        repo()?.replaceAll(entries)
    }

    /** Decodes a decrypted export payload and replaces the vault. */
    fun importFromJson(vaultJson: String): String? {
        val r = repo() ?: return "Vault is locked"
        val decoded = runCatching {
            kotlinx.serialization.json.Json { ignoreUnknownKeys = true; encodeDefaults = true }
                .decodeFromString<List<VaultEntry>>(vaultJson)
        }.getOrElse { return "That payload is not a valid vault" }
        r.replaceAll(decoded)
        AuditLogger.log(appContext, "import", "${decoded.size} entries")
        return null
    }
}

package com.omnidex.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omnidex.app.Session
import com.omnidex.app.crypto.CryptoManager
import com.omnidex.app.crypto.KeyStoreManager
import com.omnidex.app.crypto.PasswordVerifier
import com.omnidex.app.data.AppSettings
import com.omnidex.app.data.SecurePrefs
import com.omnidex.app.data.SettingsRepository
import com.omnidex.app.data.VaultRepository
import com.omnidex.app.domain.CanaryAccessVector
import com.omnidex.app.domain.HistoryAction
import com.omnidex.app.domain.HistoryPoint
import com.omnidex.app.domain.SessionToken
import com.omnidex.app.domain.VaultEntry
import com.omnidex.app.security.AccessPolicyEngine
import com.omnidex.app.security.AuditLogger
import com.omnidex.app.security.CanaryLogger
import com.omnidex.app.security.ContextProjectionEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
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
import java.util.concurrent.TimeUnit

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

    // Context-Aware Vault Projection
    private val _projectionEntries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val projectionEntries: StateFlow<List<VaultEntry>> = _projectionEntries.asStateFlow()

    // Ephemeral Session Tokens
    private val _activeSessions = MutableStateFlow<List<SessionToken>>(emptyList())
    val activeSessions: StateFlow<List<SessionToken>> = _activeSessions.asStateFlow()

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

    /**
     * Re-seals the master key for biometrics using the in-memory session key, so
     * an existing vault does not need the password typed again. Returns a short
     * message for the settings row to show.
     */
    fun repairBiometric(): String {
        val key = Session.masterKey ?: return "Unlock with your password first"
        val wrapped = runCatching { KeyStoreManager.wrapMasterKey(key) }.getOrElse { t ->
            return "Keystore refused the key · ${t.javaClass.simpleName}" +
                (t.message?.let { ": $it" } ?: "")
        }
        SecurePrefs.saveWrappedMasterKey(appContext, wrapped)
        val stored = SecurePrefs.getWrappedMasterKey(appContext)
        return when {
            stored == null -> "Keystore produced a key but nothing was stored"
            KeyStoreManager.canUnwrap(stored) -> "Biometric unlock is set up now"
            else -> "Stored, but it cannot be decrypted yet" +
                (KeyStoreManager.lastError?.let { " · $it" } ?: "")
        }
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

    fun decrypt(value: String, entryId: Long? = null): String {
        if (entryId != null) {
            val entry = entryById(entryId)
            if (entry != null) {
                val policyDecision = AccessPolicyEngine.evaluate(entry, appContext, settingsRepo.current)
                if (!policyDecision.allow) {
                    AuditLogger.log(appContext, "policy_denied", "${entry.title}: ${policyDecision.reason}")
                    return ""
                }
                if (entry.isCanary && settingsRepo.current.canaryEnabled) {
                    CanaryLogger.log(CanaryAccessVector.DECRYPT, entry.id, mapOf("title" to entry.title), appContext)
                }
            }
        }
        return repo()?.decryptValue(value) ?: ""
    }

    fun encrypt(value: String): String = repo()?.encryptValue(value) ?: ""

    fun entryById(id: Long): VaultEntry? = repo()?.getById(id)

    fun consumeOnce(id: Long): VaultEntry? = repo()?.consumeOnce(id)

    fun getDerivedFor(parentId: Long): List<VaultEntry> = repo()?.getDerivedFor(parentId) ?: emptyList()

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
        var entryToSave = entry.copy(secret = secretBlob, extrasBlob = extrasBlob)
        
        // Time-Machine: snapshot history
        if (settingsRepo.current.timeMachineEnabled) {
            val existing = if (entry.id != 0L) r.getById(entry.id) else null
            if (existing != null) {
                if (existing.secret != secretBlob || existing.extrasBlob != extrasBlob) {
                    val snapshot = HistoryPoint(
                        timestamp = System.currentTimeMillis(),
                        secretBlob = existing.secret,
                        extrasBlob = existing.extrasBlob,
                        label = null,
                        action = HistoryAction.UPDATE
                    )
                    val newHistory = (listOf(snapshot) + existing.history).take(settingsRepo.current.timeMachineMaxHistory)
                    entryToSave = entryToSave.copy(history = newHistory)
                }
            } else if (entry.id == 0L) {
                entryToSave = entryToSave.copy(history = listOf(HistoryPoint(action = HistoryAction.CREATE)))
            }
        }
        
        val saved = r.upsert(entryToSave)
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
        if (entry != null && com.omnidex.app.domain.CustomLogos.isCustom(entry.brandId)) {
            com.omnidex.app.domain.CustomLogos.delete(appContext, entry.brandId)
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
        val entry = entryById(id)
        if (entry != null) {
            val policyDecision = AccessPolicyEngine.evaluate(entry, appContext, settingsRepo.current)
            if (!policyDecision.allow) {
                AuditLogger.log(appContext, "policy_denied", "${entry.title}: ${policyDecision.reason}")
                return
            }
            if (entry.isCanary && settingsRepo.current.canaryEnabled) {
                CanaryLogger.log(CanaryAccessVector.UI_COPY, entry.id, mapOf("title" to entry.title), appContext)
            }
        }
        val resolved = if (plaintext.isBlank()) decrypt(entry?.secret.orEmpty(), id) else plaintext
        com.omnidex.app.security.ClipboardGuard.copy(
            context = appContext,
            label = label,
            value = resolved,
            clearAfterMs = if (settingsRepo.current.clipboardClear) 45_000L else Long.MAX_VALUE
        )
        recordCopy(id)
    }

    fun exportAll(): String {
        val data = repo()?.exportJson() ?: "[]"
        if (settingsRepo.current.canaryEnabled) {
            val entries = repo()?.all ?: emptyList()
            val canaries = entries.count { it.isCanary }
            if (canaries > 0) {
                CanaryLogger.log(CanaryAccessVector.EXPORT, 0, mapOf("count" to canaries.toString()), appContext)
            }
        }
        return data
    }

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
        if (settingsRepo.current.canaryEnabled) {
            val canaries = decoded.count { it.isCanary }
            if (canaries > 0) {
                CanaryLogger.log(CanaryAccessVector.IMPORT, 0, mapOf("count" to canaries.toString()), appContext)
            }
        }
        r.replaceAll(decoded)
        AuditLogger.log(appContext, "import", "${decoded.size} entries")
        return null
    }

    // Context Projection
    fun refreshProjection(context: android.content.Context) {
        val s = settingsRepo.current
        if (!s.contextProjectionEnabled) {
            _projectionEntries.value = emptyList()
            return
        }
        if (_lockState.value != LockState.UNLOCKED) {
            _projectionEntries.value = emptyList()
            return
        }
        val foreground = ContextProjectionEngine.getForegroundPackage(context)
        val mimes = ContextProjectionEngine.getClipboardMimeTypes(context)
        val scored = ContextProjectionEngine.scoreEntries(entries.value, foreground, mimes)
        val top = scored.take(s.contextProjectionMaxCount.coerceIn(1, 5)).map { it.first }
        _projectionEntries.value = top
    }

    fun dismissProjection() {
        _projectionEntries.value = emptyList()
    }

    // Ephemeral Sessions
    fun startSession(scopeLabel: String, ttlMinutes: Int, entryIds: List<Long>, targetPackage: String?): String? {
        val s = settingsRepo.current
        if (!s.sessionInjectionEnabled) return "Session injection disabled"
        if (_lockState.value != LockState.UNLOCKED) return "Vault locked"
        if (entryIds.size > s.sessionMaxEntries) return "Too many entries (max ${s.sessionMaxEntries})"
        if (ttlMinutes > s.sessionMaxTtlMinutes) return "TTL too long (max ${s.sessionMaxTtlMinutes} min)"
        if (_activeSessions.value.size >= s.sessionMaxConcurrent) return "Too many concurrent sessions"
        val masterKey = Session.masterKey ?: return "No session key"
        
        // Check for canaries
        if (s.canaryEnabled) {
            val r = repo()
            val canaries = entryIds.mapNotNull { r?.getById(it) }.count { it.isCanary }
            if (canaries > 0) {
                CanaryLogger.log(CanaryAccessVector.SESSION_INJECTION, 0, mapOf("count" to canaries.toString()), appContext)
            }
        }
        
        val ttlMs = TimeUnit.MINUTES.toMillis(ttlMinutes.toLong())
        val expiresAt = System.currentTimeMillis() + ttlMs
        val minuteBucket = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis())
        val sessionKey = CryptoManager.deriveSessionKey(masterKey, scopeLabel, minuteBucket)
        
        val token = SessionToken(
            scopeLabel = scopeLabel,
            targetPackage = targetPackage,
            entryIds = entryIds,
            expiresAt = expiresAt,
            sessionKey = sessionKey
        )
        _activeSessions.value = _activeSessions.value + token
        AuditLogger.log(appContext, "session_started", scopeLabel)
        
        viewModelScope.launch {
            delay(ttlMs)
            endSession(token.id)
        }
        return null
    }

    fun endSession(sessionId: String) {
        val sessions = _activeSessions.value
        val session = sessions.find { it.id == sessionId }
        session?.wipe()
        _activeSessions.value = sessions.filter { it.id != sessionId }
        AuditLogger.log(appContext, "session_ended", session?.scopeLabel ?: sessionId)
    }

    fun injectSession(sessionId: String) {
        val session = _activeSessions.value.find { it.id == sessionId } ?: return
        if (session.isExpired()) {
            endSession(sessionId)
            return
        }
        val r = repo() ?: return
        session.entryIds.forEach { id ->
            val entry = r.getById(id) ?: return@forEach
            val policyDecision = AccessPolicyEngine.evaluate(entry, appContext, settingsRepo.current)
            if (!policyDecision.allow) {
                AuditLogger.log(appContext, "policy_denied", "${entry.title}: ${policyDecision.reason}")
                return@forEach
            }
            if (entry.isCanary && settingsRepo.current.canaryEnabled) {
                CanaryLogger.log(CanaryAccessVector.SESSION_INJECTION, entry.id, mapOf("title" to entry.title), appContext)
            }
            val decrypted = decrypt(entry.secret, id)
            if (decrypted.isNotBlank()) {
                com.omnidex.app.security.ClipboardGuard.copy(
                    context = appContext,
                    label = "Session:${session.scopeLabel}:${entry.title}",
                    value = decrypted,
                    clearAfterMs = 5000L
                )
            }
        }
        AuditLogger.log(appContext, "session_injected", session.scopeLabel)
    }
    
    // Time-Machine
    fun rollbackToHistory(entryId: Long, historyPoint: HistoryPoint): Boolean {
        if (!settingsRepo.current.timeMachineEnabled) return false
        val r = repo() ?: return false
        val entry = r.getById(entryId) ?: return false
        val rolledBack = entry.copy(
            secret = historyPoint.secretBlob,
            extrasBlob = historyPoint.extrasBlob,
            history = (listOf(HistoryPoint(
                timestamp = System.currentTimeMillis(),
                secretBlob = entry.secret,
                extrasBlob = entry.extrasBlob,
                label = "Rollback from ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date())}",
                action = HistoryAction.ROLLBACK
            )) + entry.history).take(settingsRepo.current.timeMachineMaxHistory)
        )
        r.upsert(rolledBack)
        AuditLogger.log(appContext, "history_rollback", entry.title)
        return true
    }
    
    // Canary
    fun createCanary(type: com.omnidex.app.domain.EntryType, label: String): VaultEntry? {
        if (!settingsRepo.current.canaryEnabled) return null
        val canary = com.omnidex.app.security.CanaryFactory.generate(type, label)
        return saveEntry(canary, canary.secret)
    }
    
    // Policy
    fun evaluatePolicy(entry: VaultEntry): AccessPolicyEngine.AccessDecision {
        return AccessPolicyEngine.evaluate(entry, appContext, settingsRepo.current)
    }
    
    fun clearPolicyCache() {
        AccessPolicyEngine.clearCache()
    }
}

package com.ipafetch.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ipafetch.app.Session
import com.ipafetch.app.data.AppDatabase
import com.ipafetch.app.data.KeyRepository
import com.ipafetch.app.data.SecurePrefs
import com.ipafetch.app.crypto.CryptoManager
import com.ipafetch.app.crypto.ExportManager
import com.ipafetch.app.crypto.KeyStoreManager
import com.ipafetch.app.domain.KeyEntry
import com.ipafetch.app.security.AuditLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MainViewModel @Inject constructor(
    private val app: Application,
    private val db: AppDatabase
) : AndroidViewModel(app) {

    private val _state = MutableStateFlow<State>(State.Locked)
    val state: StateFlow<State> = _state.asStateFlow()

    sealed class State {
        object Locked : State()
        object Unlocked : State()
        object Setup : State()
        data class Error(val message: String) : State()
    }

    fun isSetupDone(): Boolean = SecurePrefs.isSetupDone(app)

    /** Set state to Locked so the biometric gate prompts. */
    fun requestUnlock() {
        _state.value = State.Locked
    }

    fun setupMasterKey(password: CharArray) {
        val salt = CryptoManager.generateSalt()
        val derived = CryptoManager.deriveMasterKey(password, salt)
        val wrapped = KeyStoreManager.wrapMasterKey(derived)
        SecurePrefs.saveSalt(app, salt)
        SecurePrefs.saveWrappedMasterKey(app, wrapped)
        SecurePrefs.markSetupDone(app, true)
        val repo = KeyRepository(db, derived)
        Session.set(derived, repo)
        _state.value = State.Unlocked
    }

    fun onMasterKeyReady(key: ByteArray) {
        val repo = KeyRepository(db, key)
        Session.set(key, repo)
        _state.value = State.Unlocked
    }

    fun getRepository(): KeyRepository =
        Session.repository ?: throw IllegalStateException("Vault not unlocked")

    fun decrypt(value: String): String =
        Session.repository?.decryptValue(value) ?: throw IllegalStateException("Vault not unlocked")

    fun encrypt(value: String): String =
        Session.repository?.encryptValue(value) ?: throw IllegalStateException("Vault not unlocked")

    fun reset() {
        SecurePrefs.reset(app)
        Session.clear()
        _state.value = State.Setup
        AuditLogger.log(app, AuditLogger.EVENT_RESET)
    }

    /** Export all entries as an encrypted JSON blob using the given passphrase. */
    suspend fun exportAll(exportPassphrase: CharArray): String {
        val repo = Session.repository ?: throw IllegalStateException("Vault not unlocked")
        val entries = repo.getAll().first()
        return ExportManager.export(entries, exportPassphrase) { repo.decryptValue(it.encryptedValue) }
    }

    /** Import entries from an encrypted JSON blob using the given passphrase. */
    suspend fun importAll(blob: String, exportPassphrase: CharArray) {
        val repo = Session.repository ?: throw IllegalStateException("Vault not unlocked")
        val imported = ExportManager.import(blob, exportPassphrase) { repo.encryptValue(it) }
        for (entry in imported) {
            repo.upsert(entry)
        }
        AuditLogger.log(app, AuditLogger.EVENT_IMPORT, "${imported.size} entries")
    }
}

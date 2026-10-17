package com.ipafetch.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ipafetch.app.Session
import com.ipafetch.app.crypto.CryptoManager
import com.ipafetch.app.crypto.KeyStoreManager
import com.ipafetch.app.data.KeyRepository
import com.ipafetch.app.data.SecurePrefs
import com.ipafetch.app.data.VaultStore
import com.ipafetch.app.domain.KeyEntry
import com.ipafetch.app.security.AuditLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel @constructor(
    private val app: Application
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

    fun setupMasterKey(password: CharArray) {
        val salt = CryptoManager.generateSalt()
        val derived = CryptoManager.deriveMasterKey(password, salt)
        val wrapped = KeyStoreManager.wrapMasterKey(derived)
        SecurePrefs.saveSalt(app, salt)
        SecurePrefs.saveWrappedMasterKey(app, wrapped)
        SecurePrefs.markSetupDone(app, true)
        Session.set(derived, KeyRepository(app, derived))
        _state.value = State.Unlocked
    }

    fun onMasterKeyReady(key: ByteArray) {
        Session.set(key, KeyRepository(app, key))
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
}

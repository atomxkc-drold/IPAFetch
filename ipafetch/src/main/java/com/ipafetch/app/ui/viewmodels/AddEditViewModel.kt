package com.ipafetch.app.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ipafetch.app.Session
import com.ipafetch.app.data.AppDatabase
import com.ipafetch.app.domain.KeyEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AddEditViewModel @Inject constructor(
    private val app: Application,
    private val db: AppDatabase
) : AndroidViewModel(app) {

    private val _entry = MutableStateFlow<KeyEntry?>(null)
    val entry: StateFlow<KeyEntry?> = _entry.asStateFlow()

    fun loadEntry(id: Long) {
        if (id == 0L) {
            _entry.value = null
            return
        }
        viewModelScope.launch {
            val repo = Session.repository ?: return@launch
            _entry.value = repo.getById(id)
        }
    }

    /**
     * Persist a new or updated entry. The plaintext secret is encrypted
     * with the session master key before touching the database.
     */
    fun save(entry: KeyEntry, plaintextSecret: String?) {
        val repo = Session.repository ?: return
        viewModelScope.launch {
            // Encrypt when there's a new/changed secret; otherwise keep the
            // existing ciphertext (edits that don't touch the secret field).
            val valueToStore = if (plaintextSecret?.isNotBlank() == true) {
                repo.encrypt(plaintextSecret)
            } else {
                entry.encryptedValue
            }
            repo.upsert(entry.copy(encryptedValue = valueToStore))
        }
    }
}

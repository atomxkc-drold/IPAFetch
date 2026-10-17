package com.ipafetch.app.ui.viewmodels

import android.app.Application
import com.ipafetch.app.Session
import com.ipafetch.app.domain.KeyEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AddEditViewModel @constructor(
    private val app: Application
) : androidx.lifecycle.AndroidViewModel(app) {

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

    fun save(entry: KeyEntry, plaintextSecret: String?) {
        val repo = Session.repository ?: return
        viewModelScope.launch {
            val valueToStore = if (plaintextSecret?.isNotBlank() == true) {
                repo.encrypt(plaintextSecret)
            } else {
                entry.encryptedValue
            }
            repo.upsert(entry.copy(encryptedValue = valueToStore))
        }
    }
}

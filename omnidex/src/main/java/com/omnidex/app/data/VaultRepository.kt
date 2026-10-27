package com.omnidex.app.data

import android.content.Context
import com.omnidex.app.crypto.CryptoManager
import com.omnidex.app.domain.LegacyKeyEntry
import com.omnidex.app.domain.VaultEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Single source of truth for vault entries.
 *
 * Storage: one JSON list, encrypted by EncryptedSharedPreferences, written back
 * on every mutation. Each entry's secret is additionally AES-256-GCM encrypted
 * with the session master key, so the blob is double-wrapped.
 */
class VaultRepository(
    private val context: Context,
    private val masterKey: ByteArray
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    private val _entries = MutableStateFlow<List<VaultEntry>>(emptyList())
    val entries: StateFlow<List<VaultEntry>> = _entries.asStateFlow()

    init {
        reload()
    }

    /** Re-read the encrypted blob (also migrates a pre-rebuild vault). */
    fun reload() {
        val blob = SecurePrefs.loadVault(context)
        if (blob.isNullOrBlank()) {
            _entries.value = emptyList()
            return
        }
        val updated = runCatching { json.decodeFromString<List<VaultEntry>>(blob) }.getOrNull()
        if (updated != null) {
            _entries.value = updated.sortedByDescending { it.updatedAt }
            return
        }
        val legacy = runCatching { json.decodeFromString<List<LegacyKeyEntry>>(blob) }.getOrNull()
        val migrated = legacy?.map { it.toEntry() }.orEmpty()
        _entries.value = migrated
        if (migrated.isNotEmpty()) persist(migrated)
    }

    fun getById(id: Long): VaultEntry? = _entries.value.firstOrNull { it.id == id }

    val all: List<VaultEntry> get() = _entries.value

    fun encryptValue(plaintext: String): String =
        if (plaintext.isEmpty()) "" else CryptoManager.encrypt(plaintext, masterKey)

    fun decryptValue(blobB64: String): String {
        if (blobB64.isEmpty()) return ""
        return runCatching { CryptoManager.decrypt(blobB64, masterKey) }.getOrDefault("")
    }

    /** Alias kept for readability at call sites. */
    fun encrypt(plaintext: String): String = encryptValue(plaintext)

    fun decrypt(blob: String): String = decryptValue(blob)

    fun upsert(entry: VaultEntry): VaultEntry {
        val now = System.currentTimeMillis()
        val current = _entries.value
        val existing = current.firstOrNull { it.id == entry.id }
        val toSave = when {
            existing != null -> entry.copy(createdAt = existing.createdAt, updatedAt = now)
            entry.id != 0L -> entry.copy(updatedAt = now)
            else -> entry.copy(id = nextId(current), createdAt = now, updatedAt = now)
        }
        val next = current.filterNot { it.id == toSave.id } + toSave
        persist(next)
        return toSave
    }

    fun delete(id: Long) {
        persist(_entries.value.filterNot { it.id == id })
    }

    fun setFavorite(id: Long, favorite: Boolean) {
        val entry = getById(id) ?: return
        upsert(entry.copy(favorite = favorite, updatedAt = System.currentTimeMillis()))
    }

    fun recordCopy(id: Long) {
        val entry = getById(id) ?: return
        persist(
            _entries.value.map {
                if (it.id == id) it.copy(lastCopiedAt = System.currentTimeMillis(), copyCount = it.copyCount + 1)
                else it
            }
        )
    }

    fun replaceAll(entries: List<VaultEntry>) {
        persist(entries)
    }

    private fun nextId(current: List<VaultEntry>): Long =
        (current.maxOfOrNull { it.id } ?: 0L) + 1L

    private fun persist(entries: List<VaultEntry>) {
        val ordered = entries.sortedWith(
            compareByDescending<VaultEntry> { it.favorite }.thenByDescending { it.updatedAt }
        )
        _entries.value = ordered
        SecurePrefs.saveVault(context, json.encodeToString(ordered))
    }

    fun exportJson(): String = json.encodeToString(_entries.value)
}

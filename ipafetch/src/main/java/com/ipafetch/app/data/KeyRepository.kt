package com.ipafetch.app.data

import android.content.Context
import com.ipafetch.app.crypto.CryptoManager
import com.ipafetch.app.domain.KeyEntry
import kotlinx.serialization.json.Json

/**
 * Single source of truth for keys, backed by VaultStore (encrypted JSON blob).
 *
 * Encryption model:
 *   - Master password -> PBKDF2-HMAC-SHA256 -> master key.
 *   - Master key wrapped inside Android Keystore (biometric/PIN-gated).
 *   - Each entry's plaintext encrypted with AES-256-GCM using master key.
 *   - All entries serialized to JSON, stored as one encrypted blob in
 *     EncryptedSharedPreferences. Nothing plaintext on disk.
 */
class KeyRepository(
    private val context: Context,
    private val masterKey: ByteArray
) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    /** All entries, decrypted from the vault blob. */
    fun getAll(): List<KeyEntry> {
        val blob = VaultStore.loadVault(context) ?: return emptyList()
        return try {
            json.decodeFromString<List<KeyEntry>>(blob)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Save all entries to the vault blob (encrypted). */
    fun saveAll(entries: List<KeyEntry>) {
        val blob = json.encodeToString(entries)
        VaultStore.saveVault(context, blob)
    }

    /** Encrypt a plaintext API key before persistence. */
    fun encryptValue(plaintext: String): String =
        CryptoManager.encrypt(plaintext, masterKey)

    /** Decrypt a stored API key back to plaintext. */
    fun decryptValue(blobB64: String): String =
        CryptoManager.decrypt(blobB64, masterKey)

    /** Persist a single entry (merge into existing list). */
    fun upsert(entry: KeyEntry) {
        val entries = getAll()
        val idx = entries.indexOfFirst { it.id == entry.id }
        val updated = if (idx >= 0) entries.toMutableList().apply { set(idx, entry) }
                      else entries.toMutableList().apply { add(entry) }
        saveAll(updated)
    }

    fun deleteById(id: Long) {
        val entries = getAll().filter { it.id != id }
        saveAll(entries)
    }
}

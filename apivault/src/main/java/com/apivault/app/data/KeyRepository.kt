package com.apivault.app.data

import android.util.Base64
import com.apivault.app.crypto.CryptoManager
import com.apivault.app.crypto.KeyStoreManager
import com.apivault.app.domain.KeyEntry
import com.apivault.app.domain.KeyEntryEntity
import com.apivault.app.domain.toDomain
import com.apivault.app.domain.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.crypto.Cipher
import javax.crypto.SecretKey

/**
 * Single source of truth for keys.
 *
 * Encryption model:
 *   - On first unlock, master key is derived from password via Argon2id and
 *     wrapped inside Android Keystore (biometric/PIN required to unwrap).
 *   - Each entry's plaintext is encrypted with AES-256-GCM using the master key.
 *   - The DB stores salt+iv+ciphertext+tag per field; nothing is plaintext.
 * - Search runs on ciphertext metadata only — no plaintext leakage.
 */
class KeyRepository(
    private val db: AppDatabase,
    private val masterKey: ByteArray
) {
    private val dao = db.keyDao()

    fun getAll(): Flow<List<KeyEntry>> = dao.getAll().map { it.map(KeyEntryEntity::toDomain) }

    fun search(query: String): Flow<List<KeyEntry>> = dao.search(query).map { it.map(KeyEntryEntity::toDomain) }

    fun byCategory(category: String): Flow<List<KeyEntry>> = dao.byCategory(category).map { it.map(KeyEntryEntity::toDomain) }

    fun expiringSoon(now: Long): Flow<List<KeyEntry>> = dao.expiringSoon(now).map { it.map(KeyEntryEntity::toDomain) }

    suspend fun getById(id: Long): KeyEntry? = dao.getById(id)?.toDomain()

    suspend fun upsert(entry: KeyEntry): Long {
        val entity = entry.toEntity()
        val id = dao.upsert(entity)
        // Room returns the rowId; -1 means no row was inserted. Fall back to the
        // entity's id so callers can use the returned value either way.
        return if (id <= 0L) entity.id else id
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    /** Encrypt a plaintext API key before persistence. */
    fun encryptValue(plaintext: String): String =
        CryptoManager.encrypt(plaintext, masterKey)

    /** Decrypt a stored API key back to plaintext. */
    fun decryptValue(blobB64: String): String =
        CryptoManager.decrypt(blobB64, masterKey)
}

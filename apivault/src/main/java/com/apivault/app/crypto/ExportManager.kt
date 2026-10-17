package com.apivault.app.crypto

import android.util.Base64
import com.apivault.app.domain.KeyEntry
import com.apivault.app.data.SecurePrefs
import com.apivault.app.security.AuditLogger
import com.apivault.app.crypto.CryptoManager
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Encrypted JSON export/import, OpenSSL-compatible.
 *
 * Format (JSON):
 *   {
 *     "v": 1,
 *     "salt": "<base64>",
 *     "iv": "<base64>",
 *     "data": "<base64 AES-256-GCM ciphertext>"
 *   }
 *
 * The export password is a user-chosen passphrase, NOT the master key. It is
 * run through Argon2id with a random salt stored in the file. This lets users
 * back up to a file without re-entering their master password, and share the
 * password separately (out-of-band).
 */
object ExportManager {

    private const val GCM_IV_LENGTH = 12
    private const val AES_KEY_LENGTH = 32
    private const val ARGON2_ITERATIONS = 3
    private const val ARGON2_MEMORY_KB = 131072   // 128 MiB
    private const val ARGON2_PARALLELISM = 4
    private const val ARGON2_HASH_LENGTH = 32

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    @kotlinx.serialization.Serializable
    data class ExportPayload(
        val v: Int = 1,
        val salt: String,
        val iv: String,
        val data: String
    )

    @kotlinx.serialization.Serializable
    data class EntryRecord(
        val name: String,
        val service: String,
        val category: String,
        val username: String,
        val endpoint: String,
        val notes: String,
        val createdAt: Long,
        val updatedAt: Long,
        val expiresAt: Long?,
        val keyLength: Int,
        val prefix: String,
        val rotatedAt: Long?,
        val value: String
    )

    fun export(
        context: android.content.Context,
        entries: List<KeyEntry>,
        exportPassphrase: CharArray,
        decrypt: (KeyEntry) -> String
    ): String {
        val salt = Random.nextBytes(16)
        val iv = Random.nextBytes(GCM_IV_LENGTH)
        val key = CryptoManager.deriveMasterKey(
            exportPassphrase, salt,
            iterations = ARGON2_ITERATIONS,
            memoryKb = ARGON2_MEMORY_KB,
            parallelism = ARGON2_PARALLELISM,
            hashLength = ARGON2_HASH_LENGTH
        )
        val records = entries.map { e ->
            EntryRecord(
                name = e.name, service = e.service, category = e.category,
                username = e.username, endpoint = e.endpoint, notes = e.notes,
                createdAt = e.createdAt, updatedAt = e.updatedAt, expiresAt = e.expiresAt,
                keyLength = e.keyLength, prefix = e.prefix, rotatedAt = e.rotatedAt,
                value = decrypt(e)
            )
        }
        val plaintext = json.encodeToString(EntryRecord.serializer(), records)
            .toByteArray(StandardCharsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext)

        val payload = ExportPayload(
            salt = Base64.encodeToString(salt, Base64.NO_WRAP),
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            data = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        )
        AuditLogger.log(context, AuditLogger.EVENT_EXPORT, "${entries.size} entries")
        return json.encodeToString(ExportPayload.serializer(), payload)
    }

    fun import(
        context: android.content.Context,
        blob: String,
        exportPassphrase: CharArray,
        encrypt: (String) -> String
    ): List<KeyEntry> {
        val payload = json.decodeFromString<ExportPayload>(blob)
        val salt = Base64.decode(payload.salt, Base64.NO_WRAP)
        val iv = Base64.decode(payload.iv, Base64.NO_WRAP)
        val ciphertext = Base64.decode(payload.data, Base64.NO_WRAP)
        val key = CryptoManager.deriveMasterKey(
            exportPassphrase, salt,
            iterations = ARGON2_ITERATIONS,
            memoryKb = ARGON2_MEMORY_KB,
            parallelism = ARGON2_PARALLELISM,
            hashLength = ARGON2_HASH_LENGTH
        )
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val plaintext = cipher.doFinal(ciphertext)
        val records = json.decodeFromString<List<EntryRecord>>(
            String(plaintext, StandardCharsets.UTF_8)
        )
        AuditLogger.log(context, AuditLogger.EVENT_IMPORT, "${records.size} entries")
        return records.map { r ->
            KeyEntry(
                name = r.name, service = r.service, category = r.category,
                username = r.username, endpoint = r.endpoint, notes = r.notes,
                createdAt = r.createdAt, updatedAt = r.updatedAt, expiresAt = r.expiresAt,
                keyLength = r.keyLength, prefix = r.prefix, rotatedAt = r.rotatedAt,
                encryptedValue = encrypt(r.value)
            )
        }
    }
}

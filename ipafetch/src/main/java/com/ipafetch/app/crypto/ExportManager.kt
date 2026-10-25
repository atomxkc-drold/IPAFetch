package com.ipafetch.app.crypto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Passphrase-protected export of the whole vault.
 *
 * Envelope: { v, salt, iv, data } where data is AES-256-GCM over the vault JSON
 * and the key is PBKDF2-HMAC-SHA256(passphrase, salt, 150k). The export
 * passphrase is independent of the master password so a backup can be shared
 * out-of-band without revealing either.
 */
object ExportManager {

    private const val VERSION = 2
    private const val ITERATIONS = 150_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    data class Envelope(
        val v: Int = VERSION,
        val salt: String,
        val iv: String,
        val data: String
    )

    fun export(vaultJson: String, passphrase: String): String {
        require(passphrase.length >= 8) { "Use at least 8 characters" }
        val salt = SecureRandom().generateSeed(SALT_BYTES)
        val iv = SecureRandom().generateSeed(IV_BYTES)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        val ciphertext = cipher.doFinal(vaultJson.toByteArray(Charsets.UTF_8))
        return json.encodeToString(
            Envelope(
                salt = Base64.getEncoder().encodeToString(salt),
                iv = Base64.getEncoder().encodeToString(iv),
                data = Base64.getEncoder().encodeToString(ciphertext)
            )
        )
    }

    /** Returns the vault JSON, or null when the passphrase is wrong / file is corrupt. */
    fun import(envelopeJson: String, passphrase: String): String? = runCatching {
        val envelope = json.decodeFromString<Envelope>(envelopeJson)
        val salt = Base64.getDecoder().decode(envelope.salt)
        val iv = Base64.getDecoder().decode(envelope.iv)
        val data = Base64.getDecoder().decode(envelope.data)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        String(cipher.doFinal(data), Charsets.UTF_8)
    }.getOrNull()

    private fun deriveKey(passphrase: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            javax.crypto.SecretKeyFactory
                .getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}

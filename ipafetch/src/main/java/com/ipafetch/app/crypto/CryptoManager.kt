package com.ipafetch.app.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Privacy core of ApiVault.
 *
 * Master password -> PBKDF2-HMAC-SHA256 (100k iterations) -> master key ->
 * per-entry AES-256-GCM blobs. Master key never touches disk in plaintext; it
 * is wrapped by Android Keystore.
 */
object CryptoManager {

    private const val PBKDF2_ITERATIONS = 100_000
    private const val PBKDF2_KEY_LENGTH = 256
    private const val PBKDF2_SALT_LENGTH = 16

    // AES-256-GCM
    private const val GCM_IV_LENGTH = 12          // bytes
    private const val GCM_TAG_LENGTH = 128        // bits

    /**
     * Derive a 256-bit master key from a password + salt using PBKDF2-HMAC-SHA256.
     */
    fun deriveMasterKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = PBKDF2_ITERATIONS
    ): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, PBKDF2_KEY_LENGTH)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return try { factory.generateSecret(spec).encoded } finally { factory.close() }
    }

    /** Generate a cryptographically random salt for key derivation. */
    fun generateSalt(): ByteArray = SecureRandom().generateSeed(PBKDF2_SALT_LENGTH)

    /**
     * Encrypt a plaintext value (e.g. an API key) using AES-256-GCM.
     * Returns: salt (16) + iv (12) + ciphertext+tag, base64-encoded.
     * The salt is stored alongside so re-encryption with the same master key works.
     */
    fun encrypt(plaintext: String, masterKey: ByteArray): String {
        val salt = SecureRandom().generateSeed(PBKDF2_SALT_LENGTH)
        val iv = SecureRandom().generateSeed(GCM_IV_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(masterKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(charset("UTF-8")))
        val blob = salt + iv + ciphertext
        return Base64.getEncoder().encodeToString(blob)
    }

    /**
     * Decrypt a value produced by [encrypt].
     * Expects base64(salt + iv + ciphertext+tag).
     */
    fun decrypt(blobB64: String, masterKey: ByteArray): String {
        val blob = Base64.getDecoder().decode(blobB64)
        val iv = blob.copyOfRange(PBKDF2_SALT_LENGTH, PBKDF2_SALT_LENGTH + GCM_IV_LENGTH)
        val ciphertext = blob.copyOfRange(PBKDF2_SALT_LENGTH + GCM_IV_LENGTH, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(masterKey, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, charset("UTF-8"))
    }

    /** Generate a random API token. */
    fun generateToken(length: Int = 40, prefix: String = ""): String {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
        val sb = StringBuilder(prefix)
        repeat(length) {
            sb.append(alphabet[SecureRandom().nextInt(alphabet.length)])
        }
        return sb.toString()
    }
}

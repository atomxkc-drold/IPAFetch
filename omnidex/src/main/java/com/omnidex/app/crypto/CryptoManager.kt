package com.omnidex.app.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
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
        return factory.generateSecret(spec).encoded
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

    /**
     * HKDF-SHA256 to derive a session key from master key.
     * Uses minute bucket for forward secrecy.
     */
    fun deriveSessionKey(masterKey: ByteArray, scopeLabel: String, minuteBucket: Long): ByteArray {
        val salt = ("devstash-session:$scopeLabel").toByteArray()
        val info = "hkdf-session-v1:$scopeLabel:$minuteBucket".toByteArray()
        // HKDF extract
        val macExtract = Mac.getInstance("HmacSHA256")
        macExtract.init(SecretKeySpec(salt, "HmacSHA256"))
        val prk = macExtract.doFinal(masterKey)
        // HKDF expand
        val macExpand = Mac.getInstance("HmacSHA256")
        macExpand.init(SecretKeySpec(prk, "HmacSHA256"))
        val t1 = macExpand.doFinal(info + byteArrayOf(0x01))
        return t1.copyOf(32)
    }

    fun encryptWithKey(plaintext: String, key: ByteArray): String {
        val iv = SecureRandom().generateSeed(GCM_IV_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(charset("UTF-8")))
        val blob = iv + ciphertext
        return Base64.getEncoder().encodeToString(blob)
    }

    fun decryptWithKey(blobB64: String, key: ByteArray): String {
        val blob = Base64.getDecoder().decode(blobB64)
        val iv = blob.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = blob.copyOfRange(GCM_IV_LENGTH, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, charset("UTF-8"))
    }
}

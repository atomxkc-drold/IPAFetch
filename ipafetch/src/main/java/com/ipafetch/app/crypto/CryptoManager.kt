package com.ipafetch.app.crypto

import com.lmax.argon2.Argon2
import com.lmax.argon2.Argon2Parameters
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Privacy core of ApiVault.
 *
 * Master password -> Argon2id -> master key -> per-entry AES-256-GCM blobs.
 * Master key never touches disk in plaintext; it is wrapped by Android Keystore.
 * Argon2 parameters are tuned for interactive login (fast enough to type a password,
 * slow enough to defeat GPU cracking).
 */
object CryptoManager {

    // Argon2id parameters (OWASP recommended for interactive use)
    private const val ARGON2_ITERATIONS = 2
    private const val ARGON2_MEMORY_KB = 65536   // 64 MiB
    private const val ARGON2_PARALLELISM = 2
    private const val ARGON2_HASH_LENGTH = 32    // 256-bit key
    private const val ARGON2_SALT_LENGTH = 16

    // AES-256-GCM
    private const val GCM_IV_LENGTH = 12          // bytes
    private const val GCM_TAG_LENGTH = 128        // bits

    /**
     * Derive a 256-bit master key from a password + salt using Argon2id.
     */
    fun deriveMasterKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = ARGON2_ITERATIONS,
        memoryKb: Int = ARGON2_MEMORY_KB,
        parallelism: Int = ARGON2_PARALLELISM,
        hashLength: Int = ARGON2_HASH_LENGTH
    ): ByteArray {
        val argon2 = Argon2.getInstance()
        val params = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withMemory(memoryKb)
            .withIterations(iterations)
            .withParallelism(parallelism)
            .withSalt(salt)
            .withHashLength(hashLength)
            .build()
        return try {
            argon2.hash(params, password)
        } finally {
            argon2.close()
        }
    }

    /** Generate a cryptographically random salt for key derivation. */
    fun generateSalt(): ByteArray = SecureRandom().generateSeed(ARGON2_SALT_LENGTH)

    /**
     * Encrypt a plaintext value (e.g. an API key) using AES-256-GCM.
     * Returns: salt (16) + iv (12) + ciphertext+tag, base64-encoded.
     * The salt is stored alongside so re-encryption with the same master key works.
     */
    fun encrypt(plaintext: String, masterKey: ByteArray): String {
        val salt = SecureRandom().generateSeed(ARGON2_SALT_LENGTH)
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
        val iv = blob.copyOfRange(ARGON2_SALT_LENGTH, ARGON2_SALT_LENGTH + GCM_IV_LENGTH)
        val ciphertext = blob.copyOfRange(ARGON2_SALT_LENGTH + GCM_IV_LENGTH, blob.size)
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

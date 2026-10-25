package com.ipafetch.app.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.random.Random

/**
 * Wraps the master key inside Android Keystore (StrongBox/TEE-backed).
 *
 * Model:
 *   - A Keystore AES-256 key is created once, gated by biometric/PIN.
 *   - The master key is encrypted with that Keystore key (AES-256-GCM).
 *   - Only the ciphertext blob + IV is persisted in SecurePrefs.
 *   - On unlock, biometric auth returns the Keystore key, which decrypts the
 *     blob into the master key — held in memory for the session only.
 */
object KeyStoreManager {

    private const val KEY_ALIAS = "apivault_master_key"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    /** Get or create the biometric-gated Keystore AES key. */
    fun getOrCreateMasterKey(): javax.crypto.SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) {
            return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val keyGenerator = javax.crypto.KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setUserAuthenticationRequired(true)
            setUserAuthenticationValidityDurationSeconds(30)
            setInvalidatedByBiometricEnrollment(true)
            setRandomizedEncryptionRequired(true)
            // StrongBox is API 28+ and not present on every device.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                runCatching { setIsStrongBoxBacked(true) }
            }
        }.build()
        val generated = runCatching {
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        }.recoverCatching {
            // Retry without StrongBox when the device cannot provide it.
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val fallback = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).apply {
                    setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    setUserAuthenticationRequired(true)
                    setUserAuthenticationValidityDurationSeconds(30)
                    setInvalidatedByBiometricEnrollment(true)
                    setRandomizedEncryptionRequired(true)
                }.build()
                keyGenerator.init(fallback)
                keyGenerator.generateKey()
            } else {
                throw it
            }
        }
        generated.getOrThrow()
        return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    /**
     * Encrypt the master key with the Keystore key.
     * Returns: iv (12) + ciphertext+tag, base64-encoded.
     */
    fun wrapMasterKey(masterKey: ByteArray): String {
        val keystoreKey = getOrCreateMasterKey()
        val iv = Random.nextBytes(GCM_IV_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keystoreKey, GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(masterKey)
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    /**
     * Decrypt the master key blob using the biometric-unlocked Keystore key.
     * Returns the plaintext master key, or null on failure.
     *
     * The blob is base64(iv(12) + ciphertext+tag). The caller must supply a
     * Cipher already initialized for AES/GCM/NoPadding decryption with the
     * biometric-authenticated Keystore key.
     */
    fun unwrapMasterKey(authenticatedCipher: Cipher, wrappedB64: String): ByteArray? {
        return try {
            val blob = Base64.getDecoder().decode(wrappedB64)
            val ciphertext = blob.copyOfRange(GCM_IV_LENGTH, blob.size)
            authenticatedCipher.doFinal(ciphertext)
        } catch (e: Exception) {
            null
        }
    }

    /** Delete the stored master key (factory reset / wipe path). */
    fun deleteMasterKey() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)
    }

    fun toB64(data: ByteArray): String = Base64.getEncoder().encodeToString(data)
    fun fromB64(b64: String): ByteArray = Base64.getDecoder().decode(b64)
}

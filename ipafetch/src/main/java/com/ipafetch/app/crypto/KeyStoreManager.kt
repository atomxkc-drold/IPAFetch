package com.ipafetch.app.crypto

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random

/**
 * Wraps the master key with a Keystore-bound AES-256-GCM key.
 *
 * Model:
 *   - A non-exportable Keystore key is created once (StrongBox when the device
 *     offers it, TEE otherwise).
 *   - The password-derived master key is encrypted with it and only the
 *     ciphertext blob is persisted, so the raw master key never touches disk.
 *   - The biometric / device-credential prompt gates the UI before the blob is
 *     unsealed; the Keystore key itself cannot be extracted from the device.
 *
 * The key deliberately does not require per-use authentication: the vault is
 * first unlocked by password (which proves knowledge) and the Keystore wrap is
 * a second, device-bound layer rather than the only check.
 */
object KeyStoreManager {

    private const val KEY_ALIAS = "ipafetch_master_wrap"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    /** Get or create the device-bound Keystore AES key. */
    fun getOrCreateMasterKey(): javax.crypto.SecretKey {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(KEY_ALIAS)) {
            return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
        }
        val generated = runCatching { generateKey(strongBox = true) }
            .recoverCatching { generateKey(strongBox = false) }
        generated.getOrThrow()
        return (ks.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    private fun generateKey(strongBox: Boolean): javax.crypto.SecretKey {
        val generator = javax.crypto.KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        ).apply {
            setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            setRandomizedEncryptionRequired(true)
            setKeySize(256)
            if (strongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                setIsStrongBoxBacked(true)
            }
        }.build()
        generator.init(spec)
        return generator.generateKey()
    }

    /**
     * Encrypt the master key with the Keystore key.
     * Returns base64(iv(12) + ciphertext+tag).
     */
    fun wrapMasterKey(masterKey: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateMasterKey(),
            GCMParameterSpec(GCM_TAG_LENGTH, Random.nextBytes(GCM_IV_LENGTH))
        )
        val ciphertext = cipher.doFinal(masterKey)
        // The IV the cipher actually used is authoritative.
        return Base64.getEncoder().encodeToString(cipher.iv + ciphertext)
    }

    /**
     * Decrypt a blob produced by [wrapMasterKey] with a cipher initialised for
     * GCM decryption on the same IV.
     */
    fun unwrapMasterKey(authenticatedCipher: Cipher, wrappedB64: String): ByteArray? {
        return try {
            val blob = Base64.getDecoder().decode(wrappedB64)
            authenticatedCipher.doFinal(blob.copyOfRange(GCM_IV_LENGTH, blob.size))
        } catch (e: Exception) {
            null
        }
    }

    /** Delete the wrapping key (reset / wipe path). */
    fun deleteMasterKey() {
        runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)
        }
    }

    fun toB64(data: ByteArray): String = Base64.getEncoder().encodeToString(data)
    fun fromB64(b64: String): ByteArray = Base64.getDecoder().decode(b64)
}

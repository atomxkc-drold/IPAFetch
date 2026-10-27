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
 * The alias is versioned (v2). The first release created an
 * authentication-required key under an older alias; that key can never be used
 * without a CryptoObject, which silently broke biometric unlock. Bumping the
 * alias guarantees a fresh, non-auth key, and [canUnwrap] lets callers verify a
 * stored blob instead of trusting its presence.
 */
object KeyStoreManager {

    private const val KEY_ALIAS = "ipafetch_wrap_v2"
    private const val LEGACY_ALIASES = listOf("ipafetch_master_wrap")
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

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

    fun wrapMasterKey(masterKey: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            getOrCreateMasterKey(),
            GCMParameterSpec(GCM_TAG_LENGTH, Random.nextBytes(GCM_IV_LENGTH))
        )
        val ciphertext = cipher.doFinal(masterKey)
        return Base64.getEncoder().encodeToString(cipher.iv + ciphertext)
    }

    /** True when the current Keystore key can actually decrypt this blob. */
    fun canUnwrap(wrappedB64: String): Boolean = runCatching {
        val blob = Base64.getDecoder().decode(wrappedB64)
        val iv = blob.copyOfRange(0, GCM_IV_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateMasterKey(),
            GCMParameterSpec(GCM_TAG_LENGTH, iv)
        )
        cipher.doFinal(blob.copyOfRange(GCM_IV_LENGTH, blob.size))
        true
    }.getOrDefault(false)

    fun unwrapMasterKey(authenticatedCipher: Cipher, wrappedB64: String): ByteArray? = runCatching {
        val blob = Base64.getDecoder().decode(wrappedB64)
        authenticatedCipher.doFinal(blob.copyOfRange(GCM_IV_LENGTH, blob.size))
    }.getOrNull()

    fun deleteMasterKey() {
        runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (ks.containsAlias(KEY_ALIAS)) ks.deleteEntry(KEY_ALIAS)
            LEGACY_ALIASES.forEach { alias ->
                if (ks.containsAlias(alias)) ks.deleteEntry(alias)
            }
        }
    }

    /** Removes keys from earlier releases that were auth-bound and unusable. */
    fun deleteLegacyKeys() {
        runCatching {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            LEGACY_ALIASES.forEach { alias ->
                if (ks.containsAlias(alias)) ks.deleteEntry(alias)
            }
        }
    }

    fun toB64(data: ByteArray): String = Base64.getEncoder().encodeToString(data)
    fun fromB64(b64: String): ByteArray = Base64.getDecoder().decode(b64)
}

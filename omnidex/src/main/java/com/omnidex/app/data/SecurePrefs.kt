package com.omnidex.app.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Device-sealed preferences (salt, wrapped master key, verifier, vault blob).
 *
 * Every value is AES-256-GCM encrypted with a non-auth Keystore key, so the
 * file is unreadable even with the app's private storage mounted. The vault
 * itself is also encrypted with the password-derived key; this is defence in
 * depth, not the only layer.
 */
object SecurePrefs {

    private const val PREFS_NAME = "ipafetch_secure"
    private const val KEY_ALIAS = "ipafetch_prefs_key"

    private const val KEY_SALT = "master_salt"
    private const val KEY_WRAPPED = "master_wrapped"
    private const val KEY_SETUP_DONE = "setup_done"
    private const val KEY_VERIFIER = "master_verifier"
    private const val KEY_VAULT = "vault_blob"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    private const val GCM_TAG_BITS = 128

    private fun prefs(context: Context) = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).apply {
                setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                setRandomizedEncryptionRequired(true)
            }.build()
        )
        return generator.generateKey()
    }

    private fun seal(value: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val ciphertext = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    private fun unseal(stored: String): String? = runCatching {
        val (ivPart, dataPart) = stored.split(":", limit = 2)
        val iv = Base64.decode(ivPart, Base64.NO_WRAP)
        val data = Base64.decode(dataPart, Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(GCM_TAG_BITS, iv))
        String(cipher.doFinal(data), Charsets.UTF_8)
    }.getOrNull()

    private fun putString(context: Context, keyName: String, value: String) {
        prefs(context).edit().putString(keyName, seal(value)).apply()
    }

    private fun getString(context: Context, keyName: String): String? =
        prefs(context).getString(keyName, null)?.let { unseal(it) }

    private fun putBool(context: Context, keyName: String, value: Boolean) =
        putString(context, keyName, if (value) "1" else "0")

    private fun getBool(context: Context, keyName: String, default: Boolean): Boolean =
        getString(context, keyName)?.let { it == "1" } ?: default

    fun isSetupDone(context: Context): Boolean = getBool(context, KEY_SETUP_DONE, false)

    fun markSetupDone(context: Context, done: Boolean) = putBool(context, KEY_SETUP_DONE, done)

    fun isBiometricEnabled(context: Context): Boolean =
        getBool(context, KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(context: Context, enabled: Boolean) =
        putBool(context, KEY_BIOMETRIC_ENABLED, enabled)

    fun getSalt(context: Context): ByteArray? =
        getString(context, KEY_SALT)?.let { Base64.decode(it, Base64.NO_WRAP) }

    fun saveSalt(context: Context, salt: ByteArray) =
        putString(context, KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))

    fun getWrappedMasterKey(context: Context): String? = getString(context, KEY_WRAPPED)

    fun saveWrappedMasterKey(context: Context, wrapped: String) =
        putString(context, KEY_WRAPPED, wrapped)

    fun getVerifier(context: Context): String? = getString(context, KEY_VERIFIER)

    fun saveVerifier(context: Context, verifier: String) =
        putString(context, KEY_VERIFIER, verifier)

    fun saveVault(context: Context, entriesJson: String) =
        putString(context, KEY_VAULT, entriesJson)

    fun loadVault(context: Context): String? = getString(context, KEY_VAULT)

    fun reset(context: Context) {
        prefs(context).edit().clear().apply()
        runCatching {
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
        }
        com.omnidex.app.crypto.KeyStoreManager.deleteMasterKey()
    }
}

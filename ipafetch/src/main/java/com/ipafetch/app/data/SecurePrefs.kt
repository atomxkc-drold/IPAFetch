package com.ipafetch.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ipafetch.app.crypto.KeyStoreManager

/**
 * Encrypted preferences backed by a Keystore master key (AES-256-GCM).
 *
 * Holds the salt for password derivation, the Keystore-wrapped master key used
 * by the biometric path, the password verifier, and the encrypted vault blob.
 * No plaintext secret is ever written here.
 */
object SecurePrefs {

    private const val PREFS_NAME = "ipafetch_secure"
    private const val KEY_SALT = "master_salt"
    private const val KEY_WRAPPED = "master_wrapped"
    private const val KEY_SETUP_DONE = "setup_done"
    private const val KEY_VERIFIER = "master_verifier"
    private const val KEY_VAULT = "vault_blob"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    private var prefs: EncryptedSharedPreferences? = null

    @Synchronized
    private fun get(context: Context): EncryptedSharedPreferences {
        prefs?.let { return it }
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        val created = EncryptedSharedPreferences.create(
            context.applicationContext,
            PREFS_NAME,
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        prefs = created
        return created
    }

    fun isSetupDone(context: Context): Boolean =
        get(context).getBoolean(KEY_SETUP_DONE, false)

    fun markSetupDone(context: Context, done: Boolean) =
        get(context).edit().putBoolean(KEY_SETUP_DONE, done).apply()

    fun isBiometricEnabled(context: Context): Boolean =
        get(context).getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(context: Context, enabled: Boolean) =
        get(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()

    fun getSalt(context: Context): ByteArray? {
        val b64 = get(context).getString(KEY_SALT, null) ?: return null
        return KeyStoreManager.fromB64(b64)
    }

    fun saveSalt(context: Context, salt: ByteArray) {
        get(context).edit().putString(KEY_SALT, KeyStoreManager.toB64(salt)).apply()
    }

    fun getWrappedMasterKey(context: Context): String? =
        get(context).getString(KEY_WRAPPED, null)

    fun saveWrappedMasterKey(context: Context, wrapped: String) {
        get(context).edit().putString(KEY_WRAPPED, wrapped).apply()
    }

    fun getVerifier(context: Context): String? = get(context).getString(KEY_VERIFIER, null)

    fun saveVerifier(context: Context, verifier: String) {
        get(context).edit().putString(KEY_VERIFIER, verifier).apply()
    }

    fun saveVault(context: Context, entriesJson: String) {
        get(context).edit().putString(KEY_VAULT, entriesJson).apply()
    }

    fun loadVault(context: Context): String? = get(context).getString(KEY_VAULT, null)

    fun reset(context: Context) {
        get(context).edit().clear().apply()
        KeyStoreManager.deleteMasterKey()
    }
}

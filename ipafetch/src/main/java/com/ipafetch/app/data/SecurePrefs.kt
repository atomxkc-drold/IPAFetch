package com.ipafetch.app.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ipafetch.app.crypto.KeyStoreManager

/**
 * Encrypted preferences backed by MasterKey + AES-256-GCM.
 *
 * Stores only the salt (for export-password key derivation) and the
 * Keystore-wrapped master key blob. The plaintext master key never
 * touches disk — it lives only in Android Keystore, unlocked by biometric/PIN.
 */
object SecurePrefs {

    private const val PREFS_NAME = "apivault_secure"
    private const val KEY_SALT = "master_salt"
    private const val KEY_WRAPPED = "master_wrapped"
    private const val KEY_SETUP_DONE = "setup_done"
    private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"

    private var prefs: EncryptedSharedPreferences? = null

    @Synchronized
    private fun get(context: Context): EncryptedSharedPreferences {
        if (prefs != null) return prefs as EncryptedSharedPreferences
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKeyAlias,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
        return prefs as EncryptedSharedPreferences
    }

    fun isSetupDone(context: Context): Boolean =
        get(context).getBoolean(KEY_SETUP_DONE, false)

    fun markSetupDone(context: Context, done: Boolean) =
        get(context).edit().putBoolean(KEY_SETUP_DONE, done).apply()

    fun isBiometricEnabled(context: Context): Boolean =
        get(context).getBoolean(KEY_BIOMETRIC_ENABLED, true)

    fun setBiometricEnabled(context: Context, enabled: Boolean) =
        get(context).edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()

    /** Salt used to derive the export-password key via Argon2id. */
    fun getSalt(context: Context): ByteArray? {
        val b64 = get(context).getString(KEY_SALT, null) ?: return null
        return KeyStoreManager.fromB64(b64)
    }

    fun saveSalt(context: Context, salt: ByteArray) {
        get(context).edit().putString(KEY_SALT, KeyStoreManager.toB64(salt)).apply()
    }

    /** Keystore-wrapped master key blob (AES-256-GCM, Keystore-keyed), base64-encoded. */
    fun getWrappedMasterKey(context: Context): String? =
        get(context).getString(KEY_WRAPPED, null)

    fun saveWrappedMasterKey(context: Context, wrapped: ByteArray) {
        get(context).edit().putString(KEY_WRAPPED, KeyStoreManager.toB64(wrapped)).apply()
    }

    fun reset(context: Context) {
        get(context).edit().clear().apply()
        KeyStoreManager.deleteMasterKey()
    }
}

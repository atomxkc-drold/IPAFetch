package com.ipafetch.app.data

import android.content.Context
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.ipafetch.app.crypto.KeyStoreManager
import kotlinx.serialization.json.Json

/**
 * Vault storage — encrypted JSON blob in EncryptedSharedPreferences.
 *
 * All entries are serialized to JSON, encrypted with AES-256-GCM via
 * EncryptedSharedPreferences, and stored under a single key. The master key
 * (for entry-level AES-256-GCM) is wrapped inside Android Keystore and
 * unlocked by biometric/PIN.
 */
object VaultStore {

    private const val PREFS_NAME = "ipafault_secure"
    private const val KEY_VAULT = "vault_blob"
    private const val KEY_SALT = "master_salt"
    private const val KEY_WRAPPED = "master_wrapped"
    private const val KEY_SETUP_DONE = "setup_done"

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

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

    fun getSalt(context: Context): ByteArray? {
        val b64 = get(context).getString(KEY_SALT, null) ?: return null
        return KeyStoreManager.fromB64(b64)
    }

    fun saveSalt(context: Context, salt: ByteArray) {
        get(context).edit().putString(KEY_SALT, KeyStoreManager.toB64(salt)).apply()
    }

    fun getWrappedMasterKey(context: Context): String? =
        get(context).getString(KEY_WRAPPED, null)

    fun saveWrappedMasterKey(context: Context, wrapped: ByteArray) {
        get(context).edit().putString(KEY_WRAPPED, KeyStoreManager.toB64(wrapped)).apply()
    }

    /** Serialize + encrypt all entries as a single JSON blob. */
    fun saveVault(context: Context, entriesJson: String) {
        get(context).edit().putString(KEY_VAULT, entriesJson).apply()
    }

    /** Decrypt + deserialize all entries from the stored JSON blob. */
    fun loadVault(context: Context): String? =
        get(context).getString(KEY_VAULT, null)

    fun reset(context: Context) {
        get(context).edit().clear().apply()
        KeyStoreManager.deleteMasterKey()
    }
}

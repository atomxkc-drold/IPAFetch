package com.apivault.app.ui.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.biometric.BiometricPrompt.PromptInfo
import androidx.fragment.app.FragmentActivity
import com.apivault.app.R
import com.apivault.app.crypto.KeyStoreManager
import com.apivault.app.data.SecurePrefs
import com.apivault.app.security.AuditLogger
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.crypto.Cipher

/**
 * Biometric/PIN gate.
 *
 * Uses BiometricPrompt.CryptoObject so the Keystore key is only usable inside
 * the authenticated session. On success, the returned cipher decrypts the
 * Keystore-wrapped master key blob into the plaintext master key.
 */
@OptIn(androidx.biometric.ExperimentalBiometricsApi::class)
class BiometricGate(
    private val activity: FragmentActivity,
    private val onUnlocked: (masterKey: ByteArray) -> Unit,
    private val onFailed: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun prompt() {
        val biometricManager = BiometricManager.from(activity)
        val canAuthenticate = biometricManager.canAuthenticate(
            BiometricManager.BIOMETRIC_WEAK or BiometricManager.BIOMETRIC_STRONG
        )
        if (canAuthenticate != BiometricManager.BIOMETRIC_SUCCESS) {
            onError(activity.getString(R.string.biometric_not_available))
            return
        }

        val promptInfo = PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_title))
            .setSubtitle(activity.getString(R.string.biometric_subtitle))
            .setConfirmationRequired(false)
            .setNegativeButtonText(activity.getString(R.string.biometric_use_pin))
            .build()

        val cipher = createDecryptCipher(activity)
        if (cipher == null) {
            onError(activity.getString(R.string.biometric_cipher_init_failed))
            return
        }

        val biometricPrompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    val authedCipher = result.cryptoObject?.cipher
                    if (authedCipher != null) {
                        val masterKey = decryptBlob(activity, authedCipher)
                        if (masterKey != null) {
                            AuditLogger.log(activity, AuditLogger.EVENT_UNLOCK_SUCCESS)
                            onUnlocked(masterKey)
                            return
                        }
                    }
                    onFailed(activity.getString(R.string.biometric_decrypt_failed))
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onFailed(activity.getString(R.string.biometric_failed))
                }
            }
        )

        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    private fun createDecryptCipher(context: Context): Cipher? {
        return try {
            val keystoreKey = KeyStoreManager.getOrCreateMasterKey()
            val wrappedB64 = SecurePrefs.getWrappedMasterKey(context) ?: return null
            // The blob is iv(12) + ciphertext; we need the iv to init the GCM cipher.
            val wrapped = com.apivault.app.crypto.KeyStoreManager.fromB64(wrappedB64)
            val iv = wrapped.copyOfRange(0, 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, keystoreKey,
                javax.crypto.spec.GCMParameterSpec(128, iv))
            cipher
        } catch (e: Exception) {
            null
        }
    }

    private fun decryptBlob(context: Context, cipher: Cipher): ByteArray? {
        val wrappedB64 = SecurePrefs.getWrappedMasterKey(context) ?: return null
        return KeyStoreManager.unwrapMasterKey(cipher, wrappedB64)
    }

    fun shutdown() {
        executor.shutdown()
    }
}

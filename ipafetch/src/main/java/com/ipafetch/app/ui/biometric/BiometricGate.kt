package com.ipafetch.app.ui.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.ipafetch.app.R
import com.ipafetch.app.crypto.KeyStoreManager
import com.ipafetch.app.data.SecurePrefs
import java.util.concurrent.Executor
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Biometric/PIN gate backed by a CryptoObject: the Keystore key is only usable
 * inside the authenticated prompt, which decrypts the wrapped master key.
 */
class BiometricGate(
    private val activity: FragmentActivity,
    private val onUnlocked: (masterKey: ByteArray) -> Unit,
    private val onFailed: (String) -> Unit,
    private val onUnavailable: (String) -> Unit
) {
    private val executor: Executor = androidx.core.content.ContextCompat.getMainExecutor(activity)
    private var prompt: BiometricPrompt? = null

    fun canAuthenticate(): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(ALLOWED) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun prompt() {
        if (!canAuthenticate()) {
            onUnavailable(activity.getString(R.string.biometric_not_available))
            return
        }
        val cipher = createDecryptCipher() ?: run {
            onUnavailable(activity.getString(R.string.biometric_cipher_init_failed))
            return
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_title))
            .setSubtitle(activity.getString(R.string.biometric_subtitle))
            .setAllowedAuthenticators(ALLOWED)
            .setConfirmationRequired(false)
            .build()

        prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authenticated = result.cryptoObject?.cipher ?: cipher
                    val key = decryptBlob(authenticated)
                    if (key != null) onUnlocked(key) else onFailed(activity.getString(R.string.biometric_decrypt_failed))
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onFailed(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    onFailed(activity.getString(R.string.biometric_failed))
                }
            }
        )
        prompt?.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    private fun createDecryptCipher(): Cipher? {
        return try {
            val wrappedB64 = SecurePrefs.getWrappedMasterKey(activity) ?: return null
            val wrapped = KeyStoreManager.fromB64(wrappedB64)
            val iv = wrapped.copyOfRange(0, 12)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                KeyStoreManager.getOrCreateMasterKey(),
                GCMParameterSpec(128, iv)
            )
            cipher
        } catch (e: Exception) {
            null
        }
    }

    private fun decryptBlob(cipher: Cipher): ByteArray? {
        val wrappedB64 = SecurePrefs.getWrappedMasterKey(activity) ?: return null
        val blob = KeyStoreManager.fromB64(wrappedB64)
        return try {
            cipher.doFinal(blob.copyOfRange(12, blob.size))
        } catch (e: Exception) {
            null
        }
    }

    fun shutdown() {
        prompt = null
    }

    companion object {
        private const val ALLOWED =
            BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}

package com.ipafetch.app.ui.biometric

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import com.ipafetch.app.R
import com.ipafetch.app.crypto.KeyStoreManager
import com.ipafetch.app.data.SecurePrefs
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec

/**
 * Biometric / device-credential gate.
 *
 * Deliberately does NOT pass a CryptoObject: the wrapped master key uses a
 * device-bound (non-auth) Keystore key, so coupling the prompt to a cipher only
 * added a class of IllegalArgumentException crashes. The prompt is the UI gate;
 * decryption happens after success.
 */
class BiometricGate(
    private val activity: FragmentActivity,
    private val onUnlocked: (masterKey: ByteArray) -> Unit,
    private val onError: (String) -> Unit
) {
    private var prompt: BiometricPrompt? = null

    private data class Plan(val authenticators: Int, val negativeButton: Boolean)

    fun isAvailable(): Boolean = runCatching { plan() != null }.getOrDefault(false)

    fun authenticate() {
        val wrapped = SecurePrefs.getWrappedMasterKey(activity)
        if (wrapped.isNullOrBlank()) {
            onError("Biometric unlock isn't set up for this vault yet. Unlock with your master password once and it will be enabled.")
            return
        }
        val chosen = runCatching { plan() }.getOrNull() ?: run {
            onError("No biometric or screen lock is set up on this device.")
            return
        }

        val infoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_title))
            .setSubtitle(activity.getString(R.string.biometric_subtitle))
            .setAllowedAuthenticators(chosen.authenticators)
            .setConfirmationRequired(false)
        if (chosen.negativeButton) {
            infoBuilder.setNegativeButtonText(activity.getString(R.string.biometric_use_password))
        }

        val listener = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val key = decryptBlob(wrapped)
                if (key != null) {
                    onUnlocked(key)
                } else {
                    onError(activity.getString(R.string.biometric_decrypt_failed))
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> onError("Biometric unlock cancelled")
                    else -> onError(errString.toString())
                }
            }
        }

        runCatching {
            val next = BiometricPrompt(
                activity,
                androidx.core.content.ContextCompat.getMainExecutor(activity),
                listener
            )
            prompt = next
            next.authenticate(infoBuilder.build())
        }.onFailure { throwable ->
            onError("Biometric prompt failed: ${throwable.message ?: throwable.javaClass.simpleName}")
        }
    }

    /**
     * Prefer biometric + device credential, then biometrics alone, then the
     * device PIN/pattern/password alone.
     */
    private fun plan(): Plan? {
        val manager = BiometricManager.from(activity)
        val bio = BiometricManager.Authenticators.BIOMETRIC_WEAK
        val credential = BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val both = runCatching { manager.canAuthenticate(bio or credential) }.getOrNull()
        if (both == BiometricManager.BIOMETRIC_SUCCESS) return Plan(bio or credential, negativeButton = false)

        val biometricOnly = runCatching { manager.canAuthenticate(bio) }.getOrNull()
        if (biometricOnly == BiometricManager.BIOMETRIC_SUCCESS) return Plan(bio, negativeButton = true)

        val credentialOnly = runCatching { manager.canAuthenticate(credential) }.getOrNull()
        if (credentialOnly == BiometricManager.BIOMETRIC_SUCCESS) return Plan(credential, negativeButton = false)

        return null
    }

    private fun decryptBlob(wrappedB64: String): ByteArray? = runCatching {
        val blob = KeyStoreManager.fromB64(wrappedB64)
        val iv = blob.copyOfRange(0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            KeyStoreManager.getOrCreateMasterKey(),
            GCMParameterSpec(128, iv)
        )
        cipher.doFinal(blob.copyOfRange(12, blob.size))
    }.getOrNull()

    fun shutdown() {
        runCatching { prompt?.cancelAuthentication() }
        prompt = null
    }
}

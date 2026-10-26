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
 * The wrapped master key is sealed with a device-bound Keystore key, so this
 * class only needs to decrypt that blob after a successful prompt. Authenticator
 * selection is adaptive: biometrics + device credential when both exist, then
 * biometric-only, then credential-only, so it works on devices that support
 * one but not the other.
 */
class BiometricGate(
    private val activity: FragmentActivity,
    private val onUnlocked: (masterKey: ByteArray) -> Unit,
    private val onError: (String) -> Unit
) {
    private var prompt: BiometricPrompt? = null

    private data class Plan(
        val authenticators: Int,
        val negativeButton: Boolean
    )

    fun isAvailable(): Boolean = plan() != null

    fun authenticate() {
        val wrapped = SecurePrefs.getWrappedMasterKey(activity)
        if (wrapped.isNullOrBlank()) {
            onError("Biometric unlock isn't set up for this vault yet. Unlock with your master password once and it will be enabled.")
            return
        }
        val chosen = plan() ?: run {
            onError("No biometric or screen lock is set up on this device.")
            return
        }
        val cipher = createDecryptCipher(wrapped) ?: run {
            onError("Could not open the vault key with the Keystore.")
            return
        }

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_title))
            .setSubtitle(activity.getString(R.string.biometric_subtitle))
            .setAllowedAuthenticators(chosen.authenticators)
            .setConfirmationRequired(false)
        if (chosen.negativeButton) {
            builder.setNegativeButtonText(activity.getString(R.string.biometric_use_password))
        }

        prompt = BiometricPrompt(
            activity,
            androidx.core.content.ContextCompat.getMainExecutor(activity),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val authenticated = result.cryptoObject?.cipher ?: cipher
                    val key = decryptBlob(authenticated, wrapped)
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
        )
        prompt?.authenticate(builder.build(), BiometricPrompt.CryptoObject(cipher))
    }

    private fun plan(): Plan? {
        val manager = BiometricManager.from(activity)
        val bio = BiometricManager.Authenticators.BIOMETRIC_WEAK
        val credential = BiometricManager.Authenticators.DEVICE_CREDENTIAL

        return when {
            manager.canAuthenticate(bio or credential) == BiometricManager.BIOMETRIC_SUCCESS ->
                Plan(bio or credential, negativeButton = false)

            manager.canAuthenticate(bio) == BiometricManager.BIOMETRIC_SUCCESS ->
                Plan(bio, negativeButton = true)

            manager.canAuthenticate(credential) == BiometricManager.BIOMETRIC_SUCCESS ->
                Plan(credential, negativeButton = false)

            else -> null
        }
    }

    private fun createDecryptCipher(wrappedB64: String): Cipher? = runCatching {
        val wrapped = KeyStoreManager.fromB64(wrappedB64)
        val iv = wrapped.copyOfRange(0, 12)
        Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(
                Cipher.DECRYPT_MODE,
                KeyStoreManager.getOrCreateMasterKey(),
                GCMParameterSpec(128, iv)
            )
        }
    }.getOrNull()

    private fun decryptBlob(cipher: Cipher, wrappedB64: String): ByteArray? = runCatching {
        val blob = KeyStoreManager.fromB64(wrappedB64)
        cipher.doFinal(blob.copyOfRange(12, blob.size))
    }.getOrNull()

    fun shutdown() {
        prompt?.cancelAuthentication()
        prompt = null
    }
}

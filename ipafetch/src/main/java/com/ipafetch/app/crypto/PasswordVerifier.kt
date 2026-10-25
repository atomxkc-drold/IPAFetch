package com.ipafetch.app.crypto

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Lets the master password unlock path prove it derived the right key without
 * storing the key itself. The verifier is an HMAC of a fixed label keyed by the
 * derived master key, so it cannot be replayed to reconstruct the key.
 */
object PasswordVerifier {

    private const val LABEL = "ipafetch-verifier-v1"

    fun create(masterKey: ByteArray): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(masterKey, "HmacSHA256"))
        return KeyStoreManager.toB64(mac.doFinal(LABEL.toByteArray(Charsets.UTF_8)))
    }

    fun matches(masterKey: ByteArray, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        val candidate = create(masterKey)
        return MessageDigest.isEqual(
            candidate.toByteArray(Charsets.UTF_8),
            stored.toByteArray(Charsets.UTF_8)
        )
    }
}

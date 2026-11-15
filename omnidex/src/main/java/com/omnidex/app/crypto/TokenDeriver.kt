package com.omnidex.app.crypto

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

data class DeriveParams(
    val label: String = "",
    val expiryAt: Long? = null,
    val maxUses: Int = 0,
    val scope: String = "read-only",
    val nonce: String = ""
)

object TokenDeriver {
    private val random = SecureRandom()

    fun derive(rootPlain: String, params: DeriveParams): Pair<String, DeriveParams> {
        val finalParams = params.copy(nonce = params.nonce.ifBlank { randomNonce() })
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(rootPlain.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val data = "devstash|v1|${finalParams.scope}|${finalParams.expiryAt}|${finalParams.maxUses}|${finalParams.nonce}|${finalParams.label}"
        val sig = mac.doFinal(data.toByteArray(Charsets.UTF_8))
        val token = Base64.encodeToString(sig, Base64.URL_SAFE or Base64.NO_WRAP)
        return "dst_${token.take(40)}" to finalParams
    }

    private fun randomNonce(): String {
        val bytes = ByteArray(12)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP)
    }
}

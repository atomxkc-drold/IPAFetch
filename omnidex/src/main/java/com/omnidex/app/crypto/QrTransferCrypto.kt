package com.omnidex.app.crypto

import java.security.SecureRandom

object QrTransferCrypto {
    private val random = SecureRandom()

    fun newTransferId(): String {
        val bytes = ByteArray(16)
        random.nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

package com.ipafetch.app.crypto

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertFailsWith

class CryptoManagerTest {

    private val masterKey = "correcthorsebatterystaple".toCharArray()
    private val salt = CryptoManager.generateSalt()

    @Test
    fun deriveMasterKey_isDeterministic() {
        val key1 = CryptoManager.deriveMasterKey(masterKey, salt)
        val key2 = CryptoManager.deriveMasterKey(masterKey, salt)
        assertEquals(key1.contentToString(), key2.contentToString())
    }

    @Test
    fun deriveMasterKey_differentSalt_producesDifferentKey() {
        val key1 = CryptoManager.deriveMasterKey(masterKey, salt)
        val key2 = CryptoManager.deriveMasterKey(masterKey, CryptoManager.generateSalt())
        assertNotEquals(key1.contentToString(), key2.contentToString())
    }

    @Test
    fun encryptDecryptRoundTrip() {
        val derived = CryptoManager.deriveMasterKey(masterKey, salt)
        val plaintext = "sk-1234567890abcdef"

        val blob = CryptoManager.encrypt(plaintext, derived)
        val decrypted = CryptoManager.decrypt(blob, derived)

        assertEquals(plaintext, decrypted)
    }

    @Test
    fun encrypt_tamperedCiphertext_fails() {
        val derived = CryptoManager.deriveMasterKey(masterKey, salt)
        val blob = CryptoManager.encrypt("secret", derived)

        // Flip a byte in the blob after salt+iv
        val tampered = blob.plus("A")
        assertFailsWith<Exception> {
            CryptoManager.decrypt(tampered, derived)
        }
    }

    @Test
    fun generateToken_isExpectedLength() {
        val token = CryptoManager.generateToken(40)
        assertEquals(40, token.length)
    }
}

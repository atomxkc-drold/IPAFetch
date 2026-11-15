package com.omnidex.app.security

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object SyncCrypto {
    data class KeyPair(
        val publicKey: ByteArray,
        val privateKey: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as KeyPair
            if (!publicKey.contentEquals(other.publicKey)) return false
            if (!privateKey.contentEquals(other.privateKey)) return false
            return true
        }
        
        override fun hashCode(): Int {
            var result = publicKey.contentHashCode()
            result = 31 * result + privateKey.contentHashCode()
            return result
        }
        
        fun wipe() {
            publicKey.fill(0)
            privateKey.fill(0)
        }
    }
    
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    
    fun generateKeyPair(): KeyPair {
        // Simplified - in real impl would use Kyber
        val pub = SecureRandom().generateSeed(32)
        val priv = SecureRandom().generateSeed(32)
        return KeyPair(pub, priv)
    }
    
    fun encapsulate(pubKey: ByteArray): Pair<ByteArray, ByteArray> {
        // Simplified - in real impl would use Kyber encapsulation
        val sharedSecret = SecureRandom().generateSeed(32)
        val ciphertext = SecureRandom().generateSeed(32)
        return Pair(ciphertext, sharedSecret)
    }
    
    fun decapsulate(ciphertext: ByteArray, privKey: ByteArray): ByteArray {
        // Simplified
        return SecureRandom().generateSeed(32)
    }
    
    fun deriveKey(sharedSecret: ByteArray, info: String = "devstash-sync-v1"): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(sharedSecret, "HmacSHA256"))
        val result = mac.doFinal(info.toByteArray())
        return result.copyOf(32)
    }
    
    fun encryptPayload(payload: String, sharedSecret: ByteArray): String {
        val key = deriveKey(sharedSecret)
        val iv = SecureRandom().generateSeed(GCM_IV_LENGTH)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val ciphertext = cipher.doFinal(payload.toByteArray(Charsets.UTF_8))
        val blob = iv + ciphertext
        return Base64.getEncoder().encodeToString(blob)
    }
    
    fun decryptPayload(encrypted: String, sharedSecret: ByteArray): String {
        val key = deriveKey(sharedSecret)
        val blob = Base64.getDecoder().decode(encrypted)
        val iv = blob.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = blob.copyOfRange(GCM_IV_LENGTH, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(GCM_TAG_LENGTH, iv))
        val plaintext = cipher.doFinal(ciphertext)
        return String(plaintext, Charsets.UTF_8)
    }
}

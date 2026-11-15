package com.omnidex.app.security

import com.omnidex.app.crypto.CryptoManager
import com.omnidex.app.domain.EntryCategories
import com.omnidex.app.domain.EntryType
import com.omnidex.app.domain.VaultEntry
import java.security.SecureRandom

object CanaryFactory {
    fun generate(type: EntryType, label: String): VaultEntry {
        val fakeSecret = when (type) {
            EntryType.API -> {
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                val random = StringBuilder()
                repeat(16) { random.append(chars[SecureRandom().nextInt(chars.length)]) }
                "AKIA$random"
            }
            EntryType.TOKEN -> {
                val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
                val random = StringBuilder()
                repeat(36) { random.append(chars[SecureRandom().nextInt(chars.length)]) }
                "ghp_$random"
            }
            EntryType.SECRET -> CryptoManager.generateToken(40, "canary_")
            EntryType.JSON -> """{"canary": true, "fake": "${CryptoManager.generateToken(16)}"}"""
            EntryType.TEXT -> "CANARY_FAKE_CREDENTIAL_${CryptoManager.generateToken(8)}"
            else -> CryptoManager.generateToken(32, "CANARY_")
        }
        
        return VaultEntry(
            title = "CANARY: $label",
            type = type.key,
            secret = fakeSecret,
            isCanary = true,
            category = EntryCategories.SECURITY,
            brandId = ""
        )
    }
}

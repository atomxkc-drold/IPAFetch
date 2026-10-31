package com.omnidex.app.domain

import java.util.UUID

data class SessionToken(
    val id: String = UUID.randomUUID().toString(),
    val scopeLabel: String,
    val targetPackage: String? = null,
    val entryIds: List<Long>,
    val expiresAt: Long,
    val sessionKey: ByteArray
) {
    fun isExpired(): Boolean = System.currentTimeMillis() >= expiresAt

    fun wipe() {
        if (sessionKey.isNotEmpty()) {
            sessionKey.fill(0)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SessionToken
        if (id != other.id) return false
        if (scopeLabel != other.scopeLabel) return false
        if (targetPackage != other.targetPackage) return false
        if (entryIds != other.entryIds) return false
        if (expiresAt != other.expiresAt) return false
        if (!sessionKey.contentEquals(other.sessionKey)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + scopeLabel.hashCode()
        result = 31 * result + (targetPackage?.hashCode() ?: 0)
        result = 31 * result + entryIds.hashCode()
        result = 31 * result + expiresAt.hashCode()
        result = 31 * result + sessionKey.contentHashCode()
        return result
    }
}

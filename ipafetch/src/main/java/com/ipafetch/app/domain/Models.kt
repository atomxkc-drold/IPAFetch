package com.ipafetch.app.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** What kind of thing is being stored. */
enum class EntryType(val key: String, val label: String, val hint: String) {
    TOKEN("token", "Token", "Bearer token, PAT or bot token"),
    API("api", "API key", "Provider-issued API key"),
    JSON("json", "JSON", "Service-account or config JSON"),
    LINK("link", "Link", "Console, dashboard or invite link"),
    SECRET("secret", "Secret", "Webhook secret, signing key"),
    NOTE("note", "Note", "Free-form secure note");

    companion object {
        fun from(key: String?): EntryType = entries.firstOrNull { it.key == key } ?: TOKEN
        val editorOrder = listOf(TOKEN, API, JSON, LINK, SECRET, NOTE)
    }
}

object EntryCategories {
    const val AI = "AI & ML"
    const val MUSIC = "Music"
    const val CLOUD = "Cloud"
    const val DEV = "Developer"
    const val PAYMENTS = "Payments"
    const val COMMS = "Communication"
    const val DATA = "Data"
    const val DESIGN = "Design"
    const val OTHER = "Other"

    val ALL = listOf(AI, MUSIC, CLOUD, DEV, PAYMENTS, COMMS, DATA, DESIGN, OTHER)
}

/**
 * One vault record. [secret] is the AES-256-GCM blob (base64) — never plaintext
 * on disk, except that links keep their URL here too so they stay private.
 */
@Serializable
data class VaultEntry(
    val id: Long = 0L,
    val title: String = "",
    val brandId: String = "",
    val type: String = EntryType.TOKEN.key,
    val account: String = "",
    val secret: String = "",
    val generateUrl: String = "",
    val docsUrl: String = "",
    val usage: String = "",
    val usedBy: String = "",
    val usedByApps: List<String> = emptyList(),
    val variant: String = "",
    val permissions: List<PermissionGrant> = emptyList(),
    val category: String = EntryCategories.OTHER,
    val tags: List<String> = emptyList(),
    val environment: String = "",
    val extrasBlob: String = "",
    val expiresAt: Long? = null,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastCopiedAt: Long? = null,
    val copyCount: Int = 0
) {
    @kotlinx.serialization.Transient
    val typeValue: EntryType = EntryType.from(type)

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return title.lowercase().contains(q) ||
            account.lowercase().contains(q) ||
            category.lowercase().contains(q) ||
            type.lowercase().contains(q) ||
            brandId.lowercase().contains(q) ||
            usage.lowercase().contains(q) ||
            tags.any { it.lowercase().contains(q) }
    }

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        expiresAt != null && expiresAt < now

    fun permissionAccess(name: String): String =
        permissions.firstOrNull { it.name == name }?.access ?: PermissionAccess.OFF

    fun expiresSoon(now: Long = System.currentTimeMillis()): Boolean {
        val e = expiresAt ?: return false
        return e >= now && e - now < 7L * 24 * 60 * 60 * 1000
    }
}

/**
 * Legacy records from the old ApiVault schema, kept so an existing install
 * keeps its data after the rebuild.
 */
@Serializable
internal data class LegacyKeyEntry(
    val id: Long = 0L,
    val name: String = "",
    val service: String = "",
    val category: String = "",
    val username: String = "",
    val endpoint: String = "",
    val notes: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val expiresAt: Long? = null,
    val keyLength: Int = 0,
    val prefix: String = "",
    val rotatedAt: Long? = null,
    @SerialName("encryptedValue") val encryptedValue: String = ""
) {
    fun toEntry(): VaultEntry = VaultEntry(
        id = id,
        title = name.ifBlank { service.ifBlank { "Imported key" } },
        brandId = "",
        type = EntryType.API.key,
        account = username,
        secret = encryptedValue,
        generateUrl = endpoint,
        docsUrl = "",
        usage = notes,
        category = when {
            category.contains("API", true) -> EntryCategories.DEV
            category.contains("Database", true) -> EntryCategories.DATA
            category.contains("Cloud", true) -> EntryCategories.CLOUD
            category.contains("Payment", true) -> EntryCategories.PAYMENTS
            category.contains("Communication", true) -> EntryCategories.COMMS
            else -> EntryCategories.OTHER
        },
        createdAt = createdAt,
        updatedAt = if (updatedAt == 0L) createdAt else updatedAt
    )
}

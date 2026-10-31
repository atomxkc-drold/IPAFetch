package com.omnidex.app.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class Severity { INFO, WARNING, CRITICAL }

enum class MeshEdgeType { USED_TOGETHER, DERIVES_FROM, ROTATES_WITH, SAME_CATEGORY }

enum class AccessPolicyType { TIME_WINDOW, REQUIRE_BIOMETRIC, REQUIRE_CHARGING, DENY_IF_ROOTED, DENY_IF_SCREEN_CAPTURE }

enum class CanaryAccessVector { UI_COPY, EXPORT, SESSION_INJECTION, DECRYPT, IMPORT }

enum class HistoryAction { CREATE, UPDATE, ROLLBACK }

@Serializable
data class LintFlag(
    val ruleId: String = "",
    val severity: Severity = Severity.INFO,
    val message: String = "",
    val remediation: String? = null
)

@Serializable
data class HistoryPoint(
    val timestamp: Long = System.currentTimeMillis(),
    val secretBlob: String = "",
    val extrasBlob: String = "",
    val label: String? = null,
    val action: HistoryAction = HistoryAction.UPDATE
)

@Serializable
data class AccessPolicy(
    val type: AccessPolicyType = AccessPolicyType.TIME_WINDOW,
    val enabled: Boolean = true,
    val params: Map<String, String> = emptyMap()
)

/** How a value behaves in the UI: masked, plain, link, command, etc. */
enum class ValueKind { SECRET, TEXT, URL, COMMAND, CONNECTION, COLOR, NUMBER }

/** What kind of thing is being stored. */
enum class EntryType(
    val key: String,
    val label: String,
    val hint: String,
    val kind: ValueKind
) {
    TOKEN("token", "Token", "Bearer token, PAT or bot token", ValueKind.SECRET),
    API("api", "API key", "Provider-issued API key", ValueKind.SECRET),
    SECRET("secret", "Secret", "Webhook secret, signing key", ValueKind.SECRET),
    JSON("json", "JSON", "Service-account or config JSON", ValueKind.TEXT),
    TEXT("text", "Text", "Any plain value or note", ValueKind.TEXT),
    NOTE("note", "Note", "Free-form secure note", ValueKind.TEXT),
    LINK("link", "Link", "Console, dashboard or invite link", ValueKind.URL),
    MODULE("module", "Module", "Magisk / KernelSU module release", ValueKind.URL),
    ADB("adb", "ADB", "adb shell command for an app", ValueKind.COMMAND),
    SSH("ssh", "SSH", "Host, port and optional key", ValueKind.CONNECTION),
    COLOR("color", "Color", "Hex colour value", ValueKind.COLOR),
    NUMBER("number", "Number", "Numeric value with a unit", ValueKind.NUMBER),
    COMPOSITE("composite", "Composite", "Generated artifact from multiple secrets", ValueKind.COMMAND);

    val isSecret: Boolean get() = kind == ValueKind.SECRET

    companion object {
        fun from(key: String?): EntryType {
            if (key.isNullOrBlank()) return TOKEN
            return values().firstOrNull { it.key == key || it.label == key } ?: TOKEN
        }
        val editorOrder = listOf(
            TOKEN, API, SECRET, JSON, TEXT, NOTE,
            LINK, MODULE, ADB, SSH, COLOR, NUMBER, COMPOSITE,
            CONTEXT, PROMPT, SKILL, COMMAND
        )
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
    val variant: String = "",
    val type: String = EntryType.TOKEN.key,
    val account: String = "",
    val usedBy: String = "",
    val usedByApps: List<String> = emptyList(),
    val secret: String = "",
    val generateUrl: String = "",
    val docsUrl: String = "",
    val usage: String = "",
    val category: String = EntryCategories.OTHER,
    val tags: List<String> = emptyList(),
    val permissions: List<PermissionGrant> = emptyList(),
    val environment: String = "",
    val expiresAt: Long? = null,
    val favorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastCopiedAt: Long? = null,
    val copyCount: Int = 0,
    val extrasBlob: String = "",
    // AI Context Vault
    val contextKind: String? = null,
    val projectScope: String? = null,
    val fileNameHint: String? = null,
    val isTemplate: Boolean = false,
    // Burn-after-read
    val burnAfterRead: Boolean = false,
    val consumedAt: Long? = null,
    val consumedByAction: String? = null,
    // Scoped derived tokens
    val derivedFromId: Long? = null,
    val derivationKind: String? = null,
    val derivationMeta: String? = null,
    // Zero-Trust Credential Linting
    val lintFlags: List<LintFlag> = emptyList(),
    // Credential Time-Machine
    val history: List<HistoryPoint> = emptyList(),
    // Canary Credential Network
    val isCanary: Boolean = false,
    // Policy-Gated Access
    val policy: List<AccessPolicy> = emptyList()
) {
    @kotlinx.serialization.Transient
    val typeValue: EntryType = EntryType.from(type)

    @kotlinx.serialization.Transient
    val isDerived: Boolean get() = derivedFromId != null && derivedFromId != 0L

    @kotlinx.serialization.Transient
    val isContext: Boolean get() = typeValue == EntryType.CONTEXT || typeValue == EntryType.PROMPT || typeValue == EntryType.SKILL || typeValue == EntryType.COMMAND

    @kotlinx.serialization.Transient
    val isConsumed: Boolean get() = burnAfterRead && consumedAt != null

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return title.lowercase().contains(q) ||
            account.lowercase().contains(q) ||
            category.lowercase().contains(q) ||
            type.lowercase().contains(q) ||
            brandId.lowercase().contains(q) ||
            usage.lowercase().contains(q) ||
            tags.any { it.lowercase().contains(q) } ||
            usedBy.lowercase().contains(q) ||
            usedByApps.any { it.lowercase().contains(q) } ||
            (contextKind?.lowercase()?.contains(q) == true) ||
            (projectScope?.lowercase()?.contains(q) == true) ||
            (fileNameHint?.lowercase()?.contains(q) == true)
    }

    fun isExpired(now: Long = System.currentTimeMillis()): Boolean =
        expiresAt != null && expiresAt < now

    fun expiresSoon(now: Long = System.currentTimeMillis()): Boolean {
        val e = expiresAt ?: return false
        return e >= now && e - now < 7L * 24 * 60 * 60 * 1000
    }

    fun permissionAccess(name: String): String =
        permissions.firstOrNull { it.name == name }?.access ?: PermissionAccess.OFF
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

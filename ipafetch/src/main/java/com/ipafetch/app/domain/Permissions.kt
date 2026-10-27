package com.ipafetch.app.domain

import kotlinx.serialization.Serializable

/** One granted scope on an entry. Off is represented by absence. */
@Serializable
data class PermissionGrant(
    val name: String,
    val access: String = PermissionAccess.READ
)

object PermissionAccess {
    const val OFF = "off"
    const val READ = "read"
    const val WRITE = "write"
}

/**
 * Known scopes per provider. Some are read-only by the provider's own design —
 * those are marked [readOnly] and the editor refuses to promote them, with the
 * reason shown to the user.
 */
object PermissionCatalog {

    data class Def(
        val name: String,
        val readOnly: Boolean = false,
        val group: String = "Repository"
    )

    val GITHUB: List<Def> = listOf(
        Def("Metadata", readOnly = true),
        Def("Contents"),
        Def("Actions"),
        Def("Workflows"),
        Def("Checks"),
        Def("Deployments"),
        Def("Environments"),
        Def("Issues"),
        Def("Pull requests"),
        Def("Commit statuses"),
        Def("Discussions"),
        Def("Packages"),
        Def("Pages"),
        Def("Projects"),
        Def("Secrets"),
        Def("Variables"),
        Def("Webhooks"),
        Def("Public keys", readOnly = true),
        Def("Administration", group = "Organization"),
        Def("Members", group = "Organization"),
        Def("Organization projects", group = "Organization"),
        Def("Billing", group = "Account"),
        Def("Codespaces", group = "Account"),
        Def("Gists", group = "Account"),
        Def("Notifications", group = "Account")
    )

    val STRIPE: List<Def> = listOf(
        Def("Charges", group = "Payments"),
        Def("Customers", group = "Payments"),
        Def("Payment links", group = "Payments"),
        Def("Invoices", group = "Billing"),
        Def("Subscriptions", group = "Billing"),
        Def("Payouts", group = "Money"),
        Def("Balance", readOnly = true, group = "Money"),
        Def("Webhooks", group = "Platform"),
        Def("Account", readOnly = true, group = "Platform")
    )

    val SLACK: List<Def> = listOf(
        Def("channels:read", readOnly = true, group = "Read"),
        Def("users:read", readOnly = true, group = "Read"),
        Def("files:read", readOnly = true, group = "Read"),
        Def("chat:write", group = "Write"),
        Def("files:write", group = "Write"),
        Def("reactions:write", group = "Write"),
        Def("incoming-webhook", readOnly = true, group = "App"),
        Def("commands", group = "App")
    )

    val SPOTIFY: List<Def> = listOf(
        Def("user-read-email", readOnly = true, group = "Profile"),
        Def("user-read-private", readOnly = true, group = "Profile"),
        Def("playlist-read-private", readOnly = true, group = "Library"),
        Def("user-library-read", readOnly = true, group = "Library"),
        Def("user-library-modify", group = "Library"),
        Def("playlist-modify-public", group = "Playlists"),
        Def("playlist-modify-private", group = "Playlists"),
        Def("streaming", readOnly = true, group = "Playback"),
        Def("user-read-playback-state", readOnly = true, group = "Playback"),
        Def("user-modify-playback-state", group = "Playback")
    )

    val DISCORD: List<Def> = listOf(
        Def("bot", readOnly = true, group = "Gateway"),
        Def("applications.commands", readOnly = true, group = "Gateway"),
        Def("guilds", readOnly = true, group = "Guild"),
        Def("guilds.members.read", readOnly = true, group = "Guild"),
        Def("messages.read", readOnly = true, group = "Guild"),
        Def("webhook.incoming", group = "Webhook")
    )

    val AWS: List<Def> = listOf(
        Def("S3 read", readOnly = true, group = "Storage"),
        Def("S3 write", group = "Storage"),
        Def("IAM read", readOnly = true, group = "Identity"),
        Def("IAM write", group = "Identity"),
        Def("Lambda invoke", group = "Compute"),
        Def("CloudWatch read", readOnly = true, group = "Observability")
    )

    val GENERIC: List<Def> = listOf(
        Def("Read", group = "Access"),
        Def("Write", group = "Access"),
        Def("Delete", group = "Access"),
        Def("Admin", group = "Access"),
        Def("Billing", group = "Account"),
        Def("Webhook", group = "Integration"),
        Def("Files", group = "Data"),
        Def("Users", group = "Data")
    )

    private val byBrand: Map<String, List<Def>> = mapOf(
        "github" to GITHUB,
        "stripe" to STRIPE,
        "slack" to SLACK,
        "spotify" to SPOTIFY,
        "discord" to DISCORD,
        "aws" to AWS
    )

    fun forBrand(brandId: String): List<Def> = byBrand[brandId] ?: GENERIC

    fun def(brandId: String, name: String): Def? =
        forBrand(brandId).firstOrNull { it.name == name }

    /** Explains why write is unavailable for a scope. */
    fun readOnlyReason(brandId: String, name: String): String? {
        val d = def(brandId, name) ?: return null
        if (!d.readOnly) return null
        return when (brandId) {
            "github" -> "GitHub only issues read access for ${d.name}, so write can't be granted."
            "spotify" -> "Spotify exposes ${d.name} as read-only."
            "slack" -> "Slack exposes ${d.name} as a read-only scope."
            else -> "${d.name} is read-only for this provider."
        }
    }

    fun groups(brandId: String): List<Pair<String, List<Def>>> =
        forBrand(brandId).groupBy { it.group }.toList()
}

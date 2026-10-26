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
 * Known scopes per provider. Fine-grained GitHub tokens expose a fixed list
 * where some permissions only ever offer read — those are marked [readOnly] and
 * the editor refuses to promote them to write, with the reason shown inline.
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

    /** Non-GitHub providers get a small generic scope vocabulary. */
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

    fun forBrand(brandId: String): List<Def> =
        if (brandId == "github") GITHUB else GENERIC

    fun def(brandId: String, name: String): Def? =
        forBrand(brandId).firstOrNull { it.name == name }

    /** Explains why tapping write does nothing. */
    fun readOnlyReason(brandId: String, name: String): String? {
        val d = def(brandId, name) ?: return null
        if (!d.readOnly) return null
        return if (brandId == "github") {
            "GitHub only issues read access for ${d.name}, so write can't be granted."
        } else {
            "${d.name} is read-only for this provider."
        }
    }

    fun groups(brandId: String): List<Pair<String, List<Def>>> =
        forBrand(brandId).groupBy { it.group }.toList()
}

object TokenVariants {
    /** GitHub token flavours, shown as a segmented control in the editor. */
    val GITHUB = listOf("Fine-grained", "Classic", "OAuth app")

    fun forBrand(brandId: String): List<String> =
        if (brandId == "github") GITHUB else emptyList()
}

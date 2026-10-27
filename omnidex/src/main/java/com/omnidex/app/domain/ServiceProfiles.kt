package com.omnidex.app.domain

/** An extra provider-specific input shown in the editor (AWS secret, OAuth secret…). */
data class ExtraField(
    val key: String,
    val label: String,
    val placeholder: String = "",
    val secret: Boolean = false
)

/**
 * Per-provider tailoring: token flavours, the explanation for each, and the
 * extra fields that provider needs. Keeps the editor honest instead of showing
 * the same generic form for every service.
 */
data class ServiceProfile(
    val variants: List<String> = emptyList(),
    val variantHints: Map<String, String> = emptyMap(),
    val fields: List<ExtraField> = emptyList(),
    val fieldsByVariant: Map<String, List<ExtraField>> = emptyMap()
) {
    fun hintFor(variant: String): String? = variantHints[variant]

    fun fieldsFor(variant: String): List<ExtraField> =
        fields + (fieldsByVariant[variant] ?: emptyList())
}

object ServiceProfiles {

    private val github = ServiceProfile(
        variants = listOf("Fine-grained", "Classic", "OAuth app"),
        variantHints = mapOf(
            "Fine-grained" to "Scoped to one owner, per-permission, expires by default.",
            "Classic" to "Account-wide scopes. Prefer fine-grained when you can.",
            "OAuth app" to "Client ID plus client secret, with a callback URL."
        ),
        fields = listOf(
            ExtraField("owner", "Owner (user or org)", "octocat"),
            ExtraField("scopes", "Scopes", "repo, read:org")
        ),
        fieldsByVariant = mapOf(
            "OAuth app" to listOf(
                ExtraField("clientId", "Client ID", "Iv1.abc123"),
                ExtraField("clientSecret", "Client secret", "••••", secret = true),
                ExtraField("callback", "Callback URL", "https://yourapp/callback")
            )
        )
    )

    private val aws = ServiceProfile(
        variants = listOf("IAM user", "Role / SSO", "Root"),
        variantHints = mapOf(
            "IAM user" to "Long-lived access key pair. Rotate regularly.",
            "Role / SSO" to "Temporary credentials; they expire, so record the expiry.",
            "Root" to "Avoid. Only for account recovery."
        ),
        fields = listOf(
            ExtraField("accessKeyId", "Access key ID", "AKIA…"),
            ExtraField("secretAccessKey", "Secret access key", "••••", secret = true),
            ExtraField("region", "Region", "eu-west-1"),
            ExtraField("accountId", "Account ID", "123456789012")
        )
    )

    private val stripe = ServiceProfile(
        variants = listOf("Standard key", "Restricted key", "OAuth app"),
        variantHints = mapOf(
            "Standard key" to "Full account access. Live and test keys differ.",
            "Restricted key" to "Scoped to specific resources — preferred.",
            "OAuth app" to "Connect an app to another Stripe account."
        ),
        fields = listOf(
            ExtraField("publishable", "Publishable key", "pk_live_…"),
            ExtraField("webhookSecret", "Webhook signing secret", "whsec_…", secret = true)
        ),
        fieldsByVariant = mapOf(
            "OAuth app" to listOf(
                ExtraField("clientId", "Client ID", "ca_…"),
                ExtraField("redirect", "Redirect URI", "https://yourapp/callback")
            )
        )
    )

    private val slack = ServiceProfile(
        variants = listOf("Bot token", "User token", "App-level token", "Incoming webhook"),
        variantHints = mapOf(
            "Bot token" to "xoxb-… acts as your app inside a workspace.",
            "User token" to "xoxp-… acts as you. Handle with care.",
            "App-level token" to "xapp-… for Socket Mode.",
            "Incoming webhook" to "A single-channel URL, no scopes needed."
        ),
        fields = listOf(
            ExtraField("appId", "App ID", "A01ABCDEF"),
            ExtraField("signingSecret", "Signing secret", "••••", secret = true),
            ExtraField("workspace", "Workspace", "acme.slack.com")
        )
    )

    private val discord = ServiceProfile(
        variants = listOf("Bot token", "Webhook URL", "OAuth2 client"),
        variantHints = mapOf(
            "Bot token" to "Bot user token from the Developer Portal.",
            "Webhook URL" to "Channel webhook; anyone with it can post.",
            "OAuth2 client" to "Client ID + secret for user authorisation."
        ),
        fields = listOf(
            ExtraField("applicationId", "Application ID", "1234567890"),
            ExtraField("publicKey", "Public key", "hex…")
        ),
        fieldsByVariant = mapOf(
            "OAuth2 client" to listOf(ExtraField("clientSecret", "Client secret", "••••", secret = true))
        )
    )

    private val spotify = ServiceProfile(
        variants = listOf("Client credentials", "Authorization code", "PKCE"),
        variantHints = mapOf(
            "Client credentials" to "App-only calls, no user data.",
            "Authorization code" to "Acts on a user's behalf; needs a redirect URI.",
            "PKCE" to "Mobile/SPA flow without a client secret."
        ),
        fields = listOf(
            ExtraField("clientId", "Client ID", "32-char hex"),
            ExtraField("redirectUri", "Redirect URI", "https://yourapp/callback")
        ),
        fieldsByVariant = mapOf(
            "Authorization code" to listOf(ExtraField("clientSecret", "Client secret", "••••", secret = true))
        )
    )

    private val lastfm = ServiceProfile(
        fields = listOf(
            ExtraField("apiKey", "API key", "32-char key"),
            ExtraField("sharedSecret", "Shared secret", "••••", secret = true),
            ExtraField("callback", "Callback URL", "https://yourapp/callback")
        )
    )

    private val openai = ServiceProfile(
        variants = listOf("Project key", "User key", "Service account"),
        variantHints = mapOf(
            "Project key" to "Scoped to one project; the recommended default.",
            "User key" to "Legacy user-level access.",
            "Service account" to "For unattended workloads."
        ),
        fields = listOf(
            ExtraField("organization", "Organization ID", "org-…"),
            ExtraField("project", "Project ID", "proj_…")
        )
    )

    private val anthropic = ServiceProfile(
        variants = listOf("API key", "Workspace key"),
        variantHints = mapOf(
            "API key" to "Console-issued key for the Messages API.",
            "Workspace key" to "Scoped to one workspace."
        ),
        fields = listOf(ExtraField("workspace", "Workspace ID", "wrksp_…"))
    )

    private val gemini = ServiceProfile(
        variants = listOf("AI Studio key", "Vertex service account"),
        variantHints = mapOf(
            "AI Studio key" to "Free-tier friendly key from Google AI Studio.",
            "Vertex" to "GCP service account JSON instead of a key."
        ),
        fields = listOf(ExtraField("project", "Project ID", "my-project-123"))
    )

    private val supabase = ServiceProfile(
        variants = listOf("Anon key", "Service role", "Personal token"),
        variantHints = mapOf(
            "Anon key" to "Public, RLS-protected. Safe for clients.",
            "Service role" to "Bypasses RLS. Server-side only.",
            "Personal token" to "Management API access."
        ),
        fields = listOf(
            ExtraField("projectUrl", "Project URL", "https://xyz.supabase.co"),
            ExtraField("projectRef", "Project ref", "xyzabcdef")
        )
    )

    private val firebase = ServiceProfile(
        variants = listOf("Service account", "Web API key", "App Check"),
        variantHints = mapOf(
            "Service account" to "JSON credential for Admin SDK.",
            "Web API key" to "Public client key, not a secret.",
            "App Check" to "Attestation token for backend calls."
        ),
        fields = listOf(
            ExtraField("projectId", "Project ID", "my-app-123"),
            ExtraField("clientEmail", "Client email", "firebase-adminsdk@….iam.gserviceaccount.com"),
            ExtraField("privateKey", "Private key", "••••", secret = true)
        )
    )

    private val vercel = ServiceProfile(
        variants = listOf("Access token", "Integration"),
        variantHints = mapOf(
            "Access token" to "Personal token scoped to your teams.",
            "Integration" to "Marketplace integration credentials."
        ),
        fields = listOf(ExtraField("team", "Team / scope", "acme"))
    )

    private val cloudflare = ServiceProfile(
        variants = listOf("API token", "Global API key"),
        variantHints = mapOf(
            "API token" to "Scoped token — preferred.",
            "Global key" to "Full account access. Legacy and risky."
        ),
        fields = listOf(
            ExtraField("accountId", "Account ID", "023e105f4ecef8ad9ca31a8372d0c353"),
            ExtraField("zoneId", "Zone ID", "optional")
        )
    )

    private val twilio = ServiceProfile(
        variants = listOf("Account + auth token", "API key"),
        variantHints = mapOf(
            "Account + auth token" to "Full account access.",
            "API key" to "Scoped key pair — preferred."
        ),
        fields = listOf(
            ExtraField("accountSid", "Account SID", "AC…"),
            ExtraField("authToken", "Auth token", "••••", secret = true),
            ExtraField("fromNumber", "From number", "+15551234567")
        )
    )

    private val defaultsByCategory: Map<String, ServiceProfile> = mapOf(
        EntryCategories.AI to ServiceProfile(
            variants = listOf("API key", "Project key"),
            fields = listOf(ExtraField("project", "Project / workspace", "optional"))
        ),
        EntryCategories.MUSIC to ServiceProfile(
            variants = listOf("API key", "OAuth app"),
            fields = listOf(ExtraField("clientId", "Client ID", "optional"))
        ),
        EntryCategories.PAYMENTS to ServiceProfile(
            variants = listOf("Secret key", "Publishable key", "Webhook secret"),
            fields = listOf(ExtraField("accountId", "Account ID", "optional"))
        ),
        EntryCategories.CLOUD to ServiceProfile(
            variants = listOf("Access key", "Service account"),
            fields = listOf(ExtraField("region", "Region", "optional"))
        ),
        EntryCategories.COMMS to ServiceProfile(
            variants = listOf("API key", "Bot token", "Webhook"),
            fields = listOf(ExtraField("sender", "Sender / from", "optional"))
        ),
        EntryCategories.DEV to ServiceProfile(
            variants = listOf("Personal token", "OAuth app"),
            fields = listOf(ExtraField("owner", "Owner / org", "optional"))
        ),
        EntryCategories.DATA to ServiceProfile(
            variants = listOf("Connection string", "API key"),
            fields = listOf(ExtraField("host", "Host", "optional"))
        ),
        EntryCategories.DESIGN to ServiceProfile(
            variants = listOf("Personal token", "OAuth app"),
            fields = listOf(ExtraField("workspace", "Workspace", "optional"))
        )
    )

    private val byBrand: Map<String, ServiceProfile> = mapOf(
        "github" to github,
        "aws" to aws,
        "stripe" to stripe,
        "slack" to slack,
        "discord" to discord,
        "spotify" to spotify,
        "lastfm" to lastfm,
        "openai" to openai,
        "anthropic" to anthropic,
        "gemini" to gemini,
        "supabase" to supabase,
        "firebase" to firebase,
        "vercel" to vercel,
        "cloudflare" to cloudflare,
        "twilio" to twilio
    )

    fun of(brandId: String, category: String = EntryCategories.OTHER): ServiceProfile {
        byBrand[brandId]?.let { return it }
        defaultsByCategory[category]?.let { return it }
        return ServiceProfile(
            variants = listOf("API key", "OAuth app"),
            fields = listOf(ExtraField("notes", "Provider notes", "optional"))
        )
    }

    /** Providers with a bespoke profile rather than the category default. */
    fun isTailored(brandId: String): Boolean = byBrand.containsKey(brandId)
}

package com.ipafetch.app.ui.brand

import androidx.compose.ui.graphics.Color

/**
 * Curated providers. [consoleUrl] is the "where to generate it" link that
 * pre-fills the editor, so adding a key is two taps instead of a search.
 */
data class Brand(
    val id: String,
    val name: String,
    val category: String,
    val color: Long,
    val consoleUrl: String,
    val docsUrl: String = "",
    val keywords: String = ""
) {
    val tint: Color get() = Color(color)
    val hasMark: Boolean get() = BrandPaths.paths.containsKey(id)
}

object BrandCatalog {

    private fun b(
        id: String,
        name: String,
        category: String,
        color: Long,
        console: String,
        docs: String = "",
        keywords: String = ""
    ) = Brand(id, name, category, color, console, docs, keywords)

    val ALL: List<Brand> = listOf(
        // AI & ML
        b("openai", "OpenAI", "AI & ML", 0xFF10A37F, "https://platform.openai.com/api-keys", "https://platform.openai.com/docs", "gpt chatgpt"),
        b("anthropic", "Anthropic", "AI & ML", 0xFFD97757, "https://console.anthropic.com/settings/keys", "https://docs.anthropic.com", "claude"),
        b("gemini", "Google Gemini", "AI & ML", 0xFF4285F4, "https://aistudio.google.com/app/apikey", "https://ai.google.dev/docs", "google ai studio"),
        b("huggingface", "Hugging Face", "AI & ML", 0xFFFFD21E, "https://huggingface.co/settings/tokens", "https://huggingface.co/docs", "hf inference"),
        b("mistral", "Mistral AI", "AI & ML", 0xFFFA520F, "https://console.mistral.ai/api-keys", "https://docs.mistral.ai"),
        b("perplexity", "Perplexity", "AI & ML", 0xFF20808D, "https://www.perplexity.ai/settings/api", "https://docs.perplexity.ai"),
        b("ollama", "Ollama", "AI & ML", 0xFF111111, "https://ollama.com/settings/keys", "https://docs.ollama.com"),
        b("nvidia", "NVIDIA", "AI & ML", 0xFF76B900, "https://build.nvidia.com/", "https://docs.nvidia.com"),
        b("openrouter", "OpenRouter", "AI & ML", 0xFF6467F2, "https://openrouter.ai/keys", "https://openrouter.ai/docs"),
        b("together", "Together AI", "AI & ML", 0xFF0F6FFF, "https://api.together.ai/settings/api-keys", "https://docs.together.ai"),
        b("replicate", "Replicate", "AI & ML", 0xFF111111, "https://replicate.com/account/api-tokens", "https://replicate.com/docs"),
        b("elevenlabs", "ElevenLabs", "AI & ML", 0xFF111111, "https://elevenlabs.io/app/settings/api-keys", "https://elevenlabs.io/docs"),

        // Cloud
        b("aws", "Amazon Web Services", "Cloud", 0xFFFF9900, "https://console.aws.amazon.com/iam/home#/security_credentials", "https://docs.aws.amazon.com", "amazon s3"),
        b("gcp", "Google Cloud", "Cloud", 0xFF4285F4, "https://console.cloud.google.com/apis/credentials", "https://cloud.google.com/docs", "gcp"),
        b("azure", "Microsoft Azure", "Cloud", 0xFF0078D4, "https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade", "https://learn.microsoft.com/azure"),
        b("cloudflare", "Cloudflare", "Cloud", 0xFFF38020, "https://dash.cloudflare.com/profile/api-tokens", "https://developers.cloudflare.com", "workers r2 dns"),
        b("vercel", "Vercel", "Cloud", 0xFF000000, "https://vercel.com/account/tokens", "https://vercel.com/docs"),
        b("netlify", "Netlify", "Cloud", 0xFF00C7B7, "https://app.netlify.com/user/applications#personal-access-tokens", "https://docs.netlify.com"),
        b("digitalocean", "DigitalOcean", "Cloud", 0xFF0080FF, "https://cloud.digitalocean.com/account/api/tokens", "https://docs.digitalocean.com"),
        b("railway", "Railway", "Cloud", 0xFF0B0D0E, "https://railway.app/account/tokens", "https://docs.railway.app"),

        // Developer
        b("github", "GitHub", "Developer", 0xFF181717, "https://github.com/settings/tokens", "https://docs.github.com", "gh pat"),
        b("gitlab", "GitLab", "Developer", 0xFFFC6D26, "https://gitlab.com/-/user_settings/personal_access_tokens", "https://docs.gitlab.com"),
        b("npm", "npm", "Developer", 0xFFCB3837, "https://www.npmjs.com/settings/~tokens", "https://docs.npmjs.com", "node registry"),
        b("docker", "Docker Hub", "Developer", 0xFF2496ED, "https://hub.docker.com/settings/security", "https://docs.docker.com"),
        b("supabase", "Supabase", "Developer", 0xFF3FCF8E, "https://supabase.com/dashboard/account/tokens", "https://supabase.com/docs", "postgres baas"),
        b("firebase", "Firebase", "Developer", 0xFFFFCA28, "https://console.firebase.google.com/", "https://firebase.google.com/docs", "google"),
        b("linear", "Linear", "Developer", 0xFF5E6AD2, "https://linear.app/settings/api", "https://developers.linear.app"),
        b("notion", "Notion", "Developer", 0xFF000000, "https://www.notion.so/my-integrations", "https://developers.notion.com"),
        b("sentry", "Sentry", "Developer", 0xFF362D59, "https://sentry.io/settings/account/api/auth-tokens/", "https://docs.sentry.io"),
        b("algolia", "Algolia", "Developer", 0xFF003DFF, "https://dashboard.algolia.com/account/api-keys", "https://www.algolia.com/doc"),
        b("cloudinary", "Cloudinary", "Developer", 0xFF3448C5, "https://console.cloudinary.com/settings/api-keys", "https://cloudinary.com/documentation"),

        // Data
        b("mongodb", "MongoDB Atlas", "Data", 0xFF47A248, "https://cloud.mongodb.com/v2#/account/publicApi", "https://www.mongodb.com/docs", "atlas"),
        b("redis", "Redis", "Data", 0xFFFF4438, "https://app.redislabs.com/", "https://redis.io/docs"),
        b("postgres", "PostgreSQL", "Data", 0xFF4169E1, "https://www.postgresql.org/docs/current/libpq-connect.html", "https://www.postgresql.org/docs", "database"),
        b("shopify", "Shopify", "Data", 0xFF7AB55C, "https://admin.shopify.com/", "https://shopify.dev/docs", "store"),
        b("mapbox", "Mapbox", "Data", 0xFF000000, "https://account.mapbox.com/access-tokens/", "https://docs.mapbox.com", "maps geo"),
        b("openstreetmap", "OpenStreetMap", "Data", 0xFF7EBC6F, "https://www.openstreetmap.org/oauth2/applications", "https://wiki.openstreetmap.org", "osm"),

        // Payments
        b("stripe", "Stripe", "Payments", 0xFF635BFF, "https://dashboard.stripe.com/apikeys", "https://stripe.com/docs", "billing"),
        b("paypal", "PayPal", "Payments", 0xFF00457C, "https://developer.paypal.com/dashboard/applications", "https://developer.paypal.com/docs"),
        b("coinbase", "Coinbase", "Payments", 0xFF0052FF, "https://www.coinbase.com/settings/api", "https://docs.cdp.coinbase.com", "crypto"),
        b("razorpay", "Razorpay", "Payments", 0xFF0C2451, "https://dashboard.razorpay.com/app/website-app-settings/api-keys", "https://razorpay.com/docs"),

        // Communication
        b("slack", "Slack", "Communication", 0xFF4A154B, "https://api.slack.com/apps", "https://api.slack.com/docs", "webhook bot"),
        b("discord", "Discord", "Communication", 0xFF5865F2, "https://discord.com/developers/applications", "https://discord.com/developers/docs", "bot webhook"),
        b("telegram", "Telegram", "Communication", 0xFF26A5E4, "https://t.me/BotFather", "https://core.telegram.org/bots", "bot"),
        b("twilio", "Twilio", "Communication", 0xFFF22F46, "https://console.twilio.com/us1/account/keys-credentials/api-keys", "https://www.twilio.com/docs", "sms voice"),
        b("resend", "Resend", "Communication", 0xFF000000, "https://resend.com/api-keys", "https://resend.com/docs", "email"),
        b("sendgrid", "SendGrid", "Communication", 0xFF1A82E2, "https://app.sendgrid.com/settings/api_keys", "https://docs.sendgrid.com", "email"),

        // Design
        b("figma", "Figma", "Design", 0xFFF24E1E, "https://www.figma.com/developers/api#access-tokens", "https://www.figma.com/developers/api", "design"),
        b("framer", "Framer", "Design", 0xFF0055FF, "https://www.framer.com/developers/", "https://www.framer.com/developers", "design site"),
        b("dropbox", "Dropbox", "Design", 0xFF0061FF, "https://www.dropbox.com/developers/apps", "https://www.dropbox.com/developers", "files"),
        b("gdrive", "Google Drive", "Design", 0xFF4285F4, "https://console.cloud.google.com/apis/credentials", "https://developers.google.com/drive", "files"),
        b("gsheets", "Google Sheets", "Design", 0xFF34A853, "https://console.cloud.google.com/apis/credentials", "https://developers.google.com/sheets", "spreadsheet")
    )

    private val byId: Map<String, Brand> = ALL.associateBy { it.id }

    fun by(id: String): Brand? = byId[id]

    fun forCategory(category: String): List<Brand> = ALL.filter { it.category == category }

    fun search(query: String): List<Brand> {
        if (query.isBlank()) return ALL
        val q = query.trim().lowercase()
        return ALL.filter {
            it.name.lowercase().contains(q) ||
                it.id.contains(q) ||
                it.category.lowercase().contains(q) ||
                it.keywords.contains(q)
        }
    }

    val categories: List<String> = ALL.map { it.category }.distinct()

    /** Console link for a brand, used by the detail screen's "Generate at" action. */
    fun consoleFor(brandId: String): String? = by(brandId)?.consoleUrl?.takeIf { it.isNotBlank() }
}

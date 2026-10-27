package com.ipafetch.app.ui.brand

import androidx.compose.ui.graphics.Color
import com.ipafetch.app.domain.EntryCategories

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
        // ------------------------------------------------------------------ AI
        b("openai", "OpenAI", EntryCategories.AI, 0xFF10A37F, "https://platform.openai.com/api-keys", "https://platform.openai.com/docs", "gpt chatgpt"),
        b("anthropic", "Anthropic", EntryCategories.AI, 0xFFD97757, "https://console.anthropic.com/settings/keys", "https://docs.anthropic.com", "claude"),
        b("gemini", "Google Gemini", EntryCategories.AI, 0xFF4285F4, "https://aistudio.google.com/app/apikey", "https://ai.google.dev/docs", "google ai studio"),
        b("deepseek", "DeepSeek", EntryCategories.AI, 0xFF4D6BFE, "https://platform.deepseek.com/api_keys", "https://api-docs.deepseek.com", "deepseek r1 v3"),
        b("xai", "xAI Grok", EntryCategories.AI, 0xFF111111, "https://console.x.ai/", "https://docs.x.ai", "grok"),
        b("groq", "Groq", EntryCategories.AI, 0xFFF55036, "https://console.groq.com/keys", "https://console.groq.com/docs", "lpU inference"),
        b("mistral", "Mistral AI", EntryCategories.AI, 0xFFFA520F, "https://console.mistral.ai/api-keys", "https://docs.mistral.ai"),
        b("cohere", "Cohere", EntryCategories.AI, 0xFF39594D, "https://dashboard.cohere.com/api-keys", "https://docs.cohere.com", "command embed"),
        b("perplexity", "Perplexity", EntryCategories.AI, 0xFF20808D, "https://www.perplexity.ai/settings/api", "https://docs.perplexity.ai", "sonar"),
        b("huggingface", "Hugging Face", EntryCategories.AI, 0xFFFFD21E, "https://huggingface.co/settings/tokens", "https://huggingface.co/docs", "hf inference"),
        b("ollama", "Ollama", EntryCategories.AI, 0xFF111111, "https://ollama.com/settings/keys", "https://docs.ollama.com"),
        b("nvidia", "NVIDIA NIM", EntryCategories.AI, 0xFF76B900, "https://build.nvidia.com/", "https://docs.nvidia.com", "cuda"),
        b("openrouter", "OpenRouter", EntryCategories.AI, 0xFF6467F2, "https://openrouter.ai/keys", "https://openrouter.ai/docs", "router"),
        b("together", "Together AI", EntryCategories.AI, 0xFF0F6FFF, "https://api.together.ai/settings/api-keys", "https://docs.together.ai"),
        b("replicate", "Replicate", EntryCategories.AI, 0xFF111111, "https://replicate.com/account/api-tokens", "https://replicate.com/docs", "models"),
        b("stability", "Stability AI", EntryCategories.AI, 0xFF8E44AD, "https://platform.stability.ai/account/keys", "https://platform.stability.ai/docs", "stable diffusion image"),
        b("elevenlabs", "ElevenLabs", EntryCategories.AI, 0xFF111111, "https://elevenlabs.io/app/settings/api-keys", "https://elevenlabs.io/docs", "voice tts"),
        b("langchain", "LangSmith", EntryCategories.AI, 0xFF1C3C3C, "https://smith.langchain.com/settings", "https://docs.smith.langchain.com", "tracing"),
        b("wandb", "Weights & Biases", EntryCategories.AI, 0xFFFFBE00, "https://wandb.ai/authorize", "https://docs.wandb.ai", "ml tracking"),

        // --------------------------------------------------------------- Music
        b("spotify", "Spotify", EntryCategories.MUSIC, 0xFF1DB954, "https://developer.spotify.com/dashboard", "https://developer.spotify.com/documentation", "playlist oauth"),
        b("lastfm", "Last.fm", EntryCategories.MUSIC, 0xFFD51007, "https://www.last.fm/api/account/create", "https://www.last.fm/api", "scrobble"),
        b("applemusic", "Apple Music", EntryCategories.MUSIC, 0xFFFA243C, "https://developer.apple.com/account/resources/authkeys/list", "https://developer.apple.com/documentation/applemusicapi", "music kit"),
        b("soundcloud", "SoundCloud", EntryCategories.MUSIC, 0xFFFF5500, "https://soundcloud.com/you/apps", "https://developers.soundcloud.com/docs", "tracks"),
        b("deezer", "Deezer", EntryCategories.MUSIC, 0xFFA238FF, "https://developers.deezer.com/myapps", "https://developers.deezer.com/api", "streaming"),
        b("musicbrainz", "MusicBrainz", EntryCategories.MUSIC, 0xFFEB743B, "https://musicbrainz.org/account/applications", "https://musicbrainz.org/doc/Development", "metadata"),
        b("discogs", "Discogs", EntryCategories.MUSIC, 0xFF333333, "https://www.discogs.com/settings/developers", "https://www.discogs.com/developers", "vinyl releases"),
        b("genius", "Genius", EntryCategories.MUSIC, 0xFFFFC31C, "https://genius.com/api-clients", "https://docs.genius.com", "lyrics"),
        b("tidal", "TIDAL", EntryCategories.MUSIC, 0xFF111111, "https://developer.tidal.com/dashboard", "https://developer.tidal.com/documentation", "streaming"),
        b("audius", "Audius", EntryCategories.MUSIC, 0xFFCC0FE0, "https://audius.co/settings", "https://docs.audius.org", "web3"),
        b("ytmusic", "YouTube Music", EntryCategories.MUSIC, 0xFFFF0000, "https://console.cloud.google.com/apis/credentials", "https://developers.google.com/youtube", "google"),
        b("bandcamp", "Bandcamp", EntryCategories.MUSIC, 0xFF629AA9, "https://bandcamp.com/developer", "https://bandcamp.com/developer", "artist"),

        // --------------------------------------------------------------- Cloud
        b("aws", "Amazon Web Services", EntryCategories.CLOUD, 0xFFFF9900, "https://console.aws.amazon.com/iam/home#/security_credentials", "https://docs.aws.amazon.com", "amazon s3 iam"),
        b("gcp", "Google Cloud", EntryCategories.CLOUD, 0xFF4285F4, "https://console.cloud.google.com/apis/credentials", "https://cloud.google.com/docs", "gcp"),
        b("azure", "Microsoft Azure", EntryCategories.CLOUD, 0xFF0078D4, "https://portal.azure.com/#view/Microsoft_AAD_RegisteredApps/ApplicationsListBlade", "https://learn.microsoft.com/azure", "entra"),
        b("cloudflare", "Cloudflare", EntryCategories.CLOUD, 0xFFF38020, "https://dash.cloudflare.com/profile/api-tokens", "https://developers.cloudflare.com", "workers r2 dns"),
        b("vercel", "Vercel", EntryCategories.CLOUD, 0xFF000000, "https://vercel.com/account/tokens", "https://vercel.com/docs"),
        b("netlify", "Netlify", EntryCategories.CLOUD, 0xFF00C7B7, "https://app.netlify.com/user/applications#personal-access-tokens", "https://docs.netlify.com"),
        b("digitalocean", "DigitalOcean", EntryCategories.CLOUD, 0xFF0080FF, "https://cloud.digitalocean.com/account/api/tokens", "https://docs.digitalocean.com", "droplets"),
        b("heroku", "Heroku", EntryCategories.CLOUD, 0xFF430098, "https://dashboard.heroku.com/account/applications", "https://devcenter.heroku.com", "dyno"),
        b("railway", "Railway", EntryCategories.CLOUD, 0xFF0B0D0E, "https://railway.app/account/tokens", "https://docs.railway.app"),

        // ----------------------------------------------------------- Developer
        b("github", "GitHub", EntryCategories.DEV, 0xFF181717, "https://github.com/settings/tokens", "https://docs.github.com", "gh pat classic fine-grained oauth"),
        b("gitlab", "GitLab", EntryCategories.DEV, 0xFFFC6D26, "https://gitlab.com/-/user_settings/personal_access_tokens", "https://docs.gitlab.com", "pat"),
        b("bitbucket", "Bitbucket", EntryCategories.DEV, 0xFF0052CC, "https://bitbucket.org/account/settings/app-passwords/", "https://developer.atlassian.com/bitbucket", "atlassian"),
        b("npm", "npm", EntryCategories.DEV, 0xFFCB3837, "https://www.npmjs.com/settings/~tokens", "https://docs.npmjs.com", "node registry"),
        b("docker", "Docker Hub", EntryCategories.DEV, 0xFF2496ED, "https://hub.docker.com/settings/security", "https://docs.docker.com", "registry"),
        b("kubernetes", "Kubernetes", EntryCategories.DEV, 0xFF326CE5, "https://kubernetes.io/docs/reference/access-authn-authz/service-accounts-admin/", "https://kubernetes.io/docs", "k8s service account"),
        b("terraform", "Terraform Cloud", EntryCategories.DEV, 0xFF7B42BC, "https://app.terraform.io/app/settings/tokens", "https://developer.hashicorp.com/terraform", "iac"),
        b("ansible", "Ansible", EntryCategories.DEV, 0xFFEE0000, "https://docs.ansible.com/ansible/latest/collections/", "https://docs.ansible.com", "automation"),
        b("jenkins", "Jenkins", EntryCategories.DEV, 0xFFD24939, "https://www.jenkins.io/doc/book/system-administration/security/", "https://www.jenkins.io/doc", "ci"),
        b("postman", "Postman", EntryCategories.DEV, 0xFFFF6C37, "https://go.postman.co/settings/me/api-keys", "https://learning.postman.com/docs", "collection"),
        b("supabase", "Supabase", EntryCategories.DEV, 0xFF3FCF8E, "https://supabase.com/dashboard/account/tokens", "https://supabase.com/docs", "postgres baas"),
        b("firebase", "Firebase", EntryCategories.DEV, 0xFFFFCA28, "https://console.firebase.google.com/", "https://firebase.google.com/docs", "google"),
        b("linear", "Linear", EntryCategories.DEV, 0xFF5E6AD2, "https://linear.app/settings/api", "https://developers.linear.app", "issues"),
        b("sentry", "Sentry", EntryCategories.DEV, 0xFF362D59, "https://sentry.io/settings/account/api/auth-tokens/", "https://docs.sentry.io", "errors"),
        b("algolia", "Algolia", EntryCategories.DEV, 0xFF003DFF, "https://dashboard.algolia.com/account/api-keys", "https://www.algolia.com/doc", "search"),
        b("cloudinary", "Cloudinary", EntryCategories.DEV, 0xFF3448C5, "https://console.cloudinary.com/settings/api-keys", "https://cloudinary.com/documentation", "media"),
        b("n8n", "n8n", EntryCategories.DEV, 0xFFEA4B71, "https://app.n8n.cloud/manage", "https://docs.n8n.io", "automation"),
        b("zapier", "Zapier", EntryCategories.DEV, 0xFFFF4F00, "https://zapier.com/app/settings/advanced", "https://platform.zapier.com", "automation"),
        b("mapbox", "Mapbox", EntryCategories.DEV, 0xFF111111, "https://account.mapbox.com/access-tokens/", "https://docs.mapbox.com", "maps geo"),

        // ---------------------------------------------------------------- Data
        b("mongodb", "MongoDB Atlas", EntryCategories.DATA, 0xFF47A248, "https://cloud.mongodb.com/v2#/account/publicApi", "https://www.mongodb.com/docs", "atlas"),
        b("redis", "Redis", EntryCategories.DATA, 0xFFFF4438, "https://app.redislabs.com/", "https://redis.io/docs", "cache"),
        b("postgres", "PostgreSQL", EntryCategories.DATA, 0xFF4169E1, "https://www.postgresql.org/docs/current/libpq-connect.html", "https://www.postgresql.org/docs", "database"),
        b("planetscale", "PlanetScale", EntryCategories.DATA, 0xFF111111, "https://app.planetscale.com/", "https://planetscale.com/docs", "mysql"),
        b("datadog", "Datadog", EntryCategories.DATA, 0xFF632CA6, "https://app.datadoghq.com/organization-settings/api-keys", "https://docs.datadoghq.com", "metrics"),
        b("grafana", "Grafana", EntryCategories.DATA, 0xFFF46800, "https://grafana.com/profile/api-keys", "https://grafana.com/docs", "dashboards"),
        b("prometheus", "Prometheus", EntryCategories.DATA, 0xFFE6522C, "https://prometheus.io/docs/prometheus/latest/configuration/configuration/", "https://prometheus.io/docs", "metrics"),
        b("elastic", "Elastic", EntryCategories.DATA, 0xFF005571, "https://cloud.elastic.co/deployment-features/keys", "https://www.elastic.co/guide", "search logs"),
        b("kafka", "Kafka", EntryCategories.DATA, 0xFF231F20, "https://kafka.apache.org/documentation/#security", "https://kafka.apache.org/documentation", "streaming"),
        b("rabbitmq", "RabbitMQ", EntryCategories.DATA, 0xFFFF6600, "https://www.rabbitmq.com/docs/access-control", "https://www.rabbitmq.com/docs", "queue"),
        b("airtable", "Airtable", EntryCategories.DATA, 0xFF18BFFF, "https://airtable.com/create/tokens", "https://airtable.com/developers", "base spreadsheet"),

        // ------------------------------------------------------------ Payments
        b("stripe", "Stripe", EntryCategories.PAYMENTS, 0xFF635BFF, "https://dashboard.stripe.com/apikeys", "https://stripe.com/docs", "billing webhook"),
        b("paypal", "PayPal", EntryCategories.PAYMENTS, 0xFF00457C, "https://developer.paypal.com/dashboard/applications", "https://developer.paypal.com/docs", "checkout"),
        b("coinbase", "Coinbase", EntryCategories.PAYMENTS, 0xFF0052FF, "https://www.coinbase.com/settings/api", "https://docs.cdp.coinbase.com", "crypto"),
        b("razorpay", "Razorpay", EntryCategories.PAYMENTS, 0xFF0C2451, "https://dashboard.razorpay.com/app/website-app-settings/api-keys", "https://razorpay.com/docs", "india"),
        b("square", "Square", EntryCategories.PAYMENTS, 0xFF3E4348, "https://developer.squareup.com/apps", "https://developer.squareup.com/docs", "pos"),
        b("wise", "Wise", EntryCategories.PAYMENTS, 0xFF9FE870, "https://wise.com/settings/api-tokens", "https://api-docs.wise.com", "transfers"),

        // -------------------------------------------------------- Communication
        b("slack", "Slack", EntryCategories.COMMS, 0xFF4A154B, "https://api.slack.com/apps", "https://api.slack.com/docs", "webhook bot oauth"),
        b("discord", "Discord", EntryCategories.COMMS, 0xFF5865F2, "https://discord.com/developers/applications", "https://discord.com/developers/docs", "bot webhook"),
        b("telegram", "Telegram", EntryCategories.COMMS, 0xFF26A5E4, "https://t.me/BotFather", "https://core.telegram.org/bots", "bot"),
        b("twilio", "Twilio", EntryCategories.COMMS, 0xFFF22F46, "https://console.twilio.com/us1/account/keys-credentials/api-keys", "https://www.twilio.com/docs", "sms voice"),
        b("resend", "Resend", EntryCategories.COMMS, 0xFF111111, "https://resend.com/api-keys", "https://resend.com/docs", "email"),
        b("sendgrid", "SendGrid", EntryCategories.COMMS, 0xFF1A82E2, "https://app.sendgrid.com/settings/api_keys", "https://docs.sendgrid.com", "email"),
        b("whatsapp", "WhatsApp", EntryCategories.COMMS, 0xFF25D366, "https://developers.facebook.com/apps/", "https://developers.facebook.com/docs/whatsapp", "meta business"),
        b("zoom", "Zoom", EntryCategories.COMMS, 0xFF0B5CFF, "https://marketplace.zoom.us/user/build", "https://developers.zoom.us/docs", "meetings"),

        // --------------------------------------------------------------- Design
        b("figma", "Figma", EntryCategories.DESIGN, 0xFFF24E1E, "https://www.figma.com/developers/api#access-tokens", "https://www.figma.com/developers/api", "design"),
        b("framer", "Framer", EntryCategories.DESIGN, 0xFF0055FF, "https://www.framer.com/developers/", "https://www.framer.com/developers", "site"),
        b("dropbox", "Dropbox", EntryCategories.DESIGN, 0xFF0061FF, "https://www.dropbox.com/developers/apps", "https://www.dropbox.com/developers", "files"),
        b("gdrive", "Google Drive", EntryCategories.DESIGN, 0xFF4285F4, "https://console.cloud.google.com/apis/credentials", "https://developers.google.com/drive", "files"),
        b("gsheets", "Google Sheets", EntryCategories.DESIGN, 0xFF34A853, "https://console.cloud.google.com/apis/credentials", "https://developers.google.com/sheets", "spreadsheet"),
        b("notion", "Notion", EntryCategories.DESIGN, 0xFF111111, "https://www.notion.so/my-integrations", "https://developers.notion.com", "wiki"),
        b("shopify", "Shopify", EntryCategories.DESIGN, 0xFF7AB55C, "https://admin.shopify.com/", "https://shopify.dev/docs", "store"),
        b("openstreetmap", "OpenStreetMap", EntryCategories.DESIGN, 0xFF7EBC6F, "https://www.openstreetmap.org/oauth2/applications", "https://wiki.openstreetmap.org", "osm geo"),

        // --------------------------------------------------------- Productivity
        b("asana", "Asana", EntryCategories.OTHER, 0xFFF06A6A, "https://app.asana.com/0/my-apps", "https://developers.asana.com/docs", "tasks"),
        b("trello", "Trello", EntryCategories.OTHER, 0xFF0052CC, "https://trello.com/power-ups/admin", "https://developer.atlassian.com/cloud/trello", "boards"),
        b("jira", "Jira", EntryCategories.OTHER, 0xFF0052CC, "https://id.atlassian.com/manage-profile/security/api-tokens", "https://developer.atlassian.com/cloud/jira", "issues")
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

    fun consoleFor(brandId: String): String? = by(brandId)?.consoleUrl?.takeIf { it.isNotBlank() }
}

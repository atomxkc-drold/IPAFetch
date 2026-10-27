package com.ipafetch.app.domain

/**
 * What an AI provider actually offers: modality, base URL and the model ids you
 * can put in a request. Shown under the documentation row so a saved key is
 * usable without leaving the app. A live connection test can hang off these
 * same two fields later.
 */
data class AiModelInfo(
    val modality: String,
    val baseUrl: String,
    val models: List<String>,
    val notes: String = ""
)

object AiCatalog {

    private val byBrand: Map<String, AiModelInfo> = mapOf(
        "openai" to AiModelInfo(
            "Text · vision · audio · embeddings",
            "https://api.openai.com/v1",
            listOf("gpt-4o", "gpt-4o-mini", "o3", "o4-mini", "text-embedding-3-large", "whisper-1", "tts-1")
        ),
        "anthropic" to AiModelInfo(
            "Text · vision",
            "https://api.anthropic.com/v1",
            listOf("claude-opus-4", "claude-sonnet-4", "claude-haiku-3.5")
        ),
        "gemini" to AiModelInfo(
            "Text · vision · audio · video",
            "https://generativelanguage.googleapis.com/v1beta",
            listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-2.0-flash", "text-embedding-004")
        ),
        "deepseek" to AiModelInfo(
            "Text · reasoning",
            "https://api.deepseek.com/v1",
            listOf("deepseek-chat", "deepseek-reasoner"),
            "OpenAI-compatible"
        ),
        "xai" to AiModelInfo(
            "Text · vision",
            "https://api.x.ai/v1",
            listOf("grok-4", "grok-3", "grok-3-mini"),
            "OpenAI-compatible"
        ),
        "groq" to AiModelInfo(
            "Text (very fast) · audio",
            "https://api.groq.com/openai/v1",
            listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "mixtral-8x7b", "whisper-large-v3"),
            "OpenAI-compatible"
        ),
        "mistral" to AiModelInfo(
            "Text · embeddings · vision",
            "https://api.mistral.ai/v1",
            listOf("mistral-large-latest", "mistral-small-latest", "codestral-latest", "mistral-embed")
        ),
        "cohere" to AiModelInfo(
            "Text · rerank · embeddings",
            "https://api.cohere.com/v2",
            listOf("command-r-plus", "command-r", "embed-english-v3", "rerank-v3")
        ),
        "perplexity" to AiModelInfo(
            "Text + live web search",
            "https://api.perplexity.ai",
            listOf("sonar-pro", "sonar", "sonar-reasoning"),
            "OpenAI-compatible"
        ),
        "huggingface" to AiModelInfo(
            "Text · image · audio (hosted models)",
            "https://api-inference.huggingface.co/models",
            listOf("meta-llama/Llama-3.3-70B", "Qwen/Qwen2.5-72B", "black-forest-labs/FLUX.1", "openai/whisper-large-v3")
        ),
        "ollama" to AiModelInfo(
            "Text · vision (local)",
            "http://localhost:11434/api",
            listOf("llama3.3", "qwen2.5", "deepseek-r1", "llava"),
            "Runs on your own machine; no cloud key needed."
        ),
        "openrouter" to AiModelInfo(
            "Text — routes to many providers",
            "https://openrouter.ai/api/v1",
            listOf("openai/gpt-4o", "anthropic/claude-sonnet-4", "deepseek/deepseek-r1", "meta-llama/llama-3.3-70b"),
            "OpenAI-compatible"
        ),
        "together" to AiModelInfo(
            "Text · image",
            "https://api.together.xyz/v1",
            listOf("meta-llama/Llama-3.3-70B-Instruct-Turbo", "mistralai/Mixtral-8x7B", "black-forest-labs/FLUX.1-schnell"),
            "OpenAI-compatible"
        ),
        "replicate" to AiModelInfo(
            "Image · video · audio · text",
            "https://api.replicate.com/v1",
            listOf("black-forest-labs/flux", "stability-ai/sdxl", "meta/llama-3", "minimax/video-01")
        ),
        "stability" to AiModelInfo(
            "Text-to-image · image edit",
            "https://api.stability.ai/v2beta",
            listOf("sd3.5-large", "sd3.5-medium", "core", "stable-image/upscale")
        ),
        "elevenlabs" to AiModelInfo(
            "Text-to-speech · voice clone",
            "https://api.elevenlabs.io/v1",
            listOf("eleven_multilingual_v2", "eleven_turbo_v2_5", "eleven_flash_v2_5")
        ),
        "nvidia" to AiModelInfo(
            "Text · vision · biology",
            "https://integrate.api.nvidia.com/v1",
            listOf("meta/llama-3.3-70b-instruct", "nvidia/nemotron-4", "stabilityai/stable-diffusion-xl"),
            "OpenAI-compatible"
        ),
        "wandb" to AiModelInfo(
            "Experiment tracking",
            "https://api.wandb.ai",
            listOf("runs", "artifacts", "sweeps")
        ),
        "langchain" to AiModelInfo(
            "Tracing · evals",
            "https://api.smith.langchain.com",
            listOf("runs", "datasets", "prompts")
        )
    )

    fun of(brandId: String): AiModelInfo? = byBrand[brandId]

    fun hasInfo(brandId: String): Boolean = byBrand.containsKey(brandId)
}

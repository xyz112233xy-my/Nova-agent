package com.example.data.model

enum class AiProvider(
    val id: String,
    val displayName: String,
    val shortName: String,
    val endpoint: String,
    val keyHint: String,
    val models: List<ModelOption>,
    val defaultModel: String,
    val supportsBuiltIn: Boolean
) {
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        shortName = "Gemini",
        endpoint = "Firebase AI Logic / Gemini API",
        keyHint = "Optional AIza... custom key override",
        models = listOf(
            ModelOption("gemini-3.8-flash", "Gemini 3.8 Flash (Fast Discovery)"),
            ModelOption("gemini-4-argon", "Gemini 4 Argon (Deep Profitability)"),
            ModelOption("gemini-3.5-flash-lite", "Gemini 3.5 Flash-Lite (Ultra Fast)")
        ),
        defaultModel = "gemini-3.8-flash",
        supportsBuiltIn = true
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        shortName = "OpenAI",
        endpoint = "https://api.openai.com/v1/chat/completions",
        keyHint = "sk-...",
        models = listOf(
            ModelOption("gpt-4.1", "GPT-4.1 (Flagship Research)"),
            ModelOption("o3", "o3 (Deep Reasoning)")
        ),
        defaultModel = "gpt-4.1",
        supportsBuiltIn = false
    ),
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        shortName = "DeepSeek",
        endpoint = "https://api.deepseek.com/chat/completions",
        keyHint = "sk-...",
        models = listOf(
            ModelOption("deepseek-flash", "DeepSeek Flash"),
            ModelOption("deepseek-v4-pro", "DeepSeek V4 Pro"),
            ModelOption("deepseek-chat", "DeepSeek Chat (V3)"),
            ModelOption("deepseek-reasoner", "DeepSeek Reasoner (R1)")
        ),
        defaultModel = "deepseek-flash",
        supportsBuiltIn = false
    ),
    PERPLEXITY(
        id = "perplexity",
        displayName = "Perplexity AI",
        shortName = "Perplexity",
        endpoint = "https://api.perplexity.ai/chat/completions",
        keyHint = "pplx-...",
        models = listOf(
            ModelOption("sonar-pro", "Sonar Pro (Deep Web Citations)"),
            ModelOption("sonar", "Sonar (Fast Web Search)")
        ),
        defaultModel = "sonar-pro",
        supportsBuiltIn = false
    );

    companion object {
        fun fromId(id: String?): AiProvider = entries.find { it.id == id } ?: GEMINI
    }
}

enum class GeminiVoiceMode(val id: String, val label: String) {
    STANDARD_READ_ALOUD("standard", "Standard Chat + Read-Aloud"),
    GEMINI_LIVE_FALLBACK("live", "Gemini Live Voice (gemini-3.8-live)")
}

data class ModelOption(
    val id: String,
    val label: String
)

sealed interface ConnectionStatus {
    data object BuiltInReady : ConnectionStatus
    data object Connected : ConnectionStatus
    data object KeyRequired : ConnectionStatus
    data object Testing : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus

    fun badgeText(): String = when (this) {
        BuiltInReady -> "Built-in Ready"
        Connected -> "Connected"
        KeyRequired -> "Key Required"
        Testing -> "Testing..."
        is Error -> "Error"
    }
}

data class WebCitation(
    val title: String,
    val uri: String
)

object NovaCartConstants {
    val CATEGORIES = listOf(
        "Fashion & Lifestyle",
        "Beauty & Personal Care",
        "Home, Kitchen & Living",
        "Electronics & Gadgets",
        "Sports, Fitness & Outdoor"
    )

    val STATUSES = listOf(
        "Newly discovered",
        "Under investigation",
        "Promising",
        "Ready for testing",
        "Approved for sourcing",
        "Declining",
        "Saturated",
        "Rejected"
    )

    val TREND_DEVELOPMENTS = listOf(
        "Emerging",
        "Growing",
        "Stable",
        "Seasonal"
    )

    val QUICK_CHIPS = listOf(
        "High-Margin Niches Under ₹500" to "Discover up to 5 high-margin, low-RTO e-commerce product niches in India with verified supplier cost strictly under ₹500 per unit across Amazon.in, Flipkart, Meesho, and IndiaMART. Follow all 7 stage gates and output the complete structured report.",
        "Trending Reels Gadgets for India" to "Find up to 5 viral problem-solving electronics, desk, or household gadgets currently gaining traction on Instagram Reels & YouTube Shorts in India, with supplier cost strictly under ₹500/unit and BIS/WPC compliance checked.",
        "Home & Kitchen Winners on Amazon.in" to "Research up to 5 high-demand Home, Kitchen & Living utility products under ₹500 supplier cost for India (Tier 1–3 demand), analyzing 1-star to 3-star competitor review gaps on Amazon.in and Flipkart.",
        "Low-RTO Fitness & Grooming Products" to "Identify up to 5 non-size-sensitive Beauty, Grooming, or Sports/Fitness accessories under ₹500 supplier cost with low RTO risk in India and strong Private Label / Dropshipping unit economics."
    )
}

data class ProductOpportunity(
    val id: String,
    val name: String,
    val category: String,
    val discoveryDate: String,
    val discoverySource: String,
    val productLinks: String,
    val supplierLinks: String,
    val supplierPriceInr: Double,
    val sellingPriceInr: Double,
    val grossMarginPct: Double,
    val netProfitEstimateInr: Double,
    val demandIndicators: String,
    val competitorSummary: String,
    val bestBusinessModel: String,
    val score: Int,
    val status: String = "Newly discovered",
    val trendDevelopment: String = "Growing",
    val finalRecommendation: String,
    val rawReport: String
)

data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: String, // "user" | "assistant"
    val content: String,
    val providerLabel: String,
    val citations: List<WebCitation> = emptyList(),
    val extractedProducts: List<ProductOpportunity> = emptyList(),
    val timestampMillis: Long = System.currentTimeMillis()
)

data class ProviderSettings(
    val activeProvider: AiProvider = AiProvider.GEMINI,
    val liveWebSearchEnabled: Boolean = true,
    val voiceRepliesEnabled: Boolean = true,
    val handsFreeWakeEnabled: Boolean = false,
    val geminiVoiceMode: GeminiVoiceMode = GeminiVoiceMode.STANDARD_READ_ALOUD,
    val apiKeys: Map<AiProvider, String> = emptyMap(),
    val selectedModels: Map<AiProvider, String> = AiProvider.entries.associateWith { it.defaultModel },
    val connectionStatuses: Map<AiProvider, ConnectionStatus> = mapOf(
        AiProvider.GEMINI to ConnectionStatus.BuiltInReady,
        AiProvider.OPENAI to ConnectionStatus.KeyRequired,
        AiProvider.DEEPSEEK to ConnectionStatus.KeyRequired,
        AiProvider.PERPLEXITY to ConnectionStatus.KeyRequired
    )
) {
    fun currentModel(): String = selectedModels[activeProvider] ?: activeProvider.defaultModel
    fun currentApiKey(): String = apiKeys[activeProvider].orEmpty()
    fun currentStatus(): ConnectionStatus {
        val explicit = connectionStatuses[activeProvider]
        if (explicit != null) return explicit
        return if (activeProvider == AiProvider.GEMINI && currentApiKey().isBlank()) {
            ConnectionStatus.BuiltInReady
        } else if (currentApiKey().isNotBlank()) {
            ConnectionStatus.Connected
        } else {
            ConnectionStatus.KeyRequired
        }
    }
}

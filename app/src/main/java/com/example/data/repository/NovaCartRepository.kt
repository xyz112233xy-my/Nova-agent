package com.example.data.repository

import android.content.Context
import com.example.data.ai.MultiProviderAiService
import com.example.data.ai.NovaCartResponseParser
import com.example.data.local.NovaCartDatabase
import com.example.data.local.SettingsDataStore
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.AiProvider
import com.example.data.model.ChatMessage
import com.example.data.model.ProductOpportunity
import com.example.data.model.ProviderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

class NovaCartRepository(context: Context) {

    private val db = NovaCartDatabase.getInstance(context)
    private val productDao = db.productDao()
    private val chatDao = db.chatDao()
    private val settingsStore = SettingsDataStore(context)
    private val aiService = MultiProviderAiService()

    val settingsFlow: Flow<ProviderSettings> = settingsStore.settingsFlow

    val savedProductsFlow: Flow<List<ProductOpportunity>> =
        productDao.observeAll().map { list -> list.map { it.toDomain() } }

    val savedProductIdsFlow: Flow<Set<String>> =
        productDao.observeSavedIds().map { it.toSet() }

    val chatMessagesFlow: Flow<List<ChatMessage>> =
        chatDao.observeAllMessages().map { list -> list.map { it.toDomain() } }

    suspend fun sendResearchPrompt(
        prompt: String,
        sessionId: String,
        fromVoice: Boolean = false
    ): Result<ChatMessage> {
        val settings = settingsFlow.first()
        val provider = settings.activeProvider
        val model = settings.currentModel()
        val apiKey = settings.currentApiKey()

        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            sessionId = sessionId,
            role = "user",
            content = prompt.trim(),
            providerLabel = "${provider.shortName} • $model"
        )
        chatDao.insertMessage(userMessage.toEntity())

        val history = chatMessagesFlow.first().filter { it.id != userMessage.id }

        val effectivePrompt = if (fromVoice || settings.voiceRepliesEnabled) {
            val modeNote = if (provider == AiProvider.GEMINI &&
                settings.geminiVoiceMode == com.example.data.model.GeminiVoiceMode.GEMINI_LIVE_FALLBACK
            ) {
                "[Voice Mode: Gemini Live fallback Read-Aloud active — start with ## Voice Summary in natural Hinglish/English.]\n"
            } else if (fromVoice) {
                "[Triggered via Voice / Hello Bhai — start with ## Voice Summary in natural Hinglish/English.]\n"
            } else {
                "[Voice Replies Enabled — start with ## Voice Summary in natural Hinglish/English.]\n"
            }
            modeNote + prompt.trim()
        } else {
            prompt.trim()
        }

        val aiResult = aiService.generateResearchResponse(
            provider = provider,
            model = model,
            apiKey = apiKey,
            liveWebSearch = settings.liveWebSearchEnabled,
            history = history,
            userPrompt = effectivePrompt
        )

        return aiResult.fold(
            onSuccess = { response ->
                val providerLabel = "${provider.shortName} • $model"
                val parsed = NovaCartResponseParser.parse(response.rawText, providerLabel)
                val assistantMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessionId,
                    role = "assistant",
                    content = parsed.displayMarkdown,
                    providerLabel = providerLabel,
                    citations = response.citations,
                    extractedProducts = parsed.products
                )
                chatDao.insertMessage(assistantMessage.toEntity())
                Result.success(assistantMessage)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    suspend fun saveProduct(product: ProductOpportunity): Result<Unit> {
        return try {
            productDao.upsert(product.toEntity())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProductStatus(productId: String, newStatus: String): Result<Unit> {
        return try {
            productDao.updateStatus(productId, newStatus)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteProduct(productId: String): Result<Unit> {
        return try {
            productDao.deleteById(productId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startNewResearchSession() {
        chatDao.deleteAllMessages()
    }

    suspend fun setActiveProvider(provider: AiProvider) = settingsStore.setActiveProvider(provider)

    suspend fun setLiveWebSearch(enabled: Boolean) = settingsStore.setLiveWebSearch(enabled)

    suspend fun setVoiceReplies(enabled: Boolean) = settingsStore.setVoiceReplies(enabled)

    suspend fun setHandsFreeWake(enabled: Boolean) = settingsStore.setHandsFreeWake(enabled)

    suspend fun setGeminiVoiceMode(mode: com.example.data.model.GeminiVoiceMode) =
        settingsStore.setGeminiVoiceMode(mode)

    suspend fun setApiKey(provider: AiProvider, key: String) = settingsStore.setApiKey(provider, key)

    suspend fun setModel(provider: AiProvider, model: String) = settingsStore.setModel(provider, model)

    suspend fun testConnection(provider: AiProvider, model: String, apiKey: String): Result<String> {
        val result = aiService.testConnection(provider, model, apiKey)
        settingsStore.markVerified(provider, result.isSuccess)
        return result
    }

    suspend fun clearAllData() {
        productDao.deleteAll()
        chatDao.deleteAllMessages()
        settingsStore.clearAllSettings()
    }

    fun buildCsvExport(products: List<ProductOpportunity>): String {
        val header = listOf(
            "id",
            "name",
            "category",
            "discoveryDate",
            "discoverySource",
            "productLinks",
            "supplierLinks",
            "supplierPriceInr",
            "sellingPriceInr",
            "grossMarginPct",
            "netProfitEstimateInr",
            "demandIndicators",
            "competitorSummary",
            "bestBusinessModel",
            "score",
            "status",
            "trendDevelopment",
            "finalRecommendation",
            "rawReport"
        ).joinToString(",")

        val rows = products.joinToString("\n") { p ->
            listOf(
                p.id,
                p.name,
                p.category,
                p.discoveryDate,
                p.discoverySource,
                p.productLinks,
                p.supplierLinks,
                p.supplierPriceInr.toString(),
                p.sellingPriceInr.toString(),
                p.grossMarginPct.toString(),
                p.netProfitEstimateInr.toString(),
                p.demandIndicators,
                p.competitorSummary,
                p.bestBusinessModel,
                p.score.toString(),
                p.status,
                p.trendDevelopment,
                p.finalRecommendation,
                p.rawReport
            ).joinToString(",") { escapeCsv(it) }
        }
        return "$header\n$rows"
    }

    private fun escapeCsv(value: String): String {
        val sanitized = value.replace("\r\n", " ").replace("\n", " ")
        return if (sanitized.contains(",") || sanitized.contains("\"")) {
            "\"${sanitized.replace("\"", "\"\"")}\""
        } else {
            sanitized
        }
    }
}

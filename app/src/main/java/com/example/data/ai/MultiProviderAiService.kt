package com.example.data.ai

import android.util.Log
import com.example.data.model.AiProvider
import com.example.data.model.ChatMessage
import com.example.data.model.WebCitation
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Tool
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

private const val TAG = "MultiProviderAiService"

data class AiGenerationResult(
    val rawText: String,
    val citations: List<WebCitation>
)

class MultiProviderAiService {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generateResearchResponse(
        provider: AiProvider,
        model: String,
        apiKey: String,
        liveWebSearch: Boolean,
        history: List<ChatMessage>,
        userPrompt: String
    ): Result<AiGenerationResult> {
        return try {
            val result = when (provider) {
                AiProvider.GEMINI -> {
                    if (apiKey.isBlank()) {
                        callBuiltInFirebaseGemini(model, liveWebSearch, history, userPrompt)
                    } else {
                        callGeminiRestWithCustomKey(model, apiKey, liveWebSearch, history, userPrompt)
                    }
                }
                AiProvider.OPENAI -> {
                    if (apiKey.isBlank()) {
                        throw AiProviderException("OpenAI API key is missing. Add your sk-... key in Settings.")
                    }
                    callOpenAiCompatible(
                        provider = provider,
                        url = provider.endpoint,
                        apiKey = apiKey,
                        model = model,
                        history = history,
                        userPrompt = userPrompt,
                        isReasoningModel = model.startsWith("o3")
                    )
                }
                AiProvider.DEEPSEEK -> {
                    if (apiKey.isBlank()) {
                        throw AiProviderException("DeepSeek API key is missing. Add your key in Settings.")
                    }
                    callOpenAiCompatible(
                        provider = provider,
                        url = provider.endpoint,
                        apiKey = apiKey,
                        model = model,
                        history = history,
                        userPrompt = userPrompt,
                        isReasoningModel = model.contains("reasoner") || model.contains("v4-pro")
                    )
                }
                AiProvider.PERPLEXITY -> {
                    if (apiKey.isBlank()) {
                        throw AiProviderException("Perplexity API key is missing. Add your pplx-... key in Settings.")
                    }
                    callPerplexity(
                        apiKey = apiKey,
                        model = model,
                        history = history,
                        userPrompt = userPrompt
                    )
                }
            }
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI request timed out", e)
            Result.failure(AiProviderException("Request timed out after 90 seconds. Try a narrower query or faster model."))
        } catch (e: AiProviderException) {
            Log.e(TAG, "Provider error: ${e.message}", e)
            Result.failure(e)
        } catch (e: UnknownHostException) {
            Log.e(TAG, "Network unreachable", e)
            Result.failure(AiProviderException("No internet connection or DNS error. Check network access and retry."))
        } catch (e: SocketTimeoutException) {
            Log.e(TAG, "Socket timeout", e)
            Result.failure(AiProviderException("Connection timed out waiting for ${provider.displayName}."))
        } catch (e: IOException) {
            Log.e(TAG, "Network IO error", e)
            Result.failure(AiProviderException("Network error contacting ${provider.displayName}: ${e.localizedMessage ?: "Connection interrupted"}"))
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected AI error", e)
            Result.failure(AiProviderException(mapFriendlyFirebaseError(provider, e)))
        }
    }

    suspend fun testConnection(
        provider: AiProvider,
        model: String,
        apiKey: String
    ): Result<String> {
        return try {
            when (provider) {
                AiProvider.GEMINI -> {
                    if (apiKey.isBlank()) {
                        withTimeout(30_000L) {
                            val gm = Firebase.ai(backend = GenerativeBackend.googleAI())
                                .generativeModel(modelName = model)
                            val resp = gm.generateContent("Reply with strictly: OK")
                            if (resp.text.isNullOrBlank()) {
                                throw AiProviderException("Built-in Gemini returned an empty response.")
                            }
                        }
                        Result.success("Built-in Gemini ($model) verified via Firebase AI Logic.")
                    } else {
                        callGeminiRestWithCustomKey(
                            model = model,
                            apiKey = apiKey,
                            liveWebSearch = false,
                            history = emptyList(),
                            userPrompt = "Reply with strictly: OK"
                        )
                        Result.success("Custom Gemini API key verified for $model.")
                    }
                }
                AiProvider.OPENAI, AiProvider.DEEPSEEK -> {
                    if (apiKey.isBlank()) {
                        return Result.failure(AiProviderException("Enter an API key first."))
                    }
                    callOpenAiCompatible(
                        provider = provider,
                        url = provider.endpoint,
                        apiKey = apiKey,
                        model = model,
                        history = emptyList(),
                        userPrompt = "Reply with strictly: OK",
                        isReasoningModel = model.startsWith("o3") || model.contains("reasoner")
                    )
                    Result.success("${provider.displayName} connected ($model).")
                }
                AiProvider.PERPLEXITY -> {
                    if (apiKey.isBlank()) {
                        return Result.failure(AiProviderException("Enter your pplx-... API key first."))
                    }
                    callPerplexity(
                        apiKey = apiKey,
                        model = model,
                        history = emptyList(),
                        userPrompt = "Reply with strictly: OK"
                    )
                    Result.success("Perplexity AI connected ($model).")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(AiProviderException(mapFriendlyFirebaseError(provider, e)))
        }
    }

    private suspend fun callBuiltInFirebaseGemini(
        modelName: String,
        liveWebSearch: Boolean,
        history: List<ChatMessage>,
        userPrompt: String
    ): AiGenerationResult {
        return withTimeout(90_000L) {
            val generativeModel = Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
                modelName = modelName,
                systemInstruction = content { text(NovaCartPrompts.SYSTEM_PROMPT) },
                tools = if (liveWebSearch) listOf(Tool.googleSearch()) else null
            )

            val recentHistory = history.takeLast(8).map { msg ->
                val role = if (msg.role == "user") "user" else "model"
                content(role = role) { text(msg.content) }
            }

            val response = if (recentHistory.isEmpty()) {
                generativeModel.generateContent(userPrompt)
            } else {
                val chat = generativeModel.startChat(history = recentHistory)
                chat.sendMessage(userPrompt)
            }

            val text = response.text?.takeIf { it.isNotBlank() }
                ?: throw AiProviderException("Gemini returned an empty response. Please try rephrasing.")

            val citations = response.candidates.firstOrNull()
                ?.groundingMetadata
                ?.groundingChunks
                ?.mapNotNull { chunk ->
                    val web = chunk.web
                    val uri = web?.uri
                    if (!uri.isNullOrBlank()) {
                        WebCitation(
                            title = web.title?.takeIf { it.isNotBlank() } ?: uri,
                            uri = uri
                        )
                    } else null
                }
                ?.distinctBy { it.uri }
                .orEmpty()

            AiGenerationResult(rawText = text, citations = citations)
        }
    }

    private suspend fun callGeminiRestWithCustomKey(
        model: String,
        apiKey: String,
        liveWebSearch: Boolean,
        history: List<ChatMessage>,
        userPrompt: String
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
        val contents = JSONArray().apply {
            history.takeLast(8).forEach { msg ->
                put(JSONObject().apply {
                    put("role", if (msg.role == "user") "user" else "model")
                    put("parts", JSONArray().put(JSONObject().put("text", msg.content)))
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
            })
        }

        val payload = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", NovaCartPrompts.SYSTEM_PROMPT)))
            })
            put("contents", contents)
            if (liveWebSearch) {
                put("tools", JSONArray().put(JSONObject().put("google_search", JSONObject())))
            }
        }

        val request = Request.Builder()
            .url(url)
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiProviderException(mapHttpError(AiProvider.GEMINI, response.code, bodyStr))
            }
            val json = JSONObject(bodyStr)
            val candidate = json.optJSONArray("candidates")?.optJSONObject(0)
                ?: throw AiProviderException("Gemini API returned no candidates.")
            val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
            val text = buildString {
                if (parts != null) {
                    for (i in 0 until parts.length()) {
                        append(parts.optJSONObject(i)?.optString("text").orEmpty())
                    }
                }
            }.trim()

            if (text.isBlank()) {
                throw AiProviderException("Gemini API returned an empty message.")
            }

            val citations = mutableListOf<WebCitation>()
            val chunks = candidate.optJSONObject("groundingMetadata")?.optJSONArray("groundingChunks")
            if (chunks != null) {
                for (i in 0 until chunks.length()) {
                    val web = chunks.optJSONObject(i)?.optJSONObject("web") ?: continue
                    val uri = web.optString("uri", "")
                    if (uri.isNotBlank()) {
                        citations += WebCitation(
                            title = web.optString("title", uri).ifBlank { uri },
                            uri = uri
                        )
                    }
                }
            }

            AiGenerationResult(rawText = text, citations = citations.distinctBy { it.uri })
        }
    }

    private suspend fun callOpenAiCompatible(
        provider: AiProvider,
        url: String,
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        userPrompt: String,
        isReasoningModel: Boolean
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", if (isReasoningModel && provider == AiProvider.OPENAI) "developer" else "system")
                put("content", NovaCartPrompts.SYSTEM_PROMPT)
            })
            history.takeLast(8).forEach { msg ->
                put(JSONObject().apply {
                    put("role", if (msg.role == "user") "user" else "assistant")
                    put("content", msg.content)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val payload = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            if (!isReasoningModel) {
                put("temperature", 0.35)
            }
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiProviderException(mapHttpError(provider, response.code, bodyStr))
            }
            val json = JSONObject(bodyStr)
            val contentText = json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                .orEmpty()

            if (contentText.isBlank()) {
                throw AiProviderException("${provider.displayName} returned an empty response.")
            }
            AiGenerationResult(rawText = contentText, citations = emptyList())
        }
    }

    private suspend fun callPerplexity(
        apiKey: String,
        model: String,
        history: List<ChatMessage>,
        userPrompt: String
    ): AiGenerationResult = withContext(Dispatchers.IO) {
        val messages = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("content", NovaCartPrompts.SYSTEM_PROMPT)
            })
            history.takeLast(6).forEach { msg ->
                put(JSONObject().apply {
                    put("role", if (msg.role == "user") "user" else "assistant")
                    put("content", msg.content)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userPrompt)
            })
        }

        val payload = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", 0.25)
        }

        val request = Request.Builder()
            .url(AiProvider.PERPLEXITY.endpoint)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .build()

        client.newCall(request).execute().use { response ->
            val bodyStr = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw AiProviderException(mapHttpError(AiProvider.PERPLEXITY, response.code, bodyStr))
            }
            val json = JSONObject(bodyStr)
            val contentText = json.optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                .orEmpty()

            if (contentText.isBlank()) {
                throw AiProviderException("Perplexity AI returned an empty response.")
            }

            val citations = mutableListOf<WebCitation>()
            val citationsArr = json.optJSONArray("citations")
            if (citationsArr != null) {
                for (i in 0 until citationsArr.length()) {
                    val uri = citationsArr.optString(i, "")
                    if (uri.isNotBlank()) {
                        val host = uri.removePrefix("https://").removePrefix("http://").substringBefore("/")
                        citations += WebCitation(title = host.ifBlank { uri }, uri = uri)
                    }
                }
            }

            AiGenerationResult(rawText = contentText, citations = citations.distinctBy { it.uri })
        }
    }

    private fun mapHttpError(provider: AiProvider, statusCode: Int, body: String): String {
        val serverMsg = try {
            JSONObject(body).optJSONObject("error")?.optString("message").orEmpty()
        } catch (_: Exception) {
            ""
        }
        return when (statusCode) {
            401, 403 -> "${provider.displayName} authentication failed (HTTP $statusCode). Check your API key in Settings."
            404 -> "${provider.displayName} model not found (HTTP 404). Try switching models in Settings."
            429 -> "${provider.displayName} rate limit or quota exceeded (HTTP 429). Wait briefly or switch provider."
            in 500..599 -> "${provider.displayName} server is temporarily unavailable (HTTP $statusCode). Please retry."
            else -> "${provider.displayName} error (HTTP $statusCode)${if (serverMsg.isNotBlank()) ": $serverMsg" else ""}"
        }
    }

    private fun mapFriendlyFirebaseError(provider: AiProvider, e: Exception): String {
        val msg = e.localizedMessage ?: e.message ?: "Unknown error"
        return when {
            msg.contains("401") || msg.contains("API key", ignoreCase = true) ->
                "${provider.displayName}: Invalid or unauthorized API key."
            msg.contains("403") || msg.contains("App Check", ignoreCase = true) ->
                "${provider.displayName}: App Check or permission error (403). Verify network or switch to a custom key in Settings."
            msg.contains("429") || msg.contains("quota", ignoreCase = true) ->
                "${provider.displayName}: Rate limit or quota exceeded (429). Wait or switch provider."
            else -> "${provider.displayName} error: $msg"
        }
    }
}

class AiProviderException(message: String) : Exception(message)

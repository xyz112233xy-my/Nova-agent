package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AiProvider
import com.example.data.model.ConnectionStatus
import com.example.data.model.GeminiVoiceMode
import com.example.data.model.ProviderSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "novacart_provider_settings"
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val ACTIVE_PROVIDER = stringPreferencesKey("active_provider")
        val LIVE_WEB_SEARCH = booleanPreferencesKey("live_web_search")
        val VOICE_REPLIES = booleanPreferencesKey("voice_replies")
        val HANDS_FREE_WAKE = booleanPreferencesKey("hands_free_wake")
        val GEMINI_VOICE_MODE = stringPreferencesKey("gemini_voice_mode")

        fun apiKeyFor(provider: AiProvider) = stringPreferencesKey("api_key_${provider.id}")
        fun modelFor(provider: AiProvider) = stringPreferencesKey("model_${provider.id}")
        fun verifiedFor(provider: AiProvider) = booleanPreferencesKey("verified_${provider.id}")
    }

    val settingsFlow: Flow<ProviderSettings> = context.settingsDataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }
        .map { prefs ->
            val activeProvider = AiProvider.fromId(prefs[Keys.ACTIVE_PROVIDER])
            val liveWeb = prefs[Keys.LIVE_WEB_SEARCH] ?: true
            val voiceReplies = prefs[Keys.VOICE_REPLIES] ?: true
            val handsFreeWake = prefs[Keys.HANDS_FREE_WAKE] ?: false
            val geminiVoiceMode = GeminiVoiceMode.entries.find {
                it.id == prefs[Keys.GEMINI_VOICE_MODE]
            } ?: GeminiVoiceMode.STANDARD_READ_ALOUD

            val keys = AiProvider.entries.associateWith { provider ->
                prefs[Keys.apiKeyFor(provider)].orEmpty()
            }
            val models = AiProvider.entries.associateWith { provider ->
                prefs[Keys.modelFor(provider)]?.takeIf { it.isNotBlank() } ?: provider.defaultModel
            }
            val statuses = AiProvider.entries.associateWith { provider ->
                val key = keys[provider].orEmpty()
                val verified = prefs[Keys.verifiedFor(provider)] ?: false
                when {
                    provider == AiProvider.GEMINI && key.isBlank() -> ConnectionStatus.BuiltInReady
                    verified && key.isNotBlank() -> ConnectionStatus.Connected
                    key.isNotBlank() -> ConnectionStatus.Connected
                    else -> ConnectionStatus.KeyRequired
                }
            }

            ProviderSettings(
                activeProvider = activeProvider,
                liveWebSearchEnabled = liveWeb,
                voiceRepliesEnabled = voiceReplies,
                handsFreeWakeEnabled = handsFreeWake,
                geminiVoiceMode = geminiVoiceMode,
                apiKeys = keys,
                selectedModels = models,
                connectionStatuses = statuses
            )
        }

    suspend fun setActiveProvider(provider: AiProvider) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROVIDER] = provider.id
        }
    }

    suspend fun setLiveWebSearch(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.LIVE_WEB_SEARCH] = enabled
        }
    }

    suspend fun setVoiceReplies(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.VOICE_REPLIES] = enabled
        }
    }

    suspend fun setHandsFreeWake(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.HANDS_FREE_WAKE] = enabled
        }
    }

    suspend fun setGeminiVoiceMode(mode: GeminiVoiceMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.GEMINI_VOICE_MODE] = mode.id
        }
    }

    suspend fun setApiKey(provider: AiProvider, key: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.apiKeyFor(provider)] = key.trim()
            prefs[Keys.verifiedFor(provider)] = false
        }
    }

    suspend fun setModel(provider: AiProvider, model: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.modelFor(provider)] = model
        }
    }

    suspend fun markVerified(provider: AiProvider, verified: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.verifiedFor(provider)] = verified
        }
    }

    suspend fun clearAllSettings() {
        context.settingsDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}

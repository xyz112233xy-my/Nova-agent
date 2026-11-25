package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AiProvider
import com.example.data.model.ChatMessage
import com.example.data.model.ConnectionStatus
import com.example.data.model.GeminiVoiceMode
import com.example.data.model.ProductOpportunity
import com.example.data.model.ProviderSettings
import com.example.data.repository.NovaCartRepository
import com.example.data.voice.NovaCartVoiceManager
import com.example.data.voice.VoiceUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class DatabaseSortOption(val label: String) {
    SCORE_DESC("Top Score"),
    COST_ASC("Lowest Cost (₹)"),
    PROFIT_DESC("Highest Net Profit"),
    DATE_DESC("Newest First")
}

data class ResearchUiState(
    val settings: ProviderSettings = ProviderSettings(),
    val messages: List<ChatMessage> = emptyList(),
    val savedIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorBanner: String? = null,
    val voiceState: VoiceUiState = VoiceUiState()
)

data class DatabaseUiState(
    val allProducts: List<ProductOpportunity> = emptyList(),
    val filteredProducts: List<ProductOpportunity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String = "All",
    val selectedStatus: String = "All",
    val sortOption: DatabaseSortOption = DatabaseSortOption.SCORE_DESC
)

data class SettingsUiState(
    val settings: ProviderSettings = ProviderSettings(),
    val testingProvider: AiProvider? = null,
    val testFeedback: Map<AiProvider, Pair<Boolean, String>> = emptyMap()
)

class NovaCartViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NovaCartRepository(application)

    private var currentSessionId = UUID.randomUUID().toString()

    private val _isLoading = MutableStateFlow(false)
    private val _errorBanner = MutableStateFlow<String?>(null)
    private val _selectedProduct = MutableStateFlow<ProductOpportunity?>(null)
    val selectedProduct: StateFlow<ProductOpportunity?> = _selectedProduct.asStateFlow()

    private val _isProviderSheetOpen = MutableStateFlow(false)
    val isProviderSheetOpen: StateFlow<Boolean> = _isProviderSheetOpen.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow("All")
    private val _selectedStatus = MutableStateFlow("All")
    private val _sortOption = MutableStateFlow(DatabaseSortOption.SCORE_DESC)

    private val _testingProvider = MutableStateFlow<AiProvider?>(null)
    private val _testFeedback = MutableStateFlow<Map<AiProvider, Pair<Boolean, String>>>(emptyMap())

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private val voiceManager = NovaCartVoiceManager(
        context = application,
        onVoiceQueryReady = { query, fromVoice ->
            sendPrompt(query, fromVoice = fromVoice)
        }
    )

    val researchUiState: StateFlow<ResearchUiState> = combine(
        combine(
            repository.settingsFlow,
            repository.chatMessagesFlow,
            repository.savedProductIdsFlow,
            _isLoading,
            _errorBanner
        ) { settings, messages, savedIds, loading, error ->
            ResearchUiState(
                settings = settings,
                messages = messages,
                savedIds = savedIds,
                isLoading = loading,
                errorBanner = error
            )
        },
        voiceManager.state
    ) { base, voice ->
        base.copy(voiceState = voice)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ResearchUiState()
    )

    val databaseUiState: StateFlow<DatabaseUiState> = combine(
        repository.savedProductsFlow,
        _searchQuery,
        _selectedCategory,
        _selectedStatus,
        _sortOption
    ) { products, query, category, status, sort ->
        val filtered = products.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.bestBusinessModel.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || item.category == category
            val matchesStatus = status == "All" || item.status == status
            matchesQuery && matchesCategory && matchesStatus
        }.let { list ->
            when (sort) {
                DatabaseSortOption.SCORE_DESC -> list.sortedByDescending { it.score }
                DatabaseSortOption.COST_ASC -> list.sortedBy { it.supplierPriceInr }
                DatabaseSortOption.PROFIT_DESC -> list.sortedByDescending { it.netProfitEstimateInr }
                DatabaseSortOption.DATE_DESC -> list.sortedByDescending { it.discoveryDate }
            }
        }
        DatabaseUiState(
            allProducts = products,
            filteredProducts = filtered,
            searchQuery = query,
            selectedCategory = category,
            selectedStatus = status,
            sortOption = sort
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DatabaseUiState()
    )

    val settingsUiState: StateFlow<SettingsUiState> = combine(
        repository.settingsFlow,
        _testingProvider,
        _testFeedback
    ) { settings, testing, feedback ->
        val enrichedStatuses = settings.connectionStatuses.toMutableMap()
        if (testing != null) {
            enrichedStatuses[testing] = ConnectionStatus.Testing
        }
        feedback.forEach { (provider, result) ->
            if (!result.first && provider != testing) {
                enrichedStatuses[provider] = ConnectionStatus.Error(result.second)
            } else if (result.first && provider != testing) {
                enrichedStatuses[provider] = if (provider == AiProvider.GEMINI && settings.apiKeys[provider].orEmpty().isBlank()) {
                    ConnectionStatus.BuiltInReady
                } else {
                    ConnectionStatus.Connected
                }
            }
        }
        SettingsUiState(
            settings = settings.copy(connectionStatuses = enrichedStatuses),
            testingProvider = testing,
            testFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun sendPrompt(prompt: String, fromVoice: Boolean = false) {
        if (prompt.isBlank() || _isLoading.value) return
        _errorBanner.value = null
        _isLoading.value = true
        viewModelScope.launch {
            val result = repository.sendResearchPrompt(prompt, currentSessionId, fromVoice = fromVoice)
            _isLoading.value = false
            result.fold(
                onSuccess = { assistantMessage ->
                    val shouldSpeak = fromVoice || researchUiState.value.settings.voiceRepliesEnabled
                    if (shouldSpeak) {
                        voiceManager.speakText(assistantMessage.id, assistantMessage.content)
                    }
                },
                onFailure = { err ->
                    _errorBanner.value = err.message ?: "Unable to reach AI provider."
                }
            )
        }
    }

    fun toggleHandsFreeWake(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHandsFreeWake(enabled)
            if (enabled) {
                voiceManager.startWakeWordMode()
                _snackbarEvents.tryEmit("Hands-free 'Hello Bhai' wake listening enabled")
            } else {
                voiceManager.stopWakeWordMode()
            }
        }
    }

    fun toggleVoiceReplies(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVoiceReplies(enabled)
            if (!enabled) {
                voiceManager.stopSpeaking()
            }
        }
    }

    fun setGeminiVoiceMode(mode: GeminiVoiceMode) {
        viewModelScope.launch {
            repository.setGeminiVoiceMode(mode)
            if (mode == GeminiVoiceMode.GEMINI_LIVE_FALLBACK) {
                _snackbarEvents.tryEmit("Gemini Live Voice enabled (uses Read-Aloud fallback in preview)")
            }
        }
    }

    fun startManualMic() {
        voiceManager.startManualListening()
    }

    fun stopManualMic() {
        voiceManager.stopManualListening()
    }

    fun toggleSpeakMessage(message: ChatMessage) {
        val currentVoice = voiceManager.state.value
        if (currentVoice.isSpeaking && currentVoice.speakingMessageId == message.id) {
            voiceManager.stopSpeaking()
        } else {
            voiceManager.speakText(message.id, message.content)
        }
    }

    fun stopSpeech() {
        voiceManager.stopSpeaking()
    }

    fun dismissVoiceWarning() {
        voiceManager.clearWarning()
    }

    fun onMicPermissionDenied() {
        viewModelScope.launch {
            repository.setHandsFreeWake(false)
            voiceManager.showPermissionDeniedWarning()
        }
    }

    fun dismissErrorBanner() {
        _errorBanner.value = null
    }

    fun startNewResearch() {
        viewModelScope.launch {
            voiceManager.stopSpeaking()
            repository.startNewResearchSession()
            currentSessionId = UUID.randomUUID().toString()
            _errorBanner.value = null
            _snackbarEvents.tryEmit("Started a fresh NOVACART research desk.")
        }
    }

    fun saveProductOpportunity(product: ProductOpportunity) {
        viewModelScope.launch {
            val result = repository.saveProduct(product)
            result.fold(
                onSuccess = {
                    _snackbarEvents.tryEmit("Saved \"${product.name}\" to Database")
                },
                onFailure = { err ->
                    _errorBanner.value = "Failed to save to local storage: ${err.localizedMessage}"
                }
            )
        }
    }

    fun saveMessageReportAsOpportunity(message: ChatMessage) {
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val firstHeading = message.content.lines()
            .firstOrNull { it.trim().startsWith("#") || it.trim().startsWith("**") }
            ?.replace("#", "")
            ?.replace("*", "")
            ?.trim()
            ?.take(64)
            ?: "NOVACART Research Report"

        val opportunity = ProductOpportunity(
            id = "report-${message.id}",
            name = firstHeading,
            category = "Home, Kitchen & Living",
            discoveryDate = todayIso,
            discoverySource = message.providerLabel,
            productLinks = message.citations.joinToString(" | ") { it.uri }.ifBlank { "Amazon.in / Flipkart / Meesho" },
            supplierLinks = "IndiaMART / Udaan (See full report)",
            supplierPriceInr = 0.0,
            sellingPriceInr = 0.0,
            grossMarginPct = 0.0,
            netProfitEstimateInr = 0.0,
            demandIndicators = "Captured from research report",
            competitorSummary = "See full report details",
            bestBusinessModel = "See report recommendation",
            score = 75,
            status = "Under investigation",
            trendDevelopment = "Growing",
            finalRecommendation = "Review extracted unit economics in full report.",
            rawReport = message.content
        )
        saveProductOpportunity(opportunity)
    }

    fun openProductDetail(product: ProductOpportunity) {
        _selectedProduct.value = product
    }

    fun closeProductDetail() {
        _selectedProduct.value = null
    }

    fun updateProductStatus(productId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateProductStatus(productId, newStatus).fold(
                onSuccess = {
                    val current = _selectedProduct.value
                    if (current != null && current.id == productId) {
                        _selectedProduct.value = current.copy(status = newStatus)
                    }
                    _snackbarEvents.tryEmit("Status updated to $newStatus")
                },
                onFailure = { err ->
                    _snackbarEvents.tryEmit("Could not update status: ${err.localizedMessage}")
                }
            )
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            repository.deleteProduct(productId).fold(
                onSuccess = {
                    if (_selectedProduct.value?.id == productId) {
                        _selectedProduct.value = null
                    }
                    _snackbarEvents.tryEmit("Removed record from Database")
                },
                onFailure = { err ->
                    _snackbarEvents.tryEmit("Failed to delete record: ${err.localizedMessage}")
                }
            )
        }
    }

    fun buildCsvString(): String {
        return repository.buildCsvExport(databaseUiState.value.allProducts)
    }

    fun notifyMessage(message: String) {
        _snackbarEvents.tryEmit(message)
    }

    fun openProviderSheet() {
        _isProviderSheetOpen.value = true
    }

    fun closeProviderSheet() {
        _isProviderSheetOpen.value = false
    }

    fun setActiveProvider(provider: AiProvider) {
        viewModelScope.launch {
            repository.setActiveProvider(provider)
            _errorBanner.value = null
            _isProviderSheetOpen.value = false
        }
    }

    fun setLiveWebSearch(enabled: Boolean) {
        viewModelScope.launch {
            repository.setLiveWebSearch(enabled)
        }
    }

    fun setApiKey(provider: AiProvider, key: String) {
        viewModelScope.launch {
            repository.setApiKey(provider, key)
            _testFeedback.value = _testFeedback.value - provider
        }
    }

    fun setModel(provider: AiProvider, model: String) {
        viewModelScope.launch {
            repository.setModel(provider, model)
        }
    }

    fun testProviderConnection(provider: AiProvider) {
        val currentSettings = settingsUiState.value.settings
        val model = currentSettings.selectedModels[provider] ?: provider.defaultModel
        val key = currentSettings.apiKeys[provider].orEmpty()

        _testingProvider.value = provider
        viewModelScope.launch {
            val result = repository.testConnection(provider, model, key)
            _testingProvider.value = null
            result.fold(
                onSuccess = { msg ->
                    _testFeedback.value = _testFeedback.value + (provider to (true to msg))
                },
                onFailure = { err ->
                    _testFeedback.value = _testFeedback.value + (provider to (false to (err.message ?: "Connection failed")))
                }
            )
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String) {
        _selectedCategory.value = category
    }

    fun setStatusFilter(status: String) {
        _selectedStatus.value = status
    }

    fun setSortOption(option: DatabaseSortOption) {
        _sortOption.value = option
    }

    fun clearAllData() {
        viewModelScope.launch {
            voiceManager.stopWakeWordMode()
            voiceManager.stopSpeaking()
            repository.clearAllData()
            _testFeedback.value = emptyMap()
            _selectedProduct.value = null
            _errorBanner.value = null
            currentSessionId = UUID.randomUUID().toString()
            _snackbarEvents.tryEmit("All saved products, chats, and custom keys cleared.")
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager.release()
    }
}

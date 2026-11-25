package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

private const val TAG = "NovaCartVoice"

data class VoiceUiState(
    val isSpeechAvailable: Boolean = true,
    val isWakeModeActive: Boolean = false,
    val isManualListening: Boolean = false,
    val isWakeTriggered: Boolean = false,
    val interimTranscript: String = "",
    val isSpeaking: Boolean = false,
    val speakingMessageId: String? = null,
    val voiceWarning: String? = null
)

object WakePhraseMatcher {
    private val WAKE_PHRASES = listOf(
        "hello bhai batao product kya hai",
        "batao product kya hai",
        "bhai product batao",
        "hello bhai",
        "helo bhai",
        "halo bhai"
    )

    data class WakeMatch(
        val matched: Boolean,
        val followUpQuery: String
    )

    fun evaluate(rawTranscript: String): WakeMatch {
        val cleaned = rawTranscript
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9₹\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleaned.isBlank()) return WakeMatch(false, "")

        for (phrase in WAKE_PHRASES) {
            val idx = cleaned.indexOf(phrase)
            if (idx != -1) {
                val after = cleaned.substring(idx + phrase.length).trim()
                val isGenericOnly = after.isBlank() ||
                    after == "batao product kya hai" ||
                    after == "product kya hai" ||
                    after == "kya hai"
                val query = if (isGenericOnly) {
                    "Hello bhai! Suggest a top trending Indian e-commerce product opportunity strictly under ₹500 supplier cost right now with full supplier price, target selling price, estimated profit & margin %, and best marketplace/business model."
                } else {
                    after
                }
                return WakeMatch(matched = true, followUpQuery = query)
            }
        }
        return WakeMatch(matched = false, followUpQuery = "")
    }
}

class NovaCartVoiceManager(
    context: Context,
    private val onVoiceQueryReady: (query: String, fromVoice: Boolean) -> Unit
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(
        VoiceUiState(isSpeechAvailable = SpeechRecognizer.isRecognitionAvailable(appContext))
    )
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var shouldKeepWakeActive = false
    private var isRestartScheduled = false

    init {
        initTextToSpeech()
    }

    private fun initTextToSpeech() {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val enIn = Locale("en", "IN")
                val hiIn = Locale("hi", "IN")
                val res = tts?.setLanguage(enIn)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    val hiRes = tts?.setLanguage(hiIn)
                    if (hiRes == TextToSpeech.LANG_MISSING_DATA || hiRes == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.US)
                    }
                }
                tts?.setSpeechRate(1.02f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainHandler.post {
                            _state.value = _state.value.copy(isSpeaking = true)
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post {
                            _state.value = _state.value.copy(
                                isSpeaking = false,
                                speakingMessageId = null
                            )
                            if (shouldKeepWakeActive) {
                                scheduleRecognizerStart(manual = false, delayMs = 450L)
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post {
                            _state.value = _state.value.copy(
                                isSpeaking = false,
                                speakingMessageId = null
                            )
                            if (shouldKeepWakeActive) {
                                scheduleRecognizerStart(manual = false, delayMs = 450L)
                            }
                        }
                    }
                })
                ttsReady = true
            } else {
                Log.w(TAG, "TextToSpeech initialization failed with status $status")
            }
        }
    }

    fun startWakeWordMode() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.value = _state.value.copy(
                isSpeechAvailable = false,
                isWakeModeActive = false,
                voiceWarning = "Speech recognition service is unavailable on this device/emulator. Use text chat or enable Google Speech Services."
            )
            return
        }
        shouldKeepWakeActive = true
        _state.value = _state.value.copy(
            isWakeModeActive = true,
            isManualListening = false,
            isWakeTriggered = false,
            voiceWarning = null
        )
        if (!_state.value.isSpeaking) {
            scheduleRecognizerStart(manual = false, delayMs = 100L)
        }
    }

    fun stopWakeWordMode() {
        shouldKeepWakeActive = false
        isRestartScheduled = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            runCatching { speechRecognizer?.stopListening() }
            runCatching { speechRecognizer?.cancel() }
            _state.value = _state.value.copy(
                isWakeModeActive = false,
                isManualListening = false,
                isWakeTriggered = false,
                interimTranscript = ""
            )
        }
    }

    fun startManualListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.value = _state.value.copy(
                isSpeechAvailable = false,
                voiceWarning = "Microphone speech recognition is unavailable on this device."
            )
            return
        }
        stopSpeaking()
        _state.value = _state.value.copy(
            isManualListening = true,
            isWakeTriggered = true,
            interimTranscript = "",
            voiceWarning = null
        )
        scheduleRecognizerStart(manual = true, delayMs = 50L)
    }

    fun stopManualListening() {
        mainHandler.post {
            runCatching { speechRecognizer?.stopListening() }
            _state.value = _state.value.copy(
                isManualListening = false,
                isWakeTriggered = false,
                interimTranscript = ""
            )
        }
    }

    fun speakText(messageId: String, rawMarkdown: String) {
        if (!ttsReady || tts == null) {
            _state.value = _state.value.copy(
                voiceWarning = "Text-to-Speech engine is initializing or unavailable on this device."
            )
            return
        }
        // Pause wake listening while speaking so TTS doesn't trigger self-wake
        mainHandler.post {
            runCatching { speechRecognizer?.cancel() }
            val clean = stripMarkdownForSpeech(rawMarkdown)
            if (clean.isBlank()) return@post
            // Trim to safe TTS buffer length (~3500 chars) focusing on Voice Summary & key highlights
            val spokenText = if (clean.length > 3200) clean.take(3200) else clean
            _state.value = _state.value.copy(
                isSpeaking = true,
                speakingMessageId = messageId,
                voiceWarning = null
            )
            tts?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            runCatching { tts?.stop() }
            _state.value = _state.value.copy(
                isSpeaking = false,
                speakingMessageId = null
            )
            if (shouldKeepWakeActive) {
                scheduleRecognizerStart(manual = false, delayMs = 350L)
            }
        }
    }

    fun clearWarning() {
        _state.value = _state.value.copy(voiceWarning = null)
    }

    fun showPermissionDeniedWarning() {
        shouldKeepWakeActive = false
        _state.value = _state.value.copy(
            isWakeModeActive = false,
            isManualListening = false,
            voiceWarning = "Microphone permission was denied. Grant audio permission to use 'Hello Bhai' hands-free wake mode or the Mic button."
        )
    }

    private fun scheduleRecognizerStart(manual: Boolean, delayMs: Long) {
        if (isRestartScheduled && !manual) return
        isRestartScheduled = true
        mainHandler.postDelayed({
            isRestartScheduled = false
            startListeningInternal(manual)
        }, delayMs)
    }

    private fun startListeningInternal(manual: Boolean) {
        if (!manual && (!shouldKeepWakeActive || _state.value.isSpeaking)) return

        runCatching {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                setRecognitionListener(createListener(manual))
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "en-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        }.onFailure { err ->
            Log.e(TAG, "Failed to start SpeechRecognizer", err)
            _state.value = _state.value.copy(
                isManualListening = false,
                voiceWarning = "Could not start microphone listener: ${err.localizedMessage}"
            )
        }
    }

    private fun createListener(manual: Boolean) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {}
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {}

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (partial.isBlank()) return

            val wakeHit = if (!manual) WakePhraseMatcher.evaluate(partial).matched else true
            _state.value = _state.value.copy(
                interimTranscript = partial,
                isWakeTriggered = wakeHit || _state.value.isWakeTriggered
            )
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val transcript = matches.firstOrNull().orEmpty().trim()
            _state.value = _state.value.copy(interimTranscript = "")

            if (manual) {
                _state.value = _state.value.copy(
                    isManualListening = false,
                    isWakeTriggered = false
                )
                if (transcript.isNotBlank()) {
                    onVoiceQueryReady(transcript, true)
                }
                if (shouldKeepWakeActive && !_state.value.isSpeaking) {
                    scheduleRecognizerStart(manual = false, delayMs = 500L)
                }
                return
            }

            // Wake-word mode evaluation
            val eval = matches.firstNotNullOfOrNull { candidate ->
                val res = WakePhraseMatcher.evaluate(candidate)
                if (res.matched) res else null
            }

            if (eval != null) {
                _state.value = _state.value.copy(isWakeTriggered = true)
                onVoiceQueryReady(eval.followUpQuery, true)
                // Keep listening after a short pause unless TTS starts
                if (shouldKeepWakeActive && !_state.value.isSpeaking) {
                    scheduleRecognizerStart(manual = false, delayMs = 800L)
                }
            } else if (shouldKeepWakeActive && !_state.value.isSpeaking) {
                _state.value = _state.value.copy(isWakeTriggered = false)
                scheduleRecognizerStart(manual = false, delayMs = 350L)
            }
        }

        override fun onError(error: Int) {
            if (manual) {
                _state.value = _state.value.copy(
                    isManualListening = false,
                    isWakeTriggered = false,
                    interimTranscript = ""
                )
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    _state.value = _state.value.copy(
                        voiceWarning = mapSpeechError(error)
                    )
                }
                if (shouldKeepWakeActive && !_state.value.isSpeaking) {
                    scheduleRecognizerStart(manual = false, delayMs = 500L)
                }
                return
            }

            // Wake mode: auto-restart cleanly on silence/timeout/no-match
            _state.value = _state.value.copy(isWakeTriggered = false, interimTranscript = "")
            if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                showPermissionDeniedWarning()
            } else if (shouldKeepWakeActive && !_state.value.isSpeaking) {
                scheduleRecognizerStart(manual = false, delayMs = 550L)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun mapSpeechError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio capture error."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition network unavailable."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy; please try again."
        else -> "Could not recognize speech (code $error)."
    }

    fun release() {
        shouldKeepWakeActive = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching { speechRecognizer?.destroy() }
        runCatching { tts?.stop(); tts?.shutdown() }
    }

    companion object {
        fun stripMarkdownForSpeech(markdown: String): String {
            return markdown
                .replace(Regex("https?://\\S+"), " ")
                .replace(Regex("```[\\s\\S]*?```"), " ")
                .replace(Regex("[#*_`|>]+"), " ")
                .replace(Regex("\\[([^\\]]+)]\\([^)]+\\)"), "$1")
                .replace("₹", " rupees ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}

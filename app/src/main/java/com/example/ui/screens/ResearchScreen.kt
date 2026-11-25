package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.model.AiProvider
import com.example.data.model.ChatMessage
import com.example.data.model.GeminiVoiceMode
import com.example.data.model.NovaCartConstants
import com.example.data.model.ProductOpportunity
import com.example.ui.ResearchUiState
import com.example.ui.components.MarkdownReportView
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.DangerOnContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldOnContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestOutline
import com.example.ui.theme.ForestSlateDeep
import com.example.ui.theme.ForestSlateElevated
import com.example.ui.theme.ForestSlateSurface
import com.example.ui.theme.ObsidianEmerald
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import com.example.ui.theme.WarmGoldContainer

private enum class PendingMicAction {
    NONE,
    WAKE_MODE,
    MANUAL_MIC
}

@Composable
fun ResearchScreen(
    uiState: ResearchUiState,
    onSendPrompt: (String) -> Unit,
    onNewResearch: () -> Unit,
    onToggleLiveWeb: (Boolean) -> Unit,
    onToggleHandsFreeWake: (Boolean) -> Unit,
    onToggleVoiceReplies: (Boolean) -> Unit,
    onStartManualMic: () -> Unit,
    onStopManualMic: () -> Unit,
    onToggleSpeakMessage: (ChatMessage) -> Unit,
    onStopSpeech: () -> Unit,
    onMicPermissionDenied: () -> Unit,
    onDismissVoiceWarning: () -> Unit,
    onSaveProduct: (ProductOpportunity) -> Unit,
    onSaveFallbackReport: (ChatMessage) -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var inputText by rememberSaveable { mutableStateOf("") }
    var pendingMicAction by remember { mutableStateOf(PendingMicAction.NONE) }
    val listState = rememberLazyListState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            when (pendingMicAction) {
                PendingMicAction.WAKE_MODE -> onToggleHandsFreeWake(true)
                PendingMicAction.MANUAL_MIC -> onStartManualMic()
                PendingMicAction.NONE -> Unit
            }
        } else {
            onMicPermissionDenied()
        }
        pendingMicAction = PendingMicAction.NONE
    }

    fun requestMicAndRun(action: PendingMicAction) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            when (action) {
                PendingMicAction.WAKE_MODE -> onToggleHandsFreeWake(true)
                PendingMicAction.MANUAL_MIC -> onStartManualMic()
                PendingMicAction.NONE -> Unit
            }
        } else {
            pendingMicAction = action
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    val voice = uiState.voiceState

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianEmerald)
            .imePadding()
    ) {
        // Control Strip 1: Live Web & Market Research + New Research Action
        Surface(
            color = ForestSlateDeep,
            border = BorderStroke(0.5.dp, ForestOutline.copy(alpha = 0.4f))
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = if (uiState.settings.liveWebSearchEnabled) EmeraldPrimary else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Live Web & Market Research",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (uiState.settings.liveWebSearchEnabled) {
                                    "Active (${uiState.settings.activeProvider.shortName} grounding)"
                                } else {
                                    "Model knowledge only"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = uiState.settings.liveWebSearchEnabled,
                            onCheckedChange = onToggleLiveWeb,
                            modifier = Modifier.testTag("live_web_research_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianEmerald,
                                checkedTrackColor = EmeraldPrimary,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = ForestSlateElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = onNewResearch,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("new_research_button"),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ForestOutline),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddComment,
                            contentDescription = null,
                            tint = WarmGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "New",
                            style = MaterialTheme.typography.labelLarge,
                            color = WarmGold
                        )
                    }
                }

                // Control Strip 2: Hands-Free "Hello Bhai" Wake Mode & Spoken Voice Replies
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ForestSlateSurface.copy(alpha = 0.65f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (voice.isWakeModeActive) {
                                    onToggleHandsFreeWake(false)
                                } else {
                                    requestMicAndRun(PendingMicAction.WAKE_MODE)
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .testTag("wake_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (voice.isWakeModeActive) Icons.Default.GraphicEq else Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (voice.isWakeModeActive) WarmGold else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (voice.isWakeModeActive) "Wake: 'Hello Bhai' ON" else "Hands-Free 'Hello Bhai'",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (voice.isWakeModeActive) WarmGold else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (voice.isSpeaking) {
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable(onClick = onStopSpeech)
                                    .testTag("stop_speech_button"),
                                shape = RoundedCornerShape(8.dp),
                                color = DangerContainer,
                                border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop speech",
                                        tint = StatusDanger,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Stop Speech",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DangerOnContainer
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggleVoiceReplies(!uiState.settings.voiceRepliesEnabled) }
                                .testTag("voice_replies_toggle"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (uiState.settings.voiceRepliesEnabled) EmeraldContainer else ForestSlateDeep,
                            border = BorderStroke(
                                1.dp,
                                if (uiState.settings.voiceRepliesEnabled) EmeraldPrimary.copy(alpha = 0.6f) else ForestOutline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = null,
                                    tint = if (uiState.settings.voiceRepliesEnabled) EmeraldPrimary else TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (uiState.settings.voiceRepliesEnabled) "Voice Replies ON" else "Voice Off",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.settings.voiceRepliesEnabled) EmeraldPrimary else TextMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // Active Voice Listening Indicator ("Listening to you...")
        AnimatedVisibility(visible = voice.isWakeModeActive || voice.isManualListening) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
                    .testTag("voice_listening_banner"),
                shape = RoundedCornerShape(12.dp),
                color = if (voice.isWakeTriggered) WarmGoldContainer else EmeraldContainer.copy(alpha = 0.65f),
                border = BorderStroke(
                    1.dp,
                    if (voice.isWakeTriggered) WarmGold else EmeraldPrimary.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(if (voice.isWakeTriggered) WarmGold else EmeraldPrimary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when {
                                    voice.isManualListening -> "Listening to you..."
                                    voice.isWakeTriggered -> "Listening to you... ('Hello Bhai' heard)"
                                    else -> "Say \"Hello bhai batao product kya hai\""
                                },
                                style = MaterialTheme.typography.labelLarge,
                                color = if (voice.isWakeTriggered) WarmGold else TextPrimary
                            )
                            if (voice.interimTranscript.isNotBlank()) {
                                Text(
                                    text = "\"${voice.interimTranscript}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else if (uiState.settings.activeProvider == AiProvider.GEMINI &&
                                uiState.settings.geminiVoiceMode == GeminiVoiceMode.GEMINI_LIVE_FALLBACK
                            ) {
                                Text(
                                    text = "Gemini Live Voice mode • Indian English / Hinglish",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                    if (voice.isManualListening) {
                        IconButton(onClick = onStopManualMic) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop listening", tint = WarmGold)
                        }
                    }
                }
            }
        }

        // Non-blocking Voice Permission / Availability Alert
        AnimatedVisibility(visible = voice.voiceWarning != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                color = WarmGoldContainer,
                border = BorderStroke(1.dp, WarmGold.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.MicOff, contentDescription = null, tint = WarmGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = voice.voiceWarning.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissVoiceWarning) {
                        Icon(Icons.Default.Close, contentDescription = "Dismiss voice alert", tint = WarmGold)
                    }
                }
            }
        }

        // Readable AI / Network Error Banner
        AnimatedVisibility(visible = uiState.errorBanner != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("research_error_banner"),
                shape = RoundedCornerShape(12.dp),
                color = DangerContainer,
                border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = StatusDanger,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = uiState.errorBanner.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerOnContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissError,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss error",
                            tint = DangerOnContainer
                        )
                    }
                }
            }
        }

        // Main Chat / Discovery Feed
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (uiState.messages.isEmpty() && !uiState.isLoading) {
                ResearchEmptyState(
                    onQuickAction = { prompt ->
                        onSendPrompt(prompt)
                    },
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { msg ->
                        if (msg.role == "user") {
                            UserPromptBubble(message = msg)
                        } else {
                            AssistantResearchCard(
                                message = msg,
                                savedIds = uiState.savedIds,
                                isSpeakingThis = voice.isSpeaking && voice.speakingMessageId == msg.id,
                                onToggleSpeak = { onToggleSpeakMessage(msg) },
                                onSaveProduct = onSaveProduct,
                                onSaveFallbackReport = onSaveFallbackReport
                            )
                        }
                    }

                    if (uiState.isLoading) {
                        item(key = "loading_indicator") {
                            ResearchLoadingCard(
                                providerName = uiState.settings.activeProvider.displayName,
                                modelName = uiState.settings.currentModel()
                            )
                        }
                    }
                }
            }
        }

        // Quick Chips Strip when conversation is active
        if (uiState.messages.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NovaCartConstants.QUICK_CHIPS.forEach { (label, prompt) ->
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = !uiState.isLoading) { onSendPrompt(prompt) },
                        shape = RoundedCornerShape(50),
                        color = ForestSlateSurface,
                        border = BorderStroke(1.dp, ForestOutline)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Composer Input Bar with Manual Mic + Send Button
        Surface(
            color = ForestSlateDeep,
            tonalElevation = 4.dp,
            border = BorderStroke(0.5.dp, ForestOutline.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledIconButton(
                    onClick = {
                        if (voice.isManualListening) {
                            onStopManualMic()
                        } else {
                            requestMicAndRun(PendingMicAction.MANUAL_MIC)
                        }
                    },
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("manual_mic_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (voice.isManualListening) WarmGold else ForestSlateElevated,
                        contentColor = if (voice.isManualListening) ObsidianEmerald else EmeraldPrimary
                    )
                ) {
                    Icon(
                        imageVector = if (voice.isManualListening) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = if (voice.isManualListening) "Stop voice input" else "Speak product question"
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("research_prompt_input"),
                    placeholder = {
                        Text(
                            text = "Ask or say 'Hello bhai batao product kya hai'...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    maxLines = 4,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !uiState.isLoading) {
                                val text = inputText
                                inputText = ""
                                onSendPrompt(text)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ForestOutline,
                        focusedContainerColor = ForestSlateSurface,
                        unfocusedContainerColor = ForestSlateSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = WarmGold
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledIconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !uiState.isLoading) {
                            val text = inputText
                            inputText = ""
                            onSendPrompt(text)
                        }
                    },
                    enabled = inputText.isNotBlank() && !uiState.isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("send_research_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = WarmGold,
                        contentColor = ObsidianEmerald,
                        disabledContainerColor = ForestSlateElevated,
                        disabledContentColor = TextMuted
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send research prompt"
                    )
                }
            }
        }
    }
}

@Composable
private fun ResearchEmptyState(
    onQuickAction: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .widthIn(max = 680.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ForestSlateSurface),
                border = BorderStroke(1.dp, ForestOutline)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_research_banner_1795612749958),
                        contentDescription = "NovaCart India Market Intelligence Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(145.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        ObsidianEmerald.copy(alpha = 0.25f),
                                        ObsidianEmerald.copy(alpha = 0.92f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = WarmGoldContainer
                        ) {
                            Text(
                                text = "INDIA E-COMMERCE DESK • < ₹500 SOURCING",
                                style = MaterialTheme.typography.labelSmall,
                                color = WarmGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "NovaCart Intelligence",
                            style = MaterialTheme.typography.headlineMedium,
                            color = TextPrimary
                        )
                        Text(
                            text = "Say \"Hello bhai batao product kya hai\" or tap a mission below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "QUICK RESEARCH MISSIONS",
                style = MaterialTheme.typography.labelMedium,
                color = WarmGold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(NovaCartConstants.QUICK_CHIPS) { (title, prompt) ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onQuickAction(prompt) }
                    .testTag("quick_chip_${title.lowercase().replace(" ", "_")}"),
                shape = RoundedCornerShape(14.dp),
                color = ForestSlateSurface,
                border = BorderStroke(1.dp, ForestOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = EmeraldContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary
                        )
                    }
                    Text(
                        text = "Run →",
                        style = MaterialTheme.typography.labelLarge,
                        color = WarmGold
                    )
                }
            }
        }
    }
}

@Composable
private fun UserPromptBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = EmeraldContainer,
            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.45f))
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun AssistantResearchCard(
    message: ChatMessage,
    savedIds: Set<String>,
    isSpeakingThis: Boolean,
    onToggleSpeak: () -> Unit,
    onSaveProduct: (ProductOpportunity) -> Unit,
    onSaveFallbackReport: (ChatMessage) -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = ForestSlateSurface),
        border = BorderStroke(1.dp, ForestOutline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Provider badge header + Speak / Stop button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = WarmGoldContainer,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = "NOVACART ANALYST • ${message.providerLabel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarmGold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleSpeak)
                        .testTag("speak_button_${message.id}"),
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSpeakingThis) DangerContainer else ForestSlateDeep,
                    border = BorderStroke(
                        1.dp,
                        if (isSpeakingThis) StatusDanger.copy(alpha = 0.6f) else ForestOutline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isSpeakingThis) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isSpeakingThis) "Stop speaking" else "Speak report aloud",
                            tint = if (isSpeakingThis) StatusDanger else EmeraldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isSpeakingThis) "Stop" else "Speak",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSpeakingThis) DangerOnContainer else EmeraldPrimary
                        )
                    }
                }
            }

            // Formatted report body
            MarkdownReportView(markdown = message.content)

            // Clickable Web Grounding Citations
            if (message.citations.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "LIVE WEB CITATIONS (${message.citations.size})",
                            style = MaterialTheme.typography.labelSmall,
                            color = EmeraldPrimary
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        message.citations.forEach { citation ->
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        runCatching { uriHandler.openUri(citation.uri) }
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = ForestSlateDeep,
                                border = BorderStroke(1.dp, ForestOutline)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = citation.title,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 180.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Open source link",
                                        tint = WarmGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Detected Product Opportunity Cards with Working "Save to Database" button
            if (message.extractedProducts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "OPPORTUNITIES READY TO SAVE (${message.extractedProducts.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = WarmGold
                    )
                    message.extractedProducts.forEach { product ->
                        val isSaved = savedIds.contains(product.id)
                        ExtractedOpportunityCard(
                            product = product,
                            isSaved = isSaved,
                            onSaveClick = { onSaveProduct(product) }
                        )
                    }
                }
            } else if (message.content.length > 240) {
                val fallbackId = "report-${message.id}"
                val isSaved = savedIds.contains(fallbackId)
                OutlinedButton(
                    onClick = { if (!isSaved) onSaveFallbackReport(message) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("save_fallback_report_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isSaved) EmeraldPrimary else WarmGold)
                ) {
                    Icon(
                        imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                        contentDescription = null,
                        tint = if (isSaved) EmeraldPrimary else WarmGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSaved) "Saved to Database" else "Save Report to Database",
                        color = if (isSaved) EmeraldPrimary else WarmGold
                    )
                }
            }
        }
    }
}

@Composable
private fun ExtractedOpportunityCard(
    product: ProductOpportunity,
    isSaved: Boolean,
    onSaveClick: () -> Unit
) {
    val scoreColor = when {
        product.score >= 80 -> StatusSuccess
        product.score >= 70 -> WarmGold
        else -> StatusWarning
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = ForestSlateDeep,
        border = BorderStroke(1.dp, ForestOutline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${product.category} • ${product.trendDevelopment}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = scoreColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "${product.score}/100",
                        style = MaterialTheme.typography.labelMedium,
                        color = scoreColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetricMiniPill(
                    label = "Supplier",
                    value = "₹${product.supplierPriceInr.toInt()}",
                    valueColor = if (product.supplierPriceInr < 500.0) EmeraldPrimary else StatusDanger,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniPill(
                    label = "Target Sell",
                    value = "₹${product.sellingPriceInr.toInt()}",
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                MetricMiniPill(
                    label = "Gross Margin",
                    value = "${product.grossMarginPct}%",
                    valueColor = WarmGold,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Best Model: ${product.bestBusinessModel}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Button(
                onClick = onSaveClick,
                enabled = !isSaved,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("save_to_database_${product.id}"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSaved) EmeraldContainer else WarmGold,
                    contentColor = if (isSaved) EmeraldOnContainer else ObsidianEmerald,
                    disabledContainerColor = EmeraldContainer,
                    disabledContentColor = EmeraldPrimary
                )
            ) {
                Icon(
                    imageVector = if (isSaved) Icons.Default.BookmarkAdded else Icons.Default.BookmarkAdd,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSaved) "Saved to Database ✓" else "Save to Database",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun MetricMiniPill(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = ForestSlateSurface
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = valueColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ResearchLoadingCard(
    providerName: String,
    modelName: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = ForestSlateSurface,
        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = WarmGold,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Running 7-Stage-Gate India Research...",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Validating demand, <₹500 suppliers & unit economics via $providerName ($modelName)",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

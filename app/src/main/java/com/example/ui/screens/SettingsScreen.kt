package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.model.AiProvider
import com.example.data.model.GeminiVoiceMode
import com.example.ui.SettingsUiState
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.DangerOnContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestOutline
import com.example.ui.theme.ForestSlateDeep
import com.example.ui.theme.ForestSlateElevated
import com.example.ui.theme.ForestSlateSurface
import com.example.ui.theme.ObsidianEmerald
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import com.example.ui.theme.WarmGoldContainer

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onSelectActiveProvider: (AiProvider) -> Unit,
    onApiKeyChange: (AiProvider, String) -> Unit,
    onModelChange: (AiProvider, String) -> Unit,
    onTestConnection: (AiProvider) -> Unit,
    onToggleLiveWeb: (Boolean) -> Unit,
    onToggleVoiceReplies: (Boolean) -> Unit,
    onSelectGeminiVoiceMode: (GeminiVoiceMode) -> Unit,
    onClearAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable { mutableStateOf(uiState.settings.activeProvider) }
    var showKeyVisible by remember { mutableStateOf(false) }
    var modelDropdownOpen by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = ForestSlateElevated,
            title = { Text("Clear All NovaCart Data?", color = TextPrimary) },
            text = {
                Text(
                    "This will permanently delete all saved product opportunities, chat research history, and stored third-party API keys.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearDialog = false
                        onClearAllData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusDanger,
                        contentColor = ObsidianEmerald
                    )
                ) {
                    Text("Clear Everything")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianEmerald),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "AI Provider & Voice Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary
                )
                Text(
                    text = "Configure independent API keys, models, and Hinglish/English voice options.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Provider Tabs
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiProvider.entries.forEach { provider ->
                    val isTabSelected = selectedTab == provider
                    val isActive = uiState.settings.activeProvider == provider
                    Surface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                selectedTab = provider
                                showKeyVisible = false
                            }
                            .testTag("settings_tab_${provider.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTabSelected) EmeraldContainer else ForestSlateSurface,
                        border = BorderStroke(
                            width = if (isTabSelected) 1.5.dp else 1.dp,
                            color = if (isTabSelected) EmeraldPrimary else ForestOutline
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = provider.shortName,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isTabSelected) EmeraldPrimary else TextPrimary
                            )
                            if (isActive) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = WarmGoldContainer
                                ) {
                                    Text(
                                        text = "ACTIVE",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = WarmGold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Provider Configuration Card
        item {
            val provider = selectedTab
            val currentKey = uiState.settings.apiKeys[provider].orEmpty()
            val currentModel = uiState.settings.selectedModels[provider] ?: provider.defaultModel
            val currentModelLabel = provider.models.firstOrNull { it.id == currentModel }?.label ?: currentModel
            val isTesting = uiState.testingProvider == provider
            val feedback = uiState.testFeedback[provider]
            val isCurrentActive = uiState.settings.activeProvider == provider

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ForestSlateSurface),
                border = BorderStroke(1.dp, ForestOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = provider.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = provider.endpoint,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        if (!isCurrentActive) {
                            Button(
                                onClick = { onSelectActiveProvider(provider) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("activate_provider_${provider.id}"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = WarmGold,
                                    contentColor = ObsidianEmerald
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("Set Active", style = MaterialTheme.typography.labelLarge)
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = EmeraldContainer,
                                border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("Active Engine", style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
                                }
                            }
                        }
                    }

                    if (provider == AiProvider.GEMINI) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = EmeraldContainer.copy(alpha = 0.55f),
                            border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Built-in Gemini works automatically out-of-the-box via AI Studio / Firebase AI Logic. Leave the override key blank unless you want to use your own Gemini API key.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    // Masked API Key Input
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (provider == AiProvider.GEMINI) "Optional Custom Gemini API Key Override" else "${provider.displayName} API Key",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary
                        )
                        OutlinedTextField(
                            value = currentKey,
                            onValueChange = { onApiKeyChange(provider, it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("api_key_input_${provider.id}"),
                            placeholder = {
                                Text(provider.keyHint, color = TextMuted)
                            },
                            singleLine = true,
                            visualTransformation = if (showKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(
                                    onClick = { showKeyVisible = !showKeyVisible },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = if (showKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Toggle key visibility",
                                        tint = TextSecondary
                                    )
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = EmeraldPrimary,
                                unfocusedBorderColor = ForestOutline,
                                focusedContainerColor = ForestSlateDeep,
                                unfocusedContainerColor = ForestSlateDeep,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    // Model Selector Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Selected Model", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                        Box {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { modelDropdownOpen = true }
                                    .testTag("model_dropdown_${provider.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = ForestSlateDeep,
                                border = BorderStroke(1.dp, ForestOutline)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentModelLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        Icons.Default.ExpandMore,
                                        contentDescription = "Choose model",
                                        tint = WarmGold
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = modelDropdownOpen,
                                onDismissRequest = { modelDropdownOpen = false },
                                containerColor = ForestSlateElevated
                            ) {
                                provider.models.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label, color = TextPrimary) },
                                        onClick = {
                                            onModelChange(provider, option.id)
                                            modelDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Test Connection Button
                    OutlinedButton(
                        onClick = { onTestConnection(provider) },
                        enabled = !isTesting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("test_connection_${provider.id}"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, WarmGold)
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(
                                color = WarmGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing ${provider.shortName}...", color = WarmGold)
                        } else {
                            Text("Test Key & Connection", color = WarmGold)
                        }
                    }

                    if (feedback != null) {
                        val (ok, message) = feedback
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (ok) EmeraldContainer else DangerContainer,
                            border = BorderStroke(1.dp, if (ok) StatusSuccess.copy(alpha = 0.5f) else StatusDanger.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (ok) StatusSuccess else DangerOnContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Voice & Two-Way Speech Settings
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ForestSlateSurface),
                border = BorderStroke(1.dp, ForestOutline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = WarmGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Two-Way Voice & 'Hello Bhai' Mode", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Spoken Voice Replies", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                                Text(
                                    "Reads Hinglish/English Voice Summary aloud across all 4 AI providers",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = uiState.settings.voiceRepliesEnabled,
                            onCheckedChange = onToggleVoiceReplies,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ObsidianEmerald,
                                checkedTrackColor = EmeraldPrimary
                            )
                        )
                    }

                    HorizontalDivider(color = ForestOutline.copy(alpha = 0.5f))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = WarmGold, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Gemini Voice Engine Mode", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                        }
                        GeminiVoiceMode.entries.forEach { mode ->
                            val selected = uiState.settings.geminiVoiceMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectGeminiVoiceMode(mode) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { onSelectGeminiVoiceMode(mode) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = EmeraldPrimary,
                                        unselectedColor = TextMuted
                                    )
                                )
                                Column {
                                    Text(mode.label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                                    Text(
                                        text = if (mode == GeminiVoiceMode.STANDARD_READ_ALOUD) {
                                            "Works across Gemini, OpenAI, DeepSeek & Perplexity with Indian English/Hindi speech"
                                        } else {
                                            "Activates Gemini Live voice prompts with automatic Read-Aloud fallback"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Global Web Grounding Toggle
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ForestSlateSurface),
                border = BorderStroke(1.dp, ForestOutline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Live Web & Market Research",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Enables Google Search Grounding (Gemini) and live web citations (Perplexity)",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = uiState.settings.liveWebSearchEnabled,
                        onCheckedChange = onToggleLiveWeb,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ObsidianEmerald,
                            checkedTrackColor = EmeraldPrimary
                        )
                    )
                }
            }
        }

        // Clear All Data Danger Zone
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DangerContainer.copy(alpha = 0.35f)),
                border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Local Storage Management",
                        style = MaterialTheme.typography.titleMedium,
                        color = DangerOnContainer
                    )
                    Text(
                        text = "Clear all saved product records, chat transcripts, and API keys stored on this device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                    Button(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("clear_all_data_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusDanger,
                            contentColor = ObsidianEmerald
                        )
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All Data", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

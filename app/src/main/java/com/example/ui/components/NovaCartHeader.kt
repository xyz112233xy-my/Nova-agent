package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiProvider
import com.example.data.model.ConnectionStatus
import com.example.data.model.ProviderSettings
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestOutline
import com.example.ui.theme.ForestSlateDeep
import com.example.ui.theme.ForestSlateElevated
import com.example.ui.theme.ForestSlateSurface
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold
import com.example.ui.theme.WarmGoldContainer

@Composable
fun NovaCartTopHeader(
    settings: ProviderSettings,
    onProviderPillClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeProvider = settings.activeProvider
    val activeModel = settings.currentModel()
    val status = settings.currentStatus()
    val dotColor = when (status) {
        ConnectionStatus.BuiltInReady, ConnectionStatus.Connected -> StatusSuccess
        ConnectionStatus.Testing -> StatusWarning
        ConnectionStatus.KeyRequired, is ConnectionStatus.Error -> StatusDanger
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = ForestSlateDeep,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_novacart_logo_1795612749978),
                        contentDescription = "NovaCart Intelligence Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .border(1.dp, WarmGold.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "NOVACART",
                            style = MaterialTheme.typography.titleLarge,
                            color = WarmGold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "India Product Intelligence",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onProviderPillClick)
                        .testTag("provider_status_pill"),
                    shape = RoundedCornerShape(50),
                    color = ForestSlateElevated,
                    border = BorderStroke(1.dp, ForestOutline)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "${activeProvider.shortName} • ${status.badgeText()}",
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = activeModel,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ExpandMore,
                            contentDescription = "Switch AI Provider",
                            tint = WarmGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(ForestOutline.copy(alpha = 0.5f))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSwitchProviderSheet(
    settings: ProviderSettings,
    onSelectProvider: (AiProvider) -> Unit,
    onGoToSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ForestSlateDeep,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI Research Engine",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    Text(
                        text = "Switch provider anytime without losing your research",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                TextButton(
                    onClick = {
                        onDismiss()
                        onGoToSettings()
                    },
                    modifier = Modifier.testTag("sheet_configure_keys_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = WarmGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Keys", color = WarmGold)
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            AiProvider.entries.forEach { provider ->
                val isSelected = provider == settings.activeProvider
                val model = settings.selectedModels[provider] ?: provider.defaultModel
                val key = settings.apiKeys[provider].orEmpty()
                val statusText = when {
                    provider == AiProvider.GEMINI && key.isBlank() -> "Built-in Ready (No Key Needed)"
                    key.isNotBlank() -> "Key Saved • Ready"
                    else -> "API Key Required"
                }
                val isReady = provider == AiProvider.GEMINI || key.isNotBlank()

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectProvider(provider) }
                        .testTag("quick_switch_${provider.id}"),
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) EmeraldContainer else ForestSlateSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) EmeraldPrimary else ForestOutline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (provider == AiProvider.GEMINI) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = WarmGoldContainer
                                    ) {
                                        Text(
                                            text = "DEFAULT",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = WarmGold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$model • $statusText",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isReady) EmeraldPrimary else StatusWarning
                            )
                        }

                        Icon(
                            imageVector = if (isSelected) Icons.Default.CheckCircle else if (isReady) Icons.Default.CheckCircle else Icons.Default.Key,
                            contentDescription = null,
                            tint = if (isSelected) EmeraldPrimary else if (isReady) TextMuted else StatusWarning,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

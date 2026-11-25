package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.NovaCartConstants
import com.example.data.model.ProductOpportunity
import com.example.ui.components.MarkdownReportView
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestOutline
import com.example.ui.theme.ForestSlateDeep
import com.example.ui.theme.ForestSlateElevated
import com.example.ui.theme.ForestSlateSurface
import com.example.ui.theme.ObsidianEmerald
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarmGold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailSheet(
    product: ProductOpportunity,
    onUpdateStatus: (String, String) -> Unit,
    onDeleteProduct: (String) -> Unit,
    onCopiedReport: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    BackHandler { onDismiss() }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = ForestSlateElevated,
            title = { Text("Delete Opportunity?", color = TextPrimary) },
            text = {
                Text(
                    "Remove \"${product.name}\" from your saved NOVACART research database?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteProduct(product.id)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusDanger,
                        contentColor = ObsidianEmerald
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = ForestSlateDeep,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Discovered ${product.discoveryDate} • ${product.discoverySource}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close details", tint = TextSecondary)
                }
            }

            // Action Buttons: Copy Full Report + Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val fullText = buildFullClipboardReport(product)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("NovaCart Product Report", fullText))
                        onCopiedReport()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("copy_full_report_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarmGold,
                        contentColor = ObsidianEmerald
                    )
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Copy Full Report", style = MaterialTheme.typography.labelLarge)
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_product_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StatusDanger.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DangerContainer.copy(alpha = 0.4f),
                        contentColor = StatusDanger
                    )
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete product", modifier = Modifier.size(18.dp))
                }
            }

            // Unit Economics Grid
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ForestSlateSurface,
                border = BorderStroke(1.dp, ForestOutline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("UNIT ECONOMICS & SCORE", style = MaterialTheme.typography.labelMedium, color = WarmGold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailStat("Supplier Cost", "₹${product.supplierPriceInr.toInt()}", Modifier.weight(1f))
                        DetailStat("Selling Price", "₹${product.sellingPriceInr.toInt()}", Modifier.weight(1f))
                        DetailStat("Gross Margin", "${product.grossMarginPct}%", Modifier.weight(1f))
                        DetailStat("Net Profit", "₹${product.netProfitEstimateInr.toInt()}", Modifier.weight(1f))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        DetailStat("Score", "${product.score}/100", Modifier.weight(1f))
                        DetailStat("Trend", product.trendDevelopment, Modifier.weight(1f))
                        DetailStat("ID", product.id.take(8), Modifier.weight(1f))
                    }
                }
            }

            // Status Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("RESEARCH STATUS", style = MaterialTheme.typography.labelMedium, color = WarmGold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    NovaCartConstants.STATUSES.forEach { st ->
                        val selected = product.status == st
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateStatus(product.id, st) },
                            label = { Text(st, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldContainer,
                                selectedLabelColor = EmeraldPrimary,
                                containerColor = ForestSlateSurface,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = ForestOutline,
                                selectedBorderColor = EmeraldPrimary
                            )
                        )
                    }
                }
            }

            // All Part C7 Fields
            DetailFieldCard("Best Business Model", product.bestBusinessModel)
            DetailFieldCard("Demand Indicators", product.demandIndicators)
            DetailFieldCard("Competitor Summary", product.competitorSummary)
            DetailFieldCard("Product Links (Marketplaces)", product.productLinks)
            DetailFieldCard("Supplier Links (IndiaMART / Udaan)", product.supplierLinks)
            DetailFieldCard("Final Recommendation", product.finalRecommendation)

            HorizontalDivider(color = ForestOutline)

            Text("FULL AGENT RESEARCH REPORT", style = MaterialTheme.typography.labelMedium, color = WarmGold)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = ForestSlateSurface,
                border = BorderStroke(1.dp, ForestOutline)
            ) {
                MarkdownReportView(
                    markdown = product.rawReport,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
        Text(value, style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DetailFieldCard(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = ForestSlateSurface,
        border = BorderStroke(0.5.dp, ForestOutline)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = EmeraldPrimary)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        }
    }
}

private fun buildFullClipboardReport(p: ProductOpportunity): String = buildString {
    appendLine("NOVACART INTELLIGENCE — PRODUCT DOSSIER")
    appendLine("==========================================")
    appendLine("ID: ${p.id}")
    appendLine("Product: ${p.name}")
    appendLine("Category: ${p.category}")
    appendLine("Status: ${p.status} | Score: ${p.score}/100 | Trend: ${p.trendDevelopment}")
    appendLine("Discovery Date: ${p.discoveryDate} | Source: ${p.discoverySource}")
    appendLine("Supplier Price: ₹${p.supplierPriceInr} | Selling Price: ₹${p.sellingPriceInr}")
    appendLine("Gross Margin: ${p.grossMarginPct}% | Est. Net Profit: ₹${p.netProfitEstimateInr}")
    appendLine("Best Business Model: ${p.bestBusinessModel}")
    appendLine("Demand Indicators: ${p.demandIndicators}")
    appendLine("Competitor Summary: ${p.competitorSummary}")
    appendLine("Product Links: ${p.productLinks}")
    appendLine("Supplier Links: ${p.supplierLinks}")
    appendLine("Final Recommendation: ${p.finalRecommendation}")
    appendLine()
    appendLine("--- FULL RESEARCH REPORT ---")
    appendLine(p.rawReport)
}

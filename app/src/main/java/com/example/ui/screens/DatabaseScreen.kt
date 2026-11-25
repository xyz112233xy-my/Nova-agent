package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.NovaCartConstants
import com.example.data.model.ProductOpportunity
import com.example.ui.DatabaseSortOption
import com.example.ui.DatabaseUiState
import com.example.ui.theme.EmeraldContainer
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

@Composable
fun DatabaseScreen(
    uiState: DatabaseUiState,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onSortChange: (DatabaseSortOption) -> Unit,
    onProductClick: (ProductOpportunity) -> Unit,
    onBuildCsv: () -> String,
    onExportFeedback: (String) -> Unit,
    onGoToResearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sortMenuOpen by remember { mutableStateOf(false) }
    var statusMenuOpen by remember { mutableStateOf(false) }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            runCatching {
                val csvData = onBuildCsv()
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(csvData.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                onExportFeedback("Exported ${uiState.allProducts.size} records to CSV.")
            }.onFailure { err ->
                onExportFeedback("CSV export failed: ${err.localizedMessage}")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianEmerald)
    ) {
        // Header Controls: Search + Export CSV
        Surface(
            color = ForestSlateDeep,
            border = BorderStroke(0.5.dp, ForestOutline.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Research Database",
                            style = MaterialTheme.typography.headlineSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = "${uiState.allProducts.size} saved Indian product opportunities",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }

                    Button(
                        onClick = { csvLauncher.launch("novacart_research_database.csv") },
                        enabled = uiState.allProducts.isNotEmpty(),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("export_csv_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = WarmGold,
                            contentColor = ObsidianEmerald,
                            disabledContainerColor = ForestSlateElevated,
                            disabledContentColor = TextMuted
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV", style = MaterialTheme.typography.labelLarge)
                    }
                }

                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = onSearchChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("database_search_input"),
                    placeholder = {
                        Text(
                            text = "Search saved products by name or model...",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextMuted
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = ForestOutline,
                        focusedContainerColor = ForestSlateSurface,
                        unfocusedContainerColor = ForestSlateSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Category filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val categories = listOf("All") + NovaCartConstants.CATEGORIES
                    categories.forEach { category ->
                        val selected = uiState.selectedCategory == category
                        FilterChip(
                            selected = selected,
                            onClick = { onCategoryChange(category) },
                            label = { Text(category, style = MaterialTheme.typography.labelMedium) },
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

                // Status filter & Sort row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { statusMenuOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("status_filter_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ForestOutline)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = WarmGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Status: ${uiState.selectedStatus}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = statusMenuOpen,
                            onDismissRequest = { statusMenuOpen = false },
                            containerColor = ForestSlateElevated
                        ) {
                            (listOf("All") + NovaCartConstants.STATUSES).forEach { st ->
                                DropdownMenuItem(
                                    text = { Text(st, color = TextPrimary) },
                                    onClick = {
                                        onStatusChange(st)
                                        statusMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { sortMenuOpen = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("sort_options_button"),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ForestOutline)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = uiState.sortOption.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = sortMenuOpen,
                            onDismissRequest = { sortMenuOpen = false },
                            containerColor = ForestSlateElevated
                        ) {
                            DatabaseSortOption.entries.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.label, color = TextPrimary) },
                                    onClick = {
                                        onSortChange(opt)
                                        sortMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Body
        if (uiState.allProducts.isEmpty()) {
            DatabaseEmptyState(
                onGoToResearch = onGoToResearch,
                modifier = Modifier.fillMaxSize()
            )
        } else if (uiState.filteredProducts.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No saved products match your current search or filters.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.filteredProducts, key = { it.id }) { product ->
                    SavedProductCard(
                        product = product,
                        onClick = { onProductClick(product) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DatabaseEmptyState(
    onGoToResearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = ForestSlateSurface,
            border = BorderStroke(1.dp, ForestOutline),
            modifier = Modifier.size(72.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Inventory2,
                    contentDescription = null,
                    tint = WarmGold,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Saved Opportunities Yet",
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Open the Research tab to discover <₹500 Indian e-commerce products, then tap \"Save to Database\" on any shortlisted opportunity.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onGoToResearch,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("empty_db_go_to_research"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = EmeraldPrimary,
                contentColor = ObsidianEmerald
            )
        ) {
            Icon(
                imageVector = Icons.Default.TravelExplore,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Go to Research Tab", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun SavedProductCard(
    product: ProductOpportunity,
    onClick: () -> Unit
) {
    val scoreColor = when {
        product.score >= 80 -> StatusSuccess
        product.score >= 70 -> WarmGold
        else -> StatusWarning
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("saved_product_card_${product.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ForestSlateSurface),
        border = BorderStroke(1.dp, ForestOutline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(status = product.status)
                        Text(
                            text = product.discoveryDate,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldPrimary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = scoreColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, scoreColor.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${product.score}",
                            style = MaterialTheme.typography.titleMedium,
                            color = scoreColor,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/100",
                            style = MaterialTheme.typography.labelSmall,
                            color = scoreColor.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DbMetricTile(
                    label = "Supplier Cost",
                    value = "₹${product.supplierPriceInr.toInt()}",
                    valueColor = if (product.supplierPriceInr < 500.0) EmeraldPrimary else StatusDanger,
                    modifier = Modifier.weight(1f)
                )
                DbMetricTile(
                    label = "Target Sell",
                    value = "₹${product.sellingPriceInr.toInt()}",
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                DbMetricTile(
                    label = "Net Profit",
                    value = "₹${product.netProfitEstimateInr.toInt()}",
                    valueColor = WarmGold,
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = product.bestBusinessModel,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun StatusBadge(status: String) {
    val color = when (status) {
        "Approved for sourcing", "Ready for testing" -> StatusSuccess
        "Promising", "Newly discovered" -> WarmGold
        "Under investigation" -> EmeraldPrimary
        else -> StatusDanger
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun DbMetricTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = ForestSlateDeep
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = TextMuted)
            Text(value, style = MaterialTheme.typography.labelLarge, color = valueColor, fontWeight = FontWeight.Bold)
        }
    }
}

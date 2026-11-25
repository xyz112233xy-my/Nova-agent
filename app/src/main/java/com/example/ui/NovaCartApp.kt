package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TravelExplore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.NovaCartTopHeader
import com.example.ui.components.QuickSwitchProviderSheet
import com.example.ui.screens.DatabaseScreen
import com.example.ui.screens.ProductDetailSheet
import com.example.ui.screens.ResearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.ForestSlateDeep
import com.example.ui.theme.ObsidianEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.WarmGold

private object NovaCartRoutes {
    const val RESEARCH = "research"
    const val DATABASE = "database"
    const val SETTINGS = "settings"
}

private data class NavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val NAV_TABS = listOf(
    NavTab(NovaCartRoutes.RESEARCH, "Research", Icons.Filled.TravelExplore, Icons.Outlined.TravelExplore),
    NavTab(NovaCartRoutes.DATABASE, "Database", Icons.Filled.Inventory2, Icons.Outlined.Inventory2),
    NavTab(NovaCartRoutes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun NovaCartApp(
    viewModel: NovaCartViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NovaCartRoutes.RESEARCH

    val researchState by viewModel.researchUiState.collectAsStateWithLifecycle()
    val databaseState by viewModel.databaseUiState.collectAsStateWithLifecycle()
    val settingsState by viewModel.settingsUiState.collectAsStateWithLifecycle()
    val selectedProduct by viewModel.selectedProduct.collectAsStateWithLifecycle()
    val isProviderSheetOpen by viewModel.isProviderSheetOpen.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navigateToTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (isProviderSheetOpen) {
        QuickSwitchProviderSheet(
            settings = settingsState.settings,
            onSelectProvider = viewModel::setActiveProvider,
            onGoToSettings = { navigateToTab(NovaCartRoutes.SETTINGS) },
            onDismiss = viewModel::closeProviderSheet
        )
    }

    selectedProduct?.let { product ->
        ProductDetailSheet(
            product = product,
            onUpdateStatus = viewModel::updateProductStatus,
            onDeleteProduct = viewModel::deleteProduct,
            onCopiedReport = { viewModel.notifyMessage("Full report copied to clipboard") },
            onDismiss = viewModel::closeProductDetail
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianEmerald,
            contentWindowInsets = WindowInsets.navigationBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                NovaCartTopHeader(
                    settings = settingsState.settings,
                    onProviderPillClick = viewModel::openProviderSheet
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        containerColor = ForestSlateDeep,
                        contentColor = TextPrimary,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NAV_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                modifier = Modifier.testTag("nav_tab_${tab.route}"),
                                icon = {
                                    val count = if (tab.route == NovaCartRoutes.DATABASE) databaseState.allProducts.size else 0
                                    if (count > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge(
                                                    containerColor = WarmGold,
                                                    contentColor = ObsidianEmerald
                                                ) {
                                                    Text(count.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = ForestSlateDeep,
                        contentColor = TextPrimary
                    ) {
                        NAV_TABS.forEach { tab ->
                            val selected = currentRoute == tab.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                modifier = Modifier.testTag("rail_tab_${tab.route}"),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EmeraldPrimary,
                                    selectedTextColor = EmeraldPrimary,
                                    indicatorColor = EmeraldContainer,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    NavHost(
                        navController = navController,
                        startDestination = NovaCartRoutes.RESEARCH
                    ) {
                        composable(NovaCartRoutes.RESEARCH) {
                            ResearchScreen(
                                uiState = researchState,
                                onSendPrompt = { viewModel.sendPrompt(it, fromVoice = false) },
                                onNewResearch = viewModel::startNewResearch,
                                onToggleLiveWeb = viewModel::setLiveWebSearch,
                                onToggleHandsFreeWake = viewModel::toggleHandsFreeWake,
                                onToggleVoiceReplies = viewModel::toggleVoiceReplies,
                                onStartManualMic = viewModel::startManualMic,
                                onStopManualMic = viewModel::stopManualMic,
                                onToggleSpeakMessage = viewModel::toggleSpeakMessage,
                                onStopSpeech = viewModel::stopSpeech,
                                onMicPermissionDenied = viewModel::onMicPermissionDenied,
                                onDismissVoiceWarning = viewModel::dismissVoiceWarning,
                                onSaveProduct = viewModel::saveProductOpportunity,
                                onSaveFallbackReport = viewModel::saveMessageReportAsOpportunity,
                                onDismissError = viewModel::dismissErrorBanner
                            )
                        }
                        composable(NovaCartRoutes.DATABASE) {
                            DatabaseScreen(
                                uiState = databaseState,
                                onSearchChange = viewModel::setSearchQuery,
                                onCategoryChange = viewModel::setCategoryFilter,
                                onStatusChange = viewModel::setStatusFilter,
                                onSortChange = viewModel::setSortOption,
                                onProductClick = viewModel::openProductDetail,
                                onBuildCsv = viewModel::buildCsvString,
                                onExportFeedback = viewModel::notifyMessage,
                                onGoToResearch = { navigateToTab(NovaCartRoutes.RESEARCH) }
                            )
                        }
                        composable(NovaCartRoutes.SETTINGS) {
                            SettingsScreen(
                                uiState = settingsState,
                                onSelectActiveProvider = viewModel::setActiveProvider,
                                onApiKeyChange = viewModel::setApiKey,
                                onModelChange = viewModel::setModel,
                                onTestConnection = viewModel::testProviderConnection,
                                onToggleLiveWeb = viewModel::setLiveWebSearch,
                                onToggleVoiceReplies = viewModel::toggleVoiceReplies,
                                onSelectGeminiVoiceMode = viewModel::setGeminiVoiceMode,
                                onClearAllData = viewModel::clearAllData
                            )
                        }
                    }
                }
            }
        }
    }
}

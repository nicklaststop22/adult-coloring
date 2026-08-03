package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.models.ColoringPage
import com.example.ui.components.AdMobBanner
import com.example.ui.components.InterstitialAdDialog
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.InspirationScreen
import com.example.ui.screens.ProUpgradeScreen
import com.example.ui.theme.GoldProAccent
import com.example.ui.viewmodel.ColoringViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Create : Screen("create", "Create", Icons.Default.AddCircle)
    object Gallery : Screen("gallery", "Gallery", Icons.Default.Collections)
    object Inspiration : Screen("inspiration", "Ideas", Icons.Default.Lightbulb)
    object ProUpgrade : Screen("pro_upgrade", "Pro Pass", Icons.Default.Star)
}

@Composable
fun InkZenApp(viewModel: ColoringViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val allPages by viewModel.allPages.collectAsState()
    val favoritePages by viewModel.favoritePages.collectAsState()

    var currentScreen by remember { mutableStateOf<Screen>(Screen.Create) }
    var selectedDetailPage by remember { mutableStateOf<ColoringPage?>(null) }

    // Interstitial Ad Dialog trigger when generating on free tier
    if (uiState.showAdDialog) {
        InterstitialAdDialog(
            onAdDismissed = { viewModel.onAdDismissed() },
            onRemoveAdsClick = {
                viewModel.onAdDismissed()
                currentScreen = Screen.ProUpgrade
            }
        )
    }

    Scaffold(
        bottomBar = {
            if (selectedDetailPage == null) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.testTag("main_bottom_nav_bar")
                ) {
                    val navItems = listOf(
                        Screen.Create,
                        Screen.Gallery,
                        Screen.Inspiration,
                        Screen.ProUpgrade
                    )

                    navItems.forEach { screen ->
                        val isSelected = currentScreen == screen
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = screen },
                            icon = {
                                Icon(
                                    imageVector = screen.icon,
                                    contentDescription = screen.title,
                                    tint = if (screen == Screen.ProUpgrade && !uiState.isProUser) GoldProAccent
                                    else if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.testTag("nav_item_${screen.route}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        if (selectedDetailPage != null) {
            DetailScreen(
                page = selectedDetailPage!!,
                onBackClick = { selectedDetailPage = null },
                onToggleFavorite = { viewModel.toggleFavorite(it) }
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                // Main Content Screen
                Box(modifier = Modifier.weight(1f)) {
                    when (currentScreen) {
                        is Screen.Create -> {
                            CreateScreen(
                                prompt = uiState.prompt,
                                selectedCategory = uiState.selectedCategory,
                                selectedStyle = uiState.selectedStyle,
                                batchCount = uiState.batchCount,
                                isGenerating = uiState.isGenerating,
                                cooldownRemainingSeconds = uiState.cooldownRemainingSeconds,
                                isProUser = uiState.isProUser,
                                generationError = uiState.generationError,
                                categories = viewModel.categories,
                                styles = viewModel.styles,
                                onPromptChange = { viewModel.updatePrompt(it) },
                                onCategorySelect = { viewModel.selectCategory(it) },
                                onStyleSelect = { viewModel.selectStyle(it) },
                                onBatchCountChange = { viewModel.setBatchCount(it) },
                                onGenerateClick = { viewModel.initiateGeneration() },
                                onUpgradeClick = { currentScreen = Screen.ProUpgrade },
                                onViewGeneratedPage = { pageId ->
                                    selectedDetailPage = allPages.find { it.id == pageId }
                                }
                            )
                        }
                        is Screen.Gallery -> {
                            GalleryScreen(
                                pages = allPages,
                                favoritePages = favoritePages,
                                onPageClick = { selectedDetailPage = it },
                                onToggleFavorite = { viewModel.toggleFavorite(it) },
                                onDeletePage = { viewModel.deletePage(it) },
                                onCreateNewClick = { currentScreen = Screen.Create }
                            )
                        }
                        is Screen.Inspiration -> {
                            InspirationScreen(
                                categories = viewModel.categories,
                                onSelectPrompt = { promptText, categoryName ->
                                    viewModel.applyPresetPrompt(promptText)
                                    viewModel.selectCategory(categoryName)
                                    currentScreen = Screen.Create
                                }
                            )
                        }
                        is Screen.ProUpgrade -> {
                            ProUpgradeScreen(
                                isProUser = uiState.isProUser,
                                onBuyProductClick = { productId -> viewModel.buyProProduct(productId) },
                                onRestoreClick = { viewModel.buyProProduct("restore") },
                                onToggleProSimulator = { viewModel.toggleProModeSimulation() }
                            )
                        }
                    }
                }

                // Anchored AdMob Banner for Free Tier Users
                AdMobBanner(
                    isProUser = uiState.isProUser,
                    onUpgradeClick = { currentScreen = Screen.ProUpgrade }
                )
            }
        }
    }
}

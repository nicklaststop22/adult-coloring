package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.CooldownCheckResult
import com.example.data.local.UserPreferencesRepository
import com.example.data.models.CategoryPreset
import com.example.data.models.ColoringPage
import com.example.data.models.StylePreset
import com.example.data.monetization.BillingManager
import com.example.data.remote.GeminiImageService
import com.example.data.repository.ColoringRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ColoringUiState(
    val prompt: String = "",
    val selectedCategory: String = "Neon Paranoia & Surveillance",
    val selectedStyle: String = "Intricate Detail",
    val batchCount: Int = 1,
    val isGenerating: Boolean = false,
    val generationError: String? = null,
    val showAdDialog: Boolean = false,
    val cooldownRemainingSeconds: Int = 0,
    val isProUser: Boolean = false,
    val newlyGeneratedPages: List<ColoringPage> = emptyList(),
    val billingMessage: String? = null
)

class ColoringViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val userPrefs = UserPreferencesRepository(application)
    val geminiService = GeminiImageService()
    val repository = ColoringRepository(application, db.coloringPageDao(), userPrefs, geminiService)
    val billingManager = BillingManager(application, userPrefs)

    val allPages: StateFlow<List<ColoringPage>> = repository.allPages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoritePages: StateFlow<List<ColoringPage>> = repository.favoritePages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _uiState = MutableStateFlow(ColoringUiState())
    val uiState: StateFlow<ColoringUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    val categories = listOf(
        CategoryPreset(
            id = "neon_paranoia",
            name = "Neon Paranoia & Surveillance",
            iconName = "videocam",
            samplePrompts = listOf(
                "Cybernetic security camera drone watching over a rain-slicked neon street, line art",
                "Grid of CRT monitors with glitching biometric facial scanners, detailed line drawing",
                "Mechanical eye lens with wiring and security laser grids, zentangle coloring page"
            )
        ),
        CategoryPreset(
            id = "garage_chopper",
            name = "3AM Garage & Chopper Breakdown",
            iconName = "build",
            samplePrompts = listOf(
                "Disassembled V-twin motorcycle engine with tools spread on shop floor, line art",
                "Custom chopper motorcycle parked under a dim garage lamp at midnight, detailed line art",
                "Piston skull emblem with wrenches, spark plugs, and exhaust pipe flourishes"
            )
        ),
        CategoryPreset(
            id = "wasteland_scrapper",
            name = "Wasteland & Scrapper Art",
            iconName = "warning",
            samplePrompts = listOf(
                "Post-apocalyptic armored dune buggy built from scrap metal and spikes, line art",
                "Mad scrapper wearing gas mask and spiked shoulder pads in desert wasteland, coloring page",
                "Rusty mech suit assembled from junk auto parts and iron chains, detailed line drawing"
            )
        ),
        CategoryPreset(
            id = "metal_horror",
            name = "Heavy Metal & Fantasy Horror",
            iconName = "dark_mode",
            samplePrompts = listOf(
                "Intricate gothic skull adorned with dark roses, horns, and ancient runes, line art",
                "Demonic guitar hero shredding in flames with skeleton crowd, line art",
                "Dark fantasy necromancer summoning spectral dragons, detailed black ink drawing"
            )
        ),
        CategoryPreset(
            id = "cyberpunk_dystopia",
            name = "Dark Cyberpunk & Dystopian Sci-Fi",
            iconName = "memory",
            samplePrompts = listOf(
                "Cyborg samurai with exposed fiber-optic cables and glowing katana, line art",
                "Futuristic mega-city skyline with towering megastructures and flying vehicles, line drawing",
                "Android hacker with VR visor wired into a high-tech mainframe, coloring page"
            )
        ),
        CategoryPreset(
            id = "weird_lowbrow",
            name = "Weird, Surreal, & Lowbrow Art",
            iconName = "psychology",
            samplePrompts = listOf(
                "Surreal hot rod cartoon monster driving with giant eyeballs and popping exhaust, line art",
                "Melting clock landscape with flying fish and bizarre floating geometric shapes, line art",
                "Lowbrow Kustom Kulture creature eating a hamburger on a skateboard, zentangle line drawing"
            )
        ),
        CategoryPreset(
            id = "ink_curves",
            name = "Ink & Curves (Classic Pinup & Lowbrow)",
            iconName = "brush",
            samplePrompts = listOf(
                "Vintage rockabilly pinup girl sitting on a classic car hood, line art",
                "Classic sailor tattoo pinup with anchor, roses, and banner, detailed line drawing",
                "Retro 1950s style pinup portrait with victory curls and dice flourish, line art"
            )
        ),
        CategoryPreset(
            id = "colorful_language",
            name = "Colorful Language",
            iconName = "format_quote",
            samplePrompts = listOf(
                "Sarcastic adult typography motto surrounded by intricate mandala and floral wreathes, line art",
                "Bold expressive humor phrase surrounded by ornate botanical vines and zentangle flourishes, line drawing",
                "Hilarious sassy quote framed by Victorian filigree, ribbon banners, and decorative scrollwork, coloring page"
            )
        )
    )

    val styles = listOf(
        StylePreset(
            id = "intricate",
            name = "Intricate Detail",
            description = "High detail, fine lines for relaxing focus",
            promptModifier = "Ultra-intricate fine line art, high detail"
        ),
        StylePreset(
            id = "bold",
            name = "Bold Outlines",
            description = "Thick dark borders, easy coloring spaces",
            promptModifier = "Bold dark vector outlines, clean large spaces"
        ),
        StylePreset(
            id = "stained_glass",
            name = "Stained Glass",
            description = "Segmented faceted geometric cell structure",
            promptModifier = "Stained glass window mosaic lines"
        ),
        StylePreset(
            id = "zentangle",
            name = "Fine Zentangle",
            description = "Meditative repetitive zen pattern shading",
            promptModifier = "Detailed zentangle doodle line art pattern"
        ),
        StylePreset(
            id = "minimalist",
            name = "Minimalist Line Art",
            description = "Modern simple aesthetic, generous space",
            promptModifier = "Clean minimalist vector line drawing"
        )
    )

    init {
        // Observe Pro status
        viewModelScope.launch {
            userPrefs.isProUser.collectLatest { isPro ->
                _uiState.update { it.copy(isProUser = isPro) }
            }
        }

        startCooldownTicker()
    }

    private fun startCooldownTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                val remaining = userPrefs.getRemainingCooldownSeconds()
                _uiState.update { it.copy(cooldownRemainingSeconds = remaining) }
                delay(1000L)
            }
        }
    }

    fun updatePrompt(newPrompt: String) {
        _uiState.update { it.copy(prompt = newPrompt, generationError = null) }
    }

    fun selectCategory(catName: String) {
        _uiState.update { it.copy(selectedCategory = catName) }
    }

    fun selectStyle(styleName: String) {
        _uiState.update { it.copy(selectedStyle = styleName) }
    }

    fun setBatchCount(count: Int) {
        _uiState.update { it.copy(batchCount = count) }
    }

    fun applyPresetPrompt(promptText: String) {
        _uiState.update { it.copy(prompt = promptText) }
    }

    fun initiateGeneration() {
        val currentState = _uiState.value
        val promptText = if (currentState.prompt.isBlank()) {
            categories.find { it.name == currentState.selectedCategory }?.samplePrompts?.random()
                ?: "Intricate mandala"
        } else currentState.prompt

        val checkResult = userPrefs.canGenerate(currentState.batchCount)
        when (checkResult) {
            is CooldownCheckResult.CooldownActive -> {
                _uiState.update {
                    it.copy(
                        generationError = "Free tier limit: Please wait ${checkResult.secondsRemaining}s or Upgrade to Pro to skip cooldown."
                    )
                }
                return
            }
            is CooldownCheckResult.ProRequiredForBatch -> {
                _uiState.update {
                    it.copy(
                        generationError = "Batch generation up to 5 pages requires ColoringForge Pro!"
                    )
                }
                return
            }
            is CooldownCheckResult.BatchLimitExceeded -> {
                _uiState.update {
                    it.copy(
                        generationError = "Maximum batch size is 5 pages."
                    )
                }
                return
            }
            is CooldownCheckResult.Allowed -> {
                _uiState.update { it.copy(generationError = null) }
                // For Free tier users, show AdMob Interstitial Ad before executing generation
                if (!currentState.isProUser) {
                    _uiState.update { it.copy(showAdDialog = true) }
                } else {
                    executeGeneration(promptText)
                }
            }
        }
    }

    fun onAdDismissed() {
        _uiState.update { it.copy(showAdDialog = false) }
        val currentState = _uiState.value
        val promptText = if (currentState.prompt.isBlank()) {
            categories.find { it.name == currentState.selectedCategory }?.samplePrompts?.random()
                ?: "Intricate mandala"
        } else currentState.prompt

        executeGeneration(promptText)
    }

    private fun executeGeneration(promptText: String) {
        val currentState = _uiState.value
        _uiState.update { it.copy(isGenerating = true, generationError = null) }

        viewModelScope.launch {
            try {
                val pages = repository.generatePages(
                    prompt = promptText,
                    category = currentState.selectedCategory,
                    styleName = currentState.selectedStyle,
                    batchCount = if (currentState.isProUser) currentState.batchCount else 1
                )
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        newlyGeneratedPages = pages
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        generationError = e.localizedMessage ?: "Failed to generate coloring page"
                    )
                }
            }
        }
    }

    fun toggleFavorite(page: ColoringPage) {
        viewModelScope.launch {
            repository.toggleFavorite(page.id, !page.isFavorite)
        }
    }

    fun deletePage(page: ColoringPage) {
        viewModelScope.launch {
            repository.deletePage(page)
        }
    }

    fun toggleProModeSimulation() {
        val currentPro = userPrefs.isProUser.value
        userPrefs.setProUser(!currentPro)
    }

    fun buyProProduct(productId: String) {
        viewModelScope.launch {
            billingManager.launchBillingFlow(productId)
            _uiState.update { it.copy(billingMessage = "Welcome to ColoringForge Pro!") }
        }
    }

    fun clearBillingMessage() {
        _uiState.update { it.copy(billingMessage = null) }
    }
}

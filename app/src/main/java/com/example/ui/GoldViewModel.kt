package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class GoldViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "GoldViewModel"
    private val database = GoldDatabase.getDatabase(application)
    private val repository = GoldRepository(database.goldDao())

    // --- State Expositions ---
    val allItems: StateFlow<List<GoldItem>> = repository.allItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allPrices: StateFlow<List<GoldPrice>> = repository.allPrices.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val marketSettings: StateFlow<List<MarketSetting>> = repository.allMarketSettings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val globalMarketData: StateFlow<GlobalMarketEntity?> = repository.globalMarketData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Logged-in state
    private val _isAppLocked = MutableStateFlow(true)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Loading & Refresh state
    private val _isRefreshingPrices = MutableStateFlow(false)
    val isRefreshingPrices: StateFlow<Boolean> = _isRefreshingPrices.asStateFlow()

    private val _priceRefreshStatus = MutableSharedFlow<String>()
    val priceRefreshStatus = _priceRefreshStatus.asSharedFlow()

    init {
        viewModelScope.launch {
            // First, load offline defaults so the DB is immediately functional
            repository.initializeOfflineDefaults()
            
            // Next, perform automated live gold price fetch on startup
            refreshGoldPrices()
        }
        
        // Check if biometric is available. If not, auto unlock or let them unlock
        if (!BiometricPromptManager.isBiometricAvailable(application)) {
            _isAppLocked.value = false
        }
    }

    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    fun isBiometricSettingActive(): Boolean {
        // Biometrics are activated by default for financial safety
        return BiometricPromptManager.isBiometricAvailable(getApplication())
    }

    /**
     * Refreshes gold prices online via Gemini API.
     */
    fun refreshGoldPrices() {
        viewModelScope.launch {
            _isRefreshingPrices.value = true
            val status = repository.refreshLivePrices()
            _isRefreshingPrices.value = false
            when {
                status == "SUCCESS" -> _priceRefreshStatus.emit("Gold prices successfully updated live!")
                status == "SUSPICIOUS_DATA" -> _priceRefreshStatus.emit("Validation failed: Difference > 15% flagged as Suspicious.")
                status.startsWith("FAILED:") -> _priceRefreshStatus.emit("Validation failed: ${status.removePrefix("FAILED: ")}")
                else -> _priceRefreshStatus.emit("Source parsing failed. No verified prices available.")
            }
        }
    }

    // --- CRUD operations ---
    fun addItem(
        name: String,
        category: String,
        purity: String,
        weight: Double,
        purchasePrice: Double,
        invoicePath: String? = null
    ) {
        viewModelScope.launch {
            val newItem = GoldItem(
                name = name,
                category = category,
                purity = purity,
                weight = weight,
                purchasePrice = purchasePrice,
                capturedInvoicePath = invoicePath
            )
            repository.insertItem(newItem)
        }
    }

    fun editItem(
        id: Int,
        name: String,
        category: String,
        purity: String,
        weight: Double,
        purchasePrice: Double,
        invoicePath: String? = null
    ) {
        viewModelScope.launch {
            val existingItem = repository.getItemById(id)
            if (existingItem != null) {
                val updated = existingItem.copy(
                    name = name,
                    category = category,
                    purity = purity,
                    weight = weight,
                    purchasePrice = purchasePrice,
                    capturedInvoicePath = invoicePath ?: existingItem.capturedInvoicePath
                )
                repository.updateItem(updated)
            }
        }
    }

    fun deleteItem(item: GoldItem) {
        viewModelScope.launch {
            repository.deleteItem(item)
            // also clean up any invoice file associated if it exists
            item.capturedInvoicePath?.let { path ->
                val file = File(path)
                if (file.exists()) {
                    file.delete()
                }
            }
        }
    }

    // --- Price customization manual overrides ---
    fun updatePurityPriceManual(purity: String, price: Double) {
        viewModelScope.launch {
            repository.updateManualPrice(purity, price)
        }
    }

    fun updateMarketSettingsManual(category: String, makingFee: Double, cashbackPct: Double) {
        viewModelScope.launch {
            val current = marketSettings.value.toMutableList()
            val existingIndex = current.indexOfFirst { it.category == category }
            val newSetting = MarketSetting(category, makingFee, cashbackPct)
            if (existingIndex >= 0) {
                current[existingIndex] = newSetting
            } else {
                current.add(newSetting)
            }
            repository.updateMarketSettings(current)
        }
    }

    // --- Calculations ---

    fun getPriceForPurity(purity: String): Double {
        return allPrices.value.find { it.purity == purity }?.pricePerGram ?: 0.0
    }

    fun getSellPriceForPurity(purity: String): Double {
        return allPrices.value.find { it.purity == purity }?.sellPricePerGram?.takeIf { it > 0.0 } ?: getPriceForPurity(purity)
    }

    /**
     * Calculates the cashback rate (EGP per gram) for a given category.
     */
    fun getCashbackPerGram(category: String): Double {
        val setting = marketSettings.value.find { it.category == category }
        return if (setting != null) {
            setting.defaultMakingFeePerGram * setting.cashbackPercentage
        } else {
            when (category) {
                "BULLION" -> 120.0 * 0.50
                "COIN" -> 150.0 * 0.45
                else -> 0.0
            }
        }
    }

    fun getGlobalFairValue21K(): Double {
        val global = globalMarketData.value ?: return 0.0
        val spotPerOz = global.spotGoldUSD
        val usdEgp = global.usdEgpRate
        return (spotPerOz / 31.1035) * usdEgp * 0.875
    }

    fun getEgyptGap21K(): Double {
        val fairValue = getGlobalFairValue21K()
        if (fairValue <= 0.0) return 0.0
        val localSellPrice = getSellPriceForPurity("21K")
        return localSellPrice - fairValue
    }

    fun getGapPercentage21K(): Double {
        val fairValue = getGlobalFairValue21K()
        if (fairValue <= 0.0) return 0.0
        val gap = getEgyptGap21K()
        return (gap / fairValue) * 100.0
    }

    /**
     * Calculates resale value of a GoldItem.
     */
    fun calculateResaleValue(item: GoldItem): Double {
        val pricePerGram = getPriceForPurity(item.purity)
        val pureGoldValue = item.weight * pricePerGram
        
        return if (item.category == "JEWELRY") {
            // Jewelry completely drops making fee on resale
            pureGoldValue
        } else {
            // Bullion / Coins get a Cashback of their making fee per gram
            val cashbackPerGram = getCashbackPerGram(item.category)
            val cashback = item.weight * cashbackPerGram
            pureGoldValue + cashback
        }
    }

    /**
     * Calculates profit/loss of a GoldItem.
     */
    fun calculateProfitLoss(item: GoldItem): Double {
        return calculateResaleValue(item) - item.purchasePrice
    }

    /**
     * Creates a file in the app files directory to save a captured photo.
     */
    fun createInvoiceFile(): File {
        val context = getApplication<Application>()
        val dir = File(context.filesDir, "invoices")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "invoice_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}.jpg")
    }
}

package com.example.data

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.Date

class GoldRepository(private val dao: GoldDao) {
    private val TAG = "GoldRepository"

    val allItems: Flow<List<GoldItem>> = dao.getAllItems()
    val allPrices: Flow<List<GoldPrice>> = dao.getAllPricesFlow()
    val allMarketSettings: Flow<List<MarketSetting>> = dao.getAllMarketSettingsFlow()
    val globalMarketData: Flow<GlobalMarketEntity?> = dao.getGlobalMarketDataFlow()

    suspend fun insertItem(item: GoldItem) {
        dao.insertItem(item)
    }

    suspend fun updateItem(item: GoldItem) {
        dao.updateItem(item)
    }

    suspend fun deleteItem(item: GoldItem) {
        dao.deleteItem(item)
    }

    suspend fun getItemById(id: Int): GoldItem? {
        return dao.getItemById(id)
    }

    /**
     * Initializes default market settings and prices if they are empty in DB.
     */
    suspend fun initializeOfflineDefaults() {
        val currentSettings = dao.getAllMarketSettings()
        if (currentSettings.isEmpty()) {
            val defaults = listOf(
                MarketSetting("BULLION", 120.0, 0.50), // 120 EGP/gram making fee, 50% cashback
                MarketSetting("COIN", 150.0, 0.45),    // 150 EGP/gram, 45% cashback
                MarketSetting("JEWELRY", 280.0, 0.00)   // 280 EGP/gram, no cashback (dropped)
            )
            dao.insertMarketSettings(defaults)
            Log.d(TAG, "Initialized default market configurations")
        }

        val currentPrices = dao.getAllPrices()
        if (currentPrices.isEmpty()) {
            val list = listOf(
                GoldPrice("24K", 0.0, 0.0, System.currentTimeMillis()),
                GoldPrice("21K", 0.0, 0.0, System.currentTimeMillis()),
                GoldPrice("18K", 0.0, 0.0, System.currentTimeMillis()),
                GoldPrice("POUND", 0.0, 0.0, System.currentTimeMillis())
            )
            dao.insertPrices(list)
            Log.d(TAG, "Initialized blank gold prices in DB (waiting for refresh)")
        }

        val globalData = dao.getGlobalMarketData()
        if (globalData == null) {
            dao.insertGlobalMarketData(GlobalMarketEntity())
            Log.d(TAG, "Initialized default global market data")
        }
    }

    /**
     * Triggers active update of gold prices from Isagha/GoldPriceToday, saving to DB.
     * Also updates global market data.
     */
    suspend fun refreshLivePrices(): String {
        return try {
            val liveGlobalData = PriceScraperService.fetchGlobalMarketData()
            dao.insertGlobalMarketData(GlobalMarketEntity(spotGoldUSD = liveGlobalData.spotGoldUSD, usdEgpRate = liveGlobalData.usdEgpRate, lastUpdated = System.currentTimeMillis()))
            
            val livePrices = PriceScraperService.fetchLiveGoldPrices()
            if (livePrices.isNotEmpty()) {
                val p24 = livePrices["24K"] ?: return "FAILED: Missing 24K Data"
                val p21 = livePrices["21K"] ?: return "FAILED: Missing 21K Data"
                val p18 = livePrices["18K"] ?: return "FAILED: Missing 18K Data"
                val pPound = livePrices["POUND"] ?: return "FAILED: Missing Pound Data"

                // --- VALIDATION LAYER ---
                if (p24.buy <= p21.buy || p21.buy <= p18.buy) {
                    return "FAILED: Math Validation Failed (24K ${p24.buy} > 21K ${p21.buy} > 18K ${p18.buy} is FALSE)"
                }
                
                if (p24.sell <= p21.sell || p21.sell <= p18.sell) {
                    return "FAILED: Math Validation Failed (Sell prices inverted: 24K ${p24.sell}, 21K ${p21.sell}, 18K ${p18.sell})"
                }

                if (pPound.buy < (p21.buy * 7.0) || pPound.buy > (p21.buy * 10.0)) {
                    return "FAILED: Pound value (${pPound.buy}) outside expected range for 21K (${p21.buy})"
                }

                if (p24.buy <= 0 || p21.buy <= 0 || p18.buy <= 0 || pPound.buy <= 0) {
                    return "FAILED: Value <= 0 detected (24K: ${p24.buy}, 21K: ${p21.buy}, 18K: ${p18.buy}, POUND: ${pPound.buy})"
                }

                val existingPrices = dao.getAllPrices()
                var suspicious = false
                val dbPrices = livePrices.map { (purity, livePrice) ->
                    val existing = existingPrices.find { it.purity == purity }
                    if (existing != null && existing.pricePerGram > 0) {
                        val diffBuy = kotlin.math.abs(livePrice.buy - existing.pricePerGram) / existing.pricePerGram
                        val diffSell = kotlin.math.abs(livePrice.sell - existing.sellPricePerGram) / existing.sellPricePerGram
                        if (diffBuy > 0.15 || diffSell > 0.15) { // increased threshold slightly just in case market skips
                            suspicious = true
                        }
                    }
                    GoldPrice(purity, livePrice.buy, livePrice.sell, System.currentTimeMillis())
                }
                
                if (suspicious) {
                    return "SUSPICIOUS_DATA"
                }

                dao.insertPrices(dbPrices)
                Log.d(TAG, "Successfully refreshed and saved live prices: $livePrices")
                "SUCCESS"
            } else {
                "FAILED: Blank Data"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed fresh price fetch in repo: ${e.message}")
            "FAILED: ${e.message}"
        }
    }

    /**
     * Updates manual price override.
     */
    suspend fun updateManualPrice(purity: String, newPrice: Double) {
        val existing = dao.getAllPrices().find { it.purity == purity }
        val sellPrice = existing?.sellPricePerGram ?: 0.0
        dao.insertPrices(listOf(GoldPrice(purity, newPrice, sellPrice, System.currentTimeMillis())))
    }

    /**
     * Updates customized market parameters.
     */
    suspend fun updateMarketSettings(list: List<MarketSetting>) {
        dao.insertMarketSettings(list)
    }
}

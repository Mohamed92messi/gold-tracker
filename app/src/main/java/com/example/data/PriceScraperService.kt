package com.example.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class LiveGoldPrice(val buy: Double, val sell: Double)

data class GlobalMarketData(val spotGoldUSD: Double, val usdEgpRate: Double)

object PriceScraperService {
    private const val TAG = "PriceScraperService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchGlobalMarketData(): GlobalMarketData = withContext(Dispatchers.IO) {
        val spotGold = fetchSpotGold()
        val usdEgp = fetchUsdEgp()
        GlobalMarketData(spotGold, usdEgp)
    }

    private fun fetchUsdEgp(): Double {
        try {
            val request = Request.Builder().url("https://open.er-api.com/v6/latest/USD").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return 0.0
                val json = JSONObject(response.body?.string() ?: return 0.0)
                return json.getJSONObject("rates").getDouble("EGP")
            }
        } catch (e: Exception) {
            return 0.0
        }
    }

    private fun fetchSpotGold(): Double {
        try {
            val request = Request.Builder()
                .url("https://data-asg.goldprice.org/dbXRates/USD")
                .addHeader("User-Agent", "Mozilla/5.0")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return 0.0
                val body = response.body?.string() ?: return 0.0
                val json = JSONObject(body)
                val items = json.getJSONArray("items")
                return items.getJSONObject(0).getDouble("xauPrice")
            }
        } catch (e: Exception) {
            return 0.0
        }
    }

    /**
     * Fetches current Egypt gold prices in EGP from Isagha.
     * Returns a map of purity ("24K", "21K", "18K", "POUND") to LiveGoldPrice (buy and sell).
     */
    suspend fun fetchLiveGoldPrices(): Map<String, LiveGoldPrice> = withContext(Dispatchers.IO) {
        return@withContext fetchFromIsagha()
    }

    private fun fetchFromIsagha(): Map<String, LiveGoldPrice> {
        val url = "https://market.isagha.com/prices"
        val doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36")
            .timeout(15000)
            .get()
            
        val tables = doc.select("table")
        if (tables.isEmpty()) throw Exception("No tables found in HTML")
        
        var price24: LiveGoldPrice? = null
        var price21: LiveGoldPrice? = null
        var price18: LiveGoldPrice? = null
        var pricePound: LiveGoldPrice? = null

        val numberRegex = Regex("[0-9]+(?:\\.[0-9]+)?")

        for (table in tables) {
            val rows = table.select("tr")
            for (i in 1 until rows.size) { 
                val cols = rows[i].select("td, th")
                if (cols.size < 4) continue
                
                val title = cols[0].text().trim()
                val sellStr = cols[1].text()
                val buyStr = cols[3].text()
                
                val sellPrice = numberRegex.find(sellStr)?.value?.toDoubleOrNull()
                val buyPrice = numberRegex.find(buyStr)?.value?.toDoubleOrNull()
                
                if (sellPrice != null && buyPrice != null) {
                    val livePrice = LiveGoldPrice(buy = sellPrice, sell = buyPrice)
                    when {
                        title.contains("24") -> price24 = livePrice
                        title.contains("21") -> price21 = livePrice
                        title.contains("18") -> price18 = livePrice
                        title.contains("جنيه ذهب") -> pricePound = livePrice
                    }
                }
            }
        }
        
        if (price24 != null && price21 != null && price18 != null && pricePound != null) {
            return mapOf(
                "24K" to price24, 
                "21K" to price21, 
                "18K" to price18, 
                "POUND" to pricePound
            )
        } else {
            throw Exception("Could not parse all required prices from Isagha table. 24K: $price24, 21K: $price21, 18K: $price18, Pound: $pricePound")
        }
    }
}

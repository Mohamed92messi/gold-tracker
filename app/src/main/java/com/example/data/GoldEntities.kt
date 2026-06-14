package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gold_items")
data class GoldItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "BULLION", "COIN", "JEWELRY"
    val purity: String, // "24K", "21K", "18K"
    val weight: Double, // in grams
    val purchasePrice: Double, // historical total cost including making fees (in EGP)
    val capturedInvoicePath: String? = null, // local path to image if captured
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "gold_prices")
data class GoldPrice(
    @PrimaryKey val purity: String, // "24K", "21K", "18K"
    val pricePerGram: Double, // in EGP (Buy Price)
    val sellPricePerGram: Double = 0.0, // in EGP (Sell Price)
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "global_market_data")
data class GlobalMarketEntity(
    @PrimaryKey val id: Int = 1,
    val spotGoldUSD: Double = 0.0,
    val usdEgpRate: Double = 0.0,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "market_settings")
data class MarketSetting(
    @PrimaryKey val category: String, // "BULLION", "COIN", "JEWELRY"
    val defaultMakingFeePerGram: Double, // typical EGP per gram
    val cashbackPercentage: Double // fraction like 0.50 (50%) of making fee refunded
)

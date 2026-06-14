package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface GoldDao {
    @Query("SELECT * FROM gold_items ORDER BY dateAdded DESC")
    fun getAllItems(): Flow<List<GoldItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: GoldItem): Long

    @Update
    suspend fun updateItem(item: GoldItem)

    @Delete
    suspend fun deleteItem(item: GoldItem)

    @Query("SELECT * FROM gold_items WHERE id = :id")
    suspend fun getItemById(id: Int): GoldItem?

    // --- Price settings ---
    @Query("SELECT * FROM gold_prices")
    fun getAllPricesFlow(): Flow<List<GoldPrice>>

    @Query("SELECT * FROM gold_prices")
    suspend fun getAllPrices(): List<GoldPrice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrices(prices: List<GoldPrice>)

    // --- Market settings ---
    @Query("SELECT * FROM market_settings")
    fun getAllMarketSettingsFlow(): Flow<List<MarketSetting>>

    @Query("SELECT * FROM market_settings")
    suspend fun getAllMarketSettings(): List<MarketSetting>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMarketSettings(settings: List<MarketSetting>)
    
    // --- Global Market Data ---
    @Query("SELECT * FROM global_market_data WHERE id = 1")
    fun getGlobalMarketDataFlow(): Flow<GlobalMarketEntity?>
    
    @Query("SELECT * FROM global_market_data WHERE id = 1")
    suspend fun getGlobalMarketData(): GlobalMarketEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGlobalMarketData(data: GlobalMarketEntity)
}

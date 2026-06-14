package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GoldItem::class, GoldPrice::class, MarketSetting::class, GlobalMarketEntity::class], version = 3, exportSchema = false)
abstract class GoldDatabase : RoomDatabase() {
    abstract fun goldDao(): GoldDao

    companion object {
        @Volatile
        private var INSTANCE: GoldDatabase? = null

        fun getDatabase(context: Context): GoldDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GoldDatabase::class.java,
                    "gold_wallet_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

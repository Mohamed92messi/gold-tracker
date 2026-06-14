package com.example

import com.example.data.PriceScraperService
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.io.File

class IsaghaScraperTest {
    @Test
    fun fetchAndInspect() = runBlocking {
        try {
            val prices = PriceScraperService.fetchLiveGoldPrices()
            File("./isagha_inspection.txt").writeText("Success: $prices")
        } catch (e: Exception) {
            File("./isagha_inspection.txt").writeText("Error: ${e.message}")
        }
    }
}



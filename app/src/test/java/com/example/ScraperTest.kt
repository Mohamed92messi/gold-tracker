package com.example

import org.jsoup.Jsoup
import org.junit.Test

class ScraperTest {
    @Test
    fun testIsagha() {
        val doc = Jsoup.connect("https://www.gold-price-today.com/egypt/")
            .userAgent("Mozilla/5.0")
            .timeout(10000)
            .get()
            
        val tables = doc.select("table")
        var output = ""
        for (table in tables) {
            output += "Table found!\n"
            for (row in table.select("tr")) {
                output += "Row: " + row.text() + "\n"
            }
        }
        java.io.File("./gold.txt").writeText(output)
    }
}

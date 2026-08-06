package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationalCurrencyExtractionTest {
    @Test
    fun extractsCurrencyNamesBeforeAndAfterAmounts() {
        val text = "Pay 1,25,000 rupees, refund dollars 49.99, fee 20 euros, deposit pounds 75, and fare 300 yen."

        val prices = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.PRICE }

        assertTrue(prices.any { it.value == "1,25,000 rupees" })
        assertTrue(prices.any { it.value == "dollars 49.99" })
        assertTrue(prices.any { it.value == "20 euros" })
        assertTrue(prices.any { it.value == "pounds 75" })
        assertTrue(prices.any { it.value == "300 yen" })
        prices.forEach { price ->
            assertEquals(price.value, text.substring(price.sourceStart, price.sourceEndExclusive))
        }
    }

    @Test
    fun ordinaryNumbersStillDoNotBecomePrices() {
        val results = RuleBasedExtractor.extract("Invoice 12345, quantity 250, PIN 515401, and version 4.7.1.")

        assertFalse(results.any { it.type == ExtractionType.PRICE })
    }
}

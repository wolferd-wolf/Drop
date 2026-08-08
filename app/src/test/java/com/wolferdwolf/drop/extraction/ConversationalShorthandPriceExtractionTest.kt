package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationalShorthandPriceExtractionTest {
    @Test
    fun extractsShorthandPricesWithCurrencyWordsOrCodes() {
        val text = "Budget 1.5 lakh rupees, hosting 2.5k USD, grant dollars 1.2 million, and estimate 3 crore INR."

        val prices = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.PRICE }

        assertTrue(prices.any { it.value == "1.5 lakh rupees" })
        assertTrue(prices.any { it.value == "2.5k USD" })
        assertTrue(prices.any { it.value == "dollars 1.2 million" })
        assertTrue(prices.any { it.value == "3 crore INR" })
        prices.forEach { price ->
            assertEquals(price.value, text.substring(price.sourceStart, price.sourceEndExclusive))
        }
    }

    @Test
    fun magnitudeWordsWithoutCurrencyStillDoNotBecomePrices() {
        val results = RuleBasedExtractor.extract("Audience 1.5 million, batch 2.5k, population 3 crore.")

        assertFalse(results.any { it.type == ExtractionType.PRICE })
    }
}

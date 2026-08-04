package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CompactPriceExtractionTest {
    @Test
    fun extractsCompactIndianAndInternationalPriceAmounts() {
        val text = "Budget ₹2.5 lakh, deposit Rs. 75k, funding USD 1.2 million, valuation €3 billion."
        val prices = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.PRICE }

        assertTrue(prices.any { it.value == "₹2.5 lakh" })
        assertTrue(prices.any { it.value == "Rs. 75k" })
        assertTrue(prices.any { it.value == "USD 1.2 million" })
        assertTrue(prices.any { it.value == "€3 billion" })
        prices.forEach { price ->
            assertEquals(price.value, text.substring(price.sourceStart, price.sourceEndExclusive))
        }
    }

    @Test
    fun compactNumberWithoutCurrencyIsNotAprice() {
        val results = RuleBasedExtractor.extract("The video reached 2.5 million views and the model has 7b parameters.")

        assertFalse(results.any { it.type == ExtractionType.PRICE })
    }

    @Test
    fun compactPriceDoesNotAlsoReturnItsNumericPrefix() {
        val prices = RuleBasedExtractor.extract("Expected cost ₹2.5 lakh.")
            .filter { it.type == ExtractionType.PRICE }

        assertEquals(listOf("₹2.5 lakh"), prices.map { it.value })
    }
}

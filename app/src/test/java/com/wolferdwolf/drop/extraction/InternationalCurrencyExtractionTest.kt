package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Test

class InternationalCurrencyExtractionTest {
    @Test
    fun commonInternationalIsoCurrencies_areExtractedInPrefixAndSuffixForms() {
        val text = "Hotel AUD 129.50, transfer 220 SGD, visa AED 250 and parts CAD 89."

        val prices = RuleBasedExtractor.extract(text)
            .filter { it.type == ExtractionType.PRICE }
            .map { it.value }

        assertEquals(listOf("AUD 129.50", "220 SGD", "AED 250", "CAD 89"), prices)
    }

    @Test
    fun internationalIsoCurrencies_supportCompactMagnitudeAmounts() {
        val text = "Equipment AUD 1.5k, logistics 2 million SGD, permit AED 3k and stock 4.25k CAD."

        val prices = RuleBasedExtractor.extract(text)
            .filter { it.type == ExtractionType.PRICE }
            .map { it.value }

        assertEquals(listOf("AUD 1.5k", "2 million SGD", "AED 3k", "4.25k CAD"), prices)
    }

    @Test
    fun isoCurrencyRule_doesNotPromoteBareNumbers() {
        val prices = RuleBasedExtractor.extract("Room 129.50 and gate 220")
            .filter { it.type == ExtractionType.PRICE }

        assertEquals(emptyList<ExtractionResult>(), prices)
    }
}

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
    fun isoCurrencyRule_doesNotPromoteBareNumbers() {
        val prices = RuleBasedExtractor.extract("Room 129.50 and gate 220")
            .filter { it.type == ExtractionType.PRICE }

        assertEquals(emptyList<ExtractionResult>(), prices)
    }
}

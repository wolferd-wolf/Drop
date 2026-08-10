package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultilineAddressExtractionTest {
    @Test
    fun labelledMultilineAddressSurvivesNormalizedDisplayValue() {
        val text = "Product launch\nVenue:\nWolf Convention Centre\nMG Road, Vijayawada\nAndhra Pradesh 520010\nNotes: Bring registration receipt"

        val address = RuleBasedExtractor.extract(text).single { it.type == ExtractionType.ADDRESS }

        assertEquals(
            "Wolf Convention Centre, MG Road, Vijayawada, Andhra Pradesh 520010",
            address.value
        )
        assertEquals("Wolf Convention Centre\nMG Road, Vijayawada\nAndhra Pradesh 520010", text.substring(address.sourceStart, address.sourceEndExclusive))
        assertTrue(address.confidence >= 0.95f)
    }
}

package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApostropheEmailExtractionTest {
    @Test
    fun extractsApostropheEmailAndPreservesExactSourceRange() {
        val text = "Email o'connor@example.com about the proposal."

        val email = RuleBasedExtractor.extract(text).single { it.type == ExtractionType.EMAIL }

        assertEquals("o'connor@example.com", email.value)
        assertEquals(email.value, text.substring(email.sourceStart, email.sourceEndExclusive))
    }

    @Test
    fun apostropheEmailDoesNotCreateNestedDomainResult() {
        val results = RuleBasedExtractor.extract("Contact o'connor@example.com")

        assertTrue(results.any { it.type == ExtractionType.EMAIL && it.value == "o'connor@example.com" })
        assertFalse(results.any { it.type == ExtractionType.URL })
    }
}

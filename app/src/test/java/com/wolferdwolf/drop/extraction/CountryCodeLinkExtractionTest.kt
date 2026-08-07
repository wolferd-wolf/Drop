package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryCodeLinkExtractionTest {
    @Test
    fun extractsCommonBareCountryCodeDomains() {
        val samples = listOf(
            "Visit example.co.uk/help" to "example.co.uk/help",
            "See service.com.au/support" to "service.com.au/support",
            "Open portal.co.nz/account" to "portal.co.nz/account",
            "Details at example.de/events" to "example.de/events"
        )

        samples.forEach { (text, expected) ->
            val urls = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.URL }
            assertTrue("Expected URL $expected in $text", urls.any { it.value == expected })
        }
    }

    @Test
    fun preservesExactRangeAndTrimsSentencePunctuation() {
        val text = "Open example.co.uk/community-event."
        val result = RuleBasedExtractor.extract(text).single { it.type == ExtractionType.URL }

        assertEquals("example.co.uk/community-event", result.value)
        assertEquals(result.value, text.substring(result.sourceStart, result.sourceEndExclusive))
    }
}

package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrFriendlyTimeExtractionTest {
    @Test
    fun extractsDotSeparatedMeridiemTimeFromOcrText() {
        val text = "Doors open at 5.30 PM."
        val time = RuleBasedExtractor.extract(text).single { it.type == ExtractionType.TIME }

        assertEquals("5.30 PM", time.value)
        assertEquals(time.value, text.substring(time.sourceStart, time.sourceEndExclusive))
    }

    @Test
    fun extractsHourLetterTimeFromEuropeanAndOcrText() {
        val text = "Registration closes at 18h45."
        val time = RuleBasedExtractor.extract(text).single { it.type == ExtractionType.TIME }

        assertEquals("18h45", time.value)
        assertEquals(time.value, text.substring(time.sourceStart, time.sourceEndExclusive))
    }

    @Test
    fun rejectsInvalidDotAndHourLetterTimes() {
        val results = RuleBasedExtractor.extract("Version 5.30.1, invalid 13.75 PM, invalid 25h10.")

        assertFalse(results.any { it.type == ExtractionType.TIME })
    }

    @Test
    fun keepsExistingColonTimeSupport() {
        val results = RuleBasedExtractor.extract("Call at 7:30 PM or 18:45.")
            .filter { it.type == ExtractionType.TIME }

        assertTrue(results.any { it.value.equals("7:30 PM", true) })
        assertTrue(results.any { it.value == "18:45" })
    }
}

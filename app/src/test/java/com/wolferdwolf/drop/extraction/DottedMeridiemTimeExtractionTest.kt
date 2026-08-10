package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DottedMeridiemTimeExtractionTest {
    @Test
    fun extractsCommonDottedAmPmFormats() {
        val text = "Call at 9 a.m., review at 10:30 p.m., and backup at 8.45 A.M."
        val times = RuleBasedExtractor.extract(text)
            .filter { it.type == ExtractionType.TIME }
            .map { it.value }

        assertTrue(times.contains("9 a.m"))
        assertTrue(times.contains("10:30 p.m"))
        assertTrue(times.contains("8.45 A.M"))
    }

    @Test
    fun extractsDottedTwentyFourHourTime() {
        val text = "Boarding closes tomorrow at 17.30 at Gate 4."
        val times = RuleBasedExtractor.extract(text)
            .filter { it.type == ExtractionType.TIME }
            .map { it.value }

        assertTrue(times.contains("17.30"))
    }

    @Test
    fun dottedTwentyFourHourRuleDoesNotSplitDottedDates() {
        val results = RuleBasedExtractor.extract("Invoice date 08.08.2026 and review at 17.30")
        val times = results.filter { it.type == ExtractionType.TIME }.map { it.value }

        assertTrue(times.contains("17.30"))
        assertFalse(times.contains("08.08"))
        assertFalse(times.contains("08.20"))
    }

    @Test
    fun preservesExactSourceRangeAfterTrailingPeriodIsTrimmed() {
        val text = "Starts 7:15 p.m. tomorrow"
        val result = RuleBasedExtractor.extract(text)
            .single { it.type == ExtractionType.TIME }

        assertEquals("7:15 p.m", result.value)
        assertEquals(result.value, text.substring(result.sourceStart, result.sourceEndExclusive))
    }

    @Test
    fun existingPlainAmPmFormatsStillWork() {
        val times = RuleBasedExtractor.extract("Breakfast 8 AM, call 6:30 pm")
            .filter { it.type == ExtractionType.TIME }
            .map { it.value.lowercase() }

        assertTrue(times.contains("8 am"))
        assertTrue(times.contains("6:30 pm"))
    }
}

package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YearFirstDateExtractionTest {
    @Test
    fun extractsYearFirstSlashAndDottedDatesWithExactRanges() {
        val text = "Audit on 2026/08/15 and review on 2026.09.03."
        val dates = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.DATE }

        assertTrue(dates.any { it.value == "2026/08/15" })
        assertTrue(dates.any { it.value == "2026.09.03" })
        dates.forEach { assertEquals(it.value, text.substring(it.sourceStart, it.sourceEndExclusive)) }
    }

    @Test
    fun rejectsInvalidYearFirstMonthAndDayValues() {
        val results = RuleBasedExtractor.extract("Invalid 2026/13/15 and 2026.08.32 must stay plain text.")

        assertFalse(results.any { it.type == ExtractionType.DATE })
    }
}

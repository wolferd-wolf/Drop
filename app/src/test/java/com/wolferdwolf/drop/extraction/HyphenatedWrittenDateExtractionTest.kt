package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HyphenatedWrittenDateExtractionTest {
    @Test
    fun extractsDayFirstAndMonthFirstHyphenatedWrittenDates() {
        val text = "Workshop 12-Aug-2026, follow-up Sep-5-2026, archive 3-September-'27."
        val dates = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.DATE }

        assertTrue(dates.any { it.value == "12-Aug-2026" })
        assertTrue(dates.any { it.value == "Sep-5-2026" })
        assertTrue(dates.any { it.value == "3-September-'27" })
        dates.forEach { date ->
            assertEquals(date.value, text.substring(date.sourceStart, date.sourceEndExclusive))
        }
    }

    @Test
    fun doesNotTurnVersionOrRangeTextIntoDates() {
        val results = RuleBasedExtractor.extract("Use release-4-Aug and pages 12-14-2026 for review.")

        assertFalse(results.any { it.type == ExtractionType.DATE })
    }
}

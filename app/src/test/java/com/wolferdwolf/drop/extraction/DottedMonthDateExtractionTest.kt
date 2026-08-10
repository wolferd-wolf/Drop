package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DottedMonthDateExtractionTest {
    @Test
    fun monthFirstDottedAbbreviationIsExtracted() {
        val text = "Appointment on Aug. 15, 2026 at 6:30 PM."
        val dates = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.DATE }

        assertTrue(dates.any { it.value == "Aug. 15, 2026" })
    }

    @Test
    fun septAbbreviationWithDotIsExtracted() {
        val text = "Quarterly review Sept. 12, 2026 at 10:00 AM."
        val dates = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.DATE }

        assertTrue(dates.any { it.value == "Sept. 12, 2026" })
    }

    @Test
    fun dayFirstDottedAbbreviationIsExtracted() {
        val text = "Maintenance window 15 Aug. 2026 at 22:00."
        val dates = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.DATE }

        assertTrue(dates.any { it.value == "15 Aug. 2026" })
    }

    @Test
    fun noYearWrittenDateStillWorksAfterDottedMonthSupport() {
        val text = "Site visit 15 Aug at noon."
        val date = RuleBasedExtractor.extract(text).first { it.type == ExtractionType.DATE }

        assertEquals("15 Aug", date.value)
    }
}

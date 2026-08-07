package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DayOfMonthWrittenDateExtractionTest {
    @Test
    fun ordinalDayOfMonthWithOfIsExtracted() {
        val text = "Board review on 15th of August 2026 at 10:30 AM."
        val dates = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.DATE }

        assertTrue(dates.any { it.value == "15th of August 2026" })
    }

    @Test
    fun plainDayOfMonthWithOfIsExtracted() {
        val text = "Renewal deadline is 3 of September 2026."
        val dates = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.DATE }

        assertTrue(dates.any { it.value == "3 of September 2026" })
    }

    @Test
    fun writtenDateWithoutOfStillWorks() {
        val text = "Site visit 15 August 2026 at noon."
        val date = RuleBasedExtractor.extract(text).first { it.type == ExtractionType.DATE }

        assertEquals("15 August 2026", date.value)
    }
}

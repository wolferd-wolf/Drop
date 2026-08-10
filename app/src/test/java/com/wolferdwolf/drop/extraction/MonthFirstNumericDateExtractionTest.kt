package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthFirstNumericDateExtractionTest {
    @Test
    fun extractsUnambiguousMonthFirstSlashDate() {
        val text = "Conference starts 08/15/2026 at 9:30 AM."
        val dates = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.DATE }

        assertEquals(listOf("08/15/2026"), dates.map { it.value })
        val result = dates.single()
        assertEquals(text.indexOf("08/15/2026"), result.sourceStart)
        assertEquals(result.sourceStart + "08/15/2026".length, result.sourceEndExclusive)
    }

    @Test
    fun keepsExistingDayFirstDateValid() {
        val dates = RuleBasedExtractor.extract("Conference starts 15/08/2026.")
            .filter { it.type == ExtractionType.DATE }

        assertEquals(listOf("15/08/2026"), dates.map { it.value })
    }

    @Test
    fun rejectsImpossibleMonthFirstDate() {
        val dates = RuleBasedExtractor.extract("Bad date 02/30/2026 should not become an action.")
            .filter { it.type == ExtractionType.DATE }

        assertFalse(dates.any { it.value == "02/30/2026" })
    }

    @Test
    fun supportsTwoDigitYearWithoutTurningInvalidDateValid() {
        val valid = RuleBasedExtractor.extract("Review 12/31/26.")
            .filter { it.type == ExtractionType.DATE }
        val invalid = RuleBasedExtractor.extract("Review 13/32/26.")
            .filter { it.type == ExtractionType.DATE }

        assertTrue(valid.any { it.value == "12/31/26" })
        assertTrue(invalid.isEmpty())
    }
}

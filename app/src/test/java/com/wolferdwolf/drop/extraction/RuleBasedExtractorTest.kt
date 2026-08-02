package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RuleBasedExtractorTest {
    @Test
    fun extractsRepresentativeIndianContactAndScheduleData() {
        val text = "Call +91 98765 43210 or email team@example.in on 15/08/2026 at 7:30 PM. Visit https://drop.app/info."

        val results = RuleBasedExtractor.extract(text)

        assertTrue(results.any { it.type == ExtractionType.PHONE && it.value.contains("98765") })
        assertTrue(results.any { it.type == ExtractionType.EMAIL && it.value == "team@example.in" })
        assertTrue(results.any { it.type == ExtractionType.DATE && it.value == "15/08/2026" })
        assertTrue(results.any { it.type == ExtractionType.TIME && it.value.equals("7:30 PM", true) })
        assertTrue(results.any { it.type == ExtractionType.URL && it.value == "https://drop.app/info" })
    }

    @Test
    fun extractsWrittenDateAndTwentyFourHourTime() {
        val results = RuleBasedExtractor.extract("Meeting 2nd August 2026 at 18:45")

        assertTrue(results.any { it.type == ExtractionType.DATE && it.value == "2nd August 2026" })
        assertTrue(results.any { it.type == ExtractionType.TIME && it.value == "18:45" })
    }

    @Test
    fun extractsIsoMonthFirstAndBareDomainInputs() {
        val text = "Launch 2026-08-18, review August 21st, 2026, and open drop.app/launch."
        val results = RuleBasedExtractor.extract(text)

        assertTrue(results.any { it.type == ExtractionType.DATE && it.value == "2026-08-18" })
        assertTrue(results.any { it.type == ExtractionType.DATE && it.value == "August 21st, 2026" })
        assertTrue(results.any { it.type == ExtractionType.URL && it.value == "drop.app/launch" })
    }

    @Test
    fun extractsRelativeDatesWeekdaysAndNaturalTimes() {
        val results = RuleBasedExtractor.extract(
            "Call tomorrow at noon, review this Friday, and submit the day after tomorrow at midnight."
        )

        assertTrue(results.any { it.type == ExtractionType.DATE && it.value.equals("tomorrow", true) })
        assertTrue(results.any { it.type == ExtractionType.DATE && it.value.equals("this Friday", true) })
        assertTrue(results.any { it.type == ExtractionType.DATE && it.value.equals("day after tomorrow", true) })
        assertTrue(results.any { it.type == ExtractionType.TIME && it.value.equals("noon", true) })
        assertTrue(results.any { it.type == ExtractionType.TIME && it.value.equals("midnight", true) })
    }

    @Test
    fun keepsLongerRelativeDateInsteadOfNestedTomorrow() {
        val dates = RuleBasedExtractor.extract("Remind me day after tomorrow")
            .filter { it.type == ExtractionType.DATE }

        assertEquals(1, dates.size)
        assertTrue(dates.single().value.equals("day after tomorrow", ignoreCase = true))
    }

    @Test
    fun preservesExactSourceRangesAfterTrailingPunctuationIsTrimmed() {
        val text = "Open https://drop.app/info, then email hello@drop.app."
        val results = RuleBasedExtractor.extract(text)

        results.forEach { result ->
            assertEquals(result.value, text.substring(result.sourceStart, result.sourceEndExclusive))
        }
    }

    @Test
    fun emailDoesNotAlsoCreateNestedDomainResult() {
        val results = RuleBasedExtractor.extract("Email hello@drop.app")

        assertEquals(1, results.size)
        assertEquals(ExtractionType.EMAIL, results.single().type)
    }

    @Test
    fun returnsNoResultsForBlankInput() {
        assertTrue(RuleBasedExtractor.extract("   ").isEmpty())
    }
}

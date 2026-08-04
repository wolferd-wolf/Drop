package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun extractsCommonModernBareDomains() {
        val text = "Portfolio wolfpack.me, services wolfpack.tech/build, store wolfpack.store, and AI wolfpack.ai."
        val links = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.URL }

        assertTrue(links.any { it.value == "wolfpack.me" })
        assertTrue(links.any { it.value == "wolfpack.tech/build" })
        assertTrue(links.any { it.value == "wolfpack.store" })
        assertTrue(links.any { it.value == "wolfpack.ai" })
        links.forEach { link ->
            assertEquals(link.value, text.substring(link.sourceStart, link.sourceEndExclusive))
        }
    }

    @Test
    fun ordinaryDottedWordsDoNotBecomeLinks() {
        val results = RuleBasedExtractor.extract("Use report.final and version.preview during internal review.")

        assertFalse(results.any { it.type == ExtractionType.URL })
    }

    @Test
    fun extractsIndianAndInternationalPrices() {
        val text = "Total ₹1,25,499.50, deposit Rs. 2,000, plan USD 19.99, fee 35 EUR, and ticket £12."
        val prices = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.PRICE }

        assertTrue(prices.any { it.value == "₹1,25,499.50" })
        assertTrue(prices.any { it.value == "Rs. 2,000" })
        assertTrue(prices.any { it.value == "USD 19.99" })
        assertTrue(prices.any { it.value == "35 EUR" })
        assertTrue(prices.any { it.value == "£12" })
    }

    @Test
    fun priceExtractionRequiresAnExplicitCurrency() {
        val text = "Invoice 12345, quantity 250, PIN 515401, date 15/08/2026, time 18:45."
        val results = RuleBasedExtractor.extract(text)

        assertFalse(results.any { it.type == ExtractionType.PRICE })
    }

    @Test
    fun priceSourceRangesRemainExact() {
        val text = "Pay INR 12,500.00 today."
        val price = RuleBasedExtractor.extract(text).single { it.type == ExtractionType.PRICE }

        assertEquals("INR 12,500.00", price.value)
        assertEquals(price.value, text.substring(price.sourceStart, price.sourceEndExclusive))
    }

    @Test
    fun extractsInternationalAndFormattedPhoneNumbers() {
        val text = "US +1 (415) 555-2671, UK +44 20 7946 0958, office (040) 2345 6789."
        val phones = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.PHONE }

        assertTrue(phones.any { it.value == "+1 (415) 555-2671" })
        assertTrue(phones.any { it.value == "+44 20 7946 0958" })
        assertTrue(phones.any { it.value == "(040) 2345 6789" })
    }

    @Test
    fun deduplicatesEquivalentPhoneFormatting() {
        val text = "Primary +91 98765 43210. Repeat: +91-98765-43210."
        val phones = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.PHONE }

        assertEquals(1, phones.size)
    }

    @Test
    fun doesNotTreatDatesTimesOrShortNumbersAsPhones() {
        val text = "Invoice 12345 dated 15/08/2026 at 18:45. PIN 515401."
        val results = RuleBasedExtractor.extract(text)

        assertFalse(results.any { it.type == ExtractionType.PHONE })
        assertTrue(results.any { it.type == ExtractionType.DATE })
        assertTrue(results.any { it.type == ExtractionType.TIME })
    }

    @Test
    fun extractsWrittenDateAndTwentyFourHourTime() {
        val results = RuleBasedExtractor.extract("Meeting 2nd August 2026 at 18:45")

        assertTrue(results.any { it.type == ExtractionType.DATE && it.value == "2nd August 2026" })
        assertTrue(results.any { it.type == ExtractionType.TIME && it.value == "18:45" })
    }

    @Test
    fun extractsDottedAndApostropheYearDates() {
        val text = "Workshop on 12.08.2026, registration closes 5 Sep '26, review October 3rd '27."
        val dates = RuleBasedExtractor.extract(text).filter { it.type == ExtractionType.DATE }

        assertTrue(dates.any { it.value == "12.08.2026" })
        assertTrue(dates.any { it.value == "5 Sep '26" })
        assertTrue(dates.any { it.value == "October 3rd '27" })
    }

    @Test
    fun dottedDateSourceRangesRemainExact() {
        val text = "Submit by 03.11.2026."
        val date = RuleBasedExtractor.extract(text).single { it.type == ExtractionType.DATE }

        assertEquals("03.11.2026", date.value)
        assertEquals(date.value, text.substring(date.sourceStart, date.sourceEndExclusive))
    }

    @Test
    fun doesNotTreatVersionNumbersAsDottedDates() {
        val results = RuleBasedExtractor.extract("Release version 4.7.1 is installed.")

        assertFalse(results.any { it.type == ExtractionType.DATE })
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

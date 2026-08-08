package com.wolferdwolf.drop.history

import com.wolferdwolf.drop.data.SavedSourceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HistorySearchTest {
    @Test
    fun blankQueryMatchesEverything() {
        assertTrue(HistorySearch.matches("   ", "Quarterly strategy", "operations review"))
    }

    @Test
    fun queryMatchesTitleOrContentIgnoringCase() {
        assertTrue(HistorySearch.matches("strategy", "Quarterly Strategy", "operations review"))
        assertTrue(HistorySearch.matches("OPERATIONS", "Quarterly Strategy", "operations review"))
    }

    @Test
    fun multiTermQueryCanMatchAcrossDifferentSavedFields() {
        assertTrue(
            HistorySearch.matches(
                "quarterly operations",
                "Quarterly wolf strategy",
                "Operations review notes for the northern region."
            )
        )
    }

    @Test
    fun multiTermQueryRequiresEveryTermToMatchSomeSavedField() {
        assertFalse(
            HistorySearch.matches(
                "quarterly invoice",
                "Quarterly wolf strategy",
                "Operations review notes for the northern region."
            )
        )
    }

    @Test
    fun repeatedWhitespaceDoesNotBreakMultiTermSearch() {
        assertTrue(HistorySearch.matches("  wolf   northern  ", "Quarterly wolf strategy", "Northern region notes"))
    }

    @Test
    fun punctuationDifferencesDoNotHideSavedPhoneEmailOrUrlValues() {
        assertTrue(HistorySearch.matches("9876543210", "Supplier contact", "Call +91 98765-43210"))
        assertTrue(HistorySearch.matches("opswolfexamplecom", "Operations", "Email ops.wolf@example.com"))
        assertTrue(HistorySearch.matches("examplecominvoices42", "Invoice", "https://example.com/invoices/42"))
    }

    @Test
    fun accentsDoNotHideSavedTitlesOrContent() {
        assertTrue(HistorySearch.matches("cafe", "Café meeting", "Vendor review"))
        assertTrue(HistorySearch.matches("resume", "Candidate résumé", "Interview notes"))
        assertTrue(HistorySearch.matches("sao", "Travel reference", "São Paulo office"))
    }

    @Test
    fun queryContainingOnlyPunctuationDoesNotAccidentallyHideHistory() {
        assertTrue(HistorySearch.matches("---", "Quarterly Strategy", "operations review"))
    }

    @Test
    fun queryDoesNotMatchUnrelatedFields() {
        assertFalse(HistorySearch.matches("invoice", "Quarterly Strategy", "operations review"))
    }

    @Test
    fun actionTypeFilterIncludesOnlyRequestedHistoryKind() {
        assertTrue(HistorySearch.includesReferences(HistoryItemFilter.ALL))
        assertTrue(HistorySearch.includesReminders(HistoryItemFilter.ALL))

        assertTrue(HistorySearch.includesReferences(HistoryItemFilter.REFERENCES))
        assertFalse(HistorySearch.includesReminders(HistoryItemFilter.REFERENCES))

        assertFalse(HistorySearch.includesReferences(HistoryItemFilter.REMINDERS))
        assertTrue(HistorySearch.includesReminders(HistoryItemFilter.REMINDERS))
    }

    @Test
    fun sourceFilterMatchesOnlyPersistedSourceType() {
        assertTrue(HistorySearch.matchesSource(HistorySourceFilter.ALL, SavedSourceType.UNKNOWN))
        assertTrue(HistorySearch.matchesSource(HistorySourceFilter.IMAGE, SavedSourceType.IMAGE))
        assertTrue(HistorySearch.matchesSource(HistorySourceFilter.PDF, SavedSourceType.PDF))
        assertFalse(HistorySearch.matchesSource(HistorySourceFilter.IMAGE, SavedSourceType.TEXT))
        assertFalse(HistorySearch.matchesSource(HistorySourceFilter.TEXT, SavedSourceType.UNKNOWN))
    }

    @Test
    fun dateFilterUsesLocalCalendarDayBoundaries() {
        val calendar = Calendar.getInstance().apply {
            set(2026, Calendar.AUGUST, 7, 18, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = calendar.timeInMillis
        val todayMorning = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 8)
        }.timeInMillis
        val yesterday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }.timeInMillis
        val eightDaysAgo = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -8)
        }.timeInMillis
        val thirtyOneDaysAgo = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -31)
        }.timeInMillis
        val future = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.MINUTE, 1)
        }.timeInMillis

        assertTrue(HistorySearch.matchesDate(HistoryDateFilter.ALL, thirtyOneDaysAgo, now))
        assertTrue(HistorySearch.matchesDate(HistoryDateFilter.TODAY, todayMorning, now))
        assertFalse(HistorySearch.matchesDate(HistoryDateFilter.TODAY, yesterday, now))
        assertTrue(HistorySearch.matchesDate(HistoryDateFilter.LAST_7_DAYS, yesterday, now))
        assertFalse(HistorySearch.matchesDate(HistoryDateFilter.LAST_7_DAYS, eightDaysAgo, now))
        assertTrue(HistorySearch.matchesDate(HistoryDateFilter.LAST_30_DAYS, eightDaysAgo, now))
        assertFalse(HistorySearch.matchesDate(HistoryDateFilter.LAST_30_DAYS, thirtyOneDaysAgo, now))
        assertFalse(HistorySearch.matchesDate(HistoryDateFilter.TODAY, future, now))
    }
}

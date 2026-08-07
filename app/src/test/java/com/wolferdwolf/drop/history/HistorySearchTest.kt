package com.wolferdwolf.drop.history

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

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
}

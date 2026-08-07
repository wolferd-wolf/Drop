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
    fun queryDoesNotMatchUnrelatedFields() {
        assertFalse(HistorySearch.matches("invoice", "Quarterly Strategy", "operations review"))
    }
}

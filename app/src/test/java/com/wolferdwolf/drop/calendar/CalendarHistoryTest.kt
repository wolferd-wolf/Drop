package com.wolferdwolf.drop.calendar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarHistoryTest {
    @Test
    fun historyTitleUsesEditedEventTitle() {
        assertEquals(
            "Calendar: Product launch review",
            CalendarConfirmationActivity.historyTitle("  Product launch review  ")
        )
    }

    @Test
    fun historyContentPreservesEditedFieldsAndLaunchStatus() {
        val content = CalendarConfirmationActivity.historyContent(
            title = "Product launch review",
            date = "2026-08-21",
            startTime = "16:00",
            endTime = "17:30",
            venue = "Vijayawada Office",
            notes = "Review the release checklist."
        )

        assertTrue(content.startsWith("Status: Opened in Calendar app"))
        assertTrue(content.contains("Event: Product launch review"))
        assertTrue(content.contains("Date: 2026-08-21"))
        assertTrue(content.contains("Start: 16:00"))
        assertTrue(content.contains("End: 17:30"))
        assertTrue(content.contains("Venue: Vijayawada Office"))
        assertTrue(content.contains("Review the release checklist."))
    }

    @Test
    fun historyContentOmitsBlankOptionalFields() {
        val content = CalendarConfirmationActivity.historyContent(
            title = "Appointment",
            date = "2026-09-02",
            startTime = "10:00",
            endTime = "",
            venue = "",
            notes = ""
        )

        assertTrue(!content.contains("End:"))
        assertTrue(!content.contains("Venue:"))
    }
}

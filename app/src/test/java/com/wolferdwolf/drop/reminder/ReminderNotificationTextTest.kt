package com.wolferdwolf.drop.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.util.Locale
import java.util.TimeZone

class ReminderNotificationTextTest {
    private val zone = TimeZone.getTimeZone("Asia/Kolkata")
    private val locale = Locale.US

    @Test
    fun includesScheduledDateTimeAndTrimmedNotes() {
        val text = ReminderNotificationText.build(
            triggerAtMillis = Instant.parse("2026-08-10T12:30:00Z").toEpochMilli(),
            notes = "  Bring documents  ",
            timeZone = zone,
            locale = locale
        )
        assertEquals("Scheduled for Aug 10, 2026, 6:00 PM · Bring documents", text)
    }

    @Test
    fun fallsBackToUsefulPromptWhenNoTimeOrNotesExist() {
        assertEquals(
            "Open Drop to view your saved content",
            ReminderNotificationText.build(0L, "   ", zone, locale)
        )
    }

    @Test
    fun preservesNotesWhenTriggerTimestampIsMissing() {
        val text = ReminderNotificationText.build(0L, "Review the saved item", zone, locale)
        assertTrue(text == "Review the saved item")
    }
}

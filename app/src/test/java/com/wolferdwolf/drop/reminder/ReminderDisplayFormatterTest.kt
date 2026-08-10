package com.wolferdwolf.drop.reminder

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderDisplayFormatterTest {
    @Test
    fun formatsFutureReminderInRequestedZoneWithScheduledStatus() {
        val trigger = Instant.parse("2026-08-02T01:30:00Z").toEpochMilli()
        val now = Instant.parse("2026-08-01T00:00:00Z").toEpochMilli()

        assertEquals(
            "Scheduled for Sun, 2 Aug 2026 at 7:00 AM",
            ReminderDisplayFormatter.format(trigger, ZoneId.of("Asia/Kolkata"), now)
        )
    }

    @Test
    fun elapsedReminderIsClearlyMarkedAsPastTriggerTime() {
        val trigger = Instant.parse("2026-08-07T18:15:00Z").toEpochMilli()
        val now = Instant.parse("2026-08-08T00:00:00Z").toEpochMilli()

        val display = ReminderDisplayFormatter.format(trigger, ZoneOffset.UTC, now)

        assertTrue(display.startsWith("Trigger time passed · "))
        assertTrue(display.contains("Fri, 7 Aug 2026 at 6:15 PM"))
    }

    @Test
    fun triggerExactlyAtNowIsNotPresentedAsStillScheduled() {
        val now = Instant.parse("2026-08-08T00:00:00Z").toEpochMilli()

        val display = ReminderDisplayFormatter.format(now, ZoneOffset.UTC, now)

        assertTrue(display.startsWith("Trigger time passed · "))
    }
}

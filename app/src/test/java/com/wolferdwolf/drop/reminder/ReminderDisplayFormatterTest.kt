package com.wolferdwolf.drop.reminder

import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderDisplayFormatterTest {
    @Test
    fun formatsReminderInRequestedZone() {
        val trigger = Instant.parse("2026-08-02T01:30:00Z").toEpochMilli()

        assertEquals(
            "Sun, 2 Aug 2026 at 7:00 AM",
            ReminderDisplayFormatter.format(trigger, ZoneId.of("Asia/Kolkata"))
        )
    }
}

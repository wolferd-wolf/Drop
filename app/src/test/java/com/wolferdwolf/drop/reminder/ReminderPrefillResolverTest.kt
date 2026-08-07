package com.wolferdwolf.drop.reminder

import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderPrefillResolverTest {
    private val today = LocalDate.of(2026, 8, 8)
    private val now = LocalTime.of(5, 0)

    @Test
    fun usesDetectedIsoDateAndTwelveHourTime() {
        val result = ReminderPrefillResolver.from(
            "Site visit on 2026-08-15 at 9:30 PM",
            today,
            now
        )

        assertEquals("2026-08-15", result.date)
        assertEquals("21:30", result.time)
    }

    @Test
    fun normalizesWrittenDateAndDottedMeridiem() {
        val result = ReminderPrefillResolver.from(
            "Review on 15th August 2026 at 9.30 p.m.",
            today,
            now
        )

        assertEquals("2026-08-15", result.date)
        assertEquals("21:30", result.time)
    }

    @Test
    fun resolvesRelativeDateInsteadOfDiscardingIt() {
        val result = ReminderPrefillResolver.from(
            "Call supplier tomorrow at noon",
            today,
            now
        )

        assertEquals("2026-08-09", result.date)
        assertEquals("12:00", result.time)
    }

    @Test
    fun keepsSafeFallbackWhenNothingIsDetected() {
        val result = ReminderPrefillResolver.from("Remember the wolf sketch", today, now)

        assertEquals("2026-08-09", result.date)
        assertEquals("06:00", result.time)
    }
}

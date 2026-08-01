package com.wolferdwolf.drop.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

class ReminderValidatorTest {
    private val clock = Clock.fixed(Instant.parse("2026-08-01T00:00:00Z"), ZoneId.systemDefault())

    @Test
    fun acceptsFutureReminderAndTrimsFields() {
        val result = ReminderValidator.validate(
            title = "  Pay electricity bill  ",
            notes = "  Account 123  ",
            dateText = "2099-08-02",
            timeText = "09:30",
            clock = clock
        )
        assertTrue(result is ReminderValidator.Result.Success)
        val reminder = (result as ReminderValidator.Result.Success).reminder
        assertEquals("Pay electricity bill", reminder.title)
        assertEquals("Account 123", reminder.notes)
        assertTrue(reminder.triggerAtMillis > clock.millis())
    }

    @Test
    fun rejectsBlankTitle() {
        val result = ReminderValidator.validate(" ", "", "2099-08-02", "09:30", clock)
        assertEquals("Enter a reminder title", (result as ReminderValidator.Result.Error).message)
    }

    @Test
    fun rejectsMalformedDateAndTime() {
        val badDate = ReminderValidator.validate("Task", "", "02/08/2099", "09:30", clock)
        assertEquals("Use date format YYYY-MM-DD", (badDate as ReminderValidator.Result.Error).message)

        val badTime = ReminderValidator.validate("Task", "", "2099-08-02", "9.30", clock)
        assertEquals("Use time format HH:MM", (badTime as ReminderValidator.Result.Error).message)
    }

    @Test
    fun rejectsPastReminder() {
        val result = ReminderValidator.validate("Old task", "", "2020-01-01", "08:00", clock)
        assertEquals("Choose a future date and time", (result as ReminderValidator.Result.Error).message)
    }
}

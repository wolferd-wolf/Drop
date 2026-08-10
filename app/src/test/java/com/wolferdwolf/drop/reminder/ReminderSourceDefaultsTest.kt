package com.wolferdwolf.drop.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderSourceDefaultsTest {
    @Test
    fun extractsConcreteIsoDateAnd24HourTime() {
        val values = ReminderSourceDefaults.from("Pay invoice on 2026-08-12 at 18:45")

        assertEquals("2026-08-12", values.date)
        assertEquals("18:45", values.time)
    }

    @Test
    fun convertsConcrete12HourTimeToEditable24HourValue() {
        val values = ReminderSourceDefaults.from("Call at 7:05 PM on 2026-08-12")

        assertEquals("2026-08-12", values.date)
        assertEquals("19:05", values.time)
    }

    @Test
    fun ignoresAmbiguousRelativeDeadlineLanguage() {
        val values = ReminderSourceDefaults.from("Please pay this by tomorrow evening")

        assertNull(values.date)
        assertNull(values.time)
    }
}

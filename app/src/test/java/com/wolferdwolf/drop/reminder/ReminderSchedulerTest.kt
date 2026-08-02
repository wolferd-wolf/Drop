package com.wolferdwolf.drop.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ReminderSchedulerTest {
    @Test
    fun requestCodeIsStableForScheduleAndCancellation() {
        val first = ReminderScheduler.requestCode("Call supplier", 1_800_000_000_000)
        val second = ReminderScheduler.requestCode("Call supplier", 1_800_000_000_000)

        assertEquals(first, second)
    }

    @Test
    fun requestCodeChangesWhenReminderIdentityChanges() {
        val original = ReminderScheduler.requestCode("Call supplier", 1_800_000_000_000)
        val differentTime = ReminderScheduler.requestCode("Call supplier", 1_800_000_060_000)
        val differentTitle = ReminderScheduler.requestCode("Call customer", 1_800_000_000_000)

        assertNotEquals(original, differentTime)
        assertNotEquals(original, differentTitle)
    }
}

package com.wolferdwolf.drop.reminder

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderRestorePlannerTest {
    @Test
    fun restoresOnlyFutureRemindersInTriggerOrder() {
        val now = 2_000L
        val records = listOf(
            record(id = "later", triggerAtMillis = 5_000L),
            record(id = "expired", triggerAtMillis = 1_000L),
            record(id = "soon", triggerAtMillis = 3_000L),
            record(id = "now", triggerAtMillis = now)
        )

        val result = ReminderRestorePlanner.futureRecords(records, now)

        assertEquals(listOf("soon", "later"), result.map(ReminderRecord::id))
    }

    @Test
    fun removesDuplicateHistoryRecordsById() {
        val duplicate = record(id = "same", triggerAtMillis = 3_000L)

        val result = ReminderRestorePlanner.futureRecords(
            records = listOf(duplicate, duplicate.copy(title = "Duplicate")),
            nowMillis = 1_000L
        )

        assertEquals(1, result.size)
        assertEquals("same", result.single().id)
    }

    private fun record(id: String, triggerAtMillis: Long) = ReminderRecord(
        id = id,
        title = "Reminder $id",
        notes = "",
        triggerAtMillis = triggerAtMillis,
        createdAtMillis = 500L
    )
}

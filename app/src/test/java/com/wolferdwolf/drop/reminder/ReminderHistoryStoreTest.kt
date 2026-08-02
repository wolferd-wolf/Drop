package com.wolferdwolf.drop.reminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderHistoryStoreTest {
    @Test
    fun codecRoundTripsMultilineAndUnicodeContent() {
        val record = ReminderRecord(
            id = "reminder-1",
            title = "Call Akila | tomorrow",
            notes = "Line one\n₹500 at 10:30 AM 🐺",
            triggerAtMillis = 1_800_000_000_000,
            createdAtMillis = 1_700_000_000_000
        )

        val decoded = ReminderHistoryStore.Codec.decode(ReminderHistoryStore.Codec.encode(record))

        assertEquals(record, decoded)
    }

    @Test
    fun codecRejectsMalformedOrUnknownRecords() {
        assertNull(ReminderHistoryStore.Codec.decode("broken"))
        assertNull(ReminderHistoryStore.Codec.decode("2|a|b|c|1|2"))
        assertNull(ReminderHistoryStore.Codec.decode("1|%%%|%%%|%%%|x|y"))
    }
}

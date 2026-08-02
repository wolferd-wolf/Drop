package com.wolferdwolf.drop.reminder

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object ReminderDisplayFormatter {
    private val formatter = DateTimeFormatter.ofPattern("EEE, d MMM yyyy 'at' h:mm a")

    fun format(triggerAtMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(triggerAtMillis)
            .atZone(zoneId)
            .format(formatter)
}

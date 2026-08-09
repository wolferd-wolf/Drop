package com.wolferdwolf.drop.reminder

import java.text.DateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object ReminderNotificationText {
    fun build(
        triggerAtMillis: Long,
        notes: String,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): String {
        val scheduledFor = triggerAtMillis.takeIf { it > 0L }?.let {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).apply {
                this.timeZone = timeZone
            }.format(Date(it))
        }
        return buildString {
            if (scheduledFor != null) append("Scheduled for ").append(scheduledFor)
            val trimmedNotes = notes.trim()
            if (trimmedNotes.isNotEmpty()) {
                if (isNotEmpty()) append(" · ")
                append(trimmedNotes)
            }
            if (isEmpty()) append("Open Drop to view your saved content")
        }
    }
}

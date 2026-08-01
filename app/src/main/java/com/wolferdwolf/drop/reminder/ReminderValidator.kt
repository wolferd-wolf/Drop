package com.wolferdwolf.drop.reminder

import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object ReminderValidator {
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val timeFormatter = DateTimeFormatter.ofPattern("H:mm")

    data class ValidReminder(
        val title: String,
        val notes: String,
        val triggerAtMillis: Long
    )

    sealed interface Result {
        data class Success(val reminder: ValidReminder) : Result
        data class Error(val message: String) : Result
    }

    fun validate(
        title: String,
        notes: String,
        dateText: String,
        timeText: String,
        clock: Clock = Clock.systemDefaultZone()
    ): Result {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return Result.Error("Enter a reminder title")
        if (cleanTitle.length > 120) return Result.Error("Title must be 120 characters or fewer")

        val date = try {
            LocalDate.parse(dateText.trim(), dateFormatter)
        } catch (_: DateTimeParseException) {
            return Result.Error("Use date format YYYY-MM-DD")
        }
        val time = try {
            LocalTime.parse(timeText.trim(), timeFormatter)
        } catch (_: DateTimeParseException) {
            return Result.Error("Use time format HH:MM")
        }

        val zone = ZoneId.systemDefault()
        val trigger = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
        if (trigger <= clock.millis()) return Result.Error("Choose a future date and time")

        return Result.Success(
            ValidReminder(
                title = cleanTitle,
                notes = notes.trim(),
                triggerAtMillis = trigger
            )
        )
    }
}

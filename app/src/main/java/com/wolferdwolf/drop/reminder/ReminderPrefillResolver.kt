package com.wolferdwolf.drop.reminder

import com.wolferdwolf.drop.extraction.ExtractionType
import com.wolferdwolf.drop.extraction.RuleBasedExtractor
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class ReminderPrefill(
    val date: String,
    val time: String
)

object ReminderPrefillResolver {
    private val outputTime = DateTimeFormatter.ofPattern("HH:mm")
    private val writtenDateFormatters = listOf(
        formatter("d MMM uuuu"),
        formatter("d MMMM uuuu"),
        formatter("MMM d uuuu"),
        formatter("MMMM d uuuu"),
        formatter("d MMM"),
        formatter("d MMMM"),
        formatter("MMM d"),
        formatter("MMMM d")
    )

    fun from(
        sourceText: String,
        today: LocalDate = LocalDate.now(),
        now: LocalTime = LocalTime.now()
    ): ReminderPrefill {
        val results = RuleBasedExtractor.extract(sourceText)
        val extractedDate = results.firstOrNull { it.type == ExtractionType.DATE }?.value
        val extractedTime = results.firstOrNull { it.type == ExtractionType.TIME }?.value

        val date = extractedDate?.let { parseDate(it, today) } ?: today.plusDays(1)
        val time = extractedTime?.let(::parseTime) ?: now.plusHours(1)
        return ReminderPrefill(date.toString(), time.format(outputTime))
    }

    private fun parseDate(raw: String, today: LocalDate): LocalDate? {
        val clean = raw.trim().replace(Regex("(?i)(\\d)(st|nd|rd|th)"), "$1")
            .replace(Regex("(?i)\\bof\\s+"), "")
            .replace(",", "")
            .replace(Regex("\\s+"), " ")
        when (clean.lowercase(Locale.ROOT)) {
            "today", "tonight" -> return today
            "tomorrow" -> return today.plusDays(1)
            "day after tomorrow" -> return today.plusDays(2)
        }

        weekday(clean)?.let { day ->
            val next = today.with(TemporalAdjusters.next(day))
            return if (clean.startsWith("this ", true)) {
                val sameOrNext = today.with(TemporalAdjusters.nextOrSame(day))
                if (sameOrNext == today) today else sameOrNext
            } else next
        }

        runCatching { LocalDate.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()?.let { return it }
        parseNumericDate(clean)?.let { return it }

        for (formatter in writtenDateFormatters) {
            val parsed = runCatching {
                if (clean.any(Char::isDigit) && Regex("\\b\\d{4}\\b").containsMatchIn(clean)) {
                    LocalDate.parse(clean, formatter)
                } else {
                    val monthDay = java.time.MonthDay.parse(clean, formatter)
                    monthDay.atYear(today.year).let { if (it.isBefore(today)) it.plusYears(1) else it }
                }
            }.getOrNull()
            if (parsed != null) return parsed
        }
        return null
    }

    private fun parseNumericDate(value: String): LocalDate? {
        val parts = value.split('/', '.', '-').mapNotNull(String::toIntOrNull)
        if (parts.size != 3) return null
        if (parts[0] in 1900..2099) return safeDate(parts[0], parts[1], parts[2])

        val year = if (parts[2] < 100) 2000 + parts[2] else parts[2]
        val first = parts[0]
        val second = parts[1]
        return when {
            first > 12 -> safeDate(year, second, first)
            second > 12 -> safeDate(year, first, second)
            else -> safeDate(year, second, first)
        }
    }

    private fun safeDate(year: Int, month: Int, day: Int): LocalDate? =
        runCatching { LocalDate.of(year, month, day) }.getOrNull()

    private fun parseTime(raw: String): LocalTime? {
        val clean = raw.trim().lowercase(Locale.ROOT).replace(".", "")
        if (clean == "noon") return LocalTime.NOON
        if (clean == "midnight") return LocalTime.MIDNIGHT

        Regex("^(\\d{1,2})h([0-5]\\d)$").matchEntire(clean)?.let {
            return safeTime(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        }

        Regex("^(\\d{1,2})(?::|\\.)([0-5]\\d)\\s*(am|pm)?$").matchEntire(raw.trim().lowercase(Locale.ROOT))?.let {
            return timeFromParts(it.groupValues[1].toInt(), it.groupValues[2].toInt(), it.groupValues[3])
        }
        Regex("^(\\d{1,2})\\s*(am|pm)$").matchEntire(clean)?.let {
            return timeFromParts(it.groupValues[1].toInt(), 0, it.groupValues[2])
        }
        return null
    }

    private fun timeFromParts(hourValue: Int, minute: Int, meridiem: String): LocalTime? {
        val hour = when (meridiem) {
            "am" -> if (hourValue == 12) 0 else hourValue
            "pm" -> if (hourValue == 12) 12 else hourValue + 12
            else -> hourValue
        }
        return safeTime(hour, minute)
    }

    private fun safeTime(hour: Int, minute: Int): LocalTime? =
        runCatching { LocalTime.of(hour, minute) }.getOrNull()

    private fun weekday(value: String): DayOfWeek? {
        val name = value.substringAfterLast(' ').lowercase(Locale.ROOT)
        return DayOfWeek.entries.firstOrNull { it.name.lowercase(Locale.ROOT) == name }
    }

    private fun formatter(pattern: String): DateTimeFormatter = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern(pattern)
        .toFormatter(Locale.ENGLISH)
}

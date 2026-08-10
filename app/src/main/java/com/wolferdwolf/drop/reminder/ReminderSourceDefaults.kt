package com.wolferdwolf.drop.reminder

/**
 * Conservative defaults derived from source text.
 * Only concrete dates and clock times are carried into the editable form;
 * ambiguous relative language is intentionally ignored.
 */
object ReminderSourceDefaults {
    private val isoDate = Regex("\\b(20\\d{2}-\\d{2}-\\d{2})\\b")
    private val time24Hour = Regex("\\b([01]\\d|2[0-3]):([0-5]\\d)\\b")
    private val time12Hour = Regex("\\b(1[0-2]|0?[1-9]):([0-5]\\d)\\s*([AaPp][Mm])\\b")

    data class Values(val date: String?, val time: String?)

    fun from(sourceText: String): Values {
        val date = isoDate.find(sourceText)?.groupValues?.getOrNull(1)
        val time = time24Hour.find(sourceText)?.value
            ?: time12Hour.find(sourceText)?.let { match ->
                val hour = match.groupValues[1].toInt()
                val minute = match.groupValues[2]
                val suffix = match.groupValues[3].uppercase()
                val convertedHour = when {
                    suffix == "AM" && hour == 12 -> 0
                    suffix == "PM" && hour != 12 -> hour + 12
                    else -> hour
                }
                "%02d:%s".format(convertedHour, minute)
            }
        return Values(date = date, time = time)
    }
}

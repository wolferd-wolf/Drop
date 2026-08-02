package com.wolferdwolf.drop.timetable

data class TimetableEntry(
    val time: String,
    val label: String,
    val confidence: Float = 1f
)

data class TimetableDocument(
    val title: String,
    val entries: List<TimetableEntry>,
    val originalText: String
)

object TimetableParser {
    private val timePattern = Regex("(?<!\\d)([01]?\\d|2[0-3])[.:\\-]([0-5]\\d)(?!\\d)")

    fun parse(text: String): TimetableDocument? {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        val entries = lines.mapNotNull(::parseLine)
            .distinctBy { "${it.time}|${it.label.lowercase()}" }

        if (entries.size < MIN_TIMETABLE_ROWS) return null

        val title = lines.firstOrNull { line ->
            !timePattern.containsMatchIn(line) && line.length in 3..80
        } ?: "Imported timetable"

        return TimetableDocument(title, entries, text.trim())
    }

    fun normalizeTime(raw: String): String? {
        val match = timePattern.find(raw) ?: return null
        val hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        return "%02d:%02d".format(hour, minute)
    }

    private fun parseLine(line: String): TimetableEntry? {
        val match = timePattern.find(line) ?: return null
        val normalized = normalizeTime(match.value) ?: return null
        val label = (line.removeRange(match.range)).trim(' ', '-', '–', '—', ':', '|')
        return TimetableEntry(normalized, label)
    }

    const val MIN_TIMETABLE_ROWS = 3
}

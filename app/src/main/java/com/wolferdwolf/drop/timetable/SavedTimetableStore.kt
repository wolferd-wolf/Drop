package com.wolferdwolf.drop.timetable

import android.content.Context
import android.util.Base64

data class SavedTimetable(
    val id: Long,
    val title: String,
    val entries: List<TimetableEntry>,
    val sourceText: String,
    val createdAtEpochMillis: Long
)

class SavedTimetableStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<SavedTimetable> = preferences.getStringSet(KEY_ITEMS, emptySet()).orEmpty()
        .mapNotNull(::decode)
        .sortedByDescending(SavedTimetable::createdAtEpochMillis)

    fun save(
        title: String,
        entries: List<TimetableEntry>,
        sourceText: String,
        now: Long = System.currentTimeMillis()
    ): SavedTimetable {
        require(entries.isNotEmpty()) { "A timetable needs at least one entry" }
        val item = SavedTimetable(
            id = now,
            title = title.trim().ifBlank { "Imported timetable" }.take(80),
            entries = entries.map { it.copy(time = it.time.trim(), label = it.label.trim()) },
            sourceText = sourceText.trim(),
            createdAtEpochMillis = now
        )
        val updated = preferences.getStringSet(KEY_ITEMS, emptySet()).orEmpty().toMutableSet()
        updated += encode(item)
        check(preferences.edit().putStringSet(KEY_ITEMS, updated).commit()) { "Unable to save timetable" }
        return item
    }

    fun delete(id: Long) {
        val updated = preferences.getStringSet(KEY_ITEMS, emptySet()).orEmpty()
            .filterNot { decode(it)?.id == id }
            .toSet()
        preferences.edit().putStringSet(KEY_ITEMS, updated).apply()
    }

    private fun encode(item: SavedTimetable): String = listOf(
        VERSION,
        item.id.toString(),
        item.createdAtEpochMillis.toString(),
        encodeText(item.title),
        encodeText(item.sourceText),
        encodeText(item.entries.joinToString("\u001e") { "${it.time}\u001f${it.label}" })
    ).joinToString("|")

    private fun decode(value: String): SavedTimetable? = runCatching {
        val parts = value.split('|', limit = 6)
        if (parts.size != 6 || parts[0] != VERSION) return null
        val entries = decodeText(parts[5]).split("\u001e").filter(String::isNotBlank).mapNotNull { row ->
            val pair = row.split("\u001f", limit = 2)
            if (pair.size != 2) null else TimetableEntry(pair[0], pair[1])
        }
        if (entries.isEmpty()) return null
        SavedTimetable(
            id = parts[1].toLong(),
            createdAtEpochMillis = parts[2].toLong(),
            title = decodeText(parts[3]),
            sourceText = decodeText(parts[4]),
            entries = entries
        )
    }.getOrNull()

    private fun encodeText(value: String): String = Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP)
    private fun decodeText(value: String): String = String(Base64.decode(value, Base64.NO_WRAP))

    private companion object {
        const val VERSION = "1"
        const val PREFERENCES_NAME = "drop_saved_timetables"
        const val KEY_ITEMS = "items"
    }
}

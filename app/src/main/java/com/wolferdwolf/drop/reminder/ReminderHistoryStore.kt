package com.wolferdwolf.drop.reminder

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.util.UUID

data class ReminderRecord(
    val id: String,
    val title: String,
    val notes: String,
    val triggerAtMillis: Long,
    val createdAtMillis: Long
)

class ReminderHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun save(reminder: ReminderValidator.ValidReminder): ReminderRecord {
        val record = ReminderRecord(
            id = UUID.randomUUID().toString(),
            title = reminder.title,
            notes = reminder.notes,
            triggerAtMillis = reminder.triggerAtMillis,
            createdAtMillis = System.currentTimeMillis()
        )
        val encoded = load().map(Codec::encode).toMutableSet().apply { add(Codec.encode(record)) }
        check(preferences.edit().putStringSet(KEY_RECORDS, encoded).commit()) {
            "Reminder history could not be saved"
        }
        return record
    }

    fun load(): List<ReminderRecord> = preferences.getStringSet(KEY_RECORDS, emptySet())
        .orEmpty()
        .mapNotNull(Codec::decode)
        .sortedByDescending(ReminderRecord::createdAtMillis)

    fun delete(id: String) {
        val encoded = load().filterNot { it.id == id }.map(Codec::encode).toSet()
        check(preferences.edit().putStringSet(KEY_RECORDS, encoded).commit()) {
            "Reminder history could not be updated"
        }
    }

    internal object Codec {
        private const val VERSION = "1"

        fun encode(record: ReminderRecord): String = listOf(
            VERSION,
            encodeText(record.id),
            encodeText(record.title),
            encodeText(record.notes),
            record.triggerAtMillis.toString(),
            record.createdAtMillis.toString()
        ).joinToString("|")

        fun decode(value: String): ReminderRecord? = runCatching {
            val parts = value.split('|')
            require(parts.size == 6 && parts[0] == VERSION)
            ReminderRecord(
                id = decodeText(parts[1]),
                title = decodeText(parts[2]),
                notes = decodeText(parts[3]),
                triggerAtMillis = parts[4].toLong(),
                createdAtMillis = parts[5].toLong()
            )
        }.getOrNull()

        private fun encodeText(value: String): String = Base64.encodeToString(
            value.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP or Base64.URL_SAFE
        )

        private fun decodeText(value: String): String = String(
            Base64.decode(value, Base64.NO_WRAP or Base64.URL_SAFE),
            StandardCharsets.UTF_8
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "reminder_history"
        const val KEY_RECORDS = "records"
    }
}

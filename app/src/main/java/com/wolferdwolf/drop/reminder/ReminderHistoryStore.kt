package com.wolferdwolf.drop.reminder

import android.content.Context
import java.nio.charset.StandardCharsets
import java.util.Base64
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
        private val encoder = Base64.getUrlEncoder().withoutPadding()
        private val decoder = Base64.getUrlDecoder()

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

        private fun encodeText(value: String): String = encoder.encodeToString(
            value.toByteArray(StandardCharsets.UTF_8)
        )

        private fun decodeText(value: String): String = String(
            decoder.decode(value),
            StandardCharsets.UTF_8
        )
    }

    private companion object {
        const val PREFERENCES_NAME = "reminder_history"
        const val KEY_RECORDS = "records"
    }
}

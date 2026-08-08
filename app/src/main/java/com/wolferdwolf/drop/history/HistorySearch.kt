package com.wolferdwolf.drop.history

import com.wolferdwolf.drop.data.SavedSourceType
import java.text.Normalizer
import java.util.Calendar

enum class HistoryItemFilter {
    ALL,
    REFERENCES,
    REMINDERS
}

enum class HistoryDateFilter {
    ALL,
    TODAY,
    LAST_7_DAYS,
    LAST_30_DAYS
}

enum class HistorySourceFilter {
    ALL,
    TEXT,
    LINK,
    IMAGE,
    PDF
}

object HistorySearch {
    fun matches(query: String, vararg fields: String): Boolean {
        val terms = query.trim()
            .split(Regex("\\s+"))
            .map(::normalize)
            .filter { it.isNotBlank() }
        if (terms.isEmpty()) return true
        val normalizedFields = fields.map(::normalize)
        return terms.all { term -> normalizedFields.any { field -> field.contains(term) } }
    }

    fun includesReferences(filter: HistoryItemFilter): Boolean =
        filter == HistoryItemFilter.ALL || filter == HistoryItemFilter.REFERENCES

    fun includesReminders(filter: HistoryItemFilter): Boolean =
        filter == HistoryItemFilter.ALL || filter == HistoryItemFilter.REMINDERS

    fun matchesSource(filter: HistorySourceFilter, sourceType: SavedSourceType): Boolean = when (filter) {
        HistorySourceFilter.ALL -> true
        HistorySourceFilter.TEXT -> sourceType == SavedSourceType.TEXT
        HistorySourceFilter.LINK -> sourceType == SavedSourceType.LINK
        HistorySourceFilter.IMAGE -> sourceType == SavedSourceType.IMAGE
        HistorySourceFilter.PDF -> sourceType == SavedSourceType.PDF
    }

    fun matchesDate(
        filter: HistoryDateFilter,
        createdAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (filter == HistoryDateFilter.ALL) return true
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(
                Calendar.DAY_OF_YEAR,
                when (filter) {
                    HistoryDateFilter.TODAY -> 0
                    HistoryDateFilter.LAST_7_DAYS -> -6
                    HistoryDateFilter.LAST_30_DAYS -> -29
                    HistoryDateFilter.ALL -> 0
                }
            )
        }
        return createdAtMillis in calendar.timeInMillis..nowMillis
    }

    private fun normalize(value: String): String {
        val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
        return buildString(decomposed.length) {
            decomposed.forEach { character ->
                if (character.isLetterOrDigit()) append(character.lowercaseChar())
            }
        }
    }
}

package com.wolferdwolf.drop.history

enum class HistoryItemFilter {
    ALL,
    REFERENCES,
    REMINDERS
}

object HistorySearch {
    fun matches(query: String, vararg fields: String): Boolean {
        val normalized = query.trim()
        if (normalized.isEmpty()) return true
        return fields.any { it.contains(normalized, ignoreCase = true) }
    }

    fun includesReferences(filter: HistoryItemFilter): Boolean =
        filter == HistoryItemFilter.ALL || filter == HistoryItemFilter.REFERENCES

    fun includesReminders(filter: HistoryItemFilter): Boolean =
        filter == HistoryItemFilter.ALL || filter == HistoryItemFilter.REMINDERS
}

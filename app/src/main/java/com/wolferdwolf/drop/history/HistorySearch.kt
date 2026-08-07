package com.wolferdwolf.drop.history

enum class HistoryItemFilter {
    ALL,
    REFERENCES,
    REMINDERS
}

object HistorySearch {
    fun matches(query: String, vararg fields: String): Boolean {
        val terms = query.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (terms.isEmpty()) return true
        return terms.all { term -> fields.any { field -> field.contains(term, ignoreCase = true) } }
    }

    fun includesReferences(filter: HistoryItemFilter): Boolean =
        filter == HistoryItemFilter.ALL || filter == HistoryItemFilter.REFERENCES

    fun includesReminders(filter: HistoryItemFilter): Boolean =
        filter == HistoryItemFilter.ALL || filter == HistoryItemFilter.REMINDERS
}

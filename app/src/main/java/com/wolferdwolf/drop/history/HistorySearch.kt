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

    private fun normalize(value: String): String = buildString(value.length) {
        value.forEach { character ->
            if (character.isLetterOrDigit()) append(character.lowercaseChar())
        }
    }
}

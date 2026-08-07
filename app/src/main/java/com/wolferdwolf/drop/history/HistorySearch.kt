package com.wolferdwolf.drop.history

object HistorySearch {
    fun matches(query: String, vararg fields: String): Boolean {
        val normalized = query.trim()
        if (normalized.isEmpty()) return true
        return fields.any { it.contains(normalized, ignoreCase = true) }
    }
}

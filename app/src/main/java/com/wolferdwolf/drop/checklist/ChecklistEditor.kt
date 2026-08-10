package com.wolferdwolf.drop.checklist

data class ChecklistItem(
    val text: String,
    val checked: Boolean = false
)

object ChecklistEditor {
    private const val MAX_ITEM_LENGTH = 240
    private val leadingMarker = Regex("""^(?:[-•*]|\d+[.)])\s+""")
    private val taskMarker = Regex("""^\[(?<state>[ xX])\]\s*""")
    private val unicodeTaskMarker = Regex("""^(?<state>[☐☒☑])\s*""")

    fun fromSource(source: String): List<ChecklistItem> = source.lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .map(::parseSourceLine)
        .filter { it.text.isNotBlank() }
        .toList()

    fun edit(items: List<ChecklistItem>, index: Int, value: String): List<ChecklistItem> =
        items.mapIndexed { itemIndex, item ->
            if (itemIndex == index) item.copy(text = value.take(MAX_ITEM_LENGTH)) else item
        }

    fun add(items: List<ChecklistItem>, value: String): List<ChecklistItem> {
        val cleanValue = clean(value)
        return if (cleanValue.isBlank()) items else items + ChecklistItem(cleanValue.take(MAX_ITEM_LENGTH))
    }

    fun delete(items: List<ChecklistItem>, index: Int): List<ChecklistItem> =
        items.filterIndexed { itemIndex, _ -> itemIndex != index }

    fun toggle(items: List<ChecklistItem>, index: Int): List<ChecklistItem> =
        items.mapIndexed { itemIndex, item -> if (itemIndex == index) item.copy(checked = !item.checked) else item }

    fun move(items: List<ChecklistItem>, index: Int, delta: Int): List<ChecklistItem> {
        val target = index + delta
        if (index !in items.indices || target !in items.indices || index == target) return items
        return items.toMutableList().apply {
            val moved = removeAt(index)
            add(target, moved)
        }
    }

    fun hasSavableItems(items: List<ChecklistItem>): Boolean = items.any { it.text.isNotBlank() }

    fun serializeForSave(items: List<ChecklistItem>): String = items
        .filter { it.text.isNotBlank() }
        .joinToString("\n") { item -> "${if (item.checked) "☒" else "☐"} ${item.text.trim()}" }

    fun encode(items: List<ChecklistItem>): String = items.joinToString("\n") { item ->
        "${if (item.checked) '1' else '0'}|${item.text.replace("\n", " ")}"
    }

    fun decode(encoded: String): List<ChecklistItem> = encoded.lineSequence()
        .filter(String::isNotBlank)
        .map { line ->
            val checked = line.startsWith("1|")
            ChecklistItem(line.substringAfter('|', line).take(MAX_ITEM_LENGTH), checked)
        }
        .toList()

    private fun parseSourceLine(value: String): ChecklistItem {
        val withoutListMarker = value.replace(leadingMarker, "").trim()
        taskMarker.find(withoutListMarker)?.let { match ->
            val state = match.groups["state"]?.value.orEmpty()
            val text = withoutListMarker.removeRange(match.range).trim().take(MAX_ITEM_LENGTH)
            return ChecklistItem(text, checked = state.equals("x", ignoreCase = true))
        }
        unicodeTaskMarker.find(withoutListMarker)?.let { match ->
            val state = match.groups["state"]?.value.orEmpty()
            val text = withoutListMarker.removeRange(match.range).trim().take(MAX_ITEM_LENGTH)
            return ChecklistItem(text, checked = state == "☒" || state == "☑")
        }
        return ChecklistItem(withoutListMarker.take(MAX_ITEM_LENGTH))
    }

    private fun clean(value: String): String = value.trim().replace(leadingMarker, "").trim().take(MAX_ITEM_LENGTH)
}

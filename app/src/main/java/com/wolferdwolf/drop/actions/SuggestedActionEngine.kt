package com.wolferdwolf.drop.actions

import com.wolferdwolf.drop.extraction.ExtractionResult
import com.wolferdwolf.drop.extraction.ExtractionType

enum class SuggestedActionType {
    SAVE_REFERENCE,
    REMINDER,
    CALENDAR,
    CHECKLIST,
    CONTACT,
    MAPS,
    OPEN_LINK,
    EMAIL,
    CALL
}

data class SuggestedAction(
    val type: SuggestedActionType,
    val title: String,
    val reason: String,
    val priority: Int
)

object SuggestedActionEngine {
    private const val MAX_VISIBLE_ACTIONS = 5
    private const val MAX_RELEVANT_ACTIONS = 4
    private const val MANUAL_PRIORITY = 40

    fun suggest(originalText: String, results: List<ExtractionResult>): List<SuggestedAction> {
        val types = results.mapTo(mutableSetOf()) { it.type }
        val lower = originalText.lowercase()
        val relevant = mutableListOf<SuggestedAction>()

        relevant += SuggestedAction(
            SuggestedActionType.SAVE_REFERENCE,
            "Save reference",
            "Keep the original content and extracted details in Drop.",
            100
        )

        if (ExtractionType.DATE in types || ExtractionType.TIME in types || containsDeadlineLanguage(lower)) {
            relevant += SuggestedAction(
                SuggestedActionType.REMINDER,
                "Create reminder",
                "A date, time, or deadline-like phrase was detected.",
                95
            )
        }

        if (ExtractionType.DATE in types && (ExtractionType.TIME in types || containsEventLanguage(lower))) {
            relevant += SuggestedAction(
                SuggestedActionType.CALENDAR,
                "Add calendar event",
                "The content looks like it may describe an event.",
                90
            )
        }

        if (looksLikeChecklist(originalText)) {
            relevant += SuggestedAction(
                SuggestedActionType.CHECKLIST,
                "Create checklist",
                "The content contains several list-like lines.",
                82
            )
        }

        if (ExtractionType.PHONE in types || ExtractionType.EMAIL in types) {
            relevant += SuggestedAction(
                SuggestedActionType.CONTACT,
                "Save contact",
                "A phone number or email address was detected.",
                86
            )
        }

        if (looksLikeAddress(lower)) {
            relevant += SuggestedAction(
                SuggestedActionType.MAPS,
                "Open in Maps",
                "The content contains address or venue language.",
                78
            )
        }

        if (ExtractionType.URL in types) {
            relevant += SuggestedAction(
                SuggestedActionType.OPEN_LINK,
                "Open link",
                "A web link was detected.",
                84
            )
        }

        if (ExtractionType.EMAIL in types) {
            relevant += SuggestedAction(
                SuggestedActionType.EMAIL,
                "Send email",
                "An email address was detected.",
                80
            )
        }

        if (ExtractionType.PHONE in types) {
            relevant += SuggestedAction(
                SuggestedActionType.CALL,
                "Call number",
                "A phone number was detected.",
                79
            )
        }

        val ranked = relevant
            .distinctBy(SuggestedAction::type)
            .sortedByDescending(SuggestedAction::priority)
            .take(MAX_RELEVANT_ACTIONS)
            .toMutableList()

        manualChoice(originalText, ranked.mapTo(mutableSetOf(), SuggestedAction::type))?.let(ranked::add)
        return ranked.take(MAX_VISIBLE_ACTIONS)
    }

    private fun manualChoice(text: String, visibleTypes: Set<SuggestedActionType>): SuggestedAction? {
        val candidates = buildList {
            if (SuggestedActionType.CHECKLIST !in visibleTypes) add(
                SuggestedAction(
                    SuggestedActionType.CHECKLIST,
                    "Create checklist manually",
                    "Manual choice: turn the imported content into an editable checklist.",
                    MANUAL_PRIORITY
                )
            )
            if (SuggestedActionType.REMINDER !in visibleTypes) add(
                SuggestedAction(
                    SuggestedActionType.REMINDER,
                    "Set a reminder manually",
                    "Manual choice: choose the reminder date and time yourself.",
                    MANUAL_PRIORITY
                )
            )
            if (SuggestedActionType.CALENDAR !in visibleTypes) add(
                SuggestedAction(
                    SuggestedActionType.CALENDAR,
                    "Create event manually",
                    "Manual choice: fill in the calendar event details yourself.",
                    MANUAL_PRIORITY
                )
            )
            if (SuggestedActionType.MAPS !in visibleTypes) add(
                SuggestedAction(
                    SuggestedActionType.MAPS,
                    "Search in Maps manually",
                    "Manual choice: search Maps using the imported content.",
                    MANUAL_PRIORITY
                )
            )
        }

        return when {
            looksLikeChecklist(text) -> candidates.firstOrNull { it.type == SuggestedActionType.REMINDER }
            containsEventLanguage(text.lowercase()) -> candidates.firstOrNull { it.type == SuggestedActionType.CHECKLIST }
            else -> candidates.firstOrNull()
        }
    }

    private fun containsDeadlineLanguage(text: String): Boolean =
        listOf("deadline", "due date", "last date", "apply by", "before ").any(text::contains)

    private fun containsEventLanguage(text: String): Boolean =
        listOf("event", "meeting", "appointment", "conference", "festival", "class", "interview").any(text::contains)

    private fun looksLikeChecklist(text: String): Boolean {
        val meaningfulLines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        val marked = meaningfulLines.count {
            it.startsWith("-") || it.startsWith("•") || it.matches(Regex("^\\d+[.)].+"))
        }
        return meaningfulLines.size >= 3 && (marked >= 2 || meaningfulLines.size >= 5)
    }

    private fun looksLikeAddress(text: String): Boolean =
        listOf("address", "venue", "road", "street", "nagar", "colony", "building", "near ", "opposite", "district").any(text::contains)
}

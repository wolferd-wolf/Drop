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
    fun suggest(originalText: String, results: List<ExtractionResult>): List<SuggestedAction> {
        val types = results.mapTo(mutableSetOf()) { it.type }
        val lower = originalText.lowercase()
        val actions = mutableListOf<SuggestedAction>()

        actions += SuggestedAction(
            SuggestedActionType.SAVE_REFERENCE,
            "Save reference",
            "Keep the original content and extracted details in Drop.",
            100
        )

        if (ExtractionType.DATE in types || ExtractionType.TIME in types || containsDeadlineLanguage(lower)) {
            actions += SuggestedAction(
                SuggestedActionType.REMINDER,
                "Create reminder",
                "A date, time, or deadline-like phrase was detected.",
                95
            )
        }

        if (ExtractionType.DATE in types && (ExtractionType.TIME in types || containsEventLanguage(lower))) {
            actions += SuggestedAction(
                SuggestedActionType.CALENDAR,
                "Add calendar event",
                "The content looks like it may describe an event.",
                90
            )
        }

        if (looksLikeChecklist(originalText)) {
            actions += SuggestedAction(
                SuggestedActionType.CHECKLIST,
                "Create checklist",
                "The content contains several list-like lines.",
                82
            )
        }

        if (ExtractionType.PHONE in types || ExtractionType.EMAIL in types) {
            actions += SuggestedAction(
                SuggestedActionType.CONTACT,
                "Save contact",
                "A phone number or email address was detected.",
                86
            )
        }

        if (looksLikeAddress(lower)) {
            actions += SuggestedAction(
                SuggestedActionType.MAPS,
                "Open in Maps",
                "The content contains address or venue language.",
                78
            )
        }

        if (ExtractionType.URL in types) {
            actions += SuggestedAction(
                SuggestedActionType.OPEN_LINK,
                "Open link",
                "A web link was detected.",
                84
            )
        }

        if (ExtractionType.EMAIL in types) {
            actions += SuggestedAction(
                SuggestedActionType.EMAIL,
                "Send email",
                "An email address was detected.",
                80
            )
        }

        if (ExtractionType.PHONE in types) {
            actions += SuggestedAction(
                SuggestedActionType.CALL,
                "Call number",
                "A phone number was detected.",
                79
            )
        }

        addManualChoiceIfMissing(
            actions,
            SuggestedActionType.REMINDER,
            "Create reminder",
            "Manual choice: set a reminder even though no clear deadline was detected."
        )
        addManualChoiceIfMissing(
            actions,
            SuggestedActionType.CALENDAR,
            "Add calendar event",
            "Manual choice: create an event and fill in its details yourself."
        )
        addManualChoiceIfMissing(
            actions,
            SuggestedActionType.CHECKLIST,
            "Create checklist",
            "Manual choice: turn the imported content into an editable checklist."
        )
        addManualChoiceIfMissing(
            actions,
            SuggestedActionType.MAPS,
            "Open in Maps",
            "Manual choice: search Maps using the imported content."
        )

        return actions.sortedByDescending(SuggestedAction::priority)
    }

    private fun addManualChoiceIfMissing(
        actions: MutableList<SuggestedAction>,
        type: SuggestedActionType,
        title: String,
        reason: String
    ) {
        if (actions.none { it.type == type }) {
            actions += SuggestedAction(type, title, reason, 40)
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

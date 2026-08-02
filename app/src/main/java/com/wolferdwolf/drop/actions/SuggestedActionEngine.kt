package com.wolferdwolf.drop.actions

import com.wolferdwolf.drop.extraction.AddressCandidateDetector
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
    private const val MAX_RELEVANT_ACTIONS = 4

    fun suggest(originalText: String, results: List<ExtractionResult>): List<SuggestedAction> {
        val types = results.mapTo(mutableSetOf()) { it.type }
        val lower = originalText.lowercase()
        val address = AddressCandidateDetector.detect(originalText)
        val relevant = mutableListOf<SuggestedAction>()

        relevant += action(
            SuggestedActionType.SAVE_REFERENCE,
            "Save reference",
            "Keep the original content and extracted details in Drop.",
            100
        )

        if (ExtractionType.DATE in types || ExtractionType.TIME in types || containsDeadlineLanguage(lower)) {
            relevant += action(
                SuggestedActionType.REMINDER,
                "Create reminder",
                "A date, time, or deadline-like phrase was detected.",
                95
            )
        }

        if (ExtractionType.DATE in types && (ExtractionType.TIME in types || containsEventLanguage(lower))) {
            relevant += action(
                SuggestedActionType.CALENDAR,
                "Add calendar event",
                "The content looks like it may describe an event.",
                90
            )
        }

        if (looksLikeChecklist(originalText)) {
            relevant += action(
                SuggestedActionType.CHECKLIST,
                "Create checklist",
                "The content contains several list-like lines.",
                82
            )
        }

        if (ExtractionType.PHONE in types || ExtractionType.EMAIL in types) {
            relevant += action(
                SuggestedActionType.CONTACT,
                "Save contact",
                "A phone number or email address was detected.",
                86
            )
        }

        if (address != null) {
            relevant += action(
                SuggestedActionType.MAPS,
                "Open in Maps",
                "A likely address or venue was detected: ${address.value.take(71)}",
                78
            )
        }

        if (ExtractionType.URL in types) {
            relevant += action(
                SuggestedActionType.OPEN_LINK,
                "Open link",
                "A web link was detected.",
                84
            )
        }

        if (ExtractionType.EMAIL in types) {
            relevant += action(
                SuggestedActionType.EMAIL,
                "Send email",
                "An email address was detected.",
                80
            )
        }

        if (ExtractionType.PHONE in types) {
            relevant += action(
                SuggestedActionType.CALL,
                "Call number",
                "A phone number was detected.",
                79
            )
        }

        return relevant
            .distinctBy(SuggestedAction::type)
            .sortedByDescending(SuggestedAction::priority)
            .take(MAX_RELEVANT_ACTIONS)
    }

    fun manualActions(results: List<ExtractionResult>): List<SuggestedAction> {
        val types = results.mapTo(mutableSetOf()) { it.type }
        return buildList {
            add(action(SuggestedActionType.SAVE_REFERENCE, "Save reference", "Save the imported content in Drop.", 0))
            add(action(SuggestedActionType.REMINDER, "Create reminder", "Choose the reminder title, date, and time.", 0))
            add(action(SuggestedActionType.CALENDAR, "Add calendar event", "Fill in the event details in your calendar app.", 0))
            add(action(SuggestedActionType.CHECKLIST, "Create checklist", "Turn the content into an editable checklist.", 0))
            add(action(SuggestedActionType.MAPS, "Search in Maps", "Search Maps using the imported content.", 0))
            if (ExtractionType.PHONE in types || ExtractionType.EMAIL in types) {
                add(action(SuggestedActionType.CONTACT, "Save contact", "Use the detected phone number or email address.", 0))
            }
            if (ExtractionType.URL in types) {
                add(action(SuggestedActionType.OPEN_LINK, "Open link", "Open the detected web link.", 0))
            }
            if (ExtractionType.EMAIL in types) {
                add(action(SuggestedActionType.EMAIL, "Send email", "Compose an email to the detected address.", 0))
            }
            if (ExtractionType.PHONE in types) {
                add(action(SuggestedActionType.CALL, "Call number", "Open the dialer with the detected number.", 0))
            }
        }
    }

    private fun action(type: SuggestedActionType, title: String, reason: String, priority: Int) =
        SuggestedAction(type, title, reason, priority)

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
}

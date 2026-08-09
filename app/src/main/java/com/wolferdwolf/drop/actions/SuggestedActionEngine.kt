package com.wolferdwolf.drop.actions

import com.wolferdwolf.drop.extraction.DocumentCategory
import com.wolferdwolf.drop.extraction.DocumentCategoryDetector
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
        val address = results.firstOrNull { it.type == ExtractionType.ADDRESS }
        val category = DocumentCategoryDetector.detect(originalText, results)
        val hasDeadlineLanguage = containsDeadlineLanguage(lower)
        val hasEventLanguage = containsEventLanguage(lower)
        val hasPhone = ExtractionType.PHONE in types
        val hasEmail = ExtractionType.EMAIL in types
        val relevant = mutableListOf<SuggestedAction>()

        relevant += action(
            SuggestedActionType.SAVE_REFERENCE,
            "Save reference",
            when (category) {
                DocumentCategory.RECEIPT -> "Keep this receipt and its detected prices in Drop."
                DocumentCategory.JOB_POST -> "Keep this job post and its application details in Drop."
                DocumentCategory.EVENT -> "Keep this event information and its detected details in Drop."
                DocumentCategory.GENERAL_REFERENCE -> "Keep the original content and extracted details in Drop."
            },
            if (category == DocumentCategory.RECEIPT) 100 else 72
        )

        if (ExtractionType.DATE in types) {
            relevant += action(
                SuggestedActionType.REMINDER,
                "Create reminder",
                if (hasDeadlineLanguage) {
                    "A deadline-like phrase and date were detected."
                } else {
                    "A date was detected."
                },
                95
            )
        }

        val isLikelyEvent = ExtractionType.DATE in types && (
            category == DocumentCategory.EVENT || hasEventLanguage
        )
        if (isLikelyEvent) {
            relevant += action(
                SuggestedActionType.CALENDAR,
                "Add calendar event",
                "The content looks like it may describe an event.",
                90
            )
        }

        if (category != DocumentCategory.RECEIPT && looksLikeChecklist(originalText)) {
            relevant += action(
                SuggestedActionType.CHECKLIST,
                "Create checklist",
                "The content contains several list-like lines.",
                82
            )
        }

        if (hasPhone || hasEmail) {
            relevant += action(
                SuggestedActionType.CONTACT,
                "Save contact",
                when {
                    hasPhone && hasEmail -> "A phone number and email address were detected for the same imported content."
                    hasPhone -> "A phone number was detected; save it as a contact if you want to keep it."
                    else -> "An email address was detected; save it as a contact if you want to keep it."
                },
                if (hasPhone && hasEmail) 86 else 76
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
                if (category == DocumentCategory.JOB_POST) {
                    "An application or information link was detected."
                } else {
                    "A web link was detected."
                },
                if (category == DocumentCategory.JOB_POST) 88 else 84
            )
        }

        if (hasEmail) {
            relevant += action(
                SuggestedActionType.EMAIL,
                "Send email",
                "An email address was detected, so you can act on it directly.",
                if (!hasPhone) 90 else 80
            )
        }

        if (hasPhone) {
            relevant += action(
                SuggestedActionType.CALL,
                "Call number",
                "A phone number was detected, so you can act on it directly.",
                if (!hasEmail) 90 else 79
            )
        }

        return relevant
            .distinctBy(SuggestedAction::type)
            .sortedByDescending(SuggestedAction::priority)
            .take(MAX_RELEVANT_ACTIONS)
    }

    fun manualActions(results: List<ExtractionResult>): List<SuggestedAction> {
        return buildList {
            add(action(SuggestedActionType.SAVE_REFERENCE, "Save reference", "Save the imported content in Drop.", 0))
            add(action(SuggestedActionType.REMINDER, "Create reminder", "Choose the reminder title, date, and time.", 0))
            add(action(SuggestedActionType.CALENDAR, "Add calendar event", "Review and edit the event details before opening Calendar.", 0))
            add(action(SuggestedActionType.CHECKLIST, "Create checklist", "Turn the content into an editable checklist.", 0))
            add(action(SuggestedActionType.CONTACT, "Save contact", "Enter or edit contact details before opening Contacts.", 0))
            add(action(SuggestedActionType.MAPS, "Search in Maps", "Review and edit a location before opening Maps.", 0))
            add(action(SuggestedActionType.OPEN_LINK, "Open link", "Enter or edit a website link before opening your browser.", 0))
            add(action(SuggestedActionType.EMAIL, "Send email", "Enter or edit the recipient before opening your email app.", 0))
            add(action(SuggestedActionType.CALL, "Call number", "Enter or edit a phone number before opening the dialer.", 0))
        }
    }

    private fun action(type: SuggestedActionType, title: String, reason: String, priority: Int) =
        SuggestedAction(type, title, reason, priority)

    private fun containsDeadlineLanguage(text: String): Boolean =
        listOf("deadline", "due date", "due by", "last date", "apply by", "apply before", "submit by", "submit before", "respond by", "pay by").any(text::contains)

    private fun containsEventLanguage(text: String): Boolean =
        listOf("event", "meeting", "appointment", "conference", "festival", "class", "interview", "launch", "workshop").any(text::contains)

    private fun looksLikeChecklist(text: String): Boolean {
        val meaningfulLines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        if (meaningfulLines.size < 3) return false

        val marked = meaningfulLines.count {
            it.startsWith("-") || it.startsWith("•") || it.matches(Regex("^\\d+[.)].+"))
        }
        if (marked >= 2) return true

        if (meaningfulLines.size > 12) return false
        val compactEntries = meaningfulLines.count { line ->
            val wordCount = line.split(Regex("\\s+")).count(String::isNotBlank)
            line.length <= 72 && wordCount in 1..8 && !line.endsWith('.') && !line.endsWith('!') && !line.endsWith('?')
        }
        val requiredCompactEntries = maxOf(3, (meaningfulLines.size * 4 + 4) / 5)
        return compactEntries >= requiredCompactEntries
    }
}

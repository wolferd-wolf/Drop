package com.wolferdwolf.drop.document

import com.wolferdwolf.drop.timetable.TimetableParser

sealed interface DocumentProfile {
    val kind: Kind
    val confidence: Float

    enum class Kind {
        TIMETABLE,
        EVENT_POSTER,
        RECEIPT,
        JOB_POST,
        MEDICINE_SCHEDULE,
        GENERAL_REFERENCE
    }

    data class Timetable(
        val title: String,
        val rowCount: Int,
        override val confidence: Float
    ) : DocumentProfile {
        override val kind: Kind = Kind.TIMETABLE
    }

    data class Generic(
        override val kind: Kind,
        override val confidence: Float
    ) : DocumentProfile
}

object DocumentProfileClassifier {
    fun classify(text: String): DocumentProfile {
        TimetableParser.parse(text)?.let { document ->
            val confidence = (0.65f + document.entries.size.coerceAtMost(7) * 0.05f).coerceAtMost(0.98f)
            return DocumentProfile.Timetable(document.title, document.entries.size, confidence)
        }

        val normalized = text.lowercase()
        return when {
            listOf("total", "subtotal", "invoice", "amount", "₹", "rs.").count(normalized::contains) >= 2 ->
                DocumentProfile.Generic(DocumentProfile.Kind.RECEIPT, 0.72f)
            listOf("apply", "salary", "experience", "qualification", "vacancy").count(normalized::contains) >= 2 ->
                DocumentProfile.Generic(DocumentProfile.Kind.JOB_POST, 0.70f)
            listOf("venue", "date", "time", "event", "programme").count(normalized::contains) >= 3 ->
                DocumentProfile.Generic(DocumentProfile.Kind.EVENT_POSTER, 0.68f)
            listOf("tablet", "capsule", "morning", "night", "dose").count(normalized::contains) >= 2 ->
                DocumentProfile.Generic(DocumentProfile.Kind.MEDICINE_SCHEDULE, 0.66f)
            else -> DocumentProfile.Generic(DocumentProfile.Kind.GENERAL_REFERENCE, 0.45f)
        }
    }
}

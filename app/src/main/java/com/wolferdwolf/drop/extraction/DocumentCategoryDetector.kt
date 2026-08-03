package com.wolferdwolf.drop.extraction

enum class DocumentCategory {
    EVENT,
    JOB_POST,
    RECEIPT,
    GENERAL_REFERENCE
}

object DocumentCategoryDetector {
    fun detect(text: String, results: List<ExtractionResult>): DocumentCategory {
        val lower = text.lowercase()
        val types = results.mapTo(mutableSetOf()) { it.type }

        if (looksLikeReceipt(lower, types)) return DocumentCategory.RECEIPT
        if (looksLikeJobPost(lower)) return DocumentCategory.JOB_POST
        if (looksLikeEvent(lower, types)) return DocumentCategory.EVENT
        return DocumentCategory.GENERAL_REFERENCE
    }

    private fun looksLikeReceipt(text: String, types: Set<ExtractionType>): Boolean {
        val receiptWords = listOf("receipt", "invoice", "subtotal", "total", "amount paid", "payment", "tax", "gst")
        return ExtractionType.PRICE in types && receiptWords.any(text::contains)
    }

    private fun looksLikeJobPost(text: String): Boolean {
        val jobWords = listOf("job", "vacancy", "hiring", "apply", "application", "salary", "qualification", "experience required")
        return jobWords.count(text::contains) >= 2 ||
            ((text.contains("apply by") || text.contains("apply before")) && jobWords.any(text::contains))
    }

    private fun looksLikeEvent(text: String, types: Set<ExtractionType>): Boolean {
        val eventWords = listOf("event", "meeting", "appointment", "conference", "festival", "class", "interview", "launch", "workshop")
        return ExtractionType.DATE in types && eventWords.any(text::contains)
    }
}

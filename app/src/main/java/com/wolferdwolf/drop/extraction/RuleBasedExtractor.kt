package com.wolferdwolf.drop.extraction

object RuleBasedExtractor {
    private data class Rule(
        val type: ExtractionType,
        val regex: Regex,
        val confidence: Float
    )

    private val rules = listOf(
        Rule(
            ExtractionType.EMAIL,
            Regex("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b"),
            0.98f
        ),
        Rule(
            ExtractionType.URL,
            Regex("(?i)\\b(?:https?://|www\\.)[^\\s<>()]+"),
            0.96f
        ),
        Rule(
            ExtractionType.PHONE,
            Regex("(?<!\\d)(?:\\+91[-\\s]?)?[6-9]\\d{4}[-\\s]?\\d{5}(?!\\d)"),
            0.94f
        ),
        Rule(
            ExtractionType.PHONE,
            Regex("(?<!\\d)\\+?[1-9]\\d{0,2}[-\\s]?(?:\\(?\\d{2,4}\\)?[-\\s]?)?\\d{3,4}[-\\s]?\\d{4}(?!\\d)"),
            0.82f
        ),
        Rule(
            ExtractionType.DATE,
            Regex("(?i)\\b(?:0?[1-9]|[12]\\d|3[01])[/-](?:0?[1-9]|1[0-2])[/-](?:\\d{2}|\\d{4})\\b"),
            0.91f
        ),
        Rule(
            ExtractionType.DATE,
            Regex("(?i)\\b(?:0?[1-9]|[12]\\d|3[01])(?:st|nd|rd|th)?\\s+(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\\s+\\d{2,4})?\\b"),
            0.93f
        ),
        Rule(
            ExtractionType.DATE,
            Regex("(?i)\\b(?:day after tomorrow|today|tonight|(?<!after )tomorrow)\\b"),
            0.90f
        ),
        Rule(
            ExtractionType.DATE,
            Regex("(?i)\\b(?:(?:this|next)\\s+)?(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b"),
            0.88f
        ),
        Rule(
            ExtractionType.TIME,
            Regex("(?i)\\b(?:1[0-2]|0?[1-9])(?::[0-5]\\d)?\\s?(?:am|pm)\\b"),
            0.95f
        ),
        Rule(
            ExtractionType.TIME,
            Regex("(?<!\\d)(?:[01]?\\d|2[0-3]):[0-5]\\d(?!\\d)"),
            0.92f
        ),
        Rule(
            ExtractionType.TIME,
            Regex("(?i)\\b(?:noon|midnight)\\b"),
            0.90f
        )
    )

    fun extract(text: String): List<ExtractionResult> {
        if (text.isBlank()) return emptyList()

        val candidates = rules.flatMap { rule ->
            rule.regex.findAll(text).map { match ->
                ExtractionResult(
                    type = rule.type,
                    value = trimTrailingPunctuation(match.value),
                    sourceStart = match.range.first,
                    sourceEndExclusive = match.range.last + 1,
                    confidence = rule.confidence
                )
            }
        }

        return candidates
            .filter { it.value.isNotBlank() }
            .sortedWith(compareBy<ExtractionResult> { it.sourceStart }.thenByDescending { it.confidence })
            .fold(mutableListOf()) { accepted, candidate ->
                val duplicate = accepted.any {
                    it.type == candidate.type && it.value.equals(candidate.value, ignoreCase = true)
                }
                val overlappingLowerConfidence = accepted.any {
                    rangesOverlap(it, candidate) && it.confidence >= candidate.confidence
                }
                if (!duplicate && !overlappingLowerConfidence) accepted += candidate
                accepted
            }
    }

    private fun rangesOverlap(a: ExtractionResult, b: ExtractionResult): Boolean =
        a.sourceStart < b.sourceEndExclusive && b.sourceStart < a.sourceEndExclusive

    private fun trimTrailingPunctuation(value: String): String =
        value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']')
}

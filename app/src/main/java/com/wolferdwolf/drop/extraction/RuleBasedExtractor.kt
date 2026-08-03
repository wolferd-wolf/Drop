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
            Regex(
                "(?i)\\b(?:(?:https?://|www\\.)[^\\s<>()]+|(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+(?:com|org|net|in|io|app|co|dev)(?:/[^\\s<>()]*)?)"
            ),
            0.96f
        ),
        Rule(
            ExtractionType.PRICE,
            Regex("(?i)(?<![A-Z0-9])(?:₹|Rs\\.?|INR|\\$|USD|€|EUR|£|GBP|¥|JPY)\\s*\\d{1,3}(?:(?:,\\d{2})*,\\d{3}|(?:,\\d{3})*|\\d*)(?:\\.\\d{1,2})?(?![\\d/])"),
            0.97f
        ),
        Rule(
            ExtractionType.PRICE,
            Regex("(?i)(?<![\\d/])\\d{1,3}(?:(?:,\\d{2})*,\\d{3}|(?:,\\d{3})*|\\d*)(?:\\.\\d{1,2})?\\s*(?:INR|USD|EUR|GBP|JPY)\\b"),
            0.95f
        ),
        Rule(
            ExtractionType.PHONE,
            Regex("(?<![\\d/])(?:\\+91[-.\\s]?)?[6-9]\\d{4}[-.\\s]?\\d{5}(?![\\d/])"),
            0.96f
        ),
        Rule(
            ExtractionType.PHONE,
            Regex("(?<![\\d/])(?:\\+|00)[1-9]\\d{0,2}[\\s.-]?(?:\\(\\d{1,4}\\)[\\s.-]?)?(?:\\d[\\s.-]?){6,12}\\d(?![\\d/])"),
            0.90f
        ),
        Rule(
            ExtractionType.PHONE,
            Regex("(?<![\\d/])(?:\\(\\d{2,4}\\)[\\s.-]?)?(?:\\d[\\s.-]?){7,11}\\d(?![\\d/])"),
            0.80f
        ),
        Rule(
            ExtractionType.DATE,
            Regex("\\b(?:19|20)\\d{2}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])\\b"),
            0.96f
        ),
        Rule(
            ExtractionType.DATE,
            Regex("(?i)\\b(?:0?[1-9]|[12]\\d|3[01])[/-](?:0?[1-9]|1[0-2])[/-](?:\\d{2}|\\d{4})\\b"),
            0.91f
        ),
        Rule(
            ExtractionType.DATE,
            Regex("(?i)\\b(?:0?[1-9]|[12]\\d|3[01])(?:st|nd|rd|th)?\\s+(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:,?\\s+\\d{2,4})?\\b"),
            0.93f
        ),
        Rule(
            ExtractionType.DATE,
            Regex("(?i)\\b(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\s+(?:0?[1-9]|[12]\\d|3[01])(?:st|nd|rd|th)?(?:,?\\s+\\d{2,4})?\\b"),
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
            rule.regex.findAll(text).mapNotNull { match ->
                val value = trimTrailingPunctuation(match.value)
                if (!isAllowed(rule.type, value)) return@mapNotNull null
                ExtractionResult(
                    type = rule.type,
                    value = value,
                    sourceStart = match.range.first,
                    sourceEndExclusive = match.range.first + value.length,
                    confidence = rule.confidence
                )
            }
        }

        return candidates
            .filter { it.value.isNotBlank() }
            .sortedWith(compareBy<ExtractionResult> { it.sourceStart }.thenByDescending { it.confidence })
            .fold(mutableListOf()) { accepted, candidate ->
                val duplicate = accepted.any {
                    it.type == candidate.type && normalizedValue(it) == normalizedValue(candidate)
                }
                val overlappingLowerConfidence = accepted.any {
                    rangesOverlap(it, candidate) && it.confidence >= candidate.confidence
                }
                if (!duplicate && !overlappingLowerConfidence) accepted += candidate
                accepted
            }
    }

    private fun isAllowed(type: ExtractionType, value: String): Boolean = when (type) {
        ExtractionType.PHONE -> {
            val digits = value.count(Char::isDigit)
            digits in 8..15 &&
                !value.matches(Regex("\\d{1,4}[-/]\\d{1,2}[-/]\\d{1,4}")) &&
                !value.matches(Regex("\\d{1,2}:\\d{2}"))
        }
        ExtractionType.PRICE -> {
            val digits = value.count(Char::isDigit)
            digits in 1..12 && value.any { it in "₹$€£¥" || it.isLetter() }
        }
        else -> true
    }

    private fun normalizedValue(result: ExtractionResult): String = when (result.type) {
        ExtractionType.PHONE -> result.value.filter(Char::isDigit)
        ExtractionType.PRICE -> result.value.uppercase().filterNot(Char::isWhitespace)
        else -> result.value.lowercase()
    }

    private fun rangesOverlap(a: ExtractionResult, b: ExtractionResult): Boolean =
        a.sourceStart < b.sourceEndExclusive && b.sourceStart < a.sourceEndExclusive

    private fun trimTrailingPunctuation(value: String): String =
        value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']')
}

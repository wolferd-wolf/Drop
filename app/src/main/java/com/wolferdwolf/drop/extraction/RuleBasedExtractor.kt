package com.wolferdwolf.drop.extraction

object RuleBasedExtractor {
    private data class Rule(
        val type: ExtractionType,
        val regex: Regex,
        val confidence: Float
    )

    private val rules = listOf(
        Rule(ExtractionType.EMAIL, Regex("(?i)\\b[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}\\b"), 0.98f),
        Rule(ExtractionType.URL, Regex("(?i)\\b(?:(?:https?://|www\\.)[^\\s<>()]+|(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+(?:com|org|net|in|io|app|co|dev|ai|me|tech|store|online|site|xyz|cloud|live|news|shop|info|biz|edu|gov|travel|design|agency|company|solutions|services|digital|studio|world|today|website|space|systems|software|network|media|pro|work|life|art|photography)(?:/[^\\s<>()]*)?)"), 0.96f),
        Rule(ExtractionType.PRICE, Regex("(?i)(?<![A-Z0-9])(?:₹|Rs\\.?|INR|\\$|USD|€|EUR|£|GBP|¥|JPY)\\s*\\d+(?:[.,]\\d+)?\\s*(?:k|thousand|lakh|lac|crore|million|billion)\\b"), 0.99f),
        Rule(ExtractionType.PRICE, Regex("(?i)(?<![A-Z0-9])(?:₹|Rs\\.?|INR|\\$|USD|€|EUR|£|GBP|¥|JPY)\\s*\\d{1,3}(?:(?:,\\d{2})*,\\d{3}|(?:,\\d{3})*|\\d*)(?:\\.\\d{1,2})?(?![\\d/])"), 0.97f),
        Rule(ExtractionType.PRICE, Regex("(?i)(?<![\\d/])\\d{1,3}(?:(?:,\\d{2})*,\\d{3}|(?:,\\d{3})*|\\d*)(?:\\.\\d{1,2})?\\s*(?:INR|USD|EUR|GBP|JPY)\\b"), 0.95f),
        Rule(ExtractionType.PRICE, Regex("(?i)\\b(?:rupees?|dollars?|euros?|pounds?|yen)\\s*\\d{1,3}(?:(?:,\\d{2})*,\\d{3}|(?:,\\d{3})*|\\d*)(?:\\.\\d{1,2})?(?![\\d/])"), 0.94f),
        Rule(ExtractionType.PRICE, Regex("(?i)(?<![\\d/])\\d{1,3}(?:(?:,\\d{2})*,\\d{3}|(?:,\\d{3})*|\\d*)(?:\\.\\d{1,2})?\\s*(?:rupees?|dollars?|euros?|pounds?|yen)\\b"), 0.94f),
        Rule(ExtractionType.PHONE, Regex("(?<![\\d/])(?:\\+91[-.\\s]?)?[6-9]\\d{4}[-.\\s]?\\d{5}(?![\\d/])"), 0.96f),
        Rule(ExtractionType.PHONE, Regex("(?<![\\d/])(?:\\+|00)[1-9]\\d{0,2}[\\s.-]?(?:\\(\\d{1,4}\\)[\\s.-]?)?(?:\\d[\\s.-]?){6,12}\\d(?![\\d/])"), 0.90f),
        Rule(ExtractionType.PHONE, Regex("(?<![\\d/])(?:\\(\\d{2,4}\\)[\\s.-]?)?(?:\\d[\\s.-]?){7,11}\\d(?![\\d/])"), 0.80f),
        Rule(ExtractionType.DATE, Regex("\\b(?:19|20)\\d{2}-(?:0[1-9]|1[0-2])-(?:0[1-9]|[12]\\d|3[01])\\b"), 0.96f),
        Rule(ExtractionType.DATE, Regex("(?<!\\d)(?:19|20)\\d{2}[/.](?:0?[1-9]|1[0-2])[/.](?:0?[1-9]|[12]\\d|3[01])(?!\\d)"), 0.95f),
        Rule(ExtractionType.DATE, Regex("(?i)\\b(?:0?[1-9]|[12]\\d|3[01])[/-](?:0?[1-9]|1[0-2])[/-](?:\\d{2}|\\d{4})\\b"), 0.91f),
        Rule(ExtractionType.DATE, Regex("(?i)\\b(?:0?[1-9]|1[0-2])[/-](?:0?[1-9]|[12]\\d|3[01])[/-](?:\\d{2}|\\d{4})\\b"), 0.89f),
        Rule(ExtractionType.DATE, Regex("(?<!\\d)(?:0?[1-9]|[12]\\d|3[01])\\.(?:0?[1-9]|1[0-2])\\.(?:\\d{2}|\\d{4})(?!\\d)"), 0.91f),
        Rule(ExtractionType.DATE, Regex("(?i)\\b(?:0?[1-9]|[12]\\d|3[01])(?:st|nd|rd|th)?[-\\s]+(?:of\\s+)?(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?|tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\.?(?:[-,\\s]+'?\\d{2,4})?\\b"), 0.93f),
        Rule(ExtractionType.DATE, Regex("(?i)\\b(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t(?:ember)?|tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\\.?[-\\s]+(?:0?[1-9]|[12]\\d|3[01])(?:st|nd|rd|th)?(?:[-,\\s]+'?\\d{2,4})?\\b"), 0.93f),
        Rule(ExtractionType.DATE, Regex("(?i)\\b(?:day after tomorrow|today|tonight|(?<!after )tomorrow)\\b"), 0.90f),
        Rule(ExtractionType.DATE, Regex("(?i)\\b(?:(?:this|next)\\s+)?(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday)\\b"), 0.88f),
        Rule(ExtractionType.TIME, Regex("(?i)\\b(?:1[0-2]|0?[1-9])(?::[0-5]\\d)?\\s?(?:a\\.?m\\.?|p\\.?m\\.?)\\b"), 0.95f),
        Rule(ExtractionType.TIME, Regex("(?i)(?<![\\d.])(?:1[0-2]|0?[1-9])\\.[0-5]\\d\\s?(?:a\\.?m\\.?|p\\.?m\\.?)\\b"), 0.94f),
        Rule(ExtractionType.TIME, Regex("(?i)(?<!\\d)(?:[01]?\\d|2[0-3])h[0-5]\\d(?!\\d)"), 0.92f),
        Rule(ExtractionType.TIME, Regex("(?<!\\d)(?:[01]?\\d|2[0-3]):[0-5]\\d(?!\\d)"), 0.92f),
        Rule(ExtractionType.TIME, Regex("(?i)\\b(?:noon|midnight)\\b"), 0.90f)
    )

    fun extract(text: String): List<ExtractionResult> {
        if (text.isBlank()) return emptyList()

        val candidates = rules.flatMap { rule ->
            rule.regex.findAll(text).mapNotNull { match ->
                val value = trimTrailingPunctuation(match.value)
                if (!isAllowed(rule.type, value)) return@mapNotNull null
                ExtractionResult(rule.type, value, match.range.first, match.range.first + value.length, rule.confidence)
            }
        }.toMutableList()

        AddressCandidateDetector.detect(text)?.let { address ->
            val start = text.indexOf(address.value, ignoreCase = true)
            if (start >= 0) {
                candidates += ExtractionResult(
                    type = ExtractionType.ADDRESS,
                    value = address.value,
                    sourceStart = start,
                    sourceEndExclusive = start + address.value.length,
                    confidence = address.confidence
                )
            }
        }

        return candidates
            .filter { it.value.isNotBlank() }
            .sortedWith(compareBy<ExtractionResult> { it.sourceStart }.thenByDescending { it.confidence })
            .fold(mutableListOf()) { accepted, candidate ->
                val duplicate = accepted.any { it.type == candidate.type && normalizedValue(it) == normalizedValue(candidate) }
                val overlappingLowerConfidence = accepted.any { rangesOverlap(it, candidate) && it.confidence >= candidate.confidence }
                if (!duplicate && !overlappingLowerConfidence) accepted += candidate
                accepted
            }
    }

    private fun isAllowed(type: ExtractionType, value: String): Boolean = when (type) {
        ExtractionType.PHONE -> {
            val digits = value.count(Char::isDigit)
            digits in 8..15 && !value.matches(Regex("\\d{1,4}[-/]\\d{1,2}[-/]\\d{1,4}")) && !value.matches(Regex("\\d{1,2}:\\d{2}"))
        }
        ExtractionType.PRICE -> {
            val digits = value.count(Char::isDigit)
            digits in 1..12 && value.any { it in "₹$€£¥" || it.isLetter() }
        }
        ExtractionType.DATE -> isValidNumericDate(value)
        else -> true
    }

    private fun isValidNumericDate(value: String): Boolean {
        val yearFirst = YEAR_FIRST_NUMERIC_DATE.matchEntire(value)
        if (yearFirst != null) {
            val (year, month, day) = yearFirst.destructured
            return isValidCalendarDate(year.toInt(), month.toInt(), day.toInt())
        }

        val dayFirst = DAY_FIRST_NUMERIC_DATE.matchEntire(value)
        if (dayFirst != null) {
            val (first, second, yearText) = dayFirst.destructured
            val year = if (yearText.length == 2) 2000 + yearText.toInt() else yearText.toInt()
            val firstNumber = first.toInt()
            val secondNumber = second.toInt()
            val dayFirstValid = isValidCalendarDate(year, secondNumber, firstNumber)
            val monthFirstValid = isValidCalendarDate(year, firstNumber, secondNumber)
            return dayFirstValid || monthFirstValid
        }

        return true
    }

    private fun isValidCalendarDate(year: Int, month: Int, day: Int): Boolean {
        if (month !in 1..12 || day < 1) return false
        val maxDay = when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return day <= maxDay
    }

    private fun isLeapYear(year: Int): Boolean =
        year % 400 == 0 || (year % 4 == 0 && year % 100 != 0)

    private fun normalizedValue(result: ExtractionResult): String = when (result.type) {
        ExtractionType.PHONE -> result.value.filter(Char::isDigit)
        ExtractionType.PRICE -> result.value.uppercase().filterNot(Char::isWhitespace)
        else -> result.value.lowercase()
    }

    private fun rangesOverlap(a: ExtractionResult, b: ExtractionResult): Boolean =
        a.sourceStart < b.sourceEndExclusive && b.sourceStart < a.sourceEndExclusive

    private fun trimTrailingPunctuation(value: String): String = value.trimEnd('.', ',', ';', ':', '!', '?', ')', ']')

    private val YEAR_FIRST_NUMERIC_DATE = Regex("((?:19|20)\\d{2})[-/.](\\d{1,2})[-/.](\\d{1,2})")
    private val DAY_FIRST_NUMERIC_DATE = Regex("(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2}|\\d{4})")
}

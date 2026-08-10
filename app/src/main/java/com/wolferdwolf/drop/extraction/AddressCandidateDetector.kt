package com.wolferdwolf.drop.extraction

data class AddressCandidate(
    val value: String,
    val confidence: Float
)

object AddressCandidateDetector {
    private val strongMarkers = listOf(
        "address", "venue", "road", "street", "lane", "avenue", "highway", "building",
        "apartment", "flat", "floor", "nagar", "colony", "district", "mandal", "village",
        "near ", "opposite", "beside", "behind", "junction", "circle", "complex", "mall",
        "hospital", "school", "college", "office", "station", "airport", "temple", "church",
        "mosque", "hotel", "restaurant", "theatre", "theater", "stadium", "hall", "centre", "center"
    )
    private val pinCode = Regex("(?<!\\d)[1-9]\\d{5}(?!\\d)")
    private val numberedPremise = Regex("(?i)\\b(?:no\\.?|house|plot|door|flat|shop|room)\\s*#?\\s*[a-z0-9/-]{1,12}\\b")
    private val locationLabel = Regex("(?i)\\b(?:venue|location|address)\\s*:\\s*(.*)$")
    private val inlineLocationLabel = Regex(
        "(?i)\\b(?:venue|location|address)\\s*:\\s*(.+?)(?=(?:[.!?](?:\\s|$))|(?:\\s+(?:date|time|phone|email|website|price|fee|deadline|notes?|contact|organizer|organiser)\\s*:)|$)"
    )
    private val nextFieldLabel = Regex(
        "(?i)^(?:date|time|phone|email|website|price|fee|deadline|notes?|contact|organizer|organiser)\\s*:"
    )
    private val venueAfterTime = Regex(
        "(?i)\\b(?:(?:[01]?\\d|2[0-3]):[0-5]\\d\\s*(?:a\\.?m\\.?|p\\.?m\\.?)?|(?:0?[1-9]|1[0-2])\\s*(?:a\\.?m\\.?|p\\.?m\\.?))\\s*,\\s*(.+?)(?=[.!?](?:\\s|$)|$)"
    )

    fun detect(text: String): AddressCandidate? {
        detectInlineLabelledValue(text)?.let { return it }
        detectVenueAfterTime(text)?.let { return it }

        val lines = text.lineSequence().map(String::trim).toList()
        val nonBlankLines = lines.filter(String::isNotBlank)
        if (nonBlankLines.isEmpty()) return null

        detectLabelledBlock(lines)?.let { return it }

        val ranked = nonBlankLines.mapNotNull { line ->
            val lower = line.lowercase()
            var score = 0f
            if (strongMarkers.any(lower::contains)) score += 0.55f
            if (pinCode.containsMatchIn(line)) score += 0.25f
            if (numberedPremise.containsMatchIn(line)) score += 0.25f
            if (line.count { it == ',' } >= 1) score += 0.10f
            if (line.length in 8..180) score += 0.05f
            if (score >= 0.55f) AddressCandidate(line, score.coerceAtMost(0.95f)) else null
        }

        return ranked.maxByOrNull(AddressCandidate::confidence)
    }

    private fun detectInlineLabelledValue(text: String): AddressCandidate? {
        val match = inlineLocationLabel.find(text) ?: return null
        val value = clean(match.groupValues[1])
        if (value.isBlank()) return null
        return AddressCandidate(value, if (pinCode.containsMatchIn(value)) 0.99f else 0.95f)
    }

    private fun detectVenueAfterTime(text: String): AddressCandidate? {
        val match = venueAfterTime.find(text) ?: return null
        val value = clean(match.groupValues[1])
        if (!looksLikeAddressLine(value)) return null
        val confidence = when {
            pinCode.containsMatchIn(value) -> 0.94f
            strongMarkers.any(value.lowercase()::contains) -> 0.90f
            else -> 0.78f
        }
        return AddressCandidate(value, confidence)
    }

    private fun detectLabelledBlock(lines: List<String>): AddressCandidate? {
        lines.forEachIndexed { index, line ->
            val match = locationLabel.find(line) ?: return@forEachIndexed
            val collected = mutableListOf<String>()
            match.groupValues[1].trim().takeIf(String::isNotBlank)?.let(collected::add)

            var cursor = index + 1
            while (cursor < lines.size && collected.size < MAX_LABELLED_LINES) {
                val next = lines[cursor].trim()
                if (next.isBlank() || nextFieldLabel.containsMatchIn(next)) break
                if (!looksLikeAddressLine(next)) break
                collected += next
                cursor += 1
            }

            val value = collected.joinToString(", ").trim().take(MAX_ADDRESS_LENGTH)
            if (value.isNotBlank()) {
                val confidence = when {
                    pinCode.containsMatchIn(value) -> 0.99f
                    collected.size >= 2 -> 0.97f
                    else -> 0.95f
                }
                return AddressCandidate(value, confidence)
            }
        }
        return null
    }

    private fun looksLikeAddressLine(line: String): Boolean {
        val lower = line.lowercase()
        return strongMarkers.any(lower::contains) ||
            pinCode.containsMatchIn(line) ||
            numberedPremise.containsMatchIn(line) ||
            line.count { it == ',' } >= 1
    }

    private fun clean(value: String): String = value
        .trim()
        .trimEnd('.', ',', ';', ':', '!', '?')
        .take(MAX_ADDRESS_LENGTH)

    private const val MAX_LABELLED_LINES = 4
    private const val MAX_ADDRESS_LENGTH = 300
}

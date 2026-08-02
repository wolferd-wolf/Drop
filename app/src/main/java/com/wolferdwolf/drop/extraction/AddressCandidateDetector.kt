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
        "mosque", "hotel", "restaurant", "theatre", "theater", "stadium", "hall"
    )
    private val pinCode = Regex("(?<!\\d)[1-9]\\d{5}(?!\\d)")
    private val numberedPremise = Regex("(?i)\\b(?:no\\.?|house|plot|door|flat|shop|room)\\s*#?\\s*[a-z0-9/-]{1,12}\\b")
    private val labelledLocation = Regex(
        "(?i)\\b(?:venue|location|address)\\s*:\\s*([^\\n.!?]+)"
    )

    fun detect(text: String): AddressCandidate? {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        if (lines.isEmpty()) return null

        val labelled = labelledLocation.find(text)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.takeIf(String::isNotBlank)
        if (labelled != null) {
            return AddressCandidate(labelled.take(300), 0.95f)
        }

        val ranked = lines.mapNotNull { line ->
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
}

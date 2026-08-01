package com.wolferdwolf.drop.extraction

enum class ExtractionType {
    PHONE,
    EMAIL,
    URL,
    DATE,
    TIME
}

data class ExtractionResult(
    val type: ExtractionType,
    val value: String,
    val sourceStart: Int,
    val sourceEndExclusive: Int,
    val confidence: Float
) {
    init {
        require(sourceStart >= 0)
        require(sourceEndExclusive > sourceStart)
        require(confidence in 0f..1f)
    }
}

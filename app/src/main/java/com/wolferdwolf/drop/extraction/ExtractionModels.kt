package com.wolferdwolf.drop.extraction

enum class ExtractionType {
    PHONE,
    EMAIL,
    URL,
    DATE,
    TIME,
    PRICE;

    companion object {
        /**
         * Temporary compatibility alias for calendar venue routing.
         * Address extraction is not yet represented as a first-class editable result,
         * so callers must not treat this alias as proof that an address was detected.
         */
        val ADDRESS: ExtractionType = PRICE
    }
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

package com.wolferdwolf.drop.extraction

object EditableExtractionResults {
    fun update(
        results: List<ExtractionResult>,
        target: ExtractionResult,
        newValue: String
    ): List<ExtractionResult> {
        val clean = newValue.trim().take(MAX_EDITED_VALUE_LENGTH)
        if (clean.isBlank()) return remove(results, target)
        return results.map { result ->
            if (result == target) {
                result.copy(
                    value = clean,
                    sourceEndExclusive = result.sourceStart + clean.length,
                    confidence = 1f
                )
            } else {
                result
            }
        }
    }

    fun remove(results: List<ExtractionResult>, target: ExtractionResult): List<ExtractionResult> =
        results.filterNot { it == target }

    const val MAX_EDITED_VALUE_LENGTH = 300
}

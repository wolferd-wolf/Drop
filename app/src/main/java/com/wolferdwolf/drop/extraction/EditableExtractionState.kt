package com.wolferdwolf.drop.extraction

import java.nio.charset.StandardCharsets
import java.util.Base64

object EditableExtractionState {
    fun encode(results: List<ExtractionResult>?): ArrayList<String> = ArrayList(
        results.orEmpty().map { result ->
            listOf(
                result.type.name,
                result.sourceStart.toString(),
                result.sourceEndExclusive.toString(),
                result.confidence.toString(),
                Base64.getUrlEncoder().withoutPadding().encodeToString(
                    result.value.toByteArray(StandardCharsets.UTF_8)
                )
            ).joinToString(SEPARATOR)
        }
    )

    fun decode(hasEditedResults: Boolean, encoded: List<String>?): List<ExtractionResult>? {
        if (!hasEditedResults) return null
        if (encoded.isNullOrEmpty()) return emptyList()

        return runCatching {
            encoded.map { item ->
                val fields = item.split(SEPARATOR, limit = FIELD_COUNT)
                require(fields.size == FIELD_COUNT)
                ExtractionResult(
                    type = ExtractionType.valueOf(fields[0]),
                    sourceStart = fields[1].toInt(),
                    sourceEndExclusive = fields[2].toInt(),
                    confidence = fields[3].toFloat(),
                    value = String(
                        Base64.getUrlDecoder().decode(fields[4]),
                        StandardCharsets.UTF_8
                    )
                )
            }
        }.getOrNull()
    }

    private const val SEPARATOR = "|"
    private const val FIELD_COUNT = 5
}

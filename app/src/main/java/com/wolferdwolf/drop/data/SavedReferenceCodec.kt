package com.wolferdwolf.drop.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SavedReferenceCodec {
    fun encode(reference: SavedReference): String = listOf(
        reference.id.toString(),
        reference.createdAtEpochMillis.toString(),
        encodePart(reference.title),
        encodePart(reference.originalText),
        encodePart(reference.notes)
    ).joinToString("|")

    fun decode(value: String): SavedReference? {
        val parts = value.split('|', limit = 5)
        if (parts.size !in 4..5) return null
        val id = parts[0].toLongOrNull() ?: return null
        val createdAt = parts[1].toLongOrNull() ?: return null
        return SavedReference(
            id = id,
            title = decodePart(parts[2]),
            originalText = decodePart(parts[3]),
            createdAtEpochMillis = createdAt,
            notes = parts.getOrNull(4)?.let(::decodePart).orEmpty()
        )
    }

    private fun encodePart(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun decodePart(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}

package com.wolferdwolf.drop.data

import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object SavedReferenceCodec {
    fun encode(reference: SavedReference): String = listOf(
        reference.id.toString(),
        reference.createdAtEpochMillis.toString(),
        encodePart(reference.title),
        encodePart(reference.originalText)
    ).joinToString("|")

    fun decode(value: String): SavedReference? {
        val parts = value.split('|', limit = 4)
        if (parts.size != 4) return null
        val id = parts[0].toLongOrNull() ?: return null
        val createdAt = parts[1].toLongOrNull() ?: return null
        return SavedReference(
            id = id,
            title = decodePart(parts[2]),
            originalText = decodePart(parts[3]),
            createdAtEpochMillis = createdAt
        )
    }

    private fun encodePart(value: String): String =
        URLEncoder.encode(value, StandardCharsets.UTF_8.name())

    private fun decodePart(value: String): String =
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}

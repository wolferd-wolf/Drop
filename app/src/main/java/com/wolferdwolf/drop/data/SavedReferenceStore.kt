package com.wolferdwolf.drop.data

import android.content.Context

class SavedReferenceStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): List<SavedReference> = preferences
        .getStringSet(KEY_REFERENCES, emptySet())
        .orEmpty()
        .mapNotNull(SavedReferenceCodec::decode)
        .sortedByDescending(SavedReference::createdAtEpochMillis)

    fun save(
        title: String,
        originalText: String,
        now: Long = System.currentTimeMillis(),
        sourceType: SavedSourceType = SavedSourceType.UNKNOWN
    ): SavedReference {
        val reference = SavedReference(
            id = now,
            title = title.trim().ifBlank { defaultTitle(originalText) },
            originalText = originalText.trim(),
            createdAtEpochMillis = now,
            sourceType = sourceType
        )
        persistReplacing(reference)
        return reference
    }

    fun update(reference: SavedReference, title: String, notes: String): SavedReference {
        val updated = reference.copy(
            title = title.trim().ifBlank { defaultTitle(reference.originalText) },
            notes = notes.trim()
        )
        persistReplacing(updated)
        return updated
    }

    fun delete(id: Long) {
        val updated = preferences.getStringSet(KEY_REFERENCES, emptySet())
            .orEmpty()
            .filterNot { SavedReferenceCodec.decode(it)?.id == id }
            .toSet()
        preferences.edit().putStringSet(KEY_REFERENCES, updated).apply()
    }

    private fun persistReplacing(reference: SavedReference) {
        val updated = preferences.getStringSet(KEY_REFERENCES, emptySet())
            .orEmpty()
            .filterNot { SavedReferenceCodec.decode(it)?.id == reference.id }
            .toMutableSet()
        updated += SavedReferenceCodec.encode(reference)
        check(preferences.edit().putStringSet(KEY_REFERENCES, updated).commit()) {
            "Unable to persist saved reference"
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 80
        private const val PREFERENCES_NAME = "drop_saved_references"
        private const val KEY_REFERENCES = "references"

        fun defaultTitle(text: String): String = text
            .lineSequence()
            .map(String::trim)
            .firstOrNull(String::isNotBlank)
            ?.take(MAX_TITLE_LENGTH)
            ?: "Saved reference"
    }
}

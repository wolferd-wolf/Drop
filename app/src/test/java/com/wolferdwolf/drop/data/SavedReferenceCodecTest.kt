package com.wolferdwolf.drop.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedReferenceCodecTest {
    @Test
    fun notesRoundTripAndLegacyRecordsRemainReadable() {
        val reference = SavedReference(42L, "Wolf plan", "Original text", 1234L, "Follow up Friday")
        assertEquals(reference, SavedReferenceCodec.decode(SavedReferenceCodec.encode(reference)))

        val legacy = "42|1234|Wolf+plan|Original+text"
        assertEquals(
            SavedReference(42L, "Wolf plan", "Original text", 1234L, ""),
            SavedReferenceCodec.decode(legacy)
        )
    }
}

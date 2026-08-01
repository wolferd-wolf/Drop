package com.wolferdwolf.drop.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedReferenceCodecTest {
    @Test
    fun roundTrip_preservesMultilineAndSymbols() {
        val original = SavedReference(
            id = 42L,
            title = "Call R&D | Monday",
            originalText = "Phone +91 98765 43210\nEmail a+b@example.com",
            createdAtEpochMillis = 123456L
        )

        assertEquals(original, SavedReferenceCodec.decode(SavedReferenceCodec.encode(original)))
    }

    @Test
    fun malformedRecord_isRejected() {
        assertNull(SavedReferenceCodec.decode("not-a-record"))
    }

    @Test
    fun defaultTitle_usesFirstNonBlankLineAndLimitsLength() {
        val text = "\n   ${"a".repeat(100)}\nsecond"
        assertEquals("a".repeat(80), SavedReferenceStore.defaultTitle(text))
    }
}

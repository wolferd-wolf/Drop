package com.wolferdwolf.drop.share

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SharedTextParserTest {
    @Test
    fun acceptsAndTrimsPlainSharedText() {
        val result = SharedTextParser.parse(Intent.ACTION_SEND, "text/plain", "  Meeting tomorrow at 10 AM  ")
        assertEquals("Meeting tomorrow at 10 AM", result)
    }

    @Test
    fun rejectsBlankUnsupportedOrWrongAction() {
        assertNull(SharedTextParser.parse(Intent.ACTION_SEND, "text/plain", "   "))
        assertNull(SharedTextParser.parse(Intent.ACTION_SEND, "image/png", "hello"))
        assertNull(SharedTextParser.parse(Intent.ACTION_VIEW, "text/plain", "hello"))
    }

    @Test
    fun boundsVeryLargeShares() {
        val oversized = "a".repeat(SharedTextParser.MAX_SHARED_TEXT_LENGTH + 500)
        val result = SharedTextParser.parse(Intent.ACTION_SEND, "text/plain", oversized)
        assertEquals(SharedTextParser.MAX_SHARED_TEXT_LENGTH, result?.length)
    }
}

package com.wolferdwolf.drop.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductPromiseTest {
    @Test
    fun blankSharedTextIsRejected() {
        assertFalse(ProductPromise.isValidSharedText("   "))
    }

    @Test
    fun meaningfulSharedTextIsAccepted() {
        assertTrue(ProductPromise.isValidSharedText("Meeting tomorrow at 10:00"))
    }
}

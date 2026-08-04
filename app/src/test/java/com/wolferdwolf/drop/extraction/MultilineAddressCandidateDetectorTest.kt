package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test

class MultilineAddressCandidateDetectorTest {
    @Test
    fun labelledMultilineAddressIsPreservedAsOneCandidate() {
        val candidate = AddressCandidateDetector.detect(
            """
            Product launch
            Venue:
            Wolf Convention Centre
            MG Road, Vijayawada
            Andhra Pradesh 520010
            Date: 18 August 2026
            """.trimIndent()
        )

        assertNotNull(candidate)
        assertEquals(
            "Wolf Convention Centre, MG Road, Vijayawada, Andhra Pradesh 520010",
            candidate?.value
        )
        assertEquals(0.99f, candidate?.confidence ?: 0f, 0.001f)
    }

    @Test
    fun labelledAddressStopsBeforeNextStructuredField() {
        val candidate = AddressCandidateDetector.detect(
            """
            Address: Plot 12
            Tech Park Road, Hyderabad 500081
            Phone: +91 98765 43210
            Email: hello@example.com
            """.trimIndent()
        )

        assertEquals("Plot 12, Tech Park Road, Hyderabad 500081", candidate?.value)
        assertFalse(candidate?.value.orEmpty().contains("Phone"))
        assertFalse(candidate?.value.orEmpty().contains("Email"))
    }

    @Test
    fun unlabelledOrdinaryParagraphIsNotPromotedToAddress() {
        val candidate = AddressCandidateDetector.detect(
            "The quarterly report explains product growth and customer retention across several markets."
        )

        assertEquals(null, candidate)
    }
}

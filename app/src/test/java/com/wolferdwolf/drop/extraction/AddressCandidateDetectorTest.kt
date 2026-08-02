package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressCandidateDetectorTest {
    @Test
    fun detectsIndianPostalAddressWithPinCode() {
        val candidate = AddressCandidateDetector.detect(
            "Interview venue\nPlot 14, MG Road, Vijayawada, Andhra Pradesh 520010"
        )

        requireNotNull(candidate)
        assertEquals("Plot 14, MG Road, Vijayawada, Andhra Pradesh 520010", candidate.value)
        assertTrue(candidate.confidence >= 0.8f)
    }

    @Test
    fun detectsVenueWithoutFormalStreetAddress() {
        val candidate = AddressCandidateDetector.detect(
            "Annual meeting at Sri Balaji Convention Hall, near RTC Bus Station"
        )

        requireNotNull(candidate)
        assertTrue(candidate.value.contains("Convention Hall"))
    }

    @Test
    fun ignoresOrdinaryNotesAndNumericIdentifiers() {
        assertNull(AddressCandidateDetector.detect("Invoice 12345\nCall tomorrow at 5 PM"))
        assertNull(AddressCandidateDetector.detect("PIN 515401 is required for verification"))
    }
}

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
    fun detectsInlineLabelledVenueInsideSentence() {
        val candidate = AddressCandidateDetector.detect(
            "Product launch meeting on August 21st, 2026 at 4:00 PM. Venue: MG Road, Vijayawada."
        )

        requireNotNull(candidate)
        assertEquals("MG Road, Vijayawada", candidate.value)
        assertTrue(candidate.confidence >= 0.9f)
    }

    @Test
    fun stopsInlineLabelledAddressAtNextField() {
        val candidate = AddressCandidateDetector.detect(
            "Address: Plot 14, MG Road, Vijayawada Phone: 9876543210"
        )

        requireNotNull(candidate)
        assertEquals("Plot 14, MG Road, Vijayawada", candidate.value)
    }

    @Test
    fun boundsUnlabelledVenueAfterEventTime() {
        val candidate = AddressCandidateDetector.detect(
            "Community workshop on 12-Aug-2026 at 6:30 PM, Town Hall Road, Gooty."
        )

        requireNotNull(candidate)
        assertEquals("Town Hall Road, Gooty", candidate.value)
        assertTrue(candidate.confidence >= 0.9f)
    }

    @Test
    fun doesNotTreatOrdinaryTextAfterTimeAsVenue() {
        assertNull(AddressCandidateDetector.detect("Call me at 6:30 PM, we should discuss the report."))
    }

    @Test
    fun ignoresOrdinaryNotesAndNumericIdentifiers() {
        assertNull(AddressCandidateDetector.detect("Invoice 12345\nCall tomorrow at 5 PM"))
        assertNull(AddressCandidateDetector.detect("PIN 515401 is required for verification"))
    }
}

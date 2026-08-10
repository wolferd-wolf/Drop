package com.wolferdwolf.drop.maps

import org.junit.Assert.assertEquals
import org.junit.Test

class MapConfirmationActivityTest {
    @Test
    fun curatedAddressOverridesOriginalSourceExtraction() {
        assertEquals(
            "Edited venue, Vijayawada",
            MapConfirmationActivity.initialQuery(
                source = "Meeting at MG Road, Vijayawada",
                curatedQuery = "  Edited venue, Vijayawada  "
            )
        )
    }

    @Test
    fun blankCuratedAddressFallsBackToDetectedSourceAddress() {
        assertEquals(
            "MG Road, Vijayawada",
            MapConfirmationActivity.initialQuery(
                source = "Venue: MG Road, Vijayawada",
                curatedQuery = "   "
            )
        )
    }
}

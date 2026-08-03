package com.wolferdwolf.drop.maps

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapConfirmationActivityTest {
    @Test
    fun historyTitleUsesEditedLocation() {
        assertEquals(
            "Maps: 12 MG Road, Bengaluru",
            MapConfirmationActivity.historyTitle("  12 MG Road, Bengaluru  ")
        )
    }

    @Test
    fun historyTitleIsBounded() {
        val title = MapConfirmationActivity.historyTitle("A".repeat(200))

        assertEquals(86, title.length)
        assertTrue(title.startsWith("Maps: "))
    }

    @Test
    fun historyContentPreservesEditedLocationAndLaunchStatus() {
        val content = MapConfirmationActivity.historyContent("  Pavan Empower Solutions, Vijayawada  ")

        assertTrue(content.startsWith("Status: Opened in Maps app"))
        assertTrue(content.contains("Location: Pavan Empower Solutions, Vijayawada"))
    }

    @Test
    fun historyContentIsBounded() {
        assertEquals(
            MapConfirmationActivity.MAX_QUERY_LENGTH,
            MapConfirmationActivity.historyContent("B".repeat(700)).length
        )
    }
}

package com.wolferdwolf.drop.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedSourceTypeTest {
    @Test
    fun storedSourceType_roundTripsKnownValuesAndProtectsLegacyData() {
        SavedSourceType.values().forEach { sourceType ->
            assertEquals(sourceType, SavedSourceType.fromStored(sourceType.name))
        }
        assertEquals(SavedSourceType.UNKNOWN, SavedSourceType.fromStored(null))
        assertEquals(SavedSourceType.UNKNOWN, SavedSourceType.fromStored("LEGACY"))
    }
}

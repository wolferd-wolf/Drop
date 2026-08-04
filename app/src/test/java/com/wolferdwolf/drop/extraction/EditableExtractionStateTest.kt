package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EditableExtractionStateTest {
    @Test
    fun roundTripPreservesEditedValuesAndMetadata() {
        val results = listOf(
            ExtractionResult(
                type = ExtractionType.PRICE,
                value = "₹3 lakh | approved",
                sourceStart = 10,
                sourceEndExclusive = 28,
                confidence = 1f
            ),
            ExtractionResult(
                type = ExtractionType.EMAIL,
                value = "owner@example.com",
                sourceStart = 35,
                sourceEndExclusive = 52,
                confidence = 0.98f
            )
        )

        assertEquals(results, EditableExtractionState.decode(true, EditableExtractionState.encode(results)))
    }

    @Test
    fun emptyEditedListRemainsDistinctFromNeverEdited() {
        assertEquals(emptyList<ExtractionResult>(), EditableExtractionState.decode(true, arrayListOf()))
        assertNull(EditableExtractionState.decode(false, arrayListOf()))
    }

    @Test
    fun malformedStateFallsBackSafely() {
        assertNull(EditableExtractionState.decode(true, listOf("broken")))
    }
}

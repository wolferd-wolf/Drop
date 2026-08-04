package com.wolferdwolf.drop.extraction

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditableExtractionResultsTest {
    private val price = ExtractionResult(
        type = ExtractionType.PRICE,
        value = "₹2.5 lakh",
        sourceStart = 10,
        sourceEndExclusive = 19,
        confidence = 0.99f
    )

    @Test
    fun editedValueReplacesOnlyTargetAndBecomesUserConfirmed() {
        val phone = ExtractionResult(
            type = ExtractionType.PHONE,
            value = "+91 98765 43210",
            sourceStart = 25,
            sourceEndExclusive = 40,
            confidence = 0.96f
        )

        val updated = EditableExtractionResults.update(listOf(price, phone), price, " ₹3 lakh ")

        assertEquals("₹3 lakh", updated.first().value)
        assertEquals(1f, updated.first().confidence)
        assertEquals(phone, updated.last())
    }

    @Test
    fun blankEditRemovesTarget() {
        val updated = EditableExtractionResults.update(listOf(price), price, "   ")

        assertTrue(updated.isEmpty())
    }

    @Test
    fun removeDeletesOnlySelectedResult() {
        val email = ExtractionResult(
            type = ExtractionType.EMAIL,
            value = "hello@example.com",
            sourceStart = 30,
            sourceEndExclusive = 47,
            confidence = 0.98f
        )

        assertEquals(listOf(email), EditableExtractionResults.remove(listOf(price, email), price))
    }

    @Test
    fun editedValueIsBounded() {
        val updated = EditableExtractionResults.update(
            listOf(price),
            price,
            "x".repeat(EditableExtractionResults.MAX_EDITED_VALUE_LENGTH + 50)
        )

        assertEquals(EditableExtractionResults.MAX_EDITED_VALUE_LENGTH, updated.single().value.length)
    }
}

package com.wolferdwolf.drop.contact

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactConfirmationActivityTest {
    @Test
    fun extractsExplicitNameAndCompanyLabels() {
        val source = "Name: Priya Reddy\nCompany: Wolf Labs\nPhone: +91 98765 43210"
        assertEquals("Priya Reddy", ContactConfirmationActivity.labelledValue(source, ContactConfirmationActivity.NAME_LABELS))
        assertEquals("Wolf Labs", ContactConfirmationActivity.labelledValue(source, ContactConfirmationActivity.COMPANY_LABELS))
    }

    @Test
    fun doesNotGuessUnlabelledNames() {
        assertEquals("", ContactConfirmationActivity.labelledValue("Call Priya tomorrow", ContactConfirmationActivity.NAME_LABELS))
    }
}

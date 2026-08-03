package com.wolferdwolf.drop.contact

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun historyTitleUsesEditedNameFirst() {
        assertEquals(
            "Contact: Priya Reddy",
            ContactConfirmationActivity.historyTitle("  Priya Reddy  ", "+91 98765 43210", "priya@example.com")
        )
    }

    @Test
    fun historyTitleFallsBackToPhoneThenEmail() {
        assertEquals(
            "Contact: +91 98765 43210",
            ContactConfirmationActivity.historyTitle("", " +91 98765 43210 ", "priya@example.com")
        )
        assertEquals(
            "Contact: priya@example.com",
            ContactConfirmationActivity.historyTitle("", "", " priya@example.com ")
        )
    }

    @Test
    fun historyContentPreservesEditedFieldsAndLaunchStatus() {
        val content = ContactConfirmationActivity.historyContent(
            name = "Priya Reddy",
            phone = "+91 98765 43210",
            email = "priya@example.com",
            company = "Wolf Labs",
            notes = "Met at the supplier review."
        )

        assertTrue(content.startsWith("Status: Opened in Contacts app"))
        assertTrue(content.contains("Name: Priya Reddy"))
        assertTrue(content.contains("Phone: +91 98765 43210"))
        assertTrue(content.contains("Email: priya@example.com"))
        assertTrue(content.contains("Company: Wolf Labs"))
        assertTrue(content.contains("Met at the supplier review."))
    }

    @Test
    fun historyContentOmitsBlankOptionalFields() {
        val content = ContactConfirmationActivity.historyContent(
            name = "Priya Reddy",
            phone = "",
            email = "",
            company = "",
            notes = ""
        )

        assertTrue(!content.contains("Phone:"))
        assertTrue(!content.contains("Email:"))
        assertTrue(!content.contains("Company:"))
    }
}

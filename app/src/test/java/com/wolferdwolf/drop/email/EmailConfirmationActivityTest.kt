package com.wolferdwolf.drop.email

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EmailConfirmationActivityTest {
    @Test
    fun requiresValidRecipient() {
        assertEquals("Enter an email address.", EmailConfirmationActivity.validateRecipient(" "))
        assertEquals("Enter a valid email address.", EmailConfirmationActivity.validateRecipient("not-an-email"))
        assertNull(EmailConfirmationActivity.validateRecipient("person@example.com"))
    }

    @Test
    fun usesExplicitSubjectLabelWhenAvailable() {
        val source = "Subject: Project update\nEmail: person@example.com\nPlease review the attached notes."
        assertEquals("Project update", EmailConfirmationActivity.subjectFrom(source))
    }

    @Test
    fun fallsBackToSourceTitle() {
        val source = "Quarterly planning notes\nperson@example.com"
        assertEquals("Quarterly planning notes", EmailConfirmationActivity.subjectFrom(source))
    }
}

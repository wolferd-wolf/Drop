package com.wolferdwolf.drop.email

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun buildsClearHistoryTitleFromEditedSubject() {
        assertEquals(
            "Email: Project update",
            EmailConfirmationActivity.historyTitle("person@example.com", " Project update ")
        )
        assertEquals(
            "Email to person@example.com",
            EmailConfirmationActivity.historyTitle(" person@example.com ", " ")
        )
    }

    @Test
    fun historyContentRecordsExternalActionStatusAndEditedFields() {
        val content = EmailConfirmationActivity.historyContent(
            " person@example.com ",
            " Project update ",
            " Please review. "
        )

        assertTrue(content.startsWith("Status: Opened in email app"))
        assertTrue(content.contains("To: person@example.com"))
        assertTrue(content.contains("Subject: Project update"))
        assertTrue(content.endsWith("Please review."))
    }
}

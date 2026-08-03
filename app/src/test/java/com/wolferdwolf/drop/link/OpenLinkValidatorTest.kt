package com.wolferdwolf.drop.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenLinkValidatorTest {
    @Test
    fun addsHttpsToBareWebHost() {
        assertEquals("https://example.com/jobs", OpenLinkValidator.normalize("example.com/jobs"))
    }

    @Test
    fun preservesHttpAndHttpsLinks() {
        assertEquals("http://example.com", OpenLinkValidator.normalize("http://example.com"))
        assertEquals("https://example.com/path", OpenLinkValidator.normalize("https://example.com/path"))
    }

    @Test
    fun rejectsNonWebSchemesWhitespaceAndInvalidHosts() {
        assertNull(OpenLinkValidator.normalize("javascript:alert(1)"))
        assertNull(OpenLinkValidator.normalize("mailto:test@example.com"))
        assertNull(OpenLinkValidator.normalize("https://example.com/a b"))
        assertNull(OpenLinkValidator.normalize("localhost"))
        assertNull(OpenLinkValidator.normalize(""))
    }

    @Test
    fun createsExplicitBrowserHistoryMetadata() {
        assertEquals("Opened link: example.com", OpenLinkConfirmationActivity.historyTitle("https://www.example.com/jobs"))
        assertEquals(
            "Status: Opened in browser\nURL: https://example.com/jobs",
            OpenLinkConfirmationActivity.historyContent("https://example.com/jobs")
        )
    }
}
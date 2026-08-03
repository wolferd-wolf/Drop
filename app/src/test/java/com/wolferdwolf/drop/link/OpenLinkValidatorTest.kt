package com.wolferdwolf.drop.link

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OpenLinkValidatorTest {
    @Test
    fun addsHttpsToAValidBareDomain() {
        assertEquals("https://example.com/jobs", OpenLinkValidator.normalize("example.com/jobs"))
    }

    @Test
    fun preservesExplicitHttpAndHttpsLinks() {
        assertEquals("https://example.com", OpenLinkValidator.normalize("https://example.com"))
        assertEquals("http://example.com", OpenLinkValidator.normalize("http://example.com"))
    }

    @Test
    fun rejectsUnsafeOrMalformedLinks() {
        assertNull(OpenLinkValidator.normalize("javascript:alert(1)"))
        assertNull(OpenLinkValidator.normalize("mailto:user@example.com"))
        assertNull(OpenLinkValidator.normalize("not a link"))
        assertNull(OpenLinkValidator.normalize("localhost"))
        assertNull(OpenLinkValidator.normalize(""))
    }
}

package com.wolferdwolf.drop.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberValidatorTest {
    @Test
    fun normalizesCommonPhoneFormats() {
        assertEquals("+919876543210", PhoneNumberValidator.normalize("+91 98765 43210"))
        assertEquals("04012345678", PhoneNumberValidator.normalize("040-1234-5678"))
    }

    @Test
    fun rejectsInvalidPhoneValues() {
        assertNull(PhoneNumberValidator.normalize("123"))
        assertNull(PhoneNumberValidator.normalize("call-me-now"))
        assertNull(PhoneNumberValidator.normalize("+91 98765 43210 ext 4"))
        assertNull(PhoneNumberValidator.normalize(""))
    }

    @Test
    fun createsExplicitDialerHistoryMetadata() {
        assertEquals("Opened dialer: +919876543210", CallConfirmationActivity.historyTitle("+919876543210"))
        assertEquals(
            "Status: Opened in phone app\nPhone: +919876543210",
            CallConfirmationActivity.historyContent("+919876543210")
        )
    }
}

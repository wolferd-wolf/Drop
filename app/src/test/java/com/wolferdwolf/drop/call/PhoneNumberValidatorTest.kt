package com.wolferdwolf.drop.call

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneNumberValidatorTest {
    @Test
    fun normalizesCommonPhoneFormats() {
        assertEquals("+919876543210", PhoneNumberValidator.normalize("+91 98765 43210"))
        assertEquals("04012345678", PhoneNumberValidator.normalize("040-1234-5678"))
        assertEquals("02079460958", PhoneNumberValidator.normalize("020 7946 0958"))
        assertEquals("+14155552671", PhoneNumberValidator.normalize("+1 (415) 555-2671"))
    }

    @Test
    fun rejectsInvalidPhoneValues() {
        assertNull(PhoneNumberValidator.normalize("123"))
        assertNull(PhoneNumberValidator.normalize("123456"))
        assertNull(PhoneNumberValidator.normalize("call-me-now"))
        assertNull(PhoneNumberValidator.normalize("+91 98765 43210 ext 4"))
        assertNull(PhoneNumberValidator.normalize("+1234567890123456"))
        assertNull(PhoneNumberValidator.normalize("+1 415 555 2671; DROP TABLE"))
        assertNull(PhoneNumberValidator.normalize("tel:+14155552671"))
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

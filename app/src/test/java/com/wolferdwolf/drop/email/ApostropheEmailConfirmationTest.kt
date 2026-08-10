package com.wolferdwolf.drop.email

import org.junit.Assert.assertNull
import org.junit.Test

class ApostropheEmailConfirmationTest {
    @Test
    fun acceptsApostropheRecipientForEditableConfirmation() {
        assertNull(EmailConfirmationActivity.validateRecipient("o'connor@example.com"))
    }
}

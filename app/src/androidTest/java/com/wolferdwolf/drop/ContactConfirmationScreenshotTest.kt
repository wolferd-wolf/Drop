package com.wolferdwolf.drop

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.contact.ContactConfirmationActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactConfirmationScreenshotTest {
    @Test
    fun captureEditableContactConfirmation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = "Name: Priya Reddy\nCompany: Wolf Labs\nPhone: +91 98765 43210\nEmail: priya@example.com\nMet at the launch event."
        val intent = Intent(context, ContactConfirmationActivity::class.java)
            .putExtra(ContactConfirmationActivity.EXTRA_SOURCE_TEXT, source)

        ActivityScenario.launch<ContactConfirmationActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Save contact", "Contact confirmation must reach the foreground")
            assertVisible(device, "Confirm contact details", "Contact confirmation must explain the review step")
            assertVisible(device, "Review and edit every field before Drop opens your Contacts app.", "Contact confirmation must explain user control")
            assertObject(device, By.clazz("android.widget.EditText").text("Priya Reddy"), "Detected name must be editable")
            assertObject(device, By.clazz("android.widget.EditText").text("+91 98765 43210"), "Detected phone must be editable")
            assertObject(device, By.clazz("android.widget.EditText").text("priya@example.com"), "Detected email must be editable")
            assertObject(device, By.clazz("android.widget.EditText").text("Wolf Labs"), "Detected company must be editable")
            assertVisibleAfterScroll(device, "Continue to Contacts", "Contact confirmation must require explicit continuation")
            assertVisibleAfterScroll(device, "Cancel", "Contact confirmation must be reversible")
            capture(device, "/data/local/tmp/drop-contact-confirmation.png")
        }
    }

    private fun capture(device: UiDevice, path: String) {
        device.waitForIdle()
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        assertTrue(device.executeShellCommand("ls -l $path").contains(path.substringAfterLast('/')))
    }

    private fun assertVisible(device: UiDevice, text: String, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(By.text(text)), CONTROL_TIMEOUT_MILLIS))
            .let { device.findObject(By.text(text)) }

    private fun assertVisibleAfterScroll(device: UiDevice, text: String, message: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT_MILLIS)?.let { return it }
        repeat(MAX_SCROLL_ATTEMPTS) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT_MILLIS)?.let { return it }
        }
        return device.findObject(By.text(text)) ?: throw AssertionError(message)
    }

    private fun assertObject(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), CONTROL_TIMEOUT_MILLIS))
            .let { device.findObject(selector) }

    private companion object {
        const val CONTROL_TIMEOUT_MILLIS = 20_000L
        const val SHORT_TIMEOUT_MILLIS = 2_000L
        const val MAX_SCROLL_ATTEMPTS = 6
    }
}

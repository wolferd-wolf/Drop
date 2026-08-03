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
import com.wolferdwolf.drop.email.EmailConfirmationActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EmailConfirmationScreenshotTest {
    @Test
    fun captureEditableEmailConfirmation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = "Subject: Project update\nEmail: priya@example.com\nPlease review the launch plan before Friday."
        val intent = Intent(context, EmailConfirmationActivity::class.java)
            .putExtra(EmailConfirmationActivity.EXTRA_SOURCE_TEXT, source)

        ActivityScenario.launch<EmailConfirmationActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Send email", "Email confirmation must reach the foreground")
            assertVisible(device, "Confirm email", "Email confirmation must explain the review step")
            assertObject(device, By.clazz("android.widget.EditText").text("priya@example.com"), "Recipient must be editable")
            assertObject(device, By.clazz("android.widget.EditText").text("Project update"), "Subject must be editable")
            assertVisibleAfterScroll(device, "Continue to Email", "Email confirmation must require explicit continuation")
            assertVisibleAfterScroll(device, "Cancel", "Email confirmation must be reversible")
            capture(device, "/data/local/tmp/drop-email-confirmation.png")
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

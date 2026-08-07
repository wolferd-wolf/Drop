package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApostropheEmailFlowTest {
    @Test
    fun apostropheEmailReachesEditableEmailConfirmation() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            tap(visible(device, "Paste text"), device)
            val input = objectFor(device, By.clazz("android.widget.EditText"), "Paste input is missing")
            input.text = "Contact o'connor@example.com about the proposal."
            tap(visible(device, "Continue"), device)
            tap(visible(device, "Extract details"), device)

            visible(device, "Extracted information")
            objectFor(device, By.clazz("android.widget.EditText").text("o'connor@example.com"), "Apostrophe email must be extracted exactly")
            tap(visibleAfterScroll(device, "See suggested actions"), device)

            visible(device, "Suggested actions")
            visibleAfterScroll(device, "Send email")
            tap(visibleAfterScroll(device, "Send email"), device)

            visible(device, "Confirm email")
            objectFor(device, By.clazz("android.widget.EditText").text("o'connor@example.com"), "Email confirmation must preserve the detected recipient")
            visibleAfterScroll(device, "Continue to Email")
            visibleAfterScroll(device, "Cancel")
            capture(device, "/data/local/tmp/drop-apostrophe-email-action.png")
        }
    }

    private fun visible(device: UiDevice, text: String): UiObject2 =
        assertNotNull("Expected visible text: $text", device.wait(Until.findObject(By.text(text)), TIMEOUT))
            .let { device.findObject(By.text(text)) }

    private fun visibleAfterScroll(device: UiDevice, text: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.let { return it }
        repeat(8) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.let { return it }
        }
        return device.findObject(By.text(text)) ?: throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun objectFor(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), TIMEOUT)).let { device.findObject(selector) }

    private fun tap(node: UiObject2, device: UiDevice) {
        var target: UiObject2? = node
        while (target != null && !target.isClickable) target = target.parent
        val bounds = (target ?: node).visibleBounds
        assertTrue("Target has no tappable area", !bounds.isEmpty)
        assertTrue("Coordinate tap failed", device.click(bounds.centerX(), bounds.centerY()))
        device.waitForIdle()
    }

    private fun capture(device: UiDevice, path: String) {
        device.waitForIdle()
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        assertTrue(device.executeShellCommand("ls -l $path").contains(path.substringAfterLast('/')))
    }

    private companion object {
        const val TIMEOUT = 20_000L
        const val SHORT_TIMEOUT = 2_000L
    }
}

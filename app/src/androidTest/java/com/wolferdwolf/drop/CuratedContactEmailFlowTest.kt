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
class CuratedContactEmailFlowTest {
    @Test
    fun editedPhoneAndEmailReachContactAndEmailConfirmations() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            clickText(device, "Paste text")
            objectFor(device, By.clazz("android.widget.EditText"), "Paste input missing").text =
                "Name: Priya Rao\nPhone: +91 98765 43210\nEmail: old@example.com"
            dismissKeyboard(device)
            clickText(device, "Continue")
            clickText(device, "Extract details")
            visible(device, "Extracted information")

            edit(device, "+91 98765 43210", "+91 91234 56789")
            edit(device, "old@example.com", "edited@example.com")
            clickText(device, "See suggested actions", scroll = true)
            visible(device, "Suggested actions")

            clickTextAndWait(device, "Save contact", "Confirm contact details")
            objectFor(device, By.clazz("android.widget.EditText").text("+91 91234 56789"), "Edited phone did not reach Contact confirmation")
            objectFor(device, By.clazz("android.widget.EditText").text("edited@example.com"), "Edited email did not reach Contact confirmation")
            capture(device, "/data/local/tmp/drop-contact-curated-values.png")
            clickText(device, "Cancel", scroll = true)
            visible(device, "Suggested actions")

            clickTextAndWait(device, "Send email", "Confirm email")
            objectFor(device, By.clazz("android.widget.EditText").text("edited@example.com"), "Edited email did not reach Email confirmation")
            capture(device, "/data/local/tmp/drop-email-curated-values.png")
        }
    }

    private fun edit(device: UiDevice, oldValue: String, newValue: String) {
        val field = objectFor(device, By.clazz("android.widget.EditText").text(oldValue), "Missing extracted value: $oldValue")
        field.click()
        field.text = newValue
        dismissKeyboard(device)
        objectFor(device, By.clazz("android.widget.EditText").text(newValue), "Edited value was not committed: $newValue")
    }

    private fun clickText(device: UiDevice, text: String, scroll: Boolean = false) {
        val node = if (scroll) visibleAfterScroll(device, text) else visible(device, text)
        tap(device, node)
        device.waitForIdle()
    }

    private fun clickTextAndWait(device: UiDevice, source: String, destination: String) {
        repeat(2) {
            tap(device, actionTargetAfterScroll(device, source))
            if (device.wait(Until.findObject(By.text(destination)), TIMEOUT) != null) return
            device.waitForIdle()
        }
        throw AssertionError("Expected $destination after $source")
    }

    private fun actionTargetAfterScroll(device: UiDevice, text: String): UiObject2 {
        repeat(9) { attempt ->
            val candidates = device.findObjects(By.text(text))
                .mapNotNull(::clickableAncestor)
                .filter { !it.visibleBounds.isEmpty }
            if (candidates.isNotEmpty()) {
                return candidates.minBy { it.visibleBounds.width() * it.visibleBounds.height() }
            }
            if (attempt < 8) {
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight * 3 / 4,
                    device.displayWidth / 2,
                    device.displayHeight / 4,
                    20
                )
                device.waitForIdle()
            }
        }
        throw AssertionError("Expected actionable control: $text")
    }

    private fun tap(device: UiDevice, node: UiObject2) {
        val target = clickableAncestor(node) ?: node
        val bounds = target.visibleBounds
        assertTrue("Target has no tappable area", !bounds.isEmpty)
        assertTrue("Tap failed", device.click(bounds.centerX(), bounds.centerY()))
    }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun dismissKeyboard(device: UiDevice) {
        device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
        device.waitForIdle()
    }

    private fun visible(device: UiDevice, text: String): UiObject2 =
        assertNotNull("Expected visible text: $text", device.wait(Until.findObject(By.text(text)), TIMEOUT))
            .let { device.findObject(By.text(text)) }

    private fun visibleAfterScroll(device: UiDevice, text: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.let { return it }
        repeat(8) {
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 3 / 4,
                device.displayWidth / 2,
                device.displayHeight / 4,
                20
            )
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.let { return it }
        }
        throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun objectFor(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), TIMEOUT)).let { device.findObject(selector) }

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

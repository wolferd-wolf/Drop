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
class CalendarNormalFlowTest {
    @Test
    fun editedVenueDateAndTimeReachCalendarFromNormalIntakeFlow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            clickText(device, "Paste text")
            val input = objectFor(device, By.clazz("android.widget.EditText"), "Paste text input is missing")
            input.text = "Product launch meeting on August 21st, 2026 at 4:00 PM. Venue: MG Road, Vijayawada."
            dismissKeyboardWithoutNavigation(device)
            clickText(device, "Continue")
            clickText(device, "Extract details")
            visible(device, "Extracted information")

            editAndWait(device, "August 21st, 2026", "2026-08-22", "Detected date must be editable")
            editAndWait(device, "4:00 PM", "6:15 PM", "Detected time must be editable")
            editAndWait(device, "MG Road, Vijayawada", "Edited venue, Vijayawada", "Detected venue must be editable")

            clickText(device, "See suggested actions", scroll = true)
            visible(device, "Suggested actions")
            clickText(device, "Add calendar event", scroll = true)

            visible(device, "Confirm event details")
            objectFor(device, By.clazz("android.widget.EditText").text("2026-08-22"), "Edited date did not reach Calendar confirmation")
            objectFor(device, By.clazz("android.widget.EditText").text("18:15"), "Edited time did not reach Calendar confirmation")
            objectFor(device, By.clazz("android.widget.EditText").text("Edited venue, Vijayawada"), "Edited venue did not reach Calendar confirmation")
            capture(device, "/data/local/tmp/drop-calendar-normal-flow.png")
        }
    }

    private fun editAndWait(device: UiDevice, oldValue: String, newValue: String, message: String) {
        val field = objectFor(device, By.clazz("android.widget.EditText").text(oldValue), message)
        field.click()
        field.text = newValue
        dismissKeyboardWithoutNavigation(device)
        objectFor(
            device,
            By.clazz("android.widget.EditText").text(newValue),
            "Edited value was not committed before the next field: $newValue"
        )
    }

    private fun clickText(device: UiDevice, text: String, scroll: Boolean = false) {
        val node = if (scroll) visibleAfterScroll(device, text) else visible(device, text)
        var target: UiObject2? = node
        while (target != null && !target.isClickable) target = target.parent
        (target ?: node).click()
        device.waitForIdle()
    }

    private fun dismissKeyboardWithoutNavigation(device: UiDevice) {
        device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
        device.waitForIdle()
    }

    private fun capture(device: UiDevice, path: String) {
        device.waitForIdle()
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        assertTrue(device.executeShellCommand("ls -l $path").contains(path.substringAfterLast('/')))
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

    private companion object {
        const val TIMEOUT = 20_000L
        const val SHORT_TIMEOUT = 2_000L
    }
}

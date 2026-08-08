package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NonEventDateTimeSuggestionFlowTest {
    @Test
    fun billWithDateAndTimeSuggestsReminderButNotCalendar() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            activateAndWait(device, "Paste text", "Add content for Drop to understand and turn into an action.")
            val input = device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)
            assertNotNull("Paste intake must provide an editable field", input)
            input.text = "Electricity bill due 12 August 2026 at 5:30 PM. Pay ₹1,250."
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            device.waitForIdle()

            activateAndWait(device, "Continue", "Import preview")
            activateAndWait(device, "Extract details", "Extracted information")
            activateAndWait(device, "See suggested actions", "Suggested actions", scroll = true)

            assertVisibleAfterScroll(device, "Create reminder", "A dated bill should still offer a useful reminder")
            assertVisibleAfterScroll(device, "Save reference", "A bill should remain saveable")
            assertVisibleAfterScroll(device, "Choose another action", "Manual action choice must remain available")
            assertFalse("Calendar must not be suggested for non-event date/time content", device.hasObject(By.text("Add calendar event")))
            capture(device, "/data/local/tmp/drop-non-event-calendar-suppressed.png")
        }
    }

    private fun activateAndWait(device: UiDevice, sourceText: String, destinationText: String, scroll: Boolean = false) {
        val source = if (scroll) assertVisibleAfterScroll(device, sourceText, "Expected action: $sourceText")
            else assertVisible(device, sourceText, "Expected action: $sourceText")
        tapResolvedTarget(device, source)
        assertNotNull("Expected $destinationText after activating $sourceText", device.wait(Until.findObject(By.text(destinationText)), TIMEOUT))
    }

    private fun assertVisible(device: UiDevice, text: String, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(By.text(text)), TIMEOUT)).let { device.findObject(By.text(text)) }

    private fun assertVisibleAfterScroll(device: UiDevice, text: String, message: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), 2_000L)?.let { return it }
        repeat(8) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), 2_000L)?.let { return it }
        }
        return device.findObject(By.text(text)) ?: throw AssertionError(message)
    }

    private fun tapResolvedTarget(device: UiDevice, node: UiObject2) {
        var target: UiObject2? = node
        while (target != null && !target.isClickable) target = target.parent
        val resolved = target ?: node
        val bounds = resolved.visibleBounds
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

    private companion object { const val TIMEOUT = 20_000L }
}

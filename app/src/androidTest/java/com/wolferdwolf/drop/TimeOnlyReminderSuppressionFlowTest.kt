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
class TimeOnlyReminderSuppressionFlowTest {
    @Test
    fun timeOnlyContentSuppressesReminderSuggestionAndKeepsManualChooser() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            activateAndWait(device, "Paste text", "Add content for Drop to understand and turn into an action.")
            val input = assertNotNull(
                "Paste intake must provide an editable field",
                device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)
            ).let { device.findObject(By.clazz("android.widget.EditText")) }
            input.text = "Team meeting at 5:30 PM in Wolf Hall."
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            device.waitForIdle()

            activateAndWait(device, "Continue", "Import preview")
            activateAndWait(device, "Extract details", "Extracted information")
            activateAndWait(device, "See suggested actions", "Suggested actions", scroll = true)

            assertVisibleAfterScroll(device, "Save reference", "Time-only content must still offer Save reference")
            assertVisibleAfterScroll(device, "Choose another action", "Manual action path must remain visible")
            assertFalse(
                "Reminder must not be suggested when no date was extracted",
                device.hasObject(By.text("Create reminder"))
            )
            assertFalse(
                "Calendar must not be suggested when no date was extracted",
                device.hasObject(By.text("Add calendar event"))
            )
            capture(device, "/data/local/tmp/drop-time-only-reminder-suppressed.png")
        }
    }

    private fun activateAndWait(device: UiDevice, sourceText: String, destinationText: String, scroll: Boolean = false) {
        val source = if (scroll) assertVisibleAfterScroll(device, sourceText, "Expected action: $sourceText")
            else assertVisible(device, sourceText, "Expected action: $sourceText")
        tapResolvedTarget(device, source)
        assertNotNull(
            "Expected $destinationText after activating $sourceText",
            device.wait(Until.findObject(By.text(destinationText)), TIMEOUT)
        )
    }

    private fun assertVisible(device: UiDevice, text: String, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(By.text(text)), TIMEOUT))
            .let { device.findObject(By.text(text)) }

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

    private companion object {
        const val TIMEOUT = 20_000L
    }
}

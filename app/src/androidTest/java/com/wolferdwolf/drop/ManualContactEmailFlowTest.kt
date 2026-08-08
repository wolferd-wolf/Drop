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
class ManualContactEmailFlowTest {
    @Test
    fun plainTextKeepsContactAndEmailAvailableThroughManualChooser() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            activateAndWait(device, "Paste text", "Add content for Drop to understand and turn into an action.")
            val input = assertNotNull(
                "Paste intake must provide an editable field",
                device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)
            ).let { device.findObject(By.clazz("android.widget.EditText")) }
            input.text = "Follow up with the supplier about the revised quote."
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            device.waitForIdle()

            activateAndWait(device, "Continue", "Import preview")
            activateAndWait(device, "Extract details", "Extracted information")
            activateAndWait(device, "See suggested actions", "Suggested actions", scroll = true)

            assertFalse("Contact should not be ranked without contact data", device.hasObject(By.text("Save contact")))
            assertFalse("Email should not be ranked without an email address", device.hasObject(By.text("Send email")))

            activateAndWait(device, "Choose another action", "Choose another action", scroll = true)
            assertVisibleAfterScroll(device, "Save contact", "Manual chooser must offer editable Contact")
            assertVisibleAfterScroll(device, "Send email", "Manual chooser must offer editable Email")
            capture(device, "/data/local/tmp/drop-manual-contact-email-actions.png")

            tapResolvedTarget(device, assertClickableAfterScroll(device, "Send email", "Expected clickable Send email manual action"))
            assertNotNull(
                "Manual Email must reach its editable confirmation screen",
                device.wait(Until.findObject(By.text("Confirm email")), TIMEOUT)
            )
            assertNotNull(
                "Manual Email must expose an editable recipient field",
                device.wait(Until.findObject(By.text("To")), TIMEOUT)
            )
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
        repeat(10) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), 2_000L)?.let { return it }
        }
        return device.findObject(By.text(text)) ?: throw AssertionError(message)
    }

    private fun assertClickableAfterScroll(device: UiDevice, text: String, message: String): UiObject2 {
        repeat(12) {
            device.findObjects(By.text(text)).firstOrNull { node -> clickableAncestor(node) != null }?.let { return it }
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
        }
        return device.findObjects(By.text(text)).firstOrNull { node -> clickableAncestor(node) != null }
            ?: throw AssertionError(message)
    }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var target: UiObject2? = node
        while (target != null && !target.isClickable) target = target.parent
        return target
    }

    private fun tapResolvedTarget(device: UiDevice, node: UiObject2) {
        val resolved = clickableAncestor(node) ?: node
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
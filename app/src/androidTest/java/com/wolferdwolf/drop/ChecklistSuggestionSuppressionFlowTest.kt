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
class ChecklistSuggestionSuppressionFlowTest {
    @Test
    fun paragraphStyleDocumentKeepsChecklistOutOfSuggestedActions() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            activateAndWait(device, "Paste text", "Add content for Drop to understand and turn into an action.")
            val input = assertNotNull(
                "Paste intake must provide an editable field",
                device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)
            ).let { device.findObject(By.clazz("android.widget.EditText")) }
            input.text = "Quarterly supplier update.\n" +
                "The revised quotation includes transport and installation charges.\n" +
                "The finance team will review the commercial terms before approval.\n" +
                "Delivery planning continues after the purchase order is released.\n" +
                "Keep this note as a reference for the next procurement review."
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            device.waitForIdle()

            activateAndWait(device, "Continue", "Import preview")
            activateAndWait(device, "Extract details", "Extracted information")
            activateAndWait(device, "See suggested actions", "Suggested actions", scroll = true)

            assertNotNull(
                "Paragraph-style content must retain the safe Save reference suggestion",
                device.wait(Until.findObject(By.text("Save reference")), TIMEOUT)
            )
            assertFalse(
                "Five ordinary prose lines must not be promoted to Create checklist",
                device.hasObject(By.text("Create checklist"))
            )
            assertNotNull(
                "Manual action path must remain visible even when checklist is suppressed",
                device.wait(Until.findObject(By.text("Choose another action")), TIMEOUT)
            )
            capture(device, "/data/local/tmp/drop-paragraph-checklist-suppressed.png")
        }
    }

    private fun activateAndWait(device: UiDevice, sourceText: String, destinationText: String, scroll: Boolean = false) {
        val source = if (scroll) assertVisibleAfterScroll(device, sourceText) else assertVisible(device, sourceText)
        tapResolvedTarget(device, source)
        assertNotNull(
            "Expected $destinationText after activating $sourceText",
            device.wait(Until.findObject(By.text(destinationText)), TIMEOUT)
        )
    }

    private fun assertVisible(device: UiDevice, text: String): UiObject2 =
        assertNotNull("Expected visible text: $text", device.wait(Until.findObject(By.text(text)), TIMEOUT))
            .let { device.findObject(By.text(text)) }

    private fun assertVisibleAfterScroll(device: UiDevice, text: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), 2_000L)?.takeIf { !it.visibleBounds.isEmpty }?.let { return it }
        repeat(10) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), 2_000L)?.takeIf { !it.visibleBounds.isEmpty }?.let { return it }
        }
        throw AssertionError("Expected visible text after scrolling: $text")
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

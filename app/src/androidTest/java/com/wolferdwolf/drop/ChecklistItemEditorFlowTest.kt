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
class ChecklistItemEditorFlowTest {
    @Test
    fun checklistProvidesItemLevelEditingBeforeSaving() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            tap(visible(device, "Paste text"), device)
            val intake = objectFor(device, By.clazz("android.widget.EditText"), "Paste input is missing")
            intake.text = "- Buy milk\n- [x] Call supplier\n- Pack charger"
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            tap(visible(device, "Continue"), device)
            tap(visible(device, "Extract details"), device)
            tap(visibleAfterScroll(device, "See suggested actions"), device)
            tap(actionTargetAfterScroll(device, "Create checklist"), device)

            visible(device, "Edit checklist items before saving")
            visibleAfterScroll(device, "Done")
            assertTrue("Imported checked marker must not remain in checklist item text", !device.hasObject(By.textContains("[x]")))
            objectFor(device, By.clazz("android.widget.EditText").text("Buy milk"), "First checklist item must be editable").text = "Buy oat milk"
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            tap(visible(device, "Mark done"), device)
            visible(device, "Done")
            tap(visible(device, "Move down"), device)

            val newItem = visibleAfterScroll(device, "New item")
            val newItemField = clickableAncestor(newItem)?.let { ancestor ->
                ancestor.findObject(By.clazz("android.widget.EditText"))
            } ?: device.findObjects(By.clazz("android.widget.EditText")).lastOrNull()
            assertNotNull("New checklist item field must be available", newItemField)
            newItemField!!.text = "Charge power bank"
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            tap(visibleAfterScroll(device, "Add item"), device)
            visibleAfterScroll(device, "Charge power bank")

            val deleteButtons = device.findObjects(By.text("Delete item")).filter { !it.visibleBounds.isEmpty }
            assertTrue("At least one visible item must be deletable", deleteButtons.isNotEmpty())
            tap(deleteButtons.last(), device)
            assertTrue("Checklist must still contain editable items after deletion", device.hasObject(By.clazz("android.widget.EditText")))

            capture(device, "/data/local/tmp/drop-checklist-item-editor.png")
            visibleAfterScroll(device, "Save checklist")
        }
    }

    private fun actionTargetAfterScroll(device: UiDevice, text: String): UiObject2 {
        repeat(10) { attempt ->
            val candidates = device.findObjects(By.text(text))
                .mapNotNull(::clickableAncestor)
                .distinctBy { it.visibleBounds }
                .filter { !it.visibleBounds.isEmpty }
            if (candidates.isNotEmpty()) return candidates.minBy { it.visibleBounds.width() * it.visibleBounds.height() }
            if (attempt < 9) {
                device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
                device.waitForIdle()
            }
        }
        throw AssertionError("Expected actionable control after scrolling: $text")
    }

    private fun visible(device: UiDevice, text: String): UiObject2 =
        assertNotNull("Expected visible text: $text", device.wait(Until.findObject(By.text(text)), TIMEOUT)).let { device.findObject(By.text(text)) }

    private fun visibleAfterScroll(device: UiDevice, text: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.takeIf { !it.visibleBounds.isEmpty }?.let { return it }
        repeat(10) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.takeIf { !it.visibleBounds.isEmpty }?.let { return it }
        }
        throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun objectFor(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), TIMEOUT)).let { device.findObject(selector) }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun tap(node: UiObject2, device: UiDevice) {
        val target = clickableAncestor(node) ?: node
        val bounds = target.visibleBounds
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

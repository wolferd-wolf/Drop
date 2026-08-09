package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.StaleObjectException
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

            visibleAfterScroll(device, "New item")
            val newItemField = device.findObjects(By.clazz("android.widget.EditText")).lastOrNull()
            assertNotNull("New checklist item field must be available", newItemField)
            newItemField!!.text = "Charge power bank"
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            tap(visibleAfterScroll(device, "Add item"), device)
            visibleAfterScroll(device, "Charge power bank")

            val deleteButtons = freshVisibleObjects(device, By.text("Delete item"))
            assertTrue("At least one visible item must be deletable", deleteButtons.isNotEmpty())
            tap(deleteButtons.last(), device)
            assertTrue("Checklist must still contain editable items after deletion", device.hasObject(By.clazz("android.widget.EditText")))

            capture(device, "/data/local/tmp/drop-checklist-item-editor.png")
            tap(visibleAfterScroll(device, "Save checklist"), device)

            visible(device, "Checklist saved")
            visible(device, "Your checklist is stored locally on this device and recorded in History.")
            visible(device, "Saved successfully")
            visible(device, "View in History")
            visible(device, "Done")
            capture(device, "/data/local/tmp/drop-checklist-saved-success.png")
            tap(visible(device, "View in History"), device)
            visibleAfterScroll(device, "References and checklists")
            visibleAfterScroll(device, "Checklist")
            visibleContainingAfterScroll(device, "☒ Buy oat milk")
            visibleContainingAfterScroll(device, "☒ Call supplier")
            visibleContainingAfterScroll(device, "☐ Pack charger")
            assertTrue(
                "Deleted checklist item must not be present after saving",
                !device.hasObject(By.textContains("Charge power bank"))
            )
            capture(device, "/data/local/tmp/drop-checklist-saved-history.png")
        }
    }

    private fun actionTargetAfterScroll(device: UiDevice, text: String): UiObject2 {
        repeat(10) { attempt ->
            val candidates = freshVisibleObjects(device, By.text(text))
                .mapNotNull(::clickableAncestorSafely)
                .distinctBy { safeBounds(it)?.toShortString() }
                .filter { safeBounds(it)?.isEmpty == false }
            if (candidates.isNotEmpty()) return candidates.minBy { safeBounds(it)?.let { bounds -> bounds.width() * bounds.height() } ?: Int.MAX_VALUE }
            if (attempt < 9) {
                scrollUp(device)
            }
        }
        throw AssertionError("Expected actionable control after scrolling: $text")
    }

    private fun visible(device: UiDevice, text: String): UiObject2 =
        objectFor(device, By.text(text), "Expected visible text: $text")

    private fun visibleAfterScroll(device: UiDevice, text: String): UiObject2 =
        visibleSelectorAfterScroll(device, By.text(text), "Expected visible text after scrolling: $text")

    private fun visibleContainingAfterScroll(device: UiDevice, text: String): UiObject2 =
        visibleSelectorAfterScroll(device, By.textContains(text), "Expected visible text containing after scrolling: $text")

    private fun visibleSelectorAfterScroll(device: UiDevice, selector: BySelector, message: String): UiObject2 {
        repeat(11) { attempt ->
            freshVisibleObject(device, selector)?.let { return it }
            if (attempt < 10) scrollUp(device)
        }
        throw AssertionError(message)
    }

    private fun freshVisibleObject(device: UiDevice, selector: BySelector): UiObject2? {
        device.wait(Until.hasObject(selector), SHORT_TIMEOUT)
        return freshVisibleObjects(device, selector).firstOrNull()
    }

    private fun freshVisibleObjects(device: UiDevice, selector: BySelector): List<UiObject2> =
        device.findObjects(selector).filter { safeBounds(it)?.isEmpty == false }

    private fun safeBounds(node: UiObject2): android.graphics.Rect? = try {
        node.visibleBounds
    } catch (_: StaleObjectException) {
        null
    }

    private fun objectFor(device: UiDevice, selector: BySelector, message: String): UiObject2 {
        assertTrue(message, device.wait(Until.hasObject(selector), TIMEOUT))
        return device.findObject(selector) ?: throw AssertionError(message)
    }

    private fun clickableAncestorSafely(node: UiObject2): UiObject2? = try {
        clickableAncestor(node)
    } catch (_: StaleObjectException) {
        null
    }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun tap(node: UiObject2, device: UiDevice) {
        val target = clickableAncestorSafely(node) ?: node
        val bounds = safeBounds(target) ?: throw AssertionError("Target became stale before tap")
        assertTrue("Target has no tappable area", !bounds.isEmpty)
        assertTrue("Coordinate tap failed", device.click(bounds.centerX(), bounds.centerY()))
        device.waitForIdle()
    }

    private fun scrollUp(device: UiDevice) {
        device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
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

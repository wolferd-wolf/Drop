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
class HistoryReferenceEditFlowTest {
    @Test
    fun savedReferenceTitleAndNotesAreEditableAndSearchable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            clickText(device, "Paste text")
            val input = objectFor(device, By.clazz("android.widget.EditText"), "Paste text input is missing")
            input.text = ORIGINAL_CONTENT
            dismissKeyboard(device)
            clickText(device, "Continue")
            clickText(device, "Extract details")
            clickText(device, "See suggested actions", scroll = true)
            clickText(device, "Save reference", scroll = true)
            visible(device, "Confirm before saving")
            val title = objectFor(device, By.clazz("android.widget.EditText"), "Reference title is missing")
            title.text = ORIGINAL_TITLE
            dismissKeyboard(device)
            clickText(device, "Save")

            visible(device, "Turn anything into the next useful action")
            clickTextMatching(device, "History")
            visible(device, ORIGINAL_TITLE)
            val cardTitle = visible(device, ORIGINAL_TITLE)
            val card = ancestorWithDescendantText(cardTitle, "View details")
                ?: throw AssertionError("Saved reference card must expose View details")
            tap(device, card.findObject(By.text("View details")) ?: throw AssertionError("View details missing"))
            visible(device, "Edit saved reference")

            val titleField = objectFor(device, By.clazz("android.widget.EditText").text(ORIGINAL_TITLE), "Editable title is missing")
            titleField.text = EDITED_TITLE
            dismissKeyboard(device)
            val notesField = objectFor(device, By.clazz("android.widget.EditText").text(""), "Notes field is missing")
            notesField.text = EDITED_NOTES
            dismissKeyboard(device)
            clickText(device, "Save changes", scroll = true)
            visible(device, "Changes saved.")
            visible(device, EDITED_TITLE)
            visible(device, EDITED_NOTES)
            capture(device, "/data/local/tmp/drop-history-reference-edited.png")

            clickText(device, "Back to History", scroll = true)
            visible(device, EDITED_TITLE)
            val search = objectFor(device, By.clazz("android.widget.EditText"), "History search field is missing")
            search.text = "followupwolf"
            dismissKeyboard(device)
            visible(device, EDITED_TITLE)
            search.text = ""
            dismissKeyboard(device)

            val editedTitle = visible(device, EDITED_TITLE)
            val editedCard = ancestorWithDescendantText(editedTitle, "Delete")
                ?: throw AssertionError("Edited saved reference card must expose Delete")
            tap(device, editedCard.findObject(By.text("Delete")) ?: throw AssertionError("Delete missing"))
            val dialogTitle = visible(device, "Delete saved reference?")
            val dialog = ancestorWithDescendantText(dialogTitle, "Keep reference")
                ?: throw AssertionError("Delete dialog missing")
            tap(device, dialog.findObject(By.text("Delete")) ?: throw AssertionError("Delete confirmation missing"))
        }
    }

    private fun clickText(device: UiDevice, text: String, scroll: Boolean = false) {
        val node = if (scroll) visibleAfterScroll(device, text) else visible(device, text)
        tap(device, node)
        device.waitForIdle()
    }

    private fun clickTextMatching(device: UiDevice, prefix: String) {
        val node = assertNotNull(
            "Expected visible text beginning with: $prefix",
            device.wait(Until.findObject(By.textStartsWith(prefix)), TIMEOUT)
        ).let { device.findObject(By.textStartsWith(prefix)) }
        tap(device, node)
        device.waitForIdle()
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
        throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun objectFor(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), TIMEOUT)).let { device.findObject(selector) }

    private fun ancestorWithDescendantText(node: UiObject2, text: String): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.findObject(By.text(text)) != null) return current
            current = current.parent
        }
        return null
    }

    private fun tap(device: UiDevice, node: UiObject2) {
        var target: UiObject2? = node
        while (target != null && !target.isClickable) target = target.parent
        target = target ?: node
        val bounds = target.visibleBounds
        assertTrue("Target has no tappable area", !bounds.isEmpty)
        assertTrue("Coordinate tap failed", device.click(bounds.centerX(), bounds.centerY()))
    }

    private fun dismissKeyboard(device: UiDevice) {
        device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
        device.waitForIdle()
    }

    private fun capture(device: UiDevice, path: String) {
        device.waitForIdle()
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        assertTrue(device.executeShellCommand("ls -l $path").contains(path.substringAfterLast('/')))
    }

    private companion object {
        const val ORIGINAL_TITLE = "Field research reference"
        const val EDITED_TITLE = "Wolf field research"
        const val EDITED_NOTES = "followupwolf review notes"
        const val ORIGINAL_CONTENT = "Field research material for product review."
        const val TIMEOUT = 20_000L
        const val SHORT_TIMEOUT = 2_000L
    }
}

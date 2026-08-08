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
            clickTextAndWaitForDestination(device, "Save reference", "Confirm before saving")
            val title = objectFor(device, By.clazz("android.widget.EditText"), "Reference title is missing")
            title.text = ORIGINAL_TITLE
            dismissKeyboard(device)
            clickText(device, "Save")

            visible(device, "Turn anything into the next useful action")
            clickTextMatching(device, "History")
            visibleAfterScroll(device, ORIGINAL_TITLE)
            val cardTitle = visibleAfterScroll(device, ORIGINAL_TITLE)
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
            visibleAfterScroll(device, EDITED_TITLE)
            val search = objectFor(device, By.clazz("android.widget.EditText"), "History search field is missing")
            search.text = "followupwolf"
            dismissKeyboard(device)
            visibleAfterScroll(device, EDITED_TITLE)
            clickText(device, "Delete", scroll = true)
            val dialogTitle = visible(device, "Delete saved reference?")
            val dialog = ancestorWithDescendantText(dialogTitle, "Keep reference")
                ?: throw AssertionError("Delete dialog missing")
            tap(device, dialog.findObject(By.text("Delete")) ?: throw AssertionError("Delete confirmation missing"))
        }
    }

    private fun clickTextAndWaitForDestination(
        device: UiDevice,
        sourceText: String,
        destinationText: String
    ) {
        repeat(2) { attempt ->
            tap(device, actionTargetAfterScroll(device, sourceText))
            if (device.wait(Until.findObject(By.text(destinationText)), TIMEOUT) != null) return
            if (attempt == 0) {
                device.waitForIdle()
                swipeUp(device)
            }
        }
        throw AssertionError("Expected visible text after activating $sourceText: $destinationText")
    }

    private fun actionTargetAfterScroll(device: UiDevice, text: String): UiObject2 {
        repeat(9) { attempt ->
            val candidates = device.findObjects(By.text(text))
                .mapNotNull(::clickableAncestor)
                .distinctBy { it.visibleBounds }
                .filter { !it.visibleBounds.isEmpty }
            if (candidates.isNotEmpty()) {
                return candidates.minBy { it.visibleBounds.width() * it.visibleBounds.height() }
            }
            if (attempt < 8) swipeUp(device)
        }
        throw AssertionError("Expected actionable control after scrolling: $text")
    }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
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
        visibleNode(device, text)?.let { return it }

        // History can preserve scroll position across detail/edit navigation. Search both directions
        // so a filtered result is verified regardless of whether the previous screen left us near
        // the top or bottom of the list.
        repeat(8) {
            swipeUp(device)
            visibleNode(device, text)?.let { return it }
        }
        repeat(16) {
            swipeDown(device)
            visibleNode(device, text)?.let { return it }
        }
        repeat(16) {
            swipeUp(device)
            visibleNode(device, text)?.let { return it }
        }
        throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun visibleNode(device: UiDevice, text: String): UiObject2? =
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.takeIf { node ->
            val bounds = node.visibleBounds
            !bounds.isEmpty && bounds.bottom > 0 && bounds.top < device.displayHeight
        }

    private fun swipeUp(device: UiDevice) {
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 3 / 4,
            device.displayWidth / 2,
            device.displayHeight / 4,
            20
        )
        device.waitForIdle()
    }

    private fun swipeDown(device: UiDevice) {
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight / 4,
            device.displayWidth / 2,
            device.displayHeight * 3 / 4,
            20
        )
        device.waitForIdle()
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
        val target = clickableAncestor(node) ?: node
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

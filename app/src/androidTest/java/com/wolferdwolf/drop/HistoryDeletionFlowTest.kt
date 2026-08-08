package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryDeletionFlowTest {
    @Test
    fun verifyHistoryPersistencePhase() {
        val phase = InstrumentationRegistry.getArguments().getString(PHASE_ARGUMENT) ?: return
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            when (phase) {
                PHASE_SAVE -> saveReference(device)
                PHASE_RESTORE_AND_DELETE -> verifyRestoredReferenceDetailAndDelete(device)
                PHASE_VERIFY_DELETION -> verifyDeletionPersisted(device)
                else -> throw AssertionError("Unknown History persistence phase: $phase")
            }
        }
    }

    private fun saveReference(device: UiDevice) {
        clickText(device, "Paste text")
        val input = objectFor(device, By.clazz("android.widget.EditText"), "Paste text input is missing")
        input.text = UNIQUE_CONTENT
        dismissKeyboardWithoutNavigation(device)
        clickText(device, "Continue")
        clickText(device, "Extract details")
        clickText(device, "See suggested actions", scroll = true)
        clickTextAndWaitForDestination(device, "Save reference", "Confirm before saving")

        val title = objectFor(device, By.clazz("android.widget.EditText"), "Reference title is missing")
        title.text = UNIQUE_TITLE
        dismissKeyboardWithoutNavigation(device)
        clickText(device, "Save")

        visible(device, "Turn anything into the next useful action")
        clickTextMatching(device, "History")
        visible(device, "Saved actions")
        visible(device, UNIQUE_TITLE)
        visible(device, UNIQUE_CONTENT)
        capture(device, "/data/local/tmp/drop-history-saved-reference.png")
    }

    private fun verifyRestoredReferenceDetailAndDelete(device: UiDevice) {
        visible(device, "Turn anything into the next useful action")
        clickTextMatching(device, "History")
        visible(device, "Saved actions")
        visible(device, UNIQUE_TITLE)
        visible(device, UNIQUE_CONTENT)
        capture(device, "/data/local/tmp/drop-history-reference-restored.png")

        clickText(device, "View details", scroll = true)
        visible(device, "Saved item details")
        visible(device, UNIQUE_TITLE)
        visible(device, "Saved reference")
        visible(device, "Original content")
        visible(device, UNIQUE_CONTENT)
        visible(device, "Stored locally on this device.")
        capture(device, "/data/local/tmp/drop-history-reference-detail.png")

        clickText(device, "Delete reference", scroll = true)
        val detailDialogTitle = visible(device, "Delete saved reference?")
        visible(device, "“$UNIQUE_TITLE” will be removed from History on this device. This cannot be undone.")
        capture(device, "/data/local/tmp/drop-history-detail-delete-confirmation.png")
        clickDialogButton(device, detailDialogTitle, "Keep reference")
        assertTrue(
            "Cancelling detail deletion must keep the saved reference visible",
            device.wait(Until.findObject(By.text(UNIQUE_TITLE)), TIMEOUT) != null
        )

        clickText(device, "Back to History")
        visible(device, "Saved actions")
        visible(device, UNIQUE_TITLE)
        val delete = visibleAfterScroll(device, "Delete")
        val card = ancestorWithDescendantText(delete, UNIQUE_TITLE)
            ?: throw AssertionError("Saved reference Delete must belong to the expected card")
        val cardDelete = card.findObject(By.text("Delete"))
            ?: throw AssertionError("Delete control is missing from saved reference card")
        tapResolvedTarget(device, cardDelete)
        device.waitForIdle()

        val historyDialogTitle = visible(device, "Delete saved reference?")
        visible(device, "“$UNIQUE_TITLE” will be removed from History on this device. This cannot be undone.")
        capture(device, "/data/local/tmp/drop-history-delete-confirmation.png")
        clickDialogButton(device, historyDialogTitle, "Keep reference")
        visible(device, UNIQUE_TITLE)

        val deleteAfterCancel = visibleAfterScroll(device, "Delete")
        val cardAfterCancel = ancestorWithDescendantText(deleteAfterCancel, UNIQUE_TITLE)
            ?: throw AssertionError("Saved reference must remain after cancelling deletion")
        val finalDelete = cardAfterCancel.findObject(By.text("Delete"))
            ?: throw AssertionError("Delete control must remain after cancelling deletion")
        tapResolvedTarget(device, finalDelete)
        val finalDialogTitle = visible(device, "Delete saved reference?")
        clickDialogButton(device, finalDialogTitle, "Delete")
        device.waitForIdle()

        assertTrue(
            "Deleted reference must disappear from History",
            device.wait(Until.gone(By.text(UNIQUE_TITLE)), TIMEOUT)
        )
        capture(device, "/data/local/tmp/drop-history-reference-deleted.png")
    }

    private fun verifyDeletionPersisted(device: UiDevice) {
        visible(device, "Turn anything into the next useful action")
        clickTextMatching(device, "History")
        visible(device, "Saved actions")
        assertTrue(
            "Deleted reference must remain absent after process restart",
            device.wait(Until.gone(By.text(UNIQUE_TITLE)), TIMEOUT)
        )
        capture(device, "/data/local/tmp/drop-history-deletion-persisted.png")
    }

    private fun clickDialogButton(device: UiDevice, dialogTitle: UiObject2, buttonText: String) {
        val dialog = ancestorWithDescendantText(dialogTitle, "Keep reference")
            ?: throw AssertionError("Deletion confirmation dialog is missing")
        val button = dialog.findObject(By.text(buttonText))
            ?: throw AssertionError("Deletion confirmation button is missing: $buttonText")
        tapResolvedTarget(device, button)
        device.waitForIdle()
        assertTrue(
            "Deletion confirmation dialog must close after $buttonText",
            device.wait(Until.gone(By.text("Delete saved reference?")), TIMEOUT)
        )
    }

    private fun ancestorWithDescendantText(node: UiObject2, text: String): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.findObject(By.text(text)) != null) return current
            current = current.parent
        }
        return null
    }

    private fun clickText(device: UiDevice, text: String, scroll: Boolean = false) {
        val node = if (scroll) visibleAfterScroll(device, text) else visible(device, text)
        tapResolvedTarget(device, node)
        device.waitForIdle()
    }

    private fun clickTextAndWaitForDestination(
        device: UiDevice,
        sourceText: String,
        destinationText: String
    ) {
        repeat(2) { attempt ->
            val source = actionTargetAfterScroll(device, sourceText)
            tapResolvedTarget(device, source)
            if (device.wait(Until.findObject(By.text(destinationText)), TIMEOUT) != null) return

            if (attempt == 0) {
                device.waitForIdle()
                modernScrollForward(device)
                device.waitForIdle()
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
            if (attempt < 8) {
                scrollIntoView(device, text)
            }
        }
        capture(device, "/data/local/tmp/drop-history-scroll-failure.png")
        throw AssertionError("Expected actionable control after scrolling: $text")
    }

    private fun clickTextMatching(device: UiDevice, prefix: String) {
        val node = assertNotNull(
            "Expected visible text beginning with: $prefix",
            device.wait(Until.findObject(By.textStartsWith(prefix)), TIMEOUT)
        ).let { device.findObject(By.textStartsWith(prefix)) }
        tapResolvedTarget(device, node)
        device.waitForIdle()
    }

    private fun tapResolvedTarget(device: UiDevice, node: UiObject2) {
        val target = clickableAncestor(node) ?: node
        val bounds = target.visibleBounds
        assertTrue("Target has no tappable area", !bounds.isEmpty)
        assertTrue("Coordinate tap failed", device.click(bounds.centerX(), bounds.centerY()))
    }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
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
        visibleNode(device, text)?.let { return it }

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
        capture(device, "/data/local/tmp/drop-history-scroll-failure.png")
        throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun visibleNode(device: UiDevice, text: String): UiObject2? {
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)
        return device.findObjects(By.text(text)).firstOrNull { node ->
            val bounds = node.visibleBounds
            !bounds.isEmpty && bounds.bottom > 0 && bounds.top < device.displayHeight
        }
    }

    private fun swipeUp(device: UiDevice) {
        val x = device.displayWidth / 2
        device.swipe(x, device.displayHeight * 3 / 4, x, device.displayHeight / 4, 20)
        device.waitForIdle()
    }

    private fun swipeDown(device: UiDevice) {
        val x = device.displayWidth / 2
        device.swipe(x, device.displayHeight / 4, x, device.displayHeight * 3 / 4, 20)
        device.waitForIdle()
    }

    private fun scrollIntoView(device: UiDevice, text: String) {
        if (visibleNode(device, text) != null) return
        modernScrollForward(device)
        device.waitForIdle()
        Thread.sleep(300)
    }

    private fun modernScrollForward(device: UiDevice) {
        val scrollable = device.findObjects(By.scrollable(true)).firstOrNull { !it.visibleBounds.isEmpty }
        if (scrollable != null) {
            runCatching { scrollable.scroll(Direction.UP, 0.65f) }
        } else {
            swipeUp(device)
        }
    }

    private fun objectFor(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), TIMEOUT)).let { device.findObject(selector) }

    private companion object {
        const val PHASE_ARGUMENT = "historyPhase"
        const val PHASE_SAVE = "save"
        const val PHASE_RESTORE_AND_DELETE = "restore-delete"
        const val PHASE_VERIFY_DELETION = "verify-deletion"
        const val UNIQUE_TITLE = "Quarterly strategy reference"
        const val UNIQUE_CONTENT = "Quarterly strategy notes for the operations review."
        const val TIMEOUT = 20_000L
        const val SHORT_TIMEOUT = 2_000L
    }
}

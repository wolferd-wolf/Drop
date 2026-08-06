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
class HistoryDeletionFlowTest {
    @Test
    fun savedReferenceCanBeDeletedFromHistory() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
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
            val savedTitle = visible(device, UNIQUE_TITLE)
            capture(device, "/data/local/tmp/drop-history-saved-reference.png")

            val card = ancestorWithDescendantText(savedTitle, "Delete")
                ?: throw AssertionError("Saved reference card must expose Delete")
            val delete = card.findObject(By.text("Delete"))
                ?: throw AssertionError("Delete control is missing from saved reference card")
            tapResolvedTarget(device, delete)
            device.waitForIdle()

            assertTrue(
                "Deleted reference must disappear from History",
                device.wait(Until.gone(By.text(UNIQUE_TITLE)), TIMEOUT)
            )
            capture(device, "/data/local/tmp/drop-history-reference-deleted.png")
        }
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
            val source = visibleAfterScroll(device, sourceText)
            tapResolvedTarget(device, source)
            if (device.wait(Until.findObject(By.text(destinationText)), TIMEOUT) != null) return

            if (attempt == 0) {
                device.waitForIdle()
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight * 2 / 3,
                    device.displayWidth / 2,
                    device.displayHeight / 2,
                    10
                )
                device.waitForIdle()
            }
        }
        throw AssertionError("Expected visible text after activating $sourceText: $destinationText")
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
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.let { return it }
        repeat(8) {
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 3 / 4,
                device.displayWidth / 2,
                device.displayHeight / 4,
                20
            )
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.let { return it }
        }
        return device.findObject(By.text(text)) ?: throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun objectFor(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), TIMEOUT)).let { device.findObject(selector) }

    private companion object {
        const val UNIQUE_TITLE = "Quarterly strategy reference"
        const val UNIQUE_CONTENT = "Quarterly strategy notes for the operations review."
        const val TIMEOUT = 20_000L
        const val SHORT_TIMEOUT = 2_000L
    }
}

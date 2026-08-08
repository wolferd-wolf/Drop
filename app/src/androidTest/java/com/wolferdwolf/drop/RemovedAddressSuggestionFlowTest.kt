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
class RemovedAddressSuggestionFlowTest {
    @Test
    fun removingDetectedAddressSuppressesMapsSuggestion() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            clickText(device, "Paste text")
            val input = objectFor(device, By.clazz("android.widget.EditText"), "Paste text input is missing")
            input.text = "Product launch meeting. Venue: MG Road, Vijayawada"
            dismissKeyboard(device)
            activateAndWait(device, "Continue", "Import preview")
            activateAndWait(device, "Extract details", "Extracted information")

            objectFor(
                device,
                By.clazz("android.widget.EditText").text("MG Road, Vijayawada"),
                "Expected a detected editable address before removal"
            )
            activateAndWait(device, "Remove Address", "See suggested actions", scroll = true)
            activateAndWait(device, "See suggested actions", "Suggested actions", scroll = true)

            visible(device, "Save reference")
            visible(device, "Choose another action")
            assertFalse(
                "Maps must not remain suggested after the user removes the address",
                device.findObjects(By.text("Open in Maps")).any { !it.visibleBounds.isEmpty }
            )
            capture(device, "/data/local/tmp/drop-removed-address-maps-suppressed.png")
        }
    }

    private fun activateAndWait(
        device: UiDevice,
        sourceText: String,
        destinationText: String,
        scroll: Boolean = false
    ) {
        repeat(2) { attempt ->
            val source = if (scroll) actionTargetAfterScroll(device, sourceText) else actionTarget(device, sourceText)
            tapResolvedTarget(device, source)
            if (device.wait(Until.findObject(By.text(destinationText)), TIMEOUT) != null) return
            if (attempt == 0) device.waitForIdle()
        }
        throw AssertionError("Expected $destinationText after activating $sourceText")
    }

    private fun actionTarget(device: UiDevice, text: String): UiObject2 {
        val candidates = visibleActionTargets(device, text)
        return candidates.minByOrNull { it.visibleBounds.width() * it.visibleBounds.height() }
            ?: visible(device, text)
    }

    private fun actionTargetAfterScroll(device: UiDevice, text: String): UiObject2 {
        visibleActionTargets(device, text).minByOrNull { it.visibleBounds.width() * it.visibleBounds.height() }
            ?.let { return it }

        repeat(SCROLL_STEPS) {
            swipeUp(device)
            visibleActionTargets(device, text).minByOrNull { it.visibleBounds.width() * it.visibleBounds.height() }
                ?.let { return it }
        }

        repeat(SCROLL_STEPS * 2) {
            swipeDown(device)
            visibleActionTargets(device, text).minByOrNull { it.visibleBounds.width() * it.visibleBounds.height() }
                ?.let { return it }
        }

        repeat(SCROLL_STEPS * 2) {
            swipeUp(device)
            visibleActionTargets(device, text).minByOrNull { it.visibleBounds.width() * it.visibleBounds.height() }
                ?.let { return it }
        }

        throw AssertionError("Expected actionable control after bidirectional scrolling: $text")
    }

    private fun visibleActionTargets(device: UiDevice, text: String): List<UiObject2> =
        device.findObjects(By.text(text))
            .map { clickableAncestor(it) ?: it }
            .distinctBy { it.visibleBounds }
            .filter { !it.visibleBounds.isEmpty }

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

    private fun clickText(device: UiDevice, text: String) {
        tapResolvedTarget(device, visible(device, text))
    }

    private fun tapResolvedTarget(device: UiDevice, node: UiObject2) {
        val target = clickableAncestor(node) ?: node
        val bounds = target.visibleBounds
        assertTrue("Target has no tappable area", !bounds.isEmpty)
        assertTrue("Coordinate tap failed", device.click(bounds.centerX(), bounds.centerY()))
        device.waitForIdle()
    }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
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

    private fun visible(device: UiDevice, text: String): UiObject2 =
        assertNotNull("Expected visible text: $text", device.wait(Until.findObject(By.text(text)), TIMEOUT))
            .let { device.findObject(By.text(text)) }

    private fun objectFor(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), TIMEOUT)).let { device.findObject(selector) }

    private companion object {
        const val TIMEOUT = 20_000L
        const val SCROLL_STEPS = 8
    }
}

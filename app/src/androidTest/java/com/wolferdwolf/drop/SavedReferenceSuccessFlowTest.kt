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
import com.wolferdwolf.drop.data.SavedReferenceStore
import com.wolferdwolf.drop.data.SavedSourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SavedReferenceSuccessFlowTest {
    @Test
    fun savedReferenceShowsSuccessAndHistoryPath() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = SavedReferenceStore(instrumentation.targetContext)
        val savedTitle = "North Star supplier terms"
        store.load().filter { it.title == savedTitle }.forEach { store.delete(it.id) }

        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(instrumentation)
            tap(visible(device, "Paste text"), device)
            val intake = objectFor(device, By.clazz("android.widget.EditText"), "Paste input is missing")
            intake.text = "Quarterly supplier terms draft for North Star Components."
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            tap(visible(device, "Continue"), device)
            tap(visible(device, "Extract details"), device)
            tap(visibleAfterScroll(device, "See suggested actions"), device)
            tap(actionTargetAfterScroll(device, "Save reference"), device)

            visible(device, "Confirm before saving")
            val titleField = objectFor(device, By.clazz("android.widget.EditText"), "Reference title must be editable")
            titleField.text = savedTitle
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            tap(visibleAfterScroll(device, "Save"), device)

            visible(device, "Reference saved")
            visible(device, "Your reference is stored locally on this device and recorded in History.")
            visible(device, "Saved successfully")
            visible(device, savedTitle)
            visible(device, "Source: Text")
            visible(device, "View in History")
            visible(device, "Done")

            val saved = store.load().firstOrNull { it.title == savedTitle }
            assertNotNull("Saved reference must be recorded before success is shown", saved)
            assertEquals(SavedSourceType.TEXT, saved!!.sourceType)
            assertEquals("Quarterly supplier terms draft for North Star Components.", saved.originalText)
            capture(device, "/data/local/tmp/drop-reference-saved-success.png")

            tap(visible(device, "View in History"), device)
            visible(device, "History")
            visibleAfterScroll(device, savedTitle)
            visibleAfterScroll(device, "Source: Text")
            capture(device, "/data/local/tmp/drop-reference-saved-history.png")
        }

        store.load().filter { it.title == savedTitle }.forEach { store.delete(it.id) }
    }

    private fun actionTargetAfterScroll(device: UiDevice, text: String): UiObject2 {
        repeat(10) { attempt ->
            val candidates = freshVisibleObjects(device, By.text(text))
                .mapNotNull(::clickableAncestorSafely)
                .distinctBy { safeBounds(it)?.toShortString() }
                .filter { safeBounds(it)?.isEmpty == false }
            if (candidates.isNotEmpty()) {
                return candidates.minBy { safeBounds(it)?.let { bounds -> bounds.width() * bounds.height() } ?: Int.MAX_VALUE }
            }
            if (attempt < 9) scrollUp(device)
        }
        throw AssertionError("Expected actionable control after scrolling: $text")
    }

    private fun visible(device: UiDevice, text: String): UiObject2 =
        objectFor(device, By.text(text), "Expected visible text: $text")

    private fun visibleAfterScroll(device: UiDevice, text: String): UiObject2 =
        visibleSelectorAfterScroll(device, By.text(text), "Expected visible text after scrolling: $text")

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

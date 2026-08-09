package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.reminder.ReminderHistoryStore
import com.wolferdwolf.drop.reminder.ReminderValidator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryReminderCancellationFlowTest {
    @Test
    fun reminderCancellationRequiresConfirmationAndReportsSuccess() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val store = ReminderHistoryStore(instrumentation.targetContext)
        val title = "Confirm cancellation supply check"
        store.load().filter { it.title == title }.forEach { store.delete(it.id) }
        val record = store.save(
            ReminderValidator.ValidReminder(
                title = title,
                notes = "Cancellation must be deliberate",
                triggerAtMillis = System.currentTimeMillis() + 24 * 60 * 60 * 1000L
            )
        )

        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                val device = UiDevice.getInstance(instrumentation)
                tapTextPrefix(device, "History")
                val search = requireVisible(device, By.clazz("android.widget.EditText"), "History search field is missing")
                search.text = title
                device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
                visibleAfterScroll(device, title)
                tapTextAfterScroll(device, "Cancel reminder")

                requireVisible(device, By.text("Cancel reminder?"), "Cancellation confirmation is missing")
                requireVisible(device, By.textContains(title), "Confirmation must identify the reminder")
                requireVisible(device, By.text("Keep reminder"), "Keep reminder action is missing")
                requireVisible(device, By.text("Yes, cancel reminder"), "Explicit cancellation action is missing")
                capture(device, "/data/local/tmp/drop-history-reminder-cancel-confirmation.png")

                tapText(device, "Keep reminder")
                assertTrue("Dismissing cancellation must preserve the reminder", store.load().any { it.id == record.id })
                tapTextAfterScroll(device, "Cancel reminder")
                requireVisible(device, By.text("Cancel reminder?"), "Cancellation confirmation did not reopen")
                tapText(device, "Yes, cancel reminder")

                visibleAfterScroll(device, "Reminder cancelled. Its notification was stopped and it was removed from History.")
                assertFalse("Confirmed cancellation must remove the reminder from History", store.load().any { it.id == record.id })
                capture(device, "/data/local/tmp/drop-history-reminder-cancelled.png")
            }
        } finally {
            store.load().filter { it.id == record.id }.forEach { store.delete(it.id) }
        }
    }

    private fun tapTextPrefix(device: UiDevice, prefix: String) {
        val node = requireVisible(device, By.textStartsWith(prefix), "Expected control beginning with $prefix")
        tap(node, device)
    }

    private fun tapText(device: UiDevice, text: String) {
        tap(requireVisible(device, By.text(text), "Expected control: $text"), device)
    }

    private fun tapTextAfterScroll(device: UiDevice, text: String) {
        repeat(12) { attempt ->
            device.findObjects(By.text(text)).firstOrNull { !it.visibleBounds.isEmpty }?.let {
                tap(it, device)
                return
            }
            if (attempt < 11) scrollForward(device)
        }
        throw AssertionError("Expected tappable text after scrolling: $text")
    }

    private fun visibleAfterScroll(device: UiDevice, text: String) {
        repeat(12) { attempt ->
            if (device.wait(Until.hasObject(By.text(text)), 750L)) return
            if (attempt < 11) scrollForward(device)
        }
        throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun requireVisible(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 {
        assertTrue(message, device.wait(Until.hasObject(selector), TIMEOUT))
        return device.findObject(selector) ?: throw AssertionError(message)
    }

    private fun tap(node: UiObject2, device: UiDevice) {
        var target: UiObject2? = node
        while (target != null && !target.isClickable) target = target.parent
        val bounds = (target ?: node).visibleBounds
        assertTrue("Target has no tappable area", !bounds.isEmpty)
        assertTrue("Tap failed", device.click(bounds.centerX(), bounds.centerY()))
        device.waitForIdle()
    }

    private fun scrollForward(device: UiDevice) {
        val list = UiScrollable(UiSelector().scrollable(true))
        if (list.exists()) {
            list.setAsVerticalList()
            list.scrollForward()
        } else {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 18)
        }
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

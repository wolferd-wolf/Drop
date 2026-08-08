package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.reminder.ReminderHistoryStore
import com.wolferdwolf.drop.reminder.ReminderValidator
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryReminderStatusFlowTest {
    @Test
    fun historyShowsScheduledAndElapsedReminderStatus() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = ReminderHistoryStore(context)
        val now = System.currentTimeMillis()
        val scheduled = store.save(
            ReminderValidator.ValidReminder(
                title = "Future wolf supply check",
                notes = "Scheduled status evidence",
                triggerAtMillis = now + 24 * 60 * 60 * 1000L
            )
        )
        val elapsed = store.save(
            ReminderValidator.ValidReminder(
                title = "Earlier wolf supply check",
                notes = "Elapsed status evidence",
                triggerAtMillis = now - 60 * 60 * 1000L
            )
        )

        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                clickTextMatching(device, "History")
                visibleAfterScroll(device, "Future wolf supply check")
                visibleAfterScroll(device, "Earlier wolf supply check")
                visibleAfterScrollPrefix(device, "Scheduled for ")
                visibleAfterScrollPrefix(device, "Trigger time passed · ")
                capture(device, "/data/local/tmp/drop-history-reminder-status.png")
            }
        } finally {
            store.delete(scheduled.id)
            store.delete(elapsed.id)
        }
    }

    private fun clickTextMatching(device: UiDevice, prefix: String) {
        val node = assertNotNull(
            "Expected visible text beginning with: $prefix",
            device.wait(Until.findObject(By.textStartsWith(prefix)), TIMEOUT)
        ).let { device.findObject(By.textStartsWith(prefix)) }
        val target = clickableAncestor(node) ?: node
        val bounds = target.visibleBounds
        assertTrue("History control has no tappable area", !bounds.isEmpty)
        assertTrue(device.click(bounds.centerX(), bounds.centerY()))
        device.waitForIdle()
    }

    private fun visibleAfterScroll(device: UiDevice, text: String) {
        repeat(10) { attempt ->
            if (device.wait(Until.hasObject(By.text(text)), 750L)) return
            if (attempt < 9) scroll(device)
        }
        throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun visibleAfterScrollPrefix(device: UiDevice, prefix: String) {
        repeat(10) { attempt ->
            if (device.wait(Until.hasObject(By.textStartsWith(prefix)), 750L)) return
            if (attempt < 9) scroll(device)
        }
        throw AssertionError("Expected visible text beginning with after scrolling: $prefix")
    }

    private fun scroll(device: UiDevice) {
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 3 / 4,
            device.displayWidth / 2,
            device.displayHeight / 4,
            18
        )
        device.waitForIdle()
    }

    private fun clickableAncestor(node: androidx.test.uiautomator.UiObject2): androidx.test.uiautomator.UiObject2? {
        var current: androidx.test.uiautomator.UiObject2? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
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

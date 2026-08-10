package com.wolferdwolf.drop

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.reminder.ReminderActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderSuccessGuardFlowTest {
    @Test
    fun successfulReminderCannotBeScheduledTwiceFromConfirmation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        device.executeShellCommand("pm grant com.wolferdwolf.drop android.permission.POST_NOTIFICATIONS")

        val intent = Intent(instrumentation.targetContext, ReminderActivity::class.java)
            .putExtra(
                ReminderActivity.EXTRA_SOURCE_TEXT,
                "Supplier follow-up\nDecember 31, 2099 at 09:30"
            )

        ActivityScenario.launch<ReminderActivity>(intent).use {
            tap(device, "Schedule")

            assertNotNull(
                "Successful reminder creation must show durable success feedback",
                device.wait(Until.findObject(By.textContains("Reminder scheduled for")), TIMEOUT)
            )
            assertNotNull(
                "The success feedback must confirm the reminder was saved in History",
                device.wait(Until.findObject(By.textContains("It is saved in History.")), TIMEOUT)
            )
            assertNotNull(
                "After a successful schedule the confirmation must offer a single safe exit",
                device.wait(Until.findObject(By.text("Done")), TIMEOUT)
            )
            assertFalse(
                "The Schedule action must disappear after success so a duplicate reminder cannot be created",
                device.hasObject(By.text("Schedule"))
            )
            capture(device, "/data/local/tmp/drop-reminder-scheduled-guard.png")
        }
    }

    private fun tap(device: UiDevice, text: String) {
        val node = assertNotNull(
            "Expected visible text: $text",
            device.wait(Until.findObject(By.text(text)), TIMEOUT)
        ).let { device.findObject(By.text(text)) }
        tapResolvedTarget(device, node)
    }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var target: UiObject2? = node
        while (target != null && !target.isClickable) target = target.parent
        return target
    }

    private fun tapResolvedTarget(device: UiDevice, node: UiObject2) {
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
    }
}

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

class DateOnlyReminderFlowTest {
    @Test
    fun curatedDateWithoutTimeSchedulesAsDateOnlyReminder() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val device = UiDevice.getInstance(instrumentation)
        device.executeShellCommand("pm grant com.wolferdwolf.drop android.permission.POST_NOTIFICATIONS")

        val intent = Intent(instrumentation.targetContext, ReminderActivity::class.java)
            .putExtra(ReminderActivity.EXTRA_SOURCE_TEXT, "Renew insurance by December 31, 2099")
            .putExtra(ReminderActivity.EXTRA_HAS_CURATED_RESULTS, true)
            .putExtra(ReminderActivity.EXTRA_CURATED_DATE, "2099-12-31")
            .putExtra(ReminderActivity.EXTRA_CURATED_TIME, "")

        ActivityScenario.launch<ReminderActivity>(intent).use {
            visible(device, "Confirm reminder details")
            visible(device, "Leave blank to remind at 09:00 on the selected date.")
            assertFalse(
                "Date-only confirmation must not invent a visible time value",
                device.hasObject(By.clazz("android.widget.EditText").text("09:00"))
            )

            tap(device, "Schedule")

            visibleContains(device, "Reminder scheduled for")
            visibleContains(device, "It is saved in History.")
            visible(device, "Done")
            assertFalse(
                "A successfully scheduled date-only reminder must not remain schedulable",
                device.hasObject(By.text("Schedule"))
            )
            capture(device, "/data/local/tmp/drop-date-only-reminder-scheduled.png")
        }
    }

    private fun visible(device: UiDevice, text: String): UiObject2 =
        assertNotNull(
            "Expected visible text: $text",
            device.wait(Until.findObject(By.text(text)), TIMEOUT)
        ).let { device.findObject(By.text(text)) }

    private fun visibleContains(device: UiDevice, text: String): UiObject2 =
        assertNotNull(
            "Expected visible text containing: $text",
            device.wait(Until.findObject(By.textContains(text)), TIMEOUT)
        ).let { device.findObject(By.textContains(text)) }

    private fun tap(device: UiDevice, text: String) {
        val node = visible(device, text)
        var target: UiObject2? = node
        while (target != null && !target.isClickable) target = target.parent
        val resolved = target ?: node
        val bounds = resolved.visibleBounds
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

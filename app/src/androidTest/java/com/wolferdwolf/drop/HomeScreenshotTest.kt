package com.wolferdwolf.drop

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.timetable.TimetableReviewActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenshotTest {
    @Test
    fun captureActionFirstHomeScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            scenario.onActivity { activity -> assertTrue(!activity.isFinishing) }
            waitForDrop(device)
            capture(device, "/data/local/tmp/drop-home.png")
            assertVisible(device, "Import screenshot or image", "Import image action must be visible on Home")
            assertVisible(device, "Import PDF", "Import PDF action must be visible on Home")
            assertVisible(device, "Paste text", "Paste text action must be visible on Home")
            assertVisible(device, "Add link", "Add link action must be visible on Home")
        }
    }

    @Test
    fun captureEditableTimetableReviewScreen() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, TimetableReviewActivity::class.java).putExtra(
            TimetableReviewActivity.EXTRA_OCR_TEXT,
            "Highschool Girls Plus\n9.00 Vadapada\n9.05 Prayer\n10.00 Class\n10.40 Break"
        )
        ActivityScenario.launch<TimetableReviewActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            waitForDrop(device)
            assertVisible(device, "Review timetable", "Timetable review title must be visible")
            assertVisible(device, "Save timetable", "Save timetable action must be visible")
            assertVisible(device, "Continue to reminders and calendar", "Follow-up action must be visible")
            capture(device, "/data/local/tmp/drop-timetable.png")
        }
    }

    private fun waitForDrop(device: UiDevice) {
        assertTrue(
            "Drop window must reach the foreground",
            device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), WINDOW_TIMEOUT_MILLIS)
        )
        device.waitForIdle()
    }

    private fun capture(device: UiDevice, path: String) {
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        assertTrue(device.executeShellCommand("ls -l $path").contains(path.substringAfterLast('/')))
    }

    private fun assertVisible(device: UiDevice, text: String, message: String) {
        assertNotNull(message, device.wait(Until.findObject(By.text(text)), CONTROL_TIMEOUT_MILLIS))
    }

    private companion object {
        const val APP_PACKAGE = "com.wolferdwolf.drop"
        const val WINDOW_TIMEOUT_MILLIS = 20_000L
        const val CONTROL_TIMEOUT_MILLIS = 10_000L
    }
}

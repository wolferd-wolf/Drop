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
    fun captureActionFirstHomeAndHistoryScreens() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            scenario.onActivity { activity -> assertTrue(!activity.isFinishing) }
            assertVisible(device, "Import screenshot or image", "Home must reach the foreground")
            assertVisible(device, "Import PDF", "Import PDF action must be visible on Home")
            assertVisible(device, "Paste text", "Paste text action must be visible on Home")
            assertVisible(device, "Add link", "Add link action must be visible on Home")
            val history = assertVisible(device, "History", "History control must be visible on Home")
            capture(device, "/data/local/tmp/drop-home.png")

            history.click()
            assertVisible(device, "Saved actions", "History screen must open from Home")
            assertVisible(device, "Back to Home", "History screen must provide a visible return action")
            capture(device, "/data/local/tmp/drop-history.png")
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
            assertVisible(device, "Review timetable", "Timetable review must reach the foreground")
            assertVisible(device, "Save as structured timetable", "Structured save action must be visible")
            assertVisible(device, "Save and create actions", "Bulk action path must be visible")
            capture(device, "/data/local/tmp/drop-timetable.png")
        }
    }

    private fun capture(device: UiDevice, path: String) {
        device.waitForIdle()
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        assertTrue(device.executeShellCommand("ls -l $path").contains(path.substringAfterLast('/')))
    }

    private fun assertVisible(device: UiDevice, text: String, message: String) =
        assertNotNull(message, device.wait(Until.findObject(By.text(text)), CONTROL_TIMEOUT_MILLIS))
            .let { device.findObject(By.text(text)) }

    private companion object {
        const val CONTROL_TIMEOUT_MILLIS = 20_000L
    }
}

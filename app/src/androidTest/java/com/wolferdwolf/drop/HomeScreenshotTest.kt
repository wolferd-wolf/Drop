package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenshotTest {
    @Test
    fun captureActionFirstHomeScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val device = UiDevice.getInstance(instrumentation)

            scenario.onActivity { activity ->
                assertTrue("Drop activity must be active", !activity.isFinishing)
            }
            assertTrue(
                "Drop window must reach the foreground",
                device.wait(Until.hasObject(By.pkg(APP_PACKAGE).depth(0)), WINDOW_TIMEOUT_MILLIS)
            )
            device.waitForIdle()

            val screenshotPath = "/data/local/tmp/drop-home.png"
            device.executeShellCommand("rm -f $screenshotPath")
            device.executeShellCommand("screencap -p $screenshotPath")
            val listing = device.executeShellCommand("ls -l $screenshotPath")
            assertTrue("Screenshot command must create a file", listing.contains("drop-home.png"))

            assertVisible(device, "Import screenshot or image", "Import image action must be visible on Home")
            assertVisible(device, "Import PDF", "Import PDF action must be visible on Home")
            assertVisible(device, "Paste text", "Paste text action must be visible on Home")
            assertVisible(device, "Add link", "Add link action must be visible on Home")
        }
    }

    private fun assertVisible(device: UiDevice, text: String, message: String) {
        assertNotNull(
            message,
            device.wait(Until.findObject(By.text(text)), CONTROL_TIMEOUT_MILLIS)
        )
    }

    private companion object {
        const val APP_PACKAGE = "com.wolferdwolf.drop"
        const val WINDOW_TIMEOUT_MILLIS = 20_000L
        const val CONTROL_TIMEOUT_MILLIS = 10_000L
    }
}

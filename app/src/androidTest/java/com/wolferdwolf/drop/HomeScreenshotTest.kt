package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenshotTest {
    @Test
    fun captureActionFirstHomeScreen() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val device = UiDevice.getInstance(instrumentation)
            device.waitForIdle()

            assertNotNull(
                "Import image action must be visible on Home",
                device.wait(Until.findObject(By.text("Import screenshot or image")), 5_000)
            )
            assertNotNull(
                "Import PDF action must be visible on Home",
                device.wait(Until.findObject(By.text("Import PDF")), 5_000)
            )
            assertNotNull(
                "Paste text action must be visible on Home",
                device.wait(Until.findObject(By.text("Paste text")), 5_000)
            )
            assertNotNull(
                "Add link action must be visible on Home",
                device.wait(Until.findObject(By.text("Add link")), 5_000)
            )

            val screenshotPath = "/data/local/tmp/drop-home.png"
            device.executeShellCommand("rm -f $screenshotPath")
            device.executeShellCommand("screencap -p $screenshotPath")
            val listing = device.executeShellCommand("ls -l $screenshotPath")
            assertNotNull("Screenshot command must create a file", listing.takeIf { it.contains("drop-home.png") })
        }
    }
}

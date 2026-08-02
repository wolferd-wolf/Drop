package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
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

            val screenshotPath = "/sdcard/drop-home.png"
            device.executeShellCommand("rm -f $screenshotPath")
            device.executeShellCommand("screencap -p $screenshotPath")
        }
    }
}

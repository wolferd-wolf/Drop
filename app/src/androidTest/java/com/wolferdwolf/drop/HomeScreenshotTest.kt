package com.wolferdwolf.drop

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import java.io.File
import org.junit.Assert.assertTrue
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

            val context: Context = instrumentation.targetContext
            val directory = File(context.getExternalFilesDir(null), "screenshots")
            assertTrue(directory.exists() || directory.mkdirs())

            val screenshot = File(directory, "drop-home.png")
            assertTrue(device.takeScreenshot(screenshot))
            assertTrue(screenshot.exists() && screenshot.length() > 0)
        }
    }
}

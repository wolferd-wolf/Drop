package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompactPriceIntakeFlowTest {
    @Test
    fun compactPriceIsVisibleAsOneCompleteExtractedValue() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Paste text").click()
            assertObject(device, By.clazz("android.widget.EditText")).text =
                "Estimated project budget ₹2.5 lakh."
            assertVisible(device, "Continue").click()
            assertVisible(device, "Import preview")
            assertVisible(device, "Extract details").click()
            assertVisible(device, "Extracted information")
            assertVisibleAfterScroll(device, "₹2.5 lakh")
            capture(device, "/data/local/tmp/drop-compact-price-extraction.png")
        }
    }

    private fun assertVisible(device: UiDevice, text: String): UiObject2 =
        assertNotNull("Expected visible text: $text", device.wait(Until.findObject(By.text(text)), TIMEOUT_MILLIS))
            .let { device.findObject(By.text(text)) }

    private fun assertVisibleAfterScroll(device: UiDevice, text: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT_MILLIS)?.let { return it }
        repeat(8) {
            device.swipe(
                device.displayWidth / 2,
                device.displayHeight * 3 / 4,
                device.displayWidth / 2,
                device.displayHeight / 4,
                20
            )
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT_MILLIS)?.let { return it }
        }
        throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun assertObject(device: UiDevice, selector: androidx.test.uiautomator.BySelector): UiObject2 =
        assertNotNull("Expected editable field", device.wait(Until.findObject(selector), TIMEOUT_MILLIS))
            .let { device.findObject(selector) }

    private fun capture(device: UiDevice, path: String) {
        device.waitForIdle()
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        assertTrue(device.executeShellCommand("ls -l $path").contains(path.substringAfterLast('/')))
    }

    private companion object {
        const val TIMEOUT_MILLIS = 20_000L
        const val SHORT_TIMEOUT_MILLIS = 2_000L
    }
}

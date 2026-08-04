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
class MultilineAddressIntakeFlowTest {
    @Test
    fun multilineAddressReachesMapsSuggestionWithCompleteLocation() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Paste text").click()
            assertObject(device, By.clazz("android.widget.EditText")).text =
                "Product launch\nVenue:\nWolf Convention Centre\nMG Road, Vijayawada\nAndhra Pradesh 520010\nDate: 18 August 2026"
            assertVisible(device, "Continue").click()
            assertVisible(device, "Import preview")
            assertVisible(device, "Extract details").click()
            assertVisible(device, "Extracted information")
            assertVisibleAfterScroll(device, "See suggested actions").click()
            assertVisible(device, "Suggested actions")
            assertVisibleAfterScroll(device, "Open in Maps")
            assertVisibleAfterScroll(
                device,
                "A likely address or venue was detected: Wolf Convention Centre, MG Road, Vijayawada, Andhra Pradesh 520010"
            )
            capture(device, "/data/local/tmp/drop-multiline-address-actions.png")
        }
    }

    private fun assertVisible(device: UiDevice, text: String): UiObject2 =
        assertNotNull("Expected visible text: $text", device.wait(Until.findObject(By.text(text)), TIMEOUT_MILLIS))
            .let { device.findObject(By.text(text)) }

    private fun assertVisibleAfterScroll(device: UiDevice, text: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT_MILLIS)?.let { return it }
        repeat(10) {
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

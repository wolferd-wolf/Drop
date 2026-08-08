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
class CountryCodeLinkFlowTest {
    @Test
    fun bareCountryCodeLinkReachesOpenLinkSuggestion() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            click(device, "Add link")
            val input = assertNotNull(device.wait(Until.findObject(By.clazz("android.widget.EditText")), 20_000L))
                .let { device.findObject(By.clazz("android.widget.EditText")) }
            input.text = "example.co.uk/community-event"
            click(device, "Continue")
            click(device, "Extract details")
            scrollAndClick(device, "See suggested actions")
            assertVisibleAfterScroll(device, "Open link")
            assertVisibleAfterScroll(device, "Choose another action")
            device.executeShellCommand("rm -f /data/local/tmp/drop-country-code-link-actions.png")
            device.executeShellCommand("screencap -p /data/local/tmp/drop-country-code-link-actions.png")
            assertTrue(device.executeShellCommand("ls -l /data/local/tmp/drop-country-code-link-actions.png").contains("drop-country-code-link-actions.png"))
        }
    }

    private fun click(device: UiDevice, text: String) {
        val node = assertNotNull(device.wait(Until.findObject(By.text(text)), 20_000L)).let { device.findObject(By.text(text)) }
        val target = generateSequence(node) { it.parent }.firstOrNull { it.isClickable } ?: node
        val bounds = target.visibleBounds
        assertTrue(device.click(bounds.centerX(), bounds.centerY()))
        device.waitForIdle()
    }

    private fun scrollAndClick(device: UiDevice, text: String) {
        click(device, assertVisibleAfterScroll(device, text).text)
    }

    private fun assertVisibleAfterScroll(device: UiDevice, text: String): androidx.test.uiautomator.UiObject2 {
        device.wait(Until.findObject(By.text(text)), 2_000L)?.let { return it }
        repeat(8) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), 2_000L)?.let { return it }
        }
        return device.findObject(By.text(text)) ?: throw AssertionError("Expected $text")
    }
}

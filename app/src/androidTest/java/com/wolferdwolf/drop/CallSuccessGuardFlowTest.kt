package com.wolferdwolf.drop

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.call.CallConfirmationActivity
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallSuccessGuardFlowTest {
    @Test
    fun successfulDialerLaunchBecomesReadOnlyDoneState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(context, CallConfirmationActivity::class.java)
            .putExtra(CallConfirmationActivity.EXTRA_PHONE, "+919876543210")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        ActivityScenario.launch<CallConfirmationActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            tap(visible(device, "Continue to Phone App"), device)

            // ACTION_DIAL moves Drop to the background. Return without placing a call
            // and verify the confirmation cannot launch or record the same action again.
            device.waitForIdle()
            device.pressBack()
            device.waitForIdle()

            visible(device, "Phone app opened")
            visible(device, "This action is saved in History. Return to Drop when you are done with the phone app.")
            visible(device, "Done")
            assertFalse("Successful Call action must not keep the launch button active", device.hasObject(By.text("Continue to Phone App")))

            val phoneField = assertNotNull(
                "Phone field must remain visible in the completed state",
                device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)
            ).let { device.findObject(By.clazz("android.widget.EditText")) }
            assertFalse("Completed Call phone field must be read-only", phoneField.isEnabled)

            capture(device, "/data/local/tmp/drop-call-opened-guard.png")
        }
    }

    private fun visible(device: UiDevice, text: String): UiObject2 =
        assertNotNull("Expected visible text: $text", device.wait(Until.findObject(By.text(text)), TIMEOUT))
            .let { device.findObject(By.text(text)) }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun tap(node: UiObject2, device: UiDevice) {
        val target = clickableAncestor(node) ?: node
        val bounds = target.visibleBounds
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
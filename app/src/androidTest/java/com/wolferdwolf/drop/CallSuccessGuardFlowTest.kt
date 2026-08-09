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
import org.junit.Assert.assertEquals
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

        ActivityScenario.launch<CallConfirmationActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            tap(visible(device, "Continue to Phone App"), device)

            // Prove the real ACTION_DIAL path actually left Drop. Then unwind whatever
            // dialer/default-app surface the emulator showed until Android restores the
            // same Drop task. This preserves the real confirmation activity state instead
            // of starting a second activity instance just for the verifier.
            assertNotNull(
                "Expected the external phone app to take the foreground",
                device.wait(Until.gone(By.pkg(context.packageName)), TIMEOUT)
            )

            repeat(MAX_BACK_ATTEMPTS) {
                if (device.currentPackageName == context.packageName) return@repeat
                device.pressBack()
                device.waitForIdle()
                device.wait(Until.findObject(By.pkg(context.packageName)), BACK_WAIT)
            }
            assertEquals(
                "Expected Back navigation to restore Drop after the external phone app",
                context.packageName,
                device.currentPackageName
            )

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
        const val BACK_WAIT = 1_500L
        const val MAX_BACK_ATTEMPTS = 6
    }
}

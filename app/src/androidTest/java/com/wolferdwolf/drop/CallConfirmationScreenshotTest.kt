package com.wolferdwolf.drop

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.call.CallConfirmationActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CallConfirmationScreenshotTest {
    @Test
    fun captureEditableCallConfirmation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, CallConfirmationActivity::class.java)
            .putExtra(CallConfirmationActivity.EXTRA_PHONE, "+91 98765 43210")

        ActivityScenario.launch<CallConfirmationActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertNotNull(device.wait(Until.findObject(By.text("Call")), 20_000L))
            assertNotNull(device.wait(Until.findObject(By.text("Confirm the phone number")), 5_000L))
            assertNotNull(device.wait(Until.findObject(By.text("Continue to Phone App")), 5_000L))
            assertNotNull(device.wait(Until.findObject(By.text("Cancel")), 5_000L))
            val path = "/data/local/tmp/drop-call-confirmation.png"
            device.waitForIdle()
            device.executeShellCommand("rm -f $path")
            device.executeShellCommand("screencap -p $path")
            assertTrue(device.executeShellCommand("ls -l $path").contains("drop-call-confirmation.png"))
        }
    }
}

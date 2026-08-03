package com.wolferdwolf.drop

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.link.OpenLinkConfirmationActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenLinkConfirmationScreenshotTest {
    @Test
    fun captureEditableOpenLinkConfirmation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, OpenLinkConfirmationActivity::class.java)
            .putExtra(OpenLinkConfirmationActivity.EXTRA_URL, "https://example.com/jobs")

        ActivityScenario.launch<OpenLinkConfirmationActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertNotNull("Open Link confirmation must reach the foreground", device.wait(Until.findObject(By.text("Open link")), 20_000L))
            assertNotNull("Review explanation must be visible", device.wait(Until.findObject(By.text("Confirm the website")), 5_000L))
            assertNotNull("Detected URL must be editable", device.wait(Until.findObject(By.clazz("android.widget.EditText").text("https://example.com/jobs")), 5_000L))
            assertNotNull("Explicit continuation must be visible", device.wait(Until.findObject(By.text("Continue to Browser")), 5_000L))
            assertNotNull("Cancel must be visible", device.wait(Until.findObject(By.text("Cancel")), 5_000L))
            val path = "/data/local/tmp/drop-open-link-confirmation.png"
            device.waitForIdle()
            device.executeShellCommand("rm -f $path")
            device.executeShellCommand("screencap -p $path")
            assertTrue(device.executeShellCommand("ls -l $path").contains("drop-open-link-confirmation.png"))
        }
    }
}

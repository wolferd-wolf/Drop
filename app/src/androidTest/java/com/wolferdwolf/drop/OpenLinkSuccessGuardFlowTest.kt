package com.wolferdwolf.drop

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.data.SavedReferenceStore
import com.wolferdwolf.drop.link.OpenLinkConfirmationActivity
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpenLinkSuccessGuardFlowTest {
    @Before
    fun setUpIntents() {
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Test
    fun successfulBrowserLaunchBecomesReadOnlyDoneStateAndRecordsOnce() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val url = "https://example.com/drop-open-link-guard"
        val store = SavedReferenceStore(context)
        val expectedTitle = OpenLinkConfirmationActivity.historyTitle(url)
        store.load().filter { it.title == expectedTitle }.forEach { store.delete(it.id) }

        val expectedBrowserIntent = allOf(
            hasAction(Intent.ACTION_VIEW),
            hasData(url)
        )
        intending(expectedBrowserIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        val intent = Intent(context, OpenLinkConfirmationActivity::class.java)
            .putExtra(OpenLinkConfirmationActivity.EXTRA_URL, url)

        try {
            ActivityScenario.launch<OpenLinkConfirmationActivity>(intent).use {
                val device = UiDevice.getInstance(instrumentation)
                tap(visible(device, "Continue to Browser"), device)

                intended(expectedBrowserIntent)
                visible(device, "Browser opened")
                visible(device, "This action is saved in History. Return to Drop when you are done with the browser.")
                visible(device, "Drop opened the browser once with the confirmed link. This screen is locked to prevent a second launch.")
                visible(device, "Done")
                assertFalse("Successful Open Link action must remove the browser launch control", device.hasObject(By.text("Continue to Browser")))
                assertFalse("Successful Open Link action must remove Cancel", device.hasObject(By.text("Cancel")))

                val urlField = assertNotNull(
                    "URL field must remain visible in the completed state",
                    device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)
                ).let { device.findObject(By.clazz("android.widget.EditText")) }
                assertFalse("Completed Open Link URL field must be read-only", urlField.isEnabled)

                val matches = store.load().filter { it.title == expectedTitle }
                assertEquals("Successful Open Link must record exactly one History item", 1, matches.size)
                assertTrue(matches.single().originalText.contains(url))

                capture(device, "/data/local/tmp/drop-open-link-opened-guard.png")
            }
        } finally {
            store.load().filter { it.title == expectedTitle }.forEach { store.delete(it.id) }
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

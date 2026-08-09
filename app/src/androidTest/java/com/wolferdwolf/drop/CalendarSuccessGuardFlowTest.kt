package com.wolferdwolf.drop

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.provider.CalendarContract
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
import com.wolferdwolf.drop.calendar.CalendarConfirmationActivity
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CalendarSuccessGuardFlowTest {
    @Before
    fun setUpIntents() {
        Intents.init()
    }

    @After
    fun releaseIntents() {
        Intents.release()
    }

    @Test
    fun successfulCalendarLaunchBecomesReadOnlyDoneState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedCalendarIntent = allOf(
            hasAction(Intent.ACTION_INSERT),
            hasData(CalendarContract.Events.CONTENT_URI)
        )
        intending(expectedCalendarIntent).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))

        val intent = Intent(context, CalendarConfirmationActivity::class.java)
            .putExtra(CalendarConfirmationActivity.EXTRA_SOURCE_TEXT, "Planning review on August 21st, 2026 at 4:00 PM. Venue: MG Road, Vijayawada.")
            .putExtra(CalendarConfirmationActivity.EXTRA_HAS_CURATED_RESULTS, true)
            .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_DATE, "2026-08-21")
            .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_TIME, "16:00")
            .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_VENUE, "MG Road, Vijayawada")

        ActivityScenario.launch<CalendarConfirmationActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            tap(visibleAfterScroll(device, "Continue to Calendar"), device)

            intended(expectedCalendarIntent)

            visible(device, "Calendar app opened")
            visible(device, "This action is saved in History. Return to Drop when you are done with the Calendar app.")
            visibleAfterScroll(device, "Done")
            assertFalse("Successful Calendar action must not keep the launch button active", device.hasObject(By.text("Continue to Calendar")))
            assertFalse("Successful Calendar action must remove the cancel path", device.hasObject(By.text("Cancel")))

            val fields = device.findObjects(By.clazz("android.widget.EditText"))
            assertTrue("Calendar fields must remain visible in the completed state", fields.size >= 6)
            assertTrue("Completed Calendar fields must all be read-only", fields.all { !it.isEnabled })

            capture(device, "/data/local/tmp/drop-calendar-opened-guard.png")
        }
    }

    private fun visible(device: UiDevice, text: String): UiObject2 =
        assertNotNull("Expected visible text: $text", device.wait(Until.findObject(By.text(text)), TIMEOUT))
            .let { device.findObject(By.text(text)) }

    private fun visibleAfterScroll(device: UiDevice, text: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.let { return it }
        repeat(8) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)?.let { return it }
        }
        return device.findObject(By.text(text)) ?: throw AssertionError("Expected visible text after scrolling: $text")
    }

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
        const val SHORT_TIMEOUT = 2_000L
    }
}

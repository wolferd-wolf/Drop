package com.wolferdwolf.drop

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.calendar.CalendarConfirmationActivity
import com.wolferdwolf.drop.maps.MapConfirmationActivity
import com.wolferdwolf.drop.pdf.PdfImportActivity
import com.wolferdwolf.drop.timetable.TimetableReviewActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenshotTest {
    @Test
    fun captureActionFirstHomeAndHistoryScreens() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            scenario.onActivity { activity -> assertTrue(!activity.isFinishing) }
            assertVisible(device, "Import screenshot or image", "Home must reach the foreground")
            assertVisible(device, "Import PDF", "Import PDF action must be visible on Home")
            assertVisible(device, "Paste text", "Paste text action must be visible on Home")
            assertVisible(device, "Add link", "Add link action must be visible on Home")
            val history = assertVisible(device, "History", "History control must be visible on Home")
            capture(device, "/data/local/tmp/drop-home.png")
            history.click()
            assertVisible(device, "Saved actions", "History screen must open from Home")
            assertVisible(device, "Back to Home", "History screen must provide a visible return action")
            capture(device, "/data/local/tmp/drop-history.png")
        }
    }

    @Test
    fun captureVisibleSuggestedAndManualActionsFlow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Import screenshot or image", "Home must reach the foreground")
            assertVisible(device, "Paste text", "Paste text action must be visible").click()
            assertVisible(device, "Add content for Drop to understand and turn into an action.", "Text entry must open")
            val input = assertObject(device, By.clazz("android.widget.EditText"), "Text entry must provide an editable field")
            input.text = "Team meeting on 2026-08-12 at 5:30 PM at MG Road, Vijayawada. Email team@example.com, call +91 98765 43210, or open drop.app/meeting"
            assertVisible(device, "Continue", "Text entry must provide Continue").click()
            assertVisible(device, "Import preview", "Imported text must reach a visible preview")
            assertVisible(device, "Extract details", "Preview must provide extraction action").click()
            assertVisible(device, "Extracted information", "Extraction screen must be visible")
            assertVisibleAfterScroll(device, "See suggested actions", "Extraction must lead to Suggested Actions").click()
            assertVisible(device, "Suggested actions", "Suggested Actions screen must be visible")
            assertVisible(device, "Save reference", "Safe default action must be visible")
            assertVisible(device, "Create reminder", "Relevant reminder action must be visible")
            capture(device, "/data/local/tmp/drop-suggested-actions.png")

            assertVisibleAfterScroll(device, "Choose another action", "Suggested Actions must expose a manual chooser").click()
            assertVisible(device, "All available actions", "Manual action chooser must open")
            assertVisibleAfterScroll(device, "Open link", "A bare domain must unlock the link action")
            capture(device, "/data/local/tmp/drop-all-actions.png")
            assertVisibleAfterScroll(device, "Create checklist", "Manual checklist action must be available")
            assertVisibleAfterScroll(device, "Search in Maps", "Manual Maps action must be available")
            assertVisibleAfterScroll(device, "Send email", "Detected email must unlock email action")
        }
    }

    @Test
    fun captureDetectedVenueMapsSuggestion() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Paste text", "Paste text action must be visible").click()
            val input = assertObject(device, By.clazz("android.widget.EditText"), "Text entry must provide an editable field")
            input.text = "Annual meeting venue: Sri Balaji Convention Hall, near RTC Bus Station, Gooty 515401"
            assertVisible(device, "Continue", "Text entry must provide Continue").click()
            assertVisible(device, "Extract details", "Preview must provide extraction action").click()
            assertVisibleAfterScroll(device, "See suggested actions", "Extraction must lead to Suggested Actions").click()
            assertVisible(device, "Open in Maps", "A likely venue must surface Maps as a relevant action")
            assertVisible(device, "A likely address or venue was detected: Annual meeting venue: Sri Balaji Convention Hall, near RTC Bus Station,", "Maps suggestion must explain the detected venue")
            capture(device, "/data/local/tmp/drop-maps-suggestion.png")
        }
    }

    @Test
    fun captureEditableMapsConfirmation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = "Annual meeting\nVenue: Sri Balaji Convention Hall, near RTC Bus Station, Gooty 515401\nBring your registration receipt."
        val intent = Intent(context, MapConfirmationActivity::class.java)
            .putExtra(MapConfirmationActivity.EXTRA_SOURCE_TEXT, source)

        ActivityScenario.launch<MapConfirmationActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Open in Maps", "Maps confirmation must reach the foreground")
            assertVisible(device, "Confirm the location", "Maps confirmation must explain the review step")
            assertVisible(device, "Drop detected a likely address or venue. Edit it before opening another app.", "Maps confirmation must explain why it is shown")
            val field = assertObject(device, By.clazz("android.widget.EditText"), "Maps confirmation must provide an editable address field")
            assertTrue(field.text.contains("Sri Balaji Convention Hall"))
            assertTrue(!field.text.contains("Bring your registration receipt"))
            assertVisible(device, "Open Maps", "Maps confirmation must provide an explicit launch action")
            assertVisible(device, "Cancel", "Maps confirmation must be reversible")
            capture(device, "/data/local/tmp/drop-maps-confirmation.png")
        }
    }

    @Test
    fun captureEditableCalendarConfirmation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = "Product launch meeting on August 21st, 2026 at 4:00 PM. Venue: MG Road, Vijayawada. Bring the final presentation."
        val intent = Intent(context, CalendarConfirmationActivity::class.java)
            .putExtra(CalendarConfirmationActivity.EXTRA_SOURCE_TEXT, source)

        ActivityScenario.launch<CalendarConfirmationActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Add calendar event", "Calendar confirmation must reach the foreground")
            assertVisible(device, "Confirm event details", "Calendar confirmation must explain the review step")
            assertVisible(device, "Review and edit every field before Drop opens your Calendar app.", "Calendar confirmation must explain control")
            assertVisible(device, "Date (YYYY-MM-DD)", "Calendar confirmation must expose an editable date")
            assertVisible(device, "Start (HH:MM)", "Calendar confirmation must expose an editable start time")
            assertVisible(device, "End (HH:MM)", "Calendar confirmation must expose an editable end time")
            assertVisible(device, "Venue", "Calendar confirmation must expose an editable venue")
            assertVisibleAfterScroll(device, "Continue to Calendar", "Calendar confirmation must require explicit continuation")
            assertVisibleAfterScroll(device, "Cancel", "Calendar confirmation must be reversible")
            capture(device, "/data/local/tmp/drop-calendar-confirmation.png")
        }
    }

    @Test
    fun captureGeneralImageOcrReviewAndUniversalFlow() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, TimetableReviewActivity::class.java).putExtra(
            TimetableReviewActivity.EXTRA_OCR_TEXT,
            "Community health camp on 18 August 2026 at 10:30 AM, Town Hall Road, Gooty. Contact help@example.com or +91 98765 43210."
        )
        ActivityScenario.launch<TimetableReviewActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Review image text", "Image OCR must open a general review screen")
            assertVisible(device, "Text extracted offline", "Image review must explain offline extraction")
            val continueAction = assertVisible(device, "Continue to Drop actions", "Image OCR must offer the standard Drop action flow")
            capture(device, "/data/local/tmp/drop-image-review.png")
            continueAction.click()
            assertVisible(device, "Import preview", "Image OCR must reach the standard import preview")
            assertVisible(device, "Extract details", "Image preview must expose extraction").click()
            assertVisible(device, "Extracted information", "Image OCR must reach extracted information")
        }
    }

    @Test
    fun capturePdfReviewAndUniversalFlow() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, PdfImportActivity::class.java)
            .putExtra(PdfImportActivity.EXTRA_TEST_NAME, "event-invitation.pdf")
            .putExtra(
                PdfImportActivity.EXTRA_TEST_TEXT,
                "Product launch meeting on August 21st, 2026 at 4:00 PM. Venue: MG Road, Vijayawada. Contact launch@example.com."
            )
        ActivityScenario.launch<PdfImportActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Review PDF text", "PDF import must open a visible review")
            assertVisible(device, "Embedded text extracted offline from 1 page.", "PDF review must explain offline extraction")
            val continueAction = assertVisible(device, "Continue to Drop actions", "PDF must offer the universal action flow")
            capture(device, "/data/local/tmp/drop-pdf-review.png")
            continueAction.click()
            assertVisible(device, "Import preview", "PDF text must reach Import Preview")
            assertVisible(device, "Extract details", "PDF preview must reach extraction").click()
            assertVisible(device, "Extracted information", "PDF must reach extraction")
            assertVisibleAfterScroll(device, "See suggested actions", "PDF must reach Suggested Actions").click()
            assertVisible(device, "Suggested actions", "PDF must reach the universal Suggested Actions screen")
            assertVisible(device, "Save reference", "PDF flow must retain Save reference")
        }
    }

    private fun capture(device: UiDevice, path: String) {
        device.waitForIdle()
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        assertTrue(device.executeShellCommand("ls -l $path").contains(path.substringAfterLast('/')))
    }

    private fun assertVisible(device: UiDevice, text: String, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(By.text(text)), CONTROL_TIMEOUT_MILLIS))
            .let { device.findObject(By.text(text)) }

    private fun assertVisibleAfterScroll(device: UiDevice, text: String, message: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT_MILLIS)?.let { return it }
        repeat(MAX_SCROLL_ATTEMPTS) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT_MILLIS)?.let { return it }
        }
        return device.findObject(By.text(text)) ?: throw AssertionError(message)
    }

    private fun assertObject(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), CONTROL_TIMEOUT_MILLIS))
            .let { device.findObject(selector) }

    private companion object {
        const val CONTROL_TIMEOUT_MILLIS = 20_000L
        const val SHORT_TIMEOUT_MILLIS = 2_000L
        const val MAX_SCROLL_ATTEMPTS = 8
    }
}

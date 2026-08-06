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
import com.wolferdwolf.drop.pdf.PdfImportActivity
import com.wolferdwolf.drop.timetable.TimetableReviewActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UniversalIntakeFlowTest {
    @Test
    fun pastedTextReachesPreviewExtractionAndSuggestedActions() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = device()
            tap(assertVisible(device, "Paste text", "Paste text must be available from Home"), device)
            val input = assertObject(device, By.clazz("android.widget.EditText"), "Paste text must provide an editable field")
            input.text = "Doctor appointment on 19 August 2026 at 10:30 AM. Call +91 98765 43210."
            tap(assertVisible(device, "Continue", "Paste text must provide Continue"), device)

            reachSuggestedActions(device, "Pasted text")
            assertVisibleAfterScroll(device, "Create reminder", "Pasted dates and times must unlock Reminder")
            capture(device, "/data/local/tmp/drop-paste-intake-actions.png")
        }
    }

    @Test
    fun addedLinkReachesPreviewExtractionAndSuggestedActions() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = device()
            tap(assertVisible(device, "Add link", "Add link must be available from Home"), device)
            val input = assertObject(device, By.clazz("android.widget.EditText"), "Add link must provide an editable field")
            input.text = "https://example.com/community-event"
            tap(assertVisible(device, "Continue", "Add link must provide Continue"), device)

            reachSuggestedActions(device, "Added link")
            assertVisibleAfterScroll(device, "Open link", "A valid web link must unlock Open link")
            capture(device, "/data/local/tmp/drop-link-intake-actions.png")
        }
    }

    @Test
    fun imageOcrReachesPreviewExtractionAndSuggestedActions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, TimetableReviewActivity::class.java).putExtra(
            TimetableReviewActivity.EXTRA_OCR_TEXT,
            "Community health camp on 18 August 2026 at 10:30 AM, Town Hall Road, Gooty. Contact help@example.com."
        )

        ActivityScenario.launch<TimetableReviewActivity>(intent).use {
            val device = device()
            assertVisible(device, "Review image text", "Image OCR must show a source review")
            tap(
                assertVisible(device, "Continue to Drop actions", "Image review must offer the universal flow"),
                device
            )

            reachSuggestedActions(device, "Image OCR")
            assertVisibleAfterScroll(device, "Save reference", "Image OCR must retain the safe default action")
            capture(device, "/data/local/tmp/drop-image-intake-actions.png")
        }
    }

    @Test
    fun pdfReachesPreviewExtractionAndSuggestedActions() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, PdfImportActivity::class.java)
            .putExtra(PdfImportActivity.EXTRA_TEST_NAME, "appointment.pdf")
            .putExtra(
                PdfImportActivity.EXTRA_TEST_TEXT,
                "Dental appointment on August 22, 2026 at 11:00 AM. Contact clinic@example.com."
            )

        ActivityScenario.launch<PdfImportActivity>(intent).use {
            val device = device()
            assertVisible(device, "Review PDF text", "PDF must show a source review")
            tap(assertVisible(device, "Continue to Drop actions", "PDF review must offer the universal flow"), device)

            reachSuggestedActions(device, "PDF")
            assertVisibleAfterScroll(device, "Save reference", "PDF must retain the safe default action")
            capture(device, "/data/local/tmp/drop-pdf-intake-actions.png")
        }
    }

    private fun reachSuggestedActions(device: UiDevice, sourceName: String) {
        assertVisible(device, "Import preview", "$sourceName must reach Import preview")
        clickAndWaitForDestination(
            device = device,
            source = assertVisible(device, "Extract details", "$sourceName preview must expose extraction"),
            destinationText = "Extracted information",
            failureMessage = "$sourceName must reach Extracted information"
        )
        clickAndWaitForDestination(
            device = device,
            source = assertVisibleAfterScroll(
                device,
                "See suggested actions",
                "$sourceName extraction must lead to Suggested Actions"
            ),
            destinationText = "Suggested actions",
            failureMessage = "$sourceName must reach Suggested Actions"
        )
        assertVisibleAfterScroll(device, "Choose another action", "$sourceName must retain the manual action path")
    }

    private fun clickAndWaitForDestination(
        device: UiDevice,
        source: UiObject2,
        destinationText: String,
        failureMessage: String
    ) {
        repeat(2) { attempt ->
            tap(source, device)
            if (device.wait(Until.findObject(By.text(destinationText)), CONTROL_TIMEOUT_MILLIS) != null) return
            if (attempt == 0) device.waitForIdle()
        }
        throw AssertionError(failureMessage)
    }

    private fun tap(node: UiObject2, device: UiDevice) {
        val target = clickableAncestor(node) ?: node
        val bounds = target.visibleBounds
        assertTrue("Target has no tappable area", !bounds.isEmpty)
        assertTrue("Coordinate tap failed", device.click(bounds.centerX(), bounds.centerY()))
        device.waitForIdle()
    }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun device(): UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

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

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
    fun captureVisiblePreviewExtractionAndSuggestedActionsFlow() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Import screenshot or image", "Home must reach the foreground")
            assertVisible(device, "Paste text", "Paste text action must be visible").click()
            assertVisible(device, "Add content for Drop to understand and turn into an action.", "Text entry must open")
            val input = assertObject(device, By.clazz("android.widget.EditText"), "Text entry must provide an editable field")
            input.text = "Team meeting on 12 August 2026 at 5:30 PM at MG Road, Vijayawada. Email team@example.com, call +91 98765 43210, or open https://example.com/meeting"
            assertVisible(device, "Continue", "Text entry must provide Continue").click()
            assertVisible(device, "Import preview", "Imported text must reach a visible preview")
            assertVisible(device, "Review before processing", "Preview must explain the review step")
            assertVisible(device, "Extract details", "Preview must provide extraction action").click()
            assertVisible(device, "Extracted information", "Extraction screen must be visible")
            assertVisibleAfterScroll(device, "See suggested actions", "Extraction must lead to Suggested Actions").click()
            assertVisible(device, "Suggested actions", "Suggested Actions screen must be visible")
            assertVisible(device, "Choose what Drop should do next", "Suggested Actions must explain the decision")
            assertVisible(device, "Save reference", "Safe default action must be visible")
            assertVisible(device, "Create reminder", "Relevant reminder action must be visible")
            capture(device, "/data/local/tmp/drop-suggested-actions.png")
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
            assertVisible(device, "Continuing does not save anything. You will still review the import, extracted details, and suggested actions.", "Image continuation must explain the confirmation flow")
            capture(device, "/data/local/tmp/drop-image-review.png")

            continueAction.click()
            assertVisible(device, "Import preview", "Image OCR must reach the standard import preview")
            assertVisible(device, "Extract details", "Image preview must expose extraction").click()
            assertVisible(device, "Extracted information", "Image OCR must reach extracted information")
            assertVisibleAfterScroll(device, "See suggested actions", "Image OCR must reach Suggested Actions").click()
            assertVisible(device, "Suggested actions", "Image OCR must reach the universal Suggested Actions screen")
            assertVisible(device, "Save reference", "Image flow must retain the safe default action")
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
        const val MAX_SCROLL_ATTEMPTS = 6
    }
}

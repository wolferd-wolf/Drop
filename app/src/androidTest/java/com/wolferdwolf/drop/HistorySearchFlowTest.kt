package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.data.SavedReferenceStore
import com.wolferdwolf.drop.data.SavedSourceType
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistorySearchFlowTest {
    @Test
    fun historySearchAndActionTypeFilterNarrowSavedActionsWithoutDeadEnds() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val store = SavedReferenceStore(context)
        val first = store.save(
            "Café quarterly wolf strategy",
            "Operations review notes for the northern region. Call +91 98765-43210.",
            now = 9_001L,
            sourceType = SavedSourceType.TEXT
        )
        val second = store.save("Supplier invoice", "Replacement bearings and machine oil.", now = 9_002L, sourceType = SavedSourceType.PDF)
        val recent = store.save("Today field note", "Fresh maintenance note saved today.", now = System.currentTimeMillis(), sourceType = SavedSourceType.IMAGE)

        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                clickTextMatching(device, "History")
                visible(device, "Saved actions")
                visible(device, "Filter by action type")
                visible(device, "All")
                visible(device, "Saved items")
                visible(device, "Reminders")
                visible(device, "Filter saved items by source")
                visible(device, "All sources")
                visible(device, "Text")
                visible(device, "Link")
                visible(device, "Image")
                visible(device, "PDF")

                setSearchText(device, "cafe")
                visibleAfterScroll(device, "Café quarterly wolf strategy")
                assertTrue("History search must match accented saved text without requiring accent input", device.wait(Until.hasObject(By.text("Café quarterly wolf strategy")), TIMEOUT))
                assertTrue("Unrelated reference must be filtered out", device.wait(Until.gone(By.text("Supplier invoice")), TIMEOUT))
                capture(device, "/data/local/tmp/drop-history-search-result.png")

                scrollToTop(device)
                setSearchText(device, "")
                scrollToTop(device)
                clickExactText(device, "PDF")
                visibleAfterScroll(device, "Supplier invoice")
                visibleAfterScroll(device, "Source: PDF")
                assertTrue("PDF source filter must hide text references", device.wait(Until.gone(By.text("Café quarterly wolf strategy")), TIMEOUT))
                assertTrue("PDF source filter must hide image references", device.wait(Until.gone(By.text("Today field note")), TIMEOUT))
                capture(device, "/data/local/tmp/drop-history-filter-pdf-source.png")

                scrollToTop(device)
                clickExactText(device, "All sources")
                scrollToTop(device)
                setSearchText(device, "")
                scrollToTop(device)
                clickExactText(device, "Reminders")
                visibleAfterScroll(device, "No saved actions are available in the selected filters.")
                assertTrue("Reminder filter must hide saved references", device.wait(Until.gone(By.text("Café quarterly wolf strategy")), TIMEOUT))
                assertTrue("Reminder filter must hide unrelated saved references", device.wait(Until.gone(By.text("Supplier invoice")), TIMEOUT))
                capture(device, "/data/local/tmp/drop-history-filter-reminders-empty.png")

                scrollToTop(device)
                clickExactText(device, "All")
                clickExactText(device, "Today")
                visibleAfterScroll(device, "Today field note")
                assertTrue("Today filter must hide older saved references", device.wait(Until.gone(By.text("Café quarterly wolf strategy")), TIMEOUT))
                assertTrue("Today filter must hide unrelated older references", device.wait(Until.gone(By.text("Supplier invoice")), TIMEOUT))
                capture(device, "/data/local/tmp/drop-history-filter-today.png")

                scrollToTop(device)
                clickExactText(device, "All dates")
                setSearchText(device, "quarterly invoice")
                visibleAfterScroll(device, "No saved actions match “quarterly invoice” in these filters. Try a different search, action type, or date.")
                assertTrue("Search must require every entered term to match the same saved item", device.wait(Until.gone(By.text("Café quarterly wolf strategy")), TIMEOUT))
                assertTrue("Search must not combine terms across separate saved items", device.wait(Until.gone(By.text("Supplier invoice")), TIMEOUT))
                capture(device, "/data/local/tmp/drop-history-search-empty.png")
            }
        } finally {
            store.delete(first.id)
            store.delete(second.id)
            store.delete(recent.id)
        }
    }

    private fun setSearchText(device: UiDevice, value: String) {
        scrollToTop(device)
        val search = visibleEditText(device)
        search.text = value
        dismissKeyboard(device)
        device.waitForIdle()
    }

    private fun clickExactText(device: UiDevice, text: String) {
        val node = assertNotNull(
            "Expected visible text: $text",
            device.wait(Until.findObject(By.text(text)), TIMEOUT)
        ).let { device.findObject(By.text(text)) }
        clickNode(device, node)
    }

    private fun clickTextMatching(device: UiDevice, prefix: String) {
        val node = assertNotNull(
            "Expected visible text beginning with: $prefix",
            device.wait(Until.findObject(By.textStartsWith(prefix)), TIMEOUT)
        ).let { device.findObject(By.textStartsWith(prefix)) }
        clickNode(device, node)
    }

    private fun clickNode(device: UiDevice, node: androidx.test.uiautomator.UiObject2) {
        val target = clickableAncestor(node) ?: node
        val bounds = target.visibleBounds
        assertTrue("History control has no tappable area", !bounds.isEmpty)
        assertTrue(device.click(bounds.centerX(), bounds.centerY()))
        device.waitForIdle()
    }

    private fun clickableAncestor(node: androidx.test.uiautomator.UiObject2): androidx.test.uiautomator.UiObject2? {
        var current: androidx.test.uiautomator.UiObject2? = node
        while (current != null) {
            if (current.isClickable) return current
            current = current.parent
        }
        return null
    }

    private fun dismissKeyboard(device: UiDevice) {
        device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
        device.waitForIdle()
    }

    private fun visible(device: UiDevice, text: String) =
        assertNotNull("Expected visible text: $text", device.wait(Until.findObject(By.text(text)), TIMEOUT))

    private fun visibleAfterScroll(device: UiDevice, text: String) {
        repeat(14) { attempt ->
            val node = device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT)
            if (node != null && runCatching { !node.visibleBounds.isEmpty }.getOrDefault(false)) return
            if (attempt < 13) swipeUp(device)
        }
        throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun visibleEditText(device: UiDevice) =
        assertNotNull(
            "History search field is missing after scrolling to top",
            device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)
        ).let { device.findObject(By.clazz("android.widget.EditText")) }

    private fun scrollToTop(device: UiDevice) {
        repeat(14) { swipeDown(device) }
    }

    private fun swipeUp(device: UiDevice) {
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight * 3 / 4,
            device.displayWidth / 2,
            device.displayHeight / 4,
            20
        )
        device.waitForIdle()
    }

    private fun swipeDown(device: UiDevice) {
        device.swipe(
            device.displayWidth / 2,
            device.displayHeight / 4,
            device.displayWidth / 2,
            device.displayHeight * 3 / 4,
            20
        )
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

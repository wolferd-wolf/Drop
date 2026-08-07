package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.data.SavedReferenceStore
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
            now = 9_001L
        )
        val second = store.save("Supplier invoice", "Replacement bearings and machine oil.", now = 9_002L)

        try {
            ActivityScenario.launch(MainActivity::class.java).use {
                val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
                clickTextMatching(device, "History")
                visible(device, "Saved actions")
                visible(device, "Filter by action type")
                visible(device, "All")
                visible(device, "Saved items")
                visible(device, "Reminders")

                val search = assertNotNull(
                    "History search field is missing",
                    device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)
                ).let { device.findObject(By.clazz("android.widget.EditText")) }

                search.text = "cafe"
                dismissKeyboard(device)
                visible(device, "Café quarterly wolf strategy")
                assertTrue("History search must match accented saved text without requiring accent input", device.wait(Until.hasObject(By.text("Café quarterly wolf strategy")), TIMEOUT))
                assertTrue("Unrelated reference must be filtered out", device.wait(Until.gone(By.text("Supplier invoice")), TIMEOUT))
                capture(device, "/data/local/tmp/drop-history-search-result.png")

                search.text = ""
                dismissKeyboard(device)
                clickTextMatching(device, "Reminders")
                visible(device, "No saved actions are available in this action-type filter.")
                assertTrue("Reminder filter must hide saved references", device.wait(Until.gone(By.text("Café quarterly wolf strategy")), TIMEOUT))
                assertTrue("Reminder filter must hide unrelated saved references", device.wait(Until.gone(By.text("Supplier invoice")), TIMEOUT))
                capture(device, "/data/local/tmp/drop-history-filter-reminders-empty.png")

                clickTextMatching(device, "All")
                visible(device, "Café quarterly wolf strategy")
                visible(device, "Supplier invoice")

                search.text = "quarterly invoice"
                dismissKeyboard(device)
                visible(device, "No saved actions match “quarterly invoice” in this filter. Try a different search or action type.")
                assertTrue("Search must require every entered term to match the same saved item", device.wait(Until.gone(By.text("Café quarterly wolf strategy")), TIMEOUT))
                assertTrue("Search must not combine terms across separate saved items", device.wait(Until.gone(By.text("Supplier invoice")), TIMEOUT))
                capture(device, "/data/local/tmp/drop-history-search-empty.png")
            }
        } finally {
            store.delete(first.id)
            store.delete(second.id)
        }
    }

    private fun clickTextMatching(device: UiDevice, prefix: String) {
        val node = assertNotNull(
            "Expected visible text beginning with: $prefix",
            device.wait(Until.findObject(By.textStartsWith(prefix)), TIMEOUT)
        ).let { device.findObject(By.textStartsWith(prefix)) }
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

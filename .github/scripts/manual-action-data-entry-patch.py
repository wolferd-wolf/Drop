from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    target = Path(path)
    text = target.read_text()
    if new in text:
        return
    if old not in text:
        raise SystemExit(f"Expected patch target not found in {path}")
    target.write_text(text.replace(old, new, 1))


replace_once(
    "app/src/main/java/com/wolferdwolf/drop/actions/SuggestedActionEngine.kt",
    '''            if (ExtractionType.URL in types) {\n                add(action(SuggestedActionType.OPEN_LINK, "Open link", "Open the detected web link.", 0))\n            }\n            add(action(SuggestedActionType.EMAIL, "Send email", "Enter or edit the recipient before opening your email app.", 0))\n            if (ExtractionType.PHONE in types) {\n                add(action(SuggestedActionType.CALL, "Call number", "Open the dialer with the detected number.", 0))\n            }''',
    '''            add(action(SuggestedActionType.OPEN_LINK, "Open link", "Enter or edit a website link before opening your browser.", 0))\n            add(action(SuggestedActionType.EMAIL, "Send email", "Enter or edit the recipient before opening your email app.", 0))\n            add(action(SuggestedActionType.CALL, "Call number", "Enter or edit a phone number before opening the dialer.", 0))'''
)

replace_once(
    "app/src/main/java/com/wolferdwolf/drop/MainActivity.kt",
    '''            SuggestedActionType.OPEN_LINK -> first(results, ExtractionType.URL)\n                ?.let {\n                    startActivity(\n                        Intent(this, OpenLinkConfirmationActivity::class.java)\n                            .putExtra(OpenLinkConfirmationActivity.EXTRA_URL, it)\n                    )\n                } ?: fail("No link was found.")''',
    '''            SuggestedActionType.OPEN_LINK -> startActivity(\n                Intent(this, OpenLinkConfirmationActivity::class.java)\n                    .putExtra(OpenLinkConfirmationActivity.EXTRA_URL, first(results, ExtractionType.URL).orEmpty())\n            )'''
)

replace_once(
    "app/src/main/java/com/wolferdwolf/drop/MainActivity.kt",
    '''            SuggestedActionType.CALL -> first(results, ExtractionType.PHONE)\n                ?.let {\n                    startActivity(\n                        Intent(this, CallConfirmationActivity::class.java)\n                            .putExtra(CallConfirmationActivity.EXTRA_PHONE, it)\n                    )\n                } ?: fail("No phone number was found.")''',
    '''            SuggestedActionType.CALL -> startActivity(\n                Intent(this, CallConfirmationActivity::class.java)\n                    .putExtra(CallConfirmationActivity.EXTRA_PHONE, first(results, ExtractionType.PHONE).orEmpty())\n            )'''
)

replace_once(
    "app/src/test/java/com/wolferdwolf/drop/actions/SuggestedActionEngineTest.kt",
    '''        assertTrue(SuggestedActionType.EMAIL in types)\n        assertFalse(SuggestedActionType.OPEN_LINK in types)\n        assertFalse(SuggestedActionType.CALL in types)''',
    '''        assertTrue(SuggestedActionType.EMAIL in types)\n        assertTrue(SuggestedActionType.OPEN_LINK in types)\n        assertTrue(SuggestedActionType.CALL in types)'''
)

instrumentation = Path("app/src/androidTest/java/com/wolferdwolf/drop/ManualLinkCallFlowTest.kt")
if not instrumentation.exists():
    instrumentation.write_text(r'''package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualLinkCallFlowTest {
    @Test
    fun plainTextKeepsLinkAndCallAvailableAsEditableManualActions() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            activateAndWait(device, "Paste text", "Add content for Drop to understand and turn into an action.")
            val input = assertNotNull(
                "Paste intake must provide an editable field",
                device.wait(Until.findObject(By.clazz("android.widget.EditText")), TIMEOUT)
            ).let { device.findObject(By.clazz("android.widget.EditText")) }
            input.text = "Follow up with the supplier about the revised quote."
            device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
            device.waitForIdle()

            activateAndWait(device, "Continue", "Import preview")
            activateAndWait(device, "Extract details", "Extracted information")
            activateAndWait(device, "See suggested actions", "Suggested actions", scroll = true)

            assertFalse("Open link should not be ranked without a URL", device.hasObject(By.text("Open link")))
            assertFalse("Call should not be ranked without a phone number", device.hasObject(By.text("Call number")))

            activateAndWait(device, "Choose another action", "Choose another action", scroll = true)
            assertVisibleAfterScroll(device, "Open link", "Manual chooser must offer editable Open link")
            assertVisibleAfterScroll(device, "Call number", "Manual chooser must offer editable Call")
            capture(device, "/data/local/tmp/drop-manual-link-call-actions.png")

            tapResolvedTarget(device, assertClickableAfterScroll(device, "Open link", "Expected clickable Open link manual action"))
            assertNotNull(
                "Manual Open link must reach its editable confirmation screen",
                device.wait(Until.findObject(By.text("Confirm the website")), TIMEOUT)
            )
            assertNotNull(
                "Manual Open link must expose an editable URL field",
                device.wait(Until.findObject(By.text("Website link")), TIMEOUT)
            )
            capture(device, "/data/local/tmp/drop-manual-link-empty-confirmation.png")
        }
    }

    private fun activateAndWait(device: UiDevice, sourceText: String, destinationText: String, scroll: Boolean = false) {
        val source = if (scroll) assertVisibleAfterScroll(device, sourceText, "Expected action: $sourceText")
            else assertVisible(device, sourceText, "Expected action: $sourceText")
        tapResolvedTarget(device, source)
        assertNotNull(
            "Expected $destinationText after activating $sourceText",
            device.wait(Until.findObject(By.text(destinationText)), TIMEOUT)
        )
    }

    private fun assertVisible(device: UiDevice, text: String, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(By.text(text)), TIMEOUT))
            .let { device.findObject(By.text(text)) }

    private fun assertVisibleAfterScroll(device: UiDevice, text: String, message: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), 2_000L)?.takeIf { !it.visibleBounds.isEmpty }?.let { return it }
        repeat(10) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), 2_000L)?.takeIf { !it.visibleBounds.isEmpty }?.let { return it }
        }
        repeat(20) {
            device.swipe(device.displayWidth / 2, device.displayHeight / 4, device.displayWidth / 2, device.displayHeight * 3 / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), 2_000L)?.takeIf { !it.visibleBounds.isEmpty }?.let { return it }
        }
        throw AssertionError(message)
    }

    private fun assertClickableAfterScroll(device: UiDevice, text: String, message: String): UiObject2 {
        repeat(12) {
            device.findObjects(By.text(text)).firstOrNull { node -> !node.visibleBounds.isEmpty && clickableAncestor(node) != null }?.let { return it }
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
        }
        repeat(24) {
            device.findObjects(By.text(text)).firstOrNull { node -> !node.visibleBounds.isEmpty && clickableAncestor(node) != null }?.let { return it }
            device.swipe(device.displayWidth / 2, device.displayHeight / 4, device.displayWidth / 2, device.displayHeight * 3 / 4, 20)
            device.waitForIdle()
        }
        throw AssertionError(message)
    }

    private fun clickableAncestor(node: UiObject2): UiObject2? {
        var target: UiObject2? = node
        while (target != null && !target.isClickable) target = target.parent
        return target
    }

    private fun tapResolvedTarget(device: UiDevice, node: UiObject2) {
        val resolved = clickableAncestor(node) ?: node
        val bounds = resolved.visibleBounds
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
''')

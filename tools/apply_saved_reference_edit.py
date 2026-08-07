from pathlib import Path
import re

root = Path('.')
main = root / 'app/src/main/java/com/wolferdwolf/drop/MainActivity.kt'
text = main.read_text()
text = text.replace(
    'HistorySearch.matches(searchQuery, it.title, it.originalText) &&',
    'HistorySearch.matches(searchQuery, it.title, it.originalText, it.notes) &&'
)
old_call = '''                        ReferenceDetailScreen(
                            reference,
                            { screen = Screen.HISTORY },
                            {
                                referenceStore.delete(reference.id)'''
new_call = '''                        ReferenceDetailScreen(
                            reference,
                            { screen = Screen.HISTORY },
                            { title, notes ->
                                val updated = referenceStore.update(reference, title, notes)
                                selectedReference = updated
                                refreshHistory()
                            },
                            {
                                referenceStore.delete(reference.id)'''
if old_call not in text:
    raise SystemExit('ReferenceDetailScreen call pattern not found')
text = text.replace(old_call, new_call)

start = text.index('@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun ReferenceDetailScreen')
end = text.index('@OptIn(ExperimentalMaterial3Api::class)\n@Composable\nprivate fun EntryScreen', start)
replacement = '''@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReferenceDetailScreen(
    reference: SavedReference,
    onBack: () -> Unit,
    onUpdate: (String, String) -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable(reference.id) { mutableStateOf(reference.title) }
    var notes by rememberSaveable(reference.id) { mutableStateOf(reference.notes) }
    var saveStatus by rememberSaveable(reference.id) { mutableStateOf<String?>(null) }
    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete saved reference?") },
            text = { Text("“${reference.title}” will be removed from History on this device. This cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    onDelete()
                }) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = false }) { Text("Keep reference") }
            }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Saved item details") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Edit saved reference", style = MaterialTheme.typography.headlineSmall) }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(SavedReferenceStore.MAX_TITLE_LENGTH); saveStatus = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it; saveStatus = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notes") },
                    supportingText = { Text("Notes are stored locally and included in History search.") },
                    minLines = 3
                )
            }
            item {
                Button(
                    onClick = { onUpdate(title, notes); saveStatus = "Changes saved." },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save changes") }
            }
            saveStatus?.let { status -> item { Text(status, color = MaterialTheme.colorScheme.primary) } }
            item { Text("Saved reference", style = MaterialTheme.typography.labelLarge) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Original content", style = MaterialTheme.typography.titleMedium)
                        Text(reference.originalText)
                    }
                }
            }
            item { Text("Stored locally on this device.") }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to History") } }
            item { TextButton(onClick = { showDeleteConfirmation = true }, modifier = Modifier.fillMaxWidth()) { Text("Delete reference") } }
        }
    }
}

'''
text = text[:start] + replacement + text[end:]
main.write_text(text)

workflow_script = root / '.github/scripts/run-screenshot-tests.sh'
ws = workflow_script.read_text()
needle = '  drop-history-reference-detail\n'
if needle not in ws:
    raise SystemExit('screenshot list insertion point not found')
ws = ws.replace(needle, needle + '  drop-history-reference-edited\n', 1)
workflow_script.write_text(ws)

unit = root / 'app/src/test/java/com/wolferdwolf/drop/data/SavedReferenceCodecTest.kt'
unit.parent.mkdir(parents=True, exist_ok=True)
unit.write_text('''package com.wolferdwolf.drop.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SavedReferenceCodecTest {
    @Test
    fun notesRoundTripAndLegacyRecordsRemainReadable() {
        val reference = SavedReference(42L, "Wolf plan", "Original text", 1234L, "Follow up Friday")
        assertEquals(reference, SavedReferenceCodec.decode(SavedReferenceCodec.encode(reference)))

        val legacy = "42|1234|Wolf+plan|Original+text"
        assertEquals(
            SavedReference(42L, "Wolf plan", "Original text", 1234L, ""),
            SavedReferenceCodec.decode(legacy)
        )
    }
}
''')

android_test = root / 'app/src/androidTest/java/com/wolferdwolf/drop/HistoryReferenceEditFlowTest.kt'
android_test.write_text(r'''package com.wolferdwolf.drop

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryReferenceEditFlowTest {
    @Test
    fun savedReferenceTitleAndNotesAreEditableAndSearchable() {
        ActivityScenario.launch(MainActivity::class.java).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            clickText(device, "Paste text")
            val input = objectFor(device, By.clazz("android.widget.EditText"), "Paste text input is missing")
            input.text = ORIGINAL_CONTENT
            dismissKeyboard(device)
            clickText(device, "Continue")
            clickText(device, "Extract details")
            clickText(device, "See suggested actions", scroll = true)
            clickText(device, "Save reference", scroll = true)
            visible(device, "Confirm before saving")
            val title = objectFor(device, By.clazz("android.widget.EditText"), "Reference title is missing")
            title.text = ORIGINAL_TITLE
            dismissKeyboard(device)
            clickText(device, "Save")

            visible(device, "Turn anything into the next useful action")
            clickTextMatching(device, "History")
            visible(device, ORIGINAL_TITLE)
            val cardTitle = visible(device, ORIGINAL_TITLE)
            val card = ancestorWithDescendantText(cardTitle, "View details")
                ?: throw AssertionError("Saved reference card must expose View details")
            tap(device, card.findObject(By.text("View details")) ?: throw AssertionError("View details missing"))
            visible(device, "Edit saved reference")

            val titleField = objectFor(device, By.clazz("android.widget.EditText").text(ORIGINAL_TITLE), "Editable title is missing")
            titleField.text = EDITED_TITLE
            dismissKeyboard(device)
            val notesField = objectFor(device, By.clazz("android.widget.EditText").text(""), "Notes field is missing")
            notesField.text = EDITED_NOTES
            dismissKeyboard(device)
            clickText(device, "Save changes", scroll = true)
            visible(device, "Changes saved.")
            visible(device, EDITED_TITLE)
            visible(device, EDITED_NOTES)
            capture(device, "/data/local/tmp/drop-history-reference-edited.png")

            clickText(device, "Back to History", scroll = true)
            visible(device, EDITED_TITLE)
            val search = objectFor(device, By.clazz("android.widget.EditText"), "History search field is missing")
            search.text = "followupwolf"
            dismissKeyboard(device)
            visible(device, EDITED_TITLE)
            search.text = ""
            dismissKeyboard(device)

            val editedTitle = visible(device, EDITED_TITLE)
            val editedCard = ancestorWithDescendantText(editedTitle, "Delete")
                ?: throw AssertionError("Edited saved reference card must expose Delete")
            tap(device, editedCard.findObject(By.text("Delete")) ?: throw AssertionError("Delete missing"))
            val dialogTitle = visible(device, "Delete saved reference?")
            val dialog = ancestorWithDescendantText(dialogTitle, "Keep reference")
                ?: throw AssertionError("Delete dialog missing")
            tap(device, dialog.findObject(By.text("Delete")) ?: throw AssertionError("Delete confirmation missing"))
        }
    }

    private fun clickText(device: UiDevice, text: String, scroll: Boolean = false) {
        val node = if (scroll) visibleAfterScroll(device, text) else visible(device, text)
        tap(device, node)
        device.waitForIdle()
    }

    private fun clickTextMatching(device: UiDevice, prefix: String) {
        val node = assertNotNull(
            "Expected visible text beginning with: $prefix",
            device.wait(Until.findObject(By.textStartsWith(prefix)), TIMEOUT)
        ).let { device.findObject(By.textStartsWith(prefix)) }
        tap(device, node)
        device.waitForIdle()
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
        throw AssertionError("Expected visible text after scrolling: $text")
    }

    private fun objectFor(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), TIMEOUT)).let { device.findObject(selector) }

    private fun ancestorWithDescendantText(node: UiObject2, text: String): UiObject2? {
        var current: UiObject2? = node
        while (current != null) {
            if (current.findObject(By.text(text)) != null) return current
            current = current.parent
        }
        return null
    }

    private fun tap(device: UiDevice, node: UiObject2) {
        var target: UiObject2? = node
        while (target != null && !target.isClickable) target = target.parent
        target = target ?: node
        val bounds = target.visibleBounds
        assertTrue("Target has no tappable area", !bounds.isEmpty)
        assertTrue("Coordinate tap failed", device.click(bounds.centerX(), bounds.centerY()))
    }

    private fun dismissKeyboard(device: UiDevice) {
        device.executeShellCommand("input keyevent KEYCODE_ESCAPE")
        device.waitForIdle()
    }

    private fun capture(device: UiDevice, path: String) {
        device.waitForIdle()
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        assertTrue(device.executeShellCommand("ls -l $path").contains(path.substringAfterLast('/')))
    }

    private companion object {
        const val ORIGINAL_TITLE = "Field research reference"
        const val EDITED_TITLE = "Wolf field research"
        const val EDITED_NOTES = "followupwolf review notes"
        const val ORIGINAL_CONTENT = "Field research material for product review."
        const val TIMEOUT = 20_000L
        const val SHORT_TIMEOUT = 2_000L
    }
}
''')

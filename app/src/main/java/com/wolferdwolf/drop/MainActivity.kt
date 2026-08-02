package com.wolferdwolf.drop

import android.content.ActivityNotFoundException
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolferdwolf.drop.actions.SuggestedAction
import com.wolferdwolf.drop.actions.SuggestedActionEngine
import com.wolferdwolf.drop.actions.SuggestedActionType
import com.wolferdwolf.drop.data.SavedReference
import com.wolferdwolf.drop.data.SavedReferenceStore
import com.wolferdwolf.drop.extraction.ExtractionResult
import com.wolferdwolf.drop.extraction.ExtractionType
import com.wolferdwolf.drop.extraction.RuleBasedExtractor
import com.wolferdwolf.drop.ocr.ImageOcrProcessor
import com.wolferdwolf.drop.reminder.ReminderActivity
import com.wolferdwolf.drop.reminder.ReminderDisplayFormatter
import com.wolferdwolf.drop.reminder.ReminderHistoryStore
import com.wolferdwolf.drop.reminder.ReminderRecord
import com.wolferdwolf.drop.reminder.ReminderScheduler
import com.wolferdwolf.drop.share.SharedTextParser
import com.wolferdwolf.drop.timetable.TimetableReviewActivity
import com.wolferdwolf.drop.ui.theme.DropTheme

class MainActivity : ComponentActivity() {
    private var sourceText by mutableStateOf<String?>(null)
    private var screen by mutableStateOf(Screen.HOME)
    private var references by mutableStateOf<List<SavedReference>>(emptyList())
    private var reminders by mutableStateOf<List<ReminderRecord>>(emptyList())
    private var actionError by mutableStateOf<String?>(null)
    private var imageStatus by mutableStateOf<String?>(null)
    private lateinit var referenceStore: SavedReferenceStore
    private lateinit var reminderStore: ReminderHistoryStore
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var imagePicker: ActivityResultLauncher<String>
    private lateinit var pdfPicker: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let(::processImage) }
        pdfPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let { uri -> importDocument(uri, "PDF document") } }
        referenceStore = SavedReferenceStore(applicationContext)
        reminderStore = ReminderHistoryStore(applicationContext)
        reminderScheduler = ReminderScheduler(applicationContext)
        refreshHistory()
        sourceText = savedInstanceState?.getString(STATE_TEXT) ?: SharedTextParser.parse(intent)
        screen = savedInstanceState?.getString(STATE_SCREEN)
            ?.let { runCatching { Screen.valueOf(it) }.getOrNull() }
            ?: if (sourceText == null) Screen.HOME else Screen.PREVIEW

        setContent {
            DropTheme {
                val text = sourceText
                val results = text?.let(RuleBasedExtractor::extract).orEmpty()
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        imageStatus = imageStatus,
                        historyCount = references.size + reminders.size,
                        onImage = { imageStatus = "Reading image offline…"; imagePicker.launch("image/*") },
                        onPdf = { pdfPicker.launch("application/pdf") },
                        onText = { screen = Screen.TEXT_ENTRY },
                        onLink = { screen = Screen.LINK_ENTRY },
                        onHistory = { refreshHistory(); screen = Screen.HISTORY }
                    )
                    Screen.HISTORY -> HistoryScreen(
                        references = references,
                        reminders = reminders,
                        onBack = { screen = Screen.HOME },
                        onDeleteReference = { referenceStore.delete(it.id); refreshHistory() },
                        onCancelReminder = { reminder ->
                            reminderScheduler.cancel(reminder).onSuccess {
                                reminderStore.delete(reminder.id)
                                refreshHistory()
                            }
                        }
                    )
                    Screen.TEXT_ENTRY -> EntryScreen("Paste text", false, { screen = Screen.HOME }, ::beginFlow)
                    Screen.LINK_ENTRY -> EntryScreen("Add link", true, { screen = Screen.HOME }, ::beginFlow)
                    Screen.PREVIEW -> if (text == null) reset() else PreviewScreen(text, ::reset) {
                        sourceText = it
                        screen = Screen.EXTRACTION
                    }
                    Screen.EXTRACTION -> if (text == null) reset() else ExtractionScreen(
                        original = text,
                        results = results,
                        onBack = { screen = Screen.PREVIEW },
                        onActions = { screen = Screen.ACTIONS },
                        onDiscard = ::reset
                    )
                    Screen.ACTIONS -> if (text == null) reset() else ActionsScreen(
                        actions = SuggestedActionEngine.suggest(text, results),
                        error = actionError,
                        onBack = { screen = Screen.EXTRACTION },
                        onAction = { execute(it, text, results) }
                    )
                    Screen.SAVE -> if (text == null) reset() else SaveScreen(
                        value = text,
                        suggestedTitle = SavedReferenceStore.defaultTitle(text),
                        onBack = { screen = Screen.ACTIONS },
                        onSave = { title ->
                            runCatching { referenceStore.save(title, text) }
                                .onSuccess { refreshHistory(); reset() }
                                .exceptionOrNull()?.message
                        }
                    )
                    Screen.CHECKLIST -> if (text == null) reset() else ChecklistScreen(
                        value = text,
                        onBack = { screen = Screen.ACTIONS },
                        onSave = { value ->
                            runCatching { referenceStore.save("Checklist", value) }
                                .onSuccess { refreshHistory(); reset() }
                                .exceptionOrNull()?.message
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::referenceStore.isInitialized) refreshHistory()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SharedTextParser.parse(intent)?.let(::beginFlow)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_TEXT, sourceText)
        outState.putString(STATE_SCREEN, screen.name)
        super.onSaveInstanceState(outState)
    }

    private fun processImage(uri: Uri) {
        imageStatus = "Reading image offline…"
        ImageOcrProcessor.process(
            this,
            uri,
            onSuccess = { text ->
                imageStatus = null
                startActivity(Intent(this, TimetableReviewActivity::class.java).putExtra(TimetableReviewActivity.EXTRA_OCR_TEXT, text))
            },
            onFailure = { imageStatus = it }
        )
    }

    private fun execute(action: SuggestedAction, text: String, results: List<ExtractionResult>) {
        actionError = null
        when (action.type) {
            SuggestedActionType.SAVE_REFERENCE -> screen = Screen.SAVE
            SuggestedActionType.REMINDER -> startActivity(Intent(this, ReminderActivity::class.java).putExtra(ReminderActivity.EXTRA_SOURCE_TEXT, text))
            SuggestedActionType.CHECKLIST -> screen = Screen.CHECKLIST
            SuggestedActionType.CALENDAR -> launch(
                Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, SavedReferenceStore.defaultTitle(text))
                    .putExtra(CalendarContract.Events.DESCRIPTION, text)
            )
            SuggestedActionType.CONTACT -> launch(
                Intent(Intent.ACTION_INSERT).setType(ContactsContract.Contacts.CONTENT_TYPE)
                    .putExtra(ContactsContract.Intents.Insert.PHONE, first(results, ExtractionType.PHONE))
                    .putExtra(ContactsContract.Intents.Insert.EMAIL, first(results, ExtractionType.EMAIL))
                    .putExtra(ContactsContract.Intents.Insert.NOTES, text)
            )
            SuggestedActionType.MAPS -> launch(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(text.take(500))}")))
            SuggestedActionType.OPEN_LINK -> first(results, ExtractionType.URL)
                ?.let { launch(Intent(Intent.ACTION_VIEW, Uri.parse(normalizeUrl(it)))) } ?: fail("No link was found.")
            SuggestedActionType.EMAIL -> first(results, ExtractionType.EMAIL)
                ?.let { launch(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(it)}"))) } ?: fail("No email address was found.")
            SuggestedActionType.CALL -> first(results, ExtractionType.PHONE)
                ?.let { launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(it)}"))) } ?: fail("No phone number was found.")
        }
    }

    private fun launch(intent: Intent) {
        try {
            if (intent.resolveActivity(packageManager) == null) fail("No compatible app is installed for this action.") else startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            fail("No compatible app is installed for this action.")
        } catch (_: SecurityException) {
            fail("Android blocked this action. Check permissions and try again.")
        }
    }

    private fun first(results: List<ExtractionResult>, type: ExtractionType) = results.firstOrNull { it.type == type }?.value
    private fun fail(message: String) { actionError = message }
    private fun normalizeUrl(value: String) = if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"

    private fun beginFlow(value: String) {
        val clean = value.trim().take(SharedTextParser.MAX_SHARED_TEXT_LENGTH)
        if (clean.isNotBlank()) {
            sourceText = clean
            screen = Screen.PREVIEW
        }
    }

    private fun importDocument(uri: Uri, label: String) {
        val metadata = metadata(uri)
        beginFlow(buildString {
            appendLine("$label imported")
            appendLine("File: ${metadata.first ?: "Unnamed file"}")
            appendLine("Size: ${metadata.second?.let(::fileSize) ?: "Unknown size"}")
            append("Source: $uri")
        })
    }

    private fun metadata(uri: Uri): Pair<String?, Long?> {
        var name: String? = null
        var size: Long? = null
        val cursor: Cursor? = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = it.getString(nameIndex)
                if (sizeIndex >= 0 && !it.isNull(sizeIndex)) size = it.getLong(sizeIndex)
            }
        }
        return name to size
    }

    private fun fileSize(bytes: Long) = when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes bytes"
    }

    private fun refreshHistory() {
        references = referenceStore.load()
        reminders = reminderStore.load()
    }

    private fun reset() {
        sourceText = null
        actionError = null
        imageStatus = null
        screen = Screen.HOME
    }

    private enum class Screen { HOME, HISTORY, TEXT_ENTRY, LINK_ENTRY, PREVIEW, EXTRACTION, ACTIONS, SAVE, CHECKLIST }

    private companion object {
        const val STATE_TEXT = "source_text"
        const val STATE_SCREEN = "screen"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    imageStatus: String?,
    historyCount: Int,
    onImage: () -> Unit,
    onPdf: () -> Unit,
    onText: () -> Unit,
    onLink: () -> Unit,
    onHistory: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Drop") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("Turn anything into the next useful action", style = MaterialTheme.typography.headlineMedium) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onImage, modifier = Modifier.fillMaxWidth()) { Text("Import screenshot or image") }
                        imageStatus?.let { Text(it, color = if (it.startsWith("Reading")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                        Button(onClick = onPdf, modifier = Modifier.fillMaxWidth()) { Text("Import PDF") }
                        FilledTonalButton(onClick = onText, modifier = Modifier.fillMaxWidth()) { Text("Paste text") }
                        FilledTonalButton(onClick = onLink, modifier = Modifier.fillMaxWidth()) { Text("Add link") }
                        Text("You can also share content to Drop from WhatsApp, Chrome, Gallery, and Files.")
                    }
                }
            }
            item {
                OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) {
                    Text(if (historyCount == 0) "History" else "History ($historyCount)")
                }
            }
            item {
                Text(
                    if (historyCount == 0) "No saved actions yet. Import something to begin."
                    else "$historyCount saved action${if (historyCount == 1) "" else "s"} available in History."
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    references: List<SavedReference>,
    reminders: List<ReminderRecord>,
    onBack: () -> Unit,
    onDeleteReference: (SavedReference) -> Unit,
    onCancelReminder: (ReminderRecord) -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("History") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Text("Saved actions", style = MaterialTheme.typography.headlineSmall) }
            if (references.isEmpty() && reminders.isEmpty()) {
                item { Text("Nothing has been saved yet.") }
            }
            if (reminders.isNotEmpty()) item { Text("Scheduled reminders", style = MaterialTheme.typography.titleLarge) }
            items(reminders, key = ReminderRecord::id) { reminder ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                        Text(ReminderDisplayFormatter.format(reminder.triggerAtMillis))
                        if (reminder.notes.isNotBlank()) Text(reminder.notes, maxLines = 3)
                        TextButton(onClick = { onCancelReminder(reminder) }) { Text("Cancel reminder") }
                    }
                }
            }
            if (references.isNotEmpty()) item { Text("References and checklists", style = MaterialTheme.typography.titleLarge) }
            items(references, key = SavedReference::id) { reference ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(reference.title, style = MaterialTheme.typography.titleMedium)
                        Text(reference.originalText, maxLines = 3)
                        TextButton(onClick = { onDeleteReference(reference) }) { Text("Delete") }
                    }
                }
            }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to Home") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntryScreen(title: String, singleLine: Boolean, onBack: () -> Unit, onContinue: (String) -> Unit) {
    var value by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Add content for Drop to understand and turn into an action.") }
            item {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it.take(SharedTextParser.MAX_SHARED_TEXT_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (singleLine) "Website link" else "Content") },
                    singleLine = singleLine,
                    minLines = if (singleLine) 1 else 8
                )
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth(0.48f)) { Text("Back") }
                    Button(onClick = { onContinue(value) }, enabled = value.isNotBlank()) { Text("Continue") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreviewScreen(value: String, onDiscard: () -> Unit, onContinue: (String) -> Unit) {
    var editable by rememberSaveable(value) { mutableStateOf(value) }
    Scaffold(topBar = { TopAppBar(title = { Text("Import preview") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Review before processing", style = MaterialTheme.typography.headlineSmall) }
            item { OutlinedTextField(editable, { editable = it.take(SharedTextParser.MAX_SHARED_TEXT_LENGTH) }, Modifier.fillMaxWidth(), label = { Text("Imported content") }, minLines = 10) }
            item { Button(onClick = { onContinue(editable.trim()) }, enabled = editable.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Extract details") } }
            item { OutlinedButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) { Text("Discard") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtractionScreen(original: String, results: List<ExtractionResult>, onBack: () -> Unit, onActions: () -> Unit, onDiscard: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Extracted information") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(if (results.isEmpty()) "Nothing specific was detected" else "${results.size} useful details found", style = MaterialTheme.typography.headlineSmall) }
            if (results.isEmpty()) item { Text("Drop can still save this content, create a reminder, or build a checklist.") }
            else items(results, key = { "${it.type}-${it.sourceStart}-${it.value}" }) { result ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(result.type.label(), style = MaterialTheme.typography.labelLarge)
                        Text(result.value, style = MaterialTheme.typography.titleMedium)
                        Text("Confidence ${(result.confidence * 100).toInt()}%")
                    }
                }
            }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Original content", style = MaterialTheme.typography.titleMedium); Text(original) } } }
            item { Button(onClick = onActions, modifier = Modifier.fillMaxWidth()) { Text("See suggested actions") } }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Edit imported content") } }
            item { TextButton(onClick = onDiscard, modifier = Modifier.fillMaxWidth()) { Text("Discard") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionsScreen(actions: List<SuggestedAction>, error: String?, onBack: () -> Unit, onAction: (SuggestedAction) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Suggested actions") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Choose what Drop should do next", style = MaterialTheme.typography.headlineSmall) }
            item { Text("Nothing happens until you choose and confirm an action.") }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            items(actions, key = { it.type.name }) { action ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(action.title, style = MaterialTheme.typography.titleMedium)
                        Text(action.reason)
                        Button(onClick = { onAction(action) }, modifier = Modifier.fillMaxWidth()) { Text(action.title) }
                    }
                }
            }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SaveScreen(value: String, suggestedTitle: String, onBack: () -> Unit, onSave: (String) -> String?) {
    var title by rememberSaveable { mutableStateOf(suggestedTitle) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Save reference") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Confirm before saving", style = MaterialTheme.typography.headlineSmall) }
            item { OutlinedTextField(title, { title = it.take(SavedReferenceStore.MAX_TITLE_LENGTH) }, Modifier.fillMaxWidth(), label = { Text("Title") }) }
            item { Card(Modifier.fillMaxWidth()) { Text(value, Modifier.padding(16.dp)) } }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item { Button(onClick = { error = onSave(title) }, modifier = Modifier.fillMaxWidth()) { Text("Save") } }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChecklistScreen(value: String, onBack: () -> Unit, onSave: (String) -> String?) {
    val suggested = value.lineSequence().map(String::trim).filter(String::isNotBlank).joinToString("\n") { "☐ ${it.trimStart('-', '•', ' ')}" }
    var checklist by rememberSaveable { mutableStateOf(suggested) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Create checklist") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Edit the checklist before saving", style = MaterialTheme.typography.headlineSmall) }
            item { OutlinedTextField(checklist, { checklist = it }, Modifier.fillMaxWidth(), label = { Text("Checklist items") }, minLines = 10) }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item { Button(onClick = { error = onSave(checklist.trim()) }, enabled = checklist.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save checklist") } }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") } }
        }
    }
}

private fun ExtractionType.label() = when (this) {
    ExtractionType.PHONE -> "Phone number"
    ExtractionType.EMAIL -> "Email"
    ExtractionType.URL -> "Web link"
    ExtractionType.DATE -> "Date"
    ExtractionType.TIME -> "Time"
}

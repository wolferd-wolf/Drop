package com.wolferdwolf.drop

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.wolferdwolf.drop.calendar.CalendarConfirmationActivity
import com.wolferdwolf.drop.call.CallConfirmationActivity
import com.wolferdwolf.drop.contact.ContactConfirmationActivity
import com.wolferdwolf.drop.data.SavedReference
import com.wolferdwolf.drop.data.SavedReferenceStore
import com.wolferdwolf.drop.email.EmailConfirmationActivity
import com.wolferdwolf.drop.extraction.EditableExtractionResults
import com.wolferdwolf.drop.extraction.EditableExtractionState
import com.wolferdwolf.drop.extraction.ExtractionResult
import com.wolferdwolf.drop.extraction.ExtractionType
import com.wolferdwolf.drop.extraction.RuleBasedExtractor
import com.wolferdwolf.drop.link.OpenLinkConfirmationActivity
import com.wolferdwolf.drop.maps.MapConfirmationActivity
import com.wolferdwolf.drop.ocr.ImageOcrProcessor
import com.wolferdwolf.drop.pdf.PdfImportActivity
import com.wolferdwolf.drop.reminder.ReminderActivity
import com.wolferdwolf.drop.reminder.ReminderDisplayFormatter
import com.wolferdwolf.drop.reminder.ReminderHistoryStore
import com.wolferdwolf.drop.reminder.ReminderRecord
import com.wolferdwolf.drop.reminder.ReminderScheduler
import com.wolferdwolf.drop.share.SharedTextParser
import com.wolferdwolf.drop.ui.theme.DropTheme

class MainActivity : ComponentActivity() {
    private var sourceText by mutableStateOf<String?>(null)
    private var editedResults by mutableStateOf<List<ExtractionResult>?>(null)
    private var screen by mutableStateOf(Screen.HOME)
    private var references by mutableStateOf<List<SavedReference>>(emptyList())
    private var reminders by mutableStateOf<List<ReminderRecord>>(emptyList())
    private var actionError by mutableStateOf<String?>(null)
    private var importStatus by mutableStateOf<String?>(null)
    private lateinit var referenceStore: SavedReferenceStore
    private lateinit var reminderStore: ReminderHistoryStore
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var imagePicker: ActivityResultLauncher<String>
    private lateinit var pdfPicker: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { it?.let(::processImage) }
        pdfPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                importStatus = null
                startActivity(Intent(this, PdfImportActivity::class.java).setData(it).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
            }
        }
        referenceStore = SavedReferenceStore(applicationContext)
        reminderStore = ReminderHistoryStore(applicationContext)
        reminderScheduler = ReminderScheduler(applicationContext)
        refreshHistory()
        sourceText = savedInstanceState?.getString(STATE_TEXT) ?: SharedTextParser.parse(intent)
        editedResults = EditableExtractionState.decode(
            savedInstanceState?.getBoolean(STATE_HAS_EDITED_RESULTS, false) == true,
            savedInstanceState?.getStringArrayList(STATE_EDITED_RESULTS)
        )
        screen = savedInstanceState?.getString(STATE_SCREEN)?.let { runCatching { Screen.valueOf(it) }.getOrNull() }
            ?: if (sourceText == null) Screen.HOME else Screen.PREVIEW

        setContent {
            DropTheme {
                val text = sourceText
                val detectedResults = text?.let(RuleBasedExtractor::extract).orEmpty()
                val results = editedResults ?: detectedResults
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        importStatus,
                        references.size + reminders.size,
                        { importStatus = "Reading image offline…"; imagePicker.launch("image/*") },
                        { importStatus = "Select a PDF to read offline"; pdfPicker.launch("application/pdf") },
                        { screen = Screen.TEXT_ENTRY },
                        { screen = Screen.LINK_ENTRY },
                        { refreshHistory(); screen = Screen.HISTORY }
                    )
                    Screen.HISTORY -> HistoryScreen(
                        references,
                        reminders,
                        { screen = Screen.HOME },
                        { referenceStore.delete(it.id); refreshHistory() },
                        { reminder -> reminderScheduler.cancel(reminder).onSuccess { reminderStore.delete(reminder.id); refreshHistory() } }
                    )
                    Screen.TEXT_ENTRY -> EntryScreen("Paste text", false, { screen = Screen.HOME }, ::beginFlow)
                    Screen.LINK_ENTRY -> EntryScreen("Add link", true, { screen = Screen.HOME }, ::beginFlow)
                    Screen.PREVIEW -> if (text == null) reset() else PreviewScreen(text, ::reset) {
                        sourceText = it
                        editedResults = null
                        screen = Screen.EXTRACTION
                    }
                    Screen.EXTRACTION -> if (text == null) reset() else ExtractionScreen(
                        text,
                        results,
                        { screen = Screen.PREVIEW },
                        { target, value -> editedResults = EditableExtractionResults.update(results, target, value) },
                        { target -> editedResults = EditableExtractionResults.remove(results, target) },
                        { screen = Screen.ACTIONS },
                        ::reset
                    )
                    Screen.ACTIONS -> if (text == null) reset() else ActionsScreen(
                        SuggestedActionEngine.suggest(text, results),
                        actionError,
                        { screen = Screen.EXTRACTION },
                        { screen = Screen.ALL_ACTIONS },
                        { execute(it, text, results) }
                    )
                    Screen.ALL_ACTIONS -> if (text == null) reset() else AllActionsScreen(
                        SuggestedActionEngine.manualActions(results),
                        { screen = Screen.ACTIONS },
                        { execute(it, text, results) }
                    )
                    Screen.SAVE -> if (text == null) reset() else SaveScreen(
                        text,
                        SavedReferenceStore.defaultTitle(text),
                        { screen = Screen.ACTIONS }
                    ) { title ->
                        runCatching { referenceStore.save(title, text) }
                            .onSuccess { refreshHistory(); reset() }
                            .exceptionOrNull()?.message
                    }
                    Screen.CHECKLIST -> if (text == null) reset() else ChecklistScreen(
                        text,
                        { screen = Screen.ACTIONS }
                    ) { value ->
                        runCatching { referenceStore.save("Checklist", value) }
                            .onSuccess { refreshHistory(); reset() }
                            .exceptionOrNull()?.message
                    }
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
        outState.putBoolean(STATE_HAS_EDITED_RESULTS, editedResults != null)
        outState.putStringArrayList(STATE_EDITED_RESULTS, EditableExtractionState.encode(editedResults))
        super.onSaveInstanceState(outState)
    }

    private fun processImage(uri: Uri) {
        importStatus = "Reading image offline…"
        ImageOcrProcessor.process(
            this,
            uri,
            onSuccess = { text ->
                importStatus = null
                beginFlow(text)
            },
            onFailure = { importStatus = it }
        )
    }

    private fun execute(action: SuggestedAction, text: String, results: List<ExtractionResult>) {
        actionError = null
        when (action.type) {
            SuggestedActionType.SAVE_REFERENCE -> screen = Screen.SAVE
            SuggestedActionType.REMINDER -> startActivity(Intent(this, ReminderActivity::class.java).putExtra(ReminderActivity.EXTRA_SOURCE_TEXT, text))
            SuggestedActionType.CHECKLIST -> screen = Screen.CHECKLIST
            SuggestedActionType.CALENDAR -> startActivity(
                Intent(this, CalendarConfirmationActivity::class.java)
                    .putExtra(CalendarConfirmationActivity.EXTRA_SOURCE_TEXT, text)
                    .putExtra(CalendarConfirmationActivity.EXTRA_HAS_CURATED_RESULTS, true)
                    .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_DATE, first(results, ExtractionType.DATE).orEmpty())
                    .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_TIME, first(results, ExtractionType.TIME).orEmpty())
                    .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_VENUE, first(results, ExtractionType.ADDRESS).orEmpty())
            )
            SuggestedActionType.CONTACT -> startActivity(
                Intent(this, ContactConfirmationActivity::class.java)
                    .putExtra(ContactConfirmationActivity.EXTRA_SOURCE_TEXT, text)
            )
            SuggestedActionType.MAPS -> startActivity(
                Intent(this, MapConfirmationActivity::class.java)
                    .putExtra(MapConfirmationActivity.EXTRA_SOURCE_TEXT, text)
            )
            SuggestedActionType.OPEN_LINK -> first(results, ExtractionType.URL)
                ?.let {
                    startActivity(
                        Intent(this, OpenLinkConfirmationActivity::class.java)
                            .putExtra(OpenLinkConfirmationActivity.EXTRA_URL, it)
                    )
                } ?: fail("No link was found.")
            SuggestedActionType.EMAIL -> startActivity(
                Intent(this, EmailConfirmationActivity::class.java)
                    .putExtra(EmailConfirmationActivity.EXTRA_SOURCE_TEXT, text)
            )
            SuggestedActionType.CALL -> first(results, ExtractionType.PHONE)
                ?.let {
                    startActivity(
                        Intent(this, CallConfirmationActivity::class.java)
                            .putExtra(CallConfirmationActivity.EXTRA_PHONE, it)
                    )
                } ?: fail("No phone number was found.")
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

    private fun beginFlow(value: String) {
        val clean = value.trim().take(SharedTextParser.MAX_SHARED_TEXT_LENGTH)
        if (clean.isNotBlank()) {
            sourceText = clean
            editedResults = null
            screen = Screen.PREVIEW
        }
    }

    private fun refreshHistory() {
        references = referenceStore.load()
        reminders = reminderStore.load()
    }

    private fun reset() {
        sourceText = null
        editedResults = null
        actionError = null
        importStatus = null
        screen = Screen.HOME
    }

    private enum class Screen { HOME, HISTORY, TEXT_ENTRY, LINK_ENTRY, PREVIEW, EXTRACTION, ACTIONS, ALL_ACTIONS, SAVE, CHECKLIST }

    private companion object {
        const val STATE_TEXT = "source_text"
        const val STATE_SCREEN = "screen"
        const val STATE_HAS_EDITED_RESULTS = "has_edited_results"
        const val STATE_EDITED_RESULTS = "edited_results"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    importStatus: String?,
    historyCount: Int,
    onImage: () -> Unit,
    onPdf: () -> Unit,
    onText: () -> Unit,
    onLink: () -> Unit,
    onHistory: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Drop") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Turn anything into the next useful action", style = MaterialTheme.typography.headlineMedium) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onImage, modifier = Modifier.fillMaxWidth()) { Text("Import screenshot or image") }
                        Button(onClick = onPdf, modifier = Modifier.fillMaxWidth()) { Text("Import PDF") }
                        importStatus?.let { Text(it, color = if (it.contains("offline", true) || it.startsWith("Select")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
                        FilledTonalButton(onClick = onText, modifier = Modifier.fillMaxWidth()) { Text("Paste text") }
                        FilledTonalButton(onClick = onLink, modifier = Modifier.fillMaxWidth()) { Text("Add link") }
                        Text("You can also share content to Drop from WhatsApp, Chrome, Gallery, and Files.")
                    }
                }
            }
            item { OutlinedButton(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text(if (historyCount == 0) "History" else "History ($historyCount)") } }
            item { Text(if (historyCount == 0) "No saved actions yet. Import something to begin." else "$historyCount saved action${if (historyCount == 1) "" else "s"} available in History.") }
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
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Text("Saved actions", style = MaterialTheme.typography.headlineSmall) }
            if (references.isEmpty() && reminders.isEmpty()) item { Text("Nothing has been saved yet.") }
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
            item { OutlinedTextField(value, { value = it.take(SharedTextParser.MAX_SHARED_TEXT_LENGTH) }, Modifier.fillMaxWidth(), label = { Text(if (singleLine) "Website link" else "Content") }, singleLine = singleLine, minLines = if (singleLine) 1 else 8) }
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
private fun ExtractionScreen(
    original: String,
    results: List<ExtractionResult>,
    onBack: () -> Unit,
    onEdit: (ExtractionResult, String) -> Unit,
    onRemove: (ExtractionResult) -> Unit,
    onActions: () -> Unit,
    onDiscard: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Extracted information") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(
                    when (results.size) {
                        0 -> "Nothing specific was detected"
                        1 -> "1 useful detail found"
                        else -> "${results.size} useful details found"
                    },
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            if (results.isEmpty()) item { Text("Drop can still save this content, create a reminder, or build a checklist.") }
            else items(results, key = { "${it.type}-${it.sourceStart}" }) { result ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(result.type.label(), style = MaterialTheme.typography.labelLarge)
                        OutlinedTextField(
                            value = result.value,
                            onValueChange = { onEdit(result, it) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Edit ${result.type.label()}") },
                            supportingText = { Text("Your edits are used for Suggested Actions.") },
                            singleLine = true
                        )
                        Text("Confidence ${(result.confidence * 100).toInt()}%")
                        TextButton(onClick = { onRemove(result) }) { Text("Remove ${result.type.label()}") }
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
private fun ActionsScreen(
    actions: List<SuggestedAction>,
    error: String?,
    onBack: () -> Unit,
    onChooseAnother: () -> Unit,
    onAction: (SuggestedAction) -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Suggested actions") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("Choose what Drop should do next", style = MaterialTheme.typography.headlineSmall) }
            item { Text("Nothing happens until you choose and confirm an action.") }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            items(actions, key = { it.type.name }) { action -> ActionCard(action, onAction) }
            item { OutlinedButton(onClick = onChooseAnother, modifier = Modifier.fillMaxWidth()) { Text("Choose another action") } }
            item { Text("Only actions with the required detected data are available.") }
            item { TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllActionsScreen(actions: List<SuggestedAction>, onBack: () -> Unit, onAction: (SuggestedAction) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Choose another action") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text("All available actions", style = MaterialTheme.typography.headlineSmall) }
            item { Text("Manual actions let you fill in missing details. Data-dependent actions only appear when Drop detected what they need.") }
            items(actions, key = { it.type.name }) { action -> ActionCard(action, onAction) }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to suggestions") } }
        }
    }
}

@Composable
private fun ActionCard(action: SuggestedAction, onAction: (SuggestedAction) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(action.title, style = MaterialTheme.typography.titleMedium)
            Text(action.reason)
            Button(onClick = { onAction(action) }, modifier = Modifier.fillMaxWidth()) { Text(action.title) }
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
    ExtractionType.PRICE -> "Price"
}

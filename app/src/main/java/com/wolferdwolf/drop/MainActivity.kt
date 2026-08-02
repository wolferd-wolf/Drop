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
import androidx.compose.foundation.layout.weight
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
import com.wolferdwolf.drop.reminder.ReminderActivity
import com.wolferdwolf.drop.reminder.ReminderDisplayFormatter
import com.wolferdwolf.drop.reminder.ReminderHistoryStore
import com.wolferdwolf.drop.reminder.ReminderRecord
import com.wolferdwolf.drop.reminder.ReminderScheduler
import com.wolferdwolf.drop.share.SharedTextParser
import com.wolferdwolf.drop.ui.theme.DropTheme

class MainActivity : ComponentActivity() {
    private var sharedText by mutableStateOf<String?>(null)
    private var screen by mutableStateOf(Screen.HOME)
    private var savedReferences by mutableStateOf<List<SavedReference>>(emptyList())
    private var reminders by mutableStateOf<List<ReminderRecord>>(emptyList())
    private var actionError by mutableStateOf<String?>(null)
    private lateinit var referenceStore: SavedReferenceStore
    private lateinit var reminderStore: ReminderHistoryStore
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var imagePicker: ActivityResultLauncher<String>
    private lateinit var pdfPicker: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { openPickedDocument(it, "Image or screenshot") }
        }
        pdfPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { openPickedDocument(it, "PDF document") }
        }
        referenceStore = SavedReferenceStore(applicationContext)
        reminderStore = ReminderHistoryStore(applicationContext)
        reminderScheduler = ReminderScheduler(applicationContext)
        refreshHistory()
        sharedText = savedInstanceState?.getString(STATE_SHARED_TEXT) ?: SharedTextParser.parse(intent)
        screen = savedInstanceState?.getString(STATE_SCREEN)
            ?.let { runCatching { Screen.valueOf(it) }.getOrNull() }
            ?: if (sharedText == null) Screen.HOME else Screen.PREVIEW

        setContent {
            DropTheme {
                val currentText = sharedText
                val results = currentText?.let(RuleBasedExtractor::extract).orEmpty()
                when (screen) {
                    Screen.HOME -> DropHomeScreen(
                        savedReferences = savedReferences,
                        reminders = reminders,
                        onImportImage = { imagePicker.launch("image/*") },
                        onImportPdf = { pdfPicker.launch("application/pdf") },
                        onPasteText = { screen = Screen.PASTE_TEXT },
                        onAddLink = { screen = Screen.ADD_LINK },
                        onDeleteReference = { reference -> referenceStore.delete(reference.id); refreshHistory() },
                        onDeleteReminder = { reminder ->
                            reminderScheduler.cancel(reminder).onSuccess {
                                reminderStore.delete(reminder.id)
                                refreshHistory()
                            }
                        }
                    )
                    Screen.PASTE_TEXT -> ManualEntryScreen(
                        title = "Paste text",
                        label = "Text to process",
                        hint = "Paste a message, advertisement, address, deadline, or any useful text.",
                        singleLine = false,
                        onBack = { screen = Screen.HOME },
                        onContinue = ::startTextFlow
                    )
                    Screen.ADD_LINK -> ManualEntryScreen(
                        title = "Add link",
                        label = "Website link",
                        hint = "Paste a complete link such as https://example.com",
                        singleLine = true,
                        onBack = { screen = Screen.HOME },
                        onContinue = ::startTextFlow
                    )
                    Screen.PREVIEW -> if (currentText == null) clearFlow() else SharedTextPreview(
                        initialText = currentText,
                        onDiscard = ::clearFlow,
                        onContinue = { edited -> sharedText = edited; screen = Screen.EXTRACTION }
                    )
                    Screen.EXTRACTION -> if (currentText == null) clearFlow() else ExtractedInformationScreen(
                        originalText = currentText,
                        results = results,
                        onBack = { screen = Screen.PREVIEW },
                        onContinue = { actionError = null; screen = Screen.ACTIONS },
                        onDiscard = ::clearFlow
                    )
                    Screen.ACTIONS -> if (currentText == null) clearFlow() else SuggestedActionsScreen(
                        actions = SuggestedActionEngine.suggest(currentText, results),
                        error = actionError,
                        onBack = { screen = Screen.EXTRACTION },
                        onAction = { action -> executeAction(action, currentText, results) }
                    )
                    Screen.SAVE_REFERENCE -> if (currentText == null) clearFlow() else SaveReferenceScreen(
                        originalText = currentText,
                        initialTitle = SavedReferenceStore.defaultTitle(currentText),
                        onBack = { screen = Screen.ACTIONS },
                        onSave = { title ->
                            runCatching { referenceStore.save(title, currentText) }
                                .onSuccess { refreshHistory(); clearFlow() }
                                .exceptionOrNull()?.message
                        }
                    )
                    Screen.CHECKLIST -> if (currentText == null) clearFlow() else ChecklistScreen(
                        sourceText = currentText,
                        onBack = { screen = Screen.ACTIONS },
                        onSave = { checklist ->
                            runCatching { referenceStore.save("Checklist", checklist) }
                                .onSuccess { refreshHistory(); clearFlow() }
                                .exceptionOrNull()?.message
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::referenceStore.isInitialized && ::reminderStore.isInitialized) refreshHistory()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SharedTextParser.parse(intent)?.let(::startTextFlow)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SHARED_TEXT, sharedText)
        outState.putString(STATE_SCREEN, screen.name)
        super.onSaveInstanceState(outState)
    }

    private fun executeAction(action: SuggestedAction, text: String, results: List<ExtractionResult>) {
        actionError = null
        when (action.type) {
            SuggestedActionType.SAVE_REFERENCE -> screen = Screen.SAVE_REFERENCE
            SuggestedActionType.REMINDER -> startActivity(
                Intent(this, ReminderActivity::class.java)
                    .putExtra(ReminderActivity.EXTRA_SOURCE_TEXT, text)
            )
            SuggestedActionType.CHECKLIST -> screen = Screen.CHECKLIST
            SuggestedActionType.CALENDAR -> launchExternal(
                Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE, SavedReferenceStore.defaultTitle(text))
                    .putExtra(CalendarContract.Events.DESCRIPTION, text)
            )
            SuggestedActionType.CONTACT -> {
                val phone = results.firstOrNull { it.type == ExtractionType.PHONE }?.value
                val email = results.firstOrNull { it.type == ExtractionType.EMAIL }?.value
                launchExternal(
                    Intent(Intent.ACTION_INSERT).setType(ContactsContract.Contacts.CONTENT_TYPE)
                        .putExtra(ContactsContract.Intents.Insert.PHONE, phone)
                        .putExtra(ContactsContract.Intents.Insert.EMAIL, email)
                        .putExtra(ContactsContract.Intents.Insert.NOTES, text)
                )
            }
            SuggestedActionType.MAPS -> launchExternal(
                Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(text.take(500))}"))
            )
            SuggestedActionType.OPEN_LINK -> results.firstOrNull { it.type == ExtractionType.URL }
                ?.value?.let { launchExternal(Intent(Intent.ACTION_VIEW, Uri.parse(normalizeUrl(it)))) }
                ?: setMissingData("No link was found.")
            SuggestedActionType.EMAIL -> results.firstOrNull { it.type == ExtractionType.EMAIL }
                ?.value?.let { launchExternal(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(it)}"))) }
                ?: setMissingData("No email address was found.")
            SuggestedActionType.CALL -> results.firstOrNull { it.type == ExtractionType.PHONE }
                ?.value?.let { launchExternal(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(it)}"))) }
                ?: setMissingData("No phone number was found.")
        }
    }

    private fun launchExternal(intent: Intent) {
        try {
            if (intent.resolveActivity(packageManager) == null) {
                actionError = "No compatible app is installed for this action."
            } else {
                startActivity(intent)
            }
        } catch (_: ActivityNotFoundException) {
            actionError = "No compatible app is installed for this action."
        } catch (_: SecurityException) {
            actionError = "Android blocked this action. Check app permissions and try again."
        }
    }

    private fun setMissingData(message: String) { actionError = message }

    private fun normalizeUrl(value: String): String =
        if (value.startsWith("http://") || value.startsWith("https://")) value else "https://$value"

    private fun startTextFlow(text: String) {
        val clean = text.trim().take(SharedTextParser.MAX_SHARED_TEXT_LENGTH)
        if (clean.isNotBlank()) {
            sharedText = clean
            screen = Screen.PREVIEW
        }
    }

    private fun openPickedDocument(uri: Uri, typeLabel: String) {
        val metadata = queryMetadata(uri)
        val sizeText = metadata.second?.let(::formatFileSize) ?: "Unknown size"
        startTextFlow(buildString {
            appendLine("$typeLabel imported")
            appendLine("File: ${metadata.first ?: "Unnamed file"}")
            appendLine("Size: $sizeText")
            append("Source: $uri")
        })
    }

    private fun queryMetadata(uri: Uri): Pair<String?, Long?> {
        var name: String? = null
        var size: Long? = null
        val cursor: Cursor? = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )
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

    private fun formatFileSize(bytes: Long): String = when {
        bytes >= 1_048_576 -> "%.1f MB".format(bytes / 1_048_576.0)
        bytes >= 1_024 -> "%.1f KB".format(bytes / 1_024.0)
        else -> "$bytes bytes"
    }

    private fun refreshHistory() {
        savedReferences = referenceStore.load()
        reminders = reminderStore.load()
    }

    private fun clearFlow() { sharedText = null; actionError = null; screen = Screen.HOME }

    private enum class Screen {
        HOME, PASTE_TEXT, ADD_LINK, PREVIEW, EXTRACTION, ACTIONS, SAVE_REFERENCE, CHECKLIST
    }

    private companion object {
        const val STATE_SHARED_TEXT = "shared_text"
        const val STATE_SCREEN = "screen"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    title: String,
    label: String,
    hint: String,
    singleLine: Boolean,
    onBack: () -> Unit,
    onContinue: (String) -> Unit
) {
    var text by rememberSaveable { mutableStateOf("") }
    Scaffold(topBar = { TopAppBar(title = { Text(title) }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(hint, style = MaterialTheme.typography.bodyLarge)
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(SharedTextParser.MAX_SHARED_TEXT_LENGTH) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text(label) },
                singleLine = singleLine
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = { onContinue(text) },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Continue") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTextPreview(initialText: String, onDiscard: () -> Unit, onContinue: (String) -> Unit) {
    var editableText by rememberSaveable(initialText) { mutableStateOf(initialText) }
    Scaffold(topBar = { TopAppBar(title = { Text("Import preview") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Review before processing", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = editableText,
                onValueChange = { editableText = it.take(SharedTextParser.MAX_SHARED_TEXT_LENGTH) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Imported content") },
                supportingText = { Text("Edit anything that is incorrect or unnecessary") }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
                Button(
                    onClick = { onContinue(editableText.trim()) },
                    enabled = editableText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Extract details") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtractedInformationScreen(
    originalText: String,
    results: List<ExtractionResult>,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onDiscard: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Extracted information") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    if (results.isEmpty()) "Nothing specific was detected" else "${results.size} useful detail${if (results.size == 1) "" else "s"} found",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            if (results.isEmpty()) item {
                Text("Drop can still save this content, create a manual reminder, or build a checklist.")
            } else items(results, key = { "${it.type}-${it.sourceStart}-${it.value}" }) { ExtractionCard(it) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Original content", style = MaterialTheme.typography.titleMedium)
                        Text(originalText)
                    }
                }
            }
            item { Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("See suggested actions") } }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Edit") }
                    TextButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestedActionsScreen(
    actions: List<SuggestedAction>,
    error: String?,
    onBack: () -> Unit,
    onAction: (SuggestedAction) -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Suggested actions") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Choose what Drop should do next", style = MaterialTheme.typography.headlineSmall)
            }
            item {
                Text("Suggestions are based on the details found. Nothing happens until you confirm an action.")
            }
            error?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            items(actions, key = { it.type.name }) { action ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(action.title, style = MaterialTheme.typography.titleMedium)
                        Text(action.reason, style = MaterialTheme.typography.bodyMedium)
                        Button(onClick = { onAction(action) }, modifier = Modifier.fillMaxWidth()) {
                            Text(action.title)
                        }
                    }
                }
            }
            item { OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to extracted details") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistScreen(sourceText: String, onBack: () -> Unit, onSave: (String) -> String?) {
    val initial = sourceText.lineSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .joinToString("\n") { line -> if (line.startsWith("☐")) line else "☐ ${line.trimStart('-', '•', ' ')}" }
    var checklist by rememberSaveable { mutableStateOf(initial) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Create checklist") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Edit the checklist before saving", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = checklist,
                onValueChange = { checklist = it },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Checklist items") }
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = { error = onSave(checklist.trim()) },
                    enabled = checklist.lineSequence().any { it.isNotBlank() },
                    modifier = Modifier.weight(1f)
                ) { Text("Save checklist") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveReferenceScreen(
    originalText: String,
    initialTitle: String,
    onBack: () -> Unit,
    onSave: (String) -> String?
) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Save reference") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Confirm before saving", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(
                value = title,
                onValueChange = { title = it.take(SavedReferenceStore.MAX_TITLE_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                supportingText = { Text("${title.length}/${SavedReferenceStore.MAX_TITLE_LENGTH}") }
            )
            Card(Modifier.fillMaxWidth().weight(1f)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Content", style = MaterialTheme.typography.titleMedium)
                    Text(originalText)
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(
                    onClick = { error = onSave(title) },
                    enabled = originalText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Save") }
            }
        }
    }
}

@Composable
private fun ExtractionCard(result: ExtractionResult) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(result.type.displayName(), style = MaterialTheme.typography.labelLarge)
            Text(result.value, style = MaterialTheme.typography.titleMedium)
            Text("Confidence ${(result.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun ExtractionType.displayName(): String = when (this) {
    ExtractionType.PHONE -> "Phone number"
    ExtractionType.EMAIL -> "Email"
    ExtractionType.URL -> "Web link"
    ExtractionType.DATE -> "Date"
    ExtractionType.TIME -> "Time"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropHomeScreen(
    savedReferences: List<SavedReference>,
    reminders: List<ReminderRecord>,
    onImportImage: () -> Unit,
    onImportPdf: () -> Unit,
    onPasteText: () -> Unit,
    onAddLink: () -> Unit,
    onDeleteReference: (SavedReference) -> Unit,
    onDeleteReminder: (ReminderRecord) -> Unit
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
                        Button(onClick = onImportImage, modifier = Modifier.fillMaxWidth()) { Text("Import screenshot or image") }
                        Button(onClick = onImportPdf, modifier = Modifier.fillMaxWidth()) { Text("Import PDF") }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = onPasteText, modifier = Modifier.weight(1f)) { Text("Paste text") }
                            FilledTonalButton(onClick = onAddLink, modifier = Modifier.weight(1f)) { Text("Add link") }
                        }
                        Text(
                            "You can also share content directly to Drop from WhatsApp, Chrome, Gallery, Files, and other Android apps.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item { Text("Scheduled reminders", style = MaterialTheme.typography.titleLarge) }
            if (reminders.isEmpty()) item { Text("No scheduled reminders yet") }
            else items(reminders, key = ReminderRecord::id) { reminder ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                        Text(ReminderDisplayFormatter.format(reminder.triggerAtMillis), style = MaterialTheme.typography.labelLarge)
                        if (reminder.notes.isNotBlank()) Text(reminder.notes, maxLines = 3)
                        TextButton(onClick = { onDeleteReminder(reminder) }) { Text("Cancel reminder") }
                    }
                }
            }
            item { Text("Saved references and checklists", style = MaterialTheme.typography.titleLarge) }
            if (savedReferences.isEmpty()) item { Text("No saved items yet") }
            else items(savedReferences, key = SavedReference::id) { reference ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(reference.title, style = MaterialTheme.typography.titleMedium)
                        Text(reference.originalText, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onDeleteReference(reference) }) { Text("Delete") }
                    }
                }
            }
        }
    }
}

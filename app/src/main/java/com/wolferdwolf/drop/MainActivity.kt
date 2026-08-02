package com.wolferdwolf.drop

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
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
                when (screen) {
                    Screen.HOME -> DropHomeScreen(
                        savedReferences = savedReferences,
                        reminders = reminders,
                        onImportImage = { imagePicker.launch("image/*") },
                        onImportPdf = { pdfPicker.launch("application/pdf") },
                        onPasteText = { screen = Screen.PASTE_TEXT },
                        onAddLink = { screen = Screen.ADD_LINK },
                        onDeleteReference = { reference ->
                            referenceStore.delete(reference.id)
                            refreshHistory()
                        },
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
                        results = RuleBasedExtractor.extract(currentText),
                        onBack = { screen = Screen.PREVIEW },
                        onSaveReference = { screen = Screen.SAVE_REFERENCE },
                        onCreateReminder = {
                            startActivity(Intent(this, ReminderActivity::class.java)
                                .putExtra(ReminderActivity.EXTRA_SOURCE_TEXT, currentText))
                        },
                        onDiscard = ::clearFlow
                    )
                    Screen.SAVE_REFERENCE -> if (currentText == null) clearFlow() else SaveReferenceScreen(
                        originalText = currentText,
                        initialTitle = SavedReferenceStore.defaultTitle(currentText),
                        onBack = { screen = Screen.EXTRACTION },
                        onSave = { title ->
                            runCatching { referenceStore.save(title, currentText) }
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
        startTextFlow(
            buildString {
                appendLine("$typeLabel imported")
                appendLine("File: ${metadata.first ?: "Unnamed file"}")
                appendLine("Size: $sizeText")
                append("Source: $uri")
            }
        )
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

    private fun clearFlow() { sharedText = null; screen = Screen.HOME }
    private enum class Screen { HOME, PASTE_TEXT, ADD_LINK, PREVIEW, EXTRACTION, SAVE_REFERENCE }
    private companion object { const val STATE_SHARED_TEXT = "shared_text"; const val STATE_SCREEN = "screen" }
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
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
                Button(onClick = { onContinue(editableText.trim()) }, enabled = editableText.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Extract details") }
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
    onSaveReference: () -> Unit,
    onCreateReminder: () -> Unit,
    onDiscard: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Extracted information") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(if (results.isEmpty()) "Nothing actionable was found" else "${results.size} useful detail${if (results.size == 1) "" else "s"} found", style = MaterialTheme.typography.headlineSmall) }
            if (results.isEmpty()) item { Text("You can still save the original content or create a reminder manually.") }
            else items(results, key = { "${it.type}-${it.sourceStart}-${it.value}" }) { ExtractionCard(it) }
            item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Original content", style = MaterialTheme.typography.titleMedium); Text(originalText) } } }
            item { Button(onClick = onCreateReminder, modifier = Modifier.fillMaxWidth()) { Text("Create reminder") } }
            item { OutlinedButton(onClick = onSaveReference, modifier = Modifier.fillMaxWidth()) { Text("Save reference") } }
            item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Edit") }
                TextButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
            } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveReferenceScreen(originalText: String, initialTitle: String, onBack: () -> Unit, onSave: (String) -> String?) {
    var title by rememberSaveable { mutableStateOf(initialTitle) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Text("Save reference") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Confirm before saving", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = title, onValueChange = { title = it.take(SavedReferenceStore.MAX_TITLE_LENGTH) }, modifier = Modifier.fillMaxWidth(), label = { Text("Title") }, supportingText = { Text("${title.length}/${SavedReferenceStore.MAX_TITLE_LENGTH}") })
            Card(Modifier.fillMaxWidth().weight(1f)) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Content", style = MaterialTheme.typography.titleMedium); Text(originalText) } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Back") }
                Button(onClick = { error = onSave(title) }, enabled = originalText.isNotBlank(), modifier = Modifier.weight(1f)) { Text("Save") }
            }
        }
    }
}

@Composable
private fun ExtractionCard(result: ExtractionResult) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(result.type.displayName(), style = MaterialTheme.typography.labelLarge)
        Text(result.value, style = MaterialTheme.typography.titleMedium)
        Text("Confidence ${(result.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
    } }
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
            item {
                Text("What do you want to turn into an action?", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onImportImage, modifier = Modifier.fillMaxWidth()) {
                            Text("Import screenshot or image")
                        }
                        Button(onClick = onImportPdf, modifier = Modifier.fillMaxWidth()) {
                            Text("Import PDF")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FilledTonalButton(onClick = onPasteText, modifier = Modifier.weight(1f)) {
                                Text("Paste text")
                            }
                            FilledTonalButton(onClick = onAddLink, modifier = Modifier.weight(1f)) {
                                Text("Add link")
                            }
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
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(reminder.title, style = MaterialTheme.typography.titleMedium)
                    Text(ReminderDisplayFormatter.format(reminder.triggerAtMillis), style = MaterialTheme.typography.labelLarge)
                    if (reminder.notes.isNotBlank()) Text(reminder.notes, maxLines = 3)
                    TextButton(onClick = { onDeleteReminder(reminder) }) { Text("Cancel reminder") }
                } }
            }
            item { Text("Saved references", style = MaterialTheme.typography.titleLarge) }
            if (savedReferences.isEmpty()) item { Text("No saved references yet") }
            else items(savedReferences, key = SavedReference::id) { reference ->
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(reference.title, style = MaterialTheme.typography.titleMedium)
                    Text(reference.originalText, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { onDeleteReference(reference) }) { Text("Delete") }
                } }
            }
        }
    }
}

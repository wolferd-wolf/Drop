package com.wolferdwolf.drop.timetable

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolferdwolf.drop.MainActivity
import com.wolferdwolf.drop.ui.theme.DropTheme

class TimetableReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_OCR_TEXT).orEmpty()
        val parsed = TimetableParser.parse(source)
        val store = SavedTimetableStore(applicationContext)

        setContent {
            DropTheme {
                var reviewStructured by rememberSaveable { mutableStateOf(false) }
                if (reviewStructured && parsed != null) {
                    TimetableReviewScreen(
                        parsed,
                        onSave = { title, entries ->
                            store.save(title, entries, source)
                            finish()
                        },
                        onContinue = { title, entries ->
                            continueWithText(format(title, entries))
                        },
                        onClose = { reviewStructured = false }
                    )
                } else {
                    ImageTextReviewScreen(
                        source = source,
                        timetableAvailable = parsed != null,
                        onContinue = ::continueWithText,
                        onReviewTimetable = { reviewStructured = true },
                        onClose = ::finish
                    )
                }
            }
        }
    }

    private fun continueWithText(text: String) {
        startActivity(
            Intent(this, MainActivity::class.java)
                .setAction(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text)
        )
        finish()
    }

    companion object {
        const val EXTRA_OCR_TEXT = "ocr_text"

        fun format(title: String, entries: List<TimetableEntry>): String = buildString {
            appendLine(title.trim().ifBlank { "Imported timetable" })
            entries.forEach { entry ->
                append(entry.time)
                if (entry.label.isNotBlank()) append(" — ${entry.label.trim()}")
                appendLine()
            }
        }.trim()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageTextReviewScreen(
    source: String,
    timetableAvailable: Boolean,
    onContinue: (String) -> Unit,
    onReviewTimetable: () -> Unit,
    onClose: () -> Unit
) {
    var editable by rememberSaveable(source) { mutableStateOf(source) }

    Scaffold(topBar = { TopAppBar(title = { Text("Review image text") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Text extracted offline", style = MaterialTheme.typography.headlineSmall)
                Text("Check and edit the OCR result before Drop extracts details and suggests actions.")
            }
            item {
                OutlinedTextField(
                    value = editable,
                    onValueChange = { editable = it.take(MAX_OCR_TEXT_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Extracted image text") },
                    minLines = 10
                )
            }
            item {
                Button(
                    onClick = { onContinue(editable.trim()) },
                    enabled = editable.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continue to Drop actions") }
            }
            item {
                Text("Continuing does not save anything. You will still review the import, extracted details, and suggested actions.")
            }
            if (timetableAvailable) {
                item {
                    OutlinedButton(onClick = onReviewTimetable, modifier = Modifier.fillMaxWidth()) {
                        Text("Review detected timetable")
                    }
                }
            }
            item { OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimetableReviewScreen(
    document: TimetableDocument,
    onSave: (String, List<TimetableEntry>) -> Unit,
    onContinue: (String, List<TimetableEntry>) -> Unit,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf(document.title) }
    val entries = remember { mutableStateListOf(*document.entries.toTypedArray()) }
    val valid = entries.isNotEmpty() && entries.all { TimetableParser.normalizeTime(it.time) != null }

    Scaffold(topBar = { TopAppBar(title = { Text("Review timetable") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Optional structured timetable", style = MaterialTheme.typography.headlineSmall)
                Text("Use this only when you want to edit timetable rows before continuing or saving.")
            }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Schedule title") },
                    singleLine = true
                )
            }
            item {
                Button(
                    onClick = { onContinue(title, entries.toList()) },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continue to Drop actions") }
            }
            item {
                OutlinedButton(
                    onClick = { onSave(title, entries.toList()) },
                    enabled = valid,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save as structured timetable") }
            }
            if (!valid) item { Text("Every entry needs a valid 24-hour time such as 09:30.", color = MaterialTheme.colorScheme.error) }
            item { Text("Detected entries", style = MaterialTheme.typography.titleLarge) }
            itemsIndexed(entries, key = { index, _ -> index }) { index, entry ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Entry ${index + 1}", style = MaterialTheme.typography.labelLarge)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = entry.time,
                                onValueChange = { entries[index] = entry.copy(time = it.take(5)) },
                                modifier = Modifier.fillMaxWidth(0.32f),
                                label = { Text("Time") },
                                singleLine = true,
                                isError = TimetableParser.normalizeTime(entry.time) == null
                            )
                            OutlinedTextField(
                                value = entry.label,
                                onValueChange = { entries[index] = entry.copy(label = it.take(120)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Label") },
                                singleLine = true
                            )
                        }
                        TextButton(onClick = { entries.removeAt(index) }) { Text("Remove entry") }
                    }
                }
            }
            item {
                OutlinedButton(
                    onClick = { entries += TimetableEntry("09:00", "") },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Add timetable entry") }
            }
            item { OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Back to image text") } }
        }
    }
}

private const val MAX_OCR_TEXT_LENGTH = 50_000

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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolferdwolf.drop.MainActivity
import com.wolferdwolf.drop.data.SavedReferenceStore
import com.wolferdwolf.drop.ui.theme.DropTheme

class TimetableReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_OCR_TEXT).orEmpty()
        val parsed = TimetableParser.parse(source)

        setContent {
            DropTheme {
                if (parsed == null) {
                    NotTimetableScreen(source, onContinue = { continueWithText(source) }, onClose = ::finish)
                } else {
                    TimetableReviewScreen(
                        parsed,
                        onSave = { title, entries ->
                            SavedReferenceStore(applicationContext).save(title, format(title, entries))
                            finish()
                        },
                        onContinue = { title, entries -> continueWithText(format(title, entries)) },
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
private fun TimetableReviewScreen(
    document: TimetableDocument,
    onSave: (String, List<TimetableEntry>) -> Unit,
    onContinue: (String, List<TimetableEntry>) -> Unit,
    onClose: () -> Unit
) {
    var title by remember { mutableStateOf(document.title) }
    val entries = remember { mutableStateListOf(*document.entries.toTypedArray()) }

    Scaffold(topBar = { TopAppBar(title = { Text("Review timetable") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Timetable detected", style = MaterialTheme.typography.headlineSmall)
                Text("Check every time and label. Handwriting can be read incorrectly.")
            }
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(80) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Schedule title") }
                )
            }
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
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = entry.label,
                                onValueChange = { entries[index] = entry.copy(label = it.take(120)) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Label") },
                                singleLine = true
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = { onSave(title, entries.toList()) },
                    enabled = entries.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save timetable") }
            }
            item {
                Button(
                    onClick = { onContinue(title, entries.toList()) },
                    enabled = entries.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Continue to reminders and calendar") }
            }
            item { OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotTimetableScreen(text: String, onContinue: () -> Unit, onClose: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Review extracted text") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Text("Text was found, but it does not look like a timetable.", style = MaterialTheme.typography.headlineSmall) }
            item { Card(Modifier.fillMaxWidth()) { Text(text, Modifier.padding(16.dp)) } }
            item { Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue with extracted text") } }
            item { OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
        }
    }
}

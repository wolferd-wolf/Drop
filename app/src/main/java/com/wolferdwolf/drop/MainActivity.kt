package com.wolferdwolf.drop

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
import androidx.compose.foundation.lazy.items
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
import com.wolferdwolf.drop.share.SharedTextParser
import com.wolferdwolf.drop.ui.theme.DropTheme

class MainActivity : ComponentActivity() {
    private var sharedText by mutableStateOf<String?>(null)
    private var screen by mutableStateOf(Screen.HOME)
    private var savedReferences by mutableStateOf<List<SavedReference>>(emptyList())
    private lateinit var referenceStore: SavedReferenceStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        referenceStore = SavedReferenceStore(applicationContext)
        savedReferences = referenceStore.load()
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
                        onDelete = { reference ->
                            referenceStore.delete(reference.id)
                            savedReferences = referenceStore.load()
                        }
                    )
                    Screen.PREVIEW -> if (currentText == null) {
                        DropHomeScreen(savedReferences, onDelete = {})
                    } else {
                        SharedTextPreview(
                            initialText = currentText,
                            onDiscard = ::clearFlow,
                            onContinue = { edited ->
                                sharedText = edited
                                screen = Screen.EXTRACTION
                            }
                        )
                    }
                    Screen.EXTRACTION -> if (currentText == null) {
                        clearFlow()
                    } else {
                        ExtractedInformationScreen(
                            originalText = currentText,
                            results = RuleBasedExtractor.extract(currentText),
                            onBack = { screen = Screen.PREVIEW },
                            onSaveReference = { screen = Screen.SAVE_REFERENCE },
                            onDiscard = ::clearFlow
                        )
                    }
                    Screen.SAVE_REFERENCE -> if (currentText == null) {
                        clearFlow()
                    } else {
                        SaveReferenceScreen(
                            originalText = currentText,
                            initialTitle = SavedReferenceStore.defaultTitle(currentText),
                            onBack = { screen = Screen.EXTRACTION },
                            onSave = { title ->
                                runCatching { referenceStore.save(title, currentText) }
                                    .onSuccess {
                                        savedReferences = referenceStore.load()
                                        clearFlow()
                                    }
                                    .exceptionOrNull()?.message
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SharedTextParser.parse(intent)?.let {
            sharedText = it
            screen = Screen.PREVIEW
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SHARED_TEXT, sharedText)
        outState.putString(STATE_SCREEN, screen.name)
        super.onSaveInstanceState(outState)
    }

    private fun clearFlow() {
        sharedText = null
        screen = Screen.HOME
    }

    private enum class Screen { HOME, PREVIEW, EXTRACTION, SAVE_REFERENCE }

    private companion object {
        const val STATE_SHARED_TEXT = "shared_text"
        const val STATE_SCREEN = "screen"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTextPreview(initialText: String, onDiscard: () -> Unit, onContinue: (String) -> Unit) {
    var editableText by rememberSaveable(initialText) { mutableStateOf(initialText) }
    Scaffold(topBar = { TopAppBar(title = { Text("Share preview") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Shared text", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = editableText,
                onValueChange = { editableText = it.take(SharedTextParser.MAX_SHARED_TEXT_LENGTH) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = { Text("Original content") },
                supportingText = { Text("Review or edit before Drop processes it") }
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
                Button(
                    onClick = { onContinue(editableText.trim()) },
                    enabled = editableText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) { Text("Continue") }
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
                    if (results.isEmpty()) "Nothing actionable was found"
                    else "${results.size} useful detail${if (results.size == 1) "" else "s"} found",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            if (results.isEmpty()) {
                item { Text("You can still save the original content as a reference.") }
            } else {
                items(results, key = { "${it.type}-${it.sourceStart}-${it.value}" }) { ExtractionCard(it) }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Original text", style = MaterialTheme.typography.titleMedium)
                        Text(originalText)
                    }
                }
            }
            item {
                Button(onClick = onSaveReference, modifier = Modifier.fillMaxWidth()) {
                    Text("Save reference")
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("Edit text") }
                    TextButton(onClick = onDiscard, modifier = Modifier.weight(1f)) { Text("Discard") }
                }
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
fun DropHomeScreen(savedReferences: List<SavedReference>, onDelete: (SavedReference) -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Drop") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Turn anything on your phone into the next useful action.", style = MaterialTheme.typography.headlineSmall)
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Ready for shared content", style = MaterialTheme.typography.titleMedium)
                        Text("Share text from another app to extract details or save it privately on this device.")
                    }
                }
            }
            item { Text("Saved references", style = MaterialTheme.typography.titleLarge) }
            if (savedReferences.isEmpty()) {
                item { Text("No saved references yet") }
            } else {
                items(savedReferences, key = SavedReference::id) { reference ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(reference.title, style = MaterialTheme.typography.titleMedium)
                            Text(reference.originalText, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
                            TextButton(onClick = { onDelete(reference) }) { Text("Delete") }
                        }
                    }
                }
            }
        }
    }
}

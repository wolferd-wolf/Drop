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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolferdwolf.drop.share.SharedTextParser
import com.wolferdwolf.drop.ui.theme.DropTheme

class MainActivity : ComponentActivity() {
    private var sharedText by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedText = savedInstanceState?.getString(STATE_SHARED_TEXT) ?: SharedTextParser.parse(intent)
        setContent {
            DropTheme {
                sharedText?.let { text ->
                    SharedTextPreview(
                        initialText = text,
                        onDiscard = { sharedText = null },
                        onContinue = { edited -> sharedText = edited }
                    )
                } ?: DropHomeScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SharedTextParser.parse(intent)?.let { sharedText = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_SHARED_TEXT, sharedText)
        super.onSaveInstanceState(outState)
    }

    private companion object {
        const val STATE_SHARED_TEXT = "shared_text"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedTextPreview(
    initialText: String,
    onDiscard: () -> Unit,
    onContinue: (String) -> Unit
) {
    var editableText by rememberSaveable(initialText) { mutableStateOf(initialText) }

    Scaffold(topBar = { TopAppBar(title = { Text("Share preview") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Shared text", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(
                value = editableText,
                onValueChange = { editableText = it.take(SharedTextParser.MAX_SHARED_TEXT_LENGTH) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = { Text("Original content") },
                supportingText = { Text("Review or edit before Drop processes it") }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDiscard, modifier = Modifier.weight(1f)) {
                    Text("Discard")
                }
                Button(
                    onClick = { onContinue(editableText.trim()) },
                    enabled = editableText.isNotBlank(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropHomeScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("Drop") }) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Turn anything on your phone into the next useful action.",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Ready for shared content", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Share text from another app to preview it safely in Drop.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            item { Text("No saved actions yet", style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

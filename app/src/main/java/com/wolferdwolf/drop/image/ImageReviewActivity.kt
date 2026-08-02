package com.wolferdwolf.drop.image

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
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
import com.wolferdwolf.drop.MainActivity
import com.wolferdwolf.drop.ui.theme.DropTheme

class ImageReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_OCR_TEXT).orEmpty()
        setContent {
            DropTheme {
                ImageTextReviewScreen(
                    source = source,
                    onContinue = ::continueWithText,
                    onClose = ::finish
                )
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageTextReviewScreen(
    source: String,
    onContinue: (String) -> Unit,
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
            item {
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel")
                }
            }
        }
    }
}

private const val MAX_OCR_TEXT_LENGTH = 50_000

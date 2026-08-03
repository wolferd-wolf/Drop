package com.wolferdwolf.drop.pdf

import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.wolferdwolf.drop.MainActivity
import com.wolferdwolf.drop.ui.theme.DropTheme

class PdfImportActivity : ComponentActivity() {
    private var state by mutableStateOf<PdfState>(PdfState.Loading)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DropTheme {
                when (val current = state) {
                    PdfState.Loading -> LoadingScreen(::finish)
                    is PdfState.Ready -> PdfReviewScreen(current, ::continueWithText, ::finish)
                    is PdfState.Error -> ErrorScreen(current.message, ::finish)
                }
            }
        }

        intent.getStringExtra(EXTRA_TEST_TEXT)?.let { testText ->
            state = PdfState.Ready(
                fileName = intent.getStringExtra(EXTRA_TEST_NAME) ?: "sample.pdf",
                fileSize = "Test document",
                pageCount = 1,
                text = testText,
                message = "Embedded text extracted offline from 1 page."
            )
            return
        }

        val uri = intent.data
        if (uri == null) {
            state = PdfState.Error("No PDF was provided. Return to Home and select the file again.")
            return
        }
        val metadata = metadata(uri)
        Thread {
            val result = PdfTextExtractor.extract(this, uri, metadata.second)
            runOnUiThread {
                result.fold(
                    onSuccess = { extraction ->
                        if (extraction.hasEmbeddedText) {
                            state = PdfState.Ready(
                                fileName = metadata.first ?: "Unnamed PDF",
                                fileSize = metadata.second?.let(::fileSize) ?: "Unknown size",
                                pageCount = extraction.pageCount,
                                text = extraction.text,
                                message = if (extraction.wasTruncated) {
                                    "Text extracted offline. Processing was limited to ${PdfTextExtractor.MAX_PAGES} pages and ${PdfTextExtractor.MAX_TEXT_LENGTH} characters."
                                } else {
                                    "Embedded text extracted offline from ${extraction.pageCount} page${if (extraction.pageCount == 1) "" else "s"}."
                                }
                            )
                        } else {
                            state = PdfState.Loading
                            runScannedPdfOcr(uri, metadata, extraction.pageCount)
                        }
                    },
                    onFailure = { state = PdfState.Error(it.message ?: "Drop could not read this PDF.") }
                )
            }
        }.start()
    }

    private fun runScannedPdfOcr(uri: Uri, metadata: Pair<String?, Long?>, pageCount: Int) {
        PdfPageOcrProcessor.process(
            this,
            uri,
            onSuccess = { result ->
                val fallback = buildString {
                    appendLine("Scanned PDF imported")
                    appendLine("File: ${metadata.first ?: "Unnamed PDF"}")
                    appendLine("Pages: $pageCount")
                    append("No readable text was found in the first ${result.attemptedPages} page${if (result.attemptedPages == 1) "" else "s"}. Add or correct text below before continuing.")
                }
                state = PdfState.Ready(
                    fileName = metadata.first ?: "Unnamed PDF",
                    fileSize = metadata.second?.let(::fileSize) ?: "Unknown size",
                    pageCount = pageCount,
                    text = result.text.ifBlank { fallback },
                    message = when {
                        result.text.isBlank() -> "No readable text was found. Drop checked ${result.attemptedPages} page${if (result.attemptedPages == 1) "" else "s"} offline."
                        result.failedPages > 0 -> "Scanned-page text extracted offline from ${result.attemptedPages - result.failedPages} of ${result.attemptedPages} checked pages."
                        result.totalPages > result.attemptedPages -> "Scanned-page text extracted offline from the first ${result.attemptedPages} of ${result.totalPages} pages."
                        else -> "Scanned-page text extracted offline from ${result.attemptedPages} page${if (result.attemptedPages == 1) "" else "s"}."
                    }
                )
            },
            onFailure = { message ->
                state = PdfState.Ready(
                    fileName = metadata.first ?: "Unnamed PDF",
                    fileSize = metadata.second?.let(::fileSize) ?: "Unknown size",
                    pageCount = pageCount,
                    text = "Scanned PDF imported\nFile: ${metadata.first ?: "Unnamed PDF"}\nPages: $pageCount\nAdd text manually before continuing.",
                    message = "$message You can still add text manually without uploading the PDF."
                )
            }
        )
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

    companion object {
        const val EXTRA_TEST_TEXT = "pdf_test_text"
        const val EXTRA_TEST_NAME = "pdf_test_name"
    }
}

private sealed interface PdfState {
    data object Loading : PdfState
    data class Ready(
        val fileName: String,
        val fileSize: String,
        val pageCount: Int,
        val text: String,
        val message: String
    ) : PdfState
    data class Error(val message: String) : PdfState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoadingScreen(onCancel: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Import PDF") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Reading PDF offline…", style = MaterialTheme.typography.headlineSmall)
            Text("Drop reads only a bounded number of pages and never uploads the document.")
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PdfReviewScreen(state: PdfState.Ready, onContinue: (String) -> Unit, onCancel: () -> Unit) {
    var editable by rememberSaveable(state.text) { mutableStateOf(state.text) }
    Scaffold(topBar = { TopAppBar(title = { Text("Review PDF text") }) }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Text("Review before extraction", style = MaterialTheme.typography.headlineSmall) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(state.fileName, style = MaterialTheme.typography.titleMedium)
                        Text("${state.pageCount} page${if (state.pageCount == 1) "" else "s"} • ${state.fileSize}")
                        Text(state.message)
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = editable,
                    onValueChange = { editable = it.take(PdfTextExtractor.MAX_TEXT_LENGTH) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("PDF content") },
                    minLines = 10
                )
            }
            item { Button(onClick = { onContinue(editable.trim()) }, enabled = editable.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Continue to Drop actions") } }
            item { Text("Continuing does not save the PDF or create an action. You will review the content again before extraction.") }
            item { OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ErrorScreen(message: String, onClose: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Import PDF") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("This PDF could not be processed", style = MaterialTheme.typography.headlineSmall)
            Text(message, color = MaterialTheme.colorScheme.error)
            Text("The file was not uploaded or saved.")
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Back to Home") }
        }
    }
}

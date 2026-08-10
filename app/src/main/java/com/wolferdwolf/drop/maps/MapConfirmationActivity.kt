package com.wolferdwolf.drop.maps

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolferdwolf.drop.data.SavedReferenceStore
import com.wolferdwolf.drop.extraction.AddressCandidateDetector
import com.wolferdwolf.drop.ui.theme.DropTheme

class MapConfirmationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_SOURCE_TEXT).orEmpty()
        val curatedQuery = intent.getStringExtra(EXTRA_CURATED_QUERY).orEmpty()
        val suggestion = initialQuery(source, curatedQuery)

        setContent {
            DropTheme {
                var query by rememberSaveable { mutableStateOf(suggestion) }
                var error by rememberSaveable { mutableStateOf<String?>(null) }

                MapConfirmationScreen(
                    query = query,
                    error = error,
                    onQueryChange = { query = it.take(MAX_QUERY_LENGTH) },
                    onOpen = {
                        val clean = query.trim()
                        if (clean.isBlank()) {
                            error = "Enter an address or place to search."
                        } else {
                            error = launchMapsAndRecord(clean)
                        }
                    },
                    onCancel = ::finish
                )
            }
        }
    }

    private fun launchMapsAndRecord(query: String): String? {
        val cleanQuery = query.trim()
        val mapsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(cleanQuery)}"))
        return try {
            if (mapsIntent.resolveActivity(packageManager) == null) {
                "No compatible Maps app is installed."
            } else {
                startActivity(mapsIntent)
                SavedReferenceStore(applicationContext).save(
                    historyTitle(cleanQuery),
                    historyContent(cleanQuery)
                )
                null
            }
        } catch (_: ActivityNotFoundException) {
            "No compatible Maps app is installed."
        } catch (_: SecurityException) {
            "Android blocked this action. Check device settings and try again."
        } catch (_: Exception) {
            "Maps opened, but Drop could not record this action in History."
        }
    }

    companion object {
        const val EXTRA_SOURCE_TEXT = "source_text"
        const val EXTRA_CURATED_QUERY = "curated_query"
        const val MAX_QUERY_LENGTH = 500

        internal fun initialQuery(source: String, curatedQuery: String): String =
  curatedQuery.trim().takeIf(String::isNotBlank)?.take(MAX_QUERY_LENGTH)
      ?: AddressCandidateDetector.detect(source)?.value
      ?: source.take(MAX_QUERY_LENGTH)

        internal fun historyTitle(query: String): String =
            "Maps: ${query.trim().take(80)}"

        internal fun historyContent(query: String): String = buildString {
            append("Status: Opened in Maps app\n")
            append("Location: ").append(query.trim())
        }.take(MAX_QUERY_LENGTH)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun MapConfirmationScreen(
    query: String,
    error: String?,
    onQueryChange: (String) -> Unit,
    onOpen: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Open in Maps") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Confirm the location", style = MaterialTheme.typography.headlineSmall)
                Text("Drop detected a likely address or venue. Edit it before opening another app.")
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Address or place") },
                    minLines = 3
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("You stay in control", style = MaterialTheme.typography.titleMedium)
                        Text("Drop does not open Maps until you confirm. Your imported content remains on this device. A record is added to History after Maps opens.")
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Button(
                    onClick = onOpen,
                    enabled = query.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Maps") }
            }
            item {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}

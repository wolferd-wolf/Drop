package com.wolferdwolf.drop.link

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
import com.wolferdwolf.drop.ui.theme.DropTheme

class OpenLinkConfirmationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val detectedUrl = intent.getStringExtra(EXTRA_URL).orEmpty()

        setContent {
            DropTheme {
                var url by rememberSaveable { mutableStateOf(detectedUrl) }
                var error by rememberSaveable { mutableStateOf<String?>(null) }

                OpenLinkConfirmationScreen(
                    url = url,
                    error = error,
                    onUrlChange = { url = it.take(MAX_URL_LENGTH) },
                    onOpen = {
                        val normalized = OpenLinkValidator.normalize(url)
                        error = when {
                            normalized == null -> "Enter a valid http or https website link."
                            else -> launchBrowser(normalized)
                        }
                    },
                    onCancel = ::finish
                )
            }
        }
    }

    private fun launchBrowser(url: String): String? {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        return try {
            if (browserIntent.resolveActivity(packageManager) == null) {
                "No compatible browser is installed."
            } else {
                startActivity(browserIntent)
                null
            }
        } catch (_: ActivityNotFoundException) {
            "No compatible browser is installed."
        } catch (_: SecurityException) {
            "Android blocked this action. Check device settings and try again."
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val MAX_URL_LENGTH = 2_048
    }
}

object OpenLinkValidator {
    fun normalize(value: String): String? {
        val clean = value.trim()
        if (clean.isBlank() || clean.any(Char::isWhitespace)) return null
        val candidate = if (clean.startsWith("http://", true) || clean.startsWith("https://", true)) clean else "https://$clean"
        val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        val host = uri.host
        return candidate.takeIf { scheme in setOf("http", "https") && !host.isNullOrBlank() && host.contains('.') }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@androidx.compose.runtime.Composable
private fun OpenLinkConfirmationScreen(
    url: String,
    error: String?,
    onUrlChange: (String) -> Unit,
    onOpen: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Open link") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Confirm the website", style = MaterialTheme.typography.headlineSmall)
                Text("Review and edit the detected link before Drop opens your browser.")
            }
            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Website link") },
                    singleLine = true
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("You stay in control", style = MaterialTheme.typography.titleMedium)
                        Text("Drop only opens http or https links after confirmation. It never opens the detected link automatically.")
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Button(onClick = onOpen, enabled = url.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Continue to Browser")
                }
            }
            item {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}

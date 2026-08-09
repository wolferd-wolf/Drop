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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolferdwolf.drop.data.SavedReferenceStore
import com.wolferdwolf.drop.ui.theme.DropTheme
import java.net.URI

class OpenLinkConfirmationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val detectedUrl = intent.getStringExtra(EXTRA_URL).orEmpty()

        setContent {
            DropTheme {
                var url by rememberSaveable { mutableStateOf(detectedUrl) }
                var error by rememberSaveable { mutableStateOf<String?>(null) }
                var completed by rememberSaveable { mutableStateOf(false) }

                OpenLinkConfirmationScreen(
                    url = url,
                    error = error,
                    completed = completed,
                    onUrlChange = { url = it.take(MAX_URL_LENGTH) },
                    onOpen = {
                        if (completed) {
                            finish()
                        } else {
                            val normalized = OpenLinkValidator.normalize(url)
                            if (normalized == null) {
                                error = "Enter a valid http or https website link."
                            } else {
                                // Lock the confirmation before leaving Drop so Android can save
                                // the completed state even if the browser immediately stops or
                                // recreates this activity. Roll back only if no browser launches.
                                completed = true
                                val outcome = launchBrowserAndRecord(normalized)
                                completed = outcome.launched
                                error = outcome.message
                            }
                        }
                    },
                    onCancel = ::finish
                )
            }
        }
    }

    private fun launchBrowserAndRecord(url: String): BrowserLaunchOutcome {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        try {
            if (browserIntent.resolveActivity(packageManager) == null) {
                return BrowserLaunchOutcome(false, "No compatible browser is installed.")
            }
            startActivity(browserIntent)
        } catch (_: ActivityNotFoundException) {
            return BrowserLaunchOutcome(false, "No compatible browser is installed.")
        } catch (_: SecurityException) {
            return BrowserLaunchOutcome(false, "Android blocked this action. Check device settings and try again.")
        } catch (_: Exception) {
            return BrowserLaunchOutcome(false, "Drop could not open the browser. Try again.")
        }

        return try {
            SavedReferenceStore(applicationContext).save(historyTitle(url), historyContent(url))
            BrowserLaunchOutcome(true, null)
        } catch (_: Exception) {
            // The external action already happened. Keep this screen read-only so a
            // History write failure cannot cause the browser to be opened twice.
            BrowserLaunchOutcome(true, "The browser opened, but Drop could not record this action in History.")
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val MAX_URL_LENGTH = 2_048

        internal fun historyTitle(url: String): String {
            val host = runCatching { URI(url).host }.getOrNull().orEmpty().removePrefix("www.")
            return if (host.isNotBlank()) "Opened link: ${host.take(80)}" else "Opened link"
        }

        internal fun historyContent(url: String): String = "Status: Opened in browser\nURL: ${url.trim()}"
    }
}

private data class BrowserLaunchOutcome(
    val launched: Boolean,
    val message: String?
)

object OpenLinkValidator {
    fun normalize(value: String): String? {
        val clean = value.trim()
        if (clean.isBlank() || clean.any(Char::isWhitespace)) return null
        val hasWebScheme = clean.startsWith("http://", true) || clean.startsWith("https://", true)
        if (!hasWebScheme && ':' in clean) return null
        val candidate = if (hasWebScheme) clean else "https://$clean"
        val uri = runCatching { URI(candidate) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        val host = uri.host
        return candidate.takeIf {
            scheme in setOf("http", "https") && !host.isNullOrBlank() && host.contains('.')
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OpenLinkConfirmationScreen(
    url: String,
    error: String?,
    completed: Boolean,
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
                Text(
                    if (completed) "Browser opened" else "Confirm the website",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    if (completed) {
                        "This action is saved in History. Return to Drop when you are done with the browser."
                    } else {
                        "Review and edit the detected link before Drop opens your browser."
                    }
                )
            }
            item {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Website link") },
                    singleLine = true,
                    enabled = !completed
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("You stay in control", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (completed) {
                                "Drop opened the browser once with the confirmed link. This screen is locked to prevent a second launch."
                            } else {
                                "Drop only opens http or https links after confirmation. It never opens a detected link automatically. A record is added to History after the browser opens."
                            }
                        )
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Button(onClick = onOpen, enabled = url.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(if (completed) "Done" else "Continue to Browser")
                }
            }
            if (!completed) {
                item {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
                }
            }
        }
    }
}

package com.wolferdwolf.drop.email

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
import com.wolferdwolf.drop.extraction.ExtractionType
import com.wolferdwolf.drop.extraction.RuleBasedExtractor
import com.wolferdwolf.drop.ui.theme.DropTheme

class EmailConfirmationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_SOURCE_TEXT).orEmpty()
        val extractedEmail = RuleBasedExtractor.extract(source)
            .firstOrNull { it.type == ExtractionType.EMAIL }
            ?.value
            .orEmpty()
        val initialSubject = subjectFrom(source)

        setContent {
            DropTheme {
                var recipient by rememberSaveable { mutableStateOf(extractedEmail) }
                var subject by rememberSaveable { mutableStateOf(initialSubject) }
                var message by rememberSaveable { mutableStateOf(source.take(MAX_MESSAGE_LENGTH)) }
                var error by rememberSaveable { mutableStateOf<String?>(null) }

                EmailConfirmationScreen(
                    recipient = recipient,
                    subject = subject,
                    message = message,
                    error = error,
                    onRecipientChange = { recipient = it.take(MAX_EMAIL_LENGTH) },
                    onSubjectChange = { subject = it.take(MAX_SUBJECT_LENGTH) },
                    onMessageChange = { message = it.take(MAX_MESSAGE_LENGTH) },
                    onContinue = { error = validateLaunchAndRecord(recipient, subject, message) },
                    onCancel = ::finish
                )
            }
        }
    }

    private fun validateLaunchAndRecord(recipient: String, subject: String, message: String): String? {
        val validationError = validateRecipient(recipient)
        if (validationError != null) return validationError

        val cleanRecipient = recipient.trim()
        val cleanSubject = subject.trim()
        val cleanMessage = message.trim()
        val emailIntent = Intent(Intent.ACTION_SENDTO)
            .setData(Uri.parse("mailto:${Uri.encode(cleanRecipient)}"))
            .putExtra(Intent.EXTRA_SUBJECT, cleanSubject)
            .putExtra(Intent.EXTRA_TEXT, cleanMessage)

        return try {
            if (emailIntent.resolveActivity(packageManager) == null) {
                "No compatible email app is installed."
            } else {
                startActivity(emailIntent)
                SavedReferenceStore(applicationContext).save(
                    historyTitle(cleanRecipient, cleanSubject),
                    historyContent(cleanRecipient, cleanSubject, cleanMessage)
                )
                null
            }
        } catch (_: ActivityNotFoundException) {
            "No compatible email app is installed."
        } catch (_: SecurityException) {
            "Android blocked this action. Check device settings and try again."
        } catch (_: Exception) {
            "The email app opened, but Drop could not record this action in History."
        }
    }

    companion object {
        const val EXTRA_SOURCE_TEXT = "source_text"
        private const val MAX_EMAIL_LENGTH = 254
        private const val MAX_SUBJECT_LENGTH = 200
        private const val MAX_MESSAGE_LENGTH = 5_000
        private val EMAIL_PATTERN = Regex("^[A-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

        internal fun validateRecipient(recipient: String): String? = when {
            recipient.isBlank() -> "Enter an email address."
            !EMAIL_PATTERN.matches(recipient.trim()) -> "Enter a valid email address."
            else -> null
        }

        internal fun subjectFrom(source: String): String {
            val labelled = Regex("(?im)^\\s*subject\\s*:\\s*([^\\n]+)")
                .find(source)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
            return labelled?.take(MAX_SUBJECT_LENGTH)
                ?: SavedReferenceStore.defaultTitle(source).take(MAX_SUBJECT_LENGTH)
        }

        internal fun historyTitle(recipient: String, subject: String): String {
            val cleanSubject = subject.trim()
            return if (cleanSubject.isNotBlank()) {
                "Email: ${cleanSubject.take(80)}"
            } else {
                "Email to ${recipient.trim()}"
            }
        }

        internal fun historyContent(recipient: String, subject: String, message: String): String = buildString {
            append("Status: Opened in email app\n")
            append("To: ").append(recipient.trim()).append('\n')
            if (subject.isNotBlank()) append("Subject: ").append(subject.trim()).append('\n')
            if (message.isNotBlank()) append("\n").append(message.trim())
        }.take(MAX_MESSAGE_LENGTH)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmailConfirmationScreen(
    recipient: String,
    subject: String,
    message: String,
    error: String?,
    onRecipientChange: (String) -> Unit,
    onSubjectChange: (String) -> Unit,
    onMessageChange: (String) -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Send email") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Confirm email", style = MaterialTheme.typography.headlineSmall)
                Text("Review and edit the recipient, subject, and message before Drop opens your email app.")
            }
            item { OutlinedTextField(recipient, onRecipientChange, Modifier.fillMaxWidth(), label = { Text("To") }, singleLine = true) }
            item { OutlinedTextField(subject, onSubjectChange, Modifier.fillMaxWidth(), label = { Text("Subject") }, singleLine = true) }
            item { OutlinedTextField(message, onMessageChange, Modifier.fillMaxWidth(), label = { Text("Message") }, minLines = 4, maxLines = 7) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nothing is sent automatically", style = MaterialTheme.typography.titleMedium)
                        Text("Drop only passes these edited details to your email app after you confirm. You still choose whether to send it. A record is added to History after the email app opens.")
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item { Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue to Email") } }
            item { OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
        }
    }
}

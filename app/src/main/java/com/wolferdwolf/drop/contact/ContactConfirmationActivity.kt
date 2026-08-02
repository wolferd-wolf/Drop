package com.wolferdwolf.drop.contact

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
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
import com.wolferdwolf.drop.extraction.ExtractionType
import com.wolferdwolf.drop.extraction.RuleBasedExtractor
import com.wolferdwolf.drop.ui.theme.DropTheme

class ContactConfirmationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_SOURCE_TEXT).orEmpty()
        val extracted = RuleBasedExtractor.extract(source)
        val initialPhone = extracted.firstOrNull { it.type == ExtractionType.PHONE }?.value.orEmpty()
        val initialEmail = extracted.firstOrNull { it.type == ExtractionType.EMAIL }?.value.orEmpty()
        val initialName = labelledValue(source, NAME_LABELS)
        val initialCompany = labelledValue(source, COMPANY_LABELS)

        setContent {
            DropTheme {
                var name by rememberSaveable { mutableStateOf(initialName) }
                var phone by rememberSaveable { mutableStateOf(initialPhone) }
                var email by rememberSaveable { mutableStateOf(initialEmail) }
                var company by rememberSaveable { mutableStateOf(initialCompany) }
                var notes by rememberSaveable { mutableStateOf(source.take(MAX_NOTES_LENGTH)) }
                var error by rememberSaveable { mutableStateOf<String?>(null) }

                ContactConfirmationScreen(
                    name = name,
                    phone = phone,
                    email = email,
                    company = company,
                    notes = notes,
                    error = error,
                    onNameChange = { name = it.take(MAX_NAME_LENGTH) },
                    onPhoneChange = { phone = it.take(MAX_PHONE_LENGTH) },
                    onEmailChange = { email = it.take(MAX_EMAIL_LENGTH) },
                    onCompanyChange = { company = it.take(MAX_COMPANY_LENGTH) },
                    onNotesChange = { notes = it.take(MAX_NOTES_LENGTH) },
                    onContinue = { error = validateAndLaunch(name, phone, email, company, notes) },
                    onCancel = ::finish
                )
            }
        }
    }

    private fun validateAndLaunch(
        name: String,
        phone: String,
        email: String,
        company: String,
        notes: String
    ): String? {
        if (name.isBlank() && phone.isBlank() && email.isBlank()) {
            return "Enter a name, phone number, or email address."
        }
        if (email.isNotBlank() && !EMAIL_PATTERN.matches(email.trim())) {
            return "Enter a valid email address."
        }
        if (phone.isNotBlank() && phone.count(Char::isDigit) !in 8..15) {
            return "Enter a phone number containing 8 to 15 digits."
        }

        val contactIntent = Intent(Intent.ACTION_INSERT)
            .setType(ContactsContract.Contacts.CONTENT_TYPE)
            .putExtra(ContactsContract.Intents.Insert.NAME, name.trim())
            .putExtra(ContactsContract.Intents.Insert.PHONE, phone.trim())
            .putExtra(ContactsContract.Intents.Insert.EMAIL, email.trim())
            .putExtra(ContactsContract.Intents.Insert.COMPANY, company.trim())
            .putExtra(ContactsContract.Intents.Insert.NOTES, notes.trim())

        return try {
            if (contactIntent.resolveActivity(packageManager) == null) {
                "No compatible Contacts app is installed."
            } else {
                startActivity(contactIntent)
                null
            }
        } catch (_: ActivityNotFoundException) {
            "No compatible Contacts app is installed."
        } catch (_: SecurityException) {
            "Android blocked this action. Check device settings and try again."
        }
    }

    companion object {
        const val EXTRA_SOURCE_TEXT = "source_text"
        private const val MAX_NAME_LENGTH = 120
        private const val MAX_PHONE_LENGTH = 40
        private const val MAX_EMAIL_LENGTH = 254
        private const val MAX_COMPANY_LENGTH = 160
        private const val MAX_NOTES_LENGTH = 5_000
        private val EMAIL_PATTERN = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
        internal val NAME_LABELS = listOf("name", "contact", "person")
        internal val COMPANY_LABELS = listOf("company", "organisation", "organization", "business")

        internal fun labelledValue(source: String, labels: List<String>): String {
            val alternatives = labels.joinToString("|") { Regex.escape(it) }
            return Regex("(?im)^\\s*(?:$alternatives)\\s*:\\s*([^\\n]+)")
                .find(source)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                .orEmpty()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactConfirmationScreen(
    name: String,
    phone: String,
    email: String,
    company: String,
    notes: String,
    error: String?,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Save contact") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Confirm contact details", style = MaterialTheme.typography.headlineSmall)
                Text("Review and edit every field before Drop opens your Contacts app.")
            }
            item { OutlinedTextField(name, onNameChange, Modifier.fillMaxWidth(), label = { Text("Name") }, singleLine = true) }
            item { OutlinedTextField(phone, onPhoneChange, Modifier.fillMaxWidth(), label = { Text("Phone") }, singleLine = true) }
            item { OutlinedTextField(email, onEmailChange, Modifier.fillMaxWidth(), label = { Text("Email") }, singleLine = true) }
            item { OutlinedTextField(company, onCompanyChange, Modifier.fillMaxWidth(), label = { Text("Company") }, singleLine = true) }
            item { OutlinedTextField(notes, onNotesChange, Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 2, maxLines = 3) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Nothing is saved automatically", style = MaterialTheme.typography.titleMedium)
                        Text("Drop only passes these edited details to Contacts after you confirm.")
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item { Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue to Contacts") } }
            item { OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
        }
    }
}

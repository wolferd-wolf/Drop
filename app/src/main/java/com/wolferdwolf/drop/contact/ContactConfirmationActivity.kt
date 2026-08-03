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
import com.wolferdwolf.drop.data.SavedReferenceStore
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
                    onContinue = { error = validateLaunchAndRecord(name, phone, email, company, notes) },
                    onCancel = ::finish
                )
            }
        }
    }

    private fun validateLaunchAndRecord(
        name: String,
        phone: String,
        email: String,
        company: String,
        notes: String
    ): String? {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanEmail = email.trim()
        val cleanCompany = company.trim()
        val cleanNotes = notes.trim()

        if (cleanName.isBlank() && cleanPhone.isBlank() && cleanEmail.isBlank()) {
            return "Enter a name, phone number, or email address."
        }
        if (cleanEmail.isNotBlank() && !EMAIL_PATTERN.matches(cleanEmail)) {
            return "Enter a valid email address."
        }
        if (cleanPhone.isNotBlank() && cleanPhone.count(Char::isDigit) !in 8..15) {
            return "Enter a phone number containing 8 to 15 digits."
        }

        val contactIntent = Intent(Intent.ACTION_INSERT)
            .setType(ContactsContract.Contacts.CONTENT_TYPE)
            .putExtra(ContactsContract.Intents.Insert.NAME, cleanName)
            .putExtra(ContactsContract.Intents.Insert.PHONE, cleanPhone)
            .putExtra(ContactsContract.Intents.Insert.EMAIL, cleanEmail)
            .putExtra(ContactsContract.Intents.Insert.COMPANY, cleanCompany)
            .putExtra(ContactsContract.Intents.Insert.NOTES, cleanNotes)

        return try {
            if (contactIntent.resolveActivity(packageManager) == null) {
                "No compatible Contacts app is installed."
            } else {
                startActivity(contactIntent)
                SavedReferenceStore(applicationContext).save(
                    historyTitle(cleanName, cleanPhone, cleanEmail),
                    historyContent(cleanName, cleanPhone, cleanEmail, cleanCompany, cleanNotes)
                )
                null
            }
        } catch (_: ActivityNotFoundException) {
            "No compatible Contacts app is installed."
        } catch (_: SecurityException) {
            "Android blocked this action. Check device settings and try again."
        } catch (_: Exception) {
            "The Contacts app opened, but Drop could not record this action in History."
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

        internal fun historyTitle(name: String, phone: String, email: String): String {
            val label = name.trim().ifBlank { phone.trim().ifBlank { email.trim() } }
            return "Contact: ${label.take(80)}"
        }

        internal fun historyContent(
            name: String,
            phone: String,
            email: String,
            company: String,
            notes: String
        ): String = buildString {
            append("Status: Opened in Contacts app\n")
            if (name.isNotBlank()) append("Name: ").append(name.trim()).append('\n')
            if (phone.isNotBlank()) append("Phone: ").append(phone.trim()).append('\n')
            if (email.isNotBlank()) append("Email: ").append(email.trim()).append('\n')
            if (company.isNotBlank()) append("Company: ").append(company.trim()).append('\n')
            if (notes.isNotBlank()) append("\n").append(notes.trim())
        }.take(MAX_NOTES_LENGTH)
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
                        Text("Drop only passes these edited details to Contacts after you confirm. You still choose whether to save the contact. A record is added to History after the Contacts app opens.")
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item { Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Continue to Contacts") } }
            item { OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
        }
    }
}

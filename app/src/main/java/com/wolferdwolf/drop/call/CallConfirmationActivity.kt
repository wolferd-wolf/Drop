package com.wolferdwolf.drop.call

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

class CallConfirmationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val detectedNumber = intent.getStringExtra(EXTRA_PHONE).orEmpty()

        setContent {
            DropTheme {
                var phone by rememberSaveable { mutableStateOf(detectedNumber) }
                var error by rememberSaveable { mutableStateOf<String?>(null) }

                CallConfirmationScreen(
                    phone = phone,
                    error = error,
                    onPhoneChange = { phone = it.take(MAX_PHONE_LENGTH) },
                    onContinue = {
                        val normalized = PhoneNumberValidator.normalize(phone)
                        error = if (normalized == null) {
                            "Enter a valid phone number."
                        } else {
                            openDialerAndRecord(normalized)
                        }
                    },
                    onCancel = ::finish
                )
            }
        }
    }

    private fun openDialerAndRecord(phone: String): String? {
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}"))
        return try {
            if (dialIntent.resolveActivity(packageManager) == null) {
                "No compatible phone app is installed."
            } else {
                startActivity(dialIntent)
                SavedReferenceStore(applicationContext).save(historyTitle(phone), historyContent(phone))
                null
            }
        } catch (_: ActivityNotFoundException) {
            "No compatible phone app is installed."
        } catch (_: SecurityException) {
            "Android blocked this action. Check device settings and try again."
        } catch (_: Exception) {
            "The dialer opened, but Drop could not record this action in History."
        }
    }

    companion object {
        const val EXTRA_PHONE = "phone"
        const val MAX_PHONE_LENGTH = 32

        internal fun historyTitle(phone: String) = "Opened dialer: ${phone.take(32)}"
        internal fun historyContent(phone: String) = "Status: Opened in phone app\nPhone: ${phone.trim()}"
    }
}

object PhoneNumberValidator {
    fun normalize(value: String): String? {
        val clean = value.trim()
        if (clean.isBlank() || clean.length > CallConfirmationActivity.MAX_PHONE_LENGTH) return null
        if (!clean.matches(Regex("^\\+?[0-9][0-9 ()-]{5,30}$"))) return null
        val digits = clean.count(Char::isDigit)
        if (digits !in 7..15) return null
        return clean.replace(Regex("[ ()-]"), "")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallConfirmationScreen(
    phone: String,
    error: String?,
    onPhoneChange: (String) -> Unit,
    onContinue: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Call") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Confirm the phone number", style = MaterialTheme.typography.headlineSmall)
                Text("Review and edit the detected number before Drop opens your phone app.")
            }
            item {
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Phone number") },
                    singleLine = true
                )
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("You stay in control", style = MaterialTheme.typography.titleMedium)
                        Text("Drop opens the dialer with this number. It never places the call automatically. A record is added to History after the phone app opens.")
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Button(onClick = onContinue, enabled = phone.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text("Continue to Phone App")
                }
            }
            item {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}

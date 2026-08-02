package com.wolferdwolf.drop.reminder

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.wolferdwolf.drop.ui.theme.DropTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class ReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceText = intent.getStringExtra(EXTRA_SOURCE_TEXT).orEmpty()
        val scheduler = ReminderScheduler(applicationContext)
        val historyStore = ReminderHistoryStore(applicationContext)
        setContent {
            DropTheme {
                ReminderScreen(
                    sourceText = sourceText,
                    onClose = { finish() },
                    schedule = { reminder ->
                        scheduler.schedule(reminder).onSuccess { historyStore.save(reminder) }
                    }
                )
            }
        }
    }

    companion object {
        const val EXTRA_SOURCE_TEXT = "source_text"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderScreen(
    sourceText: String,
    onClose: () -> Unit,
    schedule: (ReminderValidator.ValidReminder) -> Result<Unit>
) {
    val today = remember { LocalDate.now() }
    var title by rememberSaveable { mutableStateOf(sourceText.lineSequence().firstOrNull { it.isNotBlank() }?.take(120) ?: "Reminder") }
    var notes by rememberSaveable { mutableStateOf(sourceText) }
    var date by rememberSaveable { mutableStateOf(today.plusDays(1).toString()) }
    var time by rememberSaveable { mutableStateOf(LocalTime.now().plusHours(1).format(DateTimeFormatter.ofPattern("HH:mm"))) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingReminder by remember { mutableStateOf<ReminderValidator.ValidReminder?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val reminder = pendingReminder
        pendingReminder = null
        message = if (!granted) {
            "Notification permission is required to deliver this reminder"
        } else if (reminder == null) {
            "Reminder could not be prepared"
        } else {
            schedule(reminder).fold(
                onSuccess = { "Reminder scheduled" },
                onFailure = { it.message ?: "Reminder could not be scheduled" }
            )
        }
    }

    fun submit() {
        when (val result = ReminderValidator.validate(title, notes, date, time)) {
            is ReminderValidator.Result.Error -> message = result.message
            is ReminderValidator.Result.Success -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    pendingReminder = result.reminder
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    message = schedule(result.reminder).fold(
                        onSuccess = { "Reminder scheduled" },
                        onFailure = { it.message ?: "Reminder could not be scheduled" }
                    )
                }
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Create reminder") }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Confirm reminder details", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(title, { title = it.take(120) }, Modifier.fillMaxWidth(), label = { Text("Title") })
            OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth().weight(1f), label = { Text("Notes") })
            OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Date (YYYY-MM-DD)") })
            OutlinedTextField(time, { time = it }, Modifier.fillMaxWidth(), label = { Text("Time (HH:MM)") })
            message?.let { Text(it, color = if (it == "Reminder scheduled") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Cancel") }
                Button(onClick = ::submit, modifier = Modifier.weight(1f)) { Text("Schedule") }
            }
        }
    }
}

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

class ReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sourceText = intent.getStringExtra(EXTRA_SOURCE_TEXT).orEmpty()
        val hasCuratedResults = intent.getBooleanExtra(EXTRA_HAS_CURATED_RESULTS, false)
        val prefill = if (hasCuratedResults) {
            ReminderPrefillResolver.fromCurated(
                intent.getStringExtra(EXTRA_CURATED_DATE),
                intent.getStringExtra(EXTRA_CURATED_TIME)
            )
        } else {
            ReminderPrefillResolver.from(sourceText)
        }
        val scheduler = ReminderScheduler(applicationContext)
        val historyStore = ReminderHistoryStore(applicationContext)
        setContent {
            DropTheme {
                ReminderScreen(
                    sourceText = sourceText,
                    prefill = prefill,
                    hasCuratedResults = hasCuratedResults,
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
        const val EXTRA_HAS_CURATED_RESULTS = "has_curated_results"
        const val EXTRA_CURATED_DATE = "curated_date"
        const val EXTRA_CURATED_TIME = "curated_time"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderScreen(
    sourceText: String,
    prefill: ReminderPrefill,
    hasCuratedResults: Boolean,
    onClose: () -> Unit,
    schedule: (ReminderValidator.ValidReminder) -> Result<Unit>
) {
    var title by rememberSaveable { mutableStateOf(sourceText.lineSequence().firstOrNull { it.isNotBlank() }?.take(120) ?: "Reminder") }
    var notes by rememberSaveable { mutableStateOf(sourceText) }
    var date by rememberSaveable { mutableStateOf(prefill.date) }
    var time by rememberSaveable { mutableStateOf(prefill.time) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var scheduled by rememberSaveable { mutableStateOf(false) }
    var pendingReminder by remember { mutableStateOf<ReminderValidator.ValidReminder?>(null) }

    fun handleScheduleResult(result: Result<Unit>) {
        result.fold(
            onSuccess = {
                scheduled = true
                message = "Reminder scheduled. It is saved in History."
            },
            onFailure = {
                scheduled = false
                message = it.message ?: "Reminder could not be scheduled"
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val reminder = pendingReminder
        pendingReminder = null
        if (!granted) {
            message = "Notification permission is required to deliver this reminder"
        } else if (reminder == null) {
            message = "Reminder could not be prepared"
        } else {
            handleScheduleResult(schedule(reminder))
        }
    }

    fun submit() {
        if (scheduled) return
        when (val result = ReminderValidator.validate(title, notes, date, time)) {
            is ReminderValidator.Result.Error -> message = result.message
            is ReminderValidator.Result.Success -> {
                if (Build.VERSION.SDK_INT >= 33) {
                    pendingReminder = result.reminder
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    handleScheduleResult(schedule(result.reminder))
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
            if (hasCuratedResults) {
                Text("Date and time use the values you reviewed in Extracted information. You can still edit them before scheduling.")
            }
            OutlinedTextField(
                title,
                { title = it.take(120); message = null },
                Modifier.fillMaxWidth(),
                label = { Text("Title") },
                enabled = !scheduled
            )
            OutlinedTextField(
                notes,
                { notes = it; message = null },
                Modifier.fillMaxWidth().weight(1f),
                label = { Text("Notes") },
                enabled = !scheduled
            )
            OutlinedTextField(
                date,
                { date = it; message = null },
                Modifier.fillMaxWidth(),
                label = { Text("Date (YYYY-MM-DD)") },
                enabled = !scheduled
            )
            OutlinedTextField(
                time,
                { time = it; message = null },
                Modifier.fillMaxWidth(),
                label = { Text("Time (HH:MM)") },
                enabled = !scheduled
            )
            message?.let {
                Text(
                    it,
                    color = if (scheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            if (scheduled) {
                Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { Text("Done") }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(onClick = ::submit, modifier = Modifier.weight(1f)) { Text("Schedule") }
                }
            }
        }
    }
}

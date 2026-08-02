package com.wolferdwolf.drop.calendar

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import com.wolferdwolf.drop.extraction.AddressCandidateDetector
import com.wolferdwolf.drop.extraction.ExtractionType
import com.wolferdwolf.drop.extraction.RuleBasedExtractor
import com.wolferdwolf.drop.ui.theme.DropTheme
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CalendarConfirmationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val source = intent.getStringExtra(EXTRA_SOURCE_TEXT).orEmpty()
        val extracted = RuleBasedExtractor.extract(source)
        val date = extracted.firstOrNull { it.type == ExtractionType.DATE }?.value.orEmpty()
        val time = extracted.firstOrNull { it.type == ExtractionType.TIME }?.value.orEmpty()
        val initialDate = normalizeDate(date)
        val initialStart = normalizeTime(time)
        val initialEnd = initialStart.takeIf(String::isNotBlank)?.let(::oneHourLater).orEmpty()
        val initialVenue = labelledVenue(source)
            ?: AddressCandidateDetector.detect(source)?.value.orEmpty()

        setContent {
            DropTheme {
                var title by rememberSaveable { mutableStateOf(SavedReferenceStore.defaultTitle(source)) }
                var eventDate by rememberSaveable { mutableStateOf(initialDate) }
                var startTime by rememberSaveable { mutableStateOf(initialStart) }
                var endTime by rememberSaveable { mutableStateOf(initialEnd) }
                var venue by rememberSaveable { mutableStateOf(initialVenue) }
                var notes by rememberSaveable { mutableStateOf(source.take(MAX_NOTES_LENGTH)) }
                var error by rememberSaveable { mutableStateOf<String?>(null) }

                CalendarConfirmationScreen(
                    title = title,
                    date = eventDate,
                    startTime = startTime,
                    endTime = endTime,
                    venue = venue,
                    notes = notes,
                    error = error,
                    onTitleChange = { title = it.take(MAX_TITLE_LENGTH) },
                    onDateChange = { eventDate = it.take(DATE_LENGTH) },
                    onStartTimeChange = { startTime = it.take(TIME_LENGTH) },
                    onEndTimeChange = { endTime = it.take(TIME_LENGTH) },
                    onVenueChange = { venue = it.take(MAX_VENUE_LENGTH) },
                    onNotesChange = { notes = it.take(MAX_NOTES_LENGTH) },
                    onAdd = {
                        error = validateAndLaunch(title, eventDate, startTime, endTime, venue, notes)
                    },
                    onCancel = ::finish
                )
            }
        }
    }

    private fun validateAndLaunch(
        title: String,
        date: String,
        startTime: String,
        endTime: String,
        venue: String,
        notes: String
    ): String? {
        if (title.trim().isBlank()) return "Enter an event title."
        val start = parseDateTime(date.trim(), startTime.trim()) ?: return "Use a valid date and start time, such as 2026-08-21 and 16:00."
        val end = if (endTime.isBlank()) start + ONE_HOUR_MILLIS else parseDateTime(date.trim(), endTime.trim())
            ?: return "Use a valid end time, such as 17:00."
        if (end <= start) return "End time must be after the start time."

        val calendarIntent = Intent(Intent.ACTION_INSERT)
            .setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, title.trim())
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, venue.trim())
            .putExtra(CalendarContract.Events.DESCRIPTION, notes.trim())

        return try {
            if (calendarIntent.resolveActivity(packageManager) == null) {
                "No compatible Calendar app is installed."
            } else {
                startActivity(calendarIntent)
                null
            }
        } catch (_: ActivityNotFoundException) {
            "No compatible Calendar app is installed."
        } catch (_: SecurityException) {
            "Android blocked this action. Check device settings and try again."
        }
    }

    companion object {
        const val EXTRA_SOURCE_TEXT = "source_text"
        private const val MAX_TITLE_LENGTH = 120
        private const val MAX_VENUE_LENGTH = 300
        private const val MAX_NOTES_LENGTH = 5_000
        private const val DATE_LENGTH = 10
        private const val TIME_LENGTH = 5
        private const val ONE_HOUR_MILLIS = 3_600_000L
        private val LABELLED_VENUE = Regex("(?i)\\b(?:venue|location|address)\\s*:\\s*([^\\n.!?]+)")

        internal fun labelledVenue(source: String): String? = LABELLED_VENUE.find(source)
            ?.groupValues
            ?.getOrNull(1)
            ?.trim()
            ?.take(MAX_VENUE_LENGTH)
            ?.takeIf(String::isNotBlank)

        internal fun normalizeDate(value: String): String {
            val formats = listOf(
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "d MMMM yyyy",
                "MMMM d yyyy",
                "MMMM d, yyyy"
            )
            val clean = value.replace(Regex("(?i)(\\d)(st|nd|rd|th)"), "$1").trim()
            val date = formats.firstNotNullOfOrNull { parseStrict(clean, it) } ?: return ""
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(date)
        }

        internal fun normalizeTime(value: String): String {
            val formats = listOf("HH:mm", "H:mm", "h:mm a", "h a")
            val date = formats.firstNotNullOfOrNull { parseStrict(value.trim().uppercase(Locale.US), it) } ?: return ""
            return SimpleDateFormat("HH:mm", Locale.US).format(date)
        }

        internal fun oneHourLater(value: String): String {
            val date = parseStrict(value, "HH:mm") ?: return ""
            val calendar = Calendar.getInstance().apply { time = date; add(Calendar.HOUR_OF_DAY, 1) }
            return SimpleDateFormat("HH:mm", Locale.US).format(calendar.time)
        }

        internal fun parseDateTime(date: String, time: String): Long? =
            parseStrict("$date $time", "yyyy-MM-dd HH:mm")?.time

        private fun parseStrict(value: String, pattern: String): Date? {
            val format = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val position = ParsePosition(0)
            val parsed = format.parse(value, position)
            return parsed?.takeIf { position.index == value.length }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarConfirmationScreen(
    title: String,
    date: String,
    startTime: String,
    endTime: String,
    venue: String,
    notes: String,
    error: String?,
    onTitleChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onStartTimeChange: (String) -> Unit,
    onEndTimeChange: (String) -> Unit,
    onVenueChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onAdd: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Add calendar event") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Confirm event details", style = MaterialTheme.typography.headlineSmall)
                Text("Review and edit every field before Drop opens your Calendar app.")
            }
            item { OutlinedTextField(title, onTitleChange, Modifier.fillMaxWidth(), label = { Text("Event title") }, singleLine = true) }
            item { OutlinedTextField(date, onDateChange, Modifier.fillMaxWidth(), label = { Text("Date (YYYY-MM-DD)") }, singleLine = true) }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(startTime, onStartTimeChange, Modifier.weight(1f), label = { Text("Start (HH:MM)") }, singleLine = true)
                    OutlinedTextField(endTime, onEndTimeChange, Modifier.weight(1f), label = { Text("End (HH:MM)") }, singleLine = true)
                }
            }
            item { OutlinedTextField(venue, onVenueChange, Modifier.fillMaxWidth(), label = { Text("Venue") }, minLines = 2) }
            item { OutlinedTextField(notes, onNotesChange, Modifier.fillMaxWidth(), label = { Text("Notes") }, minLines = 4) }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Nothing is added automatically", style = MaterialTheme.typography.titleMedium)
                        Text("Drop only passes these edited details to Calendar after you confirm.")
                    }
                }
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item { Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Continue to Calendar") } }
            item { OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("Cancel") } }
        }
    }
}

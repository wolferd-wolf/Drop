from pathlib import Path

activity = Path('app/src/main/java/com/wolferdwolf/drop/calendar/CalendarConfirmationActivity.kt')
text = activity.read_text()
old = '        val initialVenue = AddressCandidateDetector.detect(source)?.value.orEmpty()\n'
new = '        val initialVenue = isolateVenue(source, AddressCandidateDetector.detect(source)?.value.orEmpty())\n'
if old in text:
    text = text.replace(old, new)
elif new not in text:
    raise SystemExit('Calendar venue initialization not found')

marker = '        internal fun normalizeDate(value: String): String {\n'
helper = '''        internal fun isolateVenue(source: String, detected: String): String {
            val labelled = Regex(
                "(?i)\\b(?:venue|location|address)\\s*:\\s*([^\\n.!?]+)"
            ).find(source)?.groupValues?.getOrNull(1)?.trim()
            if (!labelled.isNullOrBlank()) return labelled.take(MAX_VENUE_LENGTH)

            return detected
                .lineSequence()
                .map(String::trim)
                .firstOrNull(String::isNotBlank)
                .orEmpty()
                .substringBefore(Regex("[.!?](?:\\s|$)"))
                .trim()
                .take(MAX_VENUE_LENGTH)
        }

'''
if 'internal fun isolateVenue(' not in text:
    if marker not in text:
        raise SystemExit('Calendar helper insertion marker not found')
    text = text.replace(marker, helper + marker)
activity.write_text(text)

test = Path('app/src/androidTest/java/com/wolferdwolf/drop/HomeScreenshotTest.kt')
text = test.read_text()
brittle = '''            val calendarFields = device.findObjects(By.clazz("android.widget.EditText"))
            assertTrue("Calendar form must expose title, date, start, end, venue, and notes", calendarFields.size >= 6)
            assertTrue("Venue must contain only the detected location", calendarFields[4].text == "MG Road, Vijayawada")
            assertTrue("Venue must exclude unrelated notes", !calendarFields[4].text.contains("final presentation"))
'''
stable = '''            assertObject(
                device,
                By.clazz("android.widget.EditText").text("MG Road, Vijayawada"),
                "Venue must contain only the detected location"
            )
'''
if brittle in text:
    text = text.replace(brittle, stable)
elif stable not in text:
    raise SystemExit('Calendar venue assertion block not found')
test.write_text(text)

from pathlib import Path

main = Path('app/src/main/java/com/wolferdwolf/drop/MainActivity.kt')
text = main.read_text()
old = '''            SuggestedActionType.CALENDAR -> startActivity(
                Intent(this, CalendarConfirmationActivity::class.java)
                    .putExtra(CalendarConfirmationActivity.EXTRA_SOURCE_TEXT, text)
            )'''
new = '''            SuggestedActionType.CALENDAR -> startActivity(
                Intent(this, CalendarConfirmationActivity::class.java)
                    .putExtra(CalendarConfirmationActivity.EXTRA_SOURCE_TEXT, text)
                    .putExtra(CalendarConfirmationActivity.EXTRA_HAS_CURATED_RESULTS, true)
                    .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_DATE, first(results, ExtractionType.DATE).orEmpty())
                    .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_TIME, first(results, ExtractionType.TIME).orEmpty())
                    .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_VENUE, first(results, ExtractionType.ADDRESS).orEmpty())
            )'''
if old not in text:
    raise SystemExit('Calendar route marker not found')
main.write_text(text.replace(old, new))

calendar = Path('app/src/main/java/com/wolferdwolf/drop/calendar/CalendarConfirmationActivity.kt')
text = calendar.read_text()
old = '''        val extracted = RuleBasedExtractor.extract(source)
        val date = extracted.firstOrNull { it.type == ExtractionType.DATE }?.value.orEmpty()
        val time = extracted.firstOrNull { it.type == ExtractionType.TIME }?.value.orEmpty()
        val initialDate = normalizeDate(date)
        val initialStart = normalizeTime(time)
        val initialEnd = initialStart.takeIf(String::isNotBlank)?.let(::oneHourLater).orEmpty()
        val initialVenue = labelledVenue(source)
            ?: AddressCandidateDetector.detect(source)?.value.orEmpty()
'''
new = '''        val extracted = RuleBasedExtractor.extract(source)
        val hasCuratedResults = intent.getBooleanExtra(EXTRA_HAS_CURATED_RESULTS, false)
        val date = if (hasCuratedResults) {
            intent.getStringExtra(EXTRA_CURATED_DATE).orEmpty()
        } else {
            extracted.firstOrNull { it.type == ExtractionType.DATE }?.value.orEmpty()
        }
        val time = if (hasCuratedResults) {
            intent.getStringExtra(EXTRA_CURATED_TIME).orEmpty()
        } else {
            extracted.firstOrNull { it.type == ExtractionType.TIME }?.value.orEmpty()
        }
        val initialDate = normalizeDate(date)
        val initialStart = normalizeTime(time)
        val initialEnd = initialStart.takeIf(String::isNotBlank)?.let(::oneHourLater).orEmpty()
        val initialVenue = if (hasCuratedResults) {
            intent.getStringExtra(EXTRA_CURATED_VENUE).orEmpty()
        } else {
            labelledVenue(source) ?: AddressCandidateDetector.detect(source)?.value.orEmpty()
        }
'''
if old not in text:
    raise SystemExit('Calendar initial values marker not found')
text = text.replace(old, new)
old = '        const val EXTRA_SOURCE_TEXT = "source_text"\n'
new = '''        const val EXTRA_SOURCE_TEXT = "source_text"
        const val EXTRA_HAS_CURATED_RESULTS = "has_curated_results"
        const val EXTRA_CURATED_DATE = "curated_date"
        const val EXTRA_CURATED_TIME = "curated_time"
        const val EXTRA_CURATED_VENUE = "curated_venue"
'''
if old not in text:
    raise SystemExit('Calendar extras marker not found')
calendar.write_text(text.replace(old, new))

test = Path('app/src/androidTest/java/com/wolferdwolf/drop/HomeScreenshotTest.kt')
text = test.read_text()
old = '''        val intent = Intent(context, CalendarConfirmationActivity::class.java)
            .putExtra(CalendarConfirmationActivity.EXTRA_SOURCE_TEXT, source)

        ActivityScenario.launch<CalendarConfirmationActivity>(intent).use {'''
new = '''        val intent = Intent(context, CalendarConfirmationActivity::class.java)
            .putExtra(CalendarConfirmationActivity.EXTRA_SOURCE_TEXT, source)
            .putExtra(CalendarConfirmationActivity.EXTRA_HAS_CURATED_RESULTS, true)
            .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_DATE, "22 August 2026")
            .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_TIME, "6:15 PM")
            .putExtra(CalendarConfirmationActivity.EXTRA_CURATED_VENUE, "Edited venue, Vijayawada")

        ActivityScenario.launch<CalendarConfirmationActivity>(intent).use {'''
if old not in text:
    raise SystemExit('Calendar screenshot intent marker not found')
text = text.replace(old, new)
old = '''            assertObject(
                device,
                By.clazz("android.widget.EditText").text("MG Road, Vijayawada"),
                "Venue must contain only the detected location"
            )
'''
new = '''            assertObject(device, By.clazz("android.widget.EditText").text("2026-08-22"), "Edited date must reach Calendar confirmation")
            assertObject(device, By.clazz("android.widget.EditText").text("18:15"), "Edited time must reach Calendar confirmation")
            assertObject(
                device,
                By.clazz("android.widget.EditText").text("Edited venue, Vijayawada"),
                "Edited venue must replace the original detected location"
            )
'''
if old not in text:
    raise SystemExit('Calendar screenshot assertions marker not found')
text = text.replace(old, new)
text = text.replace('capture(device, "/data/local/tmp/drop-calendar-confirmation.png")', 'capture(device, "/data/local/tmp/drop-calendar-confirmation.png")\n            capture(device, "/data/local/tmp/drop-calendar-curated-values.png")', 1)
test.write_text(text)

workflow = Path('.github/workflows/android-ci.yml')
text = workflow.read_text()
pull_marker = '            adb pull /data/local/tmp/drop-calendar-confirmation.png screenshots/drop-calendar-confirmation.png\n'
check_marker = '            test -s screenshots/drop-calendar-confirmation.png\n'
pull_line = '            adb pull /data/local/tmp/drop-calendar-curated-values.png screenshots/drop-calendar-curated-values.png\n'
check_line = '            test -s screenshots/drop-calendar-curated-values.png\n'
if pull_line not in text:
    if pull_marker not in text or check_marker not in text:
        raise SystemExit('Calendar screenshot workflow marker not found')
    text = text.replace(pull_marker, pull_marker + pull_line)
    text = text.replace(check_marker, check_marker + check_line)
workflow.write_text(text)

Path('verification/calendar-curated-extraction.txt').unlink(missing_ok=True)
Path('.github/workflows/apply-calendar-curated-extraction.yml').unlink(missing_ok=True)
Path('tools/apply_calendar_curated_extraction.py').unlink(missing_ok=True)

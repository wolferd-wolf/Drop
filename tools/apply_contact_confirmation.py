from pathlib import Path

main = Path('app/src/main/java/com/wolferdwolf/drop/MainActivity.kt')
text = main.read_text()
text = text.replace('import android.provider.ContactsContract\n', '')
if 'import com.wolferdwolf.drop.contact.ContactConfirmationActivity\n' not in text:
    text = text.replace(
        'import com.wolferdwolf.drop.calendar.CalendarConfirmationActivity\n',
        'import com.wolferdwolf.drop.calendar.CalendarConfirmationActivity\nimport com.wolferdwolf.drop.contact.ContactConfirmationActivity\n'
    )
old = '''            SuggestedActionType.CONTACT -> launch(
                Intent(Intent.ACTION_INSERT).setType(ContactsContract.Contacts.CONTENT_TYPE)
                    .putExtra(ContactsContract.Intents.Insert.PHONE, first(results, ExtractionType.PHONE))
                    .putExtra(ContactsContract.Intents.Insert.EMAIL, first(results, ExtractionType.EMAIL))
                    .putExtra(ContactsContract.Intents.Insert.NOTES, text)
            )'''
new = '''            SuggestedActionType.CONTACT -> startActivity(
                Intent(this, ContactConfirmationActivity::class.java)
                    .putExtra(ContactConfirmationActivity.EXTRA_SOURCE_TEXT, text)
            )'''
if old not in text and new not in text:
    raise SystemExit('Contact routing block not found')
main.write_text(text.replace(old, new))

manifest = Path('app/src/main/AndroidManifest.xml')
text = manifest.read_text()
marker = '''        <activity
            android:name=".calendar.CalendarConfirmationActivity"
            android:exported="false" />
'''
addition = '''        <activity
            android:name=".contact.ContactConfirmationActivity"
            android:exported="false" />
'''
if addition not in text:
    if marker not in text:
        raise SystemExit('Calendar activity marker not found')
    text = text.replace(marker, marker + addition)
manifest.write_text(text)

unit = Path('app/src/test/java/com/wolferdwolf/drop/contact/ContactConfirmationActivityTest.kt')
unit.parent.mkdir(parents=True, exist_ok=True)
unit.write_text('''package com.wolferdwolf.drop.contact

import org.junit.Assert.assertEquals
import org.junit.Test

class ContactConfirmationActivityTest {
    @Test
    fun extractsExplicitNameAndCompanyLabels() {
        val source = "Name: Priya Reddy\\nCompany: Wolf Labs\\nPhone: +91 98765 43210"
        assertEquals("Priya Reddy", ContactConfirmationActivity.labelledValue(source, ContactConfirmationActivity.NAME_LABELS))
        assertEquals("Wolf Labs", ContactConfirmationActivity.labelledValue(source, ContactConfirmationActivity.COMPANY_LABELS))
    }

    @Test
    fun doesNotGuessUnlabelledNames() {
        assertEquals("", ContactConfirmationActivity.labelledValue("Call Priya tomorrow", ContactConfirmationActivity.NAME_LABELS))
    }
}
''')

instrumentation = Path('app/src/androidTest/java/com/wolferdwolf/drop/ContactConfirmationScreenshotTest.kt')
instrumentation.write_text('''package com.wolferdwolf.drop

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.wolferdwolf.drop.contact.ContactConfirmationActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactConfirmationScreenshotTest {
    @Test
    fun captureEditableContactConfirmation() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val source = "Name: Priya Reddy\\nCompany: Wolf Labs\\nPhone: +91 98765 43210\\nEmail: priya@example.com\\nMet at the launch event."
        val intent = Intent(context, ContactConfirmationActivity::class.java)
            .putExtra(ContactConfirmationActivity.EXTRA_SOURCE_TEXT, source)

        ActivityScenario.launch<ContactConfirmationActivity>(intent).use {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            assertVisible(device, "Save contact", "Contact confirmation must reach the foreground")
            assertVisible(device, "Confirm contact details", "Contact confirmation must explain the review step")
            assertVisible(device, "Review and edit every field before Drop opens your Contacts app.", "Contact confirmation must explain user control")
            assertObject(device, By.clazz("android.widget.EditText").text("Priya Reddy"), "Detected name must be editable")
            assertObject(device, By.clazz("android.widget.EditText").text("+91 98765 43210"), "Detected phone must be editable")
            assertObject(device, By.clazz("android.widget.EditText").text("priya@example.com"), "Detected email must be editable")
            assertObject(device, By.clazz("android.widget.EditText").text("Wolf Labs"), "Detected company must be editable")
            assertVisibleAfterScroll(device, "Continue to Contacts", "Contact confirmation must require explicit continuation")
            assertVisibleAfterScroll(device, "Cancel", "Contact confirmation must be reversible")
            capture(device, "/data/local/tmp/drop-contact-confirmation.png")
        }
    }

    private fun capture(device: UiDevice, path: String) {
        device.waitForIdle()
        device.executeShellCommand("rm -f $path")
        device.executeShellCommand("screencap -p $path")
        assertTrue(device.executeShellCommand("ls -l $path").contains(path.substringAfterLast('/')))
    }

    private fun assertVisible(device: UiDevice, text: String, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(By.text(text)), CONTROL_TIMEOUT_MILLIS))
            .let { device.findObject(By.text(text)) }

    private fun assertVisibleAfterScroll(device: UiDevice, text: String, message: String): UiObject2 {
        device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT_MILLIS)?.let { return it }
        repeat(MAX_SCROLL_ATTEMPTS) {
            device.swipe(device.displayWidth / 2, device.displayHeight * 3 / 4, device.displayWidth / 2, device.displayHeight / 4, 20)
            device.waitForIdle()
            device.wait(Until.findObject(By.text(text)), SHORT_TIMEOUT_MILLIS)?.let { return it }
        }
        return device.findObject(By.text(text)) ?: throw AssertionError(message)
    }

    private fun assertObject(device: UiDevice, selector: androidx.test.uiautomator.BySelector, message: String): UiObject2 =
        assertNotNull(message, device.wait(Until.findObject(selector), CONTROL_TIMEOUT_MILLIS))
            .let { device.findObject(selector) }

    private companion object {
        const val CONTROL_TIMEOUT_MILLIS = 20_000L
        const val SHORT_TIMEOUT_MILLIS = 2_000L
        const val MAX_SCROLL_ATTEMPTS = 6
    }
}
''')

ci = Path('.github/workflows/android-ci.yml')
text = ci.read_text()
start_marker = '  # CONTACT_INTEGRATION_START\n'
end_marker = '  # CONTACT_INTEGRATION_END\n'
if start_marker in text and end_marker in text:
    before, rest = text.split(start_marker, 1)
    _, after = rest.split(end_marker, 1)
    text = before + after
text = text.replace('permissions:\n  contents: write', 'permissions:\n  contents: read')
pull_marker = '            adb pull /data/local/tmp/drop-calendar-confirmation.png screenshots/drop-calendar-confirmation.png\n'
pull_line = '            adb pull /data/local/tmp/drop-contact-confirmation.png screenshots/drop-contact-confirmation.png\n'
check_marker = '            test -s screenshots/drop-calendar-confirmation.png\n'
check_line = '            test -s screenshots/drop-contact-confirmation.png\n'
if pull_line not in text:
    text = text.replace(pull_marker, pull_marker + pull_line)
if check_line not in text:
    text = text.replace(check_marker, check_marker + check_line)
ci.write_text(text)

for path in [
    '.github/workflows/apply-contact-confirmation.yml',
    '.github/workflows/apply-contact-target.yml',
    '.github/workflows/run-contact-integration.yml',
    'verification/contact-confirmation.txt',
    'tools/apply_contact_confirmation.py'
]:
    Path(path).unlink(missing_ok=True)

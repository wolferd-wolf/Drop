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
Path('verification/contact-confirmation.txt').unlink(missing_ok=True)
Path('tools/apply_contact_confirmation.py').unlink(missing_ok=True)

# Drop — Structured Document Profiles

Drop should not stop at extracting loose text. When the input has a recognisable structure, Drop should preserve that structure and turn it into a reusable local object.

## First profile: Timetable

A timetable image becomes:

- a title
- editable time-and-label rows
- a preserved OCR source
- a structured local record
- a direct path to reminders and calendar actions

## Growth path

The same profile architecture will support:

1. Event posters — title, date, start/end time, venue, contact, calendar action.
2. Receipts — merchant, date, line items, total, warranty/reference actions.
3. Job posts — role, company, deadline, contact, requirements, apply/reminder actions.
4. Medicine schedules — medicine, dose, recurrence, safety confirmation, reminder actions.
5. Forms and notices — fields, deadlines, contacts, checklist and saved-reference actions.

## Product rules

- Classification stays offline and deterministic in Version 1.
- The user always reviews detected structure before saving or creating actions.
- Unknown documents fall back to the general extraction flow.
- Each profile owns its validation, editing, persistence, and action mapping.
- Structured objects must remain exportable as readable text.
- No profile may silently create reminders, events, contacts, or external actions.

## Engineering direction

- `DocumentProfileClassifier` chooses a profile from OCR or imported text.
- Each profile has a typed model and parser.
- Profile-specific review screens edit typed fields rather than raw text.
- Structured records are stored locally and surfaced in Home/history.
- Generic extraction remains the fallback and shared action layer.

## Near-term tasks

- Persist structured timetables locally.
- Show saved timetables in Home/history.
- Add timetable detail and editing.
- Add multi-select reminder creation from timetable rows.
- Add recurrence/day selection before scheduling.
- Add event-poster profile next.
- Move structured persistence to Room with the rest of action history.

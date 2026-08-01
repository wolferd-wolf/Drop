# Drop — Product and Development Plan

## Product promise

**Turn anything on your phone into the next useful action.**

Drop is an offline-first Android action inbox. A user shares an image, screenshot, text, link, or PDF into Drop. Drop extracts useful information and offers a small number of clear actions.

## Non-negotiable principles

1. Offline-first. Core extraction and saved data must work without an account or cloud service.
2. Privacy-first. User content remains on-device unless the user explicitly exports it.
3. Trust over cleverness. Drop must show extracted information before creating anything.
4. Narrow scope. Drop is not a general notes app, chatbot, or full task manager.
5. Fast intake. Sharing content into Drop must feel immediate.
6. Reversible actions. Users must be able to review, edit, cancel, and remove saved results.
7. No milestone is complete until CI passes and the feature is usable on a real Android device.

## Version 1 scope

### Inputs

- Image and screenshot
- Shared text
- Website link
- PDF

### Outputs

- Reminder
- Calendar event
- Checklist
- Contact
- Maps location
- Saved reference

### Extraction targets

- Dates
- Times
- Phone numbers
- Email addresses
- URLs
- Prices and currencies
- Addresses and venue-like text
- Names and titles where confidence is sufficient
- Basic document category such as event, job post, receipt, or general reference

## Core screens

1. Home / Action history
2. Share preview
3. Extracted information
4. Suggested actions
5. Action confirmation and editing
6. Saved items
7. Search
8. Settings and privacy

## Technical direction

- Native Android
- Kotlin
- Jetpack Compose
- Material 3 with a restrained custom visual system
- Clean Architecture with clear UI, domain, and data boundaries
- Room for local persistence
- Android Sharesheet and intent handling
- ML Kit or equivalent on-device OCR
- Rule-based extraction first
- WorkManager and local notifications for reminders
- Android calendar, contacts, Maps, browser, email, and dialer intents where appropriate
- No arbitrary code execution
- No required cloud backend for Version 1

## Quality gates

Every pull request and main-branch change must satisfy the applicable gates:

- Project compiles
- Unit tests pass
- Android lint passes
- Static analysis passes
- Debug APK is produced by GitHub Actions
- No known critical crash in the primary flow
- Empty, loading, error, and permission states are handled
- Accessibility labels exist for interactive controls
- User content is not silently transmitted
- New functionality has tests for its core business rules

A successful CI build alone does not prove a feature is complete. Real-device testing is required for user-facing milestones.

## Milestones

### M0 — Repository and CI foundation

Deliverables:

- Android project skeleton
- Gradle configuration and version catalog
- Basic Compose application
- GitHub Actions workflow for build, unit tests, lint, and APK artifact upload
- Test and architecture conventions
- README with build and testing instructions

Exit criteria:

- CI succeeds from a clean checkout
- Debug APK is available as a workflow artifact
- App launches to a stable placeholder home screen

### M1 — Share intake foundation

Deliverables:

- Receive shared plain text
- Receive shared images
- Receive links
- Receive PDFs through content URIs
- Preview incoming content
- Handle unsupported, missing, and inaccessible content safely

Exit criteria:

- Content can be shared into Drop from common Android apps
- Drop never loses the original shared item before confirmation
- Permission and URI errors are explained clearly

### M2 — Offline extraction engine

Deliverables:

- On-device OCR for images and PDFs where supported
- Extraction of dates, times, phones, emails, URLs, prices, and candidate addresses
- Confidence and source-range model
- Editable extraction results
- Unit tests with representative examples

Exit criteria:

- Extraction is deterministic and testable
- Low-confidence fields are visibly identified
- Original text and extracted values can be compared

### M3 — Action creation

Deliverables:

- Reminder creation
- Calendar event creation
- Checklist creation
- Contact creation
- Maps action
- Saved reference action

Exit criteria:

- Every action has a confirmation screen
- Users can edit extracted values before execution
- Failed external intents do not crash the app

### M4 — Saved inbox and search

Deliverables:

- Action history
- Saved references
- Search and filters
- Item detail and deletion
- Local data export format

Exit criteria:

- Saved data survives app restarts
- Search works across extracted text and metadata
- Deletion behaviour is explicit and reliable

### M5 — Product-quality pass

Deliverables:

- Final visual system
- Onboarding and permission explanations
- Accessibility pass
- Performance and memory review
- Privacy page
- Error reporting that does not expose user content
- Device-test checklist

Exit criteria:

- Primary workflows are polished, consistent, and understandable without instructions
- No open critical or high-severity defect
- Release candidate APK passes the full device-test checklist

## Initial workflow priorities

The first vertical slice will be:

1. Share text into Drop.
2. Detect a date, time, phone, email, and link.
3. Display extracted information.
4. Create a reminder or saved reference.
5. Save the result to action history.

This slice will establish the architecture before image OCR, PDFs, and broader action support are added.

## Defect policy

Priority order:

1. Data loss, privacy leak, security issue, or crash
2. Broken primary workflow
3. Incorrect extraction or action creation
4. Accessibility or severe usability problem
5. Visual inconsistency
6. Enhancements

Critical and high-severity defects block new feature development.

## Scope control

Features outside Version 1 require an explicit plan update. The following are deferred:

- Cloud AI dependency
- Accounts and mandatory sync
- Team inboxes
- Automatic processing without user confirmation
- User-created automation chains
- Morphic and Object Memory integration
- Premium packs and payments

## Working agreement

- Development is performed through the repository and GitHub Actions.
- The user installs and tests APK artifacts on a real Android device.
- Reported bugs are reproduced, prioritised, fixed, and verified through CI.
- The plan is updated when product direction changes.
- Features are not described as finished until their exit criteria are met.

# Drop — Strict Development Task List

This file is the execution checklist for `PROJECT_PLAN.md`. Tasks must be completed in order unless a blocking defect requires immediate work.

## Status rules

- `[ ]` Not started
- `[-]` In progress
- `[x]` Completed and verified
- `[!]` Blocked

A task may be marked complete only when its acceptance criteria are satisfied. User-facing tasks also require real-device confirmation.

---

## M0 — Repository and CI foundation

### Project foundation

- [ ] Create native Android project using Kotlin and Jetpack Compose.
  - Acceptance: Project opens from a clean checkout and compiles without local-only files.
- [ ] Configure application ID, minimum SDK, target SDK, versioning, and app name.
  - Acceptance: Values are documented and consistent across Gradle and manifest files.
- [ ] Add Gradle wrapper and version catalog.
  - Acceptance: Dependencies and plugin versions are centrally managed.
- [ ] Establish package structure for UI, domain, data, extraction, actions, and shared intake.
  - Acceptance: Architecture boundaries are documented and no feature logic is placed directly in activities.
- [ ] Add Material 3 Compose theme with light and dark mode support.
  - Acceptance: Placeholder screens render correctly in both modes.
- [ ] Create stable placeholder Home screen.
  - Acceptance: App launches without crash and survives recreation.

### Engineering quality

- [ ] Configure Kotlin compiler checks and warnings.
- [ ] Configure Android lint.
- [ ] Add static analysis and formatting checks.
- [ ] Add unit-test framework and initial smoke test.
- [ ] Add dependency-license review process.
- [ ] Add `.gitignore`, editor settings, and repository conventions.

### GitHub Actions

- [ ] Create CI workflow for clean debug build.
- [ ] Run unit tests in CI.
- [ ] Run Android lint in CI.
- [ ] Run static analysis and formatting verification in CI.
- [ ] Upload debug APK as a workflow artifact.
- [ ] Preserve useful reports when CI fails.
- [ ] Add workflow concurrency control to cancel obsolete runs.

### Documentation

- [ ] Create README with product summary, architecture, build steps, test steps, and APK download instructions.
- [ ] Add contribution and commit conventions.
- [ ] Add manual device-test template.

### M0 exit gate

- [ ] Clean GitHub Actions run passes.
- [ ] Debug APK artifact is downloadable.
- [ ] APK installs and opens on the user’s Android phone.
- [ ] No known launch crash or broken placeholder navigation.

---

## Vertical Slice A — Shared text to useful action

This slice must be completed before broadening the product.

### Shared-text intake

- [ ] Register Drop as an Android share target for plain text.
- [ ] Receive `ACTION_SEND` text safely.
- [ ] Distinguish shared text from shared URL text.
- [ ] Preserve original shared content until the user saves or discards it.
- [ ] Handle null, blank, malformed, and excessively large shared text.
- [ ] Add clear unsupported-input and retry states.

### Share preview UI

- [ ] Build Share Preview screen.
- [ ] Display source type and original content.
- [ ] Allow user to edit or remove the imported content.
- [ ] Add explicit Continue and Discard actions.
- [ ] Handle configuration changes without losing imported content.
- [ ] Add accessibility labels and logical focus order.

### Rule-based extraction

- [ ] Define extraction result model with type, value, source range, and confidence.
- [ ] Extract phone numbers.
- [ ] Extract email addresses.
- [ ] Extract web links.
- [ ] Extract common dates.
- [ ] Extract common times.
- [ ] Prevent obvious duplicate detections.
- [ ] Preserve the exact source text used for each result.
- [ ] Add representative unit tests, including Indian formats.

### Extracted-information UI

- [ ] Build Extracted Information screen.
- [ ] Group results by type.
- [ ] Visibly distinguish low-confidence values.
- [ ] Allow values to be edited or removed.
- [ ] Allow users to compare extracted values with original text.
- [ ] Handle “nothing useful found” without dead-ending the user.

### First actions

- [ ] Implement Saved Reference action.
- [ ] Implement Reminder action using local notifications.
- [ ] Build confirmation screen for each action.
- [ ] Require user confirmation before action execution.
- [ ] Validate reminder date and time before scheduling.
- [ ] Handle notification permission state correctly.
- [ ] Show success and failure feedback.

### Local history

- [ ] Add Room database.
- [ ] Define saved item and action-history entities.
- [ ] Store original shared content safely.
- [ ] Store extraction results and completed action metadata.
- [ ] Build minimal Action History screen.
- [ ] Support viewing and deleting history entries.
- [ ] Verify persistence across app restarts.

### Vertical Slice A exit gate

- [ ] Share text from at least three common Android apps into Drop.
- [ ] Detect date, time, phone, email, and link from representative samples.
- [ ] Create a reminder after editable confirmation.
- [ ] Save a reference after editable confirmation.
- [ ] Store both actions in history.
- [ ] All automated checks pass.
- [ ] User verifies the workflow on a real device.
- [ ] No open critical or high-severity defect.

---

## M1 — Complete share intake foundation

### Image intake

- [ ] Register image MIME types in share target.
- [ ] Receive single image through a content URI.
- [ ] Persist temporary URI access where permitted.
- [ ] Create a safe local copy when persistent access is unavailable.
- [ ] Display image preview with loading and error states.
- [ ] Enforce file-size and decode-memory protections.

### Link intake

- [ ] Recognise direct shared website links.
- [ ] Extract page title from share metadata where available.
- [ ] Never require network access merely to save a shared link.
- [ ] Provide Open in browser and Save reference options.

### PDF intake

- [ ] Register PDF MIME type.
- [ ] Receive PDF through a content URI.
- [ ] Display filename, size, and page count where available.
- [ ] Handle inaccessible, password-protected, malformed, and oversized PDFs safely.
- [ ] Preserve the original PDF until confirmation.

### Intake hardening

- [ ] Handle repeated shares while Drop is already open.
- [ ] Handle process death and restoration.
- [ ] Remove abandoned temporary files safely.
- [ ] Add tests for intake routing and validation.

### M1 exit gate

- [ ] Shared text, image, link, and PDF each open the correct preview.
- [ ] Unsupported or inaccessible content produces a useful explanation, not a crash.
- [ ] Original content is never silently lost before confirmation.
- [ ] User verifies each supported input on a real device.

---

## M2 — Offline extraction engine

### OCR foundation

- [ ] Integrate an on-device OCR engine with no mandatory cloud call.
- [ ] Run OCR on shared images.
- [ ] Add image rotation and orientation handling.
- [ ] Add image downsampling and memory safeguards.
- [ ] Display OCR progress and cancellation state.
- [ ] Preserve OCR text with source metadata.

### PDF text extraction

- [ ] Extract embedded text from text-based PDFs.
- [ ] Detect image-only PDFs.
- [ ] Add bounded OCR support for image-only PDF pages.
- [ ] Prevent unbounded processing of very large documents.
- [ ] Allow partial results when some pages fail.

### Extraction targets

- [ ] Improve date parsing across numeric and written formats.
- [ ] Improve time parsing, including 12-hour and 24-hour formats.
- [ ] Improve Indian and international phone-number parsing.
- [ ] Extract prices and currencies.
- [ ] Detect address and venue candidates.
- [ ] Detect names and titles only when confidence is sufficient.
- [ ] Add basic document categories: event, job post, receipt, and general reference.
- [ ] Add deterministic confidence scoring.
- [ ] Add conflict resolution for overlapping detections.

### Extraction testing

- [ ] Build a reusable corpus of representative samples.
- [ ] Include clean, noisy, rotated, low-resolution, and multilingual-adjacent samples.
- [ ] Add regression tests for every confirmed extraction bug.
- [ ] Measure false positives as well as missed detections.

### M2 exit gate

- [ ] Image OCR works fully offline.
- [ ] Text PDFs are processed offline.
- [ ] Image-only PDFs fail gracefully or produce bounded OCR results.
- [ ] Extracted values remain editable and traceable to source text.
- [ ] Low-confidence results are clearly identified.
- [ ] No input causes uncontrolled memory use or app crash in tested limits.

---

## M3 — Complete action creation

### Reminder

- [ ] Support date-only and date-time reminders.
- [ ] Support editing title, notes, date, and time.
- [ ] Handle timezone and daylight-saving changes correctly.
- [ ] Support rescheduling and cancellation.
- [ ] Deep-link notification back to the saved item.

### Calendar event

- [ ] Build event confirmation form.
- [ ] Support title, date, start time, end time, venue, and notes.
- [ ] Launch calendar insert intent safely.
- [ ] Handle devices without a compatible calendar app.

### Checklist

- [ ] Generate checklist candidates from detected lines.
- [ ] Allow add, edit, reorder, check, and delete.
- [ ] Save checklist locally.
- [ ] Prevent empty checklist creation.

### Contact

- [ ] Build contact confirmation form.
- [ ] Support name, phone, email, organisation, and notes.
- [ ] Launch contact insert intent safely.
- [ ] Handle partial contacts and duplicate-looking data clearly.

### Maps

- [ ] Build address confirmation view.
- [ ] Launch geo or Maps intent safely.
- [ ] Offer text search when exact coordinates are unavailable.
- [ ] Handle devices without a compatible map app.

### Saved reference

- [ ] Save text, source file metadata, extracted fields, tags, and notes.
- [ ] Preserve a safe reference to or copy of shared media.
- [ ] Allow later editing.

### Suggested-action engine

- [ ] Rank only a small number of relevant actions.
- [ ] Explain why each action is suggested.
- [ ] Avoid suggesting actions that lack required data.
- [ ] Allow the user to choose any supported action manually.
- [ ] Add tests for action-ranking rules.

### M3 exit gate

- [ ] All six Version 1 outputs are usable.
- [ ] Every action has an editable confirmation step.
- [ ] Failed external intents never crash Drop.
- [ ] All action results are recorded consistently in history.
- [ ] User verifies each action on a real device.

---

## M4 — Saved inbox and search

### Home and history

- [ ] Build polished Home / Action History screen.
- [ ] Show recent items with clear source and action status.
- [ ] Add loading, empty, and error states.
- [ ] Add filter by source type, action type, and date.

### Saved items

- [ ] Build Saved Items screen.
- [ ] Build item-detail screen.
- [ ] Support editing metadata and notes.
- [ ] Support explicit deletion with confirmation.
- [ ] Define attachment cleanup behaviour.

### Search

- [ ] Add full-text search across original text, OCR text, titles, notes, and extracted metadata.
- [ ] Add search highlighting.
- [ ] Add filters and sorting.
- [ ] Keep search responsive with a realistic local dataset.

### Export and backup

- [ ] Define a versioned local export format.
- [ ] Export selected or all records.
- [ ] Include attachments safely where selected.
- [ ] Add restore validation and duplicate handling.
- [ ] Document what is and is not included in backups.

### M4 exit gate

- [ ] Saved data survives restart and app upgrade testing.
- [ ] Search returns accurate results across supported fields.
- [ ] Deletion removes the intended records and attachments only.
- [ ] Export can be read back without silent data loss.

---

## M5 — Product-quality pass

### Visual system

- [ ] Finalise brand identity, icon, typography, spacing, shape, and colour tokens.
- [ ] Apply consistent component styling across every screen.
- [ ] Verify light and dark modes.
- [ ] Remove placeholder visuals and debug text.

### Onboarding and guidance

- [ ] Create concise onboarding.
- [ ] Explain how to use Android’s share sheet.
- [ ] Explain offline processing and privacy.
- [ ] Add permission explanations before system prompts.
- [ ] Add contextual help without cluttering the primary flow.

### Accessibility

- [ ] Verify screen-reader labels and reading order.
- [ ] Verify touch-target sizes.
- [ ] Verify text scaling.
- [ ] Verify contrast.
- [ ] Verify keyboard and switch navigation where applicable.
- [ ] Remove colour-only status communication.

### Privacy and security

- [ ] Create in-app privacy page.
- [ ] Confirm no user content is transmitted silently.
- [ ] Audit logs and crash reports for content leakage.
- [ ] Secure exported data warnings and sharing flows.
- [ ] Review URI, file-provider, and intent exposure.
- [ ] Review third-party dependencies and permissions.

### Reliability and performance

- [ ] Profile startup time.
- [ ] Profile image OCR memory use.
- [ ] Profile large PDF handling.
- [ ] Test process death and state restoration.
- [ ] Test low-storage behaviour.
- [ ] Test revoked permissions and missing external apps.
- [ ] Test interrupted imports and repeated actions.
- [ ] Remove main-thread blocking work.

### Release readiness

- [ ] Create full device-test checklist.
- [ ] Run regression suite.
- [ ] Resolve all critical and high-severity defects.
- [ ] Review medium defects and explicitly accept or fix each one.
- [ ] Produce release-candidate APK through GitHub Actions.
- [ ] Verify install, upgrade, uninstall, and reinstall behaviour.
- [ ] Finalise README, privacy documentation, and release notes.

### M5 exit gate

- [ ] Primary workflows are understandable without external instructions.
- [ ] No open critical or high-severity defect.
- [ ] Release-candidate CI passes completely.
- [ ] User completes the full real-device checklist successfully.
- [ ] Version 1 scope is complete without unfinished placeholder functionality.

---

## Deferred until after Version 1

These are not to be built unless `PROJECT_PLAN.md` is intentionally revised:

- [ ] Cloud AI dependency
- [ ] User accounts or mandatory sync
- [ ] Automatic actions without confirmation
- [ ] Team or family inbox
- [ ] User-created automation chains
- [ ] Morphic integration
- [ ] Object Memory integration
- [ ] Payments and premium packs
- [ ] Full regional-language model support
- [ ] Arbitrary plugin or code execution

---

## Defect queue rules

When a defect is reported:

1. Reproduce or isolate it.
2. Assign severity.
3. Add a regression test where practical.
4. Fix the root cause, not only the visible symptom.
5. Run all applicable CI checks.
6. Produce a new APK artifact.
7. Require real-device verification for user-facing defects.

Critical and high-severity defects block new feature work.
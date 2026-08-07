#!/usr/bin/env bash
set +e

# Hosted API 35 emulators can report boot-complete while the keyguard is still
# covering the app. UiAutomator then sees no Compose nodes even though the
# activity is RESUMED. Wake and dismiss it explicitly before instrumentation.
adb shell input keyevent KEYCODE_WAKEUP || true
adb shell wm dismiss-keyguard || true
adb shell input keyevent 82 || true
adb shell settings put system screen_off_timeout 2147483647 || true
adb shell settings put global stay_on_while_plugged_in 7 || true
adb shell input keyevent KEYCODE_HOME || true
adb shell dumpsys window | grep -E 'mDreamingLockscreen|mShowingLockscreen|isKeyguardShowing' || true

gradle connectedDebugAndroidTest --stacktrace
test_status=$?

if [ "$test_status" -eq 0 ]; then
  # connectedDebugAndroidTest uninstalls its APKs after the suite. Reinstall both
  # packages before running the independent process-restart phases directly.
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  test_status=$?

  if [ "$test_status" -eq 0 ]; then
    adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    test_status=$?
  fi
fi

if [ "$test_status" -eq 0 ]; then
  history_test='com.wolferdwolf.drop.HistoryDeletionFlowTest#verifyHistoryPersistencePhase'
  runner='com.wolferdwolf.drop.test/androidx.test.runner.AndroidJUnitRunner'

  adb shell am instrument -w -r -e class "$history_test" -e historyPhase save "$runner"
  test_status=$?

  if [ "$test_status" -eq 0 ]; then
    adb shell am force-stop com.wolferdwolf.drop
    adb shell am instrument -w -r -e class "$history_test" -e historyPhase restore-delete "$runner"
    test_status=$?
  fi

  if [ "$test_status" -eq 0 ]; then
    adb shell am force-stop com.wolferdwolf.drop
    adb shell am instrument -w -r -e class "$history_test" -e historyPhase verify-deletion "$runner"
    test_status=$?
  fi
fi

mkdir -p screenshots
screenshots=(
  drop-home
  drop-history
  drop-suggested-actions
  drop-all-actions
  drop-maps-suggestion
  drop-maps-confirmation
  drop-calendar-confirmation
  drop-calendar-curated-values
  drop-calendar-normal-flow
  drop-calendar-missing-date-suppressed
  drop-contact-confirmation
  drop-email-confirmation
  drop-open-link-confirmation
  drop-call-confirmation
  drop-image-review
  drop-pdf-review
  drop-paste-intake-actions
  drop-link-intake-actions
  drop-image-intake-actions
  drop-pdf-intake-actions
  drop-modern-link-actions
  drop-compact-price-extraction
  drop-multiline-address-actions
  drop-extraction-edit-restored
  drop-hyphenated-date-actions
  drop-year-first-date-extraction
  drop-year-first-date-actions
  drop-year-first-dotted-date-extraction
  drop-year-first-dotted-date-actions
  drop-dotted-month-date-extraction
  drop-dotted-month-date-actions
  drop-dotted-meridiem-time-extraction
  drop-dotted-meridiem-time-actions
  drop-day-of-month-date-extraction
  drop-day-of-month-date-actions
  drop-month-first-date-extraction
  drop-month-first-date-actions
  drop-maps-curated-value
  drop-history-search-result
  drop-history-filter-reminders-empty
  drop-history-filter-today
  drop-history-search-empty
  drop-history-saved-reference
  drop-history-reference-restored
  drop-history-reference-detail
  drop-history-reference-edited
  drop-history-detail-delete-confirmation
  drop-history-delete-confirmation
  drop-history-reference-deleted
  drop-history-deletion-persisted
)

for name in "${screenshots[@]}"; do
  adb pull "/data/local/tmp/${name}.png" "screenshots/${name}.png" || true
done

if [ "$test_status" -ne 0 ]; then
  exit "$test_status"
fi

for required in "${screenshots[@]}"; do
  test -s "screenshots/${required}.png" || exit 1
done

# Drop

Drop is an offline-first Android action inbox.

> Turn anything on your phone into the next useful action.

## Current milestone

M0 — repository and CI foundation.

## Stack

- Kotlin
- Native Android
- Jetpack Compose
- Material 3
- JUnit 4
- GitHub Actions

## Build locally

Requirements: JDK 17, Android SDK 35, and Gradle 8.7.

```bash
gradle assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Test and validate

```bash
gradle testDebugUnitTest
gradle lintDebug
gradle assembleDebug
```

Every push to `main` runs the same checks. GitHub Actions uploads the debug APK, unit-test reports, and lint reports.

## Product governance

- `PROJECT_PLAN.md` defines product scope and milestone exit criteria.
- `TASKS.md` is the strict ordered implementation checklist.
- A user-facing task is not complete until CI passes and real-device verification is recorded.

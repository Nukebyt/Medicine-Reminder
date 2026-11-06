# Medicine Reminder (Android)

App name: **Medicine Reminder**. Package `com.nukebyt.medicinereminder`. Logo: `logo.jpeg`.

## Stack
Kotlin, Jetpack Compose, Material 3, Hilt, Room (KSP), DataStore, Navigation Compose.
minSdk 26, targetSdk 36, compileSdk 37 (current AndroidX needs it). java.time everywhere.
AGP 9.4.0 (the newest version Android Studio here supports; built-in Kotlin; there is no kotlin-android plugin). Gradle wrapper 9.8.2.

## Commands
- Build: ./gradlew assembleDebug
- Install: ./gradlew installDebug
- Unit tests: ./gradlew test
- Instrumented tests: ./gradlew connectedDebugAndroidTest
- Lint: ./gradlew lintDebug

## Rules
- ROADMAP.md is the spec. Section 3 (reminder pipeline) and section 7 (design) are binding.
- Use AlarmManager.setAlarmClock for reminders and escalations. Never WorkManager for time-critical work.
- domain/ is pure Kotlin with no Android imports; every change there needs unit tests.
- Any change to scheduling, receivers or AlarmService must keep the manual test list in ROADMAP §9 passing; tell me which items to re-run.
- Receivers: goAsync + coroutine, finish within 10 s.
- All user-facing text in strings.xml, sentence case.
- Colours only from the theme, never hard-coded in screens.
- Every new composable has light, dark and fontScale 1.5 previews.
- Room: bump version + add migration and test for any schema change; commit app/schemas.
- Log with the single tag `MedicineReminder` at every pipeline step.
- After each task: run assembleDebug and test, fix failures before reporting done.

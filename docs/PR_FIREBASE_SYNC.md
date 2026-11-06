# Synchronize owner-scoped local forms with Firebase Authentication and Firestore

Initial base: `feat/form-cover-and-continue`  
Head: `feat/firebase-form-sync`

This is a stacked PR. Review and merge the cover/Continue PR first, then retarget this PR to `main`. No direct merge to `main` has been performed.

Users can register, sign in and sign out with Firebase email/password authentication. A repository coordinates Room and Firestore while the UI continues reading local data. Edits and deletion revisions are persisted in Room, scheduled through WorkManager and acknowledged only after server confirmation. The UI distinguishes local saves, pending changes, synchronization and retryable errors. A fresh Room database can recover the authenticated owner's forms from Firestore.

Room v4 adds owner partitions and durable pending revisions through non-destructive migration. Guest drafts stay local until explicitly selected for import into an account. Direct reads by ID and writes are scoped to that account; changing sessions destroys the previous session's ViewModels and cancels its work. Existing request snapshots remain guest-local. Covers are never uploaded or replaced by remote form downloads.

The app compiles without Firebase project configuration and keeps guest editing available with an explicit unconfigured state. Debug supports a separate `demo-formlink` Emulator Suite configuration. Firestore rules enforce basic owner-only draft access; the broader security deliverable remains out of scope. Manual dependency injection and the implications of a separate Hilt migration are documented.

## Validation

- Kotlin compilation, unit tests, Android-test compilation and debug APK build passed.
- 129 unit tests passed.
- 47 Android instrumented tests passed on Pixel_9 / API 37 against Auth and Firestore emulators, with no skips.
- 3 Firestore rules tests passed using `npm test --prefix backend-tests`.
- A separate unconfigured run passed 44 instrumented tests; its 2 backend-dependent cases were explicitly skipped.
- Exercised registration, invalid credentials, WorkManager synchronization, network loss/retry, cross-account denial, explicit guest import, stale-revision acknowledgements, session restoration and recovery into a fresh Room database.

Android emulator command:

```sh
./gradlew compileDebugKotlin testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
```

Start the Firebase emulators separately with `npm run emulators --prefix backend-tests`.

## Configuration and remaining work

The team still needs to create its real Firebase project, enable email/password Auth, supply local `app/google-services.json`, deploy the committed rules and demonstrate a document in Firebase Console. Emulator verification is not presented as production verification. See [Firebase setup, synchronization, demo and dependency injection](FIREBASE_BACKEND.md).

Public web forms, usable public links, Google Sign-In, remote covers, response synchronization and migration to Hilt are not included. Firebase BoM 34.7.0 is pinned for compatibility with the existing Kotlin toolchain.

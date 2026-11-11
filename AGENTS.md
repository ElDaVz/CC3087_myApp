# AGENTS.md

## Overview & Entrypoints
- **Type**: Single-module Android app (`:app`) using Jetpack Compose & Material3.
- **Package Namespace**: `com.uvg.cc3087.myapp`
- **Main Entrypoint**: `app/src/main/java/com/uvg/cc3087/myapp/MainActivity.kt`

## Verification & Build Commands
- **Quick Kotlin compile check**: `./gradlew compileDebugKotlin`
- **Build debug APK**: `./gradlew assembleDebug`
- **Run all unit tests**: `./gradlew testDebugUnitTest`
- **Run single unit test**: `./gradlew testDebugUnitTest --tests "com.uvg.cc3087.myapp.ExampleUnitTest"`
- **Note**: Full `./gradlew lint` or `./gradlew check` can be very slow or time out in headless CLI sessions. Prefer `compileDebugKotlin` and `testDebugUnitTest` for fast feedback loops.

## Toolchain & Environment
- **JDK Requirement**: JDK 21 (Gradle daemon JVM configured for Java 21 via `gradle/gradle-daemon-jvm.properties`).
- **Android SDK**: Path specified in `local.properties` (`sdk.dir`).
- **Dependency Management**: Centralized in `gradle/libs.versions.toml`. Uses Jetpack Compose plugin (`org.jetbrains.kotlin.plugin.compose`).
- **Gradle Configuration Cache**: Enabled by default (`org.gradle.configuration-cache=true`).

## Code Structure Conventions
- `app/src/main/java/com/uvg/cc3087/myapp/ui/components/`: Modular reusable Compose UI components.
- `app/src/main/java/com/uvg/cc3087/myapp/ui/screens/`: High-level screen composables.
- `app/src/main/java/com/uvg/cc3087/myapp/ui/theme/`: Material3 color, typography, and theme definitions.

## Backend & Data Contract (v1)

### Current local implementation

- Firebase Auth and Firestore are optional integrations. Android controls encrypted Auth persistence through REST adapters. Missing project configuration keeps guest editing usable. Google Sign-In is not implemented.
- Room schema v7 includes publication metadata, account-owned request snapshots and a deduplicated Analytics ledger. Keep all exported schemas and explicit migrations v1→v7. Room uses SQLCipher with a key protected by Android Keystore; plaintext databases are converted without destructive fallback.
- Original covers and derivatives stay in encrypted private files, never database blobs. On explicit publication, an optimized copy may be uploaded to Cloudinary; Room stores public image references and owner/form-scoped upload receipts. Continue validates and waits for the latest Room save. Preview is validation-only and never submits.
- Publication is explicit and online. Private draft edits never change the public copy until republishing. Published web forms accept at most ten questions across the existing nine types. Published covers use Cloudinary; files, signatures and rating remain out of scope.
- Public submissions are atomic: immutable response snapshot, two private validation groups and a non-sensitive receipt keyed by the stable attempt UUID. Never allow public response reads or mutations. Archive/delete removes public access while preserving existing responses.
- Account requests use a separate owner-scoped Room projection and durable pending status revisions. Existing local requests remain in the guest partition; no automatic transfer. The mobile Responder entry remains disabled; answering uses the published web link.
- Requests store immutable title/question/answer snapshots and survive source draft edits or deletion. Status is `PENDING`, `IN_PROGRESS`, or `COMPLETED`; local transitions allow reopening requests.
- Use a stable submission ID for retry/process restoration, validate before insertion, and derive counts from stored requests. UI depends on repository interfaces, not Room DAOs.

### Architecture and ownership
- **Backend stack**: Firebase Authentication, Cloud Firestore, and Firebase Storage. Room is the local source of truth for editable forms and drafts; DataStore is reserved for small app preferences.
- **Offline-first behavior**: Save edits to Room immediately. When the owner is authenticated and connected, synchronize pending changes to Firestore. Until collaboration exists, the latest successful update wins.
- **Ownership**: Every form has exactly one authenticated owner (`ownerId`). Collaboration, teams, invitations, roles, and shared editing are explicitly out of scope for v1.
- **Visibility**:
  - `DRAFT`: owner may read and edit; no responses are accepted.
  - `PUBLISHED`: owner may edit; anyone with the shared link may submit a response without an account.
  - `ARCHIVED`: owner may read and edit; no responses are accepted.

### Authentication
- Email/password uses Firebase Authentication REST behind AuthRepository with a Keystore-encrypted session. Google Sign-In remains planned.
- Guests may create local-only drafts. Authenticated users own and synchronize their forms. Clear session ViewModels on account changes; never reuse another account’s repository or sync job.
- UI code accesses authentication through an `AuthRepository`; it must not call Firebase APIs directly.

### Cloud data model

```text
forms/{formId}
  ownerId: String
  title: String
  status: DRAFT | PUBLISHED | ARCHIVED
  fields: List<Field>
  createdAt: Timestamp
  updatedAt: Timestamp
  publishedAt: Timestamp?
  version: Int

forms/{formId}/responses/{responseId}
  formId: String
  submittedAt: Timestamp
  answers: List<Answer>
```

- `fields` live in the form document for v1 so that the form editor can save and load one atomic document. Move them to a subcollection only if form size or collaboration requires it.
- Remote responses are implemented with immutable snapshots, owner-only reads and status-only owner updates. `publishedForms` holds the public snapshot and non-sensitive receipts.
- When implemented, files and digital signatures will use Firebase Storage; Firestore answers retain only their storage paths or references, never raw file bytes.

### Field contract

Every field contains `id`, `type`, `label`, optional `description`, `required`, `position`, and type-specific `config`.

Supported field types and expected configuration:

| Type | Configuration |
|---|---|
| `SHORT_TEXT` | Placeholder and minimum/maximum length |
| `PARAGRAPH` | Placeholder and minimum/maximum length |
| `NUMBER` | Minimum, maximum, and decimal support |
| `EMAIL` | Email validation; allowed domains may be added later |
| `MULTIPLE_CHOICE` | Options and one selected option |
| `CHECKBOX` | One boolean value |
| `CHECKBOX_GROUP` | Options and multiple selected options |
| `DROPDOWN` | Options and one selected option |
| `DATE_TIME` | Date, time, or date-and-time mode |
| `FILE_UPLOAD` | Accepted MIME types, maximum size, and maximum file count |
| `DIGITAL_SIGNATURE` | Required setting; submitted signature is a Storage reference |
| `RATING` | Minimum, maximum, step, and future icon style |

- Existing editor types map as follows: `TEXT` → `SHORT_TEXT`; `MULTIPLE_CHOICE` remains `MULTIPLE_CHOICE`; `DATE` → `DATE_TIME` configured as date-only.
- An `Answer` contains its `fieldId` plus a value appropriate to the field type: primitive value, option IDs, or Storage references.

### Access and repository boundaries
- Firestore security rules must allow owners to read and write only forms whose `ownerId` matches `request.auth.uid`.
- Public requests may create responses only for published forms. They must not read drafts, edit forms, or read other responses.
- Compose UI must depend on repository interfaces, not Firebase or Room implementations. The expected boundaries are `FormRepository` (observe, create, save, delete, publish forms) and `AuthRepository` (observe session, sign in, sign up, sign out).

### Quality and observability
- Never put names, titles, emails, phones, answers, credentials or free text in Analytics/Crashlytics. Metric parameters are random IDs, enums and numeric durations/counts only.
- Creation ends at the first confirmed publication, never at a draft save. Public submission ends only after backend confirmation. Reuse attempt IDs across retries and deduplicate reports. Keep development separate from user studies.
- CI must compile, run meaningful tests and Android Lint on every PR. Run instrumented and browser/emulator tests before claiming verification. Panel visibility, deployed URLs and human review require actual external evidence.

### Public web presentation
- The client web form lives in `web/`. Keep presentation in `src/view.ts` and `src/style.css`; submission, retry identifiers and Analytics coordination remain in `src/main.ts` and the existing backend adapters.
- Match the Android purple palette and reuse `web/public/formlink-logo.svg`, adapted from the Android launcher vectors. Keep the brand at the upper right and support narrow screens, large text and keyboard navigation.
- Public banners accept optional Cloudinary cover metadata from the configured cloud `uxrkjzti`, with an HTTPS URL, public ID and image hash. Missing or broken images use the decorative fallback. Never upload from the web or include local paths, API Secrets or Firebase tokens in Cloudinary requests.
- Cloudinary uses an unsigned image preset configured locally. Upload only on explicit Publish through repositories, reuse confirmed uploads per owner/form/hash, preserve previous publications on failure and require explicit REMOVE to clear an image. KEEP without a local image preserves the published cover. Schema v6→v7 is non-destructive.
- Unsigned uploads are public and not authenticated by Firebase. Do not claim that client checks or preset configuration enforce user authorization, hard quotas or guaranteed exactly-once uploading.
- Browser-only design fixtures live in `web/tests/e2e/design.spec.ts`. Never expose fixture routes or answers in the production client. Use `npm test --prefix web`, `npm run build --prefix web` and the browser tests for web changes.

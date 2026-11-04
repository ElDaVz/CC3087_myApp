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

- Firebase below is a future contract, not a configured backend. The current app saves requests locally through `RequestRepository` / `RoomRequestRepository` in both build variants.
- Room schema v3 adds an optional private cover path with non-destructive v2→v3 migration. Images live in app-private files, never database blobs. Continue must validate and wait for the latest Room save before navigation.
- Room schema v2 adds `local_requests` with an explicit non-destructive migration from v1. Keep existing form data and exported schemas.
- Room stores drafts only; publication is not implemented. Keep `Responder` disabled and submission disconnected from normal app navigation until publication exists. Exercise capture only through isolated test fixtures; debug examples must be labeled as demos. Editor preview remains validation-only and must never save submissions.
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
- Planned providers: email/password and Google Sign-In through Firebase Authentication.
- Before sign-in is implemented, guests may create local-only drafts. Authenticated users own and synchronize their forms.
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
- Remote response collection remains planned; local submission, list, detail and status UI are implemented independently of this cloud contract.
- Files and digital signatures are stored in Firebase Storage; Firestore answers retain only their storage paths or references, never raw file bytes.

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

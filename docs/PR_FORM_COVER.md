# Add optional local form covers and validated Continue navigation

Base: `main`  
Head: `feat/form-cover-and-continue`

The editor now lets users add, replace or remove an optional form cover. Images are normalized into private app files; Room v3 stores relative references through a non-destructive migration. A blurred banner with a readable title appears in the editor and Preview, and the original cover appears in form-list thumbnails. Imported images and blur derivatives survive database reopen; replaced files are removed once unreferenced.

Replace the validation-only **Validar** button with **Continuar**. It uses the central validator and waits for the latest successful Room save before opening **Guardar y compartir**. Invalid input remains in the editor, and save failures preserve edits for retry. Preview stays validation-only; neither action publishes a form or creates requests. Public-link controls remain disabled until real publication exists.

## Validation

- `./gradlew compileDebugKotlin testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug`
- `./gradlew connectedDebugAndroidTest`
- 121 unit tests and 40 instrumented tests passed on Pixel_9 / Android 17 (API 37).
- Covers, orientation, database reopen, non-destructive migration, validation, failed-save retry and delayed-save navigation covered.

## Limits

Covers are optional and device-local. Inputs are limited to 10 MiB and normalized to a maximum side of 2048 px. The small blurred derivative uses CPU processing to support Android 26–30 as well as newer devices. No Firebase or public form publishing is introduced by this PR.

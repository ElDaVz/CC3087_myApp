# Pendientes de FormLink

Prioridad de trabajo; ver los resultados comprobados de cada entrega al final.

- [ ] Recuperar las portadas publicadas al iniciar sesión sin archivos locales, con actualización reactiva en tarjetas, editor y Preview. Implementación en `fix/restore-cloudinary-covers`; consultar la verificación al final de este documento.
- [x] Completar la pantalla de login con la identidad visual de FormLink, registro, recuperación por correo, validaciones y estados de carga y error. Ver la verificación de autenticación al final.
- [ ] Crear un dashboard Android de respuestas con Material 3: tarjetas de indicadores, total de solicitudes, distribución por estado y evolución temporal, con acceso a la revisión de respuestas y al cambio de estado. Calcular los indicadores desde las respuestas almacenadas mediante repositorios y ViewModels.
- [ ] Implementar login con Google mediante Firebase a través de `AuthRepository`, conservando la sesión cifrada y la separación entre cuentas.
- [ ] Evaluar una URL más amigable que `formlink-9f735.web.app`: comparar un sitio Hosting con otro identificador y un dominio propio, incluyendo costos y cómo conservar los enlaces existentes.
- [ ] Diseñar e implementar la eliminación definitiva de formularios. Antes de hacerlo, decidir qué ocurre con las solicitudes históricas, los enlaces públicos, las imágenes de Cloudinary y la confirmación al usuario. Actualmente retirar o borrar un publicado conserva el historial.

## Verificación de recuperación de portadas (2026-11-12)

Implementado en `fix/restore-cloudinary-covers`, desde `main` actualizado, sin push. Las tarjetas proyectan la URL publicada y la intención del borrador; editor y Preview reciben cambios de metadatos sin volver a abrir el formulario ni guardar otra revisión. La observación está filtrada por propietario y formulario y se cancela al cambiar de cuenta o formulario. Las publicaciones archivadas conservan su portada para el propietario.

Las tres superficies comparten la resolución: `REMOVE` usa el respaldo; una imagen privada descifrable tiene prioridad; `KEEP` permite la URL publicada cuando falta el archivo; `REPLACE` sin archivo utilizable usa el respaldo. Se valida la URL existente de Cloudinary; una descarga fallida mantiene el respaldo. El descifrado sigue en memoria y no se convierten imágenes públicas en originales privados. Room permanece en v7 y los contratos remotos y reglas no cambian.

Comprobaciones realizadas:

- `./gradlew compileDebugKotlin testDebugUnitTest`: correcto; 144 pruebas unitarias, sin fallos. Incluyen metadatos tardíos durante una edición pendiente, cambio de formulario, restauración de estado, cancelación al cambiar de cuenta y resolución de intenciones.
- Emulador Pixel_9 Android 17 (API 37): `RoomFormStoreTest`, `CoverStorageTest`, `EditorPreviewTest` y `RestoredCoverUiTest` pasan (15 pruebas). Se comprueban llegada tardía de metadatos, aislamiento de propietarios, portadas archivadas, descifrado y fallback. La matriz de UI comprueba tarjeta, editor y Preview con imágenes roja/verde controladas mediante Coil, archivo privado cifrado, archivo ausente, `KEEP`, `REMOVE`, `REPLACE`, URL rechazada y descarga fallida.
- Las 3 pruebas de `RestoredCoverUiTest` se ejecutaron otra vez tras ampliar la comprobación de navegación editor → Preview y de fallo de descarga: correctas.
- Firebase Auth/Firestore locales (`demo-formlink`): pasan 3 pruebas instrumentadas con `-PfirebaseEmulators=true` y `backendEmulators=true`: `RestBackendIntegrationTest` y `CloudinaryPublicationTest#coverRetryRemovalAndOtherDeviceEditingPreservePublishedData`. Incluyen cerrar sesión, iniciar sesión de nuevo, recuperar formularios en una base sin originales privados ni caché de publicaciones y refrescar sus metadatos de portada. El cargador de imágenes está simulado y no se producen subidas reales. La integración inicialmente omitida se ejecutó después con los servicios locales activos.
- Los primeros intentos instrumentados fallaron por `DEVICE_PROVISION_FAILED` / desconexión del emulador y después por ausencia de dispositivos. Pasaron tras iniciar el AVD sin ventana. No se detectaron fallos de código previos.

Pendiente de evidencia externa:

- Recuperación con una cuenta y una URL real ya publicada: no verificada; no se ha proporcionado una URL afectada. Las imágenes de prueba son controladas. No se ejecuta la prueba optativa `realUnsignedUploadOfASyntheticImage` ni se crean subidas reales.
- CI en Android 17 (API 37): pendiente de ejecutar tras el push. El workflow ahora usa la imagen `android-37.0/google_apis_playstore_ps16k/x86_64`, perfil Pixel 9 y 4096 MiB de RAM, como la ejecución local de pruebas. La plataforma de compilación permanece en SDK 36.1, igual que `compileSdk`; es independiente de la imagen del emulador.

Comandos instrumentados reproducibles (requieren un dispositivo conectado):

```sh
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.uvg.cc3087.myapp.RestoredCoverUiTest,com.uvg.cc3087.myapp.RoomFormStoreTest,com.uvg.cc3087.myapp.CoverStorageTest,com.uvg.cc3087.myapp.EditorPreviewTest
backend-tests/node_modules/.bin/firebase emulators:exec --project demo-formlink --only auth,firestore './gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true -Pandroid.testInstrumentationRunnerArguments.class=com.uvg.cc3087.myapp.CloudinaryPublicationTest#coverRetryRemovalAndOtherDeviceEditingPreservePublishedData,com.uvg.cc3087.myapp.RestBackendIntegrationTest'
```


## Verificación de autenticación (2026-11-12)

Implementado: vista dedicada con modos login, registro y recuperación; perfil exclusivo de la sesión activa con sincronización, importación y cierre de sesión. Mis formularios sigue siendo la entrada y se conserva el acceso como invitado. Los formularios conservan el correo al cambiar de modo y limpian contraseñas y errores; las solicitudes bloquean envíos duplicados y navegación. Los errores se traducen a categorías comprensibles y los campos tienen validación local. Los recursos en inglés y español siguen la selección de idioma de Android. La pantalla usa el tema morado local de FormLink, desplazamiento e insets del teclado sin modificar el tema global.

`AuthRepository.sendPasswordReset` usa Firebase REST `sendOobCode` con `PASSWORD_RESET`; correo desconocido recibe la misma confirmación. No persiste ni inicia sesión. Tras aceptación, el contenido se sustituye por «Revisa tu email» y el control de volver. La selección de contraseña nueva queda en la página de Firebase; no se añade restablecimiento nativo. Se cancelan solicitudes y descartan resultados anteriores al cerrar el flujo; la persistencia REST comprueba cancelación antes de guardar una sesión.

Comprobaciones realizadas:

- `./gradlew compileDebugKotlin testDebugUnitTest`: correcto, 149 pruebas unitarias sin fallos. Incluyen validaciones, confirmación, recorte exclusivo del correo, correspondencia de errores, envío único, bloqueo de cambios durante carga, limpieza de credenciales, recuperación sin sesión y descarte de resultados tardíos.
- Pixel_9 Android 17 (API 37), `AuthScreenTest` y `BackendRootTest`: cuatro pruebas ejecutadas sin fallos; el recorrido que requiere Firebase quedó omitido en esa primera ejecución. Se verifican acceso desde invitado, recuperación, mensaje exacto y controles deshabilitados durante carga.
- Con Firebase Auth/Firestore locales (`demo-formlink`), `RestBackendIntegrationTest` y `BackendRootTest`: seis pruebas ejecutadas, cero omitidas y cero fallos. Incluyen registro/login reales contra emulador, separación del perfil, cierre de sesión, aislamiento y recuperación. Se obtiene el código de `oobCodes`, se cambia la contraseña y se comprueba que la anterior falla y la nueva inicia sesión con el mismo propietario. Se comprueba también la confirmación para correo desconocido. No se envían correos reales.

Pendiente de evidencia: revisión visual claro/oscuro y teclado, omitida por petición del usuario después de un aviso de la IU del sistema del emulador; recepción de correo real y ejecución de GitHub Actions no verificadas. Google Sign-In y restablecimiento nativo siguen fuera de esta entrega.

Referencia del contrato REST: [Firebase Authentication REST](https://firebase.google.com/docs/reference/rest/auth); [emulador de Authentication](https://firebase.google.com/docs/emulator-suite/connect_auth).

Comandos reproducibles:

```sh
./gradlew compileDebugKotlin testDebugUnitTest
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.uvg.cc3087.myapp.AuthScreenTest,com.uvg.cc3087.myapp.BackendRootTest
backend-tests/node_modules/.bin/firebase emulators:exec --project demo-formlink --only auth,firestore './gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true -Pandroid.testInstrumentationRunnerArguments.class=com.uvg.cc3087.myapp.RestBackendIntegrationTest,com.uvg.cc3087.myapp.BackendRootTest'
```

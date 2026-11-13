# La portada del formulario también llega a la web

La imagen se sigue eligiendo y guardando localmente en Android. Guardar o editar un borrador no sube nada. Al pulsar **Publicar**, el repositorio prepara una copia JPEG de hasta 1600 píxeles por lado y 2 MiB, la envía por HTTPS a Cloudinary y después confirma la publicación en Firestore. La preparación se hace en memoria: el original y su versión desenfocada continúan cifrados en archivos privados.

## Configuración en este proyecto

Se utiliza el entorno `uxrkjzti` con el preset unsigned `formlink_covers`. En `local.properties`, que no se versiona:

```properties
cloudinary.cloudName=uxrkjzti
cloudinary.uploadPreset=formlink_covers
```

También se pueden proporcionar como propiedades de Gradle del mismo nombre. No se necesita API Key ni API Secret. Estos dos valores no son secretos: aunque se configuren fuera del repositorio, se incluyen en el APK para permitir la subida.

En Cloudinary, el preset debe ser **Unsigned**, permitir imágenes JPG/JPEG/PNG/WebP, usar identificadores aleatorios y tener **Disallow public ID** activado. La carpeta acordada es `formlink/covers`. La transformación de entrada opcional limita dimensiones a 1600 × 1600 sin ampliar imágenes pequeñas. El preset no ofrece un límite de archivo por preset: el límite de 2 MiB que aplica Android es una comprobación del cliente y no protege frente a clientes externos.

## Publicar una portada mientras Firebase está emulado

Firebase local y Cloudinary se configuran por separado. Por defecto, `-PfirebaseEmulators=true` mantiene las subidas reales a Cloudinary bloqueadas. Para publicar manualmente una portada real en Cloudinary mientras Auth y Firestore siguen en `demo-formlink`, agregar esta opción explícita al `local.properties` ignorado por Git:

```properties
cloudinary.cloudName=uxrkjzti
cloudinary.uploadPreset=formlink_covers
cloudinary.allowUploadsWithEmulators=true
```

También se puede usar `-Pcloudinary.allowUploadsWithEmulators=true`. La opción afecta únicamente a la integración de portadas de Debug; no cambia los hosts ni el proyecto de Firebase y no activa telemetría. La imagen se sube cuando el propietario pulsa Publicar o Actualizar publicación. Guardar, editar y previsualizar siguen siendo locales. El enlace resultante pertenece al proyecto Firebase emulado; esta configuración no despliega una web pública real.

Las pruebas automáticas deben anular el permiso local explícitamente:

```bash
./gradlew compileDebugKotlin testDebugUnitTest -PfirebaseEmulators=true -PtelemetryEnabled=false -Pcloudinary.allowUploadsWithEmulators=false
```

Al ejecutar instrumentadas, usar también `-Pcloudinary.allowUploadsWithEmulators=false` y mantener ausente `realCloudinary=true`. No ejecutar el runner que limpia instalaciones sobre un AVD con datos de usuario sin aislarlo previamente. El caso existente de publicación con uploader simulado comprueba reintentos, reutilización de referencias y conservación de la publicación ante fallos sin subir archivos reales.

La ausencia de esta opción conserva el bloqueo original. Fuera de Firebase emulado se conserva el comportamiento previo de Cloudinary. Los booleanos del entorno se evalúan en tiempo de ejecución para evitar que Kotlin conserve un valor incorporado de una compilación anterior.

## Qué sucede al cambiar la imagen

Una subida confirmada guarda su URL, public ID y huella SHA-256 en Room, por propietario y formulario. Si Firestore falla después, el siguiente intento reutiliza esa referencia. Publicar lo mismo no genera otra versión ni vuelve a subir la misma copia confirmada.

Reemplazar la imagen se guarda como una intención de reemplazo. Quitarla se guarda explícitamente como REMOVE. Conservar sin un archivo local mantiene la portada pública: un formulario recuperado en otro dispositivo no pierde la imagen solo porque ese dispositivo no tiene el archivo privado. El editor permite ver y quitar esa portada publicada cuando la sincronización recupera sus metadatos.

Cambiar el borrador no altera la portada visible en la web hasta republicar. Si falla la subida o el formulario cambia mientras se publica, se conserva la copia pública anterior. Las publicaciones antiguas sin portada siguen funcionando.

La migración Room v6→v7 agrega las intenciones, metadatos de publicación y recibos de subida sin borrar borradores, solicitudes ni el registro de Analytics. Las migraciones anteriores siguen disponibles.

## Qué puede y qué no puede garantizar este método

El preset unsigned es público. Quien lo conozca puede subir imágenes sin pasar por Firebase. La app limita tipos y tamaño y las reglas de Firestore protegen quién publica cada formulario, pero no equivalen a autorización de las subidas en Cloudinary. El plan gratuito tiene cuotas de almacenamiento, transformaciones y tráfico.

Si Cloudinary guarda una imagen pero su respuesta se pierde antes de confirmarla en Android, el reintento podría crear otra imagen. No se promete exactamente una subida en ese caso. Las referencias confirmadas sí se reutilizan. Las imágenes reemplazadas o huérfanas no se borran automáticamente: hacerlo de forma privilegiada requiere un backend y queda pendiente. Retirar un formulario bloquea el acceso por su enlace de FormLink, pero no revoca una URL pública de imagen que alguien ya conozca.

Las URLs de portada se limitan a HTTPS y al entorno Cloudinary configurado, sin parámetros ni credenciales. No se envían títulos, respuestas, correo, tokens o rutas privadas a Cloudinary ni a Analytics. La web solo descarga la imagen, mantiene el diseño responsive y utiliza el banner de marca si la imagen falta o falla.

## Revisión y publicación

La corrección para publicar con Firebase local está en `fix/cloudinary-emulator-publication`. Se conserva el flujo de revisión local: no hacer push ni desplegar hasta que el usuario revise los cambios.

Para compilar y ejecutar pruebas locales Android:

```bash
./gradlew compileDebugKotlin testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug lintDebug
```

Para las pruebas de publicación y solicitudes, iniciar Auth y Firestore locales y usar el emulador Android:

```bash
backend-tests/node_modules/.bin/firebase emulators:start --project demo-formlink --only auth,firestore
./gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -PtelemetryEnabled=false -Pcloudinary.allowUploadsWithEmulators=false -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true
```

La prueba real de Cloudinary requiere además `-Pandroid.testInstrumentationRunnerArguments.realCloudinary=true`. Esta opción sube una imagen sintética pequeña a la cuenta configurada. Sin ella, esa prueba externa se omite explícitamente.

Las verificaciones web se ejecutan con `npm test --prefix web`, `npm run build --prefix web` y, con Firebase local activo, `bash scripts/test-web-browser.sh`. Las reglas se prueban desde `backend-tests` con `npm run test:running`.

Después de aprobar e integrar la rama se deben desplegar las reglas de Firestore y Hosting, además de instalar la nueva versión Android. La web anterior y las reglas antiguas no se actualizan con un merge. El despliegue se realizará como paso separado.

## Evidencia de esta implementación

Verificado el 11 de noviembre de 2026:

- 138 pruebas unitarias Android aprobadas.
- 56 pruebas instrumentadas aprobadas en Pixel_9 / Android 17, sin omisiones, con Auth y Firestore locales. Incluyen la migración desde v6, la conservación de borradores/solicitudes/Analytics y la recuperación de las acciones de portada.
- Subida real de una imagen JPEG sintética al entorno `uxrkjzti` con el preset `formlink_covers`, ejecutada desde el adaptador Android. La prueba externa se ejecutó en las dos pasadas de la suite y dejó dos imágenes sintéticas pequeñas en Cloudinary.
- 11 pruebas de reglas aprobadas, incluida la publicación inicial de diez preguntas con portada y el envío de respuestas.
- 7 pruebas unitarias web y 13 pruebas de navegador Chrome aprobadas. El navegador comprueba la carga de una portada simulada y el respaldo cuando falla, además del envío real contra Firebase local.
- Compilaciones Kotlin, pruebas instrumentadas y web correctas. Android Lint terminó sin errores; conserva advertencias. El build web conserva la advertencia de tamaño del paquete JavaScript.

La primera pasada instrumentada encontró un fallo en un caso existente de Preview. Pasó al repetirlo aislado y la segunda suite completa terminó con las 56 pruebas en verde; no se cambió ese caso ni el flujo Preview para ocultarlo.

No se ha desplegado esta integración a Hosting ni se han cambiado reglas del proyecto real. La evidencia de Firebase es local; la única operación externa de esta verificación fue la subida sintética a Cloudinary.


## Verificación del desbloqueo de portadas con Firebase local

El 12 de noviembre de 2026 se confirmó en el Pixel 9 el mensaje “Configure Cloudinary to publish a cover”: el uploader era nulo por la condición de Firebase emulado, pese a tener cloud y preset configurados. Se separó el permiso de Cloudinary de la selección del backend Firebase, conservando el bloqueo por defecto y añadiendo el opt-in local documentado arriba.

- Compilación y 164 pruebas unitarias aprobadas con `cloudinary.allowUploadsWithEmulators=false`; se comprobó `CLOUDINARY_UPLOADS_ENABLED=false` en BuildConfig.
- Lint e instalación con el opt-in habilitado: correctos. Lint terminó sin errores y conservó 69 advertencias.
- APK actualizado en el Pixel 9 mediante instalación sin desinstalar. Se comprobó `FIREBASE_EMULATORS=true`, `CLOUDINARY_UPLOADS_ENABLED=true` y `TELEMETRY_ENABLED=false`, con Auth y Firestore en localhost.
- No se ejecutaron pruebas instrumentadas sobre la instalación con datos del usuario ni se activó `realCloudinary=true`. No se publicó el formulario del usuario ni se subieron imágenes durante esta verificación. La disponibilidad actual del preset remoto y la subida real quedan pendientes de la acción explícita Publicar del propietario.

Los cambios anteriores de eliminación permanecen fuera de estos commits. La configuración Cloudinary se conserva en `local.properties`, ignorado por Git; no se agregaron API Secrets ni nuevos SDKs.

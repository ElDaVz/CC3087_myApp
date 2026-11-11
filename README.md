# FormLink · CC3087

Aplicación Android para crear formularios, compartirlos por enlace y gestionar solicitudes de clientes. Android usa Kotlin, Compose/Material 3, MVVM, Room y repositorios. El cliente web es una página móvil con TypeScript/Vite y Firebase.

## Estado de la fase 3

La implementación incluye autenticación por correo/contraseña, formularios privados por propietario, guardado local cifrado, sincronización de borradores, publicación explícita, cliente web y bandeja de solicitudes remotas. La configuración de Hosting, Analytics, Crashlytics y CI debe distinguirse de su evidencia externa: un workflow escrito no equivale a una ejecución de GitHub ni la configuración local a un despliegue verificado.

| Inciso | Implementación | Evidencia externa |
|---|---|---|
| Planificación | Etapas, desviaciones y definición de terminado en `docs/PHASE3_DELIVERY.md` | Completar responsables/fechas e issues del equipo |
| Backend | Firebase Auth, Firestore y coordinación Room ↔ remoto | Demo real de consola y recuperación entre sesiones |
| Seguridad | Keystore, sesión/Room/portadas cifradas, reglas y HTTPS | Revisión del equipo y configuración de servicios |
| Calidad | Pruebas, workflow por PR, QA manual y telemetría | CI visible, aprobación humana y paneles reales |
| Feedback Fase 2 | Matriz preparada | Pendiente de recibir observaciones originales |

## Flujo principal

1. Invitado o cuenta crea un formulario en blanco o desde plantilla. Las portadas son opcionales y se conservan cifradas en el dispositivo hasta publicar una copia con Cloudinary.
2. Cada edición se guarda en Room. Preview valida sin guardar solicitudes.
3. **Continuar** valida y espera el último guardado antes de abrir Guardar/Compartir.
4. **Publicar** exige cuenta y conexión; confirma el backend antes de habilitar copiar/compartir el enlace HTTPS.
5. El cliente abre `/f/{id}`, completa y envía. Se confirma únicamente tras guardarse en Firestore.
6. Android sincroniza la solicitud a la bandeja y permite cambiar/reabrir su estado.

La primera web admite diez preguntas y los nueve tipos actuales del editor. No admite archivos, firmas ni rating. Las ediciones privadas no cambian la copia pública: usar **Actualizar publicación**. **Retirar publicación** bloquea nuevos envíos. Las solicitudes conservan snapshots históricos. El botón móvil Responder sigue deshabilitado; el cliente responde desde el enlace web.

## Fuente de verdad y funcionamiento sin conexión

**Room es la fuente de verdad de la UI y de los cambios pendientes**. Los repositorios escriben localmente y WorkManager coordina Auth/Firestore mediante adaptadores HTTPS. Pantallas y ViewModels no acceden a DAOs ni a Firebase directamente. Los SDK oficiales de Analytics y Crashlytics se encapsulan por separado.

- Formularios: guardar inmediatamente en Room; subir revisiones pendientes al autenticarse, editar, recuperar conexión, reintentar o volver a primer plano. Entre dispositivos gana la última escritura confirmada. Las confirmaciones no limpian revisiones nuevas.
- Publicación: **Firestore conserva la copia pública confirmada** que consume la web. Guardar/sincronizar un borrador no publica. Publicar y retirar requieren conexión.
- Solicitudes: **Firestore conserva el envío original**; Room mantiene su proyección por propietario. Android descarga al entrar/volver, reintentar y cada 15 segundos mientras está activo. Los cambios de estado se guardan localmente y se reintentan sin conexión. No se usa un listener push.
- Sin conexión: editar y consultar lo ya descargado sigue disponible; la UI informa pendientes/errores. La web muestra error y conserva el intento en la pestaña; no anuncia un envío no confirmado ni guarda respuestas en almacenamiento persistente del navegador.
- Invitados: borradores y solicitudes de fixtures locales separados de cuentas. Importar borradores a una cuenta es una selección explícita.
- Portadas: originales privados cifrados, nunca blobs de Room ni rutas transmitidas al backend. Al publicar se sube una copia optimizada a Cloudinary y se guarda su referencia pública; ver [configuración y límites](docs/CLOUDINARY_COVERS.md).

## Configuración

Requiere JDK 21 y Android SDK indicado en `local.properties`; dependencias centralizadas en `gradle/libs.versions.toml`. Configurar Firebase Android mediante `app/google-services.json` ignorado por Git. Sin configuración, la edición como invitado sigue utilizable. Registrar además una app WEB y habilitar email/password y Firestore. No subir cuentas de servicio ni credenciales privadas.

```sh
./gradlew compileDebugKotlin testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug
./gradlew lintDebug
npm ci --prefix backend-tests
npm ci --prefix web
npm run build --prefix web
npm test --prefix web
npm test --prefix backend-tests
```

Con Firebase Emulator Suite activo:

```sh
web/node_modules/.bin/playwright install --no-shell chromium
./gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true
npm run test:e2e --prefix web
```

Debug desactiva telemetría por defecto. La demo real de Analytics/Crashlytics usa `-PtelemetryEnabled=true`, sin Firebase emulado. La variante con emuladores usa `demo-formlink` y no sube imágenes reales desde el flujo normal. La prueba externa de Cloudinary requiere activar explícitamente `realCloudinary=true`.

## Arquitectura e inyección

`Compose → ViewModel → interfaces de repositorio → Room / adaptadores remotos`.

`AppContainer`, `BackendServices`, factories de ViewModels y WorkerFactory realizan inyección manual explícita. Los repositorios quedan ligados a una cuenta; al cambiar sesión se destruyen sus ViewModels y trabajos. Hilt automatizaría construcción, scopes y factories, pero no sustituye repositorios ni decide sincronización. La inyección manual es suficiente para este módulo; migrar a Hilt sería un PR separado, descrito en [la guía de arquitectura](docs/FIREBASE_BACKEND.md).

## Documentación y entrega

- [Publicación, contratos web y despliegue](docs/PUBLIC_FORMS.md).
- [Seguridad, cifrado y actualización](docs/SECURITY.md).
- [Métricas, DebugView y Crashlytics](docs/ANALYTICS.md).
- [Consultas de promedios y abandono al corte](docs/analytics/metrics.sql).
- [Checklist de QA manual](docs/QA_CHECKLIST.md).
- [Planificación, definición de terminado y feedback](docs/PHASE3_DELIVERY.md).
- `.github/workflows/quality.yml`: compilación, unitarias, Lint, pruebas de reglas, navegador e instrumentadas por PR.

El desarrollo se entrega por ramas y PRs revisados por otro integrante; no se integra directamente a main. Las pruebas automatizadas y los resultados de consola deben adjuntarse al PR correspondiente. La asistencia de IA apoyó implementación y pruebas; el equipo debe poder explicar los cambios.

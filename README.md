# FormLink · CC3087

FormLink permite crear formularios en Android, publicarlos por enlace y gestionar las respuestas recibidas. El propietario trabaja en la app; sus clientes responden desde una página web sin crear una cuenta. Los borradores se guardan localmente y la publicación es una acción explícita.

## Qué incluye

- Editor de formularios en blanco o desde plantillas, con preguntas obligatorias, validación y portada opcional.
- Acceso como invitado y autenticación por correo/contraseña, registro y recuperación por email.
- Guardado local cifrado, sincronización por cuenta, publicación, copia de enlace y envío mediante WhatsApp.
- Cliente web adaptable a móvil para responder formularios publicados.
- Dashboard con indicadores, evolución temporal, respuestas recientes y acceso a una bandeja filtrada.
- Detalle de cada solicitud y estados **Pendiente**, **En progreso** y **Completada**, con posibilidad de reabrirla.

Google Sign-In, incorporación automática de borradores, exportación e identidad empresarial de los enlaces siguen pendientes. La configuración de servicios y workflows no equivale a evidencia de despliegue, recepción de correo o CI aprobado; consultar los [resultados de verificación](docs/PENDIENTES.md).

## Stack

| Capa | Tecnologías y responsabilidad |
|---|---|
| Android | Kotlin, Jetpack Compose y Material 3; Activity, ViewModel, SavedStateHandle, coroutines y Flow. |
| Datos locales | Room v8 sobre SQLCipher; clave y sesión protegidas mediante Android Keystore. Imágenes en archivos privados cifrados. |
| Sincronización | WorkManager y repositorios por propietario; adaptadores HTTPS para Firebase Authentication y Cloud Firestore. |
| Publicación e imágenes | Firestore conserva la copia pública; Cloudinary recibe una imagen optimizada solo al publicar. |
| Web | TypeScript, Vite y Firebase JS; Vitest y Playwright para pruebas. |
| Observabilidad | SDKs de Firebase Analytics y Crashlytics encapsulados, con parámetros sin contenido personal. |
| Calidad | JUnit, pruebas de coroutines, Compose/Espresso, Firebase Emulator Suite, Android Lint y GitHub Actions. |

Es una app Android de un módulo (`:app`), con inyección manual mediante `AppContainer`, `BackendServices` y factories. El flujo de dependencias es **Compose → ViewModel → interfaces de repositorio → almacenamiento/adaptadores**; las pantallas no llaman a DAOs ni a Firebase directamente. Firebase Storage no se usa actualmente para archivos o firmas; esas funciones son futuras.

## Preparación y ejecución

Requisitos: **JDK 21**, Android Studio o herramientas del Android SDK, plataforma de compilación **36.1** y un dispositivo Android **8.0/API 26** o posterior. `targetSdk` es 36; las pruebas instrumentadas de esta entrega se ejecutan en Android 17/API 37. Para web y Firebase CLI, el workflow utiliza Node.js 22.

1. Abrir el repositorio en Android Studio y configurar `sdk.dir` en `local.properties` con la ruta del SDK.
2. Dejar que Gradle sincronice las dependencias de `gradle/libs.versions.toml` y ejecutar la configuración `app`, o construir el APK:

```sh
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

La edición como invitado funciona sin configurar Firebase. Para autenticación y publicación reales, registrar la app Android, colocar su configuración en `app/google-services.json` —ignorado por Git—, habilitar Email/Password y Firestore y aplicar las reglas e índices del repositorio. No versionar cuentas de servicio, credenciales privadas ni configuraciones locales. Ver [backend](docs/FIREBASE_BACKEND.md) y [publicación y despliegue](docs/PUBLIC_FORMS.md).

Para publicar una portada, configurar en `local.properties` el cloud y el preset unsigned documentados en [Cloudinary](docs/CLOUDINARY_COVERS.md). Nunca incluir un API Secret en el APK. El preset público no proporciona autorización por usuario ni cuotas estrictas.

Para trabajar en el cliente web:

```sh
npm ci --prefix web
npm run dev --prefix web
npm run build --prefix web
```

Configurar las opciones públicas de Firebase siguiendo `web/.env.example` y la guía de publicación; en Hosting se puede resolver la configuración del proyecto. El cliente sirve `/f/{id}` y requiere un formulario publicado. Las variables `VITE_*` son visibles para el navegador y no deben contener secretos.

Para desarrollo aislado, seguir la [guía de Firebase local](docs/FIREBASE_BACKEND.md#desarrollo-aislado-con-firebase-emulator-suite): usa `demo-formlink`, un AVD dedicado y la configuración Run **FormLink Firebase local**, sin `google-services.json` real y sin telemetría. Las dependencias actuales de pruebas Firebase requieren Node >=24.12.

## Cómo usar la app

1. **Mis formularios** es la entrada. Crear un formulario nuevo o elegir una plantilla; editar título, preguntas y portada. Los cambios se guardan automáticamente.
2. **Preview** valida y permite probar el formulario sin insertar solicitudes. **Continuar** valida y espera el último guardado antes de abrir Guardar/Compartir.
3. Abrir el perfil para iniciar sesión, registrarse o recuperar contraseña. El mensaje «Revisa tu email» confirma la solicitud de recuperación; elegir la contraseña nueva ocurre en la página de Firebase y no inicia sesión automáticamente.
4. En una cuenta, importar manualmente los borradores de invitado que se desee conservar allí. No se transfieren automáticamente ni se mezclan datos entre cuentas.
5. **Publicar** requiere cuenta y conexión. Tras confirmar el backend se puede copiar o compartir el enlace HTTPS. El cliente abre la web, completa y envía; solo se anuncia éxito tras la confirmación de Firestore.
6. Consultar **Respuestas** para revisar indicadores y solicitudes. Abrir un detalle permite consultar su snapshot y cambiar su estado.

La web acepta hasta diez preguntas y los nueve tipos actuales del editor. Archivos, firmas digitales y rating no están disponibles. El botón móvil Responder sigue deshabilitado: los clientes responden desde el enlace web.

Editar o sincronizar un borrador no modifica el formulario público: hay que **Actualizar publicación**. **Retirar publicación** bloquea nuevos envíos y conserva respuestas históricas. Estas retienen título, preguntas y respuestas originales aunque después se edite o elimine el borrador.

## Dashboard y respuestas nuevas

El dashboard muestra total de respuestas y distribución por estado. Elegir **Todos los formularios** o un formulario, y **7 días**, **30 días** o **Todo**; el período inicial es 30 días. Las tarjetas, la gráfica, las cinco respuestas recientes y el acceso a la bandeja usan los mismos filtros. Las tarjetas de estado abren la bandeja filtrada y volver del detalle conserva el contexto.

Los períodos incluyen el día actual según la zona horaria del dispositivo. La gráfica agrupa por día para 7/30 días y por mes para Todo; se puede desplazar para recorrer el historial. Las respuestas sin fecha participan en Todo, pero no en la gráfica. Los formularios con respuestas históricas permanecen consultables aunque su borrador ya no exista.

El punto rojo de **Respuestas** indica que hay solicitudes sin revisar en la cuenta activa, incluso fuera del filtro visible. Todas las respuestas existentes empiezan sin revisar; abrir correctamente un detalle lo marca revisado. Entrar al dashboard, cambiar un estado o resincronizar no equivale a leer una respuesta. Esta lectura se conserva **solo en este dispositivo**, separada por invitado/cuenta, incluso después de cerrar sesión o reiniciar; no se comparte entre dispositivos.

## Fuente de verdad y funcionamiento sin conexión

**Room es la fuente de verdad de la UI y de los cambios pendientes.** Firestore conserva las publicaciones y los envíos originales; Android mantiene una proyección local por propietario.

- **Borradores:** cada edición se guarda localmente; WorkManager envía revisiones pendientes al recuperar conexión y durante la actividad de la cuenta. Entre dispositivos gana la última escritura confirmada.
- **Solicitudes:** se descargan al volver al primer plano, reintentar y mediante comprobaciones cada 15 segundos mientras la cuenta está activa. No hay listener push ni notificaciones del sistema. El punto rojo refleja respuestas ya descargadas.
- **Sin conexión:** editar borradores, consultar respuestas descargadas, indicadores y lecturas sigue disponible. Los cambios de estado se guardan localmente y se reintentan al reconectar. Publicar, retirar publicaciones y recibir nuevas respuestas requieren conexión.
- **Portadas:** originales y derivados privados permanecen cifrados; publicar puede subir una copia optimizada. Una portada pública recuperada de otro dispositivo se puede mostrar sin recuperar el original privado.
- **Web:** ante un fallo conserva el identificador de intento en la pestaña para reintentar sin duplicar el envío. No guarda las respuestas en almacenamiento persistente del navegador ni confirma un envío fallido.

La migración v7→v8 añade únicamente el historial local de lecturas. Se conservan esquemas y migraciones anteriores sin fallback destructivo. Cerrar sesión cambia el repositorio y destruye los ViewModels de la cuenta; las cachés locales permanecen aisladas por propietario.

## Pruebas y desarrollo

```sh
./gradlew compileDebugKotlin testDebugUnitTest
./gradlew lintDebug
./gradlew connectedDebugAndroidTest
npm test --prefix web
npm run build --prefix web
npm ci --prefix backend-tests
npm test --prefix backend-tests
```

Para las integraciones Android, iniciar Auth/Firestore locales junto con el emulador Android:

```sh
backend-tests/node_modules/.bin/firebase emulators:exec --project demo-formlink --only auth,firestore './gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process'
```

La variante local utiliza `demo-formlink`. Las pruebas no deben enviar correos reales ni subir imágenes reales; la prueba externa de Cloudinary exige la opción explícita `realCloudinary=true`. Para las pruebas de navegador, instalar Chromium con `web/node_modules/.bin/playwright install --no-shell chromium` y seguir la guía web.

Debug desactiva telemetría por defecto; la verificación real de Analytics/Crashlytics usa `-PtelemetryEnabled=true` sin Firebase emulado. No registrar títulos, respuestas, correos, contraseñas ni texto libre como métricas.

El workflow `.github/workflows/quality.yml` define compilación, unitarias, Lint, pruebas instrumentadas, reglas y navegador. Trabajar con una rama por feature/fix y commits pequeños por tarea, y presentar los resultados en su PR; una ejecución local correcta no confirma por sí sola GitHub Actions.

## Documentación

- [Pendientes, prioridades y resultados comprobados](docs/PENDIENTES.md).
- [Backend e inyección de dependencias](docs/FIREBASE_BACKEND.md).
- [Publicación, web y despliegue](docs/PUBLIC_FORMS.md).
- [Portadas y Cloudinary](docs/CLOUDINARY_COVERS.md).
- [Seguridad y cifrado](docs/SECURITY.md).
- [Analytics y Crashlytics](docs/ANALYTICS.md).
- [Consultas de métricas](docs/analytics/metrics.sql).
- [Checklist de QA](docs/QA_CHECKLIST.md).
- [Entrega académica de la fase 3](docs/PHASE3_DELIVERY.md).

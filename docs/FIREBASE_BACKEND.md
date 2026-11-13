# Backend e inyección de dependencias

## Implementación actual

FormLink usa Firebase Authentication por correo/contraseña y Firestore. Android consume sus APIs HTTPS desde adaptadores de repositorio, con sesión cifrada por Android Keystore. Room cifrado conserva borradores, solicitudes descargadas y cambios pendientes. Los SDK oficiales de Analytics y Crashlytics están encapsulados aparte y no reciben información del contenido de formularios.

Los formularios editables son privados. Publicar confirma una copia independiente para el cliente web; los envíos públicos se validan mediante reglas y se guardan como snapshots inmutables. La bandeja Android observa Room por propietario y sincroniza estados. Los originales de las portadas siguen cifrados localmente; publicar puede subir una copia a Cloudinary y asociar su referencia a la copia pública. Ver [portadas](CLOUDINARY_COVERS.md). Sin configuración, el invitado puede editar y la cuenta muestra que Firebase no está configurado.

- [Fuente de verdad y sincronización](../README.md#fuente-de-verdad-y-funcionamiento-sin-conexión).
- [Publicación, web y despliegue Spark](PUBLIC_FORMS.md).
- [Sesión, cifrado y migraciones](SECURITY.md).
- [Métricas y diagnóstico](ANALYTICS.md).
- [Resultados actuales de verificación](VERIFICATION_PHASE3.md).

La configuración del proyecto real es local en `app/google-services.json`, ignorada por Git. Habilitar Email/Password y Firestore Standard, registrar la app WEB y autenticarse en Firebase CLI para desplegar reglas/índices/Hosting. No se usa Storage ni Functions ni se activa Blaze. BoM 34.7.0 mantiene compatibilidad con Kotlin 2.2.10; agregar Analytics no justifica declarar simultáneamente dos BoM distintos.

## Inyección de dependencias y Hilt

Inyectar una dependencia significa recibir un colaborador ya construido: por ejemplo, `EditFormViewModel` recibe `FormRepository` y `CoverStorage` por constructor. Así el ViewModel no abre bases de datos ni crea clientes Firebase, y sus pruebas pueden suministrar implementaciones controladas.

`AppContainer` y `BackendServices` componen las instancias de vida de aplicación: Room, almacenamiento de portadas, Auth REST, transporte HTTPS, publicación, telemetría, fuente remota y motor de sincronización. Cada sesión obtiene un repositorio ligado a su propietario y ViewModels propios. La factoría de WorkManager proporciona esos mismos servicios al worker. Los Composables reciben estado y callbacks.

Migrar a Hilt supondría agregar su plugin/procesador, marcar la Application con `@HiltAndroidApp`, la Activity con `@AndroidEntryPoint`, definir módulos `@Provides`/`@Binds`, elegir scopes y convertir los ViewModels a `@HiltViewModel` con constructor `@Inject`. Los workers requerirían la integración de Hilt para WorkManager. Se mantendrían interfaces, lógica y pruebas de repositorio; Hilt no implementa el backend, sincronización ni MVVM.

Para FormLink puede ser conveniente al crecer el grafo de Firebase, Room y tareas de fondo. En esta entrega se conserva inyección manual para que el equipo pueda explicar el flujo y revisar los cambios de backend sin otra migración transversal. Una migración posterior a Hilt debe ser un PR independiente, sin modificar comportamiento.

Al elegir scopes con Hilt, Room, el cifrador y el transporte pueden vivir en aplicación; un repositorio ligado a `ownerId` no debe convertirse en singleton. También habría que adaptar el propietario personalizado de ViewModels de sesión o una navegación por cuenta, conservando su limpieza al salir. No basta con añadir anotaciones: las pruebas de cambio de cuenta y restauración deben seguir pasando.

Referencias: [Firebase Android](https://firebase.google.com/docs/android/setup), [Authentication REST](https://firebase.google.com/docs/reference/rest/auth), [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent), [Hilt](https://developer.android.com/training/dependency-injection/hilt-android).


## Desarrollo aislado con Firebase Emulator Suite

La integración existente usa Auth REST y Firestore REST; no necesita otro SDK de autenticación. Con `-PfirebaseEmulators=true`, `BackendServices` crea la instancia `formlink-emulator` del proyecto ficticio `demo-formlink`, usa una clave de demostración y conecta Android a `10.0.2.2:9099` y `10.0.2.2:8080`. No hay alternativa automática hacia Firebase real si esos servicios fallan. El cliente web y los despliegues no forman parte de esta preparación.

Conservar el `google-services.json` real fuera de esta copia de desarrollo, por ejemplo en Downloads. `app/google-services.json` está ignorado por Git, pero no es necesario para los emuladores. El launcher local rechaza su presencia para evitar inicializar servicios del proyecto real. No se necesitan credenciales del responsable, cuentas de servicio ni `firebase deploy`.

### Herramientas y servicios

Usar JDK 21, el Android SDK indicado en `local.properties`, plataforma de compilación 36.1 y un AVD API 37. Las versiones fijadas de Firebase JS y rules-unit-testing requieren Node >=24.12; usar Node 24 aunque el workflow existente todavía declare Node 22. Firebase CLI es la dependencia de `backend-tests`, no una instalación global.

Desde la raíz de esta copia:

```bash
mise install node@24
mise x node@24 -- npm ci --prefix backend-tests
mise x node@24 -- npm run emulators --prefix backend-tests
```

Sin mise, activar primero Node 24.12 o superior y ejecutar los mismos comandos `npm`. La salida debe identificar `demo-formlink`, Auth en `127.0.0.1:9099` y Firestore en `127.0.0.1:8080`; el hub escucha en `127.0.0.1:4400`. Conservar los bindings de `firebase.json` y las reglas existentes. No habilitar acceso general ni HTTP global: Debug ya permite HTTP únicamente para las direcciones locales especificadas.

### AVD dedicado y ejecución

En Device Manager, crear un Pixel 9 API 37 con el nombre exacto `FormLink_Local_API_37`, independiente de los AVD y datos existentes. En esta máquina ya está creado. Iniciarlo en un puerto dedicado:

```bash
"$HOME/Android/Sdk/emulator/emulator" -avd FormLink_Local_API_37 -port 5580
```

En otra terminal, con los servicios iniciados:

```bash
bash scripts/run-firebase-local.sh
```

El script descubre el serial actual del AVD por su nombre, aunque Android Studio le asigne otro puerto. Verifica el nombre del AVD, los puertos locales y la ausencia del JSON real antes de instalar. Ejecuta `./gradlew installDebug -PfirebaseEmulators=true -PtelemetryEnabled=false`, limita la instalación mediante `ANDROID_SERIAL`, detiene el proceso previo y vuelve a abrir la app. No desinstala ni borra datos. Para ejecutar el comando Gradle manualmente, exportar primero `ANDROID_SERIAL=emulator-5580` y reiniciar el proceso de la app después de cambiar de entorno.

La telemetría está desactivada y el modo emulado no construye el uploader de Cloudinary. Usar únicamente cuentas y formularios ficticios; no habilitar `realCloudinary=true`. Las cuentas de emulador son distintas de las reales. Al detener los servicios, su estado es efímero salvo que se configure una exportación local explícita; Room conserva sus datos del AVD.

### Android Studio: conservar las opciones al usar Run

Abrir esta copia de trabajo como proyecto independiente. Elegir **FormLink Firebase local** en el selector de configuraciones y pulsar Run. La configuración compartida `.run/FormLink Firebase local.run.xml` es de tipo Shell Script y ejecuta el launcher, que encuentra el AVD dedicado por su nombre. El plugin Shell Script está incluido en Android Studio de esta máquina; debe estar habilitado. El script fija las dos propiedades en cada ejecución, evitando depender de argumentos temporales de la terminal o del daemon de Gradle. Los servicios y el AVD deben estar iniciados antes de Run.

Esta configuración utiliza exclusivamente el AVD llamado `FormLink_Local_API_37`, independientemente del puerto asignado por Android Studio. El selector de dispositivos del IDE no cambia ese destino; para fijar un serial manualmente se puede definir `ANDROID_SERIAL`, y el launcher seguirá rechazando cualquier AVD con otro nombre. No usar la configuración Android `app` para este flujo: omitir `-PfirebaseEmulators=true` selecciona el modo real por defecto; sin el JSON real solo se dispone del modo invitado. No cambiar el valor global del proyecto para ocultar esa diferencia.

### Comprobaciones reproducibles

Con Auth y Firestore locales activos:

```bash
./gradlew compileDebugKotlin testDebugUnitTest -PfirebaseEmulators=true -PtelemetryEnabled=false --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
mise x node@24 -- npm run test:running --prefix backend-tests
ANDROID_SERIAL=emulator-5580 ./gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -PtelemetryEnabled=false -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
```

`BackendRootTest` prueba registro y login desde la UI real, cierre de sesión y sincronización mediante WorkManager. `FirebaseBackendIntegrationTest` prueba recuperación de formularios en una base Room nueva y reintentos tras fallos. `RestBackendIntegrationTest.passwordResetUsesEmulatorCodeWithoutStartingSession` lee el código de recuperación en el endpoint local del emulador y confirma el cambio de contraseña; no espera correo real. La prueba externa de Cloudinary debe quedar omitida.


### Evidencia de esta preparación local — 12 de noviembre de 2026

Se comprobó con `git fetch origin main:main` que `main` y `origin/main` apuntaban a `147848a`. La copia original estaba en `feature/Eliminacion-formularios` con cambios pendientes; para conservarlos se creó una copia de trabajo independiente en `CC3087_myApp-firebase-local`, rama `chore/firebase-local-dev`, desde ese `main`. Las comprobaciones de hash confirmaron que el diff original y el archivo recibido de Firebase permanecieron intactos. El archivo real se validó sin mostrar su contenido y quedó fuera del repositorio. Ambos checkouts ignoran `app/google-services.json`.

Entorno comprobado: JDK 21.0.13, Android SDK con plataforma 36.1, Node 24.22.3 y Firebase CLI local 15.34.2. `npm ci --prefix backend-tests` se ejecutó con Node 24; no se cambiaron dependencias ni lockfiles. npm conserva 19 avisos de vulnerabilidades (14 moderadas y 5 altas) de las dependencias existentes; su actualización queda pendiente y no se aplicó `npm audit fix`.

Resultados:

- `compileDebugKotlin` y 161 pruebas unitarias: correctos.
- 11 pruebas de reglas sobre Auth/Firestore locales: aprobadas, sin cambiar las reglas.
- Suite instrumentada completa en `FormLink_Local_API_37` (`emulator-5580`, API 37): 71 casos, 70 aprobados, 1 omitido y ningún fallo. La omisión corresponde a la subida real a Cloudinary, que no se activó.
- `BackendRootTest`: 3 casos aprobados, incluyendo registro/login mediante la UI y sincronización real contra los emuladores locales con WorkManager. `FirebaseBackendIntegrationTest`: 1 aprobado. `RestBackendIntegrationTest`: 3 aprobados, incluida recuperación con código local y cambio de contraseña sin envío de correo real.
- Se compiló e instaló sin `app/google-services.json`; el BuildConfig generado contiene `FIREBASE_EMULATORS=true` evaluado mediante `Boolean.parseBoolean` y `TELEMETRY_ENABLED=false`. La instancia y los adaptadores existentes usan `demo-formlink` y `10.0.2.2`; el hub confirmó Auth `127.0.0.1:9099` y Firestore `127.0.0.1:8080`.
- Se leyó la configuración Run guardada y se ejecutaron exactamente su intérprete, script y serial: instalación en un solo dispositivo y lanzamiento en frío correcto. Se comprobó el formato contra el plugin Shell Script instalado. La revisión visual y el clic en Run dentro de la interfaz de Android Studio no se realizaron; abrir el checkout independiente y seleccionar la configuración indicada.
- El launcher rechazó el AVD existente `emulator-5556` antes de instalar o detener nada. Con los servicios apagados rechazó arrancar; una comprobación de UI con credenciales ficticias mostró el error de conexión al intentar login en la app emulada. Después se reiniciaron los servicios locales y el proceso de la app.

No se crearon proyectos reales, no se desplegó Firebase, no se enviaron correos reales ni se subieron imágenes a Cloudinary. Google Sign-In sigue sin implementarse. Los resultados son locales y no acreditan ejecución de GitHub Actions. Las diferencias de finales de línea de `gradlew.bat` quedaron fuera de los commits de esta tarea.

# Persistencia remota de formularios

## Alcance de esta entrega

El dato de dominio sincronizado es el formulario: título, campos completos, propietario y fechas. Firebase Authentication implementa registro, inicio y cierre de sesión por correo/contraseña. Firestore almacena `forms/{formId}`. No se publican formularios web ni se habilitan respuestas públicas. Las portadas y solicitudes permanecen locales; las solicitudes existentes pertenecen al modo invitado y no se transfieren al importar un formulario.

La app compila sin proyecto Firebase y mantiene el modo invitado. **Cuenta** muestra que el backend no está configurado. Nunca presenta una autenticación simulada como real.

## Configurar un proyecto real

1. Crear un proyecto en [Firebase Console](https://console.firebase.google.com/). Para Auth y Firestore de esta entrega no se utiliza Storage ni se requiere habilitar Blaze.
2. Registrar la app Android con paquete `com.uvg.cc3087.myapp`. Descargar `google-services.json` a `app/google-services.json` (archivo local ignorado por Git). El plugin Google Services se aplica cuando existe ese archivo.
3. En Authentication, habilitar el proveedor **Email/Password**. Crear Cloud Firestore en edición Standard y seleccionar la región del proyecto.
4. Instalar las herramientas de pruebas con `npm ci --prefix backend-tests`. Publicar las reglas del repositorio usando `backend-tests/node_modules/.bin/firebase deploy --only firestore:rules --project TU_PROJECT_ID`, autenticando previamente el CLI con la cuenta del proyecto. No usar reglas abiertas de test.
5. Compilar con `./gradlew assembleDebug` sin `-PfirebaseEmulators=true`. Entrar a **Cuenta**, crear una cuenta y volver a formularios.

La configuración Android contiene identificadores del proyecto, no una clave de servicio. No colocar credenciales administrativas ni cuentas de servicio dentro de la app. La versión Firebase BoM se fija en 34.7.0 por compatibilidad con Kotlin 2.2.10; la 35.1.0 requiere metadatos Kotlin posteriores. No se actualiza el toolchain como parte de este cambio.

## Fuente de verdad y coordinación

```text
Compose → ViewModel → FormRepository
                         ├── Room: lectura de UI y escrituras locales
                         └── cola durable → WorkManager → Firestore
AuthRepository → Firebase Authentication
```

- Room es la fuente de lectura de la interfaz. Cada edición se guarda localmente, junto con una revisión pendiente, en una transacción.
- Un repositorio queda ligado a un propietario durante toda su vida. Tanto listar como abrir por ID, editar y borrar verifican esa partición. `ownerId = null` identifica los borradores de invitado.
- Al iniciar sesión, abrir de nuevo la app, volver a primer plano, editar o pulsar **Sincronizar / reintentar**, se programa sincronización. WorkManager exige conectividad, conserva los trabajos y aplica reintentos. Al cerrar sesión se cancelan los trabajos de esa cuenta; los pendientes siguen en Room para su próximo inicio de sesión.
- Se suben pendientes y después se hace una lectura confirmada del servidor para reconciliar Room. Nunca se interpretan resultados de caché como una eliminación remota. No hay listener permanente: cambios de otro dispositivo se recuperan en la próxima sincronización.
- La confirmación incluye la revisión local enviada. Una respuesta tardía no borra la marca pendiente de una edición más reciente. Entre dispositivos gana la última escritura aceptada por el servidor; no hay edición colaborativa ni resolución por campo.
- Los borrados de cuentas autenticadas quedan como pendientes locales hasta ser confirmados. Repetir un envío o borrado utiliza el mismo ID de documento. Firestore puede incrementar su versión en un reintento, pero no crea otro formulario.
- La caché Firestore es de memoria: Room mantiene la única cola durable de la aplicación. Guardado local y sincronización remota tienen indicadores diferentes. Un fallo remoto no se reporta como pérdida del guardado local. Cada intento de sincronización tiene un límite de 30 segundos; un fallo conserva pendientes y permite reintentar.
- Al cambiar de cuenta se destruyen los ViewModels de la sesión anterior. Los trabajos y resultados comprueban su UID; nunca se aplican a la nueva cuenta.

## Invitados y portadas

Los borradores anteriores a Firebase se conservan como invitados mediante migración v3→v4. Iniciar sesión no los asigna automáticamente. En **Cuenta** se seleccionan y se importan explícitamente; la asignación local es transaccional y después se sincroniza.

Las imágenes se conservan en almacenamiento privado y Room guarda su referencia relativa. No se envían rutas, bytes ni referencias del dispositivo a Firestore. En una instalación nueva se recupera el formulario sin portada. Descargar cambios de texto conserva la portada que exista en ese dispositivo. Las solicitudes locales y sus instantáneas no se importan ni se sincronizan.

## Reglas mínimas

`firestore.rules` permite crear borradores del usuario autenticado, consultar y editar solo los propios y borrar de forma idempotente. Rechaza transferir propiedad, publicar mediante esta entrega, persistir rutas locales y acceder a respuestas. Las pruebas incluyen usuario propietario, otra cuenta y usuario no autenticado. Esto es la base de acceso necesaria para asociar datos a usuarios; la revisión ampliada y documentación de la rúbrica de seguridad quedan para otra fase.

## Pruebas con Firebase Emulator Suite

Se usa `demo-formlink`, sin proyecto real ni credenciales de producción. Requiere Node 22/24 y JDK 21. Desde la raíz:

```sh
npm ci --prefix backend-tests
npm test --prefix backend-tests
```

Ese comando levanta Auth en 9099 y Firestore en 8080, ejecuta las pruebas de reglas y apaga los servicios. Para probar Android contra esos mismos servicios:

```sh
npm run emulators --prefix backend-tests
# En otra terminal, con un emulador Android iniciado:
./gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true --max-workers=2
```

El modo emulador está disponible solo en debug, usa `10.0.2.2` y configura SDKs reales de Firebase. Release mantiene deshabilitada esa opción. `FirebaseBackendIntegrationTest` recupera datos en otra base Room, comprueba credenciales incorrectas y rechazo de otra cuenta. Sin el argumento `backendEmulators` esa prueba se omite explícitamente; las demás pruebas locales siguen disponibles.

## Demo real de persistencia

1. Configurar el proyecto real y desplegar reglas. Iniciar sesión o registrar una cuenta desde **Cuenta**.
2. Crear un formulario con una pregunta, usar **Continuar** y comprobar el guardado local. Esperar **Sincronizado con tu cuenta**.
3. Abrir Firestore Console y mostrar el documento con el mismo ID, propietario y campos. No debe contener `coverPath`.
4. Cerrar sesión y comprobar que ya no se muestran los formularios de esa cuenta. Volver a entrar y comprobar su recuperación.
5. Para demostrar que no provienen solo de Room, iniciar la misma cuenta en otra instalación/emulador sin datos previos y mostrar el formulario recuperado. La portada no se transfiere en esta fase.
6. Sin conexión, editar un dato y mostrar el guardado local pendiente. Reconectar y comprobar su sincronización. Mostrar un fallo de credenciales y su estado de error.

La ejecución con emuladores Firebase no sustituye el paso de mostrar datos en la consola del proyecto real. Esa demostración queda pendiente hasta que el equipo cree y configure su proyecto.

## Inyección de dependencias y Hilt

Inyectar una dependencia significa recibir un colaborador ya construido: por ejemplo, `EditFormViewModel` recibe `FormRepository` y `CoverStorage` por constructor. Así el ViewModel no abre bases de datos ni crea clientes Firebase, y sus pruebas pueden suministrar implementaciones controladas.

`AppContainer` y `BackendServices` componen las instancias de vida de aplicación: Room, almacenamiento de portadas, Auth, fuente remota y motor de sincronización. Cada sesión obtiene un repositorio ligado a su propietario y ViewModels propios. La factoría de WorkManager proporciona esos mismos servicios al worker. Los Composables reciben estado y callbacks.

Migrar a Hilt supondría agregar su plugin/procesador, marcar la Application con `@HiltAndroidApp`, la Activity con `@AndroidEntryPoint`, definir módulos `@Provides`/`@Binds`, elegir scopes y convertir los ViewModels a `@HiltViewModel` con constructor `@Inject`. Los workers requerirían la integración de Hilt para WorkManager. Se mantendrían interfaces, lógica y pruebas de repositorio; Hilt no implementa el backend, sincronización ni MVVM.

Para FormLink puede ser conveniente al crecer el grafo de Firebase, Room y tareas de fondo. En esta entrega se conserva inyección manual para que el equipo pueda explicar el flujo y revisar los cambios de backend sin otra migración transversal. Una migración posterior a Hilt debe ser un PR independiente, sin modificar comportamiento.

Referencias: [Firebase Android](https://firebase.google.com/docs/android/setup), [Authentication](https://firebase.google.com/docs/auth/android/password-auth), [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent), [Hilt](https://developer.android.com/training/dependency-injection/hilt-android).


## Verificación ejecutada — 4 de noviembre de 2026

- Etapa de portadas: 121 pruebas unitarias y 40 instrumentadas aprobadas en Pixel_9 / Android 17 (API 37).
- Backend: 129 unitarias y 47 instrumentadas aprobadas, sin errores ni omisiones, con Firebase Emulator Suite.
- Reglas Firestore: 3 pruebas aprobadas mediante `npm test --prefix backend-tests`.
- Sin configuración Firebase: 44 instrumentadas aprobadas y 2 pruebas de backend omitidas explícitamente en esa ejecución. La restauración adicional de sesión se verificó en la suite final de 47.
- Compilación Kotlin, compilación de instrumentadas y APK debug correctos. Se ejecutó pérdida de red del SDK, persistencia local pendiente, reconexión, reintento, registro/login reales en Auth emulado, worker de sincronización, cierre de sesión y recuperación en otra base Room.
- Las migraciones v1/v2/v3→v4 conservan datos previos. No se modificaron los esquemas exportados anteriores.
- La demostración en la consola Firebase real permanece pendiente: todavía no existe configuración del proyecto del equipo.

La verificación usó `--max-workers=2 -Pkotlin.compiler.execution.strategy=in-process` para reducir presión de memoria del entorno. No se cambió la configuración de Gradle del equipo para aplicar esos límites.

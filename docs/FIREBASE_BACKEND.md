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


## Política de eliminación de formularios

La acción Eliminar en Mis formularios pide confirmación y guarda la baja en Room. El borrador se oculta inmediatamente y se borran sus campos; se conserva el registro con `deleted=true` y su propietario original, incluso para invitados y después de confirmar la sincronización. Este marcador evita que un editor restaurado, un guardado atrasado o una descarga antigua recree el mismo ID o lo asigne a otra cuenta. Las eliminaciones recibidas por sincronización también conservan el marcador. Para cuentas, WorkManager reintenta la baja pendiente; confirmarla iguala `syncedRevision` con `revision` sin borrar el marcador. Cambiar de cuenta cancela la observación de formularios del repositorio anterior. Sin conexión el enlace publicado puede seguir aceptando respuestas hasta la confirmación del backend. El mensaje de éxito local no acredita cierre remoto.

Al sincronizar, todos los formularios conservan un registro privado en Firestore con `deleted=true` y `status=ARCHIVED`, incluidos los borradores nunca publicados. Si existe una copia pública, pasa a `active=false` en el mismo commit. Las escrituras utilizan precondiciones de versión. El enlace deja de permitir carga y nuevos envíos, aunque su URL siga existiendo en mensajes o marcadores. No existe integración con Google Forms ni acceso a enlaces externos de Google. Las reglas rechazan el borrado físico del documento, quitar su marca de eliminación, editar su contenido o reactivar su publicación. Así otro dispositivo con una edición pendiente no puede recrear un borrador eliminado. Al sincronizar, ese dispositivo reconoce la marca remota, descarta la edición antigua y oculta su copia local conservando el marcador; una publicación atrasada falla.

Las solicitudes y sus snapshots, estados e historial de lectura se conservan en su partición original. También se conservan las pruebas privadas de validación y los recibos no sensibles para confirmar intentos anteriores. No se eliminan subcolecciones de respuestas ni se transfieren datos entre cuentas. Esta es una eliminación del formulario con retención de solicitudes, no una purga definitiva de todos los datos.

Los campos locales se borran y las portadas privadas sin referencias activas se limpian junto con sus derivados; la limpieza de archivos es de mejor esfuerzo y no revierte una baja guardada. La baja pendiente conserva la ruta para volver a intentar la limpieza al confirmar la sincronización. Las imágenes públicas de Cloudinary y sus comprobantes de subida se retienen: el cliente no posee credenciales de destrucción. Una futura purga definitiva necesita un backend de confianza, autorización del propietario, política explícita de retención de solicitudes e imágenes, control de referencias, cola duradera de limpieza y reintentos idempotentes. No añadir secretos Cloudinary al APK.

Las reglas modificadas deben desplegarse por separado después de la revisión. Los cambios locales no modifican el proyecto Firebase real ni las imágenes públicas existentes.

### Regresión al eliminar y cambiar de cuenta

Se conservan los marcadores locales y remotos después de confirmar la baja, también para formularios nunca publicados. `observeForms` deja de recoger datos cuando su propietario ya no coincide con la sesión activa. Las pruebas cubren emisiones tardías de la cuenta anterior, cambio de sesión durante una baja remota, guardados restaurados, invitados eliminados y descargas antiguas.

`RestBackendIntegrationTest.unpublishedDeletionOnDeviceABlocksPendingEditFromDeviceB` usa dos bases Room independientes contra Firebase local: A sincroniza el borrador, B descarga y conserva una edición pendiente, A elimina y sincroniza, y B vuelve a sincronizar sin recrearlo. Se comprueba la marca remota, la ausencia de publicación, la limpieza de pendientes y el rechazo de otro guardado atrasado. La prueba de reglas usa dos clientes del mismo propietario y verifica que tampoco se puede quitar o borrar físicamente la marca. Sus fixtures utilizan `demo-formlink-rules`, separado de los datos de la app en `demo-formlink`.

La prueba de vista previa cierra el teclado antes de desplazar y pulsar, comprueba el contenido del campo y espera como máximo diez segundos a la confirmación. Registra el árbol semántico en el logcat capturado por CI, tanto si aparece la confirmación como si se agota la espera. Conserva la aserción de cero solicitudes guardadas. El mensaje de CI identifica la aserción fallida; los mensajes de ColorBuffer no demuestran su causa y la subida real a Cloudinary omitida es intencional.

Verificación de la corrección:

- Compilación Kotlin, 166 pruebas unitarias, Android Lint y APK: correctos. Lint informa 0 errores y 69 advertencias.
- Reglas contra Firestore local: 13 pruebas aprobadas, incluida la edición antigua de B después de eliminar el borrador en A.
- Suite instrumentada en Android 17/API 37: 78 casos, 77 aprobados y 1 subida externa a Cloudinary omitida; sin fallos. Pasan la vista previa y la integración de dos dispositivos simulados mediante bases locales independientes.
- La versión original de la prueba de vista previa pasó aislada y dentro de la suite completa local. La versión ajustada también pasó aislada y en la suite; el registro semántico del caso ajustado muestra `answer-name` con «Solo una prueba», la confirmación de éxito y ningún error de validación. No se reprodujo el fallo original de CI localmente, por lo que no se atribuye a una causa demostrada; la estabilidad del runner de CI debe confirmarse con su nueva ejecución.

No se desplegaron reglas ni se realizaron subidas reales a Cloudinary. Los resultados locales no equivalen a una ejecución de GitHub Actions aprobada.


### Verificación local de la eliminación (12 de noviembre de 2026)

- `./gradlew compileDebugKotlin testDebugUnitTest lintDebug assembleDebug --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process`: correcto; 164 pruebas unitarias, Lint sin errores (69 advertencias) y APK generado con configuración normal.
- `npm test --prefix backend-tests`: 12 pruebas aprobadas con Auth y Firestore locales. Incluyen cierre público, conservación de solicitudes/recibos, rechazo de otros propietarios y bloqueo de reactivación tras eliminación.
- `./gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process`: 74 casos, 73 aprobados y 1 omitido, sin fallos, en el AVD `Pixel_9_API_37` (API 37, 1080×2424). El caso omitido requiere habilitar una subida externa real a Cloudinary; no se activó. Se probó cancelación/confirmación desde la lista, limpieza local, conservación de solicitudes e historial, reintentos de baja y descarte de ediciones atrasadas.

La primera ejecución instrumentada detectó la retirada accidental de la entrada deshabilitada Responder y una prueba visual que confundía la nueva papelera roja con una portada. Se restauró la entrada y la prueba visual ahora captura la miniatura. La segunda suite completa terminó sin fallos. Los workflows de GitHub Actions se conservaron; estos resultados corresponden a ejecuciones locales. No se desplegaron reglas ni se purgaron imágenes del proyecto real.

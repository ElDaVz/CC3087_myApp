# Pendientes de FormLink

Entregas propuestas de alcance moderado, ordenadas para poder completarlas por grupo. Cada grupo tendrá una rama propia y commits por tarea pequeña; los grupos dependientes se iniciarán desde `main` cuando su base esté integrada. Este checklist define trabajo futuro: no implica que las funciones estén implementadas o verificadas.

## Prioridad de ejecución acordada

Las prioridades del usuario son dashboard, Google, botones de Mis formularios y cuenta con sincronización/importación, sin orden entre ellas. Se propone esta secuencia por dependencias, manteniendo entregas pequeñas:

| Orden | Entrega | Motivo y límite |
|---|---|---|
| 1 | Dashboard y respuestas nuevas (grupo 7) | Implementado en `feature/responses-dashboard`; verificación al final. |
| 2 | Importación automática con imágenes (grupo 1) | Implementado en `feature/automatic-guest-import`; verificación al final. |
| 3 | Cuenta y sincronización (grupo 2) | Presenta importación, progreso y reintentos en una cuenta clara y utilizable. |
| 4 | Acceso con Google (grupo 10) | Reutiliza el contrato de incorporación y separación de cuentas anterior. |
| 5 | Búsqueda, orden y fijados (grupo 5) | Da funcionalidad a búsqueda y más opciones de Mis formularios. |
| 6 | Accesos de configuración (primera tarea del grupo 8) | Conecta perfil y estadísticas ya existentes; exportación tendrá su propia entrega. |
| 7 | Eliminar desde más opciones (grupo 6) | Completa los botones tras definir conservación de datos y confirmación. |

Después: identidad visual (grupo 3), regreso tras publicación (grupo 4), preferencias/exportación restantes (grupo 8) e identidad empresarial/Open Graph (grupo 9). El banner empresarial sigue siendo una propuesta opcional.

Seguimiento de CI de navegación: el log aportado muestra `RootViewWithoutFocusException` en `Espresso.pressBack()` en `RequestNavigationTest.selectedRequestSurvivesSavedStateRestoration` y `RestoredCoverUiTest.editorResolvesCovers`. La corrección se realizó en `fix/navigation-tests-window-focus` y está en la base de esta entrega. No dar GitHub Actions por corregido hasta tener evidencia de una nueva ejecución.

## 1. Incorporación automática de borradores e imágenes

- [x] Tras confirmar registro o login desde invitado, incorporar automáticamente los borradores a la cuenta, incluida una cuenta existente después de cerrar sesión y volver a entrar. Sustituir la selección manual obligatoria y mantener separadas las cuentas previamente autenticadas.
- [x] Conservar las portadas locales, sus derivados cifrados, referencias e intención de portada al incorporar. No reasignar publicaciones de otro propietario ni publicar o subir imágenes a Cloudinary por importar.
- [x] Registrar la operación por cuenta en Room y transferir atómicamente, sin duplicados. Reanudar reservas pendientes tras reinicio o al volver a la cuenta, conservar originales ante fallos y mostrar progreso, resultado local y reintento en Mis formularios y Cuenta. La importación local no requiere red; la sincronización conserva su propio estado.

Contrato actualizado en `AGENTS.md`: una autenticación explícita captura los borradores invitados disponibles y los reserva para el propietario confirmado antes de persistir la sesión; la transferencia ocurre después. Restaurar una sesión solo reanuda reservas existentes. Las reservas sobreviven al cierre de sesión y otra cuenta no puede apropiárselas. Se conservan las solicitudes de invitado en su partición original. Room v8→v9 no destruye datos. Ver resultados al final.

## 2. Perfil y navegación de regreso

- [ ] Rediseñar el perfil tomando como referencia la imagen compartida: cabecera de identidad y opciones agrupadas en tarjetas o filas con iconos y jerarquía clara, usando morado, logo y tipografía de FormLink. Conservar sincronización y cierre de sesión; adaptar la importación al grupo 1. La referencia orienta la composición, sin añadir funciones inexistentes por copiar sus filas.
- [ ] Mostrar el estado real de sincronización de la cuenta (pendiente, en curso, completado o fallo), con reintento y resultado de la incorporación automática de borradores e imágenes del grupo 1. Verificar reinicio, desconexión y cambio de cuenta sin mezclar datos.
- [ ] Sustituir el texto «Volver» por una flecha con descripción accesible y área pulsable suficiente en perfil y autenticación; revisar la coherencia de los controles de regreso en el recorrido del editor y publicación.

Cierre del grupo: comprobar claro/oscuro, texto grande, teclado, navegación atrás y separación entre perfil y autenticación.

## 3. Identidad visual en Mis formularios y login

- [ ] Añadir el logo de FormLink en la parte superior derecha de Mis formularios, manteniendo accesibles búsqueda, perfil y demás acciones.
- [ ] Revisar el fondo y contraste del logo en login y Mis formularios para que sus formas no se mezclen con el contenedor, tanto en claro como en oscuro. Reutilizar los recursos de marca existentes.

## 4. Regreso después de publicar

- [ ] En la pantalla de copiar/compartir enlace, hacer que el botón de finalización regrese a Mis formularios tras confirmar el guardado y la publicación correctos. La acción de copiar debe seguir copiando el enlace.
- [ ] Mantener error y reintento cuando el guardado o la publicación fallen; al regresar, mostrar el formulario con su estado confirmado y evitar duplicados al pulsar varias veces.

## 5. Búsqueda y organización de formularios

- [ ] Buscar títulos similares sin exigir coincidencia exacta: ignorar mayúsculas, tildes y espacios externos, aceptar coincidencias parciales y tolerar errores tipográficos razonables. Definir ejemplos de relevancia y un estado sin resultados.
- [ ] Habilitar en el menú de más opciones la ordenación por fecha y por cantidad de respuestas, con dirección ascendente/descendente y etiquetas claras. Definir qué fecha se utiliza y derivar las cantidades de respuestas almacenadas.
- [ ] Permitir fijar y desfijar formularios, mantener los fijados visibles al ordenar y conservar la preferencia por propietario, incluido invitado. El menú incluirá también la acción de eliminar del grupo 6.

## 6. Eliminación con confirmación y recuperación

- [ ] Definir qué significa eliminar: retirar acceso público, conservar o eliminar solicitudes históricas, tratamiento de imágenes y diferencia entre archivado y eliminación definitiva. El comportamiento actual conserva respuestas; no cambiarlo de forma implícita.
- [ ] Conectar «Eliminar» del menú de más opciones a una interacción clara. Evaluar deslizar para eliminar o confirmar, inspirado en Spotify; si el gesto resulta poco descubrible o incómodo, usar confirmación explícita. Mantener siempre una alternativa accesible mediante botón y contemplar selección múltiple cuando corresponda.
- [ ] Añadir deshacer cuando la operación sea reversible; para una eliminación definitiva, mostrar sus consecuencias antes de confirmar. Evitar borrados accidentales y permitir reintentar ante fallos.

Dependencia: grupo 5 para integrar el menú. La eliminación definitiva requiere cerrar primero la decisión sobre los datos históricos.

## 7. Dashboard y respuestas nuevas

- [x] Crear el dashboard Android con Material 3: total de solicitudes, distribución por estado y evolución temporal, con acceso al detalle y cambio de estado. Los indicadores se calculan desde los resúmenes almacenados mediante repositorios y ViewModels.
- [x] Mostrar un pequeño punto rojo accesible en el acceso Respuestas cuando hay solicitudes sin revisar. Las filas también muestran «Sin revisar» y el indicador considera toda la partición activa, aunque el dashboard esté filtrado.
- [x] Marcar una respuesta revisada al cargar y mostrar correctamente su detalle. Conservar la lectura local por cuenta/formulario/respuesta tras reiniciar o cerrar sesión; no mezclar propietarios ni restablecer lecturas al sincronizar o cambiar estados.

Decisiones implementadas: Respuestas abre resumen y acceso a bandeja completa; selector de formulario y períodos 7 días, 30 días (predeterminado) y Todo; gráfica diaria o mensual y cinco respuestas recientes. Todas las respuestas existentes empiezan sin revisar. El estado de lectura es local a este dispositivo y no se sincroniza entre dispositivos. Room pasa a v8 mediante migración no destructiva v7→v8. Se mantiene el ciclo de descarga existente, sin push.

## 8. Configuración y exportación

- [ ] Dar funcionalidad al botón de configuración mediante opciones agrupadas: ver perfil, abrir estadísticas y exportar, reutilizando las pantallas correspondientes.
- [ ] Definir y ofrecer preferencias útiles de presentación, como apariencia y orden predeterminado, con persistencia por el alcance apropiado. Evitar controles sin comportamiento y mantener los textos en recursos de idioma.
- [ ] Definir el alcance y formato inicial de exportación de formularios/respuestas y permitir guardar o compartir el archivo con estados de progreso y error, respetando acceso del propietario.

Dependencias: grupos 2 y 7 para los destinos de perfil y estadísticas; grupo 5 para reutilizar la preferencia de orden.

## 9. Identidad de empresa y presentación de enlaces

- [ ] Permitir al propietario configurar nombre, logo e imagen de su empresa o producto, sin introducir equipos, roles ni colaboración. Definir almacenamiento y qué datos serán públicos.
- [ ] Usar esa imagen y los metadatos del formulario como vista previa Open Graph de los enlaces publicados. Evaluar cómo servir los metadatos en el HTML que leen los servicios de mensajería, su caché y el costo de alojamiento; no dar por resuelto el preview con un cambio solo en el cliente web. Comprobar el resultado con enlaces publicados y evidencia externa.
- [ ] Propuesta opcional: banner superior en Mis formularios con el nombre y la identidad de la empresa. Preparar una propuesta visual antes de implementarlo, contemplando cuentas sin empresa y pantallas estrechas.

Ideas adicionales por evaluar dentro de futuras entregas: título/descripción de enlace personalizables y vista previa antes de publicar. Mantener este grupo limitado a identidad y enlaces; exportación y estadísticas pertenecen a sus grupos propios.

## 10. Acceso con Google

- [ ] Añadir acceso con Google a través de `AuthRepository` y la integración Android apropiada; mantener el acceso por correo y como invitado. Configurar los clientes y las firmas necesarias sin guardar secretos en el repositorio.
- [ ] Reutilizar la incorporación de borradores e imágenes del grupo 1 al entrar desde invitado, conservando sesión cifrada, cierre de sesión y aislamiento entre propietarios. Definir el comportamiento si el correo ya pertenece a una cuenta para evitar cuentas duplicadas.
- [ ] Manejar cancelación, desconexión, proveedor no disponible y solicitudes duplicadas. Verificar en un entorno configurado el acceso y cambio de cuenta; los dobles de prueba no verifican por sí solos el acceso real con Google.

## 11. Experiencia de consulta y gestión de respuestas

- [ ] Mejorar la jerarquía visual del resumen, la bandeja y el detalle, manteniendo la identidad de FormLink. Facilitar la lectura de preguntas/respuestas y la gestión de estados, con controles accesibles y estados claros de carga, vacío y error.
- [ ] Orientar el recorrido a elegir un formulario y consultar sus respuestas y métricas. Mantener también el resumen de todos los formularios; conservar formulario seleccionado, filtros y desplazamiento al abrir un detalle y volver.
- [ ] Hacer más clara la relación entre filtros, estados de gestión y respuestas sin revisar; mantener el indicador independiente de los filtros y no marcar respuestas leídas solo por entrar en el resumen.

Entrega futura independiente, sin cambiar las prioridades acordadas. Reutilizar el dashboard existente; cerrar este grupo con revisión claro/oscuro, texto grande, navegación y accesibilidad.

## 12. Estadísticas por formulario y componentes

- [ ] Añadir métricas útiles de las preguntas del formulario seleccionado: distribución y porcentajes para opciones/casillas, resumen de valores numéricos y evolución de fechas. Para texto libre, empezar por cantidades respondidas/vacías, sin enviar contenidos a Firebase Analytics.
- [ ] Calcular desde snapshots de respuestas, conservando tipo de pregunta, identificadores de campo/opción y versión. Definir denominadores, ausencias y tratamiento de cambios de configuración antes de combinar resultados; no agrupar campos incompatibles solo porque tengan el mismo título.
- [ ] Mantener estadísticas generales de todos los formularios para volumen, estados y evolución temporal, con filtros coherentes. No mezclar preguntas diferentes entre formularios ni presentar conversiones o tiempos de respuesta sin datos que los sustenten.

Entrega futura independiente, coordinada con el grupo 11. El dashboard actual solo agrega resúmenes de solicitudes: estas estadísticas requieren una proyección apropiada de los componentes y respuestas, acotada al propietario.

## Otros pendientes y seguimiento existente

- [ ] Recuperar las portadas publicadas al iniciar sesión sin archivos locales, con actualización reactiva en tarjetas, editor y Preview. Implementación en `fix/restore-cloudinary-covers`; consultar la verificación y evidencia pendiente al final de este documento.
- [x] Completar la pantalla de login con la identidad visual de FormLink, registro, recuperación por correo, validaciones y estados de carga y error. Ver la verificación de autenticación al final. Los ajustes visuales adicionales se siguen en los grupos 2 y 3.
- [x] Corregir la selección incremental de Firebase real/emulado para evitar conexiones de login al puerto local 9099 después de ejecutar pruebas. Ver la verificación de entorno al final.
- [ ] Evaluar una URL más amigable que `formlink-9f735.web.app`: comparar un sitio Hosting con otro identificador y un dominio propio, incluyendo costos y cómo conservar los enlaces existentes. Coordinar con el grupo 9.

## Verificación del dashboard de respuestas (2026-11-12)

Implementado en `feature/responses-dashboard`, desde `main` que ya contiene la corrección anterior de navegación. La pestaña Respuestas abre indicadores, gráfica y cinco solicitudes recientes; tarjetas y botón de bandeja comparten los filtros de formulario y período. Volver desde detalle restaura su origen, filtros y desplazamiento. Las métricas se derivan de solicitudes almacenadas, deduplicadas por formulario/respuesta, sin leer sus respuestas ni consultar Analytics. Se conservan los snapshots de formularios eliminados.

La lectura se guarda en Room v8, en una tabla local separada de las solicitudes, con claves de partición invitado/cuenta y formulario/respuesta. Abrir correctamente el detalle marca lectura de forma idempotente y ofrece reintento si falla la escritura. El indicador global no depende de los filtros, y las descargas o cambios de estado no restablecen lecturas. No hay cambios de reglas remotas ni de publicación, push o sincronización de lecturas entre dispositivos. El README se reorganizó con funcionamiento, stack, preparación, uso, dashboard, arquitectura y límites actuales; la importación automática y Google siguen pendientes.

Comprobaciones realizadas:

- Compilación Kotlin y 161 pruebas unitarias correctas, sin omisiones. Cubren deduplicación, estados, filtros combinados, períodos de días naturales, zona horaria y cambio de horario, meses vacíos, fechas/estados ausentes, cambio de día, restauración de filtros, cancelación de sesión, reintento de lectura y resultados tardíos de otra selección.
- Android Lint correcto. El primer intento combinado perdió el daemon de Gradle; se completó al ejecutar con un solo worker. No se cambió la configuración global para resolverlo.
- Tanda final de 20 pruebas instrumentadas en Pixel_9 Android 17/API 37 con Firebase Auth/Firestore locales (`demo-formlink`): cero fallos, errores u omisiones. Incluye `ResponseDashboardTest` (4), `RequestNavigationTest` (5), `LocalRequestFlowTest` (2), `PublishedRequestFlowTest` (1), `RequestReadRepositoryTest` (2), `RoomMigrationTest` (1), `BackendRootTest` (3) y `LocaleResourcesTest` (2).
- Se comprueban migraciones desde v2/v3/v6/v7 a v8 sin pérdida de datos, lectura persistente tras reapertura, aislamiento invitado/cuentas/formularios, snapshot remoto resincronizado que conserva la lectura, cambio y reapertura de estado, navegación, filtros, restauración de estado y punto rojo independiente del período.
- Revisión visual local sobre capturas sintéticas: [claro](evidence/dashboard-light.png), [oscuro](evidence/dashboard-dark.png) y [texto al 150 %](evidence/dashboard-large-text.png). Se verificaron contraste, ajuste de textos y acceso mediante desplazamiento a la bandeja. La gráfica abre en su extremo más reciente. No equivale a revisión humana ni a una prueba de lector de pantalla.
- `git diff --check` y comprobación de credenciales/cleartext del repositorio correctos.

Incidencias resueltas durante la verificación: la primera prueba de captura buscaba un botón aún no compuesto; se corrigió el desplazamiento de la lista. Una ejecución ampliada detectó recomposición en un hilo de trabajo del arnés de cuenta y contaminación de sesión tras fallar. Se añadió `StandardTestDispatcher` al contexto de ese test y limpieza de la sesión emulada antes/después de cada caso; pasaron las tres pruebas de cuenta y posteriormente la tanda conjunta de veinte. No se alteró el dispatcher de sincronización de producción. La API de pruebas permite configurar el [contexto de ejecución de Compose](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/junit4/package-summary).

Pendiente de evidencia externa: ejecución de GitHub Actions de esta rama, revisión humana y prueba con lector de pantalla; no se han desplegado servicios ni enviado correos reales. La web no cambia y sus pruebas no se repiten en esta entrega. Las lecturas son locales a cada dispositivo y las novedades solo aparecen después de descargar las respuestas.

Comandos reproducibles:

```sh
./gradlew compileDebugKotlin testDebugUnitTest lintDebug --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process
backend-tests/node_modules/.bin/firebase emulators:exec --project demo-formlink --only auth,firestore './gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true -Pandroid.testInstrumentationRunnerArguments.class=com.uvg.cc3087.myapp.ResponseDashboardTest,com.uvg.cc3087.myapp.RequestNavigationTest,com.uvg.cc3087.myapp.LocalRequestFlowTest,com.uvg.cc3087.myapp.PublishedRequestFlowTest,com.uvg.cc3087.myapp.RequestReadRepositoryTest,com.uvg.cc3087.myapp.RoomMigrationTest,com.uvg.cc3087.myapp.BackendRootTest,com.uvg.cc3087.myapp.LocaleResourcesTest --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process'
```

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

## Verificación de selección de entorno Firebase (2026-11-12)

Arreglo en `fix/firebase-environment-selection`, basado en `feature/responses-dashboard`: ese trabajo todavía no estaba en `main` y se conserva para mantener el dashboard y Room v8 en la instalación existente.

Diagnóstico: el login fallaba con `ConnectException` hacia `10.0.2.2:9099`, aunque el `BuildConfig` generado indicaba `FIREBASE_EMULATORS=false`. El bytecode Kotlin de `BackendServices` conservaba valores `true` incrustados desde una compilación anterior para pruebas. No se inspeccionaron ni registraron credenciales. La comprobación identifica artefactos incrementales inconsistentes; no atribuye el fallo a un componente concreto del compilador.

La bandera ahora se inicializa mediante `Boolean.parseBoolean`, evitando que sea una constante de compilación. Por defecto se usa Firebase real; únicamente debug con `-PfirebaseEmulators=true` habilita los servicios locales. Valores distintos de `true` y `false` se rechazan al configurar el proyecto. No se cambian los endpoints, TLS, validaciones ni el almacenamiento de la app.

Comprobaciones realizadas:

- Reconstrucción inicial: `./gradlew clean assembleDebug compileDebugKotlin testDebugUnitTest --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process`. Correcta: 161 pruebas unitarias, cero fallos y cero omitidas. `clean` elimina artefactos de compilación, no datos del dispositivo.
- Ciclo real → emulado → real: tras la reconstrucción inicial se ejecutaron `assembleDebug assembleDebugAndroidTest -PfirebaseEmulators=true` y después `assembleDebug compileDebugKotlin` sin la propiedad, sin `clean` entre pasos. Ambas compilaciones pasaron. `javap` comprueba inicialización de la bandera con `true` y después `false`; los cinco accesos de `BackendServices` usan `getstatic BuildConfig.FIREBASE_EMULATORS`, y el bytecode de esa clase permanece idéntico entre modos.
- Android 17 (API 37), Firebase Auth/Firestore locales: las tres pruebas de `RestBackendIntegrationTest` pasaron sin omisiones. Incluyen registro/login, renovación, aislamiento por proyecto y recuperación con código del emulador; no se envían correos reales. Cada prueba usa un directorio propio para la sesión cifrada y lo elimina al terminar, sin sustituir la sesión instalada.
- Las pruebas se lanzaron con `adb shell am instrument`, después de instalar ambos APK mediante `adb install -r`, para conservar la instalación. Las huellas de los 16 archivos de datos ajenos a diagnósticos permanecieron idénticas antes y después de las pruebas; solamente rotaron archivos de Crashlytics.
- Se instaló finalmente el APK debug con Firebase real mediante `adb install -r`, se detuvo el proceso y se abrió `MainActivity`: arranque en frío correcto. La huella SHA-256 del APK instalado coincide con la del artefacto comprobado. Se conservan Room v8, borradores, imágenes y sesión; no hubo desinstalación ni limpieza de almacenamiento.

Pendiente de evidencia: login con la cuenta real del usuario y resultado de GitHub Actions. No se han dado por verificados mediante las pruebas locales. Los comandos `connectedDebugAndroidTest` anteriores se reservan para dispositivos de prueba; esta ejecución utilizó instalación con reemplazo e instrumentación directa para conservar los datos existentes.

## Verificación de incorporación automática (2026-11-12)

Implementado en `feature/automatic-guest-import` desde `main` actualizado. Tras registro o login confirmado se abre Mis formularios y se incorporan los borradores invitados capturados al enviar la autenticación. Una cuenta existente recibe el mismo tratamiento al volver a entrar. Se eliminó la selección manual obligatoria del perfil y se añadió un aviso accesible de incorporación en Mis formularios y Cuenta, con progreso, resultado local y reintento. El estado de sincronización remota se presenta por separado.

Room v9 añade `guest_imports` mediante migración explícita no destructiva v8→v9. Las reservas se vinculan al propietario confirmado por Firebase antes de persistir la sesión; no transfieren todavía los borradores. Una vez confirmada la sesión persistida, el coordinador aplica propietario, revisión pendiente y finalización en una misma transacción local. Los trabajos de sincronización también reanudan la incorporación antes de enviar revisiones. Restaurar una sesión solo reanuda reservas existentes: no captura nuevos invitados. Las reservas sobreviven al cierre de sesión, quedan excluidas de nuevas capturas y otra cuenta no puede reemplazarlas.

Se conservan identificadores, preguntas, fechas, intención y referencias de portada; no se copian ni descifran permanentemente los originales. Las portadas y sus derivados siguen cifrados en sus archivos privados. No se publican formularios ni se suben imágenes por incorporar. Las solicitudes históricas de invitado mantienen su partición. Los originales faltantes se conservan como referencias; esta entrega no implementa nuevos tipos de adjuntos ni recupera un archivo privado ya perdido.

Comprobaciones realizadas:

- Compilación Kotlin correcta. `testDebugUnitTest`: 167 pruebas, cero fallos, errores u omisiones. Las seis nuevas del coordinador cubren progreso, envíos simultáneos, reintento tras fallo, reconstrucción, cancelación, cambio de cuenta y fallo del planificador después del commit local.
- `assembleDebug`, `assembleDebugAndroidTest` y `lintDebug` correctos con `-PfirebaseEmulators=true`, un worker y compilador Kotlin en proceso. La compilación predeterminada `compileDebugKotlin assembleDebug`, sin Firebase emulado, también pasó y quedó como artefacto final.
- AVD temporal independiente `FormLink_Import_Test`, Android 17/API 37, sin utilizar los datos del AVD habitual. Con Firebase Auth/Firestore locales (`demo-formlink`), pasaron 21 pruebas instrumentadas, cero fallos, errores u omisiones: `GuestImportStoreTest` (4), `GuestImportAuthTest` (3), `GuestImportUiTest` (2), `RoomMigrationTest` (1), `BackendRootTest` (3), `RestBackendIntegrationTest` (3), `AuthScreenTest` (2) y `LocalRequestRepositoryTest` (3).
- Se comprueban reapertura de una base SQLCipher con reservas pendientes, transferencia única, rollback total ante propietario incompatible, reservas de otra cuenta, borradores nuevos excluidos al restaurar y borradores eliminados. Original y derivado cifrados mantienen exactamente sus bytes tras importar; preguntas, fechas y referencias se conservan y los datos siguen pendientes de sincronización hasta confirmación remota.
- Las pruebas con Firebase local verifican que un login fallido no reserva ni transfiere; registro y login existente preparan una operación que solo se transfiere después de la sesión; otra cuenta no puede apropiársela; una sesión reconstruida reanuda la operación. Una cancelación entre la reserva y la persistencia de sesión conserva originales y reserva para el siguiente login correcto. Se comprueba envío posterior del borrador al Firestore emulado sin publicación.
- Compose verifica progreso, reintento, confirmación local sin afirmar sincronización, navegación y cierre de sesión disponibles durante la incorporación, ausencia de selección manual y el recorrido integrado invitado → registro → borrador incorporado → cierre de sesión sin exponerlo como invitado.
- Migraciones desde v1/v2/v3/v6/v7/v8 hasta v9 conservan borradores y solicitudes; v8 conserva además la lectura existente. `git diff --check` y el comprobador del repositorio pasan. No se enviaron correos reales ni se realizaron subidas a Cloudinary.

Pendiente de evidencia externa: GitHub Actions y prueba con cuenta real. La recepción de correo, Google Sign-In y revisión humana/lector de pantalla no se dan por verificados. No cambió la web. Los grupos 11 y 12 se añadieron como trabajo futuro para experiencia de respuestas y estadísticas por componentes, sin alterar prioridades ni marcarlos como realizados.

Comandos de compilación y pruebas reproducibles:

```sh
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug -PfirebaseEmulators=true --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process
# Instalar ambos APK en un AVD dedicado; sustituir el serial si corresponde.
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
backend-tests/node_modules/.bin/firebase emulators:exec --project demo-formlink --only auth,firestore 'adb -s emulator-5556 shell am instrument -w -r -e backendEmulators true -e class com.uvg.cc3087.myapp.GuestImportStoreTest,com.uvg.cc3087.myapp.GuestImportAuthTest,com.uvg.cc3087.myapp.GuestImportUiTest,com.uvg.cc3087.myapp.RoomMigrationTest,com.uvg.cc3087.myapp.BackendRootTest,com.uvg.cc3087.myapp.RestBackendIntegrationTest,com.uvg.cc3087.myapp.AuthScreenTest,com.uvg.cc3087.myapp.LocalRequestRepositoryTest com.uvg.cc3087.myapp.test/androidx.test.runner.AndroidJUnitRunner'
./gradlew compileDebugKotlin assembleDebug --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process
```

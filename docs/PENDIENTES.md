# Pendientes de FormLink

Entregas propuestas de alcance moderado, ordenadas para poder completarlas por grupo. Cada grupo tendrá una rama propia y commits por tarea pequeña; los grupos dependientes se iniciarán desde `main` cuando su base esté integrada. Este checklist define trabajo futuro: no implica que las funciones estén implementadas o verificadas.

## Estado de integración (2026-11-16)

- **Implementado en main:** dashboard, lectura local de respuestas, autenticación y recuperación por correo, eliminación con confirmación, Cuenta, sincronización e incorporación automática con reservas de Room v9. La PR #33 se integró; su fix de cierre de sesión pasó los tres jobs de GitHub Actions.
- **Disponible en otra rama:** `refactor/remove-unused-paths` retira rutas antiguas y traslada sus pruebas útiles al código de producción. Su verificación se documenta al final; todavía requiere revisión e integración.
- **Archivos locales retirados:** los archivos npm sin consumidores de la raíz y la copia antigua de Pendientes se eliminaron durante la limpieza; nunca estuvieron versionados. Los esquemas Room y las configuraciones necesarias se conservan.
- **Pendiente:** integrar la limpieza, búsqueda/orden/fijados, navegación tipada, Google y las mejoras restantes.

Secuencia acordada: integrar Cuenta → limpiar con cobertura en una rama → integrar la limpieza → implementar búsqueda y organización en `feature/forms-search-organization`. Navegación y Google quedan para entregas posteriores. No crear checkouts temporales ni fusionar PRs sin petición del usuario.

## Experiencia de respuestas y estadísticas por componentes

- [ ] Mejorar bandeja y detalle de respuestas: jerarquía, búsqueda, filtros, estados vacíos, accesibilidad y navegación conservando lectura e instantáneas.
- [ ] Añadir estadísticas por pregunta/componente con agregación adecuada a cada tipo, valores ausentes y filtros consistentes; no registrar respuestas en Analytics.
- [ ] Definir exportación y comprobar conjuntos vacíos, formularios editados/eliminados y separación de propietarios.

## Entrega de Cuenta integrada

La PR #33 integró `feature/account-sync-clarity`: Cuenta, sincronización e incorporación automática sobre la eliminación existente, con compatibilidad de Room v9. La evidencia histórica de desarrollo se conserva al final.

## Prioridad de ejecución acordada

Las prioridades del usuario son dashboard, Google, botones de Mis formularios y cuenta con sincronización/importación, sin orden entre ellas. Se propone esta secuencia por dependencias, manteniendo entregas pequeñas:

| Orden | Entrega | Motivo y límite |
|---|---|---|
| 1 | Dashboard y respuestas nuevas (grupo 7) | Integrado en main; evidencia histórica al final. |
| 2 | Importación automática con imágenes (grupo 1) | Resuelve el paso de invitado a cuenta antes de añadir otra forma de acceso. |
| 3 | Cuenta y sincronización (grupo 2) | Presenta importación, progreso y reintentos en una cuenta clara y utilizable. |
| 4 | Limpieza con cobertura | Retirar caminos sin consumidores, trasladando primero sus escenarios útiles. |
| 5 | Búsqueda, orden y fijados (grupo 5) | Siguiente feature acordada, después de integrar la limpieza. |
| 6 | Navegación tipada y Navigation 3 | Conservar restauración y separación por cuenta. |
| 7 | Acceso con Google (grupo 10) | Reutilizar incorporación y separación de cuentas. |
| 8 | Accesos de configuración (primera tarea del grupo 8) | Conecta perfil y estadísticas ya existentes; exportación tendrá su propia entrega. |
| — | Eliminar desde más opciones (grupo 6) | Completa los botones tras definir conservación de datos y confirmación. |

Después: identidad visual (grupo 3), regreso tras publicación (grupo 4), preferencias/exportación restantes (grupo 8) e identidad empresarial/Open Graph (grupo 9). El banner empresarial sigue siendo una propuesta opcional.

Seguimiento de CI de navegación: el log aportado muestra `RootViewWithoutFocusException` en `Espresso.pressBack()` en `RequestNavigationTest.selectedRequestSurvivesSavedStateRestoration` y `RestoredCoverUiTest.editorResolvesCovers`. La corrección se realizó en `fix/navigation-tests-window-focus` y está en la base de esta entrega. No dar GitHub Actions por corregido hasta tener evidencia de una nueva ejecución.

## 1. Incorporación automática de borradores e imágenes

- [x] Al crear una cuenta desde una sesión invitada, incorporar automáticamente sus borradores a la cuenta y sustituir la selección manual como paso obligatorio. Definir el comportamiento del primer inicio de sesión en una cuenta existente, manteniendo separadas las cuentas previamente autenticadas.
- [x] Incluir las portadas e imágenes locales asociadas, conservando sus archivos privados cifrados y referencias al importar. Reutilizar referencias publicadas válidas cuando corresponda; incorporar una imagen a un borrador no debe publicarla ni subirla automáticamente a Cloudinary.
- [x] Hacer la incorporación recuperable ante cierre de app, falta de conexión o fallo parcial, sin duplicar formularios ni perder originales. Mostrar progreso, resultado y reintento cuando sea necesario.

Contrato integrado de Cuenta: registro e inicio de sesión explícitos desde invitado reservan e incorporan borradores tras confirmar la cuenta. Restaurar la sesión solo reanuda reservas anteriores. Se conservan límites entre propietarios; importar no publica ni sube imágenes. Disponible en `main` desde la PR #33.

## 2. Perfil y navegación de regreso

- [x] Rediseñar el perfil tomando como referencia la imagen compartida: cabecera de identidad y opciones agrupadas en tarjetas o filas con iconos y jerarquía clara, usando morado, logo y tipografía de FormLink. Conservar sincronización y cierre de sesión; adaptar la importación al grupo 1. La referencia orienta la composición, sin añadir funciones inexistentes por copiar sus filas.
- [x] Mostrar el estado real de sincronización de la cuenta (pendiente, en curso, completado o fallo), con reintento y resultado de la incorporación automática de borradores e imágenes del grupo 1. Verificar reinicio, desconexión y cambio de cuenta sin mezclar datos.
- [x] Sustituir el texto «Volver» por una flecha con descripción accesible y área pulsable suficiente en perfil y autenticación.
- [ ] Revisar la coherencia de los controles de regreso en el recorrido del editor y publicación.

Cuenta y autenticación: verificación local al final. Quedan revisión humana, lector de pantalla, teclado físico y coherencia del regreso en editor/publicación.

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

Decisiones acordadas para la siguiente feature (todavía sin implementar):

- Orden y fijados persistentes solo en este dispositivo, separados por invitado/cuenta; consulta restaurable dentro de la pantalla de su propietario. Los fijados no se transfieren automáticamente al importar.
- Fecha = última modificación numérica. Orden predeterminado reciente primero; cuatro opciones combinando fecha/respuestas y ambas direcciones, con ID como desempate.
- Búsqueda sin mayúsculas ni tildes, espacios normalizados y todas las palabras requeridas. Prioridad: exacta, prefijo, parcial y aproximada; tolerancia de una edición por palabra de 4–7 caracteres y dos desde 8, sin tolerancia para palabras cortas.
- Fijados primero entre coincidencias del filtro/búsqueda; después relevancia y orden elegido. Menú por tarjeta con fijar/desfijar y la eliminación confirmada existente. Estado sin resultados y limpieza de búsqueda/filtros.
- Preferencias mediante repositorio DataStore y ViewModel; añadir fecha numérica al resumen. No requiere cambios de backend ni esquema Room.

## 6. Eliminación con confirmación y recuperación

Implementado en main: eliminación explícita confirmada, retirada de publicación y conservación de solicitudes. Siguen pendientes el menú ampliado, recuperación/deshacer y selección múltiple.

- [x] Definir qué significa eliminar: retirar acceso público, conservar o eliminar solicitudes históricas, tratamiento de imágenes y diferencia entre archivado y eliminación definitiva. El comportamiento actual conserva respuestas; no cambiarlo de forma implícita.
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

## Verificación de Cuenta e incorporación automática (2026-11-16)

Entrega en `feature/account-sync-clarity`, activa en la carpeta original del proyecto, desde el commit documental `dc436dd` de main. Se reutilizaron selectivamente los cuatro commits de implementación de `feature/automatic-guest-import`, conservando la eliminación con tombstones y la configuración actual de Cloudinary. Los cambios útiles del checkout principal se consolidan en esta rama: el esquema v9 coincide exactamente y se incorporan sus comprobaciones ampliadas de migraciones y conservación durante cifrado.

Cuenta presenta identidad real, estado remoto y resultado local de incorporación en tarjetas independientes, con paleta morada clara/oscura, desplazamiento y regreso mediante flecha accesible. El cierre de sesión conserva datos y reservas, cancela trabajo por propietario y permite cancelar o confirmar cuando existen operaciones pendientes. No incorpora Google, perfil editable ni navegación nueva.

`AccountSyncRepository` entrega seis estados y contadores separados de borradores, eliminaciones y revisiones de estado de respuestas. El éxito requiere formularios, publicaciones, respuestas y comprobación transaccional de pendientes. Las imágenes privadas no se contabilizan como subidas pendientes. La última comprobación vive solo en memoria, ligada a la sesión; se descartan resultados anteriores incluso al volver a la misma cuenta. WorkManager agrupa solicitudes en un máximo de dos trabajos sin terminar por propietario; los cambios durante un pase conservan su revisión pendiente y provocan reintento o un pase posterior.

La incorporación reserva solo después de autenticar, se aplica atómicamente y conserva los archivos cifrados sin copiarlos ni subirlos. Restaurar la sesión reanuda reservas sin capturar nuevos invitados. Se omiten borradores eliminados antes de capturar o después de reservar; los tombstones impiden recrearlos desde editores antiguos. Room conserva esquemas y migraciones v1→v9, sin fallback destructivo.

Comprobaciones locales:

- Compilación Kotlin, APK debug y APK de pruebas correctos; 184 pruebas unitarias sin fallos ni omisiones. Cubren etapas remotas bloqueadas/fallidas, ausencia de éxito prematuro, revisiones durante un pase, coalescencia de cien solicitudes, cancelación, cuenta anterior, nueva sesión de la misma cuenta y reinicio del estado en memoria.
- Android Lint (`lintDebug`): cero errores, 72 advertencias y una sugerencia; no se amplía el alcance para corregir esas advertencias.
- Primera tanda de 19 instrumentadas en Pixel 9 Android 17/API 37, con datos temporales aislados y Firebase Auth/Firestore locales: cero fallos u omisiones. Incluye `AccountScreenTest` (3), `AccountSyncIntegrationTest` (1), `GuestImportStoreTest` (4), `GuestImportAuthTest` (3), `GuestImportUiTest` (2), `AuthScreenTest` (2), `BackendRootTest` (3) y `RoomMigrationTest` (1).
- Tanda definitiva tras el último cambio: **20 instrumentadas correctas, sin fallos ni omisiones**, incluidas las diecinueve anteriores y `NetworkConnectionTest`. Esta última deshabilita y restablece Wi-Fi/datos del dispositivo temporal, comprueba la espera por conexión y evita afirmar éxito al reconectar. El emulador se había cerrado antes de esta tanda; se reinició con datos temporales y se reinstalaron los APKs de pruebas.
- Firebase local confirma importación privada sin publicación, eliminación con tombstone y fallo de escritura de un estado de respuesta inexistente: este último mantiene el contador pendiente y no adelanta la última comprobación. Las unitarias inyectan por separado fallos de formularios, publicaciones y respuestas.
- Compose verifica acciones, flecha accesible, confirmación/cancelación de salida, importación y desplazamiento hasta cerrar sesión. Capturas sintéticas revisadas: [claro](evidence/account/account-light-top.png), [oscuro](evidence/account/account-dark-top.png), [texto al 150 %](evidence/account/account-large-text-top.png) y [controles inferiores con texto grande](evidence/account/account-large-text-bottom.png). Las capturas pueden mostrar posiciones intermedias de desplazamiento, no una página completa. No equivalen a revisión humana ni a uso con lector de pantalla.

Límites: no se han desplegado servicios, enviado correos reales, subido imágenes reales ni ejecutado GitHub Actions. La restauración se comprueba recreando repositorios y reabriendo Room; no se afirma una prueba completa de muerte del proceso Android. Falta revisión humana, TalkBack y teclado físico. La web permanece sin cambios y no se repiten pruebas de navegador. Navegación tipada/Navigation 3 sigue pendiente en otra rama. La entrega se publica en una única PR hacia `main`; la evaluación humana y el merge quedan a cargo del usuario.

Comandos reproducibles (requieren Firebase local y un dispositivo de pruebas aislado):

```sh
./gradlew compileDebugKotlin testDebugUnitTest assembleDebug assembleDebugAndroidTest lintDebug -PfirebaseEmulators=true --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process
adb -s emulator-5556 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5556 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5556 shell am instrument -w -r -e backendEmulators true -e class com.uvg.cc3087.myapp.AccountScreenTest,com.uvg.cc3087.myapp.AccountSyncIntegrationTest,com.uvg.cc3087.myapp.GuestImportStoreTest,com.uvg.cc3087.myapp.GuestImportAuthTest,com.uvg.cc3087.myapp.GuestImportUiTest,com.uvg.cc3087.myapp.AuthScreenTest,com.uvg.cc3087.myapp.BackendRootTest,com.uvg.cc3087.myapp.RoomMigrationTest,com.uvg.cc3087.myapp.NetworkConnectionTest com.uvg.cc3087.myapp.test/androidx.test.runner.AndroidJUnitRunner
```


## Normalización de la entrega para revisión en GitHub

`feature/account-sync-clarity` queda activa en la carpeta original del proyecto. El worktree temporal se retiró después de comprobar que todos sus commits estaban conservados. La configuración IDE distinta y el log local de aquel checkout se guardaron, fuera de Git, en `app/build/normalization-backup/retired-account-worktree/`. No se añadieron commits a `main` durante esta consolidación.

Se incorporan las pruebas locales ampliadas: `RoomMigrationTest` cubre v2→v9 hasta v9 existente, incluida ahora v4/v5; `LocalRequestRepositoryTest` cubre v1→v9. `SecureStorageTest` conserva reservas pendientes, completadas y de otro propietario durante conversión a SQLCipher y dos reaperturas. El esquema v9 y su implementación coinciden con los cambios locales anteriores.

Verificación de la rama consolidada desde la carpeta original: `compileDebugKotlin`, `testDebugUnitTest`, `lintDebug` y `assembleDebugAndroidTest` correctos; 184 unitarias sin fallos ni omisiones y Lint con cero errores, 72 advertencias y una sugerencia. Las pruebas instrumentadas modificadas se compilaron pero no se volvieron a ejecutar localmente: las 20 instrumentadas documentadas arriba corresponden a la revisión anterior. El workflow de la PR está configurado para ejecutar instrumentadas en Android 17 con Firebase local; su resultado debe comprobarse en GitHub.

Los archivos npm de la raíz y la copia antigua de Pendientes permanecen fuera de la entrega. Las futuras tareas pequeñas se guardan con commits en el proyecto; las features y fixes usan ramas, sin nuevos worktrees temporales. La PR reúne Cuenta, sincronización, importación y las pruebas de compatibilidad, y se deja al usuario la evaluación humana y el merge.


## Limpieza con cobertura (2026-11-16)

Rama `refactor/remove-unused-paths`, basada en `main` después de integrar Cuenta (PR #33). La feature de búsqueda/organización espera a la integración de esta limpieza, según la secuencia acordada.

- Se retira el repositorio Room antiguo y sus accesos DAO redundantes. Los ocho escenarios de almacenamiento se conservan en `OfflineFirstFormRepositoryStorageTest`, usando `OfflineFirstFormRepository` y `RoomFormStore`. También se adaptan editor, solicitudes, portadas y migraciones al mismo camino de producción mediante una sesión invitada aislada de pruebas.
- Se retiran la importación manual y el contador obsoleto. Sus casos de propietarios, borradores eliminados y reservas se verifican mediante `RoomGuestImportStore`. Se elimina el estado paralelo sin consumidores de `FormSyncEngine`; se mantienen resultados, exclusión mutua y pruebas de cancelación/reintento/revisiones.
- Se retiran ruta/ViewModel de la bandeja antigua. Sus cinco escenarios pasan a `ResponseDashboardInboxTest`: orden y deduplicación, fuente no configurada, recuperación, pérdida de acceso y resultados tardíos. Al trasladarlos se corrige el dashboard para vaciar datos, formularios e indicadores al perder autorización; un fallo de conexión sigue conservando caché.
- Se eliminan `PrivateCoverImage`, las dos pruebas de plantilla (aritmética y paquete), siete colores y 29 cadenas sin referencias, incluidas sus traducciones. No se retiran fixtures útiles, migraciones ni pruebas de cifrado/publicación.
- Los JSON y Markdown no tienen una exclusión general. Se conservan los nueve esquemas Room, las configuraciones y los manifiestos/locks npm de `web/` y `backend-tests/`. Firebase local y secretos siguen ignorados. Los archivos npm de la raíz solo declaraban un SDK Node sin consumidores y la copia `#PENDIENTES.md#` era un respaldo antiguo: eran archivos sin añadir, no archivos ignorados, y se eliminaron localmente. La documentación histórica con valor permanece identificada como tal.

Verificación local: `compileDebugKotlin`, `testDebugUnitTest`, `compileDebugAndroidTestKotlin` y `lintDebug` correctos. **185 unitarias, cero fallos, errores u omisiones**; la reducción de una corresponde únicamente a la prueba aritmética. Lint: cero errores, 36 advertencias y una sugerencia, sin `UnusedResources` (antes: 72 advertencias). Los APK de aplicación y pruebas también se generaron correctamente con `assembleDebug assembleDebugAndroidTest`. El control `scripts/check-repository.mjs` pasa.

Límites: las instrumentadas modificadas compilaron, pero no se ejecutaron localmente ni se utilizó el dispositivo del usuario. La nueva PR deberá ejecutar el workflow completo antes de integrar; el CI aprobado de Cuenta no verifica esta rama. Búsqueda, orden y fijados continúan pendientes.

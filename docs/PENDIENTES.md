# Pendientes de FormLink

Entregas pequeñas, una tarea por rama y commits por cambios relacionados. Las tareas dependientes comienzan desde `main` cuando su base esté integrada. Las tareas con [x] están implementadas en la rama indicada; las tareas con [ ] siguen pendientes. Implementación, integración y verificación se registran por separado. Cada actualización debe marcar lo completado y dividir las tareas parciales, conservando sus pendientes.

## Estado de integración (2026-11-19)

- **Implementado en main:** dashboard, lectura local de respuestas, autenticación y recuperación por correo, eliminación con confirmación, Cuenta, sincronización e incorporación automática con reservas de Room v9. La PR #33 se integró; su fix de cierre de sesión pasó los tres jobs de GitHub Actions.
- **Limpieza integrada en main:** la PR #34 integró `refactor/remove-unused-paths`, retirando rutas antiguas y trasladando sus pruebas útiles al código de producción.
- **Navegación integrada en main:** la base actual ya contiene Navigation 3, mejoras de dashboard/bandeja/detalle, estadísticas por componente y la corrección de sincronización de las pruebas de borradores. Este estado se comprobó en el checkout; no acredita por sí solo un resultado de GitHub Actions.
- **Configuración y apariencia integradas en main:** Cuenta, Estadísticas y tema Sistema/Claro/Oscuro por dispositivo ya están en la base actual. La [ejecución de la rama de Ajustes](https://github.com/ElDaVz/CC3087_myApp/actions/runs/44648156244) terminó correctamente. La ejecución posterior en main falló antes de arrancar los emuladores; se sigue por separado en el fix de CI. La revisión humana permanece pendiente.
- **Fix de CI en `fix/ci-configuration-cache`:** desactivar la caché de configuración únicamente en las tres invocaciones Gradle del workflow y conservar reportes de diagnóstico. Pendiente de integrar y comprobar en GitHub; no cambia código de la app.
- **Archivos locales retirados:** los archivos npm sin consumidores de la raíz y la copia antigua de Pendientes se eliminaron durante la limpieza; nunca estuvieron versionados. Los esquemas Room y las configuraciones necesarias se conservan.
- **Pendiente:** búsqueda/orden/fijados de formularios, búsqueda textual de respuestas, exportación, Google y las mejoras restantes sin marcar.

Cuenta, limpieza, navegación y Configuración/apariencia ya están integradas. La prioridad actual es estabilizar CI mediante un fix mínimo y continuar con búsqueda, ordenación, fijados y el menú vacío de Plantillas como tareas separadas. Google permanece pendiente. Cada tarea tendrá su rama desde main cuando su base esté integrada; commits por cambios relacionados y descripción de PR para que el usuario la cree y evalúe. No crear checkouts temporales, publicar ramas ni crear o fusionar PRs sin petición del usuario.

## Experiencia de respuestas y estadísticas por componentes

- [x] Mejorar bandeja y detalle: jerarquía, filtros por formulario/período/estado y sin revisar, estados vacíos, controles accesibles y navegación conservando lectura e instantáneas. Integrado en main; TalkBack y revisión humana siguen sin acreditarse.
- [ ] Añadir búsqueda textual a la bandeja de respuestas.
- [x] Añadir estadísticas por pregunta/componente con agregación adecuada a cada tipo, valores ausentes y filtros consistentes; no registrar respuestas en Analytics.
- [x] Cubrir con pruebas conjuntos vacíos, versiones de snapshots independientes de la edición/eliminación del formulario y separación de propietarios. Las estadísticas usan respuestas históricas, sin registrar su contenido en Analytics; ejecución de esta revisión documentada al final.
- [ ] Definir e implementar exportación de formularios/respuestas; seguimiento compartido con el grupo 8.
- [x] Migrar a rutas tipadas y Navigation 3, con restauración del destino y regreso al origen. Integrado en main; no completa el regreso a Mis formularios después de publicar del grupo 4.

## Entrega de Cuenta integrada

La PR #33 integró `feature/account-sync-clarity`: Cuenta, sincronización e incorporación automática sobre la eliminación existente, con compatibilidad de Room v9. La evidencia histórica de desarrollo se conserva al final.

## Prioridad de ejecución acordada

Prioridad vigente: terminar una tarea antes de iniciar la siguiente. Las antiguas agrupaciones de alcance moderado se dividen para facilitar revisión.

| Orden | Entrega | Estado y límite |
|---|---|---|
| 1 | Fix mínimo de CI | Implementado en `fix/ci-configuration-cache`; pendiente de integrar y verificar en GitHub. Configuración/apariencia ya integrada en main. |
| 2 | Búsqueda de formularios | Plan aprobado: `feature/forms-search` desde main tras integrar el fix de CI. Solo búsqueda, con las reglas del grupo 5. |
| 3 | Ordenación en el menú superior | Rama independiente después de integrar búsqueda; reutilizar sus filtros. |
| 4 | Fijados y menú por formulario | Rama independiente; conservar eliminación confirmada. |
| 5 | Menú superior de Plantillas | Definir una utilidad concreta o retirarlo; no añadir acciones ficticias. |

Dashboard, importación automática, Cuenta, limpieza, navegación y Configuración/apariencia ya están en main. Google, identidad visual, regreso tras publicación, exportación e identidad empresarial conservan sus pendientes. Cada nueva entrega parte del main que contenga sus dependencias; no se crean ramas encadenadas por anticipado.

Seguimiento histórico de CI de navegación: el log anterior muestra `RootViewWithoutFocusException` en `Espresso.pressBack()` en `RequestNavigationTest.selectedRequestSurvivesSavedStateRestoration` y `RestoredCoverUiTest.editorResolvesCovers`. La corrección se realizó en `fix/navigation-tests-window-focus` y está en la base de esta entrega. El job 135866579663 reportó después un fallo distinto: no existía el nodo `request-a/shared` al pulsarlo. Esta revisión desplaza explícitamente la LazyColumn antes de seleccionar filas y comprobar respuestas. No dar GitHub Actions por corregido hasta tener evidencia de una nueva ejecución.

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

Decisiones funcionales acordadas (todavía sin implementar; se repartirán entre búsqueda, ordenación y fijados):

- Orden y fijados persistentes solo en este dispositivo, separados por invitado/cuenta; consulta restaurable dentro de la pantalla de su propietario. Los fijados no se transfieren automáticamente al importar.
- Fecha = última modificación numérica. Orden predeterminado reciente primero; cuatro opciones combinando fecha/respuestas y ambas direcciones, con ID como desempate.
- Búsqueda sin mayúsculas ni tildes, espacios normalizados y todas las palabras requeridas. Prioridad: exacta, prefijo, parcial y aproximada; tolerancia de una edición por palabra de 4–7 caracteres y dos desde 8, sin tolerancia para palabras cortas.
- Fijados primero entre coincidencias del filtro/búsqueda; después relevancia y orden elegido. Menú por tarjeta con fijar/desfijar y la eliminación confirmada existente. Estado sin resultados y limpieza de búsqueda/filtros.
- Preferencias mediante repositorio DataStore y ViewModel; añadir fecha numérica al resumen. No requiere cambios de backend ni esquema Room.

Plan aprobado para la próxima tarea corta de búsqueda (`feature/forms-search`, después de integrar el fix de CI):

- La lupa despliega un campo sobre la lista, con foco y teclado. La consulta combina con Todos/Activos/Borradores y se conserva al abrir un formulario y regresar.
- Limpiar vacía la consulta conservando campo y filtro; cerrar también oculta campo y teclado. Atrás primero cierra el teclado y después la búsqueda.
- Consulta y despliegue se restauran con el SavedStateHandle cifrado de la sesión; se separan por propietario y no se guardan en DataStore ni telemetría. El contrato FormRepository permanece sin cambios.
- Motor independiente de Compose: cálculo fuera del hilo principal y cancelación de consultas obsoletas. Normalizar mayúsculas, tildes y espacios; exigir todas las palabras. Relevancia exacta, prefijo, parcial y aproximada, conservando el orden del repositorio en empates.
- Admitir inserción, eliminación, sustitución y transposición adyacente: una edición en palabras de 4–7 caracteres, dos desde 8 y ninguna en palabras más cortas. `inscripcion` encuentra «Inscripción»; `pediod` encuentra «Pedido»; `pedido mensual` exige ambas palabras.
- Diferenciar lista inicialmente vacía, filtro sin coincidencias y búsqueda sin resultados; ofrecer limpiar consulta o restablecer consulta y filtro. Mantener carga, error y reintento.
- Validar relevancia, tolerancia, filtros, actualizaciones, restauración, aislamiento de sesiones, teclado y regreso desde editor; revisar claro, oscuro y texto grande. Separar commits de motor/estado, UI/pruebas y documentación.


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

- [x] Conectar Configuración desde Mis formularios, Plantillas, Dashboard y Bandeja; abrir Cuenta o autenticación y Estadísticas reutilizando sus pantallas. Integrado en main desde la entrega `feature/settings-appearance`.
- [x] Ofrecer apariencia Sistema (predeterminada), Claro y Oscuro, persistente por dispositivo para invitado y cuentas, con la paleta morada de FormLink. DataStore mediante repositorio y ViewModel independientes de la sesión; error/reintento sin perder la elección confirmada.
- [ ] Añadir el orden predeterminado de formularios cuando se implemente la entrega de ordenación.
- [ ] Añadir el acceso a exportación cuando exista una exportación funcional; no mostrar un botón vacío.
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

- [x] Recuperar las portadas publicadas al iniciar sesión sin archivos locales, con actualización reactiva en tarjetas, editor y Preview. Implementación de `fix/restore-cloudinary-covers` incluida en la base; esta rama añade manejo del fallo al cargar una portada local. Consultar la verificación histórica y sus límites al final.
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


## Comparación de mejoras-navegacion-respuestas (2026-11-19)

Comparación de los seis commits `6232940`…`6509df6` con `origin/main` (PR #34):

| Cambio de la rama | Estado frente al checklist |
|---|---|
| Rutas tipadas y Navigation 3 | Implementado: rutas con identificadores, restauración y regreso al origen. El grupo 4 sigue pendiente. |
| Dashboard | Mejora del grupo 7 ya completado: jerarquía, indicadores, gráfica y estados de carga/error/caché. |
| Bandeja y detalle | Implementados filtros, resumen, estados vacíos, lectura y presentación de snapshots. Búsqueda textual pendiente. |
| Estadísticas por componente | Implementadas para los nueve tipos existentes, con períodos, variantes históricas, ausentes/inválidos y aislamiento por formulario/propietario. No implementa exportación. |
| Lint y plurales | Ajustes de recursos ES/EN, plurales de importación, Preview, gráfica y declaraciones Android. |
| Portadas y estados desconocidos | Manejo del error de lectura de portada local y conservación de estados históricos desconocidos; pruebas de metadatos y números canónicos remotos. |

Corrección del job 135866579663: `RequestNavigationTest` buscaba filas que la lista diferida todavía no había compuesto. Se usa `performScrollToNode` sobre `request-list` antes de cada selección y sobre `request-detail-list` antes de comprobar la respuesta visible. Se conservan ambos regresos, restauración y posición de lista; no se añaden pausas ni se alteran producción o el workflow.

La evidencia anterior es histórica y corresponde a sus revisiones respectivas. Los resultados de esta corrección se registran a continuación; el CI remoto requiere una nueva ejecución.

Verificación local de esta revisión:

- `compileDebugKotlin`, `testDebugUnitTest`, `compileDebugAndroidTestKotlin` y `lintDebug`: correctos. **234 unitarias, sin fallos, errores ni omisiones**. Lint: cero errores y 29 advertencias.
- `RequestNavigationTest`: **5/5 correctas** en Pixel 9 Android 17/API 37. Incluye el caso que falló en CI, ambos controles de regreso, restauración y posición de lista.
- Suite instrumentada completa con Firebase Auth/Firestore locales: ejecución correcta. El XML de resultados registra **127 casos: 126 correctos y 1 omitido**, sin fallos ni errores. La omisión es `CloudinaryPublicationTest.realUnsignedUploadOfASyntheticImage`, que requiere activar explícitamente una subida real; no se activó. La consola de Gradle imprime 128 al finalizar, por lo que se usa el conteo verificable del XML.
- `git diff --check` y `scripts/check-repository.mjs`: correctos. Se usó el AVD existente en modo `-read-only -no-snapshot`, sin guardar los cambios de esta sesión. No se utilizaron dispositivos físicos ni se desplegaron servicios.

Comandos de pruebas (con Auth/Firestore locales activos en `demo-formlink`):

```sh
./gradlew compileDebugKotlin testDebugUnitTest compileDebugAndroidTestKotlin lintDebug --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
./gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true -Pandroid.testInstrumentationRunnerArguments.class=com.uvg.cc3087.myapp.RequestNavigationTest --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
./gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process
```

Pendientes de evidencia: nueva ejecución de GitHub Actions tras publicar los commits, revisión humana, TalkBack y teclado físico. La web no cambia. La corrección y el checklist se conservan en commits separados de esta rama; no se realiza merge.

Seguimiento del job 135930418496: la API de GitHub confirma `head_sha=6509df6dc2b65d66aaaa8c00a040addea927f68c`, intento 2 de la ejecución 44610807471. La branch remota todavía apuntaba a ese commit: el reintento no incluía la corrección `17a24d9` ni el checklist `31f5144`, que seguían locales. Ese fallo corresponde al código anterior y no verifica la corrección. Se publican los commits para generar una ejecución nueva; el resultado se registra tras comprobarla.

### Ajuste del pipeline tras el reporte de empaquetado

- [x] Separar la generación de los APK del arranque de Android/Firebase, con un worker y daemon de compilación descartable para reducir el uso simultáneo de memoria.
- [x] Añadir `--stacktrace --console=plain` y conservar logs de compilación como artifacts en ambos jobs Android. El paso Bash conserva el código de fallo de Gradle mediante `pipefail`.
- [x] Identificar el fallo del log completo posterior: `FormDraftFlowTest.aFailedEditSurvivesReturningToFormsAndReopening` no encuentra «Editar» en el árbol combinado, aunque Compose confirma un nodo en el árbol sin combinar. Ese log llega a las instrumentadas; no acredita un fallo de empaquetado.
- [ ] Confirmar el resultado remoto tras corregir el selector de «Editar».

Por petición del usuario, este ajuste se guarda en un commit sin ejecutar pruebas, publicar commits ni esperar el pipeline.


### Corrección del selector Editar en pruebas de borradores

- [x] Buscar «Editar» con `useUnmergedTree = true` y desplazarlo a la vista antes de pulsarlo. `FormDraftFlowTest` espera la aparición de la acción tras la carga asíncrona de Room y reutiliza el acceso en creación/reapertura y edición fallida. Se ajusta el mismo selector en `LocalRequestFlowTest`.

Se conserva la comprobación de que la edición fallida sobrevive al regreso y se guarda al reintentar. Cambio limitado a pruebas y seguimiento; sin ejecución de pruebas, push ni espera de CI, por petición del usuario.


## Configuración y apariencia (2026-11-19)

Implementado en `feature/settings-appearance`, desde el main que ya incluye navegación y la corrección de sincronización de pruebas. La rama contiene commits separados para persistencia/tema global, pantalla/navegación y documentación/evidencia. La creación y evaluación de la PR quedan a cargo del usuario.

Configuración abre desde las barras inferiores de Mis formularios, Plantillas, Dashboard y Bandeja. Cuenta reutiliza autenticación para invitado y perfil para sesión iniciada. Estadísticas reutiliza el Dashboard mediante un destino restaurable propio para regresar a Configuración; Formularios siempre regresa a la raíz. Se conservan filtros, y el acceso repetido a Configuración reutiliza su destino existente.

Apariencia ofrece Sistema, Claro y Oscuro con paleta morada uniforme. La preferencia se guarda en Preferences DataStore por dispositivo y permanece al cambiar de cuenta o cerrar sesión. El repositorio y su ViewModel viven fuera de la sesión; la UI no accede directamente a DataStore. El tema se aplica desde la raíz y las pantallas de producción heredan su selección. Las barras del sistema reciben el modo elegido. La carga inicial espera la preferencia; un fallo permite abrir la app y ofrece reintento en Configuración. Una escritura fallida conserva la selección confirmada. No hay cambios de Room, backend o publicación.

Verificación local:

- Compilación de Kotlin y de las instrumentadas correcta; **242 unitarias, cero fallos, errores u omisiones**. Incluyen cierre/reapertura real del archivo DataStore, valores desconocidos, configuración del sistema, fallos de lectura/escritura, reintento, confirmación de escritura y bloqueo de peticiones duplicadas.
- **Lint sin errores, con 29 advertencias.**
- **Suite completa en Pixel_9 Android 17/API 37 con Firebase Auth y Firestore locales: 134 pruebas, 133 correctas, cero fallos/errores y una omitida**: la subida real optativa a Cloudinary. La tanda enfocada de Ajustes/presentación/navegación también pasó: diez pruebas sin omisiones.
- Las pruebas de Ajustes verifican sus cuatro orígenes, regreso, Cuenta, Estadísticas, acceso directo a Formularios y restauración. Comprueban selección inmediata, seguimiento del sistema, error/reintento y conservación de un borrador al cambiar tema. La sustitución del subárbol por propietario se comprueba en un arnés sintético; la suite completa ejecuta por separado los recorridos existentes de registro, inicio y cierre de sesión contra Firebase local.
- Capturas sintéticas revisadas: [claro](evidence/settings/settings-light.png), [oscuro](evidence/settings/settings-dark.png) y [texto al 150 %](evidence/settings/settings-large-text-top.png). Las pruebas recorren los accesos y las tres opciones de tema. La prueba de presentación se ejecutó después directamente con el runner para conservar las imágenes antes de la desinstalación automática de Gradle.
- Chequeo de seguridad del repositorio y `git diff --check` correctos.

Durante la verificación ampliada, el daemon compartido de Gradle se cerró al iniciar las instrumentadas, después de completar compilación, unitarias y Lint. El runner canceló esa tanda tras tres pruebas. La ejecución posterior de todas las instrumentadas con `--no-daemon` terminó correctamente; no se cambió la configuración global de Gradle.

Comando reproducible de la suite instrumentada (requiere un emulador Android conectado):

```sh
backend-tests/node_modules/.bin/firebase emulators:exec --project demo-formlink --only auth,firestore './gradlew connectedDebugAndroidTest -PfirebaseEmulators=true -Pandroid.testInstrumentationRunnerArguments.backendEmulators=true --no-daemon --console=plain --max-workers=1 -Pkotlin.compiler.execution.strategy=in-process'
```

Pendiente: revisión humana, TalkBack, teclado físico y ejecución de CI de esta entrega. Búsqueda, ordenación, fijados, menú de Plantillas y exportación conservan sus tareas independientes. La web permanece sin cambios; sus pruebas no se repiten en esta entrega.


## Fix mínimo de caché de configuración en CI (2026-11-19)

Rama `fix/ci-configuration-cache`, basada en main después de integrar Configuración y apariencia. Cambio limitado al workflow y su seguimiento; la búsqueda aprobada se implementará en `feature/forms-search` cuando este fix esté integrado.

Diagnóstico: la [ejecución 44650319212 en main](https://github.com/ElDaVz/CC3087_myApp/actions/runs/44650319212) pasó los jobs Android y web/reglas. Falló `Build instrumentation APKs before starting emulators`, antes de iniciar el emulador Android o Firebase. La rama de Ajustes había [pasado su workflow](https://github.com/ElDaVz/CC3087_myApp/actions/runs/44648156244). El log de main muestra un error al serializar `ProcessNavigationXmlTask`, seguido de un NPE en `ModuleVersionResolveException.getMessage`. Este último coincide con el [bug de Gradle #36284](https://github.com/gradle/gradle/issues/36284), que dificulta mostrar fallos de resolución de dependencias. No se ha identificado la dependencia ni la causa original del fallo de GitHub; el nombre de la tarea no demuestra que sobre Navigation 3.

Decisión: usar `--no-configuration-cache` en las tres invocaciones Gradle de CI: compilación/unitarias/Lint/APK, ensamblado de APKs instrumentados y ejecución instrumentada con Firebase local. `org.gradle.configuration-cache=true` permanece activo para desarrollo local. Se mantienen `setup-gradle`, cachés de dependencias/npm/Firebase, versiones del toolchain, emulador y todos los controles existentes. Con la configuración actual, `setup-gradle@v4` no restaura la caché de configuración entre ejecuciones porque no se ha configurado `cache-encryption-key`; la [documentación de la acción](https://github.com/gradle/actions/blob/v4/docs/setup-gradle.md#saving-configuration-cache-data) distingue esa caché de las dependencias.

Los artefactos Android e instrumentados incluyen ahora `build/reports/configuration-cache/` y `build/reports/problems/`, además de logs y resultados existentes. Se mantiene el fallo del job cuando Gradle falla: los bloques Bash que capturan logs se ejecutan con `-e -o pipefail`. Desactivar la caché evita esa vía de serialización; si GitHub muestra después un fallo de dependencias, deberá diagnosticarse con el artefacto y el error concretos. No se considera garantizada la resolución del error remoto por un ensamblado local con dependencias ya descargadas.


Validación local del fix:

- YAML parseado correctamente; se verifican las tres invocaciones con `--no-configuration-cache`, las rutas de diagnóstico en ambos artefactos y la propiedad local de caché sin cambios. Sintaxis Bash correcta para los tres bloques extraídos del propio YAML.
- El bloque Android exacto, ejecutado con `bash --noprofile --norc -eo pipefail`, completó compilación, **242 unitarias sin fallos/errores/omisiones**, Lint sin errores (29 advertencias) y `assembleDebug`. El bloque previo a los emuladores completó `assembleDebug assembleDebugAndroidTest` con los argumentos actualizados. Logs locales en `ci-logs/android-build.log` y `ci-logs/instrumented-build.log`, ignorados por Git.
- Se comprobó también que una tubería fallida con `tee` mantiene código de salida 1 bajo `pipefail`.
- El comando instrumentado exacto del YAML pasó en Pixel_9 Android 17/API 37, con Firebase Auth/Firestore locales: **134 pruebas, 133 correctas, cero fallos/errores y una omitida**, correspondiente a la subida real optativa a Cloudinary.
- El primer intento instrumentado se interrumpió con `device offline` tras 31 pruebas y el dispositivo dejó de aparecer en ADB. El registro XML marcó el caso en curso como fallido sin texto de aserción. Se conservaron sus salidas en `app/build/ci-validation/device-offline-attempt/`; tras reiniciar el mismo AVD en frío, el comando completo pasó. No se atribuye esa desconexión al fallo de caché de GitHub ni se afirma una causa del cierre del emulador.
- Chequeo de seguridad del repositorio y `git diff --check` correctos. El workflow de web/reglas no cambia y sus pruebas no se repiten en esta entrega.

La validación local usa dependencias ya descargadas. La ejecución de esta rama en GitHub y la integración en main permanecen pendientes; no se declara corregido el fallo remoto hasta disponer de esa evidencia. Los commits quedan locales y se entrega la descripción de PR para que el usuario la cree y evalúe. Después de integrar este fix se iniciará la rama de búsqueda desde main.

# Pendientes de FormLink

Entregas propuestas de alcance moderado, ordenadas para poder completarlas por grupo. Cada grupo tendrá una rama propia y commits por tarea pequeña; los grupos dependientes se iniciarán desde `main` cuando su base esté integrada. Este checklist define trabajo futuro: no implica que las funciones estén implementadas o verificadas.

## 1. Incorporación automática de borradores e imágenes

- [ ] Al crear una cuenta desde una sesión invitada, incorporar automáticamente sus borradores a la cuenta y sustituir la selección manual como paso obligatorio. Definir el comportamiento del primer inicio de sesión en una cuenta existente, manteniendo separadas las cuentas previamente autenticadas.
- [ ] Incluir las portadas e imágenes locales asociadas, conservando sus archivos privados cifrados y referencias al importar. Reutilizar referencias publicadas válidas cuando corresponda; incorporar una imagen a un borrador no debe publicarla ni subirla automáticamente a Cloudinary.
- [ ] Hacer la incorporación recuperable ante cierre de app, falta de conexión o fallo parcial, sin duplicar formularios ni perder originales. Mostrar progreso, resultado y reintento cuando sea necesario.

Cambio de contrato: hoy los borradores de invitado no se transfieren automáticamente. Al implementar este grupo, actualizar ese contrato en `AGENTS.md`, conservar los límites entre propietarios y verificar cuenta nueva, cuenta existente, reintento e imágenes.

## 2. Perfil y navegación de regreso

- [ ] Rediseñar el perfil tomando como referencia la imagen compartida: cabecera de identidad y opciones agrupadas en tarjetas o filas con iconos y jerarquía clara, usando morado, logo y tipografía de FormLink. Conservar sincronización y cierre de sesión; adaptar la importación al grupo 1. La referencia orienta la composición, sin añadir funciones inexistentes por copiar sus filas.
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

- [ ] Crear el dashboard Android con Material 3: total de solicitudes, distribución por estado y evolución temporal, con acceso al detalle y cambio de estado. Calcular los indicadores desde respuestas almacenadas mediante repositorios y ViewModels.
- [ ] Mostrar un pequeño punto rojo en el acceso al dashboard cuando lleguen respuestas nuevas. Acompañarlo de una descripción accesible para no depender solo del color.
- [ ] Definir cuándo una respuesta se considera revisada y cuándo desaparece el indicador. Conservar el estado por cuenta y formulario entre reinicios, sin mezclar propietarios ni marcar novedades solo por resincronizar datos existentes.

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

## Otros pendientes y seguimiento existente

- [ ] Recuperar las portadas publicadas al iniciar sesión sin archivos locales, con actualización reactiva en tarjetas, editor y Preview. Implementación en `fix/restore-cloudinary-covers`; consultar la verificación y evidencia pendiente al final de este documento.
- [x] Completar la pantalla de login con la identidad visual de FormLink, registro, recuperación por correo, validaciones y estados de carga y error. Ver la verificación de autenticación al final. Los ajustes visuales adicionales se siguen en los grupos 2 y 3.
- [ ] Implementar login con Google mediante Firebase a través de `AuthRepository`, conservando la sesión cifrada y la separación entre cuentas.
- [ ] Evaluar una URL más amigable que `formlink-9f735.web.app`: comparar un sitio Hosting con otro identificador y un dominio propio, incluyendo costos y cómo conservar los enlaces existentes. Coordinar con el grupo 9.

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

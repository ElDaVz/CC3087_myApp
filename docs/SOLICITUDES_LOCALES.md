# Solicitudes guardadas en el dispositivo

## Recorrido para probar

1. Ejecutar la configuración normal **app** de Android Studio.
2. Crear un formulario con título y una pregunta obligatoria. Esperar a que indique que está guardado y volver a **Mis formularios**.
3. Pulsar **Responder**, llenar los campos y seleccionar **Guardar solicitud en este dispositivo**. La pantalla avisa que no se envía a otra persona.
4. Abrir **Ver solicitud guardada**. Revisar título, fecha, identificador, preguntas y respuestas.
5. Cambiar **Pendiente** a **En proceso** o **Completada**. Un fallo al guardar mantiene el estado anterior y permite reintentar.
6. Volver a **Respuestas**: total y conteos por estado se actualizan automáticamente. Volver a **Formularios** para comprobar el número de respuestas del formulario.
7. Cerrar y volver a abrir la app: las solicitudes siguen en **Respuestas**. Editar el formulario original no cambia lo que se respondió antes.

La vista previa del editor y su botón de prueba siguen sin crear solicitudes. Una nueva entrada por **Responder** inicia una solicitud distinta; reintentar el mismo envío usa el mismo identificador y no duplica registros.

## Decisiones de almacenamiento

- `FormLinkDatabase` pasa de v1 a v2 mediante `MIGRATION_1_2`. Se agrega `local_requests` sin borrar ni recrear las tablas de formularios.
- Cada solicitud conserva el título y las etiquetas/valores de las preguntas como una instantánea JSON, además de IDs, fecha y estado. Las opciones conservan sus etiquetas y las casillas conservan su valor booleano.
- `RoomRequestRepository` valida el formulario y las respuestas dentro de una transacción. Si el formulario cambió desde que se abrió, se pide volver a abrirlo; no se mezclan versiones.
- Las solicitudes permanecen aunque se elimine el borrador original. No se implementa eliminación de solicitudes en esta entrega.
- `RequestListViewModel` observa Room; `RoomFormRepository` combina formularios con conteos. Las pantallas no acceden directamente a la base.
- Los tres estados pueden seleccionarse libremente, incluyendo reabrir una solicitud completada.

## Límites

Los registros pertenecen a esta instalación de la app. No hay backend, autenticación, sincronización, formulario web ni enlaces públicos funcionales. Los tipos de campo disponibles son los del editor actual; no se agrega captura ni descarga de archivos. El detalle conserva soporte de lectura para nombres de adjuntos en ejemplos de debug.

## Pruebas

- `LocalRequestFlowTest`: responder desde la app, guardar, abrir detalle, cambiar estado y comprobar conteos.
- `LocalRequestRepositoryTest`: persistencia al reabrir la base en disco, instantáneas tras editar/eliminar el borrador, validación, IDs compuestos, reintentos sin duplicados y migración v1 → v2 conservando formularios y preguntas.
- `SubmitRequestViewModelTest`: doble toque, fallo/reintento y restauración de un envío ya guardado.
- `RequestDetailViewModelTest`, `RequestListViewModelTest`, `RequestNavigationTest`, `RequestDetailScreenTest`: estados de carga/error, selección, restauración y navegación.
- `FormDraftFlowTest`: regresión del editor y guardado de borradores.

Compilación y pruebas unitarias: `gradlew.bat compileDebugKotlin testDebugUnitTest assembleDebug assembleDebugAndroidTest compileReleaseKotlin` con JDK 21. Las pruebas instrumentadas requieren un emulador o dispositivo.

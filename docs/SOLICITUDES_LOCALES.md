# Solicitudes guardadas en el dispositivo

## Disponibilidad y recorrido

Room solo tiene borradores; no hay estado ni flujo de publicación. **Responder** está deshabilitado en las tarjetas y la navegación normal no conecta con `SubmitRequestRoute`. Habilitarlo requiere primero implementar publicación y comprobar que el formulario está publicado. El envío público queda fuera de esta integración.

La app normal permite abrir **Respuestas**, consultar solicitudes ya almacenadas y cambiar entre **Pendiente**, **En proceso** y **Completada**. Una instalación sin solicitudes muestra la bandeja vacía, sin inyectar datos de ejemplo. Los conteos se calculan desde Room.

Para comprobar captura y persistencia se usa exclusivamente el fixture de `LocalRequestFlowTest`: entra directamente en `SubmitRequestRoute` con una base en memoria, valida una respuesta obligatoria, guarda con un ID estable y abre la app normal para comprobar detalle, estados y conteos. No habilita botones ni rutas de envío en la app normal. Los tests de repositorio también usan datos aislados.

La configuración debug **DetalleSolicitudDemo** abre ejemplos de lectura identificados como demo, fuera del recorrido normal. La vista previa del editor y **Probar envío** solo validan; nunca crean solicitudes.

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

- `LocalRequestFlowTest`: confirmar que **Responder** está deshabilitado para un borrador y que Preview no crea registros; capturar desde un fixture aislado, validar, guardar, abrir detalle, cambiar estado y comprobar conteos.
- `LocalRequestRepositoryTest`: persistencia al reabrir la base en disco, instantáneas tras editar/eliminar el borrador, validación, IDs compuestos, reintentos sin duplicados y migración v1 → v2 conservando formularios y preguntas.
- `SubmitRequestViewModelTest`: API de validación centralizada, doble toque, fallo/reintento y restauración de un envío ya guardado.
- `RequestDetailViewModelTest`, `RequestListViewModelTest`, `RequestNavigationTest`, `RequestDetailScreenTest`: estados de carga/error, selección, restauración y navegación.
- `FormDraftFlowTest`: regresión del editor y guardado de borradores.

Compilación y pruebas unitarias: `./gradlew compileDebugKotlin testDebugUnitTest compileDebugAndroidTestKotlin assembleDebug` con JDK 21. Ejecutar las instrumentadas con `./gradlew connectedDebugAndroidTest` en emulador o dispositivo; compilar su código no sustituye su ejecución. En Windows usar `gradlew.bat`.


## Verificación de integración — 3 de noviembre de 2026

Se integró primero [correcciones básicas, PR #13](https://github.com/ElDaVz/CC3087_myApp/pull/13) y luego se incorporó ese resultado a Ventura para resolver Preview conservando su API de validación centralizada. [Ventura, PR #14](https://github.com/ElDaVz/CC3087_myApp/pull/14) conserva solicitudes, migración, estados y conteos, con el envío desconectado de la navegación normal.

En ambas integraciones finalizaron correctamente `compileDebugKotlin`, `testDebugUnitTest`, `compileDebugAndroidTestKotlin` y `assembleDebug`.

- Correcciones: 25 pruebas instrumentadas aprobadas en Pixel_9 (Android 17 / API 37).
- Ventura integrada: 119 pruebas unitarias y 38 instrumentadas aprobadas, sin omisiones, en el mismo emulador.
- Se ejecutó la migración desde una base v1 construida con el esquema exportado: el borrador y su pregunta sobreviven en v2 y se puede escribir en la nueva tabla de solicitudes.
- Se ejecutaron las pruebas de borradores sin envío, Preview sin registros nuevos, captura aislada, reintentos, persistencia, instantáneas, estados y conteos.

Comando instrumentado ejecutado: `./gradlew connectedDebugAndroidTest`. Los informes locales se generan en `app/build/reports/tests/testDebugUnitTest/` y `app/build/reports/androidTests/connected/debug/`.

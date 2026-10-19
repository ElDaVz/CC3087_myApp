# Persistencia local de borradores

Antes de este cambio, el editor usaba `SavedStateHandle` para recuperar su estado cuando Android recreaba la pantalla. Eso no equivale a guardar un formulario de forma permanente.

## Qué quedó preparado

Agregamos Room para guardar borradores en el teléfono. Cada formulario conserva su identificador, título, fechas de creación y actualización, y sus campos. También se guardan las opciones con sus identificadores, el orden, las ayudas y los ajustes de fecha y número.

`FormRepository` define las operaciones de lectura, guardado y eliminación. `RoomFormRepository` las resuelve mediante un DAO. La escritura del formulario y sus campos ocurre en una transacción: si falla una parte, no queda un borrador guardado a medias.

Los borradores pueden estar incompletos. La validación del contenido sigue siendo responsabilidad del editor, no un requisito para evitar perder lo que el usuario está escribiendo. Estos datos son locales; todavía no hay sincronización con Firebase ni publicación de enlaces.

## Conexión con la app

La primera parte preparó la base de datos. En esta segunda parte, `AppContainer` ya entrega `RoomFormRepository` a los ViewModels. Forms muestra los borradores reales del teléfono; los ejemplos anteriores se conservan para las previews de desarrollo.

Al entrar desde una plantilla o un formulario en blanco se crea un identificador nuevo. Al tocar una tarjeta o su botón Editar se carga el identificador existente, sin insertar otro formulario. Volver desde un borrador reabierto lleva directamente a Forms; si venimos de una plantilla, regresamos a la selección de plantillas.

Cada cambio del editor se manda al repositorio. Una cola procesa las escrituras y lecturas en orden, por lo que reabrir un borrador espera sus cambios pendientes. El resultado de una escritura anterior tampoco puede marcar como guardada una edición más reciente.

El aviso de guardado queda fijo debajo de la barra superior. Distingue entre Guardando, Guardado en este dispositivo y un error con Reintentar. Mientras se carga un borrador no se permite editar una pantalla vacía por accidente. Si el borrador no existe o falla la lectura, se muestra el problema y se puede reintentar.

`SavedStateHandle` se mantiene como respaldo de la edición; la navegación usa `rememberSaveable`. Room es el guardado permanente. Antes de cerrar la app después de editar, hay que esperar el aviso Guardado en este dispositivo: una escritura todavía pendiente no equivale a datos confirmados en disco.

La base de producción se llamará `formlink-drafts.db`. No agregamos migraciones destructivas. El esquema de la versión 1 se conserva en `app/schemas` para preparar migraciones cuando cambie la estructura.

## Cómo se comprueba

Conservamos JUnit 4, las pruebas de corrutinas y las pruebas Compose en emulador que ya tenía el proyecto. La inyección sigue siendo manual; no se añadió Hilt ni otro framework.

Las pruebas unitarias cubren la conversión de modelos, el orden de los campos y el repositorio de ejemplo. Las pruebas de Room usan SQLite real en el emulador para revisar las configuraciones de los nueve tipos de campo, las actualizaciones, la eliminación en cascada y la reversión de una escritura que falla.

También hay una prueba que guarda en una base temporal en disco, la cierra y la abre nuevamente. Solo elimina su propia base de prueba; no toca los datos de la aplicación.

En la primera parte pasaron 70 pruebas unitarias y 13 del emulador. La conexión agrega pruebas sobre el ViewModel, el manejo de errores y el recorrido real Forms–editor–Forms con Room. No fue necesario cambiar las dependencias ni `MainActivity`.

La verificación final de esta segunda parte terminó con 82 pruebas unitarias y 15 pruebas del emulador aprobadas, sin fallos ni pruebas omitidas. El APK también compiló correctamente.

También comprobamos el arranque en frío de la app instalada: creamos el borrador Prueba local persistencia, cambiamos un campo a Cliente local, lo dejamos como opcional y lo movimos al segundo lugar. Después del aviso de guardado, cerramos el proceso y abrimos la app otra vez. El borrador apareció en Forms y conservó esos cambios al reabrirlo y entrar a Preview.

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

Las pruebas conectadas deben ejecutarse en un emulador de pruebas, sin borradores importantes: la instalación y limpieza de APKs puede retirar la app al terminar. En esta ejecución fue necesario reinstalar el APK después de la suite. Desinstalar la app o borrar sus datos elimina los borradores locales; todavía no hay respaldo en la nube.

Configuración basada en la [documentación de Room](https://developer.android.com/training/data-storage/room) y sus [dependencias oficiales](https://developer.android.com/jetpack/androidx/releases/room).

## Qué sigue pendiente

Este avance cubre borradores locales, no autenticación, sincronización con Firebase, publicación, enlaces públicos ni respuestas enviadas. En el siguiente bloque, Share ya abre la confirmación local de [Save & Share](SAVE_AND_SHARE.md); View conserva su aviso provisional. El repositorio ya permite eliminar borradores, pero la acción de eliminación desde la lista no se agregó en este paso.

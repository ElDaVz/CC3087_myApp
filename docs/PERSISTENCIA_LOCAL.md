# Persistencia local — primera parte

Antes de este cambio, el editor usaba `SavedStateHandle` para recuperar su estado cuando Android recreaba la pantalla. Eso no equivale a guardar un formulario de forma permanente.

## Qué quedó preparado

Agregamos Room para guardar borradores en el teléfono. Cada formulario conserva su identificador, título, fechas de creación y actualización, y sus campos. También se guardan las opciones con sus identificadores, el orden, las ayudas y los ajustes de fecha y número.

`FormRepository` define las operaciones de lectura, guardado y eliminación. `RoomFormRepository` las resuelve mediante un DAO. La escritura del formulario y sus campos ocurre en una transacción: si falla una parte, no queda un borrador guardado a medias.

Los borradores pueden estar incompletos. La validación del contenido sigue siendo responsabilidad del editor, no un requisito para evitar perder lo que el usuario está escribiendo. Estos datos son locales; todavía no hay sincronización con Firebase ni publicación de enlaces.

## Qué falta conectar

Este commit prepara la capa de datos. `AppContainer` todavía utiliza el repositorio de ejemplo, por lo que el editor **aún no guarda automáticamente en Room**. El siguiente paso es conectar esa implementación, mostrar el resultado del guardado y permitir reabrir los borradores desde Forms.

La base de producción se llamará `formlink-drafts.db`. No agregamos migraciones destructivas. El esquema de la versión 1 se conserva en `app/schemas` para preparar migraciones cuando cambie la estructura.

## Cómo se comprueba

Conservamos JUnit 4, las pruebas de corrutinas y las pruebas Compose en emulador que ya tenía el proyecto. La inyección sigue siendo manual; no se añadió Hilt ni otro framework.

Las pruebas unitarias cubren la conversión de modelos, el orden de los campos y el repositorio de ejemplo. Las pruebas de Room usan SQLite real en el emulador para revisar las configuraciones de los nueve tipos de campo, las actualizaciones, la eliminación en cascada y la reversión de una escritura que falla.

También hay una prueba que guarda en una base temporal en disco, la cierra y la abre nuevamente. Solo elimina su propia base de prueba; no toca los datos de la aplicación.

En esta revisión, el APK compiló y pasaron las 70 pruebas unitarias y las 13 pruebas del emulador, sin fallos ni pruebas omitidas. De estas últimas, ocho comprueban la nueva capa de Room y cinco son las pruebas de interfaz que ya teníamos.

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
```

Configuración basada en la [documentación de Room](https://developer.android.com/training/data-storage/room) y sus [dependencias oficiales](https://developer.android.com/jetpack/androidx/releases/room).

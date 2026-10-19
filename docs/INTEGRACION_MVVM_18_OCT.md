# Integración del editor con MVVM — 18 de octubre de 2026

## Qué pasó

El PR #8 entró a main mientras el PR #11 de la refactorización seguía abierto. Los dos trabajos cambiaban el editor, su modelo y la navegación, así que Git no pudo combinarlos solo. La integración se está haciendo en Features/tony-mvvm; no es un PR nuevo.

## Cómo quedaron los cambios

Conservé el catálogo de campos, la búsqueda, el botón flotante, el desplazamiento y enfoque del campo nuevo, las opciones configurables y la vista previa que llegaron desde main. El título y los campos editables siguen en EditFormViewModel. La pantalla envía acciones por ID y no mantiene otra copia del formulario.

UpdateField permite modificar el campo completo, incluyendo opciones, ayudas, límites numéricos y ajustes de fecha. El catálogo crea preguntas vacías, igual que la versión integrada, para que el usuario escriba su contenido. Validar sigue disponible al final de la lista y ahora también revisa las configuraciones de los campos.

Los estados del catálogo, el enfoque, el desplazamiento y la apertura de la vista previa son de presentación. Las respuestas de prueba de la vista previa siguen siendo temporales y separadas del formulario; no se envían ni se guardan en un servidor.

## Estado restaurado y traducciones

El formato anterior guardaba cuatro valores por campo y no alcanzaba para las nuevas opciones. Ahora SavedStateHandle conserva la lista completa de modelos serializables con la clave editor_fields_v2. Todavía puede leer el formato anterior; al editarlo pasa al nuevo. Esto sirve para recuperar una tarea de Android, no para guardar formularios permanentemente.

La raíz de la UI resuelve los recursos de los datos de ejemplo y actualiza los repositorios en memoria. El ViewModel no recibe Context ni Resources. Al cambiar el idioma se actualiza la lista de ejemplos y las plantillas de futuros borradores, sin traducir ni reemplazar lo que ya se está editando.

Las etiquetas de los nueve tipos de campo se resuelven en la UI. Mantuvimos Lifecycle 2.9.4 y las dependencias de MVVM, junto con Espresso 3.7.0 que llegó desde main. No actualicé AGP, el wrapper de Gradle ni MainActivity.

## Verificación

La estrategia existente sigue siendo JUnit 4, repositorios falsos e inyección por constructor. Las pruebas de interfaz usan Compose Test y Espresso. No agregué Hilt, mocks, otra base de datos ni frameworks de capturas.

- compilación del código y generación de los APK de aplicación y pruebas correctas
- 65 pruebas unitarias aprobadas, sin fallos, errores ni omisiones
- restauración del borrador probada también mediante serialización Java real, incluyendo IDs de opciones y configuraciones
- compatibilidad con los registros anteriores de cuatro valores comprobada
- XML en español e inglés con 180 entradas cada uno, sin nombres duplicados
- 5 pruebas instrumentadas aprobadas en Pixel 9 Pro con Android 17, sin fallos ni omisiones

Las pruebas de interfaz comprueban la validación de respuestas y el regreso al editor sin modificar el formulario, el orden en la vista previa, la restauración y reinicio de respuestas, y la restauración de opciones personalizadas y selección múltiple. El quinto caso es la prueba instrumentada de ejemplo del proyecto. La suite fija el español dentro de su composición para que sus selectores no dependan del idioma del emulador.

Las comprobaciones heredadas se actualizaron para reflejar que las opciones vacías son inválidas y que un desplegable puede permitir varias respuestas si se configura así. La prueba del separador decimal ahora indica el idioma explícitamente y comprueba coma en español y punto en inglés.

Los primeros intentos de las pruebas de interfaz no encontraron la actividad porque el emulador estaba dormido y bloqueado. Se despertó sin modificar el código de producción por ese motivo y se repitió la suite.

## Recorrido adicional en la app real

Se usó el mismo Pixel 9 Pro, identificado como emulator-5554. Android CLI permitió consultar el layout antes de los toques y revisar las capturas. Este recorrido es manual guiado, separado de las cinco pruebas instrumentadas.

### Acción: abrir una plantilla desde Formularios ✅

- comandos: seleccionar Draft con input tap 558 660, New Form con input tap 1038 480 y Order Form con input tap 945 1314
- resultado: se abrió el editor con el título y los tres campos de la plantilla, incluyendo las opciones Order y Booking

### Acción: buscar e insertar un campo numérico ✅

- comandos: input tap 1037 2652 para abrir el catálogo; enfocar su búsqueda con input tap 640 1500; tras comprobar el foco, input text Number; seleccionar el resultado con input tap 347 1551
- resultado: la búsqueda mostró Number, el catálogo se cerró y la lista se desplazó hasta Field 4 con la pregunta enfocada automáticamente
- después de comprobar el foco: input text Quantity, seguido de input tap 1155 252 para abrir Preview
- resultado: aparecieron los cuatro campos, con Quantity al final y las opciones originales de la plantilla intactas

### Acción: recuperar el borrador en otro proceso ✅

- preparación: regresar al editor con input tap 84 252, activar Required con input tap 640 1503 y Decimal con input tap 479 1791
- comandos: input keyevent KEYCODE_HOME, esperar dos segundos y am kill com.uvg.cc3087.myapp; después abrir la tarea con la acción MAIN, categoría LAUNCHER y flags 0x10200000
- resultado: el proceso anterior, 10428, dejó de existir. El proceso nuevo, 10811, recuperó Field 4, Quantity, Required activado y Decimal seleccionado, conservando el desplazamiento
- no se utilizó force-stop, no se borraron datos ni se quitó la tarea de Recientes

### Acción: comprobar los idiomas y regresar a Formularios ✅

- comando: cmd locale set-app-locales com.uvg.cc3087.myapp --user 0 --locales es
- resultado: las etiquetas del editor pasaron a español y Quantity siguió igual como contenido del formulario
- comandos: regresar a Plantillas con input tap 84 252 y, tras comprobar esa pantalla, volver a Formularios con otro toque en la flecha
- resultado: Borradores siguió seleccionado y los tres ejemplos mostraron sus títulos traducidos
- cierre: se restauraron los locales originales [] y el tiempo de apagado original 2147483647; no se cerró el emulador que ya estaba abierto

Las dos capturas se guardaron fuera de Git, en Documentos/Plataformas Moviles/evidencias-integracion-mvvm-18oct: 01-preview-integrada.png y 02-editor-restaurado.png. Ambas se revisaron visualmente. Las coordenadas de este registro corresponden solo a esta AVD.

Comandos de comprobación:

```powershell
.\gradlew.bat compileDebugKotlin assembleDebug assembleDebugAndroidTest --console=plain
.\gradlew.bat testDebugUnitTest connectedDebugAndroidTest --console=plain
```

## Qué sigue siendo manual o está fuera del alcance

Antony hace git add, el commit de integración y el push a la misma rama. Hasta marcar los archivos con git add, Git puede seguir mostrando UU aunque los marcadores ya no estén en los archivos. El PR no se fusiona automáticamente al subir estos cambios.

La revisión final de git diff HEAD --check no encontró problemas de espacios. La búsqueda de marcadores de conflicto en app, gradle y docs no encontró coincidencias. MainActivity y gradle-wrapper.properties no tienen cambios respecto al HEAD anterior de esta rama.

No se implementaron autenticación, Room, Firebase, guardado permanente ni publicación. Tampoco se hizo una prueba en un teléfono físico, una auditoría de accesibilidad completa o una matriz de tamaños y escalas de fuente. Los resultados históricos del 8 y 9 de octubre siguen en REFACTOR_MVVM.md y PRUEBAS_MVVM.md.

Referencia consultada: [SavedStateHandle y tipos admitidos](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate).

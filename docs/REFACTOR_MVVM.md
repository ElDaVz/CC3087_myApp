# Refactor MVVM - avance del 8 de octubre

## Qué cambié en Formularios

Separé el estado y los filtros de la pantalla. `FormsViewModel` recibe los formularios desde `FormRepository`, calcula la lista visible y expone un `FormsUiState` mediante `StateFlow`. La pantalla recibe ese estado y envía la selección del usuario al ViewModel.

El filtro se guarda como un nombre estable en `SavedStateHandle`, así que no depende del idioma. El ViewModel se obtiene en la raíz de la app para conservar la misma instancia al entrar a Plantillas y regresar. La navegación y los mensajes visuales siguen en la UI.

`AppContainer` conecta el repositorio de ejemplo con el ViewModel mediante su constructor y una factory. Las previews reciben datos directamente y no necesitan crear ViewModels.

## Archivos principales

- `data/repository/FormRepository.kt`: contrato de lectura de formularios
- `data/repository/SampleFormRepository.kt`: fuente actual con los diez ejemplos
- `di/AppContainer.kt`: elección de la implementación del repositorio
- `ui/state/FormFilter.kt`: opciones de filtro, sin recursos de Android
- `ui/state/FormsUiState.kt`: lista visible y filtro seleccionado
- `ui/viewmodel/FormsViewModel.kt`: filtros y recuperación de la selección
- `ui/screens/FormsScreen.kt`: interfaz que dibuja el estado recibido
- `ui/navigation/FormLinkApp.kt`: conexión del ViewModel con la UI y navegación existente

## Pruebas de este bloque

El proyecto ya tenía JUnit 4 para pruebas locales, Compose Test y Espresso en `androidTest`. Las pruebas existentes eran las de ejemplo. En este bloque mantuve JUnit y agregué `kotlinx-coroutines-test` para controlar las coroutines. La inyección es por constructor y las pruebas usan un repositorio falso; no necesitan Firebase, una base de datos, mocks ni un teléfono.

Las pruebas nuevas comprueban los tres filtros, el orden de los elementos, los cambios emitidos por el repositorio, una lista sin coincidencias y la selección restaurada en un ViewModel nuevo. También revisan que los datos de ejemplo conserven IDs únicos y el mismo orden en distintas suscripciones.

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
```

El 8 de octubre ejecuté esos comandos: la compilación y el APK terminaron correctamente. Pasaron las diez pruebas locales: ocho del ViewModel, una del repositorio y la prueba de ejemplo que ya tenía el proyecto.

La prueba con `SavedStateHandle` simula valores restaurados. No sustituye una prueba de navegación, rotación o cierre del proceso en Android; esas comprobaciones se deben registrar aparte cuando se ejecuten.

## Segundo bloque: Plantillas

Saqué la lista de tarjetas de `ChooseTemplate.kt`. Ahora `TemplateSampleData` contiene los cuatro ejemplos y `SampleTemplateRepository` los entrega a través de `TemplateRepository`. `ChooseTemplateViewModel` convierte esa lista en un `ChooseTemplateUiState`, que la pantalla recibe desde la navegación.

`FormTemplate` tiene un ID estable y un tipo. Los títulos y subtítulos se resuelven en `ui/resources/TemplateResources.kt`, usando los mismos recursos en español e inglés. Así, ni el modelo ni el ViewModel dependen de `R`, `Context` o del idioma del dispositivo. El grid usa el ID de cada plantilla como key.

Conservé el diseño y las acciones existentes: formulario en blanco o una plantilla abren el editor, y la flecha regresa a Formularios. No agregué una selección persistente porque el toque ya lleva directamente al editor. Tampoco cambié el contenido de los campos de ejemplo: las cuatro plantillas siguen usando los mismos campos que usaban antes.

Las previews reciben datos de ejemplo sin crear ViewModels. `AppContainer` elige el repositorio de plantillas, con la misma inyección por constructor del bloque anterior. No agregué dependencias ni modifiqué `MainActivity`.

Agregué cuatro pruebas de `ChooseTemplateViewModel` para comprobar el orden, las actualizaciones, una lista vacía y una instancia nueva que vuelve a leer el repositorio. La prueba de `SampleTemplateRepository` revisa los cuatro tipos, sus IDs únicos y las suscripciones repetidas. Estas pruebas son locales; no comprueban toques ni rotación en Android.

Para verificar este segundo bloque ejecuté:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug
```

La compilación y el APK terminaron correctamente el 8 de octubre. Pasaron las quince pruebas locales, sin fallos ni pruebas omitidas: las diez del bloque anterior y las cinco nuevas de Plantillas. La comprobación visual en Android sigue pendiente.

## Tercer bloque: Editor y validaciones

Moví el título y la lista de campos a `EditFormViewModel`. `EditForm.kt` recibe un `EditFormUiState` y envía acciones para cambiar el título, agregar campos, editar sus nombres, marcar obligatorios, reordenar y eliminar. Las tarjetas reciben sus errores desde el estado en lugar de calcularlos dentro del componente.

`FormDraftValidator`, en `domain/validation`, mantiene las reglas anteriores y devuelve un resultado sin recursos de Android. Primero revisa el título del formulario, después que exista al menos un campo y finalmente los nombres de los campos. La pantalla conserva los mismos mensajes y el Snackbar. Validar todavía no guarda ni publica nada.

Los campos se modifican por ID, no por posición. Los nuevos usan un UUID, así que eliminar y agregar no reutiliza el identificador anterior. Al moverlos se conservan sus títulos, tipos y obligatoriedad.

El ViewModel conserva el borrador durante los cambios de configuración. Además, guarda el título y una lista de valores simples en `SavedStateHandle` para restaurar este editor básico: ID, tipo, nombre y obligatoriedad, manteniendo el orden. Esta copia es estado temporal y pequeño, no una base de datos; cuando se integre Room, los borradores deberán vivir allí. `SavedStateHandle` no sustituye el guardado al cerrar la tarea, forzar la detención o reiniciar el teléfono.

Solo abrir un formulario en blanco o una plantilla inicia un borrador nuevo. Una recomposición no llama a ese inicio. Por ahora, regresar a Plantillas y elegir otra tarjeta comienza de nuevo, igual que antes; no agregué una pantalla de confirmación para descartar cambios.

Las pruebas nuevas cubren las ediciones, los límites al mover campos, IDs desconocidos, IDs únicos, el reinicio de un borrador y los datos restaurados en un ViewModel nuevo. Las pruebas del validador comprueban las reglas y su prioridad. La restauración se simula con otro `SavedStateHandle`; no equivale a una prueba de rotación o muerte del proceso en un dispositivo.

Volví a ejecutar `:app:compileDebugKotlin`, `:app:testDebugUnitTest` y `:app:assembleDebug` el 8 de octubre. La compilación y el APK terminaron correctamente. Pasaron las 37 pruebas locales, sin fallos ni omisiones: 15 anteriores, 16 del editor y 6 del validador. No ejecuté pruebas de interfaz en un dispositivo en este bloque.

No agregué dependencias, no cambié `MainActivity` ni implementé Room o Firebase. Los textos del editor siguen como estaban; su localización queda pendiente de una revisión aparte.

## Lo que queda pendiente

Las tres pantallas actuales ya tienen su estado separado, pero falta comprobar visualmente la navegación, los toques y la rotación en Android antes del PR. `FormRepository` solo cubre `observeForms()`, cuya firma coincide con la lectura que prepara el equipo. Guardar, eliminar, publicar y autenticar se integrarán con el contrato de backend y los repositorios del equipo; estos bloques no implementan persistencia permanente.

Conservé las traducciones y las versiones de AGP y Gradle. Coroutines se declara en `1.10.2`, que ya era la versión resuelta por el proyecto.

## Referencias

- [Recomendaciones de arquitectura de Android](https://developer.android.com/topic/architecture/recommendations)
- [ViewModels con dependencias](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories)
- [Pruebas de Kotlin Flow](https://developer.android.com/kotlin/flow/test)
- [Estado guardado en ViewModel y sus límites](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)

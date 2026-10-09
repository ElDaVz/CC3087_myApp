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

## Lo que queda pendiente

La migración completa todavía no está terminada. Faltan el editor y sus validaciones, además de las comprobaciones visuales de navegación y rotación en Android. `FormRepository` solo cubre `observeForms()`, cuya firma coincide con la lectura que prepara el equipo. Guardar, eliminar, publicar y autenticar se integrarán con el contrato de backend y los repositorios del equipo; estos bloques no implementan persistencia permanente.

Conservé las traducciones y las versiones de AGP y Gradle. Coroutines se declara en `1.10.2`, que ya era la versión resuelta por el proyecto.

## Referencias

- [Recomendaciones de arquitectura de Android](https://developer.android.com/topic/architecture/recommendations)
- [ViewModels con dependencias](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories)
- [Pruebas de Kotlin Flow](https://developer.android.com/kotlin/flow/test)

# Refactor MVVM - avances del 8 y 9 de octubre

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

## Revisión en Android del 8 de octubre

Después de guardar los tres bloques, probé el recorrido en el emulador Pixel 9 Pro. Funcionaron el filtro Draft, New Form, Order Form, Blank Form, las ediciones de campos, el cambio de obligatoriedad, el orden, agregar y eliminar, los avisos de Validar y los botones de regreso.

También giré el editor a horizontal y regresé a vertical: se conservaron el título, el campo editado, el orden y las casillas. Al regresar a Forms, Draft siguió seleccionado. No necesité cambiar el código durante esta revisión.

El detalle de las acciones, los resultados, las capturas y los límites está en [PRUEBAS_MVVM.md](PRUEBAS_MVVM.md). La prueba real de cierre del proceso sigue pendiente; no debe confundirse con la rotación ni con la restauración simulada de las pruebas locales.

## Cuarto bloque: inicio del borrador desde el repositorio

En la revisión del 9 de octubre encontré que `FormLinkApp` todavía elegía directamente los campos de `FormEditorSampleData`. El estado del editor ya estaba en su ViewModel, pero esa decisión seguía en la navegación.

Ahora la UI envía la plantilla seleccionada y resuelve su título con los recursos del idioma. `EditFormViewModel` recibe `TemplateRepository` por constructor y pide los campos con `getInitialFields(templateType)`. Si se elige un formulario en blanco, comienza sin campos y no consulta una plantilla. La navegación solo cambia de pantalla y conecta los callbacks.

`SampleTemplateRepository` conserva los mismos tres campos para las cuatro plantillas. No diseñé contenido nuevo para cada una; el cambio es de organización, no de funcionalidad. La lectura es síncrona porque este catálogo está en memoria y no hace consultas a disco ni a la red. Cada lectura devuelve una lista propia y el ViewModel copia los campos al iniciar el borrador.

La selección se identifica por `TemplateType`, no por el título visible. Así, el idioma o un título diferente no determinan qué datos recibe el editor. Crear o restaurar el ViewModel tampoco consulta de nuevo la plantilla: el borrador solo se inicia con la acción del usuario, conservando la restauración desde `SavedStateHandle`.

Mantuve JUnit 4 y la inyección manual que ya tenía esta rama. Con la guía `testing-setup`, agregué `testing/FakeTemplateRepository` y lo compartí entre las pruebas del catálogo y del editor. Las seis pruebas nuevas comprueban la selección por tipo, el cambio entre plantillas con el mismo título, una plantilla sin campos, el inicio del ViewModel sin consultas y los datos y copias del repositorio de ejemplo. También actualicé las pruebas anteriores para usar el contrato nuevo y comprobar que la restauración no consulta el catálogo.

El 9 de octubre ejecuté:

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --console=plain
```

La compilación y el APK terminaron correctamente. Pasaron las 43 pruebas locales, sin fallos ni omisiones: 37 anteriores y 6 nuevas. No ejecuté el emulador en este bloque; las capturas del 8 de octubre corresponden a la versión anterior.

No modifiqué `MainActivity`, dependencias, otras ramas ni otros PR. Este bloque tampoco agrega autenticación, guardado permanente ni conexión con Firebase.

## Quinto bloque: textos de interfaz fuera del modelo

Quité `label` y `defaultTitle` de `FormFieldType`. El enum conserva los mismos tipos `TEXT`, `MULTIPLE_CHOICE` y `DATE`, pero ya no incluye textos de presentación en español. `FormFieldDraft` sigue guardando el título del campo como un dato del borrador.

Las etiquetas, los títulos iniciales, los botones, los mensajes de validación y las descripciones de accesibilidad del editor ahora están en recursos. Agregué `ui/resources/EditorResources.kt` para relacionar cada tipo y resultado de validación con su recurso, igual que ya se hacía con las plantillas.

Al tocar Agregar, `EditForm` resuelve el título inicial con `stringResource` y envía `AddField(type, initialTitle)`. El ViewModel crea el campo con el texto recibido, sin usar `Context`, `R` ni un idioma fijo. Las validaciones siguen en `FormDraftValidator`, y la UI decide qué mensaje muestra el Snackbar.

Conservé los textos que se veían en español y añadí los equivalentes en inglés. No cambié la distribución ni agregué acciones. Los títulos ya escritos y los campos de ejemplo de las plantillas no se traducen al cambiar el idioma: son contenido del formulario, no etiquetas de la interfaz. Los nombres de los tipos y el formato de `SavedStateHandle` tampoco cambiaron.

Siguiendo `testing-setup`, mantuve JUnit 4 y agregué seis pruebas locales: cuatro comprueban las asociaciones de recursos y dos revisan que el título recibido se conserve al restaurar y que un título vacío siga mostrando el error de validación. Las pruebas anteriores ahora envían el título inicial de manera explícita.

El 9 de octubre ejecuté otra vez `:app:compileDebugKotlin`, `:app:testDebugUnitTest` y `:app:assembleDebug`. Compilaron el código y los recursos, se generó el APK y pasaron las 49 pruebas locales, sin fallos ni omisiones. Además, revisé los XML: los 27 textos nuevos tienen las mismas claves en español e inglés, sin duplicados.

Las pruebas de recursos comprueban IDs, no el texto dibujado por Android. No ejecuté el emulador en este bloque; todavía falta comprobar visualmente la versión final. No modifiqué dependencias, `MainActivity`, otras ramas ni otros PR.

## Lo que queda pendiente

Las tres pantallas actuales ya tienen su estado separado, los campos iniciales del editor pasan por el repositorio y los textos de presentación están fuera del modelo. Para cerrar esta refactorización falta repetir la revisión del recorrido con la versión final y dejar los resultados actualizados en el PR. La navegación, los recursos y el Snackbar permanecen en la UI porque son responsabilidades de presentación.

El PR sigue abierto y el alcance es únicamente el código actual de `Features/tony-mvvm`, sin integrar las ramas del equipo. `FormRepository` todavía solo cubre `observeForms()`: guardar, eliminar, publicar y autenticar son funcionalidades de otra etapa, no pendientes de este refactor. Las comprobaciones adicionales del reporte se mantienen como límites documentados.

Conservé las traducciones y las versiones de AGP y Gradle. Coroutines se declara en `1.10.2`, que ya era la versión resuelta por el proyecto.

## Referencias

- [Recomendaciones de arquitectura de Android](https://developer.android.com/topic/architecture/recommendations)
- [ViewModels con dependencias](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-factories)
- [Pruebas de Kotlin Flow](https://developer.android.com/kotlin/flow/test)
- [Estado guardado en ViewModel y sus límites](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate)
- [Repositorios y límites de la capa de datos](https://developer.android.com/topic/architecture/data-layer)
- [Producción del estado de la UI](https://developer.android.com/topic/architecture/ui-layer/state-production)
- [Recursos de texto de Android](https://developer.android.com/guide/topics/resources/string-resource)

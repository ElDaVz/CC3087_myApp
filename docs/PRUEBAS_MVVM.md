# Journey: revisión del refactor MVVM

## Verificación final del 9 de octubre de 2026

Revisé el commit `179a5ed` de `Features/tony-mvvm`, incluyendo el inicio del borrador desde el repositorio y la separación de los textos del modelo. La compilación fue correcta, las 49 pruebas locales pasaron y el recorrido actual funcionó en el emulador. No cambié código durante esta revisión.

El PR #11 sigue abierto, sobre `main`, con el mismo head de esta rama. No integré otros PR ni hice operaciones de commit, push o merge.

### Entorno y preparación

Usé Pixel 9 Pro, Android 17, API 37, en arranque frío y sin ventana. Android CLI inspeccionó el layout y capturó las imágenes; ADB envió los toques, el texto y los cambios de configuración. El idioma inicial del dispositivo era `en-US` y el de la app seguía al sistema, con una lista de locales vacía.

El primer arranque no llegó a iniciar Android: el emulador informó demasiadas instancias, aunque no había procesos activos. Windows tenía excluidos los puertos TCP 5540–5639. Inicié la misma AVD con el puerto temporal 5640, sin editar su configuración ni las reservas de Windows:

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" '@Pixel_9_Pro' -no-snapshot-load -no-window -port 5640
```

La opción de puerto explícito está documentada en [Android Developers](https://developer.android.com/studio/run/emulator-commandline). Para estas pruebas el dispositivo fue `emulator-5640`. Las coordenadas de abajo se localizaron en este dispositivo con `android layout --flat --no-idle`; no son universales.

### Compilación y pruebas locales

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug --console=plain
.\gradlew.bat :app:testDebugUnitTest --rerun --console=plain
```

El primer comando terminó correctamente con sus tareas actualizadas. Después ejecuté de nuevo la tarea de pruebas con `--rerun`: Gradle registró una tarea ejecutada y 23 actualizadas, con `BUILD SUCCESSFUL`. Los XML registran 49 pruebas, cero fallos, cero errores y cero omisiones:

| Grupo | Pruebas |
| --- | ---: |
| FormsViewModel | 8 |
| ChooseTemplateViewModel | 4 |
| EditFormViewModel | 22 |
| FormDraftValidator | 6 |
| SampleFormRepository | 1 |
| SampleTemplateRepository | 3 |
| EditorResources | 4 |
| Prueba de ejemplo existente | 1 |
| Total | 49 |

El diff respecto a `origin/main` pasó `git diff --check`. También comprobé que `MainActivity.kt` y `gradle-wrapper.properties` no cambian respecto a ese main.

### Resultados del recorrido final

En los comandos abreviados, `input`, `wm`, `am`, `pidof` y `cmd` se ejecutaron con `adb -s emulator-5640 shell`.

#### Acción: abrir la app y comprobar los filtros ✅

- **Comandos**: `android run --device=emulator-5640 --apks=<app-debug.apk> --activity=com.uvg.cc3087.myapp.MainActivity`, `input tap 329 660`, `input tap 558 660`
- **Resultado**: Forms abrió con All seleccionado y miniaturas visibles. Active quedó marcado y las tarjetas visibles tenían ese estado. Draft mostró sus tres formularios de ejemplo
- **Captura**: `00-inicio.png`

#### Acción: abrir New Form y elegir Order Form ✅

- **Comandos**: `input tap 1038 480`, `input tap 945 1314`
- **Resultado**: el catálogo mostró Blank Form y las cuatro plantillas. Order Form abrió el editor con su título y los campos de ejemplo; el tercero se comprobó al desplazar la lista
- **Captura**: `01-editor-english.png`

#### Acción: editar el título y el nombre del primer campo ✅

- **Comandos del título**: `input tap 640 588`, `input keycombination 113 29`, `input text 'Revision%sfinal%sMVVM'`
- **Comandos del campo**: `input tap 640 1704`, `input keycombination 113 29`, `input text 'Cliente%sfinal'`
- **Resultado**: comprobé el foco antes de escribir. Quedaron Revision final MVVM y Cliente final, sin cambiar el segundo campo. Para ocultar el teclado usé su control visible, `input tap 128 2784`
- **Captura**: `02-editor-titulo-editado.png`

#### Acción: cambiar obligatoriedad y reordenar ✅

- **Comandos**: `input tap 168 1932`, `input tap 1112 1932`
- **Resultado**: desmarqué Required de Cliente final y lo moví al segundo lugar. Tipo de solicitud pasó al primero y conservó su casilla marcada; Cliente final quedó desmarcado

#### Acción: agregar y eliminar un campo de texto ✅

- **Comandos**: `input tap 640 900`, `input swipe 640 2200 640 700 700`, `input tap 1112 2331`
- **Resultado**: apareció Field 4 con Text question. Al eliminarlo quedaron los tres campos anteriores, con sus cambios conservados

#### Acción: girar el editor y volver a vertical ✅

- **Comandos**: `input swipe 640 800 640 2300 700`, `wm user-rotation lock 1`, `wm user-rotation lock 0`
- **Resultado**: el título se conservó en horizontal. Al volver a vertical también seguían el orden modificado, Cliente final y los valores de las casillas
- **Capturas**: `03-editor-horizontal.png`, `04-editor-despues-rotacion.png`

#### Acción: validar el borrador completo ✅

- **Comando**: `input tap 1152 252`
- **Resultado**: el Snackbar mostró Form ready to continue :D. Esta acción solo valida; no guarda ni publica

#### Acción: recuperar el borrador después de detener el proceso ✅

- **Comandos**: `pidof com.uvg.cc3087.myapp`, `input keyevent 3`, `am kill com.uvg.cc3087.myapp`, otra consulta con `pidof`, reapertura mediante el Intent del lanzador y consulta del proceso nuevo
- **Resultado**: el proceso 6815 desapareció mientras la app estaba en segundo plano. Al reabrir, el proceso 6910 recuperó el editor, Revision final MVVM, los campos, el orden y las casillas. Más adelante, al regresar a Forms, Draft seguía marcado
- **Reapertura utilizada**:

```powershell
adb -s emulator-5640 shell am start -W -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.uvg.cc3087.myapp/.MainActivity -f 0x10200000
```

Esperé dos segundos después de Home antes de ejecutar `am kill`. No usé `force-stop` ni eliminé la tarea de Recientes.

En un primer intento, `am start -n` sin el Intent del lanzador creó una Activity adicional en la tarea 51 y mostró Forms con All. Lo comprobé en `dumpsys activity activities`: había dos MainActivity. Al cerrar la adicional apareció el editor restaurado. Después repetí la prueba con el comando anterior, que llevó la tarea original al frente sin agregar otra Activity.

#### Acción: cambiar el idioma sin traducir el contenido del borrador ✅

- **Comandos**: `cmd locale set-app-locales com.uvg.cc3087.myapp --user 0 --locales es`, `input tap 640 900`, `input swipe 640 2200 640 700 700`, `cmd locale set-app-locales com.uvg.cc3087.myapp --user 0`
- **Resultado**: las etiquetas cambiaron a Editar formulario, Agregar campo, Nombre del campo y Obligatorio. El título y Cliente final siguieron iguales. El campo agregado en español recibió Pregunta de texto y conservó ese contenido al volver al inglés; las etiquetas volvieron a Field, Text y Required
- **Captura**: `05-editor-espanol-restaurado.png`

#### Acción: volver a Forms con el filtro restaurado ✅

- **Comandos**: `input keyevent 4` desde el editor, `input tap 84 252` desde Plantillas
- **Resultado**: comprobé Plantillas entre ambos pasos. Forms volvió con Draft seleccionado y sus tres formularios, después de las pruebas de rotación, proceso e idioma

#### Acción: iniciar Blank Form y validar sin campos ✅

- **Comandos**: `input tap 1038 480`, `input tap 640 714`, `input tap 1152 252`
- **Resultado**: comenzó Untitled form, sin los campos ni el título del borrador anterior. El Snackbar mostró Add at least one field

#### Acción: agregar opción múltiple y comprobar el error de nombre vacío ✅

- **Comandos**: `input tap 640 1068`, `input tap 640 1704`, `input keycombination 113 29`, `input keyevent 67`, ocultar el teclado y `input tap 1152 252`
- **Resultado**: el campo nuevo comenzó como Choose an option. Después de comprobar el foco y borrar el texto apareció The name cannot be empty debajo del campo. Al validar se mostró Fill in all field names

#### Acción: agregar fecha y comprobar la prioridad del título obligatorio ✅

- **Comandos**: `input tap 640 1236`, `input tap 640 588`, `input keycombination 113 29`, `input keyevent 67`, ocultar el teclado y `input tap 1152 252`
- **Resultado**: Date comenzó con Choose a date. Al dejar vacío el título del formulario, la validación mostró Enter the form title, antes del error de nombre del campo que seguía vacío

#### Acción: comprobar los errores en español ✅

- **Comandos**: `cmd locale set-app-locales com.uvg.cc3087.myapp --user 0 --locales es`, `input tap 1164 252`
- **Resultado**: se vieron el título en rojo, El nombre no puede quedar vacío y el Snackbar Escribe el título del formulario. Choose a date permaneció como contenido del campo creado antes en inglés
- **Captura**: `06-validacion-espanol.png`

#### Acción: restaurar la configuración y regresar a Forms ✅

- **Comandos**: `cmd locale set-app-locales com.uvg.cc3087.myapp --user 0`, `wm user-rotation free`, `cmd locale get-app-locales com.uvg.cc3087.myapp --user 0`, `wm user-rotation`, dos toques en `84 252`, comprobando Plantillas entre ambos
- **Resultado**: confirmé locales `[]` y rotación `free`, como al inicio. Forms quedó con Draft y sus tres tarjetas. Detuve únicamente la instancia de prueba con `android emulator stop emulator-5640`; ADB quedó sin dispositivos conectados
- **Captura**: `07-forms-draft-final.png`

### Evidencias y límites actuales

Las ocho capturas se guardaron fuera del repositorio, en `C:\Users\eferr\Documents\Plataformas Moviles\evidencias-mvvm-9oct-revision-final`. Se revisaron visualmente al capturarlas y se pueden adjuntar manualmente al PR. Los PNG y los logs del emulador no se agregaron a Git.

- Esta fue una revisión guiada con Android CLI y ADB, no una suite automática de UI ni de capturas
- Solo usé esta AVD y su escala de fuente actual. No comprobé otros teléfonos, tablets ni una revisión completa del catálogo en horizontal
- Las etiquetas principales y los errores se comprobaron en español e inglés. No audité todas las descripciones de accesibilidad con un lector de pantalla
- La restauración se verificó con la tarea conservada después de `am kill`. No prueba guardado permanente, reinicio del teléfono, borrar Recientes ni forzar detención
- Un primer `input keyevent 4` con el teclado abierto regresó a Plantillas. Para continuar la edición oculté el teclado con su control visible. Atrás para navegar se comprobó con el teclado cerrado; no comprobé el gesto predictivo ni cambié el manejo de IME. El bloque de BackHandler conserva la lógica que ya había en `origin/main`
- El catálogo muestra las cuatro plantillas; en el recorrido final elegí Order Form y Blank Form. El contrato de campos iniciales para los cuatro tipos está cubierto por pruebas locales
- Room, Firebase, autenticación, guardado y publicación permanecen fuera de este PR

Con ese alcance, las tres pantallas actuales quedaron verificadas para enviar el refactor a revisión. El cierre o merge del PR sigue siendo un paso manual del equipo.

## Registro histórico del 8 de octubre

Este registro corresponde al commit anterior `4d5aa46` y sus 37 pruebas. Sus pendientes describen esa fecha; la verificación del 9 de octubre, arriba, es el resultado actual.

### Entorno y alcance

Probé el refactor el 8 de octubre de 2026, en la rama `Features/tony-mvvm`, hasta el commit `4d5aa46`. Usé el emulador Pixel 9 Pro con Android 17, API 37, iniciado sin ventana y sin cargar un snapshot.

La revisión fue del recorrido que ya existe: Formularios, Plantillas y Editor. No agregué pantallas ni conecté el backend. Usé Android CLI para inspeccionar la interfaz y capturar imágenes, y ADB para los toques, el teclado y la rotación. Estas comprobaciones no son una suite automática de pruebas de interfaz.

### Compilación y pruebas locales

```powershell
.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest :app:assembleDebug
```

Gradle terminó en `BUILD SUCCESSFUL`. En esta revisión las tareas estaban actualizadas y reutilizaron sus resultados. Los reportes XML del código verificado registran 37 pruebas, sin fallos, errores ni omisiones:

| Grupo | Pruebas |
| --- | ---: |
| FormsViewModel | 8 |
| ChooseTemplateViewModel | 4 |
| EditFormViewModel | 16 |
| FormDraftValidator | 6 |
| Repositorios de ejemplo | 2 |
| Prueba de ejemplo existente | 1 |

También revisé el diff completo con `git diff --check origin/main...HEAD`, sin errores de formato. Consulté `main` en GitHub sin hacer fetch ni pull: seguía en `2763151`, y esta rama tenía cuatro commits nuevos y ninguno pendiente de integrar desde ese main.

### Resultados del recorrido

Los comandos de interacción que aparecen abajo usan `adb -s emulator-5554`. Las coordenadas corresponden a este emulador; en otro dispositivo se deben localizar de nuevo con `android layout`.

#### Acción: abrir la app en Forms ✅

- **Comando**: `android run --device=emulator-5554 --apks=<app-debug.apk> --activity=com.uvg.cc3087.myapp.MainActivity`
- **Resultado**: se abrió Forms, con All seleccionado. Las miniaturas cargaron y se vieron las tarjetas y sus acciones
- **Captura**: `01-forms.png`

#### Acción: seleccionar Draft ✅

- **Comando**: `adb -s emulator-5554 shell input tap 558 660`
- **Resultado**: Draft quedó marcado y la lista mostró los tres formularios de ejemplo con ese estado

#### Acción: abrir New Form ✅

- **Comando**: `adb -s emulator-5554 shell input tap 1038 480`
- **Resultado**: se abrió ChooseTemplate con Blank Form y las cuatro plantillas recomendadas
- **Captura**: `02-choose-template.png`

#### Acción: elegir Order Form ✅

- **Comando**: `adb -s emulator-5554 shell input tap 831 1380`
- **Resultado**: se abrió el editor con el título Order Form y los campos de ejemplo. El tercero se comprobó al desplazar la lista

#### Acción: editar el título ✅

- **Comandos**: `input tap 640 588`, `input keycombination 113 29`, `input text 'Prueba%sMVVM'`, `input keyevent 4`
- **Resultado**: después de comprobar que el título tenía el foco, lo cambié a Prueba MVVM y cerré el teclado. El texto actualizado quedó visible

#### Acción: editar el nombre del primer campo ✅

- **Comandos**: `input tap 640 1704`, `input keycombination 113 29`, `input text 'Cliente%sMVVM'`, `input keyevent 4`
- **Resultado**: después de comprobar el foco, cambié Nombre completo por Cliente MVVM. El segundo campo conservó su nombre

#### Acción: quitar Obligatorio del campo editado ✅

- **Comando**: `adb -s emulator-5554 shell input tap 168 1932`
- **Resultado**: la primera casilla quedó desmarcada; la del segundo campo siguió marcada

#### Acción: mover el campo editado hacia abajo ✅

- **Comando**: `adb -s emulator-5554 shell input tap 1112 1932`
- **Resultado**: Tipo de solicitud pasó a Campo 1 y Cliente MVVM a Campo 2. Cada campo conservó su nombre y su casilla

#### Acción: agregar un campo de texto ✅

- **Comandos**: `input tap 640 900`, `input swipe 640 2200 640 700 700`
- **Resultado**: al desplazar la lista apareció Campo 4, de tipo Texto, con el nombre Pregunta de texto

#### Acción: eliminar el campo recién agregado ✅

- **Comando**: `adb -s emulator-5554 shell input tap 1112 2160`
- **Resultado**: desapareció Campo 4 y quedaron los otros tres. Regresé al inicio con `input swipe 640 800 640 2300 700`

#### Acción: girar a horizontal y volver a vertical ✅

- **Comandos**: `shell wm user-rotation lock 1`, `shell wm user-rotation lock 0`
- **Resultado**: en horizontal se conservó Prueba MVVM. Al volver a vertical también seguían Cliente MVVM, el orden modificado y las dos casillas con sus valores anteriores
- **Capturas**: `03-editor-antes-rotacion.png`, `04-editor-horizontal.png`, `05-editor-despues-rotacion.png`

#### Acción: validar el formulario con campos completos ✅

- **Comando**: `adb -s emulator-5554 shell input tap 1164 252`
- **Resultado**: el Snackbar mostró el aviso de que el formulario estaba listo para continuar

#### Acción: usar Atrás de Android desde el editor ✅

- **Comando**: `adb -s emulator-5554 shell input keyevent 4`
- **Resultado**: regresó a ChooseTemplate, sin cerrar la app

#### Acción: iniciar Blank Form ✅

- **Comando**: `adb -s emulator-5554 shell input tap 640 798`
- **Resultado**: el editor mostró Untitled form, sin campos ni los cambios del borrador anterior

#### Acción: validar el formulario sin campos ✅

- **Comando**: `adb -s emulator-5554 shell input tap 1164 252`
- **Resultado**: el Snackbar pidió agregar al menos un campo

#### Acción: regresar con las flechas hasta Forms ✅

- **Comandos**: dos toques en `84 252`, comprobando ChooseTemplate entre ambos
- **Resultado**: regresó a Forms y Draft siguió seleccionado, con sus tres formularios visibles
- **Captura**: `06-forms-draft-al-regresar.png`

Al terminar restauré la rotación a su valor inicial con `shell wm user-rotation free` y confirmé que respondía `free`. No se observaron cierres inesperados durante este recorrido.

### Capturas

Las seis imágenes se guardaron fuera del repositorio, en la carpeta local `evidencias-mvvm-8oct-revision-final`, dentro de Documentos/Plataformas Moviles. Se pueden adjuntar manualmente al PR; este documento no incorpora esos PNG a Git.

### Límites de esta revisión

- No probé un cierre del proceso provocado por Android; la restauración de valores en un ViewModel nuevo está cubierta por pruebas locales
- No ejecuté este bloque en un teléfono físico ni en varios tamaños, idiomas o escalas de fuente
- El guardado permanente, Room, Firebase, autenticación y publicación siguen fuera de este refactor
- Los textos del editor siguen en español y las plantillas conservan sus campos de ejemplo actuales
- No hice una revisión visual completa de Plantillas en horizontal; la rotación de este recorrido fue sobre el editor
- No creé el PR ni hice commits o push; esos pasos los realiza Antony

Con este alcance, no encontré un fallo que requiriera otro cambio de código antes de enviar el refactor a revisión del equipo.

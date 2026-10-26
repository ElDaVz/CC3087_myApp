# CC3087_myApp

Proyecto desarrollado con **Android Studio**, utilizando **Kotlin** y **Jetpack Compose**.

## Descripción

Aplicación móvil dirigida a pequeños emprendedores que reciben pedidos, reservas o solicitudes mediante WhatsApp.

La aplicación permite crear formularios web y compartirlos con los clientes mediante un enlace. Las respuestas se reciben en la aplicación, donde el emprendedor puede consultar la información, gestionar sus solicitudes y actualizar su estado.

## Funcionalidades principales

- Crear y compartir formularios web.
- Recibir solicitudes de clientes.
- Consultar el detalle de cada solicitud.
- Cambiar el estado de una solicitud.
- Gestionar productos o servicios.
- Controlar inventario básico.
- Compartir formularios mediante WhatsApp.

## Plataformas

- **Aplicación Android:** utilizada por el emprendedor.
- **Formulario web:** utilizado por el cliente sin necesidad de instalar una aplicación.

## Requisitos

- Android Studio Meerkat o superior.
- JDK 21.
- Android SDK actualizado.

## Compilar
| Sistema Operativo | Comando |
|-------------------|----------|
| macOS / Linux | `./gradlew assembleDebug` |
| Windows | `gradlew.bat assembleDebug` |

## Estado de esta entrega

La entrega actual cubre el flujo móvil de fase 2: lista de formularios, plantillas, edición, vista previa de prueba y guardado/recuperación local de borradores. Las acciones de guardar y compartir confirman el guardado en este dispositivo; todavía no publican un formulario web ni crean un enlace público. La búsqueda, algunas opciones de menú y destinos de navegación muestran respuestas provisionales.

La asistencia de IA se utilizó para apoyar revisión, documentación, implementación y pruebas. El equipo debe poder explicar cada cambio y verificarlo en el repositorio.

## Arquitectura y datos locales

```text
Compose UI → ViewModel → FormRepository → Room
```

Compose presenta estado y envía acciones. Los ViewModels coordinan filtros, validación y edición; `FormRepository` define el acceso a los formularios y `RoomFormRepository` conserva los borradores en Room. La base `formlink-drafts.db` se abre como instancia única desde `AppContainer`. Room es la fuente de verdad local para títulos, configuración y orden de campos. La lista observa un `Flow`, por lo que refleja altas, ediciones y eliminaciones sin recarga manual. Firebase, autenticación y sincronización remota aún no forman parte de esta entrega.

## Demo reproducible de persistencia

1. Abrir la app y crear un formulario o editar uno existente.
2. Cambiar el título y al menos un campo; esperar a que aparezca «Guardado en este dispositivo».
3. Cerrar la app por completo y volver a abrirla.
4. Comprobar que aparece el mismo título en la lista y que al abrirlo se conservan los campos y su orden.

La prueba instrumentada `FormDraftFlowTest` cubre creación y edición dentro de la app. `RoomFormRepositoryTest` cubre las emisiones del flujo tras insertar, editar y eliminar, y la reapertura de una base en disco. La demostración manual de cerrar y abrir en un dispositivo/emulador queda pendiente cuando haya uno disponible.

## Compromisos y estado

| Compromiso | Estado | Evidencia / desviación |
| --- | --- | --- |
| Forms, edición, preview y borradores locales | Implementado en esta rama | Compose, ViewModels y Room; ver `docs/REVISION_ENTREGA_UI.md` y pruebas indicadas arriba |
| Autenticación y publicación del formulario | Pendiente: 2–6 de noviembre de 2026 | No existe integración activa de Firebase Authentication/Firestore; guardar localmente no publica |
| Respuestas desde formulario web | Pendiente: 9–13 de noviembre de 2026 | No hay formulario web ni captura remota de respuestas |
| Demo manual de persistencia tras cierre completo | Pendiente de dispositivo/emulador | El entorno de desarrollo actual no ofrece una demo presencial registrada |

Las fechas son compromisos de planificación para trabajo pendiente, no resultados completados.

## Autoevaluación: feedback simulado de la fase de UI

Esta autoevaluación usa los puntos verificables resumidos en `docs/REVISION_ENTREGA_UI.md`; el PDF original del plan no está en este repositorio. Es una revisión propia y **no corresponde al feedback de los auxiliares**.

| Punto comprobable | Estado | Evidencia |
| --- | --- | --- |
| Compose y Material 3 | Implementado | Scaffold, componentes Material 3 y tema central |
| Estado de carga, lista vacía, contenido y error | Implementado | `FormsUiState` y `FormsViewModelTest` |
| Persistencia local reactiva | Implementado; demo manual pendiente | Room, `FormRepository`, prueba de Flow y prueba de reapertura en disco |
| Validación centralizada para Preview | Implementado | Métodos del `EditFormViewModel` y prueba unitaria |
| Comparación visual completa con Figma | Parcial | La revisión existente identifica diferencias que aún requieren revisión visual |
| Acciones provisionales diferenciadas de las reales | Documentado | Esta tabla y la sección de funcionalidades |

### Espacio para feedback de auxiliares

Pendiente de incorporar después de recibirlo: observación, cambio acordado, responsable y fecha.

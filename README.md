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

## Estado del avance

Las funcionalidades descritas arriba son el objetivo del producto, no una lista de funciones ya terminadas. La versión actual permite crear, editar y recuperar borradores locales con Room, probarlos en Preview y consultar su confirmación de guardado. El backend, la cuenta del negocio, los enlaces públicos y las solicitudes reales todavía están pendientes en esta rama.

La comparación con la entrega y la planificación está en [Revisión del avance de UI](docs/REVISION_ENTREGA_UI.md). Los recursos de identidad están documentados en [Iconos de FormLink](docs/ICONOS_APP.md).

## Uso de IA

Se utilizó asistencia de IA para analizar requisitos y wireframes, apoyar la refactorización MVVM, implementar y probar partes de la UI y la persistencia local, integrar los iconos y preparar documentación. Antes de integrar los cambios, el equipo debe revisar el código, poder explicarlo y comprobar los flujos con el dispositivo o emulador. Las pruebas no sustituyen esa revisión ni convierten las funciones pendientes en terminadas.

## Requisitos

- Android Studio Meerkat o superior.
- JDK 21.
- Android SDK actualizado.

## Compilar
| Sistema Operativo | Comando |
|-------------------|----------|
| macOS / Linux | `./gradlew assembleDebug` |
| Windows | `gradlew.bat assembleDebug` |

# Entregable del 11 de septiembre

## Qué trabajé

Durante esta revisión trabajé sobre la pantalla de formularios de FormLink. La lista muestra diez formularios de ejemplo mediante una `LazyColumn` y permite cambiar entre los filtros Todos, Activos y Borradores. Cada tarjeta presenta el título, la cantidad de respuestas, la fecha de actualización, el estado y una imagen remota cargada con Coil.

También adapté al español el texto visible de las pantallas Formularios y Nuevo formulario. Esto incluye los filtros, botones, estados, mensajes, fechas, nombres de los formularios y descripciones de accesibilidad.

El botón Nuevo formulario abre la pantalla de plantillas y permite regresar a Formularios. Además, corregí el manejo del estado para que el filtro seleccionado no se pierda al entrar a Plantillas y volver :D

## Cómo está organizado

El código está separado por responsabilidades: los modelos representan la información, los datos de ejemplo viven en su propio archivo, las tarjetas están en componentes reutilizables, las pantallas contienen la interfaz y `FormLinkApp` controla la navegación y el estado compartido entre ellas.

## Comprobaciones realizadas

- compilación exitosa del APK de depuración
- ejecución de la aplicación en el emulador Pixel de Android Studio
- visualización de la interfaz en español
- funcionamiento de los filtros Todos, Activos y Borradores
- desplazamiento de la lista de formularios
- carga de imágenes remotas con Coil
- navegación de Nuevo formulario hacia la pantalla de plantillas
- regreso a Formularios conservando el filtro seleccionado
- acciones provisionales visibles mediante mensajes `Snackbar`

## Pendientes

- agregar una descripción accesible y una acción al botón de más opciones de la pantalla de plantillas
- conectar Formulario en blanco y las plantillas con la futura pantalla de edición
- reemplazar las acciones provisionales Compartir, Editar y Ver cuando se implementen sus respectivos flujos
- ampliar las pruebas automatizadas en las etapas posteriores del proyecto

## Apoyo utilizado

Usé IA como apoyo para revisar los requisitos, detectar detalles pendientes y comprobar el comportamiento de la aplicación. Los cambios se trabajaron y revisaron por partes antes de incorporarlos a la rama.

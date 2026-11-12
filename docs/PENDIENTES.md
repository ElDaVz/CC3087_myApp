# Pendientes de FormLink

Este checklist reúne mejoras para retomar después. No son parte de la integración de portadas.

- [ ] Evaluar una URL más amigable que `formlink-9f735.web.app`: comparar un sitio Hosting con otro identificador y un dominio propio, incluyendo costos y cómo conservar los enlaces existentes.
- [ ] Diseñar e implementar la eliminación definitiva de formularios. Antes de hacerlo, decidir qué ocurre con las solicitudes históricas, los enlaces públicos, las imágenes de Cloudinary y la confirmación al usuario. Actualmente retirar o borrar un publicado conserva el historial.
- [ ] Completar la pantalla de login con la identidad visual de FormLink, registro, validaciones y estados de carga y error.
- [ ] Implementar login con Google mediante Firebase a través de `AuthRepository`, conservando la sesión cifrada y la separación entre cuentas.
- [ ] Crear un dashboard Android de respuestas con Material 3: tarjetas de indicadores, total de solicitudes, distribución por estado y evolución temporal, con acceso a la revisión de respuestas y al cambio de estado. Calcular los indicadores desde las respuestas almacenadas mediante repositorios y ViewModels.

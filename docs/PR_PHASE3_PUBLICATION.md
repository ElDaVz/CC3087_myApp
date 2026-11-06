# Publish real form links and receive validated web requests

Base: `feat/phase3-secure-storage` → rama: `feat/phase3-public-forms`.

## Cambio

Añade publicación explícita desde Guardar/Compartir y un cliente web móvil en Firebase Hosting. El enlace se habilita tras confirmación remota; editar un borrador no modifica la copia publicada hasta actualizarla. Archivar bloquea nuevos envíos conservando solicitudes históricas.

La web soporta diez preguntas y los nueve tipos existentes. Cada envío crea atómicamente un snapshot, dos comprobantes privados de validación y un recibo sin respuestas; el identificador estable evita duplicados al reintentar. Android recibe solicitudes en Room por propietario y sincroniza cambios de estado pendientes, conteos y detalle.

## Validación

Pruebas de publicación–envío público–bandeja, permisos negativos, todos los tipos, máximo de diez preguntas y reintentos. Pruebas de ViewModel para confirmación, error y resultados tardíos. Consultar el informe agregado de verificación.

## Límites

Portadas locales; sin Storage/Functions, archivos, firmas o rating. El despliegue real requiere una sesión Firebase autorizada y una app WEB registrada; no se presenta una URL construida como un despliegue comprobado.

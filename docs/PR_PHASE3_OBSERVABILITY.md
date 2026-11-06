# Measure confirmed form creation and submission with Firebase telemetry

Base: `feat/phase3-public-forms` → rama: `feat/phase3-observability`.

## Cambio

Instrumenta `form_creation_started`/`form_published` en Android y `form_opened`/`form_submitted` en web. Cada flujo conserva su intento aleatorio e inicio, finaliza solo tras confirmación remota y envía duración/cantidad de campos sin información personal. Guardar un borrador o actualizar una publicación no crea una primera publicación nueva.

Incluye ledger cifrado para deduplicación local, preservación de datos de la primera publicación, separación entre desarrollo y usuarios, consultas de promedios e incompletos y Crashlytics con una demo controlada exclusiva de debug.

## Validación y límites

Pruebas de tiempos, parámetros sin texto del dominio, fallo de telemetría y reclamación única de hitos. Analytics no es una transacción con Firestore: el informe deduplica por intento y documenta pérdida posible de eventos. La visibilidad en DebugView/Crashlytics requiere configuración y evidencia real de consola.

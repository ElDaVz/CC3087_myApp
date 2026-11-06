# Métricas y diagnóstico

## Dos medidas

| Flujo | Inicio | Final confirmado | Parámetros |
|---|---|---|---|
| Creación Android | Nuevo formulario → `form_creation_started` | Primera publicación remota con enlace → `form_published` | `form_id`, `creation_attempt_id`, `template_type`; al finalizar `duration_seconds`, `field_count` |
| Solicitud web | Apertura del enlace → `form_opened` al cargar una publicación válida | Backend confirma respuesta o recibo → `form_submitted` | `form_id`, `attempt_id`, `form_type`; al finalizar duración y cantidad de campos |

Cada intento tiene UUID aleatorio. Android registra el inicio antes de seleccionar plantilla (`unselected`) y persiste la plantilla definitiva con el borrador. Room y el estado restaurable cifrado conservan el momento inicial. El final usa el reloj del cliente cuando recibe la confirmación remota, para medir inicio y final en el mismo flujo. La fecha de publicación del servidor se conserva como dato del dominio; las actualizaciones conservan primera fecha, plantilla y número de campos. Solo se mide la publicación en una instalación que tenga registrado el inicio del intento, evitando mezclar relojes de dispositivos diferentes. Importar un borrador no reinicia su intento. Formularios antiguos sin inicio registrado no entran en esta medida.

En la web se toma el inicio al abrir el enlace, incluyendo su carga. La pestaña conserva únicamente ID, fecha y marcas de medición en sessionStorage; no guarda respuestas. Reintentos y recargas reutilizan el mismo ID. Una nueva pestaña corresponde a otro intento. Solo una confirmación del backend finaliza el envío.

El ledger cifrado de Android deduplica hitos por ID lógico y reclama cada evento antes de entregarlo al SDK. Las marcas de pestaña cumplen la misma función en web. Firebase Analytics no es transaccional con Firestore: una caída entre reclamar y enviar puede perder telemetría; varios dispositivos pueden repetir el mismo hito. Por ello los informes deduplican por intento y no se usan los eventos como prueba única de persistencia del dominio. Bloqueadores o falta de soporte también pueden impedir medición sin impedir el envío.

La media suma duraciones válidas y divide entre completados medidos. Guardar borrador no es publicar. Duraciones negativas/no finitas se excluyen. `metrics.sql` deduplica y separa completados, incompletos al corte y mediciones inválidas. Meta de creación: <600 segundos. El brief no define una meta numérica para responder. Revisar segmentos por plantilla/tipo y cantidad de campos; reportar incompletos junto al promedio, sin afirmar abandono permanente.

## Configuración y evidencia real

1. Activar Analytics en el proyecto y registrar Android y WEB. La web necesita su `measurementId`; Hosting expone configuración pública o puede generarse desde `.env.local` durante el build.
2. Debug Android no recolecta por defecto. Para una demo controlada: compilar `-PtelemetryEnabled=true`, sin `-PfirebaseEmulators=true`. Activar DebugView con `adb shell setprop debug.firebase.analytics.app com.uvg.cc3087.myapp`.
3. La web local/emulada no recolecta por defecto. Una demo local real puede usar `VITE_TELEMETRY_ENABLED=true` y `VITE_ANALYTICS_ENV=development`. El dominio público usa `users`; previews usan `development`.
4. En GA4 configurar el filtro de tráfico de desarrolladores y excluir `development` de informes del estudio. No mezclar datos de pruebas con usuarios. Desactivar medición mejorada de interacciones de formularios; solo los cuatro eventos propios son evidencia de estas métricas.
5. Registrar `duration_seconds` como métrica personalizada y plantilla/tipo, ambiente y cantidad de campos como dimensiones según el informe. No registrar UUIDs como dimensiones de alta cardinalidad.
6. Vincular exportación **diaria** a BigQuery Sandbox, sin streaming/facturación, y adaptar `docs/analytics/metrics.sql`. La exportación no es inmediata; DebugView sirve para mostrar el orden durante la demo. Los parámetros de fecha de la consulta incluyen un margen anterior suficiente para intentos que comenzaron otro día.
7. Conservar capturas de inicio/final con el mismo UUID, parámetros numéricos y reporte por segmentos. No afirmar visibilidad en consola hasta comprobarla.

No se envían títulos, etiquetas, nombres, teléfonos, correos, respuestas, contraseñas, tokens ni texto libre. No se establece un userId con la cuenta del propietario. La configuración de medición solo acepta identificadores aleatorios, enums y números.

## Crashlytics

Se configura en Android con el SDK y plugin oficiales, condicionado a configuración Firebase. Recolección desactivada en debug salvo `-PtelemetryEnabled=true`; siempre desactivada para emuladores Firebase. La actividad `.debug.CrashlyticsDemoActivity` existe exclusivamente en debug y muestra un botón identificado como **DEMO QA**. Abrirla con `adb shell am start -n com.uvg.cc3087.myapp/.debug.CrashlyticsDemoActivity`, provocar el fallo y reiniciar la app. Confirmar su aparición en el panel antes de marcar esta evidencia completa.

Priorizar primero fallos que impiden abrir/guardar/enviar, después cantidad de usuarios y sesiones afectadas y frecuencia. Relacionar cada corrección con un issue, versión y regresión automatizada. No agregar usuarios identificables, respuestas o excepciones con cuerpos HTTP a logs, custom keys o errores no fatales.

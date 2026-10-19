# Revisión del avance de UI — 18 de octubre de 2026

Esta revisión contrasta la rama `Features/tony-avance-ui` con `Proyecto_Avance_UI.pdf` y el plan final `planDeProyecto-moviles-1.pdf`. No modifica la planificación original ni confirma el estado actual de GitHub o de las ramas de otros integrantes.

## Qué tenemos realmente

El flujo local Forms → Choose template → Edit Form → Preview → Save & Share está conectado. Forms lista los borradores reales de Room, el editor conserva sus cambios y Preview permite probar respuestas sin enviarlas. Save & Share confirma el guardado local, no la publicación en internet.

Los cinco frames principales encontrados en el Figma compartido son Forms (`20:2`), Choose template (`3:4`), Edit Form (`3:6`), Preview (`3:8`) y Save & Share (`3:10`). Existen sus pantallas en código. Esto no equivale a una comparación visual completa: el editor se amplió respecto al wireframe y el tema actual admite color dinámico. Settings y Responses aparecen como destinos de la barra, pero no tienen un frame de pantalla propio en la página revisada.

## Requisitos del PDF de avance

| Punto | Estado comprobado | Qué falta |
| --- | --- | --- |
| App Compose con Material 3 | Implementado: Scaffold, barras, tarjetas, botones y tema centralizado | Completar la comparación visual con los cinco frames, especialmente editor y Preview |
| Colores y tipografía desde el tema | Las pantallas usan MaterialTheme; la búsqueda no encontró tamaños de texto ni colores de marca fijos en sus Composables | El tema todavía personaliza pocos estilos y activa color dinámico; acordar su fidelidad al diseño |
| Espaciado consistente | Los paddings y separaciones revisados usan múltiplos de 4 dp | Revisar horizontal, texto grande y pantallas pequeñas antes de cerrar calidad |
| Contenido dinámico | LazyColumn y Coil implementados | Probar desplazamiento con suficientes borradores reales y fallos de carga de imagen |
| Interacciones | Crear, editar, reordenar, Preview, guardar y Share local conectados | Search, More, View, Responses y Settings mantienen acciones provisionales; el PDF permite provisionalidad, pero no hay que presentarlas como funciones terminadas |
| Nombres coherentes con Figma | Forms, ChooseTemplate y EditForm son reconocibles | Unificar los nombres públicos FormPreviewScreen y SaveShareScreen con Preview y SaveAndShare, coordinando llamadas y pruebas |
| Demo desde main | No verificado en este bloque; compilamos la rama de trabajo | Revisión y aprobación del equipo, integración autorizada y nueva compilación desde main; no usar force push para saltar la revisión |
| Historial y participación | Los commits y pushes los realiza Antony | Revisar en GitHub la participación real y subir los cambios antes de la clase |
| PR hacia main | Pendiente de comprobar con el equipo | Confirmar que el PR contiene esta rama y publicar su enlace en Canvas según la consigna |
| Plan vs. real y feedback | Esta revisión identifica el estado técnico | Llevar el plan original, acordar nuevas fechas de lo pendiente y registrar el feedback recibido; no inventar fechas ni resultados |
| Transparencia y comprensión | Declaración de asistencia añadida al README | Todos deben poder explicar los archivos y distinguir lo real de lo provisional |

La única coincidencia de color explícito encontrada fuera del tema fue `Color.Transparent` en un contenedor de campo. No representa una paleta de marca fija ni demuestra por sí sola un incumplimiento visual.

## Planificación: lo urgente no es solo visual

El hito 2 estaba comprometido para el 12–16 de octubre. La parte local está implementada, pero el hito completo sigue parcial en esta rama:

| Compromiso | Estado local |
| --- | --- |
| Guardar y recuperar borradores | Room implementado y probado |
| Tres plantillas y Preview | Hay catálogo y Preview; las plantillas comparten campos de ejemplo y requieren contenido específico antes de considerarlas tres flujos completos |
| Registro, inicio y cierre de sesión del negocio | Pendiente: no hay AuthRepository de producción ni Firebase Authentication integrado |
| Publicación y permisos remotos | Pendiente: no hay publicación en Firestore ni reglas desplegadas verificadas |
| Enlace real abierto desde otro dispositivo | Pendiente: guardar un borrador no crea un formulario web público |
| Copiar o compartir el enlace real | Acciones de Android preparadas, deshabilitadas mientras no exista un enlace real |

La semana del 19–23 de octubre compromete captura de respuestas web, validación, guardado, control de duplicados y eventos de apertura/envío. Ese trabajo depende de la publicación y el backend todavía pendientes. La lista y el detalle de solicitudes en Android están previstos para el 26–30 de octubre; el catálogo simple, para el 2–6 de noviembre.

Por eso no corresponde decir que la entrega está completa solo por tener más pantallas. El backend y la cuenta del negocio requieren coordinación con Daniel y David. Antes de comprometer nuevas fechas, el equipo debe confirmar lo que ya exista en sus ramas.

## Siguiente bloque de esta rama

Los iconos adjuntos ya están integrados. El desbloqueo biométrico y su preferencia en Settings siguen pendientes. Será un bloqueo local del acceso a la app: no sustituye la cuenta del negocio, no publica formularios y no cifra Room por sí solo.

Para la integración estable de BiometricPrompt hace falta coordinar un ajuste mínimo de la clase base de MainActivity, de ComponentActivity a FragmentActivity. Antony decidió dejar la huella pendiente por ahora; no se modificó MainActivity ni se añadió una opción que aparentara funcionar sin esa integración.

No hay credenciales de Firebase configuradas ni autorización para inventar un servicio remoto. Esta revisión tampoco asigna al equipo fechas nuevas ni registra feedback que no hayamos recibido.

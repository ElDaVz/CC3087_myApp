# Fase 3: planificación y evidencias

## Secuencia de entrega

| Etapa | Estado al desarrollar | Resultado | Evidencia que falta confirmar externamente |
|---|---|---|---|
| Seguridad local y transporte | Implementado y verificado localmente | Sesión/Room/portadas cifradas; adaptadores HTTPS | Revisión por otro integrante |
| Publicación, web y solicitudes | Implementado y verificado localmente | Copia pública, enlace y bandeja remota | Hosting real y demo de consola |
| Analytics y Crashlytics | Código y pruebas locales completos | Intentos, tiempos y diagnóstico | DebugView, exportación y panel real |
| CI y proceso de calidad | Workflow validado localmente | Compilación, tests, Lint y pruebas de navegador | Ejecución visible y revisión aprobada en GitHub |
| Feedback real de Fase 2 | Pendiente de recibir | Matriz preparada | Observaciones originales del catedrático |

No se sustituyen los compromisos originales con fechas inventadas. Cuando el equipo proporcione su planificación, añadir responsable, fecha acordada y referencia al issue real. El README conserva referencias históricas que deben distinguirse del estado actual.

## Desviaciones justificadas

- Se amplía la fase previa, que solo sincronizaba borradores, porque ambas métricas necesitan publicación y recepción reales.
- Se conserva Spark: cliente estático en Hosting y reglas de Firestore. Sin Functions/Storage; portadas locales.
- La web inicial admite diez preguntas y nueve tipos existentes. La validación se reparte en dos comprobantes privados atómicos para respetar el presupuesto de reglas, sin confiar exclusivamente en validación del navegador.
- La exigencia de tokens cifrados obliga a controlar la persistencia: Firebase Auth y Firestore se consumen por REST detrás de repositorios. No cambia el proveedor ni la arquitectura MVVM. La actualización requiere autenticarse de nuevo.
- El workflow se incorpora desde la rama de seguridad y se hereda en las demás, para validar cada PR antes de integrar. La última etapa completa documentación, checklist y comprobaciones del repositorio.
- Se mantiene inyección manual; Hilt sería una migración separada, descrita en `FIREBASE_BACKEND.md`.

## Definición de terminado

Código revisable en una rama, criterios de aceptación cubiertos, migraciones verificadas, pruebas locales y CI en verde, QA relevante registrado, documentación actualizada y aprobación de otro integrante. El despliegue y los paneles solo están completos cuando existe evidencia real; escribir su configuración no basta.

## Matriz de feedback

| Observación recibida | Cambio acordado | Issue/PR | Responsable | Evidencia | Estado |
|---|---|---|---|---|---|
| No proporcionada todavía | Pendiente | — | — | — | Pendiente |

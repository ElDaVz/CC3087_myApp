# Add PR quality gates and phase 3 delivery evidence

Base: `feat/phase3-observability` → rama: `ci/phase3-quality`.

## Cambio

Añade GitHub Actions por PR para compilación Android, pruebas unitarias, Android Lint, APK, build/test web, reglas, navegador e instrumentadas contra Firebase Emulator Suite. Los jobs conservan reportes y no necesitan credenciales de producción.

Actualiza README, instrucciones de arquitectura, planificación y desviaciones justificadas. Incorpora checklist de QA, definición de terminado y plantillas para PRs/issues. El feedback de Fase 2 sigue pendiente de recibir: no se atribuyen observaciones ni aprobaciones inexistentes.

## Evidencia

Consultar `docs/VERIFICATION_PHASE3.md`. Ejecutar el workflow en el PR y obtener aprobación de otro integrante antes de integrarlo. Los reportes locales no sustituyen una ejecución visible de Actions ni una revisión humana.

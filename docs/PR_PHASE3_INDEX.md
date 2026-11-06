# PRs de la Fase 3

La rama base inicial sigue siendo `feat/firebase-form-sync`: main remoto contiene cover/continue, pero al iniciar este trabajo no contenía la integración Firebase. Integrar esa base antes de completar esta serie. Después de cada merge, actualizar la base del siguiente PR hacia main.

| Orden | Rama | Base inicial | Título y descripción |
|---|---|---|---|
| 1 | `feat/phase3-secure-storage` | `feat/firebase-form-sync` | [Seguridad](PR_PHASE3_SECURITY.md) |
| 2 | `feat/phase3-public-forms` | `feat/phase3-secure-storage` | [Publicación y web](PR_PHASE3_PUBLICATION.md) |
| 3 | `feat/phase3-observability` | `feat/phase3-public-forms` | [Métricas y Crashlytics](PR_PHASE3_OBSERVABILITY.md) |
| 4 | `ci/phase3-quality` | `feat/phase3-observability` | [Calidad y entrega](PR_PHASE3_QUALITY.md) |

## Abrir en GitHub

- [Preparar PR de seguridad](https://github.com/ElDaVz/CC3087_myApp/compare/feat/firebase-form-sync...feat/phase3-secure-storage?expand=1).
- [Preparar PR de publicación](https://github.com/ElDaVz/CC3087_myApp/compare/feat/phase3-secure-storage...feat/phase3-public-forms?expand=1).
- [Preparar PR de métricas](https://github.com/ElDaVz/CC3087_myApp/compare/feat/phase3-public-forms...feat/phase3-observability?expand=1).
- [Preparar PR de calidad](https://github.com/ElDaVz/CC3087_myApp/compare/feat/phase3-observability...ci/phase3-quality?expand=1).

Son enlaces de preparación; no afirman que los PRs ya estén abiertos. Copiar el título y Markdown de cada archivo, adjuntar la evidencia y solicitar revisión de otro integrante. Ver [resultados y pendientes](VERIFICATION_PHASE3.md).

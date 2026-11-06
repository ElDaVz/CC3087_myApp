# Encrypt local data and Firebase sessions behind repository adapters

Base: `feat/firebase-form-sync` → rama: `feat/phase3-secure-storage`.

## Cambio

La sesión de Firebase, los datos de Room, las portadas y el estado sensible restaurable pasan a almacenamiento cifrado respaldado por Android Keystore. Los repositorios usan Firebase Auth/Firestore mediante HTTPS para controlar la persistencia de credenciales. La migración exporta y verifica los registros antes de sustituir SQLite; los errores no borran automáticamente información.

Se restringe HTTP a direcciones de emuladores exclusivamente en debug, se excluyen datos de backups y se conservan Photo Picker y los límites de tamaño de imágenes. La actualización solicita autenticarse nuevamente; los borradores se conservan.

## Validación

Compilación y pruebas unitarias; pruebas instrumentadas de autenticación real emulada, importación de portadas cifradas, integridad/reapertura de base y rechazo de manipulación AES-GCM. Consultar `docs/VERIFICATION_PHASE3.md` en la última etapa para el resultado agregado.

## Dependencia

La integración inicial de Firebase debe integrarse antes. No contiene publicación ni cambios directos a main.

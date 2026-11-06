# Publicación y cliente web (Spark)

El flujo es Continuar → guardado local confirmado → Guardar/Compartir → Publicar. Un invitado puede editar pero necesita autenticarse e importar su borrador para publicar. No se anuncia un enlace hasta confirmar la publicación en Firestore.

`forms/{id}` sigue siendo privado. `publishedForms/{id}` contiene exclusivamente la copia publicada que consume `/f/{id}`. Cambiar el borrador no la modifica. Actualizar publicación conserva la URL y aumenta la versión; una repetición sin cambios es idempotente. Archivar bloquea lecturas públicas y respuestas. Borrar un formulario publicado deja un registro remoto archivado para conservar propiedad e historial de solicitudes.

La primera versión admite 1–10 preguntas de los nueve tipos actuales, títulos y opciones de hasta 200 caracteres y texto de respuesta de hasta 10,000 caracteres. No publica portadas, archivos, firmas o rating. Las fechas se representan como medianoche UTC y minutos desde medianoche; la restricción de fechas pasadas se compara con el día UTC tanto en cliente como en reglas.

## Respuestas

Cada intento usa un UUID estable en la pestaña. Se guarda una respuesta con snapshot inmutable de título/campos/versión y valores, dos documentos privados `responseChecks/{attempt}_0|1` y un recibo sin respuestas en `publishedForms/{id}/receipts/{attempt}` en el mismo batch. Los comprobantes reparten la validación en grupos de cinco para respetar el máximo de expresiones de reglas. Sin ambos comprobantes válidos no puede crearse la respuesta; no pueden leerse públicamente ni actualizarse. El recibo admite únicamente consulta por ID aleatorio, sin listados. Una confirmación perdida se recupera consultando ese recibo, nunca leyendo una respuesta públicamente.

Room almacena las respuestas remotas por propietario, separadas de los fixtures locales del invitado. La bandeja, detalle y conteos observan Room. Las transiciones de estado se encolan con revisiones y se confirman solo si no existe una edición posterior. WorkManager coordina la sincronización y la app solicita actualización cada 15 segundos mientras está en primer plano. No es push instantáneo.

## Desarrollo y despliegue

1. `npm ci --prefix backend-tests` y `npm ci --prefix web`.
2. `backend-tests/node_modules/.bin/firebase emulators:start --project demo-formlink --only auth,firestore`.
3. Crear `web/.env.local` con `VITE_FIREBASE_EMULATORS=true`; `npm run dev --prefix web`. Las pruebas E2E establecen la variable automáticamente.
4. `npm test --prefix web`; con emuladores activos, `npm run test:e2e --prefix web`.
5. Producción: registrar una app WEB en el proyecto Firebase, generar su configuración local si fuera necesaria y compilar `npm run build --prefix web` sin la variable de emuladores.
6. Autenticarse en Firebase CLI y desplegar `firebase deploy --project formlink-9f735 --only firestore:rules,firestore:indexes,hosting`. Hosting entrega su configuración pública desde `/__/firebase/init.json`; la app web debe estar registrada.
7. Confirmar el enlace `https://formlink-9f735.web.app/f/{id}` desde una publicación real. El enlace no se considera verificado solo porque el código lo construye.

No se incluye una cuenta de servicio ni código de administración en la web. Las reglas controlan el acceso. La protección antiabuso avanzada y la limitación por IP requieren otro alcance; Spark no ofrece un servidor propio para aplicar esas cuotas por cliente.

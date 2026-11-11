# Backend e inyección de dependencias

## Implementación actual

FormLink usa Firebase Authentication por correo/contraseña y Firestore. Android consume sus APIs HTTPS desde adaptadores de repositorio, con sesión cifrada por Android Keystore. Room cifrado conserva borradores, solicitudes descargadas y cambios pendientes. Los SDK oficiales de Analytics y Crashlytics están encapsulados aparte y no reciben información del contenido de formularios.

Los formularios editables son privados. Publicar confirma una copia independiente para el cliente web; los envíos públicos se validan mediante reglas y se guardan como snapshots inmutables. La bandeja Android observa Room por propietario y sincroniza estados. Los originales de las portadas siguen cifrados localmente; publicar puede subir una copia a Cloudinary y asociar su referencia a la copia pública. Ver [portadas](CLOUDINARY_COVERS.md). Sin configuración, el invitado puede editar y la cuenta muestra que Firebase no está configurado.

- [Fuente de verdad y sincronización](../README.md#fuente-de-verdad-y-funcionamiento-sin-conexión).
- [Publicación, web y despliegue Spark](PUBLIC_FORMS.md).
- [Sesión, cifrado y migraciones](SECURITY.md).
- [Métricas y diagnóstico](ANALYTICS.md).
- [Resultados actuales de verificación](VERIFICATION_PHASE3.md).

La configuración del proyecto real es local en `app/google-services.json`, ignorada por Git. Habilitar Email/Password y Firestore Standard, registrar la app WEB y autenticarse en Firebase CLI para desplegar reglas/índices/Hosting. No se usa Storage ni Functions ni se activa Blaze. BoM 34.7.0 mantiene compatibilidad con Kotlin 2.2.10; agregar Analytics no justifica declarar simultáneamente dos BoM distintos.

## Inyección de dependencias y Hilt

Inyectar una dependencia significa recibir un colaborador ya construido: por ejemplo, `EditFormViewModel` recibe `FormRepository` y `CoverStorage` por constructor. Así el ViewModel no abre bases de datos ni crea clientes Firebase, y sus pruebas pueden suministrar implementaciones controladas.

`AppContainer` y `BackendServices` componen las instancias de vida de aplicación: Room, almacenamiento de portadas, Auth REST, transporte HTTPS, publicación, telemetría, fuente remota y motor de sincronización. Cada sesión obtiene un repositorio ligado a su propietario y ViewModels propios. La factoría de WorkManager proporciona esos mismos servicios al worker. Los Composables reciben estado y callbacks.

Migrar a Hilt supondría agregar su plugin/procesador, marcar la Application con `@HiltAndroidApp`, la Activity con `@AndroidEntryPoint`, definir módulos `@Provides`/`@Binds`, elegir scopes y convertir los ViewModels a `@HiltViewModel` con constructor `@Inject`. Los workers requerirían la integración de Hilt para WorkManager. Se mantendrían interfaces, lógica y pruebas de repositorio; Hilt no implementa el backend, sincronización ni MVVM.

Para FormLink puede ser conveniente al crecer el grafo de Firebase, Room y tareas de fondo. En esta entrega se conserva inyección manual para que el equipo pueda explicar el flujo y revisar los cambios de backend sin otra migración transversal. Una migración posterior a Hilt debe ser un PR independiente, sin modificar comportamiento.

Al elegir scopes con Hilt, Room, el cifrador y el transporte pueden vivir en aplicación; un repositorio ligado a `ownerId` no debe convertirse en singleton. También habría que adaptar el propietario personalizado de ViewModels de sesión o una navegación por cuenta, conservando su limpieza al salir. No basta con añadir anotaciones: las pruebas de cambio de cuenta y restauración deben seguir pasando.

Referencias: [Firebase Android](https://firebase.google.com/docs/android/setup), [Authentication REST](https://firebase.google.com/docs/reference/rest/auth), [WorkManager](https://developer.android.com/develop/background-work/background-tasks/persistent), [Hilt](https://developer.android.com/training/dependency-injection/hilt-android).

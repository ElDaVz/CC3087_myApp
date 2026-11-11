# Seguridad de FormLink

La UI depende de repositorios. Firebase Authentication y Firestore se consumen por HTTPS en Android mediante adaptadores REST para poder controlar la persistencia de la sesión: el SDK nativo de Auth no ofrece una configuración de almacenamiento Keystore. Analytics y Crashlytics conservan sus SDK oficiales. No almacenamos contraseñas.

`DeviceCipher` usa AES-256-GCM, nonce aleatorio y una clave no exportable de Android Keystore. Cifra el archivo de sesión y protege la contraseña aleatoria de SQLCipher. Room usa SQLCipher; al encontrar una base SQLite previa se exporta a un archivo cifrado, se verifica su integridad y se reemplaza el original. No se usan migraciones destructivas. Un fallo antes del reemplazo conserva el original. La eliminación del archivo anterior no constituye borrado forense garantizado de memoria flash.

Las portadas y derivados están cifrados; se normalizan y descifran en memoria. Las portadas previas se convierten antes de abrir la aplicación. Al publicar, una copia optimizada se sube a Cloudinary y pasa a ser pública por URL; ese cifrado local no implica privacidad de la copia publicada. El preset unsigned no autentica usuarios de Firebase; sus límites y el manejo de imágenes antiguas se explican en [portadas](CLOUDINARY_COVERS.md). El estado de los ViewModels de la sesión se serializa y cifra antes de entregarse al sistema de restauración. Los datos protegidos se excluyen de backup/transferencia; una clave perdida no provoca borrado automático.

La actualización solicita volver a autenticarse y elimina los archivos de sesión de la implementación anterior. Cambiar de cuenta descarta ViewModels y trabajos de la cuenta anterior. La sesión persiste cifrada; solo el token de la cuenta actual puede autorizar una operación.

Producción rechaza HTTP. Debug solo permite HTTP a localhost/127.0.0.1/10.0.2.2 para Emulator Suite. No se solicita almacenamiento externo: Photo Picker concede acceso a una imagen seleccionada. El único permiso de red explícito es INTERNET.

Los errores no muestran cuerpos de respuesta, URLs con parámetros ni credenciales. No deben registrarse respuestas, contraseñas, tokens o texto de formularios en Analytics/Crashlytics. `google-services.json`, `.env` y credenciales de servicio permanecen fuera de Git. La configuración web de Firebase es pública por diseño y no concede autorización; las reglas sí la conceden. Nunca colocar cuentas de servicio en el cliente.

## Comprobación

- `SecureStorageTest`: exportación de base existente, reapertura, texto sensible ausente y rechazo desde SQLite sin clave; AES-GCM rechaza manipulación y usa nonces diferentes.
- `CoverStorageTest`: portada cifrada, orientación, reapertura y eliminación.
- Pruebas de backend: sesiones, aislamiento entre propietarios y reglas.
- Revisión de Git antes de publicar: buscar credenciales sin imprimir valores; si una credencial privada estuvo en el historial, rotarla desde su proveedor y registrar únicamente la evidencia redactada.

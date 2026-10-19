# Save & Share — avance de la interfaz

Esta pantalla completa el siguiente paso del flujo: Preview → Save form → Save and share. La referencia es el [wireframe de Figma](https://www.figma.com/design/iiQcuGrIcwk3eCEBsdyhsE/Wireframes?node-id=3-10).

## Qué agregamos

La pantalla muestra la confirmación del guardado, el espacio del enlace, los botones de WhatsApp y copiar, el número real de campos y el estado del formulario. El botón inferior regresa a Forms. También se puede abrir desde Share en una tarjeta.

`SaveShareScreen` presenta el estado. `SaveShareViewModel` recibe el identificador y consulta el formulario guardado a través de `FormRepository`; no construye la base ni vuelve a insertar el borrador. `SaveShareUiState` separa la carga, los errores y los datos que necesita la pantalla.

El botón Save form de Preview se habilita cuando la configuración es válida y el guardado local terminó. Las respuestas de prueba de Preview siguen siendo temporales: guardar el formulario no las convierte en respuestas enviadas por un cliente.

## Guardado local no significa publicado

Room todavía solo contiene borradores. Por eso, la app muestra Draft y No public link yet. Copiar y compartir permanecen deshabilitados; no se genera una dirección inventada ni se presenta el borrador como Active.

Las acciones de Android para copiar un enlace y abrir WhatsApp están separadas de los ViewModels. Solo reciben un enlace si existe. El repositorio local no proporciona ninguno en esta entrega; la publicación real sigue dependiendo del backend y la autenticación pendientes.

La tarjeta Share también se deshabilita si ese borrador tiene cambios pendientes o un error de escritura. Así evitamos confirmar como guardada una edición que todavía no llegó a Room.

## Diseño y recursos

Conservamos la jerarquía y los bloques del wireframe, usando los componentes y el tema Material 3 del proyecto. El wireframe original usa una paleta clara y morada; el tema global actual también permite modo oscuro y color dinámico. No se cambió ese comportamiento en este paso.

Descargamos los cinco SVG originales a `app/src/main/res/raw`. Se cargan desde el APK con Coil y su módulo SVG, de la misma versión que ya usa el proyecto. No quedan URLs temporales de Figma en el código.

| Recurso | Ubicación en la pantalla | Tamaño del icono |
| --- | --- | --- |
| `save_share_back.svg` | Flecha de regreso | 16 × 16 dp |
| `save_share_success.svg` | Confirmación circular | 40 × 40 dp |
| `save_share_copy.svg` | Acción dentro del enlace | 14.1667 × 16.6667 dp |
| `save_share_whatsapp.svg` | Botón de WhatsApp | 20 × 20 dp |
| `save_share_link.svg` | Botón de copiar enlace | 16.6667 × 8.33333 dp |

Los tamaños respetan las proporciones originales. Los iconos de un solo color reciben el color del tema para mantener su legibilidad; los SVG no fueron redibujados.

## Pruebas

Conservamos JUnit 4, corrutinas de prueba, Compose Test y la inyección manual existentes. Los casos nuevos revisan la lectura del borrador sin escrituras adicionales, la restauración del identificador, los errores, el reintento y los botones deshabilitados sin enlace público.

Una prueba con Room en memoria recorre Preview → guardar → confirmación → Forms → Share. Otra comprueba que los cinco SVG locales se pueden decodificar. Las pruebas de copiar y compartir usan callbacks de prueba: no se enviaron mensajes ni se modificó el portapapeles del dispositivo.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest
```

Las pruebas instrumentadas de esta ronda se ejecutan directamente con `am instrument`, después de instalar ambos APKs con `adb install -r`. No se utiliza la limpieza automática de `connectedDebugAndroidTest`. Aun así, conviene usar un emulador de pruebas y conservar aparte los datos importantes.

La verificación terminó con 87 pruebas unitarias y 19 instrumentadas aprobadas, sin fallos. Compilaron tanto la app como el APK de pruebas.

También recorrimos la app instalada en el emulador Pixel 9 Pro. Creamos el borrador Prueba Save and Share para no cambiar los formularios existentes: Preview abrió la confirmación con sus 3 campos, Draft y el aviso de guardado local. Revisamos los cinco iconos, los márgenes, los botones públicos deshabilitados y los regresos al editor y a Forms. La captura de esta revisión quedó fuera del repositorio, en la carpeta de evidencias local.

La implementación de SVG se apoya en la [documentación oficial de Coil](https://coil-kt.github.io/coil/svgs/). Las acciones para compartir usan las [APIs de Android](https://developer.android.com/develop/ui/compose/sharing/send).

## Pendientes

Falta publicar en Firebase, generar el enlace real y habilitar las acciones públicas con esos datos. También sigue pendiente el desbloqueo biométrico y su opción en Settings. Este bloque no agrega autenticación, no modifica `MainActivity` y no sustituye la revisión final de la entrega.

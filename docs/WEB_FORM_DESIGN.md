# Un formulario web que se sienta parte de FormLink

La web ya permitía responder solicitudes, pero visualmente estaba separada de la app. Este cambio le da una presentación más cuidada, inspirada en la claridad de Google Forms y usando la identidad de FormLink.

## Qué vas a encontrar

Arriba a la derecha aparece el mismo icono de documento y enlace que usa Android al iniciar, acompañado del nombre FormLink. El SVG conserva los trazos y colores del recurso original; no es un logotipo nuevo.

Debajo hay un banner morado, el título del formulario y la cantidad de preguntas. Cada pregunta tiene su propia tarjeta, con una numeración discreta, descripción cuando existe y una indicación de si es opcional u obligatoria. Los controles seleccionados y los errores tienen estados visibles. El botón de envío ocupa todo el ancho en pantallas pequeñas.

La página se adapta a móvil, tablet y escritorio. Se puede recorrer con teclado, el foco lleva al primer campo con error y la confirmación recibe el foco al terminar. Las preferencias de movimiento reducido se respetan.

La carga, los enlaces que ya no están disponibles, los errores de conexión y la confirmación también usan el mismo estilo. Durante el envío se bloquean temporalmente los campos para evitar que la respuesta visible cambie mientras se confirma. Si falla, vuelven a habilitarse con lo que la persona había escrito.

## La portada del editor

El rediseño inicial usaba un banner decorativo. La integración posterior permite mostrar la portada publicada con Cloudinary y mantiene ese banner como respaldo. La imagen se sube al publicar el formulario, no al elegirla en el editor. Ver [configuración y comportamiento de portadas](CLOUDINARY_COVERS.md).

En esta tarea no hay cambios de Room, reglas de Firestore ni sincronización Android. El dashboard de administración también queda pendiente.

## Cómo revisar el diseño

La rama de trabajo es `feat/web-form-design`. Los cambios quedan locales para revisión: no se publica una nueva web ni se abre una PR en este paso.

Las pruebas visuales usan un formulario sintético con los nueve tipos de pregunta. Ese ejemplo existe solo dentro de las pruebas: no hay una ruta de demostración en la web publicada ni se envían sus datos a un backend real.

Desde la raíz del proyecto:

```bash
npm test --prefix web
npm run build --prefix web
# Solo diseño; requiere Chromium instalado para Playwright:
npm run test:e2e --prefix web -- design.spec.ts
```

En este equipo también puede utilizarse el Chrome instalado:

```bash
PLAYWRIGHT_CHROME_EXECUTABLE=/usr/sbin/google-chrome-stable npm run test:e2e --prefix web -- design.spec.ts
```

Las capturas se generan en `web/test-results/web-design-320.png`, `web-design-390.png`, `web-design-768.png` y `web-design-1440.png`. En ese mismo directorio quedan las pantallas de carga, error y confirmación. Son archivos de revisión generados, no recursos de la app.

Para probar además el envío real, el reintento sin conexión y la recuperación tras recargar, se ejecuta la suite completa contra Firebase local:

```bash
backend-tests/node_modules/.bin/firebase emulators:exec --project demo-formlink --only auth,firestore "bash scripts/test-web-browser.sh"
```

La verificación incluye pantallas de 320, 390, 768 y 1440 píxeles, títulos largos y texto ampliado al 200 %. La revisión visual se hizo sobre las capturas de móvil, escritorio y confirmación.

## Lo que se comprobó

El 11 de noviembre de 2026 pasaron las cinco pruebas unitarias web y las doce pruebas de navegador en Chrome. Estas últimas incluyen las diez pruebas nuevas de diseño y los dos flujos existentes contra los emuladores de Auth y Firestore: envío, reintento sin conexión, ausencia de duplicados, recarga y enlace inexistente.

La compilación TypeScript y el build de producción también terminaron correctamente. Vite advierte que el archivo JavaScript supera 500 kB antes de comprimir; la advertencia no impide compilar. En este trabajo no se cambiaron dependencias ni se amplió el alcance para optimizar ese paquete.

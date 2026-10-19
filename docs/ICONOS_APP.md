# Iconos de FormLink

Integramos los recursos entregados en `FormLink_Icon_Auto_Light_Dark.zip`. El launcher conserva los nombres `ic_launcher` e `ic_launcher_round`, así que no fue necesario cambiar el manifiesto. El nombre visible de la app ahora es FormLink, tanto en español como en inglés.

## Qué incluye

- versión clara con fondo morado y símbolo blanco y lavanda
- versión oscura mediante recursos `drawable-night` y `mipmap-night-*`
- versión monocromática para los iconos temáticos de Android 13 o superior
- imágenes WebP de las cinco densidades incluidas en el ZIP

Conservamos los diseños originales. Además de los XML adaptativos del ZIP, agregamos sus equivalentes en `mipmap-night-anydpi`: sin esa variante, el calificador nocturno de los WebP tenía prioridad y Android cargaba una imagen plana en modo oscuro. La prueba detectó ese problema antes del cierre del bloque.

El launcher puede conservar una copia del icono al cambiar el tema. Su actualización inmediata y el tintado de los iconos temáticos dependen del launcher; no significan que cambie la paleta interna de FormLink.

## Verificación

`LauncherIconTest` carga las variantes clara y oscura sin cambiar el tema global del dispositivo. Comprueba que los iconos normal y redondo son adaptativos, que su fondo corresponde al tema, que el símbolo no está vacío y que existe la capa monocromática en Android 13 o superior.

Se mantiene la configuración de pruebas existente: JUnit 4, Compose Test, corrutinas de prueba e inyección manual. No se agregaron dependencias ni frameworks para integrar los iconos.

La instalación se hace con `adb install -r`, sin desinstalar la app ni borrar los borradores locales.

El cierre de esta ronda terminó con compilación correcta, 87 pruebas unitarias y 20 instrumentadas aprobadas. La primera ejecución de la prueba nueva detectó la selección del WebP oscuro; después de agregar `mipmap-night-anydpi`, la suite completa pasó.

También revisamos el icono instalado en el launcher del Pixel 9 Pro y lo usamos para abrir la app. La captura `07-icono-launcher.png` quedó en la carpeta local de evidencias, fuera de Git. La revisión visual se hizo en el tema oscuro actual; la selección de recursos de ambos temas y la capa monocromática quedaron cubiertas por la prueba instrumentada.

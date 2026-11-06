# QA manual antes de integrar a main

Registrar fecha, commit, dispositivo/navegador, cuenta de prueba y responsable. Marcar solo casos realmente ejecutados; las cuentas y respuestas de demo no deben ser datos personales reales.

- [ ] Invitado crea y edita; cerrar y reabrir conserva cambios y portada.
- [ ] Actualizar desde una instalación anterior conserva borradores y solicitudes.
- [ ] Registro, contraseña incorrecta, login, logout y cambio de cuenta muestran estados claros y no mezclan datos.
- [ ] Importación de invitado requiere selección explícita.
- [ ] Preview valida pero no genera solicitudes.
- [ ] Continuar señala errores y espera el último guardado.
- [ ] Publicar sin sesión o sin conexión no anuncia un enlace nuevo.
- [ ] Publicar con conexión muestra un enlace HTTPS que abre desde otro navegador.
- [ ] El cliente no puede abrir un borrador ni listar formularios/respuestas.
- [ ] Probar los nueve tipos, requeridos, rangos, fechas, opciones inválidas y límite de diez preguntas.
- [ ] Envío confirmado aparece en consola y bandeja; cerrar sesión y volver a entrar lo conserva.
- [ ] Doble toque, recarga y reintento tras perder conexión no duplican la solicitud.
- [ ] Cambiar/reabrir estado sin conexión se sincroniza al recuperar red.
- [ ] Editar un borrador no modifica la copia publicada; actualizar conserva enlace.
- [ ] Retirar publicación bloquea envíos; borrar no elimina solicitudes históricas.
- [ ] Analytics DebugView muestra inicio/final con el mismo intento, duración válida y sin respuestas/texto libre.
- [ ] Solo la primera publicación finaliza la métrica; guardar borrador no la finaliza.
- [ ] Crash controlado en variante de prueba aparece tras reiniciar, sin datos sensibles.
- [ ] CI de este PR pasa compilación, pruebas y Lint.
- [ ] Otro integrante revisa y aprueba el PR.

## Registro de ejecución

| Fecha | Commit | Entorno | Casos | Resultado / evidencia | Responsable |
|---|---|---|---|---|---|
| Pendiente | — | — | — | Este checklist no representa una ejecución manual por sí mismo | — |

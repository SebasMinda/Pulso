# Validación de Pulso 0.3.0

APK Release compilado. Certificado e identidad iguales a Pulso 0.2.0; versionCode aumenta a 3. Firma verificada con apksigner. Android Lint sin errores.

## Descargas

Nueve pruebas HTTP locales comprueban unión exacta de partes y conexiones simultáneas, alternativa cuando se ignoran o se devuelven rangos incorrectos, reintento de una parte, cancelación, rechazo de respuestas que no son audio, archivo pequeño, cancelación antes de iniciar y rechazo de transferencias cortadas. La comparación usa todos los bytes del archivo esperado. Se mantienen las cinco pruebas del sistema de actualización.

Se usa una consulta inicial de hasta 256 KB y tres trabajadores que descargan partes de hasta 1 MB. Cada parte fallida se reintenta una vez. Si falla el modo por partes, se cierran todos los trabajadores antes de comenzar la alternativa de una conexión. La cancelación no inicia esa alternativa. Se validan los rangos y, cuando el servidor lo aporta, ETag o Last-Modified.

No se ha medido la velocidad con YouTube ni ejecutado el APK en un teléfono o emulador. Las pruebas locales comprueban el mecanismo y la integridad; no garantizan aceleración en todos los servidores o redes. La descarga real anterior de YouTube quedó bloqueada por la red del entorno.

## Prueba en el teléfono

1. Instalar 0.3.0 sobre Pulso 0.2.0 sin desinstalar y comprobar que conserva la biblioteca.
2. Descargar el mismo audio con ambas versiones usando la misma red y formato, anotando el tiempo.
3. Reproducir el archivo completo, mover la posición y exportarlo.
4. Cancelar durante la descarga y volver a consultar el enlace.
5. Probar reproducción y controles con pantalla bloqueada.
6. Cuando exista una Release firmada superior, probar su instalación desde la app.

Esta versión importa FLAC pero no convierte las descargas de YouTube a FLAC. Mantener la app abierta hasta que termine la descarga.

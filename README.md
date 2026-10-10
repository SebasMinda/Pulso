# Pulso para Android

Reproductor de música personal en español, con biblioteca local, audio de videos públicos de YouTube y actualización desde la propia app. Android 8 o posterior.

## Funciones

- Reproducción de MP3, M4A, FLAC, WAV y WebM/Opus.
- Reproducción en segundo plano y controles en la notificación.
- Importación de varios archivos desde el selector de Android.
- Consulta de formatos pegando un enlace o compartiéndolo desde YouTube.
- Descarga del flujo original sin convertir ni volver a comprimir.
- Descarga por partes con hasta tres conexiones, reintentos y alternativa de una conexión.
- Progreso con cantidad descargada y velocidad promedio.
- Exportación de descargas a una carpeta elegida por el usuario.
- Aviso de actualizaciones y descarga del APK firmado desde **SebasMinda/Pulso**.

## Instalar

Las versiones firmadas se publican en [Releases](https://github.com/SebasMinda/Pulso/releases). Descarga `Pulso.apk` y confirma la instalación en Android. Los APKs de desarrollo de Actions son para pruebas; no son las actualizaciones de la app estable.

Pulso 0.1 usaba una firma de prueba cuya clave no se conservó. La distribución estable 0.2.0 se instala junto a 0.1 y mantiene una firma fija para las siguientes versiones. Consulta [cómo pasar tus canciones y publicar actualizaciones](docs/ACTUALIZACIONES.md).

## Usar

1. Pulsa **+ Importar** y selecciona tus canciones.
2. Toca una canción para reproducirla. Concede el permiso de notificaciones si quieres los controles fuera de la app.
3. Para YouTube, pega el enlace de un video o usa **Compartir → Pulso** y pulsa **Buscar audio**.
4. Elige un formato y mantén Pulso abierta hasta que termine la descarga.
5. Con **⋮ → Guardar una copia…** puedes copiar una descarga a una carpeta del teléfono.
6. En **Actualizaciones**, pulsa **Buscar actualizaciones**. Si hay una versión nueva, acepta su descarga y confirma la instalación. **Avisar al abrir Pulso** consulta como máximo una vez al día mientras abres la app.

## Calidad y límites

Se conserva el formato original disponible: M4A suele contener AAC y WebM puede contener Opus. El bitrate se muestra tal como lo informa el extractor; no es una medida universal para comparar distintos códecs.

Convertir audio comprimido a FLAC no recupera información perdida. Puedes importar FLAC que ya tengas, pero no se ofrece conversión de YouTube a FLAC.

- Las descargas requieren mantener la app abierta y se cancelan al cerrar la actividad.
- No hay búsqueda por nombre, carátulas, ecualizador ni descarga de playlists.
- YouTube puede cambiar su sistema o restringir videos. Se rechazan respuestas que no sean audio; no se eluden inicios de sesión ni protección de contenido.
- Quitar una canción de la biblioteca no borra su archivo. Desinstalar borra las descargas privadas: exporta lo que quieras conservar antes de hacerlo.
- Las actualizaciones requieren confirmación. Se comprueban tamaño, SHA-256, identidad, versión y firma del APK.
- Consulta [VALIDACION.md](VALIDACION.md) para el alcance de las pruebas.

Descarga únicamente material propio o con permiso. Pulso no está afiliada a YouTube, Google ni NewPipe.

## Desarrollo

JDK 17 y Android SDK 35. Abre el proyecto en Android Studio o ejecuta:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Para compilar una versión firmada, establece `PULSO_KEYSTORE_PATH` y `PULSO_KEYSTORE_PASSWORD`, y ejecuta `./gradlew :app:assembleRelease`. El alias es `pulso`; clave y almacén usan la misma contraseña. Nunca incluyas la clave privada en este repositorio.

[GitHub Actions](.github/workflows/android.yml) ejecuta las comprobaciones con cada cambio en `main`. Para publicar necesita los dos secretos descritos en [ACTUALIZACIONES.md](docs/ACTUALIZACIONES.md).

## Licencia

GPL-3.0-or-later. Código fuente de Pulso y avisos de dependencias incluidos en `third-party`.

- NewPipeExtractor, Team NewPipe, GPL-3.0-or-later, revisión `65cabc2ba5216ee871ace4a9963c08bdbf5d5dc0`.
- OkHttp 4.12.0 y AndroidX Core 1.15.0, Apache-2.0.
- Las versiones de dependencias se identifican en Gradle para reconstruir la aplicación.

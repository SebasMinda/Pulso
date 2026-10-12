# Pulso para Android

Reproductor de música personal en español, con biblioteca local, audio de videos públicos de YouTube y actualización desde la propia app. Android 8 o posterior.

## Funciones

- Búsqueda de canciones y artistas en YouTube normal con miniaturas, canal, duración y paginación.
- Lista persistente de descargas, calidad por canción, botón Descargar todo y reintento de fallidas. Cada resultado también permite descargar solo esa canción con la mejor calidad disponible.
- Agregar enlaces individuales o pegar varios enlaces, uno por línea.

- Reproducción de MP3, M4A, FLAC, WAV y WebM/Opus.
- Pestañas Biblioteca, Descargas y Ajustes. Reproductor expandible desde la barra inferior mediante gesto o toque. Desliza a la izquierda o a la derecha para cambiar de canción en ambos tamaños.
- Modo aleatorio guardado, sin repetir canciones dentro de cada vuelta y con historial para Anterior.
- Portadas de los nuevos videos descargados, portadas incrustadas de archivos importados y elección manual de una imagen.
- Portadas guardadas en el teléfono para escucharlas sin internet.
- Reproducción en segundo plano y controles en la notificación.
- Importación de varios archivos desde el selector de Android.
- Consulta de formatos pegando un enlace o compartiéndolo desde YouTube.
- Descarga del flujo original sin convertir ni volver a comprimir.
- Descarga por partes con hasta tres conexiones, reintentos y alternativa de una conexión.
- Progreso con cantidad descargada y velocidad promedio.
- Exportación de descargas a una carpeta elegida por el usuario.
- Aviso de actualizaciones y descarga del APK firmado desde **SebasMinda/Pulso**.
- Apariencia en Ajustes con el tema Pulso clásico y colores personalizados para fondo, botones y detalles. La elección se guarda y se aplica sin detener la música.

## Instalar

Las versiones firmadas se publican en [Releases](https://github.com/SebasMinda/Pulso/releases). Descarga `Pulso.apk` y confirma la instalación en Android. Los APKs de desarrollo de Actions son para pruebas; no son las actualizaciones de la app estable.

Pulso 0.1 usaba una firma de prueba cuya clave no se conservó. La distribución estable 0.2.0 se instala junto a 0.1 y mantiene una firma fija para las siguientes versiones. Consulta [cómo pasar tus canciones y publicar actualizaciones](docs/ACTUALIZACIONES.md).

## Usar

1. Pulsa **+ Importar** y selecciona tus canciones.
2. En Biblioteca, toca una canción para escucharla. Desliza la barrita inferior hacia arriba para abrir el reproductor y hacia abajo para reducirlo. Puedes activar Aleatorio dentro del reproductor. Concede el permiso de notificaciones si quieres los controles fuera de la app.
3. En Descargas, busca una canción o artista y pulsa **Seleccionar** en el video elegido. Escoge **Agregar al listado** o **Descargar solo esta** para usar automáticamente la mejor calidad disponible. Abre **Tu lista** para ver tus pendientes. Abre **Por enlace** para pegar un enlace o usar **Pegar varios enlaces**. También puedes compartir un enlace desde YouTube.
4. Revisa tu lista y cambia **Calidad** en cada canción si quieres. Pulsa **Descargar todo** y mantén Pulso abierta. **Detener descargas** conserva lo terminado y permite reiniciar lo pendiente. Las fallidas quedan en la lista para reintentar. Las descargadas se quitan automáticamente de la lista y aparecen en la biblioteca.
5. Con **⋮ → Guardar una copia…** puedes copiar una descarga a una carpeta del teléfono.
6. En **Ajustes → Actualizaciones**, pulsa **Buscar actualizaciones**. Si hay una versión nueva, acepta su descarga y confirma la instalación. **Avisar al abrir Pulso** consulta como máximo una vez al día mientras abres la app.
7. En **Ajustes → Apariencia**, escoge **Pulso clásico** para recuperar el tema predeterminado o **Personalizado** para volver a tus colores guardados. Cambia el fondo, los botones y los detalles con los controles de color o un código hexadecimal y pulsa **Guardar**.

## Calidad y límites

Se conserva el formato original disponible: M4A suele contener AAC y WebM puede contener Opus. El bitrate se muestra tal como lo informa el extractor; no es una medida universal para comparar distintos códecs.

Convertir audio comprimido a FLAC no recupera información perdida. Puedes importar FLAC que ya tengas, pero no se ofrece conversión de YouTube a FLAC.

- Las descargas requieren mantener la app abierta y se cancelan al cerrar la actividad.
- No hay ecualizador ni importación automática de playlists. Puedes armar una lista propia desde resultados o enlaces.
- La lista admite 100 canciones; cada búsqueda muestra hasta 200 resultados. Se descarga una canción por vez, con hasta tres conexiones por archivo.
- Las calidades y videos disponibles dependen de YouTube. Si desaparece la calidad elegida, se pide cambiarla desde **Calidad**; no se sustituye silenciosamente.
- Las descargas antiguas sin imagen guardada pueden usar **⋮ → Elegir portada…**. No se consulta automáticamente una portada de álbum por nombre.
- La imagen de YouTube es la miniatura del video y puede no ser una portada oficial del álbum.
- YouTube puede cambiar su sistema o restringir videos. Se rechazan respuestas que no sean audio; no se eluden inicios de sesión ni protección de contenido.
- Quitar una canción de la biblioteca no borra su archivo. Desinstalar borra las descargas privadas: exporta lo que quieras conservar antes de hacerlo.
- Las actualizaciones requieren confirmación. Se comprueban tamaño, SHA-256, identidad, versión y firma del APK.
- Consulta [VALIDACION.md](VALIDACION.md) para el alcance de las pruebas.

Descarga únicamente material propio o con permiso. Pulso no está afiliada a YouTube, Google ni NewPipe.

## Guía del código

Consulta [el mapa de archivos de Pulso](docs/MAPA_DEL_PROYECTO.md) para entender qué hace cada archivo y cómo se conectan biblioteca, descargas, portadas y reproducción. El código propio incluye comentarios simples en español.

## Desarrollo

JDK 17 y Android SDK 35. Abre el proyecto en Android Studio o ejecuta:

```sh
./gradlew :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Para compilar una versión firmada, establece `PULSO_KEYSTORE_PATH` y `PULSO_KEYSTORE_PASSWORD`, y ejecuta `./gradlew :app:assembleRelease`. El alias es `pulso`; clave y almacén usan la misma contraseña. Nunca incluyas la clave privada en este repositorio.

[GitHub Actions](.github/workflows/android.yml) ejecuta las comprobaciones con cada cambio en `main`. El método elegido es firmar localmente y subir el APK a `.release/Pulso.apk`. El flujo de publicación comprueba la firma estable y publica el APK y sus metadatos en Releases, sin secretos de firma en GitHub. La firma automática con dos secretos se conserva como alternativa opcional. Consulta [ACTUALIZACIONES.md](docs/ACTUALIZACIONES.md).

## Licencia

GPL-3.0-or-later. Código fuente de Pulso y avisos de dependencias incluidos en `third-party`.

- NewPipeExtractor, Team NewPipe, GPL-3.0-or-later, revisión `65cabc2ba5216ee871ace4a9963c08bdbf5d5dc0`.
- OkHttp 4.12.0 y AndroidX Core 1.15.0, Apache-2.0.
- Las versiones de dependencias se identifican en Gradle para reconstruir la aplicación.

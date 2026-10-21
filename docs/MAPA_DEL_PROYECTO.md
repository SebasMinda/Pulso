# Guía para leer el código de Pulso

Pulso usa Java para construir las pantallas y Android para reproducir música. Esta guía indica qué leer primero y dónde cambiar cada función. El código de la aplicación está en `app/src/main/java/com/sebas/pulso/` y las pruebas están en `app/src/test/java/com/sebas/pulso/`.

## Un recorrido sencillo

1. Empieza por `Track.java`. Representa una canción y sus cuatro datos: título, ubicación del audio, portada y artista.
2. Lee `MainActivity.onCreate` y `MainActivity.build`. Son la entrada a la aplicación y el orden de creación de las pantallas.
3. Abre `PlayerService.java`. Controla la reproducción y sigue funcionando cuando la actividad deja de estar delante.
4. Sigue el recorrido de una descarga: `prepareVideo`, `useAudioQuality`, `startDownloads`, `runDownloadBatch`, `TrackDownloader.download` y `AudioDownload.download`.
5. Mira `LibraryStore.java` para entender cómo se conservan biblioteca y pendientes al cerrar la app.

Los nombres de clases y métodos están en inglés, como es habitual en Java. Los comentarios explican su propósito en español. No hace falta leer todo de una vez: busca primero la función que quieres entender y luego sus llamadas.

## Responsabilidad de cada clase

| Archivo | Responsabilidad |
| --- | --- |
| `MainActivity.java` | Coordina las acciones del usuario, construye las pantallas y actualiza sus controles. |
| `PlayerService.java` | Reproduce con MediaPlayer, gestiona el foco de audio y muestra los controles multimedia de Android. |
| `Track.java` | Datos de una canción y conversión a JSON. |
| `LibraryStore.java` | Lee y guarda biblioteca y pendientes usando las mismas preferencias de las versiones anteriores. |
| `DownloadQueue.java` | Mantiene entradas pendientes, calidad, estado y error. Entrega copias para que el hilo de descarga trabaje sin modificar el listado directamente. |
| `TrackDownloader.java` | Resuelve la dirección temporal del audio, prepara el archivo y devuelve una canción completa. No cambia las vistas ni el listado. |
| `AudioDownload.java` | Copia los bytes del audio por partes o con una conexión. Reintenta, verifica rangos y elimina archivos incompletos. |
| `AudioChoice.java` | Identifica la calidad y la pista de audio. Ordena por tasa de audio y conserva la elección cuando cambia la dirección de descarga. |
| `Net.java` | Adapta las peticiones de NewPipe a OkHttp. |
| `CoverArt.java` | Carga portadas en segundo plano, guarda copias locales y comparte la imagen en memoria entre las vistas. |
| `ShuffleOrder.java` | Orden aleatorio sin repetir canciones en una vuelta e historial para volver atrás. |
| `PlayerGesture.java` | Decide si un movimiento corresponde a siguiente, anterior, expandir o contraer. |
| `ThemePalette.java` | Colores del tema, mezcla de tonos y contraste del texto. |
| `ColorPickerDialog.java` | Vista previa, controles RGB y entrada hexadecimal. Devuelve el color confirmado; la actividad guarda el tema. |
| `BrandView.java` | Dibuja PULSO, la esfera y el disco. Cada dibujo tiene su propio método. |
| `PlaybackHeaderView.java` | Alterna BrandView y las barras. Abre Visualizer sobre la sesión de Pulso y lo libera al pausar u ocultarse. |
| `AudioSpectrum.java` | Agrupa las frecuencias reales en 24 bandas y suaviza sus alturas. Es Java sin dependencias de Android. |
| `VisualizerPermission.java` | Explica el permiso de audio al empezar a escuchar y permite recuperarlo desde Apariencia. |
| `WaveSeekBar.java` | Dibuja la línea restante y la onda decorativa de progreso. Android mantiene el arrastre y la accesibilidad. |
| `Updater.java` | Busca una actualización, descarga el APK, verifica identidad y firma y abre el instalador. |
| `UpdateSpec.java` | Comprueba los metadatos de actualización y la integridad del archivo mediante tamaño y SHA-256. |

La cola de reproducción del servicio indica qué escuchar. `DownloadQueue` indica qué archivos descargar. Son listas distintas.

## Cómo leer MainActivity

### Creación de la pantalla

`build` llama en orden a `buildShell`, `buildBrandHeader`, `buildPageContainer`, `buildLibraryPage`, `buildExpandedPlayerPage`, `buildDownloads`, `buildSettingsPage`, `buildMiniPlayer` y `buildNavigation`.

Las constantes `PAGE_LIBRARY`, `PAGE_PLAYER`, `PAGE_DOWNLOADS` y `PAGE_SETTINGS` sustituyen índices dispersos. El reproductor grande se superpone a la página elegida; no tiene una pestaña inferior propia.

`page` crea una página desplazable y cambia la referencia `root` al contenedor de esa página. Por eso cada constructor debe crear su página antes de añadir secciones con `section`. Esta referencia es solo para construir la interfaz, no representa la actividad completa.

### Descargas

`buildDownloads` separa buscador, progreso, resultados, enlaces y pendientes en cinco métodos de construcción.

Al seleccionar un resultado, `prepareVideo` consulta sus formatos. `useAudioQuality` utiliza automáticamente la mayor tasa disponible para canciones nuevas. Al editar una entrada del listado se abre el selector manual.

`startDownloads` prepara controles y cancelación. `runDownloadBatch` recorre las entradas y pide a `TrackDownloader` una canción completa. Los métodos `showDownloadStarted`, `showDownloadProgress`, `addDownloadedTrack`, `showDownloadFailed` y `finishDownloads` muestran cada etapa.

El parámetro `queued` indica si la operación pertenece al listado. Cuando es falso, se descarga una sola canción y las entradas pendientes se conservan.

### Actualización del reproductor

`tick` se ejecuta cada `PLAYER_REFRESH_MS`, actualmente 500 milisegundos. Llama a `refreshPlaybackUi`, que separa textos, portadas y posición en sus propios métodos. Mientras `seeking` sea verdadero, el usuario está arrastrando la barra y la actividad no sobrescribe su posición.

`shownCover` evita volver a cargar la misma portada en cada actualización.

### Cabecera que responde a la música

`refreshPlaybackHeader` consulta `PlayerService.playing` y `audioSessionId`. En reposo se ve el mismo `BrandView` de antes. Mientras se reproduce, `PlaybackHeaderView` abre `android.media.audiofx.Visualizer` con la sesión de esa canción, nunca con la sesión cero que representa toda la salida del teléfono.

Cada 33 milisegundos lee una FFT: pares de números que indican la energía de las distintas frecuencias. `AudioSpectrum` reúne esas frecuencias en 24 bandas de graves a agudos. Una subida rápida y una bajada suave hacen legibles los golpes de la música. Un silencio lleva todas las alturas a cero; no se usan valores aleatorios ni una animación prefijada.

`VisualizerPermission` ofrece el permiso una vez al comenzar a escuchar. Android lo presenta como Micrófono porque Visualizer exige `RECORD_AUDIO`, pero aquí el efecto se conecta al audio de MediaPlayer. Rechazarlo conserva la marca y permite seguir usando la app. El botón de Apariencia permite volver a intentarlo. Cancelar la explicación no se confunde con un rechazo permanente del permiso.

Al pausar, cambiar de canción, ocultar la ventana o pasar a segundo plano se cancelan las lecturas y se libera el efecto. Al volver, se abre para la sesión actual. Si falla en un equipo, vuelve la marca sin reintentar continuamente. Si las animaciones de Android están desactivadas, no se abre el analizador.

## Hilos y cancelación

El hilo principal de Android dibuja las vistas y atiende botones. Las consultas y la copia de audio van a ejecutores para evitar que la pantalla se congele.

| Elemento | Dónde trabaja |
| --- | --- |
| `lookups` en MainActivity | Consulta YouTube y prepara enlaces. |
| `jobs` en MainActivity | Copia audio y exporta archivos, de forma secuencial. |
| `ui` en MainActivity | Envía cambios de vistas y listados al hilo principal mediante Handler. |
| `TrackDownloader.cancel` | Marca la cancelación y detiene el AudioDownload activo. |
| `CoverArt.worker` | Lee y descarga imágenes; después envía su resultado al hilo principal. |
| `Updater.worker` | Consulta GitHub, descarga y comprueba el APK. |

Una variable `volatile` permite que distintos hilos vean el cambio de un indicador, como una cancelación. No convierte una lista compartida en segura: por eso la biblioteca y los pendientes se modifican en el hilo principal.

En el código, `try` ejecuta una operación que puede fallar, `catch` atiende ese fallo y `finally` libera recursos incluso si hubo un error. Los archivos temporales terminan en `.part` y solo se renombran al completarse correctamente.

## Guardado de datos

`LibraryStore` recibe `getPreferences(MODE_PRIVATE)` de MainActivity. Conserva las claves `tracks` y `downloadQueue`, por lo que puede leer las bibliotecas existentes.

`saveTracks` y `saveDownloads` guardan cada listado por separado. `saveSnapshot` usa una sola edición de preferencias cuando termina una descarga, para conservar juntos la canción añadida y el listado pendiente actualizado.

| Dato | Ubicación dentro de Android |
| --- | --- |
| Audio descargado | `files/music/` |
| Portadas locales | `files/covers/` |
| APK temporal de actualización | `cache/updates/` |
| Canciones importadas | La ubicación elegida en el selector de archivos. |
| Biblioteca, pendientes y opciones | Preferencias privadas de la app. |

Estas son rutas del teléfono. Quitar una entrada de la biblioteca no borra su archivo. Desinstalar Pulso elimina sus archivos privados.

## Dónde hacer cambios

| Quieres cambiar | Revisa |
| --- | --- |
| Títulos, botones o distribución de una pantalla | Sus métodos `build...` en MainActivity. |
| Colores y degradados | ThemePalette y los métodos `paint...` de MainActivity. |
| Selector de colores | ColorPickerDialog. |
| Animación de letras, esfera o disco | `BrandView.onDraw`, `drawLogo`, `drawSphere`, `drawDisc` y `drawLetters`. |
| Visualizador de la cabecera | PlaybackHeaderView para captura y dibujo; AudioSpectrum para bandas y suavizado; VisualizerPermission para el permiso. |
| Onda de avance | `WaveSeekBar.drawPlayedWave` y `updateAnimation`. Es decorativa, no mide el sonido. |
| Direcciones y distancia mínima de los gestos | PlayerGesture. |
| Reproducción o notificación multimedia | PlayerService. |
| Calidad predeterminada | AudioChoice y `MainActivity.useAudioQuality`. |
| Crear nombres de archivos o resolver el audio | TrackDownloader. |
| Conexiones paralelas, rangos y reintentos | AudioDownload. |
| Guardado de biblioteca y pendientes | LibraryStore. |
| Comprobación e instalación de versiones | Updater y UpdateSpec. |

## Pruebas y compilación

Las pruebas de Track, AudioChoice, DownloadQueue, ShuffleOrder, PlayerGesture, ThemePalette y UpdateSpec comprueban sus reglas. AudioDownloadTest usa un servidor local para probar partes, reintentos, cancelación y respuestas incompletas. LibraryStoreTest verifica compatibilidad con los datos guardados y la edición conjunta de biblioteca y pendientes.

AudioSpectrumTest comprueba silencio, graves frente a agudos, intensidad, caída al silencio, datos FFT extremos, entradas inválidas y reinicio entre canciones. El permiso y la captura nativa requieren además una revisión en un teléfono.

Para ejecutar las comprobaciones con Java 17 y el SDK Android configurados:

```bash
bash gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease
```

Las pruebas de lógica y la compilación no sustituyen revisar gestos y animaciones en un teléfono.

## Otros archivos del repositorio

| Archivo | Propósito |
| --- | --- |
| `app/src/main/AndroidManifest.xml` | Permisos, actividad, servicio y proveedor para entregar el APK al instalador. |
| `app/src/main/res/drawable/` | Icono, nota musical y portada de reemplazo como vectores. |
| `app/src/main/res/values/styles.xml` | Tema inicial de Android. |
| `app/src/main/res/xml/file_paths.xml` | Acceso temporal al archivo de actualización mediante FileProvider. |
| `build.gradle` | Declara la versión del complemento Android. |
| `settings.gradle` y `app/build.gradle` | Módulo, dependencias, SDK, Java, identidad y firma de la app. |
| `version.properties` | Versión interna y visible; también la leen los scripts de publicación. |
| `gradlew`, `gradlew.bat` y `gradle/wrapper/` | Ejecutan la versión de Gradle del proyecto. |
| `tools/make_update.py` | Genera metadatos desde el APK real. |
| `tools/verify_local_release.py` | Verifica el certificado público, la identidad y la versión del APK. |
| `tools/ExtractorProbe.java` | Consulta formatos y comprueba el audio del extractor. |
| `.github/workflows/android.yml` | Pruebas, revisión y compilación en GitHub Actions. |
| `.github/workflows/publish-local.yml` | Publica cuando cambia el APK firmado de `.release/` en main. |
| `SIGNING_CERTIFICATE.txt` | Huella pública del certificado, sin clave privada. |
| `docs/ACTUALIZACIONES.md` | Procedimiento de firma y publicación. |
| `third-party/` y `LICENSE` | Atribuciones y licencias, conservadas íntegramente. |

La versión se define en `version.properties`. Subir código a main ejecuta las comprobaciones; la publicación para el teléfono solo ocurre cuando se añade el APK firmado en `.release/Pulso.apk` y pasa el flujo de publicación.

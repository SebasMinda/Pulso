# Mapa de archivos de Pulso

Pulso es un reproductor de música para Android. Esta guía explica el propósito de los archivos del repositorio; el código propio lleva comentarios en español en las clases y operaciones principales.

## Por dónde empezar

1. `Track.java`: los datos de una canción.
2. `MainActivity.java`: las pantallas y las acciones del usuario.
3. `PlayerService.java`: cómo se reproduce la música.
4. `DownloadQueue.java`, `AudioChoice.java` y `AudioDownload.java`: cómo se prepara y descarga el audio.
5. `CoverArt.java`: cómo se obtienen y guardan las portadas.
6. `Updater.java` y `UpdateSpec.java`: cómo se comprueban e instalan actualizaciones.

## Cómo se conectan las partes

Al abrir la app, `MainActivity` recupera la biblioteca y conecta `PlayerService`. Cuando eliges una canción, el servicio abre su archivo y controla la reproducción. La actividad consulta al servicio cada medio segundo para actualizar la pantalla.

Al buscar un video, NewPipe obtiene sus datos usando `Net`. `AudioChoice` identifica la calidad y `DownloadQueue` guarda los pendientes. Al descargar, se consulta una dirección nueva para la misma calidad y `AudioDownload` copia el audio a un archivo temporal. Si termina correctamente, la actividad lo añade como `Track` a la biblioteca y lo quita de pendientes.

`CoverArt` obtiene una imagen elegida, una miniatura del video o una portada incrustada. Guarda una copia en el teléfono y la reutiliza. `ThemePalette` calcula los colores y `PlayerGesture` decide qué acción corresponde a un deslizamiento.

## Código de la aplicación

Ruta: `app/src/main/java/com/sebas/pulso/`.

| Archivo | Qué hace |
| --- | --- |
| `MainActivity.java` | Construye la interfaz en Java: biblioteca, reproductor, descargas y ajustes. Gestiona importación, exportación, búsqueda y guardado de datos. |
| `PlayerService.java` | Usa `MediaPlayer` para reproducir en segundo plano. Gestiona la sesión multimedia, el audio, la cola y los controles de la notificación. Pausa al desconectar auriculares. |
| `Track.java` | Contiene título, URI del audio, portada y artista. Convierte esos datos a JSON y los recupera. Una URI indica dónde leer el archivo. |
| `CoverArt.java` | Carga portadas fuera del hilo de la pantalla. Reduce imágenes grandes y reutiliza copias en memoria y almacenamiento local. |
| `DownloadQueue.java` | Mantiene hasta 100 descargas con calidad, estado y error. Recupera pendientes al volver a abrir Pulso. |
| `AudioChoice.java` | Identifica formato, pista de audio y bitrate. Conserva la calidad elegida aunque caduque la dirección de descarga. |
| `AudioDownload.java` | Copia audio M4A o WebM sin convertirlo. Intenta hasta tres conexiones, reintenta partes y puede usar una sola conexión. Elimina archivos incompletos si falla. |
| `Net.java` | Conecta NewPipe con OkHttp, la biblioteca que realiza peticiones de red. |
| `ShuffleOrder.java` | Genera vueltas aleatorias sin repetir canciones y guarda historial para volver a la anterior. |
| `PlayerGesture.java` | Interpreta deslizamientos: izquierda/siguiente, derecha/anterior, arriba/expandir y abajo/reducir cuando corresponde. |
| `ThemePalette.java` | Define fondo, botones y detalles. Calcula tonos derivados y el color del texto según el fondo. |
| `Updater.java` | Consulta la publicación estable más reciente de GitHub, descarga el APK, comprueba su firma y abre el instalador. |
| `UpdateSpec.java` | Valida los metadatos de actualización, origen del enlace, versión, tamaño y SHA-256. SHA-256 es una huella que permite detectar cambios en el archivo. |

La lista de reproducción de `PlayerService` y la lista de descargas de `DownloadQueue` tienen propósitos distintos: una determina qué escuchar y la otra qué archivos descargar.

## Android y recursos visuales

| Archivo | Qué hace |
| --- | --- |
| `app/src/main/AndroidManifest.xml` | Declara permisos, pantalla principal, recepción de enlaces compartidos, servicio de reproducción y proveedor de archivos. |
| `app/src/main/res/drawable/icon.xml` | Dibuja el icono de Pulso como vector. |
| `app/src/main/res/drawable/cover_placeholder.xml` | Dibuja la portada de reemplazo cuando falta una imagen. |
| `app/src/main/res/values/styles.xml` | Define el tema inicial de Android y sus colores. |
| `app/src/main/res/xml/file_paths.xml` | Permite entregar al instalador un APK de la carpeta temporal `updates/` mediante `FileProvider`. |

Las pantallas se construyen en `MainActivity`; este proyecto no tiene archivos de diseño en `res/layout/`.

## Compilación y Git

| Archivo | Qué hace |
| --- | --- |
| `.gitignore` | Evita subir cachés, compilaciones, configuración local y claves privadas. No excluye `.release/Pulso.apk`. |
| `build.gradle` | En el repositorio configura el complemento de Android en el proyecto raíz. Está eliminado en la copia local al redactar esta guía; se respetó ese cambio previo. |
| `settings.gradle` | Nombra el proyecto, incluye el módulo `app` y define los repositorios de complementos y dependencias. |
| `app/build.gradle` | Configura la app: identificador, SDK, versión, Java, firma de producción y bibliotecas. Lee la versión desde `version.properties`. |
| `gradle.properties` | Configura memoria, UTF-8 y AndroidX para Gradle. |
| `version.properties` | Contiene `versionCode`, el número interno, y `versionName`, la versión visible. También lo leen los scripts de publicación. |
| `gradlew` | Inicia Gradle en Linux y macOS usando el wrapper del proyecto. |
| `gradlew.bat` | Inicia Gradle en Windows. |
| `gradle/wrapper/gradle-wrapper.properties` | Fija la versión de Gradle, su descarga y la huella para verificarla. |
| `gradle/wrapper/gradle-wrapper.jar` | Ejecutable del wrapper que prepara Gradle. Es un binario; su función se documenta aquí. |

`local.properties`, si existe en tu equipo, contiene configuración local como la ruta del SDK. Las carpetas `.gradle/`, `build/` y `app/build/` son cachés o resultados generados. `.git/` contiene el historial y la configuración de Git, no código de la app.

## Pruebas existentes

Ruta: `app/src/test/java/com/sebas/pulso/`. Estas pruebas comprueban la lógica con JUnit; no sustituyen la interacción en un teléfono.

| Archivo | Qué comprueba |
| --- | --- |
| `TrackTest.java` | Guardado de canciones, portadas y artistas; compatibilidad con bibliotecas antiguas. |
| `AudioChoiceTest.java` | Calidad persistente, direcciones caducadas, formatos duplicados y conservación de la pista elegida. |
| `AudioDownloadTest.java` | Descargas por partes con un servidor local, reintentos, cancelación, alternativa de una conexión y rechazo de respuestas inválidas o incompletas. |
| `DownloadQueueTest.java` | Persistencia de pendientes, calidades, duplicados, copias de trabajo y eliminación de descargas terminadas. |
| `PlayerGestureTest.java` | Direcciones, distancia mínima, diagonales y gestos de ambos tamaños del reproductor. |
| `ShuffleOrderTest.java` | Vueltas sin repeticiones, historial y cambios de biblioteca. |
| `ThemePaletteTest.java` | Colores clásicos, mezcla de colores, texto claro u oscuro y códigos hexadecimales. |
| `UpdateSpecTest.java` | Versiones nuevas, enlaces permitidos, identidad, tamaño e integridad del archivo. |

## Herramientas y publicación

| Archivo | Qué hace |
| --- | --- |
| `tools/ExtractorProbe.java` | Consulta formatos de un video para comprobar NewPipe. Con un segundo argumento intenta descargar el audio elegido y exige una cabecera WebM. |
| `tools/make_update.py` | Genera `update.json` a partir del APK real, la versión y el registro de cambios. Incluye tamaño y SHA-256. |
| `tools/verify_local_release.py` | Comprueba que un APK firmado localmente coincida con el certificado público, el identificador y la versión del proyecto. Recibe las salidas de `apksigner` y `aapt`. |
| `.github/workflows/android.yml` | Ejecuta pruebas, Lint y compilaciones en GitHub Actions. Prepara artefactos para firma local y ofrece publicación mediante secretos de firma. |
| `.github/workflows/publish-local.yml` | Al cambiar `.release/Pulso.apk` en `main`, ejecuta comprobaciones, valida su firma y publica APK, metadatos y fuentes en Releases. |
| `.release/Pulso.apk` | APK ya compilado para publicación. Es un binario; los comentarios se añaden al código fuente, no al APK. |
| `SIGNING_CERTIFICATE.txt` | Huella pública de la firma estable para verificar el APK. No contiene la clave privada. |

`update.json` se genera durante la publicación y lo consume el actualizador de la app. Consulta [ACTUALIZACIONES.md](ACTUALIZACIONES.md) para los pasos de firma y publicación.

## Documentación y licencias

| Archivo | Qué explica |
| --- | --- |
| `README.md` | Funciones, instalación, uso, límites y desarrollo de Pulso. |
| `CHANGELOG.md` | Cambios de la versión; se usa en las notas de publicación y en los metadatos de actualización. |
| `VALIDACION.md` | Comprobaciones registradas y alcance de las pruebas. |
| `docs/ACTUALIZACIONES.md` | Migración entre versiones, firma y publicación de actualizaciones. |
| `docs/MAPA_DEL_PROYECTO.md` | Esta guía para entender los archivos y su relación. |
| `LICENSE` | Texto legal de la licencia GPL del proyecto. |
| `third-party/Apache-2.0.txt` | Texto de la licencia Apache 2.0 de dependencias que la usan. |
| `third-party/Rhino-MPL-2.0.txt` | Licencia MPL 2.0 de Rhino. |
| `third-party/jsoup-MIT.txt` | Licencia MIT de jsoup. |
| `third-party/nanojson-license.txt` | Licencia de nanojson. |
| `third-party/protobuf-BSD.txt` | Licencia BSD de Protocol Buffers. |
| `third-party/DEPENDENCY_NOTICES.txt` | Avisos y atribuciones incluidos por las dependencias. |
| `third-party/NewPipeExtractor-sources.jar` | Archivo con fuentes de NewPipeExtractor. Es material externo; su función se documenta aquí. |

Los textos legales y las fuentes de terceros se conservan íntegros. Las explicaciones sobre ellos están en esta guía.

## Dónde quedan tus canciones y preferencias

- Los archivos importados se leen desde la ubicación seleccionada en Android.
- Las descargas se guardan en `files/music/` dentro del almacenamiento privado de Pulso.
- Las portadas se guardan en `files/covers/`.
- El APK de actualización se prepara en `cache/updates/`.
- Biblioteca, pendientes, tema, pestaña, modo aleatorio y opciones de actualización se guardan en preferencias de Android.

Estas rutas son del teléfono; no son carpetas del repositorio. Quitar una entrada de la biblioteca no borra su archivo. Desinstalar la app elimina sus archivos privados.

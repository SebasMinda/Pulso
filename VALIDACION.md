# Validación Pulso 0.18.0

La cabecera conserva BrandView en reposo. Mientras se reproduce, PlaybackHeaderView analiza la sesión de audio de Pulso con Visualizer y dibuja 24 bandas con el color del tema. Al pausar o terminar vuelve la marca. La captura se libera al ocultarse o pasar a segundo plano y se vuelve a crear al cambiar de sesión. Falta de permiso, animaciones desactivadas o efecto no disponible conservan PULSO.

AudioSpectrumTest comprueba silencio, separación de graves y agudos, intensidad, decaimiento, límites, datos inválidos y reinicio. El permiso se ofrece una sola vez y puede recuperarse desde Apariencia. Cancelar la explicación no se trata como un rechazo permanente.

La transición entre marca y barras dura 350 ms, conserva su posición al invertirla y se cancela al ocultarse. Al cambiar de sesión libera la captura sin obligar a mostrar la marca entre pistas listas. Respeta las animaciones desactivadas.

SongDuplicatesTest verifica coincidencias por video y URI, compatibilidad con miniaturas antiguas, tres copias agrupadas, título renombrado, prioridad del enlace sobre la portada y tratamiento de versiones distintas. DownloadPlanTest verifica biblioteca, repeticiones dentro del lote y confirmación de posibles versiones. Se comprueba persistencia del origen y de esa confirmación en TrackTest y DownloadQueueTest.

Detección basada en identificadores y metadatos; no es reconocimiento acústico. La revisión de duplicados requiere confirmación para quitar entradas y conserva sus archivos.

Pendientes: pruebas completas, Lint, compilaciones y firma de esta revisión. Revisión en teléfono pendiente: fundido al reproducir/pausar rápidamente, cambio de pista, biblioteca antigua con tres copias, descargas directas y por lote, revisión de posibles versiones, elección de copia y conservación de archivos.

WaveSeekBar conserva la onda decorativa de avance de 0.16.0; esa onda no mide el audio. Las portadas y la animación original de BrandView no se modifican.

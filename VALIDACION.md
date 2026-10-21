# Validación Pulso 0.17.0

La cabecera conserva BrandView en reposo. Mientras se reproduce, PlaybackHeaderView analiza la sesión de audio de Pulso con Visualizer y dibuja 24 bandas con el color del tema. Al pausar o terminar vuelve la marca. La captura se libera al ocultarse o pasar a segundo plano y se vuelve a crear al cambiar de sesión. Falta de permiso, animaciones desactivadas o efecto no disponible conservan PULSO.

AudioSpectrumTest comprueba silencio, separación de graves y agudos, intensidad, decaimiento, límites, datos inválidos y reinicio. El permiso se ofrece una sola vez y puede recuperarse desde Apariencia. Cancelar la explicación no se trata como un rechazo permanente.

Pendientes al preparar esta revisión: pruebas completas, Lint, compilaciones y verificación de firma. Revisión en un teléfono pendiente: permiso concedido/rechazado, barras siguiendo canciones reales y silencio, pausa/reanudación, cambio de pista, pantalla apagada, vuelta a la app y colores personalizados.

WaveSeekBar conserva la onda decorativa de avance de 0.16.0; esa onda no mide el audio. Las portadas y la animación original de BrandView no se modifican.

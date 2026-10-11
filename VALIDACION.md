# Validación de Pulso 0.7.0

Búsqueda mediante NewPipeExtractor en YouTube normal, filtro videos. Resultados con miniatura, canal y duración; paginación hasta 200 resultados por consulta. Lista de hasta 100 videos con elección de formato, bitrate y pista de audio. La lista guarda preferencias, no direcciones CDN que caducan; se extrae un enlace nuevo al iniciar cada descarga. Una calidad que ya no existe se marca fallida sin cambiarla silenciosamente.

Descarga secuencial con tres conexiones por archivo, integridad validada por AudioDownload. Detener conserva lo terminado y deja pendientes para reiniciar. Una descarga interrumpida al cerrar vuelve a Pendiente al abrir. Las fallidas no detienen la lista y las completadas no se repiten. Edición bloqueada durante descargas y consultas. La búsqueda puede seguir durante el lote.

Pruebas nuevas: persistencia de calidad por canción, recuperación de interrupciones, omisión de completadas, reintento de fallidas, duplicados, instantánea del lote, entradas dañadas, enlaces caducados y rechazo de cambio de calidad o pista.

Pendiente compilación, Lint, firma y prueba real en Android. Las descargas siguen requiriendo mantener la app abierta.

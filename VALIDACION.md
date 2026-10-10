# Validación de Pulso 0.4.0

## Comprobaciones

Release compilada y firmada localmente con la misma identidad y certificado de Pulso 0.2 y 0.3. VersionCode 4. No se agregaron permisos Android.

Diecisiete pruebas automáticas: nueve de descarga HTTP, cinco de actualización y tres de compatibilidad de la biblioteca. Estas últimas comprueban que las canciones antiguas sin portada sigan cargándose, que portada y artista se conserven al guardar y que agregar una imagen no cambie la URI del audio.

Android Lint sin errores. Firma verificada con apksigner.

## Portadas e interfaz

Cuatro pestañas independientes mantienen sus vistas y estado al navegar. Biblioteca muestra portadas pequeñas; Reproductor muestra una imagen grande. Un minirreproductor permite pausar o abrir el reproductor desde las otras pestañas. Descargas y Ajustes conservan sus funciones anteriores.

Las nuevas descargas guardan la dirección de la miniatura de YouTube y el nombre del canal. Las portadas se descargan por separado del audio. Los archivos importados usan su imagen incrustada si Android la puede leer. El menú de cada canción permite elegir una imagen del teléfono, útil también para las descargas anteriores que no guardaron la dirección de su portada.

Las imágenes se cargan fuera del hilo de interfaz, con límite de 6 MB para el archivo de imagen y reducción de resolución. Las miniaturas se reducen adicionalmente para la lista. Se mantiene una caché de memoria de 8 MB y copias JPEG privadas para mostrarlas sin internet. Si una portada no está disponible, se muestra una imagen predeterminada; el audio no depende de que la portada se descargue.

La imagen del video puede no ser la portada oficial del álbum. No se convierte audio a FLAC; se conserva su reproducción.

## Pendiente en el teléfono

No se ejecutó esta interfaz en un teléfono o emulador. La compilación y el análisis estático no sustituyen la prueba visual y de interacción.

1. Instalar sobre 0.3 sin desinstalar y comprobar la biblioteca.
2. Probar las cuatro pestañas y el minirreproductor mientras suena música.
3. Importar un MP3 o FLAC con portada incrustada.
4. Descargar un video y comprobar su miniatura en biblioteca y reproductor.
5. Elegir una portada manual desde ⋮ y comprobarla al reiniciar sin internet.
6. Probar notificación, pantalla bloqueada, descarga al cambiar de pestaña y actualización.
7. Revisar legibilidad de títulos largos, teclado y pantalla horizontal.

El usuario confirmó que las descargas de 0.3 mejoraron en su teléfono. No se hizo un benchmark de velocidad en este entorno.

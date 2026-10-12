# Validación Pulso 0.14.1

La carga de portadas ya no toma el ancho de LayoutParams para generar miniaturas. Ese ancho puede ser cero antes de medir el reproductor, lo que reducía la portada a 64 píxeles y la estiraba al expandirlo. Todas las vistas reciben el mismo bitmap decodificado y ImageView lo dibuja a su tamaño. Se conserva el límite de decodificación, la caché de 8 MiB y las copias locales.

Revisión del flujo de carga inicial, reutilización de caché y carga asíncrona: ambas rutas asignan directamente la portada sin reducción por vista. Pruebas unitarias, Lint y compilaciones de desarrollo y producción completadas en https://github.com/SebasMinda/Pulso/actions/runs/38285755748. APK firmado y certificado estable verificado. Comprobación visual final en un teléfono pendiente.

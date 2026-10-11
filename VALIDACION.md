# Validación de Pulso 0.9.0

Modo aleatorio en PlayerService para botones de app, notificación, pantalla bloqueada y fin de canción. Preferencia conservada. Cada vuelta mezcla las canciones sin repetición inmediata entre vueltas si hay varias. Anterior sigue el historial cuando la posición es menor a tres segundos; después reinicia la canción actual. Cambios de biblioteca recalculan el orden con archivos vigentes.

Reproductor superpuesto sobre el apartado actual, sin pestaña propia. Gesto vertical ascendente en minirreproductor para expandir y descendente en reproductor al inicio del scroll para reducir. La barra de progreso no activa el gesto. Animación corta y alternativas por toque y botón Atrás. Elegir canción conserva Biblioteca.

Seis pruebas de orden aleatorio: cobertura de primera vuelta y vueltas posteriores, historial, cambio de vuelta, bibliotecas vacías o de un archivo y eliminación de canciones.

Compilación Release, 33 pruebas y Android Lint aprobados en GitHub. Firma estable, identidad y versión 0.9.0 verificadas localmente. Pendiente probar gestos, animación y reproducción aleatoria en Android.

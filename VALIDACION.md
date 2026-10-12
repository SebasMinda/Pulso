# Validación Pulso 0.13.0

Gestos horizontales en ambos reproductores. El gesto vertical conserva sus direcciones. Se excluye la barra de posición, se cancela el toque del control antes de navegar y se consume el gesto hasta soltar para evitar cambios repetidos. El gesto a la derecha siempre cambia a la canción anterior, con historial aleatorio.

PlayerGestureTest comprueba ambas vistas, direcciones, movimientos pequeños, diagonales, gestos verticales y umbral según densidad. Las 44 pruebas unitarias, Lint y compilaciones de desarrollo y producción pasaron en https://github.com/SebasMinda/Pulso/actions/runs/38200725439. APK firmado y certificado estable verificado. El servidor de pruebas solo cuenta solicitudes con cabeceras completas para excluir sockets cancelados. Interacción visual en un teléfono pendiente.

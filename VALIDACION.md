# Validación de Pulso 0.2.0

Resultado: compilación de Release correcta, cinco pruebas aprobadas, Android Lint con 0 errores y 6 avisos. Firma del APK verificada con apksigner. El flujo de GitHub Actions se revisó como YAML y shell, pero no pudo ejecutarse porque GitHub rechazó la subida por falta de acceso de escritura.

Las pruebas automáticas comprueban versiones superiores, direcciones limitadas a SebasMinda/Pulso, identidad de aplicación y rechazo de archivos corruptos o incompletos mediante tamaño y SHA-256.

En la primera versión, una consulta real a YouTube devolvió cinco flujos M4A y WebM/Opus. La descarga completa quedó bloqueada por la red del entorno y no se considera exitosa.

No se ejecutó la app en un teléfono ni en un emulador. Instalación, reproducción, importación, exportación y actualización mediante el instalador de Android siguen pendientes de prueba en un dispositivo.

## Prueba en el teléfono

1. Instalar la versión estable junto a 0.1 e importar una canción.
2. Reproducir, pausar, cambiar de canción y bloquear la pantalla.
3. Buscar actualizaciones antes de que exista una Release: debe indicar que no hay una versión publicada.
4. Tras publicar una versión superior con la misma firma, buscarla, aceptar la descarga y confirmar la instalación.
5. Comprobar que la versión cambió y que la biblioteca y sus archivos siguen presentes.
6. Desactivar el aviso al abrir y comprobar que la consulta manual sigue disponible.

La compilación y el análisis estático no sustituyen estas pruebas en un dispositivo.

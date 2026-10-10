# Publicar actualizaciones de Pulso

La app consulta únicamente la última versión publicada en **SebasMinda/Pulso**. Las versiones de prueba y los borradores no se ofrecen al teléfono. GitHub mantiene el código y compila el APK; la actualización no cambia tu música dentro de la app con firma estable.

## Configuración inicial de la firma

Se entrega por separado `Pulso-firma-privada.zip`. Guárdalo en un lugar privado y duradero. Contiene una clave cifrada `pulso.jks`, su contraseña y los dos valores preparados para GitHub. **Nunca lo subas al repositorio público ni lo adjuntes a una Release.**

En el repositorio, abre **Settings → Secrets and variables → Actions → New repository secret** y crea:

| Nombre | Valor |
| --- | --- |
| `PULSO_KEYSTORE_BASE64` | Contenido completo de `keystore-base64.txt` del paquete privado. |
| `PULSO_KEYSTORE_PASSWORD` | Contenido de `signing-password.txt` del paquete privado. |

La clave usa el alias `pulso`. No cambies ni reemplaces estos valores para cada versión. Sin ellos, las pruebas se ejecutan pero la publicación firmada se detiene con un mensaje claro.

## Primera publicación

1. Abre **Actions → Android → Run workflow**.
2. Elige `main` y marca **Publicar APK firmado y activar la actualización**.
3. Espera a que los dos trabajos terminen correctamente.
4. En **Releases** aparecerán `Pulso.apk`, `update.json` y el código fuente de esa versión.

La publicación se prepara como borrador, se adjuntan todos los archivos y luego se hace visible. No reutiliza etiquetas ni reemplaza APKs de una versión publicada.

## Publicaciones posteriores

1. Actualiza `version.properties`: aumenta `versionCode` y cambia `versionName`.
2. Describe los cambios en `CHANGELOG.md`.
3. Sube los cambios a `main`; GitHub ejecuta pruebas, Lint y compilación.
4. Ejecuta el flujo de publicación como arriba, o sube una etiqueta que coincida con `versionName`, por ejemplo `v0.3.0`.

Pulso comprobará al abrir la app como máximo una vez al día. También puedes pulsar **Buscar actualizaciones**. El usuario acepta la descarga y Android confirma la instalación. No hay instalación silenciosa.

## Paso de Pulso 0.1 a la versión estable

La firma privada de Pulso 0.1 no se conservó. Por eso la versión estable usa otra identidad de aplicación y puede instalarse junto a 0.1. No desinstales 0.1 antes de exportar los archivos que quieras conservar:

1. En Pulso 0.1, usa **⋮ → Guardar una copia…** en cada canción descargada.
2. Instala el APK firmado de 0.2.0.
3. En la nueva Pulso, pulsa **+ Importar** y selecciona esas copias y tus otros archivos.

Una vez comprobada la biblioteca nueva, puedes quitar la versión anterior si deseas. Las siguientes versiones usan la misma firma e identidad y actualizan la versión estable conservando sus datos.

## Comprobaciones antes de instalar

Pulso valida el repositorio de origen, el tamaño, SHA-256, identidad de la aplicación, versión y certificado de firma. Rechaza archivos incompletos y APKs de otra aplicación o firma. La firma se conserva; no se admite rotación de claves en esta primera implementación.

# Publicar actualizaciones de Pulso

La app consulta únicamente la última versión publicada en **SebasMinda/Pulso**. Las versiones de prueba y los borradores no se ofrecen al teléfono. GitHub mantiene el código y compila el APK; la actualización no cambia tu música dentro de la app con firma estable.

## Método elegido: firmar las versiones localmente

Las nuevas versiones de Pulso se compilan y firman localmente con la misma clave estable que Pulso 0.2.0. GitHub conserva el código y ejecuta las pruebas; no necesita guardar secretos de firma para este método. La clave privada se mantiene fuera del repositorio.

1. Mantener `applicationId` y la clave estable, y aumentar `versionCode` y `versionName`.
2. Ejecutar las pruebas y Lint.
3. Compilar con `PULSO_KEYSTORE_PATH` y `PULSO_KEYSTORE_PASSWORD` definidos en el entorno privado: `./gradlew :app:assembleRelease`.
4. Verificar el APK con `apksigner verify --print-certs` y comprobar que coincide con `SIGNING_CERTIFICATE.txt`.
5. Nombrar el APK firmado `Pulso.apk` y generar los metadatos: `python3 tools/make_update.py Pulso.apk update.json`.
6. Crear una Release en **SebasMinda/Pulso** con la etiqueta de la versión y adjuntar `Pulso.apk`, `update.json` y el código fuente correspondiente. Publicar cuando estén todos los archivos.

El APK firmado de 0.3.0 ya se generó localmente. Publicar el código en `main` por sí solo no publica la actualización: también deben existir los archivos en una Release. Android pide confirmar la instalación.

Guarda `Pulso-firma-privada.zip` en un lugar privado y duradero. Nunca lo subas al repositorio ni a una Release.

## Alternativa opcional: firma automática en GitHub

Solo si después se decide compilar y firmar en GitHub, configurar una vez estos secretos en **Settings → Secrets and variables → Actions**:

| Nombre | Valor |
| --- | --- |
| `PULSO_KEYSTORE_BASE64` | Contenido de `keystore-base64.txt` del respaldo privado. |
| `PULSO_KEYSTORE_PASSWORD` | Contenido de `signing-password.txt` del respaldo privado. |

La clave usa el alias `pulso`. No cambiar estos valores por versión. Los pasos siguientes de Actions son para esta alternativa; no hacen falta para la firma local elegida.

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

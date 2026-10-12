package com.sebas.pulso;

import org.json.JSONObject;
import java.io.*;
import java.net.URI;
import java.security.MessageDigest;
import java.util.Locale;

/** Valida los datos de una actualización y comprueba el tamaño y la huella SHA-256 del APK. */
public final class UpdateSpec {
    public static final long MAX_BYTES = 100L * 1024 * 1024;
    public final int versionCode;
    public final String versionName, sha256, packageId, changes;
    public final long sizeBytes;

    public UpdateSpec(JSONObject json, String expectedPackage) throws Exception {
        long code = json.getLong("versionCode");
        versionName = json.getString("versionName");
        sha256 = json.getString("sha256").toLowerCase(Locale.ROOT);
        packageId = json.getString("packageId");
        sizeBytes = json.getLong("sizeBytes");
        changes = json.optString("changes", "Mejoras de Pulso.");
        if (code < 1 || code > Integer.MAX_VALUE || versionName.trim().isEmpty()
                || versionName.length() > 80 || !packageId.equals(expectedPackage)
                || !sha256.matches("[0-9a-f]{64}") || sizeBytes < 1024 || sizeBytes > MAX_BYTES) {
            throw new IOException("La información de actualización no es válida.");
        }
        versionCode = (int) code;
    }

    // Compara el número interno de versión con el instalado.
    public boolean newerThan(long installedCode) { return versionCode > installedCode; }

    // Acepta solo enlaces HTTPS de archivos en Releases del repositorio Pulso.
    public static boolean allowedAsset(String url) {
        try {
            URI uri = URI.create(url);
            String path = uri.getPath();
            return "https".equals(uri.getScheme()) && "github.com".equals(uri.getHost())
                    && uri.getUserInfo() == null && uri.getPort() == -1
                    && uri.getQuery() == null && uri.getFragment() == null
                    && path != null && path.startsWith("/SebasMinda/Pulso/releases/download/")
                    && !path.contains("/../") && !path.contains("/./") && !path.contains("\\");
        } catch (RuntimeException e) { return false; }
    }

    // Comprueba que el archivo tenga el tamaño y SHA-256 anunciados.
    public void verify(File file) throws Exception {
        if (file.length() != sizeBytes) throw new IOException("La descarga está incompleta.");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = new FileInputStream(file)) {
            byte[] buffer = new byte[65536];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format(Locale.ROOT, "%02x", value & 255));
        if (!sha256.equals(result.toString())) throw new IOException("El archivo no coincide con la actualización publicada.");
    }
}

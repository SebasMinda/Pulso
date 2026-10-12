package com.sebas.pulso;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import androidx.core.content.FileProvider;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import okhttp3.*;

/** Busca versiones de Pulso en GitHub, verifica el APK y abre el instalador de Android. */
public final class Updater {
    private static final String RELEASES = "https://api.github.com/repos/SebasMinda/Pulso/releases/latest";
    private final Activity host;
    private final Button button;
    private final TextView status;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final SharedPreferences preferences;
    private volatile Call call;
    private volatile boolean closed;
    private boolean busy;
    private UpdateSpec update;
    private String apkUrl;
    private File ready;

    public Updater(Activity activity, Button button, TextView status) {
        host = activity; this.button = button; this.status = status;
        preferences = host.getSharedPreferences("updates", 0);
        button.setOnClickListener(v -> {
            if (busy) return;
            if (ready != null) install();
            else if (update != null) confirmDownload();
            else check(true);
        });
    }

    public boolean automatic() { return preferences.getBoolean("automatic", true); }
    public void automatic(boolean enabled) { preferences.edit().putBoolean("automatic", enabled).apply(); }
    // Consulta al abrir como máximo una vez al día si el aviso automático está activo.
    public void checkOnLaunch() {
        if (automatic() && System.currentTimeMillis() - preferences.getLong("lastCheck", 0) > 86400000L) check(false);
    }

    private void show(Runnable action) { ui.post(() -> { if (!closed && !host.isDestroyed()) action.run(); }); }
    private void state(boolean working, String message, String label) {
        busy = working; status.setText(message); button.setEnabled(!working); button.setText(label);
    }
    private Response request(String url) throws IOException {
        call = Net.CLIENT.newCall(new Request.Builder().url(url)
                .header("User-Agent", "Pulso/" + BuildConfig.VERSION_NAME)
                .header("Accept", "application/vnd.github+json").build());
        return call.execute();
    }
    // Lee los metadatos con un límite de un megabyte.
    private String readJson(Response response) throws IOException {
        if (response.body() == null) throw new IOException("Respuesta vacía.");
        try (InputStream input = response.body().byteStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192]; int read;
            while ((read = input.read(buffer)) != -1) {
                if (output.size() + read > 1048576) throw new IOException("Respuesta demasiado grande.");
                output.write(buffer, 0, read);
            }
            return output.toString("UTF-8");
        }
    }
    private void markChecked() { preferences.edit().putLong("lastCheck", System.currentTimeMillis()).apply(); }

    // Consulta la última publicación estable y comprueba sus archivos y versión.
    public void check(boolean manual) {
        if (busy || closed) return;
        state(true, "Buscando una versión nueva…", "Buscando…");
        worker.execute(() -> {
            try (Response release = request(RELEASES)) {
                if (release.code() == 404) {
                    markChecked(); show(() -> state(false, "Todavía no hay una versión publicada en GitHub.", "Buscar actualizaciones")); return;
                }
                if (!release.isSuccessful()) throw new IOException("GitHub no respondió. Prueba más tarde.");
                JSONObject info = new JSONObject(readJson(release));
                if (info.optBoolean("draft") || info.optBoolean("prerelease")) throw new IOException("La versión aún está en pruebas.");
                JSONArray assets = info.getJSONArray("assets");
                String manifestUrl = null, binaryUrl = null; long binarySize = -1;
                for (int n = 0; n < assets.length(); n++) {
                    JSONObject asset = assets.getJSONObject(n);
                    if (asset.getString("name").equals("update.json")) manifestUrl = asset.getString("browser_download_url");
                    if (asset.getString("name").equals("Pulso.apk")) {
                        binaryUrl = asset.getString("browser_download_url"); binarySize = asset.getLong("size");
                    }
                }
                if (!UpdateSpec.allowedAsset(manifestUrl) || !UpdateSpec.allowedAsset(binaryUrl)) throw new IOException("La publicación no incluye una actualización válida.");
                UpdateSpec spec;
                try (Response manifest = request(manifestUrl)) {
                    if (!manifest.isSuccessful()) throw new IOException("No se pudo consultar la actualización.");
                    spec = new UpdateSpec(new JSONObject(readJson(manifest)), BuildConfig.APPLICATION_ID);
                }
                if (spec.sizeBytes != binarySize) throw new IOException("El tamaño publicado no coincide.");
                markChecked();
                if (!spec.newerThan(BuildConfig.VERSION_CODE)) {
                    show(() -> state(false, "Ya tienes la última versión publicada.", "Buscar actualizaciones")); return;
                }
                final String url = binaryUrl;
                show(() -> {
                    update = spec; apkUrl = url;
                    state(false, "Disponible: Pulso " + spec.versionName, "Actualizar a " + spec.versionName);
                    if (manual) confirmDownload();
                });
            } catch (Exception e) {
                show(() -> state(false, "No se pudo comprobar la actualización. Revisa tu conexión y vuelve a intentar.", "Buscar actualizaciones"));
            } finally { call = null; }
        });
    }

    // Muestra los cambios y solicita la descarga de la actualización.
    private void confirmDownload() {
        String changes = update.changes;
        if (changes.length() > 600) changes = changes.substring(0, 600);
        new AlertDialog.Builder(host).setTitle("Pulso " + update.versionName)
                .setMessage(changes + "\n\nSe descargará la actualización y Android te pedirá confirmar la instalación. Tus canciones se conservan.")
                .setPositiveButton("Descargar", (dialog, which) -> download())
                .setNegativeButton("Ahora no", null).show();
    }

    // Descarga a un archivo temporal y verifica su integridad antes de ofrecer instalarlo.
    private void download() {
        if (busy || closed) return;
        UpdateSpec spec = update;
        state(true, "Descargando actualización… Mantén Pulso abierta.", "Descargando…");
        worker.execute(() -> {
            File partial = null;
            try {
                File directory = new File(host.getCacheDir(), "updates");
                if (!directory.exists() && !directory.mkdirs()) throw new IOException("No hay espacio para guardar.");
                partial = new File(directory, "Pulso.apk.part");
                File target = new File(directory, "Pulso.apk");
                call = Net.CLIENT.newCall(new Request.Builder().url(apkUrl).header("User-Agent", "Pulso").build());
                try (Response response = call.execute()) {
                    if (!response.isSuccessful() || response.body() == null) throw new IOException("No se pudo descargar.");
                    try (InputStream input = response.body().byteStream(); OutputStream output = new FileOutputStream(partial)) {
                        byte[] buffer = new byte[65536]; int read; long received = 0, last = 0;
                        while ((read = input.read(buffer)) != -1) {
                            if (closed) throw new IOException("Cancelado.");
                            received += read;
                            if (received > spec.sizeBytes) throw new IOException("El archivo supera el tamaño publicado.");
                            output.write(buffer, 0, read);
                            long now = System.currentTimeMillis();
                            if (now - last > 500) {
                                last = now; final long progress = received * 100 / spec.sizeBytes;
                                show(() -> status.setText("Descargando actualización: " + progress + " %"));
                            }
                        }
                    }
                }
                spec.verify(partial);
                verifyApk(partial, spec);
                if (closed) throw new IOException("Cancelado.");
                if (target.exists() && !target.delete()) throw new IOException("No se puede reemplazar la copia temporal.");
                if (!partial.renameTo(target)) throw new IOException("No se pudo guardar.");
                show(() -> { ready = target; state(false, "Actualización verificada. Lista para instalar.", "Instalar actualización"); install(); });
            } catch (Exception e) {
                if (partial != null) partial.delete();
                show(() -> state(false, "No se pudo descargar o verificar la actualización. Inténtalo otra vez.", "Reintentar actualización"));
            } finally { call = null; }
        });
    }

    // Comprueba que el APK tenga el paquete, la versión y la firma esperados.
    private void verifyApk(File apk, UpdateSpec spec) throws Exception {
        PackageManager pm = host.getPackageManager();
        int flags = Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo candidate = pm.getPackageArchiveInfo(apk.getAbsolutePath(), flags);
        PackageInfo installed = pm.getPackageInfo(host.getPackageName(), flags);
        if (candidate == null || !candidate.packageName.equals(host.getPackageName())) throw new IOException("El APK no pertenece a Pulso.");
        long code = Build.VERSION.SDK_INT >= 28 ? candidate.getLongVersionCode() : candidate.versionCode;
        if (code != spec.versionCode || !spec.newerThan(BuildConfig.VERSION_CODE)) throw new IOException("La versión del APK no coincide.");
        if (!signers(candidate).equals(signers(installed))) throw new IOException("La firma no coincide con tu Pulso.");
    }
    // Calcula las huellas de los certificados que firmaron el APK.
    private Set<String> signers(PackageInfo info) throws Exception {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= 28) {
            if (info.signingInfo == null) throw new IOException("Sin firma.");
            signatures = info.signingInfo.getApkContentsSigners();
        } else signatures = info.signatures;
        if (signatures == null || signatures.length == 0) throw new IOException("Sin firma.");
        Set<String> result = new HashSet<>();
        for (Signature signature : signatures) result.add(Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())));
        return result;
    }

    // Comprueba el permiso y abre el instalador de Android con acceso al APK.
    private void install() {
        if (ready == null || !ready.isFile()) return;
        if (!host.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(host).setTitle("Permitir actualizar Pulso")
                    .setMessage("Android necesita que permitas a Pulso instalar su actualización. Activa el permiso y vuelve para pulsar Instalar actualización.")
                    .setPositiveButton("Abrir ajustes", (d, w) -> host.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + host.getPackageName()))))
                    .setNegativeButton("Ahora no", null).show();
            return;
        }
        try {
            Uri uri = FileProvider.getUriForFile(host, BuildConfig.APPLICATION_ID + ".files", ready);
            host.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION));
        } catch (Exception e) { status.setText("Android no pudo abrir el instalador. Vuelve a intentarlo."); }
    }
    // Cancela la consulta o descarga y libera sus tareas.
    public void close() {
        closed = true; if (call != null) call.cancel(); worker.shutdownNow(); ui.removeCallbacksAndMessages(null);
    }
}

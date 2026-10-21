package com.sebas.pulso;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

/** Explica el permiso del espectro y permite rechazarlo sin interrumpir la música. */
final class VisualizerPermission {
  static final int REQUEST_CODE = 41;
  private static final String OFFERED = "visualizer.permission.offered";
  private static final String REQUESTED = "visualizer.permission.requested";
  private final Activity activity;
  private final SharedPreferences preferences;
  private AlertDialog dialog;
  private boolean requesting;

  VisualizerPermission(Activity activity, SharedPreferences preferences) {
    this.activity = activity;
    this.preferences = preferences;
  }

  private boolean granted() {
    return activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO)
        == PackageManager.PERMISSION_GRANTED;
  }

  /** Ofrece activarlo una sola vez, justo cuando empieza la primera canción. */
  void offerOnPlayback() {
    if (!granted() && !preferences.getBoolean(OFFERED, false)) request();
  }

  /** También se puede abrir desde Apariencia si antes se rechazó el permiso. */
  void request() {
    if (dialog != null || requesting || activity.isFinishing()) return;
    if (granted()) {
      dialog =
          new AlertDialog.Builder(activity)
              .setTitle("Visualizador activado")
              .setMessage(
                  "Las barras aparecerán en lugar de PULSO mientras se reproduzca una canción.")
              .setPositiveButton("Aceptar", null)
              .create();
    } else {
      boolean openSettings =
          preferences.getBoolean(REQUESTED, false)
              && !activity.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO);
      preferences.edit().putBoolean(OFFERED, true).apply();
      dialog =
          new AlertDialog.Builder(activity)
              .setTitle("Visualizador al ritmo de tu música")
              .setMessage(
                  "Pulso analizará el sonido de su reproductor para mover las barras. Android"
                      + " muestra este acceso como permiso de micrófono. El análisis ocurre en el"
                      + " dispositivo y no guarda audio."
                      + (openSettings
                          ? " Actívalo en Permisos → Micrófono dentro de los ajustes de Android."
                          : ""))
              .setNegativeButton("Ahora no", null)
              .setPositiveButton(
                  openSettings ? "Abrir ajustes" : "Activar",
                  (d, which) -> {
                    if (openSettings) {
                      activity.startActivity(
                          new Intent(
                              Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                              Uri.parse("package:" + activity.getPackageName())));
                    } else {
                      requesting = true;
                      preferences.edit().putBoolean(REQUESTED, true).apply();
                      activity.requestPermissions(
                          new String[] {Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
                    }
                  })
              .create();
    }
    dialog.setOnDismissListener(d -> dialog = null);
    dialog.show();
  }

  void onResult() {
    requesting = false;
  }

  void close() {
    if (dialog != null) dialog.dismiss();
  }
}

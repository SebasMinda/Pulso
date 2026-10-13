package com.sebas.pulso;

import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * Guarda la biblioteca y los pendientes en las preferencias existentes de Pulso.
 *
 * <p>La actividad entrega sus propias preferencias, por lo que se conservan el archivo y las claves
 * de las versiones anteriores. Esta clase no descarga archivos ni modifica el tema. Sus métodos se
 * llaman desde el hilo de la pantalla, que es quien modifica las listas.
 */
public final class LibraryStore {
  private static final String TRACKS_KEY = "tracks";
  private static final String DOWNLOADS_KEY = "downloadQueue";
  private final SharedPreferences preferences;

  public LibraryStore(SharedPreferences preferences) {
    this.preferences = preferences;
  }

  /** Recupera las canciones. Si el JSON está dañado, conserva las que ya pudo leer. */
  public List<Track> loadTracks() {
    List<Track> tracks = new ArrayList<>();
    try {
      JSONArray stored = new JSONArray(preferences.getString(TRACKS_KEY, "[]"));
      for (int index = 0; index < stored.length(); index++) {
        tracks.add(Track.fromJson(stored.getJSONObject(index)));
      }
    } catch (Exception ignored) {
      // Mantiene la recuperación tolerante de las versiones anteriores.
    }
    return tracks;
  }

  /** DownloadQueue decide cómo recuperar estados interrumpidos y entradas antiguas. */
  public void restoreDownloads(DownloadQueue queue) {
    try {
      queue.restore(new JSONArray(preferences.getString(DOWNLOADS_KEY, "[]")));
    } catch (JSONException ignored) {
      // Un listado ilegible no impide abrir la biblioteca.
    }
  }

  public void saveTracks(List<Track> tracks) {
    try {
      preferences.edit().putString(TRACKS_KEY, encodeTracks(tracks).toString()).apply();
    } catch (Exception ignored) {
      // Conserva el comportamiento del guardado anterior ante datos inválidos.
    }
  }

  public void saveDownloads(DownloadQueue queue) {
    try {
      preferences.edit().putString(DOWNLOADS_KEY, queue.json().toString()).apply();
    } catch (JSONException ignored) {
      // No reemplaza la copia anterior si no se puede construir un JSON válido.
    }
  }

  /**
   * Guarda ambos listados en una sola edición al terminar una descarga.
   *
   * <p>Así la canción queda en la biblioteca y sale de pendientes en el mismo guardado.
   */
  public void saveSnapshot(List<Track> tracks, DownloadQueue queue) {
    try {
      preferences
          .edit()
          .putString(TRACKS_KEY, encodeTracks(tracks).toString())
          .putString(DOWNLOADS_KEY, queue.json().toString())
          .apply();
    } catch (JSONException ignored) {
      // La edición solo se aplica cuando ambas listas se pudieron convertir.
    }
  }

  private JSONArray encodeTracks(List<Track> tracks) throws JSONException {
    JSONArray result = new JSONArray();
    for (Track track : tracks) result.put(track.json());
    return result;
  }
}

package com.sebas.pulso;

import java.util.*;
import org.json.*;

/**
 * Organiza las descargas pendientes. La pantalla modifica la lista y los trabajadores usan copias.
 */
public final class DownloadQueue {
  public static final String PENDING = "pending",
      RUNNING = "running",
      DONE = "done",
      FAILED = "failed";
  public static final int LIMIT = 100;

  public static final class Entry {
    public final String url, title, cover, artist;
    public AudioChoice quality;
    public String state = PENDING, error = "";
    // Solo se activa cuando el usuario confirma una posible versión con el mismo nombre.
    public boolean allowSimilar;

    public Entry(String url, String title, String cover, String artist, AudioChoice quality) {
      this.url = url;
      this.title = title;
      this.cover = cover;
      this.artist = artist;
      this.quality = quality;
    }

    // Copia una entrada para que una tarea trabaje con datos estables.
    public Entry snapshot() {
      Entry e = new Entry(url, title, cover, artist, quality);
      e.state = state;
      e.error = error;
      e.allowSimilar = allowSimilar;
      return e;
    }

    public JSONObject json() throws JSONException {
      return new JSONObject()
          .put("url", url)
          .put("title", title)
          .put("cover", cover)
          .put("artist", artist)
          .put("quality", quality.json())
          .put("state", state)
          .put("error", error)
          .put("allowSimilar", allowSimilar);
    }
  }

  public final List<Entry> entries = new ArrayList<>();

  // Añade un video si no estaba en la lista y respeta el límite de 100.
  public boolean add(Entry e) {
    for (Entry old : entries)
      if (old.url.equals(e.url) || SongDuplicates.sameVideo(old.url, e.url)) return false;
    if (entries.size() >= LIMIT)
      throw new IllegalStateException("La lista admite hasta 100 canciones.");
    entries.add(e);
    return true;
  }

  // Devuelve copias de entradas sin terminar, incluidas las fallidas para reintentar.
  public List<Entry> pending() {
    List<Entry> result = new ArrayList<>();
    for (Entry e : entries) if (!DONE.equals(e.state)) result.add(e.snapshot());
    return result;
  }

  // Actualiza el estado y el error de un video.
  public void update(String url, String state, String error) {
    for (Entry e : entries)
      if (e.url.equals(url)) {
        e.state = state;
        e.error = error;
        break;
      }
  }

  // Quita de pendientes únicamente la canción descargada correctamente.
  public void complete(String url) {
    entries.removeIf(e -> e.url.equals(url));
  }

  public JSONArray json() throws JSONException {
    JSONArray a = new JSONArray();
    for (Entry e : entries) a.put(e.json());
    return a;
  }

  // Recupera la lista, omite datos dañados y devuelve las interrumpidas a pendientes.
  public void restore(JSONArray a) {
    entries.clear();
    for (int i = 0; i < Math.min(a.length(), LIMIT); i++)
      try {
        JSONObject o = a.getJSONObject(i);
        Entry e =
            new Entry(
                o.getString("url"),
                o.getString("title"),
                o.optString("cover", ""),
                o.optString("artist", ""),
                AudioChoice.fromJson(o.getJSONObject("quality")));
        e.state = o.optString("state", PENDING);
        e.error = o.optString("error", "");
        e.allowSimilar = o.optBoolean("allowSimilar", false);
        if (DONE.equals(e.state)) continue;
        if (RUNNING.equals(e.state)) {
          e.state = PENDING;
          e.error = "";
        }
        add(e);
      } catch (JSONException | IllegalStateException ignored) {
      }
  }
}

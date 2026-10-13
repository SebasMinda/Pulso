package com.sebas.pulso;

import org.json.*;

/** Representa una canción: título, ubicación del audio, portada y artista. */
public class Track {
  public final String title, uri;
  public String cover, artist;

  public Track(String title, String uri) {
    this(title, uri, "", "");
  }

  public Track(String title, String uri, String cover, String artist) {
    this.title = title;
    this.uri = uri;
    this.cover = cover == null ? "" : cover;
    this.artist = artist == null ? "" : artist;
  }

  // Carga una canción y acepta bibliotecas antiguas sin portada ni artista.
  public static Track fromJson(JSONObject o) throws JSONException {
    return new Track(
        o.getString("title"),
        o.getString("uri"),
        o.optString("cover", ""),
        o.optString("artist", ""));
  }

  // Convierte la canción a JSON para guardar la biblioteca.
  public JSONObject json() throws JSONException {
    return new JSONObject()
        .put("title", title)
        .put("uri", uri)
        .put("cover", cover)
        .put("artist", artist);
  }
}

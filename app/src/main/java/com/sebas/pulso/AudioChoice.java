package com.sebas.pulso;

import java.io.IOException;
import java.util.*;
import org.json.*;
import org.schabi.newpipe.extractor.stream.AudioStream;

/** Guarda la calidad elegida, sin guardar la dirección temporal de descarga que puede caducar. */
public final class AudioChoice {
  public final String id, suffix, trackId, label;
  public final int bitrate;

  private static String safe(String s) {
    return s == null ? "" : s;
  }

  public AudioChoice(String id, String suffix, String trackId, int bitrate, String label) {
    this.id = safe(id);
    this.suffix = suffix;
    this.trackId = safe(trackId);
    this.bitrate = bitrate;
    this.label = label;
  }

  // Extrae los datos del formato y prepara el texto que verá el usuario.
  public static AudioChoice of(AudioStream s) {
    String label =
        s.getFormat().getName()
            + " · "
            + (s.getAverageBitrate() > 0 ? s.getAverageBitrate() + " kb/s" : "tasa no indicada");
    if (s.getAudioTrackName() != null && !s.getAudioTrackName().isEmpty())
      label += " · " + s.getAudioTrackName();
    return new AudioChoice(
        s.getId(), s.getFormat().getSuffix(), s.getAudioTrackId(), s.getAverageBitrate(), label);
  }

  // Comprueba formato, pista de audio y bitrate con la elección guardada.
  public boolean matches(AudioStream s) {
    return s.getFormat() != null
        && id.equals(safe(s.getId()))
        && suffix.equals(s.getFormat().getSuffix())
        && trackId.equals(safe(s.getAudioTrackId()))
        && bitrate == s.getAverageBitrate();
  }

  // Busca una dirección nueva para la misma calidad; avisa si dejó de estar disponible.
  public AudioStream resolve(List<AudioStream> streams) throws IOException {
    for (AudioStream s : streams) if (matches(s)) return s;
    throw new IOException(
        "La calidad elegida ya no está disponible. Pulsa Calidad para elegir otra.");
  }

  // Convierte la calidad a JSON para guardarla.
  public JSONObject json() throws JSONException {
    return new JSONObject()
        .put("id", id)
        .put("suffix", suffix)
        .put("trackId", trackId)
        .put("bitrate", bitrate)
        .put("label", label);
  }

  // Recupera la calidad guardada.
  public static AudioChoice fromJson(JSONObject o) throws JSONException {
    return new AudioChoice(
        o.getString("id"),
        o.getString("suffix"),
        o.optString("trackId", ""),
        o.getInt("bitrate"),
        o.getString("label"));
  }

  // Filtra M4A y WebM descargables, elimina duplicados y ordena por bitrate.
  public static List<AudioStream> available(List<AudioStream> input) {
    List<AudioStream> out = new ArrayList<>();
    Set<String> seen = new HashSet<>();
    for (AudioStream s : input)
      if (s.isUrl()
          && s.getFormat() != null
          && (s.getFormat().getSuffix().equals("m4a")
              || s.getFormat().getSuffix().equals("webm"))) {
        AudioChoice c = of(s);
        String key = c.id + "\n" + c.suffix + "\n" + c.trackId + "\n" + c.bitrate;
        if (seen.add(key)) out.add(s);
      }
    out.sort((a, b) -> Integer.compare(b.getAverageBitrate(), a.getAverageBitrate()));
    return out;
  }
}

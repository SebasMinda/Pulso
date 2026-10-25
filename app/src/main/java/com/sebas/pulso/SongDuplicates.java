package com.sebas.pulso;

import java.net.URI;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reconoce archivos o videos repetidos y separa las coincidencias de nombre y artista. */
public final class SongDuplicates {
  private static final Pattern VIDEO_QUERY = Pattern.compile("(?:^|&)v=([A-Za-z0-9_-]{11})(?:&|$)");
  private static final Pattern THUMBNAIL = Pattern.compile("^/(?:vi|vi_webp)/([A-Za-z0-9_-]{11})/");
  private static final Pattern MARKS = Pattern.compile("\\p{M}+");
  private static final Pattern PUNCTUATION = Pattern.compile("[^\\p{L}\\p{N}]+");

  public static final class Match {
    public final Track track;
    public final boolean exact;

    private Match(Track track, boolean exact) {
      this.track = track;
      this.exact = exact;
    }
  }

  public static final class Group {
    public final List<Track> tracks = new ArrayList<>();
    public final boolean exact;

    private Group(List<Track> tracks, boolean exact) {
      this.tracks.addAll(tracks);
      this.exact = exact;
    }
  }

  private SongDuplicates() {}

  /** La identidad del video es más fiable que su título o el nombre del archivo descargado. */
  public static String videoId(String address) {
    if (address == null || address.isEmpty()) return "";
    try {
      URI uri = URI.create(address);
      String host = uri.getHost();
      if (host == null) return "";
      host = host.toLowerCase(Locale.ROOT);
      String path = uri.getPath() == null ? "" : uri.getPath();
      if (host.equals("youtu.be")) return validId(path.substring(path.lastIndexOf('/') + 1));
      if (host.equals("youtube.com")
          || host.equals("www.youtube.com")
          || host.equals("m.youtube.com")) {
        Matcher query = VIDEO_QUERY.matcher(uri.getRawQuery() == null ? "" : uri.getRawQuery());
        if (query.find()) return query.group(1);
        String[] parts = path.split("/");
        if (parts.length == 3
            && (parts[1].equals("shorts") || parts[1].equals("live") || parts[1].equals("embed"))) {
          return validId(parts[2]);
        }
      }
      if (host.equals("ytimg.com") || host.endsWith(".ytimg.com")) {
        Matcher image = THUMBNAIL.matcher(path);
        if (image.find()) return image.group(1);
      }
    } catch (IllegalArgumentException ignored) {
      // Los archivos locales y las portadas elegidas por el usuario pueden no tener un video.
    }
    return "";
  }

  private static String validId(String value) {
    return value.matches("[A-Za-z0-9_-]{11}") ? value : "";
  }

  private static String source(Track track) {
    // Las bibliotecas anteriores no guardaban sourceUrl, pero sí la miniatura original de YouTube.
    String id = videoId(track.sourceUrl);
    return id.isEmpty() ? videoId(track.cover) : id;
  }

  /** Conserva palabras como live, remix y remastered: pueden indicar versiones distintas. */
  private static String normalized(String value) {
    if (value == null) return "";
    String plain = Normalizer.normalize(value, Normalizer.Form.NFD);
    return PUNCTUATION
        .matcher(MARKS.matcher(plain).replaceAll("").toLowerCase(Locale.ROOT))
        .replaceAll(" ")
        .trim();
  }

  private static String metadata(Track track) {
    String title = normalized(track.title), artist = normalized(track.artist);
    return title.isEmpty() || artist.isEmpty() ? "" : title + "\n" + artist;
  }

  public static Track candidate(DownloadQueue.Entry entry) {
    return new Track(entry.title, "", entry.cover, entry.artist, entry.url);
  }

  public static boolean sameVideo(String first, String second) {
    String id = videoId(first);
    return !id.isEmpty() && id.equals(videoId(second));
  }

  /** Busca primero una coincidencia segura, aunque antes aparezca una de nombre y artista. */
  public static Match find(List<Track> library, Track candidate) {
    String id = source(candidate), name = metadata(candidate);
    Match possible = null;
    for (Track old : library) {
      if ((!candidate.uri.isEmpty() && candidate.uri.equals(old.uri))
          || (!id.isEmpty() && id.equals(source(old)))) return new Match(old, true);
      if (possible == null && !name.isEmpty() && name.equals(metadata(old))) {
        possible = new Match(old, false);
      }
    }
    return possible;
  }

  /** Agrupa en tiempo lineal mediante claves; una canción solo aparece en un grupo. */
  public static List<Group> groups(List<Track> library) {
    int size = library.size();
    int[] parents = new int[size];
    Map<String, Integer> seen = new HashMap<>();
    for (int index = 0; index < size; index++) {
      parents[index] = index;
      Track track = library.get(index);
      String[] values = {track.uri, source(track), metadata(track)};
      for (int kind = 0; kind < values.length; kind++) {
        if (values[kind].isEmpty()) continue;
        Integer before = seen.putIfAbsent(kind + ":" + values[kind], index);
        if (before != null) parents[root(parents, index)] = root(parents, before);
      }
    }
    Map<Integer, List<Track>> joined = new LinkedHashMap<>();
    for (int index = 0; index < size; index++) {
      joined
          .computeIfAbsent(root(parents, index), ignored -> new ArrayList<>())
          .add(library.get(index));
    }
    List<Group> result = new ArrayList<>();
    for (List<Track> group : joined.values()) {
      if (group.size() < 2) continue;
      Track first = group.get(0);
      boolean exact = true;
      for (int index = 1; index < group.size(); index++) {
        Match match = find(Collections.singletonList(first), group.get(index));
        if (match == null || !match.exact) exact = false;
      }
      result.add(new Group(group, exact));
    }
    return result;
  }

  private static int root(int[] parents, int index) {
    while (parents[index] != index) {
      parents[index] = parents[parents[index]];
      index = parents[index];
    }
    return index;
  }
}

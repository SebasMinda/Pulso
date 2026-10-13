package com.sebas.pulso;

import static org.junit.Assert.*;

import android.content.SharedPreferences;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** Comprueba que la separación del guardado conserve los datos de las versiones anteriores. */
public class LibraryStoreTest {
  @Test
  public void readsLibraryAlreadySavedByEarlierVersions() {
    MemoryPreferences memory = new MemoryPreferences();
    memory.values.put(
        "tracks", "[{\"title\":\"Canción antigua\",\"uri\":\"file:/music/old.m4a\"}]");
    List<Track> restored = new LibraryStore(memory.preferences).loadTracks();
    assertEquals(1, restored.size());
    assertEquals("Canción antigua", restored.get(0).title);
    assertEquals("file:/music/old.m4a", restored.get(0).uri);
    assertEquals("", restored.get(0).cover);
  }

  @Test
  public void savesLibraryAndPendingQueueTogetherWithoutTouchingAppearance() {
    MemoryPreferences memory = new MemoryPreferences();
    memory.values.put("theme.accent", 0xffc9ad72);
    LibraryStore store = new LibraryStore(memory.preferences);
    AudioChoice quality = new AudioChoice("140", "m4a", "original", 128, "M4A · 128 kb/s");
    DownloadQueue queue = new DownloadQueue();
    queue.add(
        new DownloadQueue.Entry(
            "https://video/next", "Pendiente", "cover-next", "Artista", quality));
    store.saveSnapshot(
        Arrays.asList(new Track("Descargada", "file:/music/song.m4a", "cover-song", "Artista")),
        queue);

    // Simula abrir de nuevo la app: otra instancia lee las mismas claves de preferencias.
    LibraryStore reopened = new LibraryStore(memory.preferences);
    DownloadQueue restoredQueue = new DownloadQueue();
    reopened.restoreDownloads(restoredQueue);
    assertEquals(1, memory.editsApplied);
    assertEquals("Descargada", reopened.loadTracks().get(0).title);
    assertEquals("cover-song", reopened.loadTracks().get(0).cover);
    assertEquals(1, restoredQueue.pending().size());
    assertEquals(128, restoredQueue.pending().get(0).quality.bitrate);
    assertEquals("original", restoredQueue.pending().get(0).quality.trackId);
    assertEquals(0xffc9ad72, memory.values.get("theme.accent"));
  }

  /** Sustituye solo las operaciones de preferencias usadas por LibraryStore, sin emulador. */
  private static final class MemoryPreferences {
    final Map<String, Object> values = new HashMap<>();
    int editsApplied;
    final SharedPreferences preferences =
        (SharedPreferences)
            Proxy.newProxyInstance(
                SharedPreferences.class.getClassLoader(),
                new Class<?>[] {SharedPreferences.class},
                (proxy, method, arguments) -> {
                  if (method.getName().equals("getString")) {
                    return values.getOrDefault((String) arguments[0], arguments[1]);
                  }
                  if (method.getName().equals("edit")) return editor();
                  throw new AssertionError("Operación no esperada: " + method.getName());
                });

    private SharedPreferences.Editor editor() {
      Map<String, Object> pending = new HashMap<>();
      return (SharedPreferences.Editor)
          Proxy.newProxyInstance(
              SharedPreferences.Editor.class.getClassLoader(),
              new Class<?>[] {SharedPreferences.Editor.class},
              (proxy, method, arguments) -> {
                if (method.getName().equals("putString")) {
                  pending.put((String) arguments[0], arguments[1]);
                  return proxy;
                }
                if (method.getName().equals("apply")) {
                  values.putAll(pending);
                  editsApplied++;
                  return null;
                }
                throw new AssertionError("Operación no esperada: " + method.getName());
              });
    }
  }
}

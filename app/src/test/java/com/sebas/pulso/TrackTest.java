package com.sebas.pulso;

import static org.junit.Assert.*;

import org.json.JSONObject;
import org.junit.Test;

/** Comprueba que la biblioteca conserve los datos y acepte canciones de versiones antiguas. */
public class TrackTest {
  @Test
  public void oldLibraryLoadsWithoutLosingSongs() throws Exception {
    Track t =
        Track.fromJson(
            new JSONObject("{\"title\":\"Mi canción\",\"uri\":\"file:///music/song.m4a\"}"));
    assertEquals("Mi canción", t.title);
    assertEquals("file:///music/song.m4a", t.uri);
    assertEquals("", t.cover);
    assertEquals("", t.artist);
  }

  @Test
  public void coverAndArtistSurviveLibrarySave() throws Exception {
    Track t = new Track("Tema", "content://music/42", "content://images/3", "Artista");
    Track loaded = Track.fromJson(new JSONObject(t.json().toString()));
    assertEquals(t.uri, loaded.uri);
    assertEquals(t.title, loaded.title);
    assertEquals(t.cover, loaded.cover);
    assertEquals(t.artist, loaded.artist);
  }

  @Test
  public void oldLibraryRemainsCompatibleAfterAddingCover() throws Exception {
    Track t =
        Track.fromJson(new JSONObject().put("title", "FLAC local").put("uri", "content://music/5"));
    t.cover = "https://i.ytimg.com/vi/example/hqdefault.jpg";
    Track loaded = Track.fromJson(t.json());
    assertEquals("content://music/5", loaded.uri);
    assertEquals(t.cover, loaded.cover);
  }
}

package com.sebas.pulso;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** Prueba tanto la biblioteca antigua como las versiones distintas que no deben bloquearse. */
public class SongDuplicatesTest {
  private static final String FIRST = "abcdefghijk", SECOND = "ABCDEFGHIJK";

  private Track downloaded(String title, String artist, String id, String file) {
    return new Track(
        title, "file:///music/" + file, "", artist, "https://www.youtube.com/watch?v=" + id);
  }

  @Test
  public void sameVideoIsRecognizedAcrossLinksAndRenamedTitles() {
    Track old = downloaded("Nombre antiguo", "Canal", FIRST, "old.m4a");
    Track candidate =
        new Track("Otro título", "", "", "Otro canal", "https://youtu.be/" + FIRST + "?t=8");
    assertTrue(SongDuplicates.find(Collections.singletonList(old), candidate).exact);
    assertEquals(FIRST, SongDuplicates.videoId("https://m.youtube.com/shorts/" + FIRST));
    assertEquals(FIRST, SongDuplicates.videoId("https://www.youtube.com/live/" + FIRST));
    assertEquals(
        FIRST,
        SongDuplicates.videoId("https://www.youtube.com/watch?list=abc&v=" + FIRST + "&t=1"));
  }

  @Test
  public void oldDownloadsAreRecognizedFromTheirOriginalThumbnail() {
    Track old =
        new Track(
            "Canción",
            "file:///old.m4a",
            "https://i.ytimg.com/vi_webp/" + FIRST + "/maxresdefault.webp",
            "Artista");
    SongDuplicates.Match match =
        SongDuplicates.find(
            Collections.singletonList(old), downloaded("Canción nueva", "Canal", FIRST, "new.m4a"));
    assertNotNull(match);
    assertTrue(match.exact);
  }

  @Test
  public void explicitSourceTakesPrecedenceOverChosenCover() {
    Track old =
        new Track(
            "Canción",
            "file:///old.m4a",
            "https://i.ytimg.com/vi/" + SECOND + "/hqdefault.jpg",
            "Artista",
            "https://youtu.be/" + FIRST);
    assertNull(
        SongDuplicates.find(
            Collections.singletonList(old), downloaded("Otra", "Otro", SECOND, "new.m4a")));
  }

  @Test
  public void exactMatchWinsOverAnEarlierNameMatch() {
    Track possible = downloaded("Canción", "Artista", SECOND, "1.m4a");
    Track exact = downloaded("Otro título", "Otro", FIRST, "2.m4a");
    SongDuplicates.Match found =
        SongDuplicates.find(
            Arrays.asList(possible, exact), downloaded("Canción", "Artista", FIRST, "3.m4a"));
    assertTrue(found.exact);
    assertSame(exact, found.track);
  }

  @Test
  public void nameAndArtistAreOnlyAPossibleMatch() {
    Track old = downloaded("  Canción — Azul ", "Ártista", FIRST, "old.m4a");
    SongDuplicates.Match match =
        SongDuplicates.find(
            Collections.singletonList(old),
            downloaded("cancion azul", "artista", SECOND, "new.m4a"));
    assertNotNull(match);
    assertFalse(match.exact);
  }

  @Test
  public void differentArtistsAndNamedVersionsRemainSeparate() {
    Track old = downloaded("Canción", "Artista", FIRST, "1.m4a");
    List<Track> library = Collections.singletonList(old);
    assertNull(
        SongDuplicates.find(library, downloaded("Canción live", "Artista", SECOND, "2.m4a")));
    assertNull(
        SongDuplicates.find(library, downloaded("Canción remix", "Artista", SECOND, "2.m4a")));
    assertNull(
        SongDuplicates.find(library, downloaded("Canción", "Otro artista", SECOND, "2.m4a")));
    assertNull(
        SongDuplicates.find(
            Collections.singletonList(new Track("Intro", "file:///a")),
            new Track("Intro", "file:///b")));
  }

  @Test
  public void invalidOrLookalikeHostsCannotCreateAVideoMatch() {
    for (String url :
        Arrays.asList(
            "bad address",
            "",
            "https://youtube.com.evil.test/watch?v=" + FIRST,
            "https://notytimg.com/vi/" + FIRST + "/a.jpg",
            "https://youtube.com/watch?v=short")) {
      assertEquals("", SongDuplicates.videoId(url));
    }
  }

  @Test
  public void repeatedFileIsExactEvenWithoutArtistOrVideo() {
    Track old = new Track("A", "content://music/42");
    assertTrue(SongDuplicates.find(Collections.singletonList(old), new Track("B", old.uri)).exact);
  }

  @Test
  public void threeOldCopiesAppearInASingleGroup() {
    List<Track> library = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      library.add(
          new Track(
              "Canción",
              "file:///song-" + i + ".m4a",
              "https://i.ytimg.com/vi/" + FIRST + "/hqdefault.jpg",
              "Artista"));
    }
    library.add(downloaded("Otra", "Otro", SECOND, "other.m4a"));
    List<SongDuplicates.Group> groups = SongDuplicates.groups(library);
    assertEquals(1, groups.size());
    assertEquals(3, groups.get(0).tracks.size());
    assertTrue(groups.get(0).exact);
    assertEquals(4, library.size());
  }

  @Test
  public void overlappingNameAndSourceMatchesDoNotRepeatGroups() {
    Track a = downloaded("Tema", "Artista", FIRST, "1.m4a");
    Track b = downloaded("Renombrado", "Artista", FIRST, "2.m4a");
    Track c = downloaded("Renombrado", "Artista", SECOND, "3.m4a");
    List<SongDuplicates.Group> groups = SongDuplicates.groups(Arrays.asList(a, b, c));
    assertEquals(1, groups.size());
    assertEquals(3, groups.get(0).tracks.size());
    assertFalse(groups.get(0).exact);
  }
}

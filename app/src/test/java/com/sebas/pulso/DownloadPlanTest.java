package com.sebas.pulso;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** Evita copias entre descargas individuales, lotes y canciones guardadas anteriormente. */
public class DownloadPlanTest {
  private DownloadQueue.Entry entry(String id, String title) {
    return new DownloadQueue.Entry(
        "https://www.youtube.com/watch?v=" + id,
        title,
        "",
        "Artista",
        new AudioChoice("140", "m4a", "", 128, "M4A"));
  }

  @Test
  public void alreadyDownloadedVideoIsSkippedEvenWhenTitleChanged() {
    DownloadQueue.Entry entry = entry("abcdefghijk", "Título nuevo");
    Track saved =
        new Track(
            "Título viejo",
            "file:///old.m4a",
            "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg",
            "Artista");
    DownloadPlan plan =
        new DownloadPlan(Collections.singletonList(entry), Collections.singletonList(saved));
    assertEquals(Collections.singletonList(entry), plan.existing);
    assertTrue(plan.similar.isEmpty());
  }

  @Test
  public void sameVideoInsideOneBatchIsSkipped() {
    DownloadQueue.Entry first = entry("abcdefghijk", "Canción");
    DownloadQueue.Entry second = entry("abcdefghijk", "Renombrada");
    DownloadPlan plan = new DownloadPlan(Arrays.asList(first, second), Collections.emptyList());
    assertEquals(Collections.singletonList(second), plan.existing);
  }

  @Test
  public void differentVideosWithSameMetadataNeedAChoice() {
    DownloadQueue.Entry first = entry("abcdefghijk", "Canción");
    DownloadQueue.Entry second = entry("ABCDEFGHIJK", "Canción");
    DownloadPlan plan = new DownloadPlan(Arrays.asList(first, second), Collections.emptyList());
    assertTrue(plan.existing.isEmpty());
    assertEquals(Collections.singletonList(second), plan.similar);
    assertFalse(second.allowSimilar);
    assertEquals(DownloadQueue.PENDING, first.state);
  }

  @Test
  public void confirmedDifferentVersionIsAllowedButSameVideoStillBlocked() {
    DownloadQueue.Entry first = entry("abcdefghijk", "Canción");
    DownloadQueue.Entry second = entry("ABCDEFGHIJK", "Canción");
    second.allowSimilar = true;
    Track saved = SongDuplicates.candidate(first);
    DownloadPlan plan =
        new DownloadPlan(Arrays.asList(first, second), Collections.singletonList(saved));
    assertEquals(Collections.singletonList(first), plan.existing);
    assertTrue(plan.similar.isEmpty());
  }

  @Test
  public void liveAndRemixAreNotSkipped() {
    DownloadQueue.Entry first = entry("abcdefghijk", "Canción");
    DownloadQueue.Entry second = entry("ABCDEFGHIJK", "Canción live");
    DownloadPlan plan =
        new DownloadPlan(
            Collections.singletonList(second),
            Collections.singletonList(SongDuplicates.candidate(first)));
    assertTrue(plan.existing.isEmpty());
    assertTrue(plan.similar.isEmpty());
  }
}

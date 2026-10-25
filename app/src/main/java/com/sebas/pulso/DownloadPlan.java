package com.sebas.pulso;

import java.util.ArrayList;
import java.util.List;

/** Revisa el lote antes de gastar datos. No modifica la biblioteca ni el listado guardado. */
final class DownloadPlan {
  final List<DownloadQueue.Entry> existing = new ArrayList<>();
  final List<DownloadQueue.Entry> similar = new ArrayList<>();

  DownloadPlan(List<DownloadQueue.Entry> batch, List<Track> library) {
    List<Track> known = new ArrayList<>(library);
    for (DownloadQueue.Entry entry : batch) {
      Track candidate = SongDuplicates.candidate(entry);
      SongDuplicates.Match match = SongDuplicates.find(known, candidate);
      if (match != null && match.exact) existing.add(entry);
      else if (match != null && !entry.allowSimilar) similar.add(entry);
      known.add(candidate);
    }
  }
}

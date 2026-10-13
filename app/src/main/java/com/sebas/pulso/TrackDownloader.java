package com.sebas.pulso;

import java.io.File;
import java.io.IOException;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.StreamInfo;

/**
 * Descarga una canción y devuelve sus datos cuando el archivo está completo.
 *
 * <p>MainActivity controla el listado y la pantalla. Esta clase resuelve el enlace temporal, crea
 * el archivo y usa AudioDownload para copiar sus bytes. download se ejecuta en un hilo de trabajo;
 * cancel puede llamarse desde el hilo de la pantalla.
 */
public final class TrackDownloader {
  /** El tiempo inicial permite calcular la velocidad sin incluir la consulta del video. */
  public interface Progress {
    void update(long bytes, long total, long startedAtNanos);
  }

  private final File musicDirectory;
  private volatile AudioDownload activeDownload;
  private volatile boolean cancelled;

  public TrackDownloader(File filesDirectory) {
    musicDirectory = new File(filesDirectory, "music");
  }

  /** Prepara un nuevo lote. Se llama cuando el lote anterior ya terminó. */
  public void resetCancellation() {
    cancelled = false;
  }

  /** Marca el lote como detenido y cancela la copia de audio que esté en marcha. */
  public void cancel() {
    cancelled = true;
    AudioDownload current = activeDownload;
    if (current != null) current.cancel();
  }

  private void checkCancellation() throws IOException {
    if (cancelled) throw new IOException("Cancelado");
  }

  /**
   * Devuelve un Track listo para añadir a la biblioteca; nunca modifica el listado de descargas.
   *
   * <p>La dirección del audio se consulta de nuevo porque los enlaces del servidor caducan. La
   * calidad elegida se mantiene. El archivo .part solo se renombra cuando la descarga terminó
   * correctamente.
   */
  public Track download(DownloadQueue.Entry entry, Progress progress) throws Exception {
    File partial = null;
    try {
      StreamInfo info = StreamInfo.getInfo(ServiceList.YouTube, entry.url);
      checkCancellation();
      AudioStream audio = entry.quality.resolve(AudioChoice.available(info.getAudioStreams()));
      File destination = destination(entry.title, audio.getFormat().getSuffix());
      partial = new File(destination + ".part");
      AudioDownload task = new AudioDownload(Net.CLIENT);
      activeDownload = task;
      // Cubre la carrera en la que se pulsó Detener justo antes de asignar la tarea.
      if (cancelled) task.cancel();
      long startedAtNanos = System.nanoTime();
      task.download(
          audio.getContent(),
          partial,
          (bytes, total) -> progress.update(bytes, total, startedAtNanos));
      checkCancellation();
      if (!partial.renameTo(destination)) throw new IOException("No se pudo guardar.");
      return new Track(entry.title, destination.toURI().toString(), entry.cover, entry.artist);
    } catch (Exception failure) {
      if (partial != null) partial.delete();
      throw failure;
    } finally {
      activeDownload = null;
    }
  }

  /** Forma un nombre seguro y conserva la extensión original del audio. */
  private File destination(String title, String suffix) throws IOException {
    if (!musicDirectory.exists() && !musicDirectory.mkdirs()) {
      throw new IOException("No se pudo abrir la carpeta de música.");
    }
    String filename = title.replaceAll("[^\\p{L}\\p{N} ._-]", "_");
    if (filename.length() > 90) filename = filename.substring(0, 90);
    return new File(musicDirectory, filename + "-" + System.currentTimeMillis() + "." + suffix);
  }
}

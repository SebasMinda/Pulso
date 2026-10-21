package com.sebas.pulso;

import java.util.Arrays;

/** Convierte las frecuencias reales de una canción en alturas suaves para las barras. */
final class AudioSpectrum {
  static final int BAND_COUNT = 24;
  private final float[] levels = new float[BAND_COUNT];

  float level(int band) {
    return levels[band];
  }

  void clear() {
    Arrays.fill(levels, 0);
  }

  /** Recibe la FFT de Android y su frecuencia de muestreo en hercios, no milihercios. */
  void update(byte[] fft, int sampleRateHz) {
    if (fft == null || fft.length < 4 || fft.length % 2 != 0 || sampleRateHz <= 0) {
      clear();
      return;
    }
    int lastBin = fft.length / 2 - 1;
    double binHz = (double) sampleRateHz / fft.length;
    double lowHz = Math.max(40, binHz);
    double highHz = Math.max(lowHz, Math.min(16000, lastBin * binHz));
    double ratio = highHz / lowHz;
    for (int band = 0; band < BAND_COUNT; band++) {
      // La escala logarítmica deja espacio para graves, voces y agudos.
      double fromHz = lowHz * Math.pow(ratio, band / (double) BAND_COUNT);
      double toHz = lowHz * Math.pow(ratio, (band + 1) / (double) BAND_COUNT);
      int from = Math.max(1, Math.min(lastBin, (int) Math.floor(fromHz / binHz)));
      int to = Math.max(from + 1, Math.min(lastBin + 1, (int) Math.ceil(toHz / binHz)));
      double peak = 0;
      for (int bin = from; bin < to; bin++) {
        // Los índices 0 y 1 son DC y Nyquist. Los demás vienen en pares real/imaginario.
        peak = Math.max(peak, Math.hypot(fft[2 * bin], fft[2 * bin + 1]));
      }
      // La raíz hace visibles los sonidos suaves sin superar la altura disponible.
      float target = (float) Math.sqrt(Math.min(1, Math.max(0, peak - 1) / 127));
      // Sube rápido con cada golpe y baja más despacio. El silencio termina en cero.
      float smoothing = target > levels[band] ? .65f : .18f;
      levels[band] += (target - levels[band]) * smoothing;
      if (levels[band] < .002f) levels[band] = 0;
    }
  }
}

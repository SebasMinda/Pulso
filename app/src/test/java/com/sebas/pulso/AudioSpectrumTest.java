package com.sebas.pulso;

import static org.junit.Assert.*;

import org.junit.Test;

/** Comprueba que las barras dependen del sonido y no conservan movimiento inventado. */
public class AudioSpectrumTest {
  @Test
  public void silenceStaysAtRest() {
    AudioSpectrum spectrum = new AudioSpectrum();
    for (int frame = 0; frame < 10; frame++) spectrum.update(new byte[1024], 48000);
    assertAtRest(spectrum);
  }

  @Test
  public void bassAndTrebleLightUpDifferentRegions() {
    AudioSpectrum bass = new AudioSpectrum();
    AudioSpectrum treble = new AudioSpectrum();
    bass.update(toneAt(4), 48000); // 187,5 Hz.
    treble.update(toneAt(170), 48000); // Aproximadamente 8 kHz.
    assertTrue(strongestBand(bass) < AudioSpectrum.BAND_COUNT / 2);
    assertTrue(strongestBand(treble) > AudioSpectrum.BAND_COUNT / 2);
  }

  @Test
  public void louderSoundProducesTallerBars() {
    AudioSpectrum soft = new AudioSpectrum();
    AudioSpectrum loud = new AudioSpectrum();
    byte[] quiet = toneAt(20);
    quiet[40] = 8;
    soft.update(quiet, 44100);
    loud.update(toneAt(20), 44100);
    assertTrue(loud.level(strongestBand(loud)) > soft.level(strongestBand(soft)));
  }

  @Test
  public void silenceDecaysAndEventuallyReachesZero() {
    AudioSpectrum spectrum = new AudioSpectrum();
    spectrum.update(toneAt(20), 48000);
    int band = strongestBand(spectrum);
    float before = spectrum.level(band);
    spectrum.update(new byte[1024], 48000);
    assertTrue(spectrum.level(band) > 0);
    assertTrue(spectrum.level(band) < before);
    for (int frame = 0; frame < 100; frame++) spectrum.update(new byte[1024], 48000);
    assertAtRest(spectrum);
  }

  @Test
  public void signedFftExtremesRemainWithinDrawingBounds() {
    AudioSpectrum spectrum = new AudioSpectrum();
    byte[] fft = new byte[128];
    java.util.Arrays.fill(fft, Byte.MIN_VALUE);
    for (int frame = 0; frame < 50; frame++) spectrum.update(fft, 8000);
    for (int band = 0; band < AudioSpectrum.BAND_COUNT; band++) {
      assertTrue(Float.isFinite(spectrum.level(band)));
      assertTrue(spectrum.level(band) >= 0);
      assertTrue(spectrum.level(band) <= 1);
    }
  }

  @Test
  public void invalidInputAndTrackResetDiscardPreviousLevels() {
    AudioSpectrum spectrum = new AudioSpectrum();
    byte[][] invalid = {null, new byte[0], new byte[3], new byte[5]};
    for (byte[] fft : invalid) {
      spectrum.update(toneAt(20), 48000);
      spectrum.update(fft, 48000);
      assertAtRest(spectrum);
    }
    spectrum.update(toneAt(20), 48000);
    spectrum.update(toneAt(20), 0);
    assertAtRest(spectrum);
    spectrum.update(toneAt(20), 48000);
    spectrum.clear();
    assertAtRest(spectrum);
  }

  @Test
  public void dcAndNyquistAreNotMistakenForMusicalBands() {
    AudioSpectrum spectrum = new AudioSpectrum();
    byte[] fft = new byte[1024];
    fft[0] = 127;
    fft[1] = 127;
    spectrum.update(fft, 48000);
    assertAtRest(spectrum);
  }

  private static byte[] toneAt(int bin) {
    byte[] fft = new byte[1024];
    fft[2 * bin] = 100;
    return fft;
  }

  private static int strongestBand(AudioSpectrum spectrum) {
    int strongest = 0;
    for (int band = 1; band < AudioSpectrum.BAND_COUNT; band++) {
      if (spectrum.level(band) > spectrum.level(strongest)) strongest = band;
    }
    assertTrue(spectrum.level(strongest) > 0);
    return strongest;
  }

  private static void assertAtRest(AudioSpectrum spectrum) {
    for (int band = 0; band < AudioSpectrum.BAND_COUNT; band++) {
      assertEquals(0, spectrum.level(band), 0);
    }
  }
}

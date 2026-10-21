package com.sebas.pulso;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.FrameLayout;

/** Alterna la marca original y un espectro del audio que reproduce Pulso. */
public final class PlaybackHeaderView extends FrameLayout {
  private static final long FRAME_MS = 33;
  private final Handler handler = new Handler(Looper.getMainLooper());
  private final BrandView brand;
  private final SpectrumView spectrum;
  private Visualizer visualizer;
  private byte[] fft;
  private int requestedSession;
  // Recuerda un fallo para no intentar abrir el mismo efecto cada medio segundo.
  private int attemptedSession;
  private boolean foreground;

  public PlaybackHeaderView(Context context) {
    super(context);
    brand = new BrandView(context);
    spectrum = new SpectrumView(context);
    addView(brand, new LayoutParams(-1, -1));
    addView(spectrum, new LayoutParams(-1, -1));
    spectrum.setVisibility(GONE);
  }

  public void setAccent(int color) {
    brand.setAccent(color);
    spectrum.paint.setColor(color);
    spectrum.invalidate();
  }

  /** La sesión cambia cuando MediaPlayer abre otra canción; cero significa reposo. */
  public void setPlayback(boolean playing, int audioSessionId) {
    int next = playing && audioSessionId > 0 ? audioSessionId : 0;
    if (requestedSession != next) {
      requestedSession = next;
      attemptedSession = 0;
      stopCapture();
    }
    updateCapture();
  }

  /** La música sigue en segundo plano, pero el análisis solo trabaja con la pantalla delante. */
  public void setForeground(boolean visible) {
    if (foreground == visible) return;
    foreground = visible;
    attemptedSession = 0;
    updateCapture();
  }

  private void updateCapture() {
    // Android puede notificar visibilidad durante la construcción, antes de crear los hijos.
    if (brand == null) return;
    boolean allowed =
        getContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED;
    if (!foreground
        || requestedSession <= 0
        || !allowed
        || !isAttachedToWindow()
        || getWindowVisibility() != VISIBLE
        || !isShown()
        || !ValueAnimator.areAnimatorsEnabled()) {
      stopCapture();
      return;
    }
    if (visualizer != null || attemptedSession == requestedSession) return;
    attemptedSession = requestedSession;
    try {
      // Nunca usa la sesión cero: analiza únicamente el reproductor de Pulso.
      visualizer = new Visualizer(requestedSession);
      int[] range = Visualizer.getCaptureSizeRange();
      int size = Math.max(range[0], Math.min(1024, range[1]));
      if (visualizer.setCaptureSize(size) != Visualizer.SUCCESS
          || visualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED) != Visualizer.SUCCESS
          || visualizer.setEnabled(true) != Visualizer.SUCCESS) {
        stopCapture();
        return;
      }
      fft = new byte[size];
      spectrum.audio.clear();
      brand.setVisibility(GONE);
      spectrum.setVisibility(VISIBLE);
      handler.post(captureFrame);
    } catch (RuntimeException e) {
      // El efecto no está disponible en todos los equipos. Se conserva la marca.
      stopCapture();
    }
  }

  /** Lee y dibuja en el hilo principal: no hay callbacks pendientes de otra canción. */
  private final Runnable captureFrame =
      new Runnable() {
        @Override
        public void run() {
          if (visualizer == null) return;
          try {
            if (visualizer.getFft(fft) != Visualizer.SUCCESS) {
              stopCapture();
              return;
            }
            spectrum.audio.update(fft, visualizer.getSamplingRate() / 1000);
            spectrum.invalidate();
            handler.postDelayed(this, FRAME_MS);
          } catch (RuntimeException e) {
            stopCapture();
          }
        }
      };

  private void stopCapture() {
    handler.removeCallbacks(captureFrame);
    Visualizer previous = visualizer;
    visualizer = null;
    fft = null;
    if (previous != null) {
      try {
        previous.setEnabled(false);
      } catch (RuntimeException ignored) {
        // Aunque el sistema haya cerrado el efecto, todavía hay que liberar la referencia nativa.
      } finally {
        try {
          previous.release();
        } catch (RuntimeException ignored) {
          // Un servidor de audio que ya terminó tampoco debe cerrar la pantalla.
        }
      }
    }
    spectrum.setVisibility(GONE);
    if (brand.getVisibility() != VISIBLE) brand.setVisibility(VISIBLE);
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    attemptedSession = 0;
    updateCapture();
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
    super.onWindowVisibilityChanged(visibility);
    if (visibility == VISIBLE) attemptedSession = 0;
    updateCapture();
  }

  @Override
  protected void onVisibilityChanged(View changedView, int visibility) {
    super.onVisibilityChanged(changedView, visibility);
    if (visibility == VISIBLE) attemptedSession = 0;
    updateCapture();
  }

  @Override
  protected void onDetachedFromWindow() {
    stopCapture();
    super.onDetachedFromWindow();
  }

  /** Solo dibuja alturas calculadas del audio; no inventa ondas ni valores aleatorios. */
  private static final class SpectrumView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final AudioSpectrum audio = new AudioSpectrum();

    SpectrumView(Context context) {
      super(context);
      setContentDescription("Visualizador de la música de Pulso");
      setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
    }

    @Override
    protected void onDraw(Canvas canvas) {
      super.onDraw(canvas);
      float density = getResources().getDisplayMetrics().density;
      float width = Math.min(getWidth(), 164 * density);
      float step = width / AudioSpectrum.BAND_COUNT;
      float barWidth = step * .58f;
      float maxHeight = Math.min(getHeight() * .84f, 32 * density);
      float minHeight = Math.min(maxHeight, 2 * density);
      float left = (getWidth() - width) / 2;
      float centerY = getHeight() / 2f;
      for (int band = 0; band < AudioSpectrum.BAND_COUNT; band++) {
        float height = minHeight + audio.level(band) * (maxHeight - minHeight);
        float x = left + step * band + (step - barWidth) / 2;
        canvas.drawRoundRect(
            x,
            centerY - height / 2,
            x + barWidth,
            centerY + height / 2,
            barWidth / 2,
            barWidth / 2,
            paint);
      }
    }
  }
}

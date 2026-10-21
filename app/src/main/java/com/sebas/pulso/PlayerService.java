package com.sebas.pulso;

import android.app.*;
import android.content.*;
import android.graphics.Bitmap;
import android.media.*;
import android.media.session.*;
import android.net.Uri;
import android.os.*;
import java.util.*;

/** Reproduce canciones en segundo plano y ofrece controles en la notificación de Android. */
public class PlayerService extends Service {
  public static PlayerService instance;
  public final ArrayList<Track> queue = new ArrayList<>();
  private MediaPlayer player;
  private MediaSession session;
  private AudioManager audio;
  private AudioFocusRequest focus;
  private String artworkKey = "";
  private Bitmap artwork;
  private final ShuffleOrder shuffled = new ShuffleOrder();
  private boolean shuffle;
  private int index = -1;
  private boolean preparing = false;
  public String error = "";

  public class LocalBinder extends Binder {
    public PlayerService get() {
      return PlayerService.this;
    }
  }

  // Prepara el audio, la sesión multimedia y el canal de notificaciones.
  @Override
  public void onCreate() {
    super.onCreate();
    instance = this;
    shuffle = getSharedPreferences("playback", 0).getBoolean("shuffle", false);
    getSystemService(NotificationManager.class)
        .createNotificationChannel(
            new NotificationChannel("player", "Reproducción", NotificationManager.IMPORTANCE_LOW));
    audio = (AudioManager) getSystemService(AUDIO_SERVICE);
    AudioAttributes attrs =
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build();
    focus =
        new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attrs)
            .setOnAudioFocusChangeListener(
                change -> {
                  if (change == AudioManager.AUDIOFOCUS_LOSS
                      || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause();
                  if (player != null)
                    player.setVolume(
                        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ? 0.2f : 1f,
                        change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ? 0.2f : 1f);
                })
            .build();
    session = new MediaSession(this, "Pulso");
    session.setCallback(
        new MediaSession.Callback() {
          @Override
          public void onPlay() {
            resume();
          }

          @Override
          public void onPause() {
            pause();
          }

          @Override
          public void onSkipToNext() {
            next();
          }

          @Override
          public void onSkipToPrevious() {
            previous();
          }

          @Override
          public void onSeekTo(long pos) {
            seek((int) pos);
          }
        });
    session.setActive(true);
    registerReceiver(noisy, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
  }

  private final BroadcastReceiver noisy =
      new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
          pause();
        }
      };

  @Override
  public IBinder onBind(Intent i) {
    return new LocalBinder();
  }

  // Recibe las acciones de los controles de la notificación.
  @Override
  public int onStartCommand(Intent i, int flags, int id) {
    if (i != null && i.getAction() != null)
      switch (i.getAction()) {
        case "toggle":
          toggle();
          break;
        case "next":
          next();
          break;
        case "prev":
          previous();
          break;
      }
    return START_NOT_STICKY;
  }

  private List<String> urls() {
    List<String> ids = new ArrayList<>();
    for (Track t : queue) ids.add(t.uri);
    return ids;
  }

  private void resetShuffle() {
    shuffled.reset(urls(), currentTrack() == null ? null : currentTrack().uri);
  }

  public boolean shuffleEnabled() {
    return shuffle;
  }

  // Guarda el modo aleatorio y reinicia su orden.
  public void setShuffle(boolean enabled) {
    shuffle = enabled;
    getSharedPreferences("playback", 0).edit().putBoolean("shuffle", enabled).apply();
    resetShuffle();
    update();
  }

  // Actualiza la lista conservando la canción actual si todavía existe.
  public void setQueue(List<Track> tracks) {
    List<String> before = urls();
    String current = currentTrack() == null ? null : currentTrack().uri;
    queue.clear();
    queue.addAll(tracks);
    index = -1;
    if (current != null)
      for (int n = 0; n < queue.size(); n++)
        if (queue.get(n).uri.equals(current)) {
          index = n;
          break;
        }
    if (!before.equals(urls())) resetShuffle();
  }

  public Track currentTrack() {
    return index >= 0 && index < queue.size() ? queue.get(index) : null;
  }

  // Fuerza a recargar la portada local de la notificación.
  public void refreshArtwork() {
    artworkKey = "";
    update();
  }

  public String title() {
    return index >= 0 && index < queue.size() ? queue.get(index).title : "Elige una canción";
  }

  public boolean playing() {
    return player != null && !preparing && player.isPlaying();
  }

  /** Permite visualizar solo este audio. Cero impide capturar la mezcla general del teléfono. */
  public int audioSessionId() {
    return player != null && !preparing ? player.getAudioSessionId() : 0;
  }

  public int duration() {
    return player != null && !preparing ? player.getDuration() : 0;
  }

  public int position() {
    return player != null && !preparing ? player.getCurrentPosition() : 0;
  }

  // Cambia la posición del audio cuando el reproductor ya está preparado.
  public void seek(int p) {
    if (player != null && !preparing) player.seekTo(p);
  }

  public void play(int n) {
    playInternal(n, true);
  }

  // Abre el archivo y prepara el audio sin bloquear; informa si falla.
  private void playInternal(int n, boolean selected) {
    if (n < 0 || n >= queue.size()) return;
    if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      error = "Otra aplicación está usando el audio.";
      return;
    }
    if (player != null) player.release();
    index = n;
    if (selected) resetShuffle();
    preparing = true;
    error = "";
    player = new MediaPlayer();
    player.setAudioAttributes(
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build());
    player.setOnPreparedListener(
        p -> {
          preparing = false;
          p.start();
          update();
        });
    player.setOnCompletionListener(
        p -> {
          if (shuffle || index + 1 < queue.size()) next();
          else pause();
        });
    player.setOnErrorListener(
        (p, w, e) -> {
          preparing = false;
          error = "No se pudo reproducir este archivo. Prueba otra canción.";
          p.release();
          player = null;
          stopForeground(STOP_FOREGROUND_REMOVE);
          return true;
        });
    try {
      player.setDataSource(this, Uri.parse(queue.get(index).uri));
      update();
      player.prepareAsync();
    } catch (Exception e) {
      preparing = false;
      error = "No se puede abrir el archivo. Vuelve a importarlo.";
      player.release();
      player = null;
      stopForeground(STOP_FOREGROUND_REMOVE);
    }
  }

  public void toggle() {
    if (playing()) pause();
    else resume();
  }

  // Reanuda la música o elige una canción si aún no hay un reproductor.
  public void resume() {
    if (player == null) {
      if (!queue.isEmpty()) {
        if (shuffle) next();
        else play(Math.max(index, 0));
      }
      return;
    }
    if (!preparing && audio.requestAudioFocus(focus) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
      player.start();
      update();
    }
  }

  // Pausa el audio y actualiza los controles.
  public void pause() {
    if (player != null && !preparing && player.isPlaying()) player.pause();
    update();
  }

  private void playUri(String uri) {
    if (uri != null)
      for (int n = 0; n < queue.size(); n++)
        if (queue.get(n).uri.equals(uri)) {
          playInternal(n, false);
          return;
        }
  }

  // Avanza según el orden normal o el modo aleatorio.
  public void next() {
    if (!queue.isEmpty()) {
      if (shuffle) playUri(shuffled.next());
      else playInternal((index + 1) % queue.size(), false);
    }
  }

  // Reinicia la canción si lleva más de tres segundos; en otro caso vuelve a la anterior.
  public void previous() {
    if (position() > 3000) seek(0);
    else skipPrevious();
  }

  // Cambia directamente a la canción anterior, incluido el historial aleatorio.
  public void skipPrevious() {
    if (!queue.isEmpty()) {
      if (shuffle) playUri(shuffled.previous());
      else playInternal((index - 1 + queue.size()) % queue.size(), false);
    }
  }

  private PendingIntent action(String a) {
    return PendingIntent.getService(
        this,
        a.hashCode(),
        new Intent(this, PlayerService.class).setAction(a),
        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
  }

  // Sincroniza título, portada, estado y botones con Android y la notificación.
  private void update() {
    if (player == null) return;
    Track track = currentTrack();
    String key = track == null ? "empty" : track.uri + "\n" + track.cover;
    if (!key.equals(artworkKey)) {
      artworkKey = key;
      artwork = CoverArt.cached(this, track);
    }
    session.setMetadata(
        new MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title())
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track == null ? "" : track.artist)
            .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, artwork)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, duration())
            .build());
    session.setPlaybackState(
        new PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY
                    | PlaybackState.ACTION_PAUSE
                    | PlaybackState.ACTION_SKIP_TO_NEXT
                    | PlaybackState.ACTION_SKIP_TO_PREVIOUS
                    | PlaybackState.ACTION_SEEK_TO)
            .setState(
                playing() ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, position(), 1)
            .build());
    Notification n =
        new Notification.Builder(this, "player")
            .setSmallIcon(com.sebas.pulso.R.drawable.icon)
            .setContentTitle(title())
            .setContentText("Pulso · Tu música")
            .setLargeIcon(artwork)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE))
            .addAction(
                new Notification.Action.Builder(
                        android.R.drawable.ic_media_previous, "Anterior", action("prev"))
                    .build())
            .addAction(
                new Notification.Action.Builder(
                        playing()
                            ? android.R.drawable.ic_media_pause
                            : android.R.drawable.ic_media_play,
                        playing() ? "Pausar" : "Reproducir",
                        action("toggle"))
                    .build())
            .addAction(
                new Notification.Action.Builder(
                        android.R.drawable.ic_media_next, "Siguiente", action("next"))
                    .build())
            .setStyle(
                new Notification.MediaStyle()
                    .setMediaSession(session.getSessionToken())
                    .setShowActionsInCompactView(0, 1, 2))
            .setOnlyAlertOnce(true)
            .setOngoing(playing())
            .build();
    startForeground(1, n);
  }

  // Libera el reproductor, el control del audio y la sesión multimedia.
  @Override
  public void onDestroy() {
    unregisterReceiver(noisy);
    if (player != null) player.release();
    audio.abandonAudioFocusRequest(focus);
    session.release();
    instance = null;
    super.onDestroy();
  }
}

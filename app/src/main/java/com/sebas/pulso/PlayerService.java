package com.sebas.pulso;

import android.app.*;
import android.content.*;
import android.media.*;
import android.media.session.*;
import android.net.Uri;
import android.graphics.Bitmap;
import android.os.*;
import java.util.*;

public class PlayerService extends Service {
 public static PlayerService instance;
 public final ArrayList<Track> queue = new ArrayList<>();
 private MediaPlayer player; private MediaSession session; private AudioManager audio; private AudioFocusRequest focus;
 private String artworkKey="";private Bitmap artwork;
 private int index=-1; private boolean preparing=false; public String error="";
 public class LocalBinder extends Binder { public PlayerService get() { return PlayerService.this; } }
 @Override public void onCreate() {
  super.onCreate(); instance=this;
  getSystemService(NotificationManager.class).createNotificationChannel(new NotificationChannel("player","Reproducción",NotificationManager.IMPORTANCE_LOW));
  audio=(AudioManager)getSystemService(AUDIO_SERVICE);
  AudioAttributes attrs=new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(attrs).setOnAudioFocusChangeListener(change->{
   if(change==AudioManager.AUDIOFOCUS_LOSS || change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) pause();
   if(player!=null) player.setVolume(change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK?0.2f:1f,change==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK?0.2f:1f);
  }).build();
  session=new MediaSession(this,"Pulso");
  session.setCallback(new MediaSession.Callback(){
   @Override public void onPlay(){resume();} @Override public void onPause(){pause();}
   @Override public void onSkipToNext(){next();} @Override public void onSkipToPrevious(){previous();}
   @Override public void onSeekTo(long pos){seek((int)pos);}
  }); session.setActive(true);
  registerReceiver(noisy,new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
 }
 private final BroadcastReceiver noisy=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){pause();}};
 @Override public IBinder onBind(Intent i){return new LocalBinder();}
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i!=null && i.getAction()!=null) switch(i.getAction()) {case "toggle":toggle();break;case "next":next();break;case "prev":previous();break;}
  return START_NOT_STICKY;
 }
 public void setQueue(List<Track> tracks){String current=index>=0&&index<queue.size()?queue.get(index).uri:null;queue.clear();queue.addAll(tracks);if(current!=null){index=-1;for(int n=0;n<queue.size();n++)if(queue.get(n).uri.equals(current)){index=n;break;}}}
 public Track currentTrack(){return index>=0&&index<queue.size()?queue.get(index):null;}
 public void refreshArtwork(){artworkKey="";update();}
 public String title(){return index>=0&&index<queue.size()?queue.get(index).title:"Elige una canción";}
 public boolean playing(){return player!=null&&!preparing&&player.isPlaying();}
 public int duration(){return player!=null&&!preparing?player.getDuration():0;}
 public int position(){return player!=null&&!preparing?player.getCurrentPosition():0;}
 public void seek(int p){if(player!=null&&!preparing)player.seekTo(p);}
 public void play(int n){
  if(n<0||n>=queue.size())return;
  if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){error="Otra aplicación está usando el audio.";return;}
  if(player!=null)player.release();index=n;preparing=true;error="";
  player=new MediaPlayer();
  player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
  player.setOnPreparedListener(p->{preparing=false;p.start();update();});
  player.setOnCompletionListener(p->{if(index+1<queue.size())next();else pause();});
  player.setOnErrorListener((p,w,e)->{preparing=false;error="No se pudo reproducir este archivo. Prueba otra canción.";p.release();player=null;stopForeground(STOP_FOREGROUND_REMOVE);return true;});
  try{player.setDataSource(this,Uri.parse(queue.get(index).uri));update();player.prepareAsync();}
  catch(Exception e){preparing=false;error="No se puede abrir el archivo. Vuelve a importarlo.";player.release();player=null;stopForeground(STOP_FOREGROUND_REMOVE);}
 }
 public void toggle(){if(playing())pause();else resume();}
 public void resume(){if(player==null){if(!queue.isEmpty())play(Math.max(index,0));return;}if(!preparing&&audio.requestAudioFocus(focus)==AudioManager.AUDIOFOCUS_REQUEST_GRANTED){player.start();update();}}
 public void pause(){if(player!=null&&!preparing&&player.isPlaying())player.pause();update();}
 public void next(){if(!queue.isEmpty())play((index+1)%queue.size());}
 public void previous(){if(position()>3000)seek(0);else if(!queue.isEmpty())play((index-1+queue.size())%queue.size());}
 private PendingIntent action(String a){return PendingIntent.getService(this,a.hashCode(),new Intent(this,PlayerService.class).setAction(a),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
 private void update(){
  if(player==null)return;
  Track track=currentTrack();String key=track==null?"empty":track.uri+"\n"+track.cover;
  if(!key.equals(artworkKey)){artworkKey=key;artwork=CoverArt.cached(this,track);}
  session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,title()).putString(MediaMetadata.METADATA_KEY_ARTIST,track==null?"":track.artist).putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART,artwork).putLong(MediaMetadata.METADATA_KEY_DURATION,duration()).build());
  session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_SEEK_TO).setState(playing()?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,position(),1).build());
  Notification n=new Notification.Builder(this,"player").setSmallIcon(com.sebas.pulso.R.drawable.icon).setContentTitle(title()).setContentText("Pulso · Tu música")
   .setLargeIcon(artwork)
   .setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE))
   .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_previous,"Anterior",action("prev")).build())
   .addAction(new Notification.Action.Builder(playing()?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing()?"Pausar":"Reproducir",action("toggle")).build())
   .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next,"Siguiente",action("next")).build())
   .setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).setOnlyAlertOnce(true).setOngoing(playing()).build();
  startForeground(1,n);
 }
 @Override public void onDestroy(){unregisterReceiver(noisy);if(player!=null)player.release();audio.abandonAudioFocusRequest(focus);session.release();instance=null;super.onDestroy();}
}

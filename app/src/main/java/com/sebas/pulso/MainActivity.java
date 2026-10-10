package com.sebas.pulso;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;
import org.json.*;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private final int BG=Color.rgb(12,18,29), CARD=Color.rgb(24,34,49), GREEN=Color.rgb(163,244,199), MUTED=Color.rgb(166,180,198);
 private LinearLayout root, songs; private TextView now,status,time; private Button play,download; private SeekBar seek; private EditText link;
 private PlayerService player; private boolean bound, seeking; private final ArrayList<Track> tracks=new ArrayList<>();
 private Updater updater;
 private final Handler handler=new Handler(Looper.getMainLooper()); private final ExecutorService jobs=Executors.newSingleThreadExecutor();
 private List<AudioStream> streams; private String videoTitle; private File exportFile; private volatile okhttp3.Call activeCall; private volatile boolean cancelled,busy;
 private final ServiceConnection connection=new ServiceConnection(){
  @Override public void onServiceConnected(ComponentName n,IBinder b){player=((PlayerService.LocalBinder)b).get();bound=true;player.setQueue(tracks);}
  @Override public void onServiceDisconnected(ComponentName n){player=null;bound=false;}
 };
 @Override public void onCreate(Bundle state){
  super.onCreate(state);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  load();NewPipe.init(new Net());build();bindService(new Intent(this,PlayerService.class),connection,BIND_AUTO_CREATE);
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},9);
  acceptShare(getIntent());handler.post(tick);
 }
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(5));return t;}
 private void rounded(View v,int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));v.setBackground(d);}
 private Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(BG);rounded(b,GREEN);b.setOnClickListener(v->r.run());return b;}
 private LinearLayout section(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(14),dp(18),dp(14));rounded(box,CARD);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(12),0,dp(8));root.addView(box,lp);return box;}
 private void build(){
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setBackgroundColor(BG);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(32),dp(20),dp(28));sc.addView(root);setContentView(sc);
  root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(20),dp(16)+insets.getSystemWindowInsetTop(),dp(20),dp(20)+insets.getSystemWindowInsetBottom());return insets;});
  TextView logo=text("pulso",42,GREEN);logo.setTypeface(null,Typeface.BOLD);root.addView(logo);root.addView(text("Tu música, a tu ritmo.",16,MUTED));
  LinearLayout box=section();box.addView(text("AHORA SUENA",11,GREEN));now=text("Elige una canción",22,Color.WHITE);box.addView(now);
  seek=new SeekBar(this);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(GREEN));seek.setThumbTintList(android.content.res.ColorStateList.valueOf(GREEN));box.addView(seek);
  time=text("0:00 / 0:00",12,MUTED);box.addView(time);
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){}public void onStartTrackingTouch(SeekBar s){seeking=true;}public void onStopTrackingTouch(SeekBar s){if(player!=null)player.seek(s.getProgress());seeking=false;}});
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);box.addView(controls);
  Button prev=button("◀",()->{if(player!=null)player.previous();});play=button("Reproducir",()->{if(player!=null){startService(new Intent(this,PlayerService.class));player.toggle();}});Button next=button("▶",()->{if(player!=null)player.next();});
  for(Button b: new Button[]{prev,play,next}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(52),b==play?2:1);lp.setMargins(dp(4),dp(4),dp(4),dp(4));controls.addView(b,lp);}
  LinearLayout yt=section();yt.addView(text("Desde YouTube",22,Color.WHITE));yt.addView(text("Pega un enlace de video y elige el audio original disponible.",14,MUTED));
  link=new EditText(this);link.setTextColor(Color.WHITE);link.setHintTextColor(MUTED);link.setTextSize(14);link.setHint("https://youtu.be/…");link.setSingleLine(true);link.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);yt.addView(link);
  download=button("Buscar audio",this::extract);yt.addView(download);status=text("Sin conversiones: conserva el audio de origen.",13,MUTED);yt.addView(status);
  Button cancel=button("Cancelar descarga",()->{cancelled=true;okhttp3.Call c=activeCall;if(c!=null)c.cancel();});yt.addView(cancel);
  yt.addView(text("Descarga contenido propio o con permiso. Los videos privados y protegidos no están disponibles.",12,MUTED));
  LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);root.addView(row);TextView heading=text("Tu biblioteca",23,Color.WHITE);row.addView(heading,new LinearLayout.LayoutParams(0,-2,1));row.addView(button("+ Importar",this::pick));
  root.addView(text("MP3 · M4A · FLAC · WAV · WebM/Opus",12,MUTED));songs=new LinearLayout(this);songs.setOrientation(LinearLayout.VERTICAL);root.addView(songs);renderSongs();
  LinearLayout updates=section();updates.addView(text("Actualizaciones",22,Color.WHITE));
  TextView updateStatus=text("Pulso "+BuildConfig.VERSION_NAME,13,MUTED);updates.addView(updateStatus);
  Button updateButton=button("Buscar actualizaciones",()->{});updates.addView(updateButton);updater=new Updater(this,updateButton,updateStatus);
  Switch automatic=new Switch(this);automatic.setText("Avisar al abrir Pulso");automatic.setTextColor(MUTED);automatic.setChecked(updater.automatic());automatic.setOnCheckedChangeListener((v,value)->updater.automatic(value));updates.addView(automatic);updater.checkOnLaunch();
  root.addView(text("Pulso "+BuildConfig.VERSION_NAME+" · Tu música, a tu ritmo",12,MUTED));
 }
 private void renderSongs(){songs.removeAllViews();if(tracks.isEmpty()){songs.addView(text("Aquí aparecerán tus canciones. Importa archivos o descarga un audio.",15,MUTED));return;}
  for(int i=0;i<tracks.size();i++){final int n=i;Track t=tracks.get(i);LinearLayout row=new LinearLayout(this);row.setPadding(dp(12),dp(9),dp(12),dp(9));rounded(row,CARD);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(6),0,0);songs.addView(row,lp);
   TextView title=text("♪  "+t.title,16,Color.WHITE);row.addView(title,new LinearLayout.LayoutParams(0,-2,1));title.setOnClickListener(v->{if(player!=null){startService(new Intent(this,PlayerService.class));player.setQueue(tracks);player.play(n);}});
   TextView more=text("  ⋮  ",24,GREEN);row.addView(more);more.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(t.title).setItems(new String[]{"Guardar una copia…","Quitar de la biblioteca"},(d,w)->{if(w==0)export(t);else{tracks.remove(t);save();renderSongs();if(player!=null){player.pause();player.setQueue(tracks);}}}).show());
  }
 }
 private void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,1);}
 private void addUri(Uri u,int flags){try{getContentResolver().takePersistableUriPermission(u,flags&Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}String name="Canción";try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(idx>=0)name=c.getString(idx);}}catch(Exception ignored){}for(Track t:tracks)if(t.uri.equals(u.toString()))return;tracks.add(new Track(name,u.toString()));}
 @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null)return;if(req==1){if(data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++)addUri(data.getClipData().getItemAt(i).getUri(),data.getFlags());else if(data.getData()!=null)addUri(data.getData(),data.getFlags());save();renderSongs();if(player!=null)player.setQueue(tracks);}else if(req==2&&data.getData()!=null&&exportFile!=null){File f=exportFile;Uri dest=data.getData();jobs.execute(()->{try(InputStream in=new FileInputStream(f);OutputStream out=getContentResolver().openOutputStream(dest)){if(out==null)throw new IOException();copy(in,out);ui(()->toast("Copia guardada"));}catch(Exception e){ui(()->toast("No se pudo guardar la copia"));}});}}
 private void export(Track t){if(!t.uri.startsWith("file:")){toast("Este archivo ya está en tu dispositivo.");return;}exportFile=new File(Uri.parse(t.uri).getPath());Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(exportFile.getName().endsWith(".m4a")?"audio/mp4":"audio/webm").putExtra(Intent.EXTRA_TITLE,exportFile.getName());startActivityForResult(i,2);}
 private void load(){try{JSONArray a=new JSONArray(getPreferences(0).getString("tracks","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);tracks.add(new Track(o.getString("title"),o.getString("uri")));}}catch(Exception ignored){}}
 private void save(){JSONArray a=new JSONArray();try{for(Track t:tracks)a.put(t.json());getPreferences(0).edit().putString("tracks",a.toString()).apply();}catch(Exception ignored){}}
 private void ui(Runnable r){handler.post(()->{if(!isDestroyed())r.run();});}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private void busy(boolean value,String message){busy=value;download.setEnabled(!value);status.setText(message);}
 private String normalize(String raw){
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("https?://[^\\s]+").matcher(raw);if(!m.find())throw new IllegalArgumentException("Pega un enlace válido de YouTube.");Uri u=Uri.parse(m.group());String host=u.getHost();String id=null;
  if("youtu.be".equals(host)){id=u.getLastPathSegment();}else if("youtube.com".equals(host)||"www.youtube.com".equals(host)||"m.youtube.com".equals(host)){id=u.getQueryParameter("v");List<String> p=u.getPathSegments();if(id==null&&p.size()==2&&(p.get(0).equals("shorts")||p.get(0).equals("live")))id=p.get(1);}
  if(id==null||!id.matches("[A-Za-z0-9_-]{11}"))throw new IllegalArgumentException("Usa el enlace de un video de YouTube normal.");return "https://www.youtube.com/watch?v="+id;
 }
 private void extract(){if(busy)return;final String url;try{url=normalize(link.getText().toString());}catch(Exception e){toast(e.getMessage());return;}cancelled=false;busy(true,"Buscando formatos de audio…");
  jobs.execute(()->{try{StreamInfo info=StreamInfo.getInfo(ServiceList.YouTube,url);List<AudioStream> list=new ArrayList<>();for(AudioStream s:info.getAudioStreams())if(s.isUrl()&&s.getFormat()!=null&&(s.getFormat().getSuffix().equals("m4a")||s.getFormat().getSuffix().equals("webm")))list.add(s);
    list.sort((a,b)->Integer.compare(b.getAverageBitrate(),a.getAverageBitrate()));if(list.isEmpty())throw new IOException("No hay audio directo compatible para este video.");if(cancelled){ui(()->busy(false,"Cancelado."));return;}streams=list;videoTitle=info.getName();ui(()->{busy(false,"Audio encontrado: "+videoTitle);String[] options=new String[list.size()];for(int i=0;i<list.size();i++){AudioStream s=list.get(i);options[i]=s.getFormat().getName()+" · "+s.getAverageBitrate()+" kb/s";}new AlertDialog.Builder(this).setTitle("Elige el audio original").setItems(options,(d,w)->fetch(list.get(w),videoTitle)).setNegativeButton("Cerrar",null).show();});
   }catch(Exception e){ui(()->busy(false,"No se pudo consultar YouTube. Puede estar bloqueado o requerir una actualización del extractor. "+e.getClass().getSimpleName()));}});
 }
 private void fetch(AudioStream stream,String title){if(busy)return;cancelled=false;busy(true,"Descargando… Mantén la app abierta hasta terminar.");getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  jobs.execute(()->{File partial=null;try{File dir=new File(getFilesDir(),"music");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Sin espacio");String base=title.replaceAll("[^\\p{L}\\p{N} ._-]","_");if(base.length()>90)base=base.substring(0,90);File dest=new File(dir,base+"-"+System.currentTimeMillis()+"."+stream.getFormat().getSuffix());partial=new File(dest+".part");
    okhttp3.Request req=new okhttp3.Request.Builder().url(stream.getContent()).header("User-Agent","Mozilla/5.0").build();activeCall=Net.CLIENT.newCall(req);
    try(okhttp3.Response r=activeCall.execute()){if(!r.isSuccessful()||r.body()==null)throw new IOException("HTTP "+r.code());long total=r.body().contentLength();try(BufferedInputStream in=new BufferedInputStream(r.body().byteStream());OutputStream out=new FileOutputStream(partial)){in.mark(16);byte[] head=new byte[12];int read=0;while(read<head.length){int got=in.read(head,read,head.length-read);if(got<0)break;read+=got;}if(read<head.length)throw new IOException("Archivo incompleto");in.reset();boolean webm=head.length>=4&&(head[0]&255)==0x1a&&(head[1]&255)==0x45&&(head[2]&255)==0xdf&&(head[3]&255)==0xa3;boolean mp4=head.length>=8&&head[4]=='f'&&head[5]=='t'&&head[6]=='y'&&head[7]=='p';if(!webm&&!mp4)throw new IOException("La red devolvió una respuesta que no es audio");byte[] b=new byte[65536];int n;long done=0,last=0;while((n=in.read(b))!=-1){if(cancelled)throw new IOException("Cancelado");out.write(b,0,n);done+=n;long clock=System.currentTimeMillis();if(clock-last>500){last=clock;String msg=total>0?"Descargando: "+(done*100/total)+" %":"Descargando: "+(done/1048576)+" MB";ui(()->status.setText(msg));}}if(done==0||(total>0&&done!=total))throw new IOException("Descarga incompleta");}}
    if(cancelled)throw new IOException("Cancelado");if(!partial.renameTo(dest))throw new IOException("No se pudo guardar");ui(()->{tracks.add(new Track(title,dest.toURI().toString()));save();renderSongs();if(player!=null)player.setQueue(tracks);busy(false,"Guardado. Ya puedes escucharlo sin internet.");});
   }catch(Exception e){if(partial!=null)partial.delete();ui(()->busy(false,cancelled?"Descarga cancelada.":"No se pudo descargar. Prueba consultar el enlace otra vez. "+e.getMessage()));}
   finally{activeCall=null;ui(()->getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));}});
 }
 private static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
 private void acceptShare(Intent i){if(Intent.ACTION_SEND.equals(i.getAction())){String s=i.getStringExtra(Intent.EXTRA_TEXT);if(s!=null)link.setText(s);}}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);acceptShare(i);}
 private String clock(int ms){return String.format(Locale.ROOT,"%d:%02d",ms/60000,ms/1000%60);}
 private final Runnable tick=new Runnable(){public void run(){if(player!=null){now.setText(player.title());play.setText(player.playing()?"Pausar":"Reproducir");if(!seeking){seek.setMax(Math.max(player.duration(),1));seek.setProgress(player.position());}time.setText(clock(player.position())+" / "+clock(player.duration()));if(!player.error.isEmpty()){toast(player.error);player.error="";}}handler.postDelayed(this,500);}};
 @Override public void onDestroy(){if(updater!=null)updater.close();handler.removeCallbacksAndMessages(null);cancelled=true;okhttp3.Call c=activeCall;if(c!=null)c.cancel();jobs.shutdownNow();if(bound)unbindService(connection);super.onDestroy();}
}

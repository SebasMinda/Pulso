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
 private final int BG=Color.rgb(8,8,8), CARD=Color.rgb(26,20,20), GOLD=Color.rgb(201,173,114), RED=Color.rgb(87,25,35), MUTED=Color.rgb(191,179,164);
 private LinearLayout root, songs; private TextView now,status,time; private Button play,download; private SeekBar seek; private EditText link;
 private PlayerService player; private boolean bound, seeking; private final ArrayList<Track> tracks=new ArrayList<>();
 private Updater updater;
 private CoverArt covers;
 private final ScrollView[] pages=new ScrollView[4];
 private final TextView[] tabs=new TextView[4];
 private int selectedTab;
 private ImageView playerCover,miniCover;private TextView artist,miniTitle,libraryCount;private Button miniPlay;
 private LinearLayout miniPlayer;private Track coverTarget;private String shownCover="";

 private final Handler handler=new Handler(Looper.getMainLooper()); private final ExecutorService jobs=Executors.newSingleThreadExecutor();
 private List<AudioStream> streams; private String videoTitle; private File exportFile; private volatile AudioDownload audioDownload; private volatile boolean cancelled,busy;
 private final ServiceConnection connection=new ServiceConnection(){
  @Override public void onServiceConnected(ComponentName n,IBinder b){player=((PlayerService.LocalBinder)b).get();bound=true;player.setQueue(tracks);}
  @Override public void onServiceDisconnected(ComponentName n){player=null;bound=false;}
 };
 @Override public void onCreate(Bundle state){
  super.onCreate(state);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  load();covers=new CoverArt(this);NewPipe.init(new Net());build();bindService(new Intent(this,PlayerService.class),connection,BIND_AUTO_CREATE);
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},9);
  acceptShare(getIntent());handler.post(tick);
 }
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,dp(5),0,dp(5));return t;}
 private void rounded(View v,int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(18));v.setBackground(d);}
 private GradientDrawable metallicRed(boolean selected){
  GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,selected?new int[]{Color.rgb(60,28,31),Color.rgb(42,15,20),Color.rgb(27,12,15)}:new int[]{Color.rgb(111,39,49),Color.rgb(87,25,35),Color.rgb(48,13,21)});
  d.setCornerRadius(dp(18));d.setStroke(dp(1),selected?Color.rgb(117,95,58):Color.rgb(127,66,67));return d;
 }
 private void goldMetal(TextView view){
  view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
   if(b>t){view.getPaint().setShader(new android.graphics.LinearGradient(0,0,0,b-t,new int[]{Color.rgb(169,139,83),Color.rgb(225,203,156),GOLD,Color.rgb(169,139,83)},new float[]{0,.38f,.58f,1},android.graphics.Shader.TileMode.CLAMP));view.invalidate();}
  });
 }
 private Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);b.setTextColor(Color.rgb(241,231,218));
  android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
  GradientDrawable disabled=metallicRed(false);disabled.setAlpha(110);states.addState(new int[]{-android.R.attr.state_enabled},disabled);states.addState(new int[]{},metallicRed(false));
  GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(dp(18));
  b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf(Color.argb(38,225,203,156)),states,mask));b.setOnClickListener(v->r.run());return b;}

 private LinearLayout section(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(14),dp(18),dp(14));rounded(box,CARD);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(12),0,dp(8));root.addView(box,lp);return box;}
 private ImageView cover(int size){ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setImageResource(R.drawable.cover_placeholder);rounded(image,CARD);image.setClipToOutline(true);image.setContentDescription("Portada de la canción");image.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return image;}
 private void page(int index,FrameLayout container){
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(12),dp(20),dp(24));sc.addView(root);container.addView(sc,new FrameLayout.LayoutParams(-1,-1));pages[index]=sc;
 }
 private void heading(String title,String subtitle){TextView h=text(title,30,Color.WHITE);h.setTypeface(null,Typeface.BOLD);root.addView(h);root.addView(text(subtitle,14,MUTED));}
 private void selectTab(int index){selectedTab=index;for(int i=0;i<4;i++){pages[i].setVisibility(i==index?View.VISIBLE:View.GONE);tabs[i].setTextColor(i==index?GOLD:MUTED);tabs[i].setTypeface(null,i==index?Typeface.BOLD:Typeface.NORMAL);if(i==index)tabs[i].setBackground(metallicRed(true));else rounded(tabs[i],BG);tabs[i].setSelected(i==index);}miniPlayer.setVisibility(index==1?View.GONE:View.VISIBLE);}
 private void togglePlayback(){if(player!=null){startService(new Intent(this,PlayerService.class));player.toggle();}}
 private void build(){
  LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);shell.setBackgroundColor(BG);setContentView(shell);
  shell.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets;});
  LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);brand.setPadding(dp(20),dp(8),dp(20),dp(4));shell.addView(brand);
  TextView logo=text("pulso",28,GOLD);logo.setTypeface(null,Typeface.BOLD);goldMetal(logo);brand.addView(logo,new LinearLayout.LayoutParams(0,-2,1));brand.addView(text("TU MÚSICA",10,MUTED));
  FrameLayout container=new FrameLayout(this);shell.addView(container,new LinearLayout.LayoutParams(-1,0,1));

  page(0,container);heading("Tu biblioteca");
  LinearLayout libraryRow=new LinearLayout(this);libraryRow.setGravity(Gravity.CENTER_VERTICAL);root.addView(libraryRow);libraryCount=text("0 canciones",13,MUTED);libraryRow.addView(libraryCount,new LinearLayout.LayoutParams(0,-2,1));libraryRow.addView(button("+ Importar",this::pick));
  songs=new LinearLayout(this);songs.setOrientation(LinearLayout.VERTICAL);root.addView(songs);root.addView(text("MP3 · M4A · FLAC · WAV · WebM/Opus",11,MUTED));renderSongs();

  page(1,container);heading("Ahora suena");
  LinearLayout box=section();
  int artSize=Math.min(300,Math.max(140,(int)(getResources().getDisplayMetrics().widthPixels/getResources().getDisplayMetrics().density)-80));
  playerCover=cover(artSize);LinearLayout.LayoutParams artLp=new LinearLayout.LayoutParams(dp(artSize),dp(artSize));artLp.gravity=Gravity.CENTER_HORIZONTAL;artLp.setMargins(0,dp(8),0,dp(20));box.addView(playerCover,artLp);
  now=text("Elige una canción",23,Color.WHITE);now.setTypeface(null,Typeface.BOLD);box.addView(now);artist=text("Tu biblioteca",14,MUTED);box.addView(artist);
  seek=new SeekBar(this);seek.setProgressTintList(android.content.res.ColorStateList.valueOf(GOLD));GradientDrawable goldThumb=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{Color.rgb(225,203,156),GOLD,Color.rgb(148,116,64)});goldThumb.setShape(GradientDrawable.OVAL);goldThumb.setSize(dp(20),dp(20));goldThumb.setStroke(dp(1),Color.rgb(145,116,65));seek.setThumb(goldThumb);box.addView(seek);time=text("0:00 / 0:00",12,MUTED);box.addView(time);
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){}public void onStartTrackingTouch(SeekBar s){seeking=true;}public void onStopTrackingTouch(SeekBar s){if(player!=null)player.seek(s.getProgress());seeking=false;}});
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);box.addView(controls);
  Button prev=button("◀",()->{if(player!=null)player.previous();});play=button("Reproducir",this::togglePlayback);Button next=button("▶",()->{if(player!=null)player.next();});prev.setContentDescription("Canción anterior");next.setContentDescription("Canción siguiente");
  for(Button b:new Button[]{prev,play,next}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(56),b==play?2:1);lp.setMargins(dp(3),dp(8),dp(3),dp(8));controls.addView(b,lp);}
  box.addView(text("La música sigue mientras navegas por las pestañas.",12,MUTED));

  page(2,container);heading("Descargas","De YouTube a tu biblioteca.");
  LinearLayout yt=section();yt.addView(text("Un enlace, tu música",21,Color.WHITE));yt.addView(text("Pega un enlace y elige el audio original disponible.",14,MUTED));
  link=new EditText(this);link.setTextColor(Color.WHITE);link.setHintTextColor(MUTED);link.setTextSize(14);link.setHint("https://youtu.be/…");link.setSingleLine(true);link.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);link.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GOLD));yt.addView(link);
  download=button("Buscar audio",this::extract);yt.addView(download);status=text("Listo para tu próxima canción.",13,MUTED);yt.addView(status);
  Button cancel=button("Cancelar descarga",()->{cancelled=true;AudioDownload d=audioDownload;if(d!=null)d.cancel();});yt.addView(cancel);
  yt.addView(text("Mantén Pulso abierta hasta terminar. Puedes cambiar de pestaña.",12,MUTED));
  root.addView(text("El audio se guarda sin conversiones. La portada del video se añade a la biblioteca.",13,MUTED));root.addView(text("Descarga contenido propio o con permiso. Los videos privados y protegidos no están disponibles.",12,MUTED));

  page(3,container);heading("Ajustes","Pulso, a tu manera.");
  LinearLayout updates=section();updates.addView(text("Actualizaciones",21,Color.WHITE));TextView updateStatus=text("Pulso "+BuildConfig.VERSION_NAME,13,MUTED);updates.addView(updateStatus);
  Button updateButton=button("Buscar actualizaciones",()->{});updates.addView(updateButton);updater=new Updater(this,updateButton,updateStatus);
  Switch automatic=new Switch(this);automatic.setText("Avisar al abrir Pulso");automatic.setTextColor(MUTED);automatic.setThumbTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{GOLD,MUTED}));automatic.setTrackTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{RED,CARD}));automatic.setChecked(updater.automatic());automatic.setOnCheckedChangeListener((v,value)->updater.automatic(value));updates.addView(automatic);updater.checkOnLaunch();
  LinearLayout about=section();about.addView(text("Tu música es tuya",21,Color.WHITE));about.addView(text("Importa tus archivos FLAC y otros formatos. Si una canción no tiene portada, puedes elegir una desde su menú ⋮.",14,MUTED));about.addView(text("Pulso "+BuildConfig.VERSION_NAME+" · Hecha para escuchar",12,GOLD));

  miniPlayer=new LinearLayout(this);miniPlayer.setGravity(Gravity.CENTER_VERTICAL);miniPlayer.setPadding(dp(12),dp(8),dp(12),dp(8));rounded(miniPlayer,CARD);LinearLayout.LayoutParams miniLp=new LinearLayout.LayoutParams(-1,-2);miniLp.setMargins(dp(12),dp(4),dp(12),dp(8));shell.addView(miniPlayer,miniLp);
  miniCover=cover(44);miniPlayer.addView(miniCover);miniCover.setOnClickListener(v->selectTab(1));miniTitle=text("Elige una canción",13,Color.WHITE);miniTitle.setMaxLines(1);miniTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);miniTitle.setPadding(dp(12),0,dp(8),0);miniPlayer.addView(miniTitle,new LinearLayout.LayoutParams(0,-2,1));miniTitle.setOnClickListener(v->selectTab(1));miniPlay=button("▶",this::togglePlayback);miniPlay.setContentDescription("Reproducir o pausar");miniPlayer.addView(miniPlay,new LinearLayout.LayoutParams(dp(56),dp(48)));
  LinearLayout navigation=new LinearLayout(this);navigation.setPadding(dp(4),dp(2),dp(4),dp(8));shell.addView(navigation);
  String[] labels={"Biblioteca","Reproductor","Descargas","Ajustes"};String[] icons={"♫","◉","↓","⚙"};
  for(int i=0;i<4;i++){final int tab=i;TextView item=text(icons[i]+"\n"+labels[i],11,MUTED);item.setGravity(Gravity.CENTER);item.setMinHeight(dp(56));item.setContentDescription(labels[i]);item.setOnClickListener(v->selectTab(tab));tabs[i]=item;navigation.addView(item,new LinearLayout.LayoutParams(0,-2,1));}
  selectTab(Math.max(0,Math.min(3,getPreferences(0).getInt("tab",0))));
 }
 private void renderSongs(){songs.removeAllViews();libraryCount.setText(tracks.size()+(tracks.size()==1?" canción":" canciones"));if(tracks.isEmpty()){LinearLayout empty=new LinearLayout(this);empty.setOrientation(LinearLayout.VERTICAL);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(20),dp(40),dp(20),dp(40));ImageView art=cover(100);empty.addView(art);empty.addView(text("Tu próxima canción te espera",19,Color.WHITE));empty.addView(text("Importa música o añade un enlace en Descargas.",14,MUTED));songs.addView(empty);return;}
  for(int i=0;i<tracks.size();i++){final int n=i;Track t=tracks.get(i);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(10),dp(6),dp(10));rounded(row,CARD);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,0);songs.addView(row,lp);
   ImageView image=cover(58);row.addView(image);covers.bind(image,t,null);
   LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.setPadding(dp(12),0,dp(4),0);row.addView(titles,new LinearLayout.LayoutParams(0,-2,1));TextView title=text(t.title,15,Color.WHITE);title.setTypeface(null,Typeface.BOLD);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);titles.addView(title);TextView subtitle=text(t.artist.isEmpty()?"Audio local":t.artist,12,MUTED);subtitle.setMaxLines(1);subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);titles.addView(subtitle);
   View.OnClickListener listen=v->{if(player!=null){startService(new Intent(this,PlayerService.class));player.setQueue(tracks);player.play(n);selectTab(1);}};image.setOnClickListener(listen);titles.setOnClickListener(listen);
   TextView more=text("⋮",24,GOLD);more.setGravity(Gravity.CENTER);more.setContentDescription("Opciones de "+t.title);row.addView(more,new LinearLayout.LayoutParams(dp(48),dp(56)));more.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(t.title).setItems(new String[]{"Elegir portada…","Guardar una copia…","Quitar de la biblioteca"},(d,w)->{if(w==0)chooseCover(t);else if(w==1)export(t);else{tracks.remove(t);save();renderSongs();if(player!=null){player.pause();player.setQueue(tracks);}}}).show());
  }
 }
 private void chooseCover(Track t){coverTarget=t;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,3);}
 private void pick(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,1);}
 private void addUri(Uri u,int flags){try{getContentResolver().takePersistableUriPermission(u,flags&Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}String name="Canción";try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int idx=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(idx>=0)name=c.getString(idx);}}catch(Exception ignored){}for(Track t:tracks)if(t.uri.equals(u.toString()))return;tracks.add(new Track(name,u.toString()));}
 @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null)return;if(req==3&&data.getData()!=null&&coverTarget!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}coverTarget.cover=u.toString();save();renderSongs();shownCover="";coverTarget=null;}else if(req==1){if(data.getClipData()!=null)for(int i=0;i<data.getClipData().getItemCount();i++)addUri(data.getClipData().getItemAt(i).getUri(),data.getFlags());else if(data.getData()!=null)addUri(data.getData(),data.getFlags());save();renderSongs();if(player!=null)player.setQueue(tracks);}else if(req==2&&data.getData()!=null&&exportFile!=null){File f=exportFile;Uri dest=data.getData();jobs.execute(()->{try(InputStream in=new FileInputStream(f);OutputStream out=getContentResolver().openOutputStream(dest)){if(out==null)throw new IOException();copy(in,out);ui(()->toast("Copia guardada"));}catch(Exception e){ui(()->toast("No se pudo guardar la copia"));}});}}
 private void export(Track t){if(!t.uri.startsWith("file:")){toast("Este archivo ya está en tu dispositivo.");return;}exportFile=new File(Uri.parse(t.uri).getPath());Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(exportFile.getName().endsWith(".m4a")?"audio/mp4":"audio/webm").putExtra(Intent.EXTRA_TITLE,exportFile.getName());startActivityForResult(i,2);}
 private void load(){try{JSONArray a=new JSONArray(getPreferences(0).getString("tracks","[]"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);tracks.add(Track.fromJson(o));}}catch(Exception ignored){}}
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
    list.sort((a,b)->Integer.compare(b.getAverageBitrate(),a.getAverageBitrate()));if(list.isEmpty())throw new IOException("No hay audio directo compatible para este video.");if(cancelled){ui(()->busy(false,"Cancelado."));return;}streams=list;videoTitle=info.getName();String art=info.getThumbnails().isEmpty()?"":info.getThumbnails().get(info.getThumbnails().size()-1).getUrl();String uploader=info.getUploaderName();ui(()->{busy(false,"Audio encontrado: "+videoTitle);String[] options=new String[list.size()];for(int i=0;i<list.size();i++){AudioStream s=list.get(i);options[i]=s.getFormat().getName()+" · "+s.getAverageBitrate()+" kb/s";}new AlertDialog.Builder(this).setTitle("Elige el audio original").setItems(options,(d,w)->fetch(list.get(w),info.getName(),art,uploader)).setNegativeButton("Cerrar",null).show();});
   }catch(Exception e){ui(()->busy(false,"No se pudo consultar YouTube. Puede estar bloqueado o requerir una actualización del extractor. "+e.getClass().getSimpleName()));}});
 }
 private void fetch(AudioStream stream,String title,String artwork,String uploader){if(busy)return;cancelled=false;busy(true,"Descargando… Mantén la app abierta hasta terminar.");getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  jobs.execute(()->{File partial=null;try{File dir=new File(getFilesDir(),"music");if(!dir.exists()&&!dir.mkdirs())throw new IOException("Sin espacio");String base=title.replaceAll("[^\\p{L}\\p{N} ._-]","_");if(base.length()>90)base=base.substring(0,90);File dest=new File(dir,base+"-"+System.currentTimeMillis()+"."+stream.getFormat().getSuffix());partial=new File(dest+".part");
    AudioDownload task=new AudioDownload(Net.CLIENT);audioDownload=task;if(cancelled)task.cancel();
    final long started=System.nanoTime();
    task.download(stream.getContent(),partial,(done,total)->{
     double seconds=Math.max(0.1,(System.nanoTime()-started)/1e9);
     String msg=String.format(Locale.ROOT,"Descargando: %s · %.1f MB · %.0f KB/s",total>0?(done*100/total)+" %":"…",done/1048576.0,done/1024.0/seconds);
     ui(()->status.setText(msg));
    });
    if(cancelled)throw new IOException("Cancelado");if(!partial.renameTo(dest))throw new IOException("No se pudo guardar");ui(()->{tracks.add(new Track(title,dest.toURI().toString(),artwork,uploader));save();renderSongs();if(player!=null)player.setQueue(tracks);busy(false,"Guardado. Ya puedes escucharlo sin internet.");});
   }catch(Exception e){if(partial!=null)partial.delete();ui(()->busy(false,cancelled?"Descarga cancelada.":"No se pudo descargar. Prueba consultar el enlace otra vez. "+e.getMessage()));}
   finally{audioDownload=null;ui(()->getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));}});
 }
 private static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
 private void acceptShare(Intent i){if(Intent.ACTION_SEND.equals(i.getAction())){String s=i.getStringExtra(Intent.EXTRA_TEXT);if(s!=null){link.setText(s);selectTab(2);}}}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);acceptShare(i);}
 private String clock(int ms){return String.format(Locale.ROOT,"%d:%02d",ms/60000,ms/1000%60);}
 private final Runnable tick=new Runnable(){public void run(){if(player!=null){now.setText(player.title());miniTitle.setText(player.title());play.setText(player.playing()?"Pausar":"Reproducir");miniPlay.setText(player.playing()?"Ⅱ":"▶");
   Track current=player.currentTrack();String id=current==null?"empty":current.uri+"\n"+current.cover;
   if(!id.equals(shownCover)){shownCover=id;artist.setText(current==null||current.artist.isEmpty()?"Tu biblioteca":current.artist);covers.bind(playerCover,current,()->{if(player!=null)player.refreshArtwork();});covers.bind(miniCover,current,null);}
if(!seeking){seek.setMax(Math.max(player.duration(),1));seek.setProgress(player.position());}time.setText(clock(player.position())+" / "+clock(player.duration()));if(!player.error.isEmpty()){toast(player.error);player.error="";}}handler.postDelayed(this,500);}};
 @Override public void onDestroy(){getPreferences(0).edit().putInt("tab",selectedTab).apply();if(covers!=null)covers.close();if(updater!=null)updater.close();handler.removeCallbacksAndMessages(null);cancelled=true;AudioDownload d=audioDownload;if(d!=null)d.cancel();jobs.shutdownNow();if(bound)unbindService(connection);super.onDestroy();}
}

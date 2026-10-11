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
 private final DownloadQueue downloadQueue=new DownloadQueue();
 private final ExecutorService lookups=Executors.newSingleThreadExecutor();
 private EditText query;private Button searchButton,moreResults,downloadAll,cancelBatch;private TextView searchStatus,queueCount;private LinearLayout searchRows,queueRows;
 private final List<Button> queueButtons=new ArrayList<>();private final List<StreamInfoItem> searchVideos=new ArrayList<>();private String searchTerm="";private org.schabi.newpipe.extractor.Page nextSearchPage;private boolean lookupBusy;
 private File exportFile; private volatile AudioDownload audioDownload; private volatile boolean cancelled,busy;
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

  page(0,container);heading("Tu biblioteca","Tus canciones, siempre contigo.");
  LinearLayout libraryRow=new LinearLayout(this);libraryRow.setGravity(Gravity.CENTER_VERTICAL);root.addView(libraryRow);libraryCount=text("0 canciones",13,MUTED);libraryRow.addView(libraryCount,new LinearLayout.LayoutParams(0,-2,1));libraryRow.addView(button("+ Importar",this::pick));
  songs=new LinearLayout(this);songs.setOrientation(LinearLayout.VERTICAL);root.addView(songs);root.addView(text("MP3 · M4A · FLAC · WAV · WebM/Opus",11,MUTED));renderSongs();

  page(1,container);heading("Ahora suena","Dale un momento a tu música.");
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

  page(2,container);buildDownloads();

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
 private String normalize(String raw){
  java.util.regex.Matcher m=java.util.regex.Pattern.compile("https?://[^\\s]+").matcher(raw);if(!m.find())throw new IllegalArgumentException("Pega un enlace válido de YouTube.");Uri u=Uri.parse(m.group());String host=u.getHost();String id=null;
  if("youtu.be".equals(host)){id=u.getLastPathSegment();}else if("youtube.com".equals(host)||"www.youtube.com".equals(host)||"m.youtube.com".equals(host)){id=u.getQueryParameter("v");List<String> p=u.getPathSegments();if(id==null&&p.size()==2&&(p.get(0).equals("shorts")||p.get(0).equals("live")))id=p.get(1);}
  if(id==null||!id.matches("[A-Za-z0-9_-]{11}"))throw new IllegalArgumentException("Usa el enlace de un video de YouTube normal.");return "https://www.youtube.com/watch?v="+id;
 }
 private void buildDownloads(){
  heading("Descargas","Busca, elige y arma tu lista.");
  LinearLayout find=section();find.addView(text("Buscar en YouTube",21,Color.WHITE));
  query=field("Canción o artista");query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);find.addView(query);
  searchButton=button("Buscar",()->search(false));find.addView(searchButton);
  query.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){search(false);return true;}return false;});
  searchStatus=text("Resultados de YouTube normal, con imagen, canal y duración.",12,MUTED);find.addView(searchStatus);
  LinearLayout yt=section();yt.addView(text("También por enlace",19,Color.WHITE));link=field("https://youtu.be/…");link.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);yt.addView(link);
  download=button("Agregar enlace a la lista",()->prepareLink(link.getText().toString()));yt.addView(download);yt.addView(button("Pegar varios enlaces…",this::pasteLinks));
  LinearLayout plan=section();queueCount=text("Tu lista",21,Color.WHITE);plan.addView(queueCount);plan.addView(text("Elige la calidad de cada canción antes de descargar.",12,MUTED));queueRows=new LinearLayout(this);queueRows.setOrientation(LinearLayout.VERTICAL);plan.addView(queueRows);
  downloadAll=button("Descargar todo",this::startBatch);plan.addView(downloadAll);cancelBatch=button("Detener descargas",this::stopBatch);plan.addView(cancelBatch);cancelBatch.setVisibility(View.GONE);
  status=text("Añade canciones para empezar.",13,MUTED);plan.addView(status);plan.addView(text("Mantén Pulso abierta. Puedes escuchar música y cambiar de pestaña. La lista se guarda al cerrar.",12,MUTED));
  LinearLayout results=section();results.addView(text("Resultados",19,Color.WHITE));searchRows=new LinearLayout(this);searchRows.setOrientation(LinearLayout.VERTICAL);results.addView(searchRows);moreResults=button("Ver más resultados",()->search(true));results.addView(moreResults);moreResults.setVisibility(View.GONE);
  root.addView(text("Audio original, sin conversiones. Descarga contenido propio o con permiso.",12,MUTED));
  try{downloadQueue.restore(new JSONArray(getPreferences(0).getString("downloadQueue","[]")));}catch(JSONException ignored){}
  renderQueue();
 }
 private EditText field(String hint){EditText e=new EditText(this);e.setTextColor(Color.WHITE);e.setHintTextColor(MUTED);e.setTextSize(14);e.setHint(hint);e.setSingleLine(true);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GOLD));return e;}
 private void hideKeyboard(){android.view.inputmethod.InputMethodManager m=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(m!=null&&getCurrentFocus()!=null)m.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);}
 private void saveQueue(){try{getPreferences(0).edit().putString("downloadQueue",downloadQueue.json().toString()).apply();}catch(JSONException ignored){}}
 private void controls(){
  boolean edit=!busy&&!lookupBusy;download.setEnabled(edit);downloadAll.setEnabled(edit&&!downloadQueue.pending().isEmpty());searchButton.setEnabled(!lookupBusy);moreResults.setEnabled(!lookupBusy);cancelBatch.setVisibility(busy?View.VISIBLE:View.GONE);
  for(Button b:queueButtons)b.setEnabled(edit);
 }
 private void renderQueue(){
  queueRows.removeAllViews();queueButtons.clear();int pending=downloadQueue.pending().size();queueCount.setText("Tu lista · "+downloadQueue.entries.size()+" canciones");
  if(downloadQueue.entries.isEmpty())queueRows.addView(text("Agrega resultados o pega enlaces. Nada se descarga hasta que pulses Descargar todo.",13,MUTED));
  for(DownloadQueue.Entry e:downloadQueue.entries){
   LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setPadding(0,dp(12),0,dp(12));queueRows.addView(item);
   LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);item.addView(top);ImageView image=cover(48);top.addView(image);covers.bind(image,new Track(e.title,e.url,e.cover,e.artist),null);
   TextView name=text(e.title,14,Color.WHITE);name.setMaxLines(2);name.setEllipsize(android.text.TextUtils.TruncateAt.END);name.setPadding(dp(10),0,0,0);top.addView(name,new LinearLayout.LayoutParams(0,-2,1));
   item.addView(text(e.quality.label,12,GOLD));String state=DownloadQueue.DONE.equals(e.state)?"✓ En tu biblioteca":DownloadQueue.RUNNING.equals(e.state)?"Descargando…":DownloadQueue.FAILED.equals(e.state)?"No se descargó: "+e.error:"Pendiente";item.addView(text(state,12,MUTED));
   LinearLayout actions=new LinearLayout(this);item.addView(actions);if(!DownloadQueue.DONE.equals(e.state)){Button quality=button("Calidad",()->prepareVideo(e.url,e));actions.addView(quality,new LinearLayout.LayoutParams(0,dp(48),1));queueButtons.add(quality);}
   Button remove=button("Quitar",()->{if(busy||lookupBusy)return;downloadQueue.entries.remove(e);saveQueue();renderQueue();});actions.addView(remove,new LinearLayout.LayoutParams(0,dp(48),1));queueButtons.add(remove);
  }
  downloadAll.setText(pending>0?"Descargar todo · "+pending:"Descargar todo");controls();
 }
 private String thumbnail(List<org.schabi.newpipe.extractor.Image> images){return images.isEmpty()?"":images.get(images.size()-1).getUrl();}
 private String duration(long seconds){if(seconds<0)return "Duración no indicada";return String.format(Locale.ROOT,seconds>=3600?"%d:%02d:%02d":"%d:%02d",seconds>=3600?new Object[]{seconds/3600,seconds/60%60,seconds%60}:new Object[]{seconds/60,seconds%60});}
 private void search(boolean more){
  if(lookupBusy)return;String term=more?searchTerm:query.getText().toString().trim();if(term.isEmpty()){toast("Escribe una canción o un artista.");return;}if(more&&!org.schabi.newpipe.extractor.Page.isValid(nextSearchPage))return;
  hideKeyboard();lookupBusy=true;controls();searchStatus.setText(more?"Buscando más resultados…":"Buscando en YouTube…");
  final org.schabi.newpipe.extractor.Page page=nextSearchPage;
  lookups.execute(()->{try{
   org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler q=ServiceList.YouTube.getSearchQHFactory().fromQuery(term,Collections.singletonList("videos"),"");
   List<org.schabi.newpipe.extractor.InfoItem> items;org.schabi.newpipe.extractor.Page next;
   if(more){org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage<org.schabi.newpipe.extractor.InfoItem> p=org.schabi.newpipe.extractor.search.SearchInfo.getMoreItems(ServiceList.YouTube,q,page);items=p.getItems();next=p.getNextPage();}
   else{org.schabi.newpipe.extractor.search.SearchInfo info=org.schabi.newpipe.extractor.search.SearchInfo.getInfo(ServiceList.YouTube,q);items=info.getRelatedItems();next=info.getNextPage();}
   List<StreamInfoItem> videos=new ArrayList<>();for(org.schabi.newpipe.extractor.InfoItem item:items)if(item instanceof StreamInfoItem)videos.add((StreamInfoItem)item);
   ui(()->{if(!more)searchVideos.clear();Set<String> existing=new HashSet<>();for(StreamInfoItem v:searchVideos)existing.add(v.getUrl());for(StreamInfoItem v:videos)if(existing.add(v.getUrl())&&searchVideos.size()<200)searchVideos.add(v);searchTerm=term;nextSearchPage=next;lookupBusy=false;renderSearch();searchStatus.setText(searchVideos.isEmpty()?"No se encontraron videos. Prueba otro nombre.":searchVideos.size()+" resultados · "+term);controls();});
  }catch(Exception e){ui(()->{lookupBusy=false;searchStatus.setText("No se pudo buscar. Comprueba tu conexión y vuelve a intentar.");controls();});}});
 }
 private void renderSearch(){
  searchRows.removeAllViews();for(StreamInfoItem v:searchVideos){
   LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setPadding(0,dp(10),0,dp(16));searchRows.addView(item);
   LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);item.addView(top);ImageView image=cover(88);image.setLayoutParams(new LinearLayout.LayoutParams(dp(112),dp(70)));top.addView(image);covers.bind(image,new Track(v.getName(),v.getUrl(),thumbnail(v.getThumbnails()),v.getUploaderName()),null);
   LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(12),0,0,0);top.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView title=text(v.getName(),14,Color.WHITE);title.setMaxLines(3);title.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(title);labels.addView(text((v.getUploaderName()==null?"YouTube":v.getUploaderName())+" · "+duration(v.getDuration()),12,MUTED));
   item.addView(button("+ Agregar y elegir calidad",()->prepareVideo(v.getUrl(),null)));
  }
  moreResults.setVisibility(org.schabi.newpipe.extractor.Page.isValid(nextSearchPage)&&searchVideos.size()<200?View.VISIBLE:View.GONE);
 }
 private void prepareLink(String raw){try{prepareVideo(normalize(raw),null);}catch(IllegalArgumentException e){toast(e.getMessage());}}
 private void prepareVideo(String raw,DownloadQueue.Entry editing){
  if(busy||lookupBusy){toast("Espera a que termine la tarea actual para editar la lista.");return;}
  final String url;try{url=normalize(raw);}catch(IllegalArgumentException e){toast(e.getMessage());return;}
  if(editing==null)for(DownloadQueue.Entry e:downloadQueue.entries)if(e.url.equals(url)){toast("Esta canción ya está en tu lista.");return;}
  lookupBusy=true;controls();status.setText("Consultando las calidades disponibles…");hideKeyboard();
  lookups.execute(()->{try{StreamInfo info=StreamInfo.getInfo(ServiceList.YouTube,url);List<AudioStream> list=AudioChoice.available(info.getAudioStreams());if(list.isEmpty())throw new IOException("No hay audio compatible.");
   ui(()->{lookupBusy=false;controls();String[] options=new String[list.size()];for(int i=0;i<list.size();i++)options[i]=AudioChoice.of(list.get(i)).label;
    new AlertDialog.Builder(this).setTitle(info.getName()).setItems(options,(d,w)->{if(busy)return;AudioChoice quality=AudioChoice.of(list.get(w));try{if(editing!=null){editing.quality=quality;editing.state=DownloadQueue.PENDING;editing.error="";}else downloadQueue.add(new DownloadQueue.Entry(url,info.getName(),thumbnail(info.getThumbnails()),info.getUploaderName()==null?"":info.getUploaderName(),quality));saveQueue();renderQueue();status.setText("Añadida a tu lista. Pulsa Descargar todo cuando estés listo.");}catch(IllegalStateException e){toast(e.getMessage());}}).setNegativeButton("Cerrar",null).show();status.setText("Elige calidad para añadirla a la lista.");});
  }catch(Exception e){ui(()->{lookupBusy=false;controls();status.setText("No se pudo consultar el audio. Revisa el enlace o vuelve a intentar.");});}});
 }
 private void pasteLinks(){
  if(busy||lookupBusy){toast("Espera a que termine la tarea actual.");return;}EditText input=field("Un enlace de YouTube por línea");input.setSingleLine(false);input.setMinLines(4);input.setMaxLines(8);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
  new AlertDialog.Builder(this).setTitle("Agregar varios enlaces").setMessage("Se añadirá el audio de mayor tasa disponible de cada video. Luego puedes cambiar Calidad en cada canción.").setView(input).setPositiveButton("Agregar",(d,w)->addLinks(input.getText().toString())).setNegativeButton("Cancelar",null).show();
 }
 private void addLinks(String raw){
  if(busy||lookupBusy)return;Set<String> urls=new LinkedHashSet<>();int invalid=0;
  for(String line:raw.split("\\R"))if(!line.trim().isEmpty())try{urls.add(normalize(line));}catch(IllegalArgumentException e){invalid++;}
  if(urls.isEmpty()){toast("No hay enlaces válidos.");return;}if(urls.size()>100){toast("Agrega como máximo 100 enlaces a la vez.");return;}
  final int invalidCount=invalid;lookupBusy=true;controls();status.setText("Preparando enlaces…");
  lookups.execute(()->{List<DownloadQueue.Entry> additions=new ArrayList<>();int errors=invalidCount;
   for(String url:urls){if(Thread.currentThread().isInterrupted())return;try{StreamInfo info=StreamInfo.getInfo(ServiceList.YouTube,url);List<AudioStream> audio=AudioChoice.available(info.getAudioStreams());if(audio.isEmpty())throw new IOException();additions.add(new DownloadQueue.Entry(url,info.getName(),thumbnail(info.getThumbnails()),info.getUploaderName()==null?"":info.getUploaderName(),AudioChoice.of(audio.get(0))));}catch(Exception e){errors++;}}
   final int failed=errors;ui(()->{int added=0,duplicates=0,full=0;for(DownloadQueue.Entry e:additions)try{if(downloadQueue.add(e))added++;else duplicates++;}catch(IllegalStateException e){full++;}lookupBusy=false;saveQueue();renderQueue();status.setText(added+" añadidas · "+duplicates+" ya estaban · "+(failed+full)+" no añadidas. Revisa Calidad antes de descargar.");});
  });
 }
 private void stopBatch(){cancelled=true;AudioDownload d=audioDownload;if(d!=null)d.cancel();status.setText("Deteniendo… Las canciones pendientes quedan en la lista.");cancelBatch.setEnabled(false);}
 private void startBatch(){
  if(busy||lookupBusy)return;final List<DownloadQueue.Entry> batch=downloadQueue.pending();if(batch.isEmpty()){toast("Agrega canciones a tu lista.");return;}cancelled=false;busy=true;cancelBatch.setEnabled(true);controls();status.setText("Preparando "+batch.size()+" canciones…");getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  jobs.execute(()->{int done=0,failed=0;
   try{for(int n=0;n<batch.size();n++){
    if(cancelled||Thread.currentThread().isInterrupted())break;DownloadQueue.Entry e=batch.get(n);final int position=n+1;ui(()->{downloadQueue.update(e.url,DownloadQueue.RUNNING,"");saveQueue();renderQueue();status.setText(position+"/"+batch.size()+" · Consultando "+e.title);});
    File partial=null;
    try{StreamInfo info=StreamInfo.getInfo(ServiceList.YouTube,e.url);if(cancelled)throw new IOException("Cancelado");AudioStream audio=e.quality.resolve(AudioChoice.available(info.getAudioStreams()));
     File dir=new File(getFilesDir(),"music");if(!dir.exists()&&!dir.mkdirs())throw new IOException("No se pudo abrir la carpeta de música.");String base=e.title.replaceAll("[^\\p{L}\\p{N} ._-]","_");if(base.length()>90)base=base.substring(0,90);File dest=new File(dir,base+"-"+System.currentTimeMillis()+"."+audio.getFormat().getSuffix());partial=new File(dest+".part");
     AudioDownload task=new AudioDownload(Net.CLIENT);audioDownload=task;if(cancelled)task.cancel();final long started=System.nanoTime();
     task.download(audio.getContent(),partial,(bytes,total)->{String message=String.format(Locale.ROOT,"%d/%d · %s\n%s · %.1f MB · %.0f KB/s",position,batch.size(),e.title,total>0?(bytes*100/total)+" %":"…",bytes/1048576.0,bytes/1024.0/Math.max(.1,(System.nanoTime()-started)/1e9));ui(()->{if(!cancelled)status.setText(message);});});
     if(cancelled)throw new IOException("Cancelado");if(!partial.renameTo(dest))throw new IOException("No se pudo guardar.");done++;
     ui(()->{tracks.add(new Track(e.title,dest.toURI().toString(),e.cover,e.artist));save();renderSongs();if(player!=null)player.setQueue(tracks);downloadQueue.update(e.url,DownloadQueue.DONE,"");saveQueue();renderQueue();});
    }catch(Exception ex){if(partial!=null)partial.delete();if(cancelled||Thread.currentThread().isInterrupted()){ui(()->{downloadQueue.update(e.url,DownloadQueue.PENDING,"");saveQueue();renderQueue();});break;}failed++;final String reason=ex instanceof IOException&&ex.getMessage()!=null?ex.getMessage():"No disponible o sin conexión. Vuelve a intentar.";ui(()->{downloadQueue.update(e.url,DownloadQueue.FAILED,reason);saveQueue();renderQueue();});}
    finally{audioDownload=null;}
   }}finally{final int completed=done,errors=failed;ui(()->{busy=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);renderQueue();status.setText((cancelled?"Detenido. ":"Lista terminada. ")+completed+" descargadas · "+errors+" fallidas. Las pendientes pueden reintentarse.");});}
  });
 }
 private static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
 private void acceptShare(Intent i){if(Intent.ACTION_SEND.equals(i.getAction())){String s=i.getStringExtra(Intent.EXTRA_TEXT);if(s!=null){link.setText(s);selectTab(2);}}}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);acceptShare(i);}
 private String clock(int ms){return String.format(Locale.ROOT,"%d:%02d",ms/60000,ms/1000%60);}
 private final Runnable tick=new Runnable(){public void run(){if(player!=null){now.setText(player.title());miniTitle.setText(player.title());play.setText(player.playing()?"Pausar":"Reproducir");miniPlay.setText(player.playing()?"Ⅱ":"▶");
   Track current=player.currentTrack();String id=current==null?"empty":current.uri+"\n"+current.cover;
   if(!id.equals(shownCover)){shownCover=id;artist.setText(current==null||current.artist.isEmpty()?"Tu biblioteca":current.artist);covers.bind(playerCover,current,()->{if(player!=null)player.refreshArtwork();});covers.bind(miniCover,current,null);}
if(!seeking){seek.setMax(Math.max(player.duration(),1));seek.setProgress(player.position());}time.setText(clock(player.position())+" / "+clock(player.duration()));if(!player.error.isEmpty()){toast(player.error);player.error="";}}handler.postDelayed(this,500);}};
 @Override public void onDestroy(){getPreferences(0).edit().putInt("tab",selectedTab).apply();if(covers!=null)covers.close();if(updater!=null)updater.close();handler.removeCallbacksAndMessages(null);cancelled=true;AudioDownload d=audioDownload;if(d!=null)d.cancel();jobs.shutdownNow();lookups.shutdownNow();if(bound)unbindService(connection);super.onDestroy();}
}

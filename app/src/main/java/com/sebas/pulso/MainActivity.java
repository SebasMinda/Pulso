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
 private ThemePalette palette;
 private final Map<TextView,Integer> textRoles=new WeakHashMap<>();private final Map<View,Integer> backgroundRoles=new WeakHashMap<>();
 private final Set<View> roundedViews=Collections.newSetFromMap(new WeakHashMap<>());private final Set<Button> themedButtons=Collections.newSetFromMap(new WeakHashMap<>());private final Set<TextView> metalLabels=Collections.newSetFromMap(new WeakHashMap<>());
 private TextView themeName;private Switch automatic;
 private LinearLayout root, songs; private TextView now,status,time; private Button play,download; private SeekBar seek; private EditText link;
 private PlayerService player; private boolean bound, seeking; private final ArrayList<Track> tracks=new ArrayList<>();
 private Updater updater;
 private CoverArt covers;
 private final ViewGroup[] pages=new ViewGroup[4];
 private final TextView[] tabs=new TextView[4];
 private int selectedTab;private boolean navigationReady;
 private ImageView playerCover,miniCover;private TextView artist,miniTitle,libraryCount;private Button miniPlay;
 private Button shuffleButton;private boolean playerExpanded,gestureEligible,gestureConsumed;private float gestureX,gestureY;
 private LinearLayout miniPlayer;private Track coverTarget;private String shownCover="";

 private final Handler handler=new Handler(Looper.getMainLooper()); private final ExecutorService jobs=Executors.newSingleThreadExecutor();
 private final DownloadQueue downloadQueue=new DownloadQueue();
 private final ExecutorService lookups=Executors.newSingleThreadExecutor();
 private EditText query;private Button searchButton,moreResults,downloadAll,cancelBatch,listToggle;private TextView searchStatus,queueCount;private LinearLayout searchRows,queueRows,linksPanel,queuePanel,resultPanel,downloadProgress;
 private final List<Button> queueButtons=new ArrayList<>();private final List<StreamInfoItem> searchVideos=new ArrayList<>();private String searchTerm="";private org.schabi.newpipe.extractor.Page nextSearchPage;private boolean lookupBusy,bulkLoading;private volatile boolean lookupCancelled;
 private File exportFile; private volatile AudioDownload audioDownload; private volatile boolean cancelled,busy;
 private final ServiceConnection connection=new ServiceConnection(){
  @Override public void onServiceConnected(ComponentName n,IBinder b){player=((PlayerService.LocalBinder)b).get();bound=true;player.setQueue(tracks);}
  @Override public void onServiceDisconnected(ComponentName n){player=null;bound=false;}
 };
 @Override public void onCreate(Bundle state){
  super.onCreate(state);loadTheme();setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);getWindow().setStatusBarColor(palette.background);getWindow().setNavigationBarColor(palette.background);
  load();covers=new CoverArt(this);covers.setAccent(palette.accent);NewPipe.init(new Net());build();bindService(new Intent(this,PlayerService.class),connection,BIND_AUTO_CREATE);
  if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},9);
  acceptShare(getIntent());handler.post(tick);
 }
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density);}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);textRoles.put(t,color);t.setTextColor(themeColor(color));t.setPadding(0,dp(5),0,dp(5));return t;}
 private int themeColor(int role){if(role==BG)return palette.background;if(role==CARD)return palette.card();if(role==GOLD)return palette.accent;if(role==RED)return palette.button;if(role==MUTED)return palette.muted();if(role==Color.WHITE)return ThemePalette.foreground(palette.background);return role;}
 private void background(View v,int role,boolean round){backgroundRoles.put(v,role);if(round)roundedViews.add(v);else roundedViews.remove(v);paintBackground(v,role,round);}
 private void paintBackground(View v,int role,boolean round){GradientDrawable d=new GradientDrawable();d.setColor(themeColor(role));if(round)d.setCornerRadius(dp(18));v.setBackground(d);}
 private void rounded(View v,int color){background(v,color,true);}
 private GradientDrawable metallicRed(boolean selected){
  int base=selected?ThemePalette.mix(palette.background,palette.button,.35f):palette.button;
  int[] shades=palette.original()?(selected?new int[]{0xff3c1c1f,0xff2a0f14,0xff1b0c0f}:new int[]{0xff6f2731,RED,0xff300d15}):new int[]{ThemePalette.mix(base,Color.WHITE,.16f),base,ThemePalette.mix(base,Color.BLACK,.30f)};
  GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,shades);d.setCornerRadius(dp(18));d.setStroke(dp(1),palette.original()?(selected?0xff755f3a:0xff7f4243):ThemePalette.mix(base,selected?palette.accent:Color.WHITE,.32f));return d;
 }
 private void paintGold(TextView view){int h=view.getHeight();if(h<=0)return;int[] colors=palette.original()?new int[]{0xffa98b53,0xffe1cb9c,GOLD,0xffa98b53}:new int[]{ThemePalette.mix(palette.accent,Color.BLACK,.16f),ThemePalette.mix(palette.accent,Color.WHITE,.30f),palette.accent,ThemePalette.mix(palette.accent,Color.BLACK,.16f)};view.getPaint().setShader(new android.graphics.LinearGradient(0,0,0,h,colors,new float[]{0,.38f,.58f,1},android.graphics.Shader.TileMode.CLAMP));view.invalidate();}
 private void goldMetal(TextView view){metalLabels.add(view);view.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->paintGold(view));}
 private void paintButton(Button b){
  b.setTextColor(palette.original()?0xfff1e7da:ThemePalette.foreground(palette.button));android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();GradientDrawable disabled=metallicRed(false);disabled.setAlpha(110);states.addState(new int[]{-android.R.attr.state_enabled},disabled);states.addState(new int[]{},metallicRed(false));GradientDrawable mask=new GradientDrawable();mask.setColor(Color.WHITE);mask.setCornerRadius(dp(18));b.setBackground(new android.graphics.drawable.RippleDrawable(android.content.res.ColorStateList.valueOf((palette.accent&0xffffff)|0x26000000),states,mask));
 }
 private Button button(String s,Runnable r){Button b=new Button(this);b.setText(s);b.setAllCaps(false);themedButtons.add(b);paintButton(b);b.setOnClickListener(v->{pressFeedback(b);r.run();});return b;}
 private void spacedButton(LinearLayout parent,Button b){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(10),0,0);b.setMinHeight(dp(48));parent.addView(b,lp);}
 private void togglePanel(View panel){boolean opening=panel.getVisibility()!=View.VISIBLE;panel.setVisibility(opening?View.VISIBLE:View.GONE);if(opening)pages[2].post(()->((ScrollView)pages[2]).smoothScrollTo(0,panel.getTop()));}
 private void downloadStatus(String message){status.setText(message);downloadProgress.setVisibility(View.VISIBLE);}

 private void loadTheme(){boolean custom=getPreferences(0).getBoolean("theme.custom",false);palette=custom?new ThemePalette(getPreferences(0).getInt("theme.background",BG),getPreferences(0).getInt("theme.button",RED),getPreferences(0).getInt("theme.accent",GOLD)):new ThemePalette(BG,RED,GOLD);}
 private void saveTheme(ThemePalette colors,boolean custom){palette=colors;android.content.SharedPreferences.Editor e=getPreferences(0).edit().putBoolean("theme.custom",custom);if(custom)e.putInt("theme.background",colors.background).putInt("theme.button",colors.button).putInt("theme.accent",colors.accent);e.apply();applyTheme();}
 private void paintSeek(){seek.setProgressTintList(android.content.res.ColorStateList.valueOf(palette.accent));seek.setProgressBackgroundTintList(android.content.res.ColorStateList.valueOf(palette.muted()));GradientDrawable thumb=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{ThemePalette.mix(palette.accent,Color.WHITE,.30f),palette.accent,ThemePalette.mix(palette.accent,Color.BLACK,.30f)});thumb.setShape(GradientDrawable.OVAL);thumb.setSize(dp(20),dp(20));thumb.setStroke(dp(1),ThemePalette.mix(palette.accent,Color.BLACK,.30f));seek.setThumb(thumb);}
 private void paintSwitch(){automatic.setTextColor(themeColor(MUTED));automatic.setThumbTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{palette.accent,palette.muted()}));automatic.setTrackTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_checked},new int[]{}},new int[]{palette.button,palette.card()}));}
 private void applyTheme(){
  getWindow().setStatusBarColor(palette.background);getWindow().setNavigationBarColor(palette.background);int flags=getWindow().getDecorView().getSystemUiVisibility();int light=View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR|View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;getWindow().getDecorView().setSystemUiVisibility(ThemePalette.foreground(palette.background)==0xff101010?flags|light:flags&~light);
  for(Map.Entry<View,Integer> e:backgroundRoles.entrySet())paintBackground(e.getKey(),e.getValue(),roundedViews.contains(e.getKey()));
  for(Map.Entry<TextView,Integer> e:textRoles.entrySet()){TextView v=e.getKey();v.setTextColor(themeColor(e.getValue()));if(v instanceof EditText){((EditText)v).setHintTextColor(palette.muted());v.setBackgroundTintList(android.content.res.ColorStateList.valueOf(palette.accent));}}
  for(Button b:themedButtons)paintButton(b);for(TextView v:metalLabels)paintGold(v);paintSeek();paintSwitch();
  for(int i:new int[]{0,2,3}){tabs[i].setTextColor(themeColor(i==selectedTab?GOLD:MUTED));if(i==selectedTab)tabs[i].setBackground(metallicRed(true));}
  covers.setAccent(palette.accent);refreshShuffle();themeName.setText(getPreferences(0).getBoolean("theme.custom",false)?"Tema actual: Personalizado":"Tema actual: Pulso clásico");
 }
 private void buildAppearance(){LinearLayout appearance=section();appearance.addView(text("Apariencia",21,Color.WHITE));themeName=text(getPreferences(0).getBoolean("theme.custom",false)?"Tema actual: Personalizado":"Tema actual: Pulso clásico",13,GOLD);appearance.addView(themeName);
  appearance.addView(text("Fondo: negro · Botones: rojo · Detalles: dorado en el tema clásico.",12,MUTED));spacedButton(appearance,button("Escoger tema",()->new AlertDialog.Builder(this).setTitle("Tema de Pulso").setSingleChoiceItems(new String[]{"Pulso clásico · predeterminado","Personalizado"},getPreferences(0).getBoolean("theme.custom",false)?1:0,(d,w)->{d.dismiss();if(w==0)saveTheme(new ThemePalette(BG,RED,GOLD),false);else saveTheme(new ThemePalette(getPreferences(0).getInt("theme.background",palette.background),getPreferences(0).getInt("theme.button",palette.button),getPreferences(0).getInt("theme.accent",palette.accent)),true);}).setNegativeButton("Cerrar",null).show()));
  spacedButton(appearance,button("Fondo y superficies",()->chooseThemeColor(0)));spacedButton(appearance,button("Botones y apartados activos",()->chooseThemeColor(1)));spacedButton(appearance,button("Detalles, iconos y barra de progreso",()->chooseThemeColor(2)));
 }
 private void chooseThemeColor(int role){
  int color=role==0?palette.background:role==1?palette.button:palette.accent;final int[] chosen={color};
  LinearLayout content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(20),dp(12),dp(20),dp(12));
  View swatch=new View(this);GradientDrawable preview=new GradientDrawable();preview.setCornerRadius(dp(16));preview.setColor(color);preview.setStroke(dp(1),Color.GRAY);swatch.setBackground(preview);content.addView(swatch,new LinearLayout.LayoutParams(-1,dp(54)));
  EditText hex=new EditText(this);hex.setSingleLine(true);hex.setHint("#RRGGBB");hex.setText(ThemePalette.hex(color));hex.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);content.addView(hex);
  String[] names={"Rojo","Verde","Azul"};SeekBar[] channels=new SeekBar[3];TextView[] labels=new TextView[3];final boolean[] syncing={false};
  Runnable update=()->{chosen[0]=Color.rgb(channels[0].getProgress(),channels[1].getProgress(),channels[2].getProgress());preview.setColor(chosen[0]);for(int i=0;i<3;i++)labels[i].setText(names[i]+": "+channels[i].getProgress());syncing[0]=true;hex.setText(ThemePalette.hex(chosen[0]));syncing[0]=false;};
  for(int i=0;i<3;i++){labels[i]=new TextView(this);content.addView(labels[i]);channels[i]=new SeekBar(this);channels[i].setMax(255);channels[i].setProgress((color>>(16-8*i))&255);content.addView(channels[i]);}
  for(SeekBar channel:channels)channel.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar bar,int value,boolean user){if(user)update.run();}public void onStartTrackingTouch(SeekBar bar){}public void onStopTrackingTouch(SeekBar bar){}});update.run();
  hex.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void onTextChanged(CharSequence s,int a,int b,int c){if(syncing[0])return;try{chosen[0]=ThemePalette.parse(s.toString());preview.setColor(chosen[0]);for(int i=0;i<3;i++){channels[i].setProgress((chosen[0]>>(16-8*i))&255);labels[i].setText(names[i]+": "+channels[i].getProgress());}hex.setError(null);}catch(IllegalArgumentException ignored){}}public void afterTextChanged(android.text.Editable e){}});
  ScrollView scroll=new ScrollView(this);scroll.addView(content);AlertDialog dialog=new AlertDialog.Builder(this).setTitle(role==0?"Color de fondo":role==1?"Color de botones":"Color de detalles").setView(scroll).setNegativeButton("Cancelar",null).setPositiveButton("Guardar",null).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{int value=ThemePalette.parse(hex.getText().toString());saveTheme(new ThemePalette(role==0?value:palette.background,role==1?value:palette.button,role==2?value:palette.accent),true);dialog.dismiss();}catch(IllegalArgumentException e){hex.setError(e.getMessage());}}));dialog.show();
 }
 private boolean motionEnabled(){return android.animation.ValueAnimator.areAnimatorsEnabled();}
 private void pressFeedback(View view){
  view.animate().cancel();view.setScaleX(1);view.setScaleY(1);view.setAlpha(1);
  if(!motionEnabled()||!view.isShown())return;
  view.setScaleX(.96f);view.setScaleY(.96f);view.animate().scaleX(1).scaleY(1).setStartDelay(0).setDuration(170).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
 }
 private void reveal(View view,float offsetX,float offsetY){
  view.animate().cancel();view.setScaleX(1);view.setScaleY(1);view.setAlpha(1);view.setTranslationX(0);view.setTranslationY(0);
  if(!motionEnabled()||!view.isShown())return;
  view.setAlpha(.55f);view.setTranslationX(offsetX);view.setTranslationY(offsetY);
  view.animate().alpha(1).translationX(0).translationY(0).setStartDelay(0).setDuration(200).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();
 }
 private void updateAnimatedText(TextView view,String value){
  if(value.contentEquals(view.getText()))return;view.setText(value);reveal(view,0,0);
 }
 private LinearLayout section(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(14),dp(18),dp(14));rounded(box,CARD);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(12),0,dp(8));root.addView(box,lp);return box;}
 private ImageView cover(int size){ImageView image=new ImageView(this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setImageResource(R.drawable.cover_placeholder);rounded(image,CARD);image.setClipToOutline(true);image.setContentDescription("Portada de la canción");image.setLayoutParams(new LinearLayout.LayoutParams(dp(size),dp(size)));return image;}
 private void page(int index,FrameLayout container){
  ScrollView sc=new ScrollView(this);sc.setFillViewport(true);sc.setClipToPadding(false);background(sc,BG,false);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),dp(12),dp(20),dp(24));sc.addView(root);container.addView(sc,new FrameLayout.LayoutParams(-1,-1));pages[index]=sc;
 }
 private void heading(String title,String subtitle){TextView h=text(title,30,Color.WHITE);h.setTypeface(null,Typeface.BOLD);root.addView(h);root.addView(text(subtitle,14,MUTED));}
 private void selectTab(int index){
  if(index==1){setPlayerExpanded(true,true);return;}if(index!=0&&index!=2&&index!=3)index=0;
  boolean changed=navigationReady&&selectedTab!=index;int previous=selectedTab;
  if(playerExpanded)setPlayerExpanded(false,false);selectedTab=index;
  for(int i=0;i<4;i++){if(i!=1){pages[i].animate().cancel();pages[i].setAlpha(1);pages[i].setTranslationX(0);pages[i].setTranslationY(0);pages[i].setVisibility(i==index?View.VISIBLE:View.GONE);}if(tabs[i]!=null){tabs[i].setTextColor(themeColor(i==index?GOLD:MUTED));tabs[i].setTypeface(null,i==index?Typeface.BOLD:Typeface.NORMAL);if(i==index)tabs[i].setBackground(metallicRed(true));else rounded(tabs[i],BG);tabs[i].setSelected(i==index);}}
  pages[1].setVisibility(View.GONE);miniPlayer.setVisibility(View.VISIBLE);
  if(changed){reveal(pages[index],dp(index>previous?12:-12),0);pressFeedback(tabs[index]);}
 }
 private void setPlayerExpanded(boolean expanded,boolean animate){
  if(pages[1]==null||miniPlayer==null)return;animate=animate&&motionEnabled();playerExpanded=expanded;pages[1].animate().cancel();pages[1].setAlpha(1);miniPlayer.animate().cancel();miniPlayer.setAlpha(1);
  if(expanded){hideKeyboard();pages[1].setVisibility(View.VISIBLE);pages[1].bringToFront();miniPlayer.setVisibility(View.GONE);if(animate){pages[1].setTranslationY(Math.max(dp(160),pages[1].getHeight()));pages[1].animate().translationY(0).setStartDelay(0).setDuration(300).setInterpolator(new android.view.animation.DecelerateInterpolator()).start();}else pages[1].setTranslationY(0);}
  else{miniPlayer.setVisibility(View.VISIBLE);if(animate){reveal(miniPlayer,0,0);pages[1].animate().translationY(Math.max(dp(160),pages[1].getHeight())).setStartDelay(0).setDuration(240).setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator()).withEndAction(()->{if(!playerExpanded){pages[1].setVisibility(View.GONE);pages[1].setTranslationY(0);}}).start();}else{pages[1].setVisibility(View.GONE);pages[1].setTranslationY(0);}}
 }
 private boolean inside(View v,float x,float y){android.graphics.Rect r=new android.graphics.Rect();return v!=null&&v.getGlobalVisibleRect(r)&&r.contains((int)x,(int)y);}
 @Override public boolean dispatchTouchEvent(MotionEvent e){
  int action=e.getActionMasked();
  if(action==MotionEvent.ACTION_DOWN){gestureX=e.getRawX();gestureY=e.getRawY();gestureConsumed=false;gestureEligible=playerExpanded?inside(pages[1],gestureX,gestureY)&&!inside(seek,gestureX,gestureY):inside(miniPlayer,gestureX,gestureY);}
  if(action==MotionEvent.ACTION_MOVE&&gestureEligible&&!gestureConsumed){float dx=e.getRawX()-gestureX,dy=e.getRawY()-gestureY;if(Math.abs(dy)>dp(48)&&Math.abs(dy)>Math.abs(dx)*1.5f&&(playerExpanded?dy>0:dy<0)){MotionEvent cancel=MotionEvent.obtain(e);cancel.setAction(MotionEvent.ACTION_CANCEL);super.dispatchTouchEvent(cancel);cancel.recycle();gestureConsumed=true;setPlayerExpanded(!playerExpanded,true);}}
  if(gestureConsumed){if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL)gestureEligible=false;return true;}
  return super.dispatchTouchEvent(e);
 }
 @Override public void onBackPressed(){if(playerExpanded)setPlayerExpanded(false,true);else super.onBackPressed();}
 private void togglePlayback(){if(player!=null){startService(new Intent(this,PlayerService.class));player.toggle();}}
 private void build(){
  LinearLayout shell=new LinearLayout(this);shell.setOrientation(LinearLayout.VERTICAL);background(shell,BG,false);shell.setFocusableInTouchMode(true);setContentView(shell);
  shell.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(0,insets.getSystemWindowInsetTop(),0,insets.getSystemWindowInsetBottom());return insets;});
  LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);brand.setPadding(dp(20),dp(8),dp(20),dp(4));shell.addView(brand);
  TextView logo=text("pulso",28,GOLD);logo.setTypeface(null,Typeface.BOLD);goldMetal(logo);brand.addView(logo,new LinearLayout.LayoutParams(0,-2,1));brand.addView(text("TU MÚSICA",10,MUTED));
  FrameLayout container=new FrameLayout(this);shell.addView(container,new LinearLayout.LayoutParams(-1,0,1));

  page(0,container);heading("Tu biblioteca","Tus canciones, siempre contigo.");
  LinearLayout libraryRow=new LinearLayout(this);libraryRow.setGravity(Gravity.CENTER_VERTICAL);root.addView(libraryRow);libraryCount=text("0 canciones",13,MUTED);libraryRow.addView(libraryCount,new LinearLayout.LayoutParams(0,-2,1));libraryRow.addView(button("+ Importar",this::pick));
  songs=new LinearLayout(this);songs.setOrientation(LinearLayout.VERTICAL);root.addView(songs);root.addView(text("MP3 · M4A · FLAC · WAV · WebM/Opus",11,MUTED));renderSongs();

  FrameLayout playerPage=new FrameLayout(this);background(playerPage,BG,false);container.addView(playerPage,new FrameLayout.LayoutParams(-1,-1));pages[1]=playerPage;
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(20),0,dp(20),dp(8));playerPage.addView(root,new FrameLayout.LayoutParams(-1,-1));
  TextView handle=text("━",24,GOLD);handle.setPadding(0,0,0,0);handle.setGravity(Gravity.CENTER);handle.setContentDescription("Reducir reproductor");handle.setOnClickListener(v->setPlayerExpanded(false,true));root.addView(handle,new LinearLayout.LayoutParams(-1,dp(32)));
  LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(8),dp(16),dp(12));rounded(box,CARD);root.addView(box,new LinearLayout.LayoutParams(-1,0,1));
  FrameLayout artSpace=new FrameLayout(this);LinearLayout.LayoutParams spaceLp=new LinearLayout.LayoutParams(-1,0,1);spaceLp.setMargins(0,0,0,dp(8));box.addView(artSpace,spaceLp);
  playerCover=cover(0);artSpace.addView(playerCover,new FrameLayout.LayoutParams(0,0,Gravity.CENTER));
  artSpace.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{int size=Math.max(0,Math.min(dp(300),Math.min(r-l,b-t)));FrameLayout.LayoutParams lp=(FrameLayout.LayoutParams)playerCover.getLayoutParams();if(lp.width!=size||lp.height!=size){lp.width=size;lp.height=size;playerCover.setLayoutParams(lp);}});
  now=text("Elige una canción",21,Color.WHITE);now.setTypeface(null,Typeface.BOLD);now.setMaxLines(2);now.setEllipsize(android.text.TextUtils.TruncateAt.END);box.addView(now);artist=text("Tu biblioteca",14,MUTED);artist.setMaxLines(1);artist.setEllipsize(android.text.TextUtils.TruncateAt.END);box.addView(artist);
  seek=new SeekBar(this);paintSeek();box.addView(seek,new LinearLayout.LayoutParams(-1,dp(40)));time=text("0:00 / 0:00",12,MUTED);box.addView(time);
  seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){}public void onStartTrackingTouch(SeekBar s){seeking=true;}public void onStopTrackingTouch(SeekBar s){if(player!=null)player.seek(s.getProgress());seeking=false;}});
  LinearLayout controls=new LinearLayout(this);controls.setGravity(Gravity.CENTER);box.addView(controls);
  Button prev=button("◀",()->{if(player!=null)player.previous();});play=button("Reproducir",this::togglePlayback);Button next=button("▶",()->{if(player!=null)player.next();});prev.setContentDescription("Canción anterior");next.setContentDescription("Canción siguiente");
  for(Button b:new Button[]{prev,play,next}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(56),b==play?2:1);lp.setMargins(dp(3),dp(4),dp(3),dp(4));controls.addView(b,lp);}
  shuffleButton=button("Aleatorio: desactivado",()->{if(player!=null){player.setShuffle(!player.shuffleEnabled());refreshShuffle();}});box.addView(shuffleButton,new LinearLayout.LayoutParams(-1,dp(48)));

  page(2,container);buildDownloads();

  page(3,container);heading("Ajustes","Pulso, a tu manera.");buildAppearance();
  LinearLayout updates=section();updates.addView(text("Actualizaciones",21,Color.WHITE));TextView updateStatus=text("Pulso "+BuildConfig.VERSION_NAME,13,MUTED);updates.addView(updateStatus);
  Button updateButton=button("Buscar actualizaciones",()->{});updates.addView(updateButton);updater=new Updater(this,updateButton,updateStatus);
  automatic=new Switch(this);automatic.setText("Avisar al abrir Pulso");paintSwitch();automatic.setChecked(updater.automatic());automatic.setOnCheckedChangeListener((v,value)->updater.automatic(value));updates.addView(automatic);updater.checkOnLaunch();
  LinearLayout about=section();about.addView(text("Tu música es tuya",21,Color.WHITE));about.addView(text("Importa tus archivos FLAC y otros formatos. Si una canción no tiene portada, puedes elegir una desde su menú ⋮.",14,MUTED));about.addView(text("Pulso "+BuildConfig.VERSION_NAME+" · Hecha para escuchar",12,GOLD));

  miniPlayer=new LinearLayout(this);miniPlayer.setGravity(Gravity.CENTER_VERTICAL);miniPlayer.setContentDescription("Reproductor: desliza hacia arriba para expandir");miniPlayer.setOnClickListener(v->setPlayerExpanded(true,true));miniPlayer.setPadding(dp(12),dp(8),dp(12),dp(8));rounded(miniPlayer,CARD);LinearLayout.LayoutParams miniLp=new LinearLayout.LayoutParams(-1,-2);miniLp.setMargins(dp(12),dp(4),dp(12),dp(8));shell.addView(miniPlayer,miniLp);
  miniCover=cover(44);miniPlayer.addView(miniCover);miniCover.setOnClickListener(v->setPlayerExpanded(true,true));miniTitle=text("Elige una canción",13,Color.WHITE);miniTitle.setMaxLines(1);miniTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);miniTitle.setPadding(dp(12),0,dp(8),0);miniPlayer.addView(miniTitle,new LinearLayout.LayoutParams(0,-2,1));miniTitle.setOnClickListener(v->setPlayerExpanded(true,true));miniPlay=button("▶",this::togglePlayback);miniPlay.setContentDescription("Reproducir o pausar");miniPlayer.addView(miniPlay,new LinearLayout.LayoutParams(dp(56),dp(48)));
  LinearLayout navigation=new LinearLayout(this);navigation.setPadding(dp(4),dp(2),dp(4),dp(8));shell.addView(navigation);
  String[] labels={"Biblioteca","Descargas","Ajustes"};String[] icons={"♫","↓","⚙"};int[] destinations={0,2,3};
  for(int i=0;i<3;i++){final int tab=destinations[i];TextView item=text(icons[i]+"\n"+labels[i],11,MUTED);item.setGravity(Gravity.CENTER);item.setMinHeight(dp(56));item.setContentDescription(labels[i]);item.setOnClickListener(v->selectTab(tab));tabs[tab]=item;navigation.addView(item,new LinearLayout.LayoutParams(0,-2,1));}
  int initialTab=getPreferences(0).getInt("tab",0);selectTab(initialTab==1?0:initialTab);navigationReady=true;applyTheme();
 }
 private void renderSongs(){songs.removeAllViews();if(navigationReady)songs.post(()->reveal(songs,0,dp(8)));libraryCount.setText(tracks.size()+(tracks.size()==1?" canción":" canciones"));if(tracks.isEmpty()){LinearLayout empty=new LinearLayout(this);empty.setOrientation(LinearLayout.VERTICAL);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(20),dp(40),dp(20),dp(40));ImageView art=cover(100);empty.addView(art);empty.addView(text("Tu próxima canción te espera",19,Color.WHITE));empty.addView(text("Importa música o añade un enlace en Descargas.",14,MUTED));songs.addView(empty);return;}
  for(int i=0;i<tracks.size();i++){final int n=i;Track t=tracks.get(i);LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(dp(10),dp(10),dp(6),dp(10));rounded(row,CARD);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(8),0,0);songs.addView(row,lp);
   ImageView image=cover(58);row.addView(image);covers.bind(image,t,null);
   LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);titles.setPadding(dp(12),0,dp(4),0);row.addView(titles,new LinearLayout.LayoutParams(0,-2,1));TextView title=text(t.title,15,Color.WHITE);title.setTypeface(null,Typeface.BOLD);title.setMaxLines(2);title.setEllipsize(android.text.TextUtils.TruncateAt.END);titles.addView(title);TextView subtitle=text(t.artist.isEmpty()?"Audio local":t.artist,12,MUTED);subtitle.setMaxLines(1);subtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);titles.addView(subtitle);
   View.OnClickListener listen=v->{if(player!=null){startService(new Intent(this,PlayerService.class));player.setQueue(tracks);player.play(n);}};image.setOnClickListener(listen);titles.setOnClickListener(listen);
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
  heading("Descargas","Busca tu próxima canción.");
  LinearLayout find=section();find.addView(text("Buscar en YouTube",21,Color.WHITE));
  query=field("Canción o artista");query.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);find.addView(query);
  searchButton=button("Buscar",()->search(false));spacedButton(find,searchButton);
  query.setOnEditorActionListener((v,action,event)->{if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){search(false);return true;}return false;});
  searchStatus=text("",12,MUTED);find.addView(searchStatus);
  LinearLayout shortcuts=new LinearLayout(this);find.addView(shortcuts);
  Button linksToggle=button("Por enlace",()->togglePanel(linksPanel));listToggle=button("Tu lista",()->togglePanel(queuePanel));
  for(Button b:new Button[]{linksToggle,listToggle}){LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,-2,1);lp.setMargins(b==listToggle?dp(6):0,dp(10),b==linksToggle?dp(6):0,0);shortcuts.addView(b,lp);}
  downloadProgress=section();status=text("",13,MUTED);downloadProgress.addView(status);cancelBatch=button("Detener descargas",this::stopBatch);spacedButton(downloadProgress,cancelBatch);cancelBatch.setVisibility(View.GONE);downloadProgress.addView(text("Mantén Pulso abierta durante la descarga.",12,MUTED));downloadProgress.setVisibility(View.GONE);
  resultPanel=section();resultPanel.addView(text("Resultados",19,Color.WHITE));searchRows=new LinearLayout(this);searchRows.setOrientation(LinearLayout.VERTICAL);resultPanel.addView(searchRows);moreResults=button("Ver más resultados",()->search(true));spacedButton(resultPanel,moreResults);moreResults.setVisibility(View.GONE);resultPanel.setVisibility(View.GONE);
  linksPanel=section();linksPanel.addView(text("También por enlace",19,Color.WHITE));link=field("https://youtu.be/…");link.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);linksPanel.addView(link);
  download=button("Agregar enlace a la lista",()->prepareLink(link.getText().toString()));spacedButton(linksPanel,download);spacedButton(linksPanel,button("Pegar varios enlaces…",this::pasteLinks));linksPanel.setVisibility(View.GONE);
  queuePanel=section();queueCount=text("Tu lista",21,Color.WHITE);queuePanel.addView(queueCount);queuePanel.addView(text("Elige la calidad de cada canción antes de descargar.",12,MUTED));queueRows=new LinearLayout(this);queueRows.setOrientation(LinearLayout.VERTICAL);queuePanel.addView(queueRows);
  downloadAll=button("Descargar todo",this::startBatch);spacedButton(queuePanel,downloadAll);queuePanel.setVisibility(View.GONE);
  try{downloadQueue.restore(new JSONArray(getPreferences(0).getString("downloadQueue","[]")));}catch(JSONException ignored){}
  renderQueue();
 }
 private EditText field(String hint){EditText e=new EditText(this);textRoles.put(e,Color.WHITE);e.setTextColor(themeColor(Color.WHITE));e.setHintTextColor(themeColor(MUTED));e.setTextSize(14);e.setHint(hint);e.setSingleLine(true);e.setBackgroundTintList(android.content.res.ColorStateList.valueOf(palette.accent));return e;}
 private void hideKeyboard(){android.view.inputmethod.InputMethodManager m=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(m!=null&&getCurrentFocus()!=null)m.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),0);}
 private void scrollToSection(View child){pages[2].post(()->((ScrollView)pages[2]).smoothScrollTo(0,((View)child.getParent()).getTop()));}
 private void renderQueueKeepingPosition(){
  final ScrollView page=(ScrollView)pages[2];final int y=page.getScrollY(),x=page.getScrollX();final View results=(View)searchRows.getParent();final int before=results.getTop();final boolean showingResults=y>=before-dp(24);
  renderQueue();page.getViewTreeObserver().addOnPreDrawListener(new android.view.ViewTreeObserver.OnPreDrawListener(){public boolean onPreDraw(){if(page.getViewTreeObserver().isAlive())page.getViewTreeObserver().removeOnPreDrawListener(this);int delta=showingResults?results.getTop()-before:0;page.scrollTo(x,Math.max(0,y+delta));return true;}});
 }
 private void confirmAdded(boolean edited){reveal(searchStatus,0,0);pressFeedback(downloadAll);Toast.makeText(this,edited?"✓ Calidad actualizada":"✓ Añadida a tu lista",Toast.LENGTH_SHORT).show();}
 private void saveLibraryAndQueue(){try{JSONArray a=new JSONArray();for(Track t:tracks)a.put(t.json());getPreferences(0).edit().putString("tracks",a.toString()).putString("downloadQueue",downloadQueue.json().toString()).apply();}catch(JSONException ignored){}}
 private void saveQueue(){try{getPreferences(0).edit().putString("downloadQueue",downloadQueue.json().toString()).apply();}catch(JSONException ignored){}}
 private void controls(){
  boolean edit=!busy&&!lookupBusy;download.setEnabled(edit);downloadAll.setEnabled(edit&&!downloadQueue.pending().isEmpty());searchButton.setEnabled(!lookupBusy);moreResults.setEnabled(!lookupBusy);cancelBatch.setText(bulkLoading?"Detener preparación":"Detener descargas");cancelBatch.setVisibility(busy||bulkLoading?View.VISIBLE:View.GONE);
  for(Button b:queueButtons)b.setEnabled(edit);
 }
 private void renderQueue(){
  queueRows.removeAllViews();queueButtons.clear();int pending=downloadQueue.pending().size();listToggle.setText("Tu lista · "+downloadQueue.entries.size());queueCount.setText("Tu lista · "+downloadQueue.entries.size()+" canciones");
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
   ui(()->{if(!more)searchVideos.clear();Set<String> existing=new HashSet<>();for(StreamInfoItem v:searchVideos)existing.add(v.getUrl());for(StreamInfoItem v:videos)if(existing.add(v.getUrl())&&searchVideos.size()<200)searchVideos.add(v);searchTerm=term;nextSearchPage=next;lookupBusy=false;renderSearch();if(!more&&!searchVideos.isEmpty())scrollToSection(searchRows);searchStatus.setText(searchVideos.isEmpty()?"No se encontraron videos. Prueba otro nombre.":searchVideos.size()+" resultados · "+term);controls();});
  }catch(Exception e){ui(()->{lookupBusy=false;searchStatus.setText("No se pudo buscar. Comprueba tu conexión y vuelve a intentar.");controls();});}});
 }
 private void renderSearch(){
  resultPanel.setVisibility(searchVideos.isEmpty()?View.GONE:View.VISIBLE);searchRows.removeAllViews();searchRows.post(()->reveal(searchRows,0,dp(8)));for(StreamInfoItem v:searchVideos){
   LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setPadding(0,dp(10),0,dp(16));searchRows.addView(item);
   LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);item.addView(top);ImageView image=cover(88);image.setLayoutParams(new LinearLayout.LayoutParams(dp(112),dp(70)));top.addView(image);covers.bind(image,new Track(v.getName(),v.getUrl(),thumbnail(v.getThumbnails()),v.getUploaderName()),null);
   LinearLayout labels=new LinearLayout(this);labels.setOrientation(LinearLayout.VERTICAL);labels.setPadding(dp(12),0,0,0);top.addView(labels,new LinearLayout.LayoutParams(0,-2,1));TextView title=text(v.getName(),14,Color.WHITE);title.setMaxLines(3);title.setEllipsize(android.text.TextUtils.TruncateAt.END);labels.addView(title);labels.addView(text((v.getUploaderName()==null?"YouTube":v.getUploaderName())+" · "+duration(v.getDuration()),12,MUTED));
   spacedButton(item,button("Seleccionar",()->new AlertDialog.Builder(this).setTitle(v.getName()).setItems(new String[]{"Agregar al listado","Descargar solo esta"},(d,w)->prepareVideo(v.getUrl(),null,w==1)).setNegativeButton("Cancelar",null).show()));
  }
  moreResults.setVisibility(org.schabi.newpipe.extractor.Page.isValid(nextSearchPage)&&searchVideos.size()<200?View.VISIBLE:View.GONE);
 }
 private void prepareLink(String raw){try{prepareVideo(normalize(raw),null);}catch(IllegalArgumentException e){toast(e.getMessage());}}
 private void prepareVideo(String raw,DownloadQueue.Entry editing){prepareVideo(raw,editing,false);}
 private void prepareVideo(String raw,DownloadQueue.Entry editing,boolean direct){
  if(busy||lookupBusy){toast("Espera a que termine la tarea actual para editar la lista.");return;}
  final String url;try{url=normalize(raw);}catch(IllegalArgumentException e){toast(e.getMessage());return;}
  if(!direct&&editing==null)for(DownloadQueue.Entry e:downloadQueue.entries)if(e.url.equals(url)){toast("Esta canción ya está en tu lista.");return;}
  lookupBusy=true;controls();downloadStatus("Consultando las calidades disponibles…");hideKeyboard();query.clearFocus();link.clearFocus();((View)pages[2].getParent().getParent()).requestFocus();
  lookups.execute(()->{try{StreamInfo info=StreamInfo.getInfo(ServiceList.YouTube,url);List<AudioStream> list=AudioChoice.available(info.getAudioStreams());if(list.isEmpty())throw new IOException("No hay audio compatible.");
   ui(()->{lookupBusy=false;controls();String[] options=new String[list.size()];for(int i=0;i<list.size();i++)options[i]=AudioChoice.of(list.get(i)).label;
    new AlertDialog.Builder(this).setTitle(info.getName()).setItems(options,(d,w)->{if(busy||lookupBusy){toast("Espera a que termine la tarea actual.");return;}AudioChoice quality=AudioChoice.of(list.get(w));if(direct){startDownloads(Collections.singletonList(new DownloadQueue.Entry(url,info.getName(),thumbnail(info.getThumbnails()),info.getUploaderName()==null?"":info.getUploaderName(),quality)),false);return;}try{if(editing!=null){editing.quality=quality;editing.state=DownloadQueue.PENDING;editing.error="";}else downloadQueue.add(new DownloadQueue.Entry(url,info.getName(),thumbnail(info.getThumbnails()),info.getUploaderName()==null?"":info.getUploaderName(),quality));saveQueue();renderQueueKeepingPosition();confirmAdded(editing!=null);downloadStatus("Lista guardada. Pulsa Descargar todo cuando estés listo.");}catch(IllegalStateException e){toast(e.getMessage());}}).setNegativeButton("Cerrar",null).show();downloadStatus(direct?"Elige la calidad para descargar esta canción.":"Elige calidad para añadirla a la lista.");});
  }catch(Exception e){ui(()->{lookupBusy=false;controls();downloadStatus("No se pudo consultar el audio. Revisa el enlace o vuelve a intentar.");});}});
 }
 private void pasteLinks(){
  if(busy||lookupBusy){toast("Espera a que termine la tarea actual.");return;}EditText input=field("Un enlace de YouTube por línea");input.setSingleLine(false);input.setMinLines(4);input.setMaxLines(8);input.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
  new AlertDialog.Builder(this).setTitle("Agregar varios enlaces").setMessage("Se añadirá el audio de mayor tasa disponible de cada video. Luego puedes cambiar Calidad en cada canción.").setView(input).setPositiveButton("Agregar",(d,w)->addLinks(input.getText().toString())).setNegativeButton("Cancelar",null).show();
 }
 private void addLinks(String raw){
  if(busy||lookupBusy)return;Set<String> urls=new LinkedHashSet<>();int invalid=0;
  for(String line:raw.split("\\R"))if(!line.trim().isEmpty())try{urls.add(normalize(line));}catch(IllegalArgumentException e){invalid++;}
  if(urls.isEmpty()){toast("No hay enlaces válidos.");return;}if(urls.size()>100){toast("Agrega como máximo 100 enlaces a la vez.");return;}
  final int invalidCount=invalid;lookupBusy=true;bulkLoading=true;lookupCancelled=false;cancelBatch.setEnabled(true);controls();downloadStatus("Preparando enlaces…");
  lookups.execute(()->{List<DownloadQueue.Entry> additions=new ArrayList<>();int errors=invalidCount;
   int preparing=0;for(String url:urls){if(Thread.currentThread().isInterrupted())return;if(lookupCancelled)break;final int index=++preparing;ui(()->{if(!lookupCancelled)downloadStatus("Preparando enlace "+index+"/"+urls.size()+"…");});try{StreamInfo info=StreamInfo.getInfo(ServiceList.YouTube,url);List<AudioStream> audio=AudioChoice.available(info.getAudioStreams());if(audio.isEmpty())throw new IOException();additions.add(new DownloadQueue.Entry(url,info.getName(),thumbnail(info.getThumbnails()),info.getUploaderName()==null?"":info.getUploaderName(),AudioChoice.of(audio.get(0))));}catch(Exception e){errors++;}}
   final int failed=errors;ui(()->{int added=0,duplicates=0,full=0;for(DownloadQueue.Entry e:additions)try{if(downloadQueue.add(e))added++;else duplicates++;}catch(IllegalStateException limitError){full++;}lookupBusy=false;bulkLoading=false;saveQueue();renderQueueKeepingPosition();Toast.makeText(this,added+" canciones añadidas",Toast.LENGTH_SHORT).show();downloadStatus(added+" añadidas · "+duplicates+" ya estaban · "+(failed+full)+" no añadidas. Revisa Calidad antes de descargar.");});
  });
 }
 private void stopBatch(){if(bulkLoading){lookupCancelled=true;downloadStatus("Deteniendo preparación… Se conservarán los enlaces ya consultados.");cancelBatch.setEnabled(false);return;}cancelled=true;AudioDownload d=audioDownload;if(d!=null)d.cancel();downloadStatus("Deteniendo… Las canciones pendientes quedan en la lista.");cancelBatch.setEnabled(false);}
 private void startBatch(){
  if(busy||lookupBusy)return;final List<DownloadQueue.Entry> batch=downloadQueue.pending();if(batch.isEmpty()){toast("Agrega canciones a tu lista.");return;}startDownloads(batch,true);
 }
 private void startDownloads(List<DownloadQueue.Entry> batch,boolean queued){
  if(busy||lookupBusy||batch.isEmpty())return;cancelled=false;busy=true;cancelBatch.setEnabled(true);controls();downloadStatus("Preparando "+batch.size()+" canciones…");scrollToSection(status);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  jobs.execute(()->{int done=0,failed=0;
   try{for(int n=0;n<batch.size();n++){
    if(cancelled||Thread.currentThread().isInterrupted())break;DownloadQueue.Entry e=batch.get(n);final int position=n+1;ui(()->{if(queued){downloadQueue.update(e.url,DownloadQueue.RUNNING,"");saveQueue();renderQueue();}downloadStatus(position+"/"+batch.size()+" · Consultando "+e.title);});
    File partial=null;
    try{StreamInfo info=StreamInfo.getInfo(ServiceList.YouTube,e.url);if(cancelled)throw new IOException("Cancelado");AudioStream audio=e.quality.resolve(AudioChoice.available(info.getAudioStreams()));
     File dir=new File(getFilesDir(),"music");if(!dir.exists()&&!dir.mkdirs())throw new IOException("No se pudo abrir la carpeta de música.");String base=e.title.replaceAll("[^\\p{L}\\p{N} ._-]","_");if(base.length()>90)base=base.substring(0,90);File dest=new File(dir,base+"-"+System.currentTimeMillis()+"."+audio.getFormat().getSuffix());partial=new File(dest+".part");
     AudioDownload task=new AudioDownload(Net.CLIENT);audioDownload=task;if(cancelled)task.cancel();final long started=System.nanoTime();
     task.download(audio.getContent(),partial,(bytes,total)->{String message=String.format(Locale.ROOT,"%d/%d · %s\n%s · %.1f MB · %.0f KB/s",position,batch.size(),e.title,total>0?(bytes*100/total)+" %":"…",bytes/1048576.0,bytes/1024.0/Math.max(.1,(System.nanoTime()-started)/1e9));ui(()->{if(!cancelled)downloadStatus(message);});});
     if(cancelled)throw new IOException("Cancelado");if(!partial.renameTo(dest))throw new IOException("No se pudo guardar.");done++;
     ui(()->{tracks.add(new Track(e.title,dest.toURI().toString(),e.cover,e.artist));if(queued)downloadQueue.complete(e.url);saveLibraryAndQueue();renderSongs();if(player!=null)player.setQueue(tracks);renderQueue();});
    }catch(Exception ex){if(partial!=null)partial.delete();if(cancelled||Thread.currentThread().isInterrupted()){if(queued)ui(()->{downloadQueue.update(e.url,DownloadQueue.PENDING,"");saveQueue();renderQueue();});break;}failed++;final String reason=ex instanceof IOException&&ex.getMessage()!=null?ex.getMessage():"No disponible o sin conexión. Vuelve a intentar.";ui(()->{if(queued){downloadQueue.update(e.url,DownloadQueue.FAILED,reason);saveQueue();renderQueue();}else downloadStatus("No se pudo descargar "+e.title+": "+reason);});}
    finally{audioDownload=null;}
   }}finally{final int completed=done,errors=failed;ui(()->{busy=false;getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);renderQueue();downloadStatus(queued?(cancelled?"Detenido. ":"Lista terminada. ")+completed+" descargadas · "+errors+" fallidas. Las pendientes pueden reintentarse.":cancelled?"Descarga detenida. Tu lista se conserva.":completed>0?"✓ Canción descargada y guardada en tu biblioteca.":"No se pudo descargar la canción. Puedes seleccionarla para reintentar.");});}
  });
 }
 private static void copy(InputStream in,OutputStream out)throws IOException{byte[] b=new byte[65536];int n;while((n=in.read(b))!=-1)out.write(b,0,n);}
 private void acceptShare(Intent i){if(Intent.ACTION_SEND.equals(i.getAction())){String s=i.getStringExtra(Intent.EXTRA_TEXT);if(s!=null){link.setText(s);selectTab(2);}}}
 @Override protected void onNewIntent(Intent i){super.onNewIntent(i);acceptShare(i);}
 private String clock(int ms){return String.format(Locale.ROOT,"%d:%02d",ms/60000,ms/1000%60);}
 private void refreshShuffle(){if(player!=null&&shuffleButton!=null){boolean enabled=player.shuffleEnabled();shuffleButton.setText(enabled?"⇄ Aleatorio: activado":"⇄ Aleatorio: desactivado");shuffleButton.setTextColor(enabled?palette.accent:(palette.original()?0xfff1e7da:ThemePalette.foreground(palette.button)));shuffleButton.setContentDescription(enabled?"Desactivar modo aleatorio":"Activar modo aleatorio");}}
 private final Runnable tick=new Runnable(){public void run(){if(player!=null){refreshShuffle();updateAnimatedText(now,player.title());updateAnimatedText(miniTitle,player.title());updateAnimatedText(play,player.playing()?"Pausar":"Reproducir");updateAnimatedText(miniPlay,player.playing()?"Ⅱ":"▶");
   Track current=player.currentTrack();String id=current==null?"empty":current.uri+"\n"+current.cover;
   if(!id.equals(shownCover)){shownCover=id;updateAnimatedText(artist,current==null||current.artist.isEmpty()?"Tu biblioteca":current.artist);reveal(playerCover,0,0);reveal(miniCover,0,0);covers.bind(playerCover,current,()->{reveal(playerCover,0,0);if(player!=null)player.refreshArtwork();});covers.bind(miniCover,current,()->reveal(miniCover,0,0));}
if(!seeking){seek.setMax(Math.max(player.duration(),1));seek.setProgress(player.position());}time.setText(clock(player.position())+" / "+clock(player.duration()));if(!player.error.isEmpty()){toast(player.error);player.error="";}}handler.postDelayed(this,500);}};
 @Override public void onDestroy(){getPreferences(0).edit().putInt("tab",selectedTab).apply();if(covers!=null)covers.close();if(updater!=null)updater.close();handler.removeCallbacksAndMessages(null);cancelled=true;AudioDownload d=audioDownload;if(d!=null)d.cancel();jobs.shutdownNow();lookups.shutdownNow();if(bound)unbindService(connection);super.onDestroy();}
}

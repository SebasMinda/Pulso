package com.sebas.pulso;

import android.content.Context;
import android.graphics.*;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.*;
import android.util.LruCache;
import android.widget.ImageView;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import okhttp3.*;

/** Carga portadas sin bloquear la pantalla y guarda una copia local para usarlas sin internet. */
public final class CoverArt {
 private static final int LIMIT=6*1024*1024;
 private final Context context;
 private final Handler ui=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newFixedThreadPool(2);
 private final OkHttpClient client=Net.CLIENT.newBuilder().callTimeout(10,TimeUnit.SECONDS).build();
 private final Set<Call> calls=ConcurrentHashMap.newKeySet();
 private final Map<String,List<Runnable>> pending=new HashMap<>();
 private final Set<String> missing=new HashSet<>();
 private final LruCache<String,Bitmap> memory=new LruCache<String,Bitmap>(8*1024*1024){
  @Override protected int sizeOf(String k,Bitmap b){return b.getAllocationByteCount();}
 };
 private volatile boolean closed;private int accent=ThemePalette.ACCENT;private final Map<ImageView,Boolean> placeholders=new WeakHashMap<>();
 // Actualiza el color de las portadas de reemplazo del tema.
 public void setAccent(int color){accent=color;for(Map.Entry<ImageView,Boolean> e:placeholders.entrySet())if(e.getValue())tintPlaceholder(e.getKey());}
 private void tintPlaceholder(ImageView view){if(accent==ThemePalette.ACCENT)view.clearColorFilter();else view.setColorFilter(accent,PorterDuff.Mode.SRC_IN);}
 private void showBitmap(ImageView view,Bitmap bitmap){placeholders.put(view,false);view.clearColorFilter();view.setImageBitmap(bitmap);}
 public CoverArt(Context context){this.context=context.getApplicationContext();}
 private static String key(Track t){return t.uri+"\n"+t.cover;}
 // Calcula un nombre estable para la portada usando la canción y su imagen.
 private static File file(Context c,Track t) throws Exception {
  byte[] bytes=MessageDigest.getInstance("SHA-256").digest(key(t).getBytes(java.nio.charset.StandardCharsets.UTF_8));
  StringBuilder name=new StringBuilder();for(byte b:bytes)name.append(String.format(Locale.ROOT,"%02x",b&255));
  return new File(new File(c.getFilesDir(),"covers"),name+".jpg");
 }
 // Reduce las imágenes grandes antes de cargarlas en memoria.
 private static Bitmap decode(byte[] data){
  BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;
  BitmapFactory.decodeByteArray(data,0,data.length,o);
  if(o.outWidth<=0||o.outHeight<=0)return null;
  o.inSampleSize=1;while(Math.max(o.outWidth,o.outHeight)/o.inSampleSize>640)o.inSampleSize*=2;
  o.inJustDecodeBounds=false;return BitmapFactory.decodeByteArray(data,0,data.length,o);
 }
 // Lee la imagen con un límite de tamaño para evitar consumo excesivo de memoria.
 private static byte[] read(InputStream in) throws IOException {
  if(in==null)throw new IOException("Sin imagen");
  try(InputStream input=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[16384];int n;while((n=input.read(b))!=-1){if(out.size()+n>LIMIT)throw new IOException("Imagen demasiado grande");out.write(b,0,n);}return out.toByteArray();
  }
 }
 // Lee únicamente la portada guardada en el teléfono.
 public static Bitmap cached(Context c,Track t){
  if(t==null)return null;
  try {File f=file(c,t);if(f.isFile()&&f.length()<=LIMIT)return decode(read(new FileInputStream(f)));}catch(Exception ignored){}return null;
 }
 // Todas las vistas comparten la portada decodificada. ImageView ajusta su dibujo
 // sin crear miniaturas que después puedan estirarse al expandir el reproductor.
 // Busca la copia local; si falta, lee la imagen elegida o incrustada y la guarda.
 private Bitmap load(Track t) {
  Bitmap cached=cached(context,t);if(cached!=null)return cached;
  if(closed)return null;
  try{
   byte[] data=null;
   if(!t.cover.isEmpty()) {
    Uri uri=Uri.parse(t.cover);
    if("https".equals(uri.getScheme())){
     Call call=client.newCall(new Request.Builder().url(t.cover).build());calls.add(call);
     try(Response r=call.execute()){
      if(!r.isSuccessful()||r.body()==null||r.body().contentLength()>LIMIT)throw new IOException("Portada no disponible");
      data=read(r.body().byteStream());
     }finally{calls.remove(call);}
    }else if("content".equals(uri.getScheme()))data=read(context.getContentResolver().openInputStream(uri));
   } else {
    MediaMetadataRetriever retriever=new MediaMetadataRetriever();
    try {retriever.setDataSource(context,Uri.parse(t.uri));data=retriever.getEmbeddedPicture();}
    finally {retriever.release();}
   }
   if(data==null||data.length>LIMIT)return null;
   Bitmap bitmap=decode(data);if(bitmap==null)return null;
   File f=file(context,t);File dir=f.getParentFile();if(!dir.exists()&&!dir.mkdirs())return bitmap;
   File temp=File.createTempFile("cover-",".part",dir);
   try {try(OutputStream out=new FileOutputStream(temp)){bitmap.compress(Bitmap.CompressFormat.JPEG,90,out);}if(!temp.renameTo(f))temp.delete();}
   finally{temp.delete();}
   return bitmap;
  }catch(Exception|OutOfMemoryError ignored){return null;}
 }
 // Asocia la canción a una vista y evita mostrar portadas de filas que ya cambiaron.
 public void bind(ImageView view,Track track,Runnable loaded){
  if(closed)return;
  String id=track==null?"":key(track);view.setTag(id);view.setImageResource(R.drawable.cover_placeholder);placeholders.put(view,true);tintPlaceholder(view);
  if(track==null)return;view.setContentDescription("Portada de "+track.title);
  Bitmap bitmap=memory.get(id);if(bitmap!=null){showBitmap(view,bitmap);return;}
  if(missing.contains(id))return;
  Runnable apply=()->{if(!closed&&id.equals(view.getTag())){Bitmap b=memory.get(id);if(b!=null){showBitmap(view,b);if(loaded!=null)loaded.run();}}};
  List<Runnable> waiters=pending.get(id);if(waiters!=null){waiters.add(apply);return;}
  waiters=new ArrayList<>();waiters.add(apply);pending.put(id,waiters);
  // Conserva la portada elegida al iniciar la carga aunque el usuario cambie después la imagen.
  Track snapshot=new Track(track.title,track.uri,track.cover,track.artist);
  worker.execute(()->{Bitmap b=load(snapshot);ui.post(()->{
   if(closed)return;if(b!=null)memory.put(id,b);else missing.add(id);
   List<Runnable> callbacks=pending.remove(id);if(callbacks!=null)for(Runnable r:callbacks)r.run();
  });});
 }
 // Cancela las cargas y vacía los recursos de imágenes.
 public void close(){closed=true;for(Call c:calls)c.cancel();worker.shutdownNow();ui.removeCallbacksAndMessages(null);pending.clear();memory.evictAll();}
}

package com.sebas.pulso;

import java.io.*;
import java.util.Set;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.*;
import okhttp3.*;

/** Copies original audio bytes, using bounded parallel ranges when supported. */
public final class AudioDownload {
 private static final int CHUNK = 1024 * 1024, PROBE = 256 * 1024;
 private final OkHttpClient client;
 private final Set<Call> calls = ConcurrentHashMap.newKeySet();
 private volatile boolean cancelled;
 public interface Progress { void update(long done, long total); }
 public AudioDownload(OkHttpClient client) { this.client = client; }
 public void cancel() { cancelled = true; for (Call c : calls) c.cancel(); }
 private void check() throws IOException {
  if (cancelled || Thread.currentThread().isInterrupted()) throw new IOException("Cancelado");
 }
 private Call call(String url, String range, String validator) throws IOException {
  check();
  Request.Builder b = new Request.Builder().url(url).header("User-Agent", "Mozilla/5.0")
    .header("Accept-Encoding", "identity");
  if (range != null) b.header("Range", range);
  if (validator != null) b.header("If-Range", validator);
  Call c = client.newCall(b.build()); calls.add(c);
  if (cancelled) { c.cancel(); calls.remove(c); check(); }
  return c;
 }
 private static long[] range(Response r) throws IOException {
  Matcher m = Pattern.compile("bytes (\\d+)-(\\d+)/(\\d+)").matcher(r.header("Content-Range", ""));
  if (r.code() != 206 || !m.matches()) throw new IOException("El servidor no admite partes exactas");
  try {
   long a=Long.parseLong(m.group(1)), b=Long.parseLong(m.group(2)), n=Long.parseLong(m.group(3));
   if(a>b || b>=n) throw new IOException("Rango inválido");
   return new long[]{a,b,n};
  } catch(NumberFormatException e) { throw new IOException("Rango inválido", e); }
 }
 public void download(String url, File file, Progress progress) throws IOException {
  boolean success=false;
  try {
   try { ranged(url,file,progress); }
   catch(IOException e) { check(); progress.update(0,-1); single(url,file,progress); }
   check(); validate(file); success=true;
  } finally { if(!success) file.delete(); }
 }
 private void ranged(String url, File file, Progress progress) throws IOException {
  long total, first;
  String validator;
  Call probe=call(url,"bytes=0-"+(PROBE-1),null);
  try(Response r=probe.execute()) {
   long[] bounds=range(r); total=bounds[2]; first=bounds[1]+1;
   if(bounds[0]!=0 || first!=Math.min(total,PROBE)) throw new IOException("Rango inicial inválido");
   validator=r.header("ETag");
   if(validator==null || validator.startsWith("W/")) validator=r.header("Last-Modified");
   try(RandomAccessFile out=new RandomAccessFile(file,"rw")) {
    out.setLength(total); write(r,out,0,first);
   }
  } finally { calls.remove(probe); }
  progress.update(first,total);
  if(first==total) return;
  final long size=total;
  final String tag=validator;
  AtomicLong position=new AtomicLong(first), done=new AtomicLong(first);
  ExecutorService pool=Executors.newFixedThreadPool(3);
  CompletionService<Void> completion=new ExecutorCompletionService<>(pool);
  List<Future<Void>> futures=new ArrayList<>();
  try {
   for(int i=0;i<3;i++) futures.add(completion.submit(()->{
    try(RandomAccessFile out=new RandomAccessFile(file,"rw")) {
     while(true) {
      check(); long start=position.getAndAdd(CHUNK); if(start>=size) return null;
      long end=Math.min(size-1,start+CHUNK-1);
      for(int attempt=0;;attempt++) {
       Call c=call(url,"bytes="+start+"-"+end,tag);
       try(Response r=c.execute()) {
        long[] b=range(r);
        if(b[0]!=start || b[1]!=end || b[2]!=size) throw new IOException("Parte incorrecta");
        if(tag!=null && !tag.equals(r.header("ETag")) && !tag.equals(r.header("Last-Modified"))) throw new IOException("El audio cambió durante la descarga");
        write(r,out,start,end-start+1); break;
       } catch(IOException e) { check(); if(attempt>=1) throw e; }
       finally { calls.remove(c); }
      }
      synchronized(done) { progress.update(done.addAndGet(end-start+1),size); }
     }
    }
   }));
   for(int i=0;i<3;i++) completion.take().get();
  } catch(InterruptedException e) {
   Thread.currentThread().interrupt(); throw new IOException("Cancelado",e);
  } catch(ExecutionException e) { throw new IOException("No se pudieron descargar las partes",e.getCause()); }
  finally {
   for(Future<Void> f:futures) f.cancel(true);
   for(Call c:calls) c.cancel();
   pool.shutdownNow();
   // Workers must close their files before a sequential fallback can overwrite them.
   boolean interrupted=Thread.interrupted();
   try { while(!pool.isTerminated()) { try { pool.awaitTermination(1,TimeUnit.SECONDS); } catch(InterruptedException e) { interrupted=true; } } }
   finally { if(interrupted) Thread.currentThread().interrupt(); }
  }
 }
 private void write(Response r, RandomAccessFile out, long offset, long count) throws IOException {
  if(r.body()==null) throw new IOException("Sin audio");
  out.seek(offset); byte[] buffer=new byte[65536]; long written=0;
  try(InputStream in=r.body().byteStream()) {
   int n;
   while((n=in.read(buffer))!=-1) {
    check(); if(n>count-written) throw new IOException("Parte demasiado larga");
    out.write(buffer,0,n); written+=n;
   }
  }
  if(written!=count) throw new IOException("Parte incompleta");
 }
 private void single(String url, File file, Progress progress) throws IOException {
  Call c=call(url,null,null);
  try(Response r=c.execute()) {
   if(r.code()!=200 || r.body()==null) throw new IOException("HTTP "+r.code());
   long total=r.body().contentLength(),done=0,last=0;
   try(InputStream in=r.body().byteStream(); OutputStream out=new BufferedOutputStream(new FileOutputStream(file),65536)) {
    byte[] buffer=new byte[65536]; int n;
    while((n=in.read(buffer))!=-1) {
     check(); out.write(buffer,0,n); done+=n;
     long clock=System.nanoTime();
     if(clock-last>250000000L) { last=clock; progress.update(done,total); }
    }
   }
   if(done==0 || (total>=0 && done!=total)) throw new IOException("Descarga incompleta");
   progress.update(done,total);
  } finally { calls.remove(c); }
 }
 private static void validate(File file) throws IOException {
  byte[] head=new byte[12];
  try(DataInputStream in=new DataInputStream(new FileInputStream(file))) { in.readFully(head); }
  boolean webm=(head[0]&255)==0x1a && (head[1]&255)==0x45 && (head[2]&255)==0xdf && (head[3]&255)==0xa3;
  boolean mp4=head[4]=='f' && head[5]=='t' && head[6]=='y' && head[7]=='p';
  if(!webm && !mp4) throw new IOException("La red devolvió una respuesta que no es audio");
 }
}

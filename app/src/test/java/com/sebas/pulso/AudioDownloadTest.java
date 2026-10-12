package com.sebas.pulso;

import java.net.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import okhttp3.OkHttpClient;
import org.junit.*;
import static org.junit.Assert.*;

public class AudioDownloadTest {
 private ServerSocket server;
 private ExecutorService workers;
 private File output;
 private byte[] audio;
 private String mode;
 private final AtomicInteger active=new AtomicInteger(), max=new AtomicInteger(), requests=new AtomicInteger(), singles=new AtomicInteger();
 private final AtomicBoolean failedOnce=new AtomicBoolean();
 private CountDownLatch parallel;
 @Before public void setup() throws Exception {
  audio=new byte[4*1024*1024+153];
  for(int i=0;i<audio.length;i++) audio[i]=(byte)(i*31);
  audio[0]=0x1a;audio[1]=0x45;audio[2]=(byte)0xdf;audio[3]=(byte)0xa3;
  output=File.createTempFile("pulso-audio",".part");
  mode="range"; parallel=new CountDownLatch(2);
  server=new ServerSocket(0,20,InetAddress.getByName("127.0.0.1"));
  workers=Executors.newCachedThreadPool();
  workers.submit(()->{while(!server.isClosed()) {try {Socket socket=server.accept();workers.submit(()->serve(socket));}catch(IOException ignored){}}});
 }
 private void serve(Socket socket) {
  int running=active.incrementAndGet();max.accumulateAndGet(running,Math::max);
  try(Socket client=socket) {
   client.setSoTimeout(5000);
   BufferedReader reader=new BufferedReader(new InputStreamReader(client.getInputStream(),java.nio.charset.StandardCharsets.US_ASCII));
   String requestLine=reader.readLine();if(requestLine==null)return;String line,range=null;
   while((line=reader.readLine())!=null && !line.isEmpty()) if(line.toLowerCase(java.util.Locale.ROOT).startsWith("range:")) range=line.substring(6).trim();
   if(line==null)return; // A cancelled socket without complete headers is not an HTTP request.
   requests.incrementAndGet();
   OutputStream out=client.getOutputStream();
   if(range==null) singles.incrementAndGet();
   if(range!=null && !mode.equals("ignore")) {
    String[] b=range.substring(6).split("-");int start=Integer.parseInt(b[0]),end=Math.min(audio.length-1,Integer.parseInt(b[1]));
    if(start>0 && mode.equals("retry") && failedOnce.compareAndSet(false,true)) {headers(out,503,0,"");return;}
    if(start>0 && mode.equals("parallel")) {parallel.countDown();parallel.await(2,TimeUnit.SECONDS);}
    int announced=mode.equals("badRange") && start>0?start+1:start;
    headers(out,206,end-start+1,"Content-Range: bytes "+announced+"-"+end+"/"+audio.length+"\r\nETag: \"audio-v1\"\r\n");
    out.write(audio,start,mode.equals("truncated")?Math.min(20,end-start+1):end-start+1);
   } else {headers(out,200,audio.length,"");out.write(audio,0,mode.equals("truncated")?20:audio.length);}
  } catch(IOException ignored) {} catch(InterruptedException e) {Thread.currentThread().interrupt();}
  finally {active.decrementAndGet();}
 }
 private static void headers(OutputStream out,int code,int length,String extra) throws IOException {
  out.write(("HTTP/1.1 "+code+" Response\r\nContent-Length: "+length+"\r\nConnection: close\r\n"+extra+"\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));

 }
 @After public void cleanup() throws Exception {server.close();workers.shutdownNow();output.delete();}
 private AudioDownload task() {return new AudioDownload(new OkHttpClient.Builder().readTimeout(5,TimeUnit.SECONDS).build());}
 private String url() {return "http://127.0.0.1:"+server.getLocalPort()+"/audio";}
 private void verify() throws Exception {assertArrayEquals(audio,Files.readAllBytes(output.toPath()));}
 @Test public void combinesParallelPartsExactly() throws Exception {
  mode="parallel";AtomicLong last=new AtomicLong();
  task().download(url(),output,(done,total)->{assertTrue(done>=last.get());last.set(done);});
  verify();assertEquals(audio.length,last.get());assertTrue(max.get()>=2);assertEquals(0,singles.get());
 }
 @Test public void ignoredRangesFallBackToSingleCopy() throws Exception {
  mode="ignore";task().download(url(),output,(d,t)->{});verify();assertEquals(1,singles.get());
 }
 @Test public void incorrectRangesFallBackWithoutCorruption() throws Exception {
  mode="badRange";task().download(url(),output,(d,t)->{});verify();assertEquals(1,singles.get());
 }
 @Test public void retriesFailedPartWithoutRestartingWholeAudio() throws Exception {
  mode="retry";task().download(url(),output,(d,t)->{});verify();assertTrue(failedOnce.get());assertEquals(0,singles.get());
 }
 @Test public void cancellationRemovesPartialAndDoesNotFallBack() throws Exception {
  AudioDownload task=task();
  try {task.download(url(),output,(d,t)->task.cancel());fail("Should cancel");}catch(IOException expected){}
  assertFalse(output.exists());assertEquals(0,singles.get());
 }
 @Test public void rejectsNonAudioResponse() throws Exception {
  Arrays.fill(audio,(byte)'x');
  try {task().download(url(),output,(d,t)->{});fail("Should reject");}catch(IOException expected){}
  assertFalse(output.exists());
 }
 @Test public void smallAudioUsesOnlyProbe() throws Exception {
  audio=Arrays.copyOf(audio,1024);task().download(url(),output,(d,t)->{});verify();assertEquals(1,requests.get());
 }
 @Test public void incompleteResponsesAreNeverSaved() throws Exception {
  mode="truncated";
  try {task().download(url(),output,(d,t)->{});fail("Should reject truncation");}catch(IOException expected){}
  assertFalse(output.exists());assertEquals(1,singles.get());
 }
 @Test public void cancellationBeforeStartDoesNotSendRequest() throws Exception {
  AudioDownload task=task();task.cancel();
  try{task.download(url(),output,(d,t)->{});fail("Should cancel");}catch(IOException expected){}
  assertEquals(0,requests.get());assertFalse(output.exists());
 }
}

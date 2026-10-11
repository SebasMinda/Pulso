package com.sebas.pulso;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class DownloadQueueTest {
 private DownloadQueue.Entry entry(String url,int rate){return new DownloadQueue.Entry(url,"Canción","https://image","Canal",new AudioChoice("140","m4a","",rate,"M4A"));}
 @Test public void persistsEachSongsQualityAndRestoresInterruptedItem()throws Exception{
  DownloadQueue q=new DownloadQueue();q.add(entry("a",128));q.add(entry("b",64));q.update("a",DownloadQueue.RUNNING,"");
  DownloadQueue loaded=new DownloadQueue();loaded.restore(new JSONArray(q.json().toString()));
  assertEquals(128,loaded.entries.get(0).quality.bitrate);assertEquals(64,loaded.entries.get(1).quality.bitrate);assertEquals(DownloadQueue.PENDING,loaded.entries.get(0).state);
 }
 @Test public void completedSongsAreSkippedButFailedSongsRetry(){DownloadQueue q=new DownloadQueue();q.add(entry("a",128));q.add(entry("b",64));q.update("a",DownloadQueue.DONE,"");q.update("b",DownloadQueue.FAILED,"sin red");assertEquals(1,q.pending().size());assertEquals("b",q.pending().get(0).url);}
 @Test public void duplicateDoesNotReplaceFinishedDownloadOrQuality(){DownloadQueue q=new DownloadQueue();q.add(entry("a",128));q.update("a",DownloadQueue.DONE,"");assertFalse(q.add(entry("a",64)));assertEquals(128,q.entries.get(0).quality.bitrate);assertTrue(q.pending().isEmpty());}
 @Test public void batchSnapshotDoesNotChangeWhenUserEditsQueue(){DownloadQueue q=new DownloadQueue();q.add(entry("a",128));DownloadQueue.Entry e=q.pending().get(0);q.entries.get(0).quality=entry("x",64).quality;q.update("a",DownloadQueue.DONE,"");assertEquals(128,e.quality.bitrate);assertEquals(DownloadQueue.PENDING,e.state);}
 @Test public void damagedEntryDoesNotHideOtherSongs()throws Exception{DownloadQueue q=new DownloadQueue();q.restore(new JSONArray().put(new JSONObject().put("url","bad")).put(entry("good",128).json()));assertEquals(1,q.entries.size());assertEquals("good",q.entries.get(0).url);}
 @Test public void completingRemovesOnlySuccessfulSongAndPersistsRemaining()throws Exception{DownloadQueue q=new DownloadQueue();q.add(entry("a",128));q.add(entry("b",64));q.update("b",DownloadQueue.FAILED,"sin red");q.complete("a");DownloadQueue loaded=new DownloadQueue();loaded.restore(q.json());assertEquals(1,loaded.entries.size());assertEquals("b",loaded.entries.get(0).url);assertEquals(DownloadQueue.FAILED,loaded.entries.get(0).state);}
 @Test public void oldCompletedEntriesAreRemovedDuringUpgrade()throws Exception{DownloadQueue q=new DownloadQueue();q.add(entry("a",128));q.add(entry("b",64));q.update("a",DownloadQueue.DONE,"");DownloadQueue loaded=new DownloadQueue();loaded.restore(q.json());assertEquals(1,loaded.entries.size());assertEquals("b",loaded.entries.get(0).url);}
}

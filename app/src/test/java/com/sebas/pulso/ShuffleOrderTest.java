package com.sebas.pulso;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Comprueba vueltas sin repeticiones, historial y cambios de biblioteca. */
public class ShuffleOrderTest {
 @Test public void cycleVisitsAllOtherSongsWithoutRepeatingCurrent(){ShuffleOrder s=new ShuffleOrder(new Random(42));s.reset(Arrays.asList("a","b","c","d"),"a");Set<String> played=new HashSet<>();for(int i=0;i<3;i++){String n=s.next();assertNotEquals("a",n);assertTrue(played.add(n));}assertEquals(new HashSet<>(Arrays.asList("b","c","d")),played);}
 @Test public void previousUsesHistoryAndNextReturnsToIt(){ShuffleOrder s=new ShuffleOrder(new Random(5));s.reset(Arrays.asList("a","b","c"),"a");String b=s.next(),c=s.next();assertEquals(b,s.previous());assertEquals("a",s.previous());assertEquals(b,s.next());assertEquals(c,s.next());}
 @Test public void nextCycleDoesNotRepeatItsLastSong(){ShuffleOrder s=new ShuffleOrder(new Random(2));s.reset(Arrays.asList("a","b","c"),"a");s.next();String last=s.next();assertNotEquals(last,s.next());}
 @Test public void emptyAndSingleSongAreSafe(){ShuffleOrder s=new ShuffleOrder();s.reset(Collections.emptyList(),null);assertNull(s.next());assertNull(s.previous());s.reset(Collections.singletonList("a"),"a");assertEquals("a",s.next());assertEquals("a",s.previous());}
 @Test public void changedLibraryCannotPlayRemovedSongs(){ShuffleOrder s=new ShuffleOrder();s.reset(Arrays.asList("a","b","c"),"a");s.next();s.reset(Arrays.asList("a","d"),"a");for(int i=0;i<10;i++)assertTrue(Arrays.asList("a","d").contains(s.next()));}
 @Test public void laterCycleStillIncludesEverySong(){ShuffleOrder s=new ShuffleOrder(new Random(9));s.reset(Arrays.asList("a","b","c","d"),"a");for(int i=0;i<3;i++)s.next();Set<String> cycle=new HashSet<>();for(int i=0;i<4;i++)assertTrue(cycle.add(s.next()));assertEquals(new HashSet<>(Arrays.asList("a","b","c","d")),cycle);}
}

package com.sebas.pulso;
import java.util.*;

/** A shuffled cycle plus real playback history for the previous button. */
public final class ShuffleOrder {
 private final Random random;
 private final List<String> tracks=new ArrayList<>(),bag=new ArrayList<>(),history=new ArrayList<>();
 private int cursor=-1;
 public ShuffleOrder(){this(new Random());}
 public ShuffleOrder(Random random){this.random=random;}
 public void reset(List<String> urls,String current){tracks.clear();tracks.addAll(new LinkedHashSet<>(urls));history.clear();cursor=-1;if(current!=null&&tracks.contains(current)){history.add(current);cursor=0;}fill(current);}
 private void fill(String current){bag.clear();for(String url:tracks)if(!url.equals(current)||tracks.size()==1)bag.add(url);Collections.shuffle(bag,random);}
 public String next(){
  if(tracks.isEmpty())return null;
  if(cursor+1<history.size())return history.get(++cursor);
  String current=cursor>=0?history.get(cursor):null;if(bag.isEmpty())fill(current);
  String next=bag.remove(0);history.add(next);cursor=history.size()-1;
  if(history.size()>512){history.remove(0);cursor--;}
  return next;
 }
 public String previous(){if(cursor>0)return history.get(--cursor);return cursor==0?history.get(0):null;}
}

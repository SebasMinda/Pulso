package com.sebas.pulso;
import java.util.*;

/** Elige canciones al azar sin repetirlas en una vuelta y conserva el historial para volver atrás. */
public final class ShuffleOrder {
 private final Random random;
 private final List<String> tracks=new ArrayList<>(),bag=new ArrayList<>(),history=new ArrayList<>();
 private int cursor=-1;
 public ShuffleOrder(){this(new Random());}
 public ShuffleOrder(Random random){this.random=random;}
 // Reconstruye el orden cuando cambia la biblioteca o se elige una canción.
 public void reset(List<String> urls,String current){tracks.clear();tracks.addAll(new LinkedHashSet<>(urls));history.clear();cursor=-1;if(current!=null&&tracks.contains(current)){history.add(current);cursor=0;}fill(current);}
 // Mezcla las canciones restantes dejando fuera la actual si hay otras.
 private void fill(String current){bag.clear();for(String url:tracks)if(!url.equals(current)||tracks.size()==1)bag.add(url);Collections.shuffle(bag,random);}
 // Recorre el historial o elige otra canción; al acabar la vuelta vuelve a mezclar.
 public String next(){
  if(tracks.isEmpty())return null;
  if(cursor+1<history.size())return history.get(++cursor);
  String current=cursor>=0?history.get(cursor):null;if(bag.isEmpty()){bag.addAll(tracks);Collections.shuffle(bag,random);if(bag.size()>1&&bag.get(0).equals(current))Collections.swap(bag,0,1);}
  String next=bag.remove(0);history.add(next);cursor=history.size()-1;
  if(history.size()>512){history.remove(0);cursor--;}
  return next;
 }
 // Vuelve por el historial de canciones reproducidas.
 public String previous(){if(cursor>0)return history.get(--cursor);return cursor==0?history.get(0):null;}
}

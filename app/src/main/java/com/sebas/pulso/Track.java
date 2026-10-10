package com.sebas.pulso;
import org.json.*;
public class Track {
 public final String title, uri;
 public String cover, artist;
 public Track(String title,String uri) { this(title,uri,"",""); }
 public Track(String title,String uri,String cover,String artist) {
  this.title=title;this.uri=uri;this.cover=cover==null?"":cover;this.artist=artist==null?"":artist;
 }
 public static Track fromJson(JSONObject o) throws JSONException {
  return new Track(o.getString("title"),o.getString("uri"),o.optString("cover",""),o.optString("artist",""));
 }
 public JSONObject json() throws JSONException { return new JSONObject().put("title",title).put("uri",uri).put("cover",cover).put("artist",artist); }
}

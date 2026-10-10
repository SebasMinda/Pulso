package com.sebas.pulso;
import org.json.*;
public class Track {
 public final String title, uri;
 public Track(String title,String uri) { this.title=title;this.uri=uri; }
 public JSONObject json() throws JSONException { return new JSONObject().put("title",title).put("uri",uri); }
}

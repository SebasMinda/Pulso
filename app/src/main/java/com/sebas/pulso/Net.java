package com.sebas.pulso;

import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.*;

public final class Net extends Downloader {
 static final OkHttpClient CLIENT = new OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(45,TimeUnit.SECONDS).build();
 @Override public Response execute(Request req) throws IOException {
  okhttp3.Request.Builder b = new okhttp3.Request.Builder().url(req.url()).header("User-Agent","Mozilla/5.0");
  req.headers().forEach((k,vs)->vs.forEach(v->b.addHeader(k,v)));
  byte[] data=req.dataToSend();
  b.method(req.httpMethod(), data==null?null:RequestBody.create(data, MediaType.parse("application/json")));
  try(okhttp3.Response r=CLIENT.newCall(b.build()).execute()) {
   return new Response(r.code(),r.message(),r.headers().toMultimap(),r.body()==null?"":r.body().string(),r.request().url().toString());
  }
 }
}

import com.sebas.pulso.Net;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.stream.StreamInfo;

// Comprobación de integración; no descarga la película.
public class ExtractorProbe {
 public static void main(String[] args) throws Exception {
  NewPipe.init(new Net());
  StreamInfo info=StreamInfo.getInfo(ServiceList.YouTube,args[0]);
  if(info.getAudioStreams().isEmpty())throw new AssertionError("Sin audio");
  System.out.println("Audio streams: "+info.getAudioStreams().size());
  for(var stream:info.getAudioStreams())System.out.println(stream.getFormat()+" "+stream.getAverageBitrate()+" kb/s");
  if(args.length>1){
   var best=info.getAudioStreams().stream().filter(s->s.isUrl()).max(java.util.Comparator.comparingInt(s->s.getAverageBitrate())).orElseThrow();
   var client=new okhttp3.OkHttpClient.Builder().readTimeout(45,java.util.concurrent.TimeUnit.SECONDS).build();
   try(var response=client.newCall(new okhttp3.Request.Builder().url(best.getContent()).header("User-Agent","Mozilla/5.0").build()).execute()){
    if(!response.isSuccessful()||response.body()==null)throw new java.io.IOException("HTTP "+response.code());
    try(var in=new java.io.BufferedInputStream(response.body().byteStream());var out=new java.io.FileOutputStream(args[1])){in.mark(16);byte[] h=in.readNBytes(12);in.reset();if(h.length<4||(h[0]&255)!=0x1a||(h[1]&255)!=0x45||(h[2]&255)!=0xdf||(h[3]&255)!=0xa3)throw new java.io.IOException("La respuesta no es WebM: descarga bloqueada o inválida");in.transferTo(out);}
   }
   System.out.println("Downloaded bytes: "+new java.io.File(args[1]).length());
  }
 }
}

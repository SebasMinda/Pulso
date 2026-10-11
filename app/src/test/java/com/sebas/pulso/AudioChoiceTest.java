package com.sebas.pulso;
import java.util.*;
import java.io.IOException;
import org.junit.Test;
import org.schabi.newpipe.extractor.MediaFormat;
import org.schabi.newpipe.extractor.stream.AudioStream;
import static org.junit.Assert.*;
public class AudioChoiceTest {
 private AudioStream audio(String id,String url,int rate,String track){return new AudioStream.Builder().setId(id).setContent(url,true).setMediaFormat(MediaFormat.M4A).setAverageBitrate(rate).setAudioTrackId(track).build();}
 @Test public void qualitySurvivesExpiredDownloadAddress()throws Exception{AudioStream old=audio("140","https://old",128,"original");AudioChoice c=AudioChoice.fromJson(AudioChoice.of(old).json());AudioStream fresh=audio("140","https://new",128,"original");assertSame(fresh,c.resolve(Arrays.asList(fresh)));}
 @Test(expected=IOException.class)public void neverSilentlyDowngradesOrChangesLanguage()throws Exception{AudioChoice.of(audio("140","https://old",128,"original")).resolve(Arrays.asList(audio("139","https://new",64,"original"),audio("140","https://new",128,"dubbed")));}
 @Test public void displaysHighestBitrateFirstWithoutDuplicateFormats(){AudioStream a=audio("140","https://a",128,"");AudioStream b=audio("139","https://b",64,"");List<AudioStream> options=AudioChoice.available(Arrays.asList(b,a,a));assertEquals(2,options.size());assertSame(a,options.get(0));}
}

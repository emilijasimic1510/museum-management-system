package rs.ac.uns.ftn.museum.audio;

import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Production provider. Credentials are supplied only through environment/configuration. */
@Service
@ConditionalOnProperty(name="museum.tts.provider", havingValue="azure")
public class AzureTtsService implements TtsService {
    private final String key, region, voice;
    private final HttpClient client;
    public AzureTtsService(@Value("${museum.tts.azure-key:}") String key,
            @Value("${museum.tts.azure-region:}") String region,
            @Value("${museum.tts.azure-voice:sr-RS-SophieNeural}") String voice,
            @Value("${museum.tts.timeout-seconds:30}") long timeout) {
        this.key=key;this.region=region;this.voice=voice;
        this.client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(Math.max(1,timeout))).build();
    }
    @Override public void generate(String text,String language,Path output) {
        if(key.isBlank() || region.isBlank()) throw new AudioException("Azure Speech nije konfigurisan. Podesite MUSEUM_TTS_AZURE_KEY i MUSEUM_TTS_AZURE_REGION.");
        String safeLanguage = "sr".equalsIgnoreCase(language) ? "sr-RS" : language;
        String xml = "<speak version=\"1.0\" xml:lang=\""+escape(safeLanguage)+"\"><voice name=\""+escape(voice)+"\">"+escape(text)+"</voice></speak>";
        HttpRequest request=HttpRequest.newBuilder(URI.create("https://"+region+".tts.speech.microsoft.com/cognitiveservices/v1"))
                .timeout(Duration.ofSeconds(30)).header("Ocp-Apim-Subscription-Key",key)
                .header("Content-Type","application/ssml+xml").header("X-Microsoft-OutputFormat","riff-24khz-16bit-mono-pcm")
                .header("User-Agent","museum-management-system").POST(HttpRequest.BodyPublishers.ofString(xml)).build();
        try {
            HttpResponse<byte[]> response=client.send(request,HttpResponse.BodyHandlers.ofByteArray());
            if(response.statusCode()<200 || response.statusCode()>=300) throw new AudioException("Azure Speech nije uspeo da generiše audio (HTTP "+response.statusCode()+").");
            Files.write(output,response.body(),StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING);
            AudioStorage.validateWav(output);
        } catch(AudioException e){throw e;} catch(IOException e){throw new AudioException("Azure Speech nije dostupan. Proverite mrežu i region.");}
          catch(InterruptedException e){Thread.currentThread().interrupt();throw new AudioException("Generisanje audija je prekinuto.");}
    }
    private static String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;");}
}

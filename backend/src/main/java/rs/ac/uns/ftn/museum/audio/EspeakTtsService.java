package rs.ac.uns.ftn.museum.audio;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
@Service
@ConditionalOnProperty(name="museum.tts.provider", havingValue="espeak", matchIfMissing=true)
public class EspeakTtsService implements TtsService {
    private final String executable;
    private final long timeout;
    public EspeakTtsService(@Value("${museum.tts.espeak-path:espeak-ng}") String executable,
            @Value("${museum.tts.timeout-seconds:30}") long timeout) {
        this.executable=executable;this.timeout=Math.max(1,timeout);
    }
    public void generate(String text,String language,Path output) {
        Process process=null;Path input=null;
        try {
            input=Files.createTempFile(output.getParent(),"tts-",".txt");
            Files.writeString(input,text,StandardCharsets.UTF_8);
            process=new ProcessBuilder(executable,"-v",language,"-b","1","-f",input.toString(),"-w",output.toString())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if(!process.waitFor(timeout,TimeUnit.SECONDS))throw new AudioException("Generisanje audija je prekoračilo vremensko ograničenje.");
            if(process.exitValue()!=0)throw new AudioException("eSpeak NG nije uspešno generisao audio.");
            AudioStorage.validateWav(output);
        } catch(IOException e) {throw new AudioException("eSpeak NG nije dostupan. Proverite instalaciju i museum.tts.espeak-path.");}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new AudioException("Generisanje audija je prekinuto.");}
        finally {
            if(process!=null && process.isAlive()) {process.destroyForcibly();try{process.waitFor(5,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
            if(input!=null)try{Files.deleteIfExists(input);}catch(IOException ignored){}
        }
    }
}

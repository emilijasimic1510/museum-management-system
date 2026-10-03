package rs.ac.uns.ftn.museum.audio;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/** Local neural TTS provider. Install piper-tts and a licensed Serbian voice model separately. */
@Service
@ConditionalOnProperty(name="museum.tts.provider", havingValue="piper")
public class PiperTtsService implements TtsService {
    private final String python, model; private final long timeout;
    public PiperTtsService(@Value("${museum.tts.piper-python:python}") String python,
            @Value("${museum.tts.piper-model:}") String model,
            @Value("${museum.tts.timeout-seconds:60}") long timeout){this.python=python;this.model=model;this.timeout=Math.max(1,timeout);}
    @Override public void generate(String text,String language,Path output){
        if(model.isBlank()) throw new AudioException("Piper nije konfigurisan. Podesite MUSEUM_TTS_PIPER_MODEL na .onnx model.");
        try {
            Process p=new ProcessBuilder(python,"-m","piper","--model",model,"--output_file",output.toString())
                    .redirectErrorStream(true).start();
            // Browser/database input can contain an unpaired UTF-16 surrogate (often
            // introduced by pasted emoji). Piper's eSpeak phonemizer rejects it.
            StringBuilder cleaned=new StringBuilder(text.length());
            text.codePoints().filter(cp -> cp!=0 && !Character.isSurrogate((char)cp)
                    && !(cp>=0xD800 && cp<=0xDFFF) && !(cp<0x20 && cp!='\n' && cp!='\r' && cp!='\t'))
                    .forEach(cleaned::appendCodePoint);
            String safeText=cleaned.toString().replaceAll("\\\\u[dD][89A-Fa-f0-9]{2}", "");
            // Piper's Windows espeak bridge can reject malformed/non-BMP input even
            // after Java sanitization. ASCII is a final process-boundary guarantee;
            // the original Serbian text remains stored and visible in the UI.
            String piperText=java.text.Normalizer.normalize(safeText, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "").replaceAll("[^\\x00-\\x7F]", "?");
            try(OutputStream in=p.getOutputStream()){in.write(piperText.getBytes(StandardCharsets.US_ASCII));}
            if(!p.waitFor(timeout,TimeUnit.SECONDS)){p.destroyForcibly();throw new AudioException("Piper generisanje je prekoračilo vremensko ograničenje.");}
            String diagnostic=new String(p.getInputStream().readAllBytes(),StandardCharsets.UTF_8).trim();
            if(p.exitValue()!=0) {
                String detail=diagnostic.length()>1800?diagnostic.substring(diagnostic.length()-1800):diagnostic;
                throw new AudioException("Piper nije uspešno generisao audio."+(detail.isBlank()?"":" "+detail));
            }
            AudioStorage.validateWav(output);
        } catch(IOException e){throw new AudioException("Piper nije dostupan. Instalirajte piper-tts i proverite putanju modela.");}
          catch(InterruptedException e){Thread.currentThread().interrupt();throw new AudioException("Generisanje audija je prekinuto.");}
    }
}

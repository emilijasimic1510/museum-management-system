package rs.ac.uns.ftn.museum.audio;
import java.nio.file.*;
import java.io.*;
import java.util.UUID;
import javax.sound.sampled.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class AudioStorage {
    public static final String PREFIX="private-audio:";
    private final Path root;
    public AudioStorage(@Value("${museum.audio.storage-path:./runtime/audio}") String root){this.root=Path.of(root).toAbsolutePath().normalize();}
    public String reference(long id,String language,int version){return PREFIX+id+"-"+language+"-v"+version+"-"+UUID.randomUUID()+".wav";}
    public Path resolve(String reference) {
        if(reference==null || !reference.matches("private-audio:[0-9]+-(sr|en)-v[0-9]+-[a-f0-9-]{36}\\.wav"))throw new AudioException("Neispravna interna audio referenca.");
        Path path=root.resolve(reference.substring(PREFIX.length())).normalize();
        if(!path.getParent().equals(root)||Files.isSymbolicLink(path))throw new AudioException("Neispravna interna audio referenca.");
        return path;
    }
    public Path prepare(String reference){try{Files.createDirectories(root);return resolve(reference);}catch(IOException e){throw new AudioException("Privatno audio skladište nije dostupno.");}}
    public byte[] read(String reference){try{return Files.readAllBytes(resolve(reference));}catch(IOException e){throw new AudioException("Audio fajl trenutno nije dostupan.");}}
    public void delete(String reference){try{Files.deleteIfExists(resolve(reference));}catch(IOException|AudioException e){org.slf4j.LoggerFactory.getLogger(AudioStorage.class).warn("Privremeni audio fajl nije moguće obrisati.");}}
    public static void validateWav(Path path){
        try {var format=AudioSystem.getAudioFileFormat(path.toFile());
            if(!AudioFileFormat.Type.WAVE.equals(format.getType())||Files.size(path)<=44)throw new IOException();
        }catch(Exception e){throw new AudioException("eSpeak NG nije kreirao ispravan WAV fajl.");}
    }
    public static String publicReference(String path){return path!=null&&path.startsWith(PREFIX)?"":path;}
}

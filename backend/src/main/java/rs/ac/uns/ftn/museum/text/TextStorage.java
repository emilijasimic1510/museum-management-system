package rs.ac.uns.ftn.museum.text;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.io.IOException;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TextStorage {
    public static final String PREFIX="private-text:";
    private final Path root;
    public TextStorage(@Value("${museum.text.storage-path:./runtime/text}") String root){this.root=Path.of(root).toAbsolutePath().normalize();}
    public String reference(long participation,String language,int version){return PREFIX+participation+"-"+language+"-v"+version+"-"+UUID.randomUUID()+".txt";}
    private Path resolve(String ref){
        if(ref==null||!ref.matches("private-text:[0-9]+-(sr|en)-v[0-9]+-[a-f0-9-]{36}\\.txt"))throw new IllegalArgumentException("Invalid private text reference");
        Path p=root.resolve(ref.substring(PREFIX.length())).normalize();
        if(!p.getParent().equals(root)||Files.isSymbolicLink(p))throw new IllegalArgumentException("Invalid private text reference");
        return p;
    }
    public void write(String ref,String text){try{Files.createDirectories(root);Files.writeString(resolve(ref),text,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW);}catch(IOException|RuntimeException e){throw new IllegalStateException("Private text storage unavailable",e);}}
    public String read(String ref){try{return Files.readString(resolve(ref),StandardCharsets.UTF_8);}catch(IOException|RuntimeException e){throw new IllegalStateException("Text is unavailable",e);}}
    public void delete(String ref){try{Files.deleteIfExists(resolve(ref));}catch(Exception ignored){}}
}

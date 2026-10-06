package rs.ac.uns.ftn.museum.media;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

/** All uploads are decoded and re-encoded as PNG; client filenames are never used. */
@Service
public class ImageStorage {
    public static final String PREFIX="private-image:";
    private final Path root;
    private final long maxBytes;
    public ImageStorage(@Value("${museum.image.storage-path:./runtime/images}") String root,
                        @Value("${museum.image.max-bytes:5242880}") long maxBytes) {
        this.root=Path.of(root).toAbsolutePath().normalize();this.maxBytes=maxBytes;
    }
    public byte[] validate(MultipartFile file) {
        if(file.isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Empty image");
        if(file.getSize()>maxBytes)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Image too large");
        String mime=file.getContentType();
        if(!Set.of("image/png","image/jpeg").contains(mime==null?"":mime))
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"Only PNG and JPEG images are supported");
        try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))) {
            var readers=ImageIO.getImageReaders(stream);
            if(!readers.hasNext())throw new IOException();
            var reader=readers.next();
            try {
                reader.setInput(stream,true,true);
                String format=reader.getFormatName();
                if(!(mime.equals("image/png")&&format.equalsIgnoreCase("png"))&&!(mime.equals("image/jpeg")&&format.equalsIgnoreCase("jpeg")))throw new IOException();
                int w=reader.getWidth(0),h=reader.getHeight(0);
                if(w<=0||h<=0||w>6000||h>6000||(long)w*h>12000000)throw new IOException();
                BufferedImage decoded=reader.read(0);
                var out=new ByteArrayOutputStream();ImageIO.write(decoded,"png",out);return out.toByteArray();
            } finally {reader.dispose();}
        } catch(IOException|IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid image or dimensions");}
    }
    private Path resolve(String key) {
        if(key==null||!key.matches("(?:exhibit-[1-9][0-9]*|exhibition-[1-9][0-9]*|private-[a-f0-9-]{36})\\.png"))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");
        Path path=root.resolve(key).normalize();
        if(!path.getParent().equals(root)||Files.isSymbolicLink(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");
        return path;
    }
    public String privateKey(){return "private-"+UUID.randomUUID()+".png";}
    public String fromReference(String ref){if(ref==null||!ref.startsWith(PREFIX))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");String key=ref.substring(PREFIX.length());if(!key.startsWith("private-"))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");resolve(key);return key;}
    public boolean exists(String key){return Files.isRegularFile(resolve(key),LinkOption.NOFOLLOW_LINKS);}
    public void write(String key,byte[] bytes) {
        Path temp=null;
        try {Files.createDirectories(root);Path dest=resolve(key);temp=Files.createTempFile(root,"image-",".tmp");Files.write(temp,bytes);Files.move(temp,dest,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
        catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Image storage unavailable");}
        finally {if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}}
    }
    public byte[] read(String key){try{return Files.readAllBytes(resolve(key));}catch(IOException e){throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");}}
    public void delete(String key){try{Files.deleteIfExists(resolve(key));}catch(IOException e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Image storage unavailable");}}
}

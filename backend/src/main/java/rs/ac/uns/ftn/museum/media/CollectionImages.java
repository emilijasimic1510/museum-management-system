package rs.ac.uns.ftn.museum.media;

import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Bundled CC0 reference images; attribution is returned only for exact seeded bytes. */
public final class CollectionImages {
    private CollectionImages() {}
    private static final byte[][] IMAGES = new byte[5][];
    private static final List<Map<String,Object>> SOURCES;
    static {
        try (var in=CollectionImages.class.getResourceAsStream("/collection-images/sources.json")) {
            SOURCES=new ObjectMapper().readValue(in,new com.fasterxml.jackson.core.type.TypeReference<>(){});
            for(int i=0;i<IMAGES.length;i++) {
                try(var file=CollectionImages.class.getResourceAsStream("/collection-images/met-"+i+".jpg")) {
                    var decoded=ImageIO.read(file);
                    if(decoded==null)throw new IOException("Invalid collection image");
                    var out=new ByteArrayOutputStream();ImageIO.write(decoded,"png",out);IMAGES[i]=out.toByteArray();
                }
            }
        } catch(IOException e) {throw new ExceptionInInitializerError(e);}
    }
    public static byte[] image(int variant){return IMAGES[Math.floorMod(variant,IMAGES.length)].clone();}
    public static Map<String,Object> attribution(byte[] bytes){
        for(int i=0;i<IMAGES.length;i++)if(Arrays.equals(bytes,IMAGES[i]))return SOURCES.get(i);
        return Map.of();
    }
    public static int theme(int group){return new int[]{2,4,3,2,4,0,1,4}[Math.floorMod(group,8)];}
}

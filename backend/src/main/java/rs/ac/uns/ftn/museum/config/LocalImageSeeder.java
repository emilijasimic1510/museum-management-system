package rs.ac.uns.ftn.museum.config;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.boot.*;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.museum.media.ImageStorage;

/** Open-access thematic reference images from The Metropolitan Museum of Art. Never overwrites curator images. */
@Component @Profile("local & !production & !prod") @Order(30)
public class LocalImageSeeder implements ApplicationRunner {
    private final JdbcTemplate db;private final ImageStorage storage;
    public LocalImageSeeder(JdbcTemplate db,ImageStorage storage){this.db=db;this.storage=storage;}
    @Override @Transactional public void run(ApplicationArguments args){
        db.execute("select pg_advisory_xact_lock(20261003, 1)");
        var exhibits=db.queryForList("select id from eksponat where inventarski_broj like 'DEMO-RICH-EX-%'",Long.class);
        for(long id:exhibits){
            String inventory=db.queryForObject("select inventarski_broj from eksponat where id=?",String.class,id);
            int group=(Integer.parseInt(inventory.substring(inventory.length()-3))-1)/7;
            putMissing("exhibit-"+id+".png",(int)id,rs.ac.uns.ftn.museum.media.CollectionImages.theme(group));
        }
        // The compact local catalogue also receives real CC0 artwork, so no public
        // exhibit card falls back to a generated illustration.
        var baseExhibits=db.queryForList("select id from eksponat where inventarski_broj not like 'DEMO-RICH-EX-%'",Long.class);
        for(long id:baseExhibits) putMissing("exhibit-"+id+".png",(int)id,rs.ac.uns.ftn.museum.media.CollectionImages.theme((int)(id%8)));
        var allShows=db.queryForList("select id from izlozba",Long.class);
        for(long id:allShows) putMissing("exhibition-"+id+".png",(int)id,rs.ac.uns.ftn.museum.media.CollectionImages.theme((int)((id+2)%8)));
        for(int i=0;i<RichDemoCatalog.SHOWS.size();i++){
            var shows=db.queryForList("select id from izlozba where naziv=?",Long.class,RichDemoCatalog.SHOWS.get(i)[0]);
            for(long id:shows)putMissing("exhibition-"+id+".png",i+80,rs.ac.uns.ftn.museum.media.CollectionImages.theme(Integer.parseInt(RichDemoCatalog.SHOWS.get(i)[3])));
            for(int slot=0;slot<8;slot++){
                String token=RichDemoCatalog.token(i,slot);
                var parts=db.queryForList("select id_ucesca from ucesce_eksponata_na_izlozbi where qr_token=?",Long.class,token);
                for(long id:parts){
                    String key="private-"+UUID.nameUUIDFromBytes(("image:"+token).getBytes(StandardCharsets.UTF_8))+".png";
                    String ref=ImageStorage.PREFIX+key;
                    var existing=db.queryForList("select putanja from digitalni_sadrzaj where ucesce_id=? and tip_sadrzaja='SLIKA'",String.class,id);
                    if(existing.isEmpty()){
                        putMissing(key,i*8+slot,rs.ac.uns.ftn.museum.media.CollectionImages.theme(RichDemoCatalog.workIndex(i,slot)/7));
                        db.update("insert into digitalni_sadrzaj(ucesce_id,tip_sadrzaja,putanja,jezik,verzija,generisan_automatski,datum_kreiranja) values(?,'SLIKA',?,null,1,false,now())",id,ref);
                    }else if(existing.contains(ref))putMissing(key,i*8+slot,rs.ac.uns.ftn.museum.media.CollectionImages.theme(RichDemoCatalog.workIndex(i,slot)/7));
                }
            }
        }
    }
    private void putMissing(String key,int variant,int asset){
        if(!storage.exists(key) || java.util.Arrays.equals(storage.read(key),legacyIllustration(variant)))
            storage.write(key,rs.ac.uns.ftn.museum.media.CollectionImages.image(asset));
    }
    public static byte[] illustration(int variant){return rs.ac.uns.ftn.museum.media.CollectionImages.image(variant);}
    private static byte[] legacyIllustration(int variant){
        var image=new BufferedImage(800,600,BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();
        try {g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(239,234,222));g.fillRect(0,0,800,600);
            g.setColor(Color.getHSBColor((variant%12)/12f,.24f,.52f));g.fillRoundRect(140,65,520,470,240,240);
            g.setColor(new Color(212,163,104));g.fillOval(200+(variant%4)*65,125,165,165);
            g.setColor(new Color(247,242,228));g.fillRect(215,320,370,150);
            g.setColor(new Color(54,72,65));for(int x=240;x<580;x+=60)g.fillRect(x,350,18,120);
            g.setFont(new Font(Font.SANS_SERIF,Font.PLAIN,14));g.drawString("DEMO / STUDY "+(variant+1),40,560);
        }finally{g.dispose();}
        try{var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();}catch(IOException e){throw new IllegalStateException(e);}
    }
}

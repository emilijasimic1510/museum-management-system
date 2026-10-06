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

/** Original geometric demo illustrations, generated locally. Never overwrites curator images. */
@Component @Profile("local & !production & !prod") @Order(30)
public class LocalImageSeeder implements ApplicationRunner {
    private final JdbcTemplate db;private final ImageStorage storage;
    public LocalImageSeeder(JdbcTemplate db,ImageStorage storage){this.db=db;this.storage=storage;}
    @Override @Transactional public void run(ApplicationArguments args){
        db.execute("select pg_advisory_xact_lock(20261003, 1)");
        var exhibits=db.queryForList("select id from eksponat where inventarski_broj like 'DEMO-RICH-EX-%'",Long.class);
        for(long id:exhibits)putMissing("exhibit-"+id+".png",(int)id);
        for(int i=0;i<RichDemoCatalog.SHOWS.size();i++){
            var shows=db.queryForList("select id from izlozba where naziv=?",Long.class,RichDemoCatalog.SHOWS.get(i)[0]);
            for(long id:shows)putMissing("exhibition-"+id+".png",i+80);
            for(int slot=0;slot<8;slot++){
                String token=RichDemoCatalog.token(i,slot);
                var parts=db.queryForList("select id_ucesca from ucesce_eksponata_na_izlozbi where qr_token=?",Long.class,token);
                for(long id:parts){
                    String key="private-"+UUID.nameUUIDFromBytes(("image:"+token).getBytes(StandardCharsets.UTF_8))+".png";
                    String ref=ImageStorage.PREFIX+key;
                    var existing=db.queryForList("select putanja from digitalni_sadrzaj where ucesce_id=? and tip_sadrzaja='SLIKA'",String.class,id);
                    if(existing.isEmpty()){
                        putMissing(key,i*8+slot);
                        db.update("insert into digitalni_sadrzaj(ucesce_id,tip_sadrzaja,putanja,jezik,verzija,generisan_automatski,datum_kreiranja) values(?,'SLIKA',?,null,1,false,now())",id,ref);
                    }else if(existing.contains(ref))putMissing(key,i*8+slot);
                }
            }
        }
    }
    private void putMissing(String key,int variant){if(!storage.exists(key))storage.write(key,illustration(variant));}
    public static byte[] illustration(int variant){
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

package rs.ac.uns.ftn.museum.media;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.repository.*;
import rs.ac.uns.ftn.museum.journey.service.VisitService;

@Service
public class ImageService {
    private final ImageStorage storage;private final ExhibitRepository exhibits;private final ExhibitionRepository exhibitions;
    private final ExhibitParticipationRepository participations;private final DigitalContentRepository contents;private final VisitService visits;private final Clock clock;
    public ImageService(ImageStorage s,ExhibitRepository e,ExhibitionRepository x,ExhibitParticipationRepository p,DigitalContentRepository c,VisitService v,Clock clock){storage=s;exhibits=e;exhibitions=x;participations=p;contents=c;visits=v;this.clock=clock;}
    public record ImageResult(Long contentId){}
    private String publicKey(String kind,Long id,boolean publishing){
        if(id==null||id<=0)throw missing();
        if("exhibits".equals(kind)){if(!exhibits.existsById(id))throw missing();return "exhibit-"+id+".png";}
        if("exhibitions".equals(kind)){var e=exhibitions.findById(id).orElseThrow(ImageService::missing);if(!publishing&&!Boolean.TRUE.equals(e.getOnlineVisible()))throw missing();return "exhibition-"+id+".png";}
        throw missing();
    }
    private static ResponseStatusException missing(){return new ResponseStatusException(HttpStatus.NOT_FOUND,"Image not found");}
    @Transactional(readOnly=true) public byte[] publicImage(String kind,Long id){return storage.read(publicKey(kind,id,false));}
    @Transactional(readOnly=true) public byte[] curatorPublicImage(String kind,Long id){return storage.read(publicKey(kind,id,true));}
    @Transactional public ImageResult uploadPublic(String kind,Long id,MultipartFile file){String key=publicKey(kind,id,true);storage.write(key,storage.validate(file));return new ImageResult(null);}
    @Transactional public ImageResult uploadContext(Long id,MultipartFile file){
        var p=participations.findLockedById(id).orElseThrow(ImageService::missing);
        byte[] bytes=storage.validate(file);String key=storage.privateKey();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){@Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)storage.delete(key);}});
        storage.write(key,bytes);
        var d=new DigitalContent();d.setParticipation(p);d.setType(ContentType.SLIKA);d.setPath(ImageStorage.PREFIX+key);d.setLanguage(null);
        d.setVersion(contents.findByParticipationIdOrderByVersionDescId(id).stream().filter(c->c.getType()==ContentType.SLIKA).mapToInt(DigitalContent::getVersion).max().orElse(0)+1);
        d.setAutomaticallyGenerated(false);d.setCreatedAt(clock.instant());contents.saveAndFlush(d);return new ImageResult(d.getId());
    }
    @Transactional(readOnly=true) public byte[] context(Long visit,Long participation,Long content){
        visits.content(visit,participation);return curatorContext(participation,content);
    }
    @Transactional(readOnly=true) public byte[] curatorContext(Long participation,Long content){
        var d=contents.findById(content).orElseThrow(ImageService::missing);
        if(d.getType()!=ContentType.SLIKA||!d.getParticipation().getId().equals(participation))throw missing();
        return storage.read(storage.fromReference(d.getPath()));
    }
}

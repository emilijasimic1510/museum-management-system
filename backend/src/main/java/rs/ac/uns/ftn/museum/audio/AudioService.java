package rs.ac.uns.ftn.museum.audio;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.dto.ExhibitDtos.DigitalContentResponse;
import rs.ac.uns.ftn.museum.catalog.repository.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import rs.ac.uns.ftn.museum.journey.service.VisitService;
@Service
public class AudioService {
    private final ExhibitParticipationRepository participations;private final DigitalContentRepository contents;
    private final TtsService tts;private final AudioStorage storage;private final Clock clock;private final VisitService visits; private final rs.ac.uns.ftn.museum.text.TextService texts;
    public AudioService(ExhibitParticipationRepository p,DigitalContentRepository c,TtsService t,AudioStorage s,Clock clock,VisitService v){this(p,c,t,s,clock,v,null);}
    @org.springframework.beans.factory.annotation.Autowired
    public AudioService(ExhibitParticipationRepository p,DigitalContentRepository c,TtsService t,AudioStorage s,Clock clock,VisitService v,rs.ac.uns.ftn.museum.text.TextService x){participations=p;contents=c;tts=t;storage=s;this.clock=clock;visits=v;texts=x;}
    @Transactional public DigitalContentResponse generate(Long id,String language){
        if(!"sr".equals(language)&&!"en".equals(language))throw new InvalidRequestException("Podržani su jezici sr i en.");
        var p=participations.findLockedById(id).orElseThrow(()->new NotFoundException("Participation not found"));
        String spoken=p.getContextDescription();
        if(texts!=null){
            var translated=texts.curatorIfPresent(id,language);
            if(translated.isPresent()) spoken=translated.get().text();
            else if("en".equals(language)) spoken=null;
        }
        if(spoken==null||spoken.isBlank())throw new InvalidRequestException("Prvo unesite tekstualni sadržaj za izabrani jezik.");
        int version=contents.findByParticipationIdOrderByVersionDescId(id).stream()
            .filter(d->d.getType()==ContentType.AUDIO&&language.equals(d.getLanguage())).mapToInt(DigitalContent::getVersion).max().orElse(0)+1;
        String ref=storage.reference(id,language,version);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)storage.delete(ref);}
        });
        try {
            var file=storage.prepare(ref);tts.generate(spoken,language,file);AudioStorage.validateWav(file);
            var d=new DigitalContent();d.setParticipation(p);d.setType(ContentType.AUDIO);d.setPath(ref);d.setLanguage(language);
            d.setVersion(version);d.setAutomaticallyGenerated(true);d.setCreatedAt(clock.instant());contents.saveAndFlush(d);
            return new DigitalContentResponse(d.getId(),id,d.getType(),"",language,version,true,d.getCreatedAt());
        }catch(RuntimeException e){storage.delete(ref);throw e;}
    }
    @Transactional(readOnly=true) public byte[] download(Long visit,Long participation,Long content){
        visits.content(visit,participation);
        var d=contents.findById(content).orElseThrow(()->new NotFoundException("Audio not found"));
        if(!d.getParticipation().getId().equals(participation)||d.getType()!=ContentType.AUDIO||!d.getPath().startsWith(AudioStorage.PREFIX))
            throw new NotFoundException("Audio not found for this participation");
        return storage.read(d.getPath());
    }
}

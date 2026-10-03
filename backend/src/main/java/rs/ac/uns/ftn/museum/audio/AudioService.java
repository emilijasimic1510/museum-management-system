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
    private final TtsService tts;private final AudioStorage storage;private final Clock clock;private final VisitService visits;
    public AudioService(ExhibitParticipationRepository p,DigitalContentRepository c,TtsService t,AudioStorage s,Clock clock,VisitService v){participations=p;contents=c;tts=t;storage=s;this.clock=clock;visits=v;}
    @Transactional public DigitalContentResponse generate(Long id,String language){
        if(!"sr".equals(language))throw new InvalidRequestException("Podržan je jezik sr (srpski).");
        var p=participations.findLockedById(id).orElseThrow(()->new NotFoundException("Participation not found"));
        if(p.getContextDescription()==null||p.getContextDescription().isBlank())throw new InvalidRequestException("Prvo unesite kontekstualni opis ove postavke.");
        int version=contents.findByParticipationIdOrderByVersionDescId(id).stream()
            .filter(d->d.getType()==ContentType.AUDIO&&language.equals(d.getLanguage())).mapToInt(DigitalContent::getVersion).max().orElse(0)+1;
        String ref=storage.reference(id,language,version);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)storage.delete(ref);}
        });
        try {
            var file=storage.prepare(ref);tts.generate(p.getContextDescription(),language,file);AudioStorage.validateWav(file);
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

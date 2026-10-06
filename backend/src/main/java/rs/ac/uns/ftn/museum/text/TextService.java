package rs.ac.uns.ftn.museum.text;

import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.repository.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import rs.ac.uns.ftn.museum.journey.service.VisitService;
import java.util.Optional;

@Service
public class TextService {
  public record TextResponse(Long id,Long participationId,String language,Integer version,String text){}
  private final ExhibitParticipationRepository parts; private final DigitalContentRepository contents; private final TextStorage storage; private final Clock clock; private final VisitService visits;
  public TextService(ExhibitParticipationRepository p,DigitalContentRepository c,TextStorage s,Clock k,VisitService v){parts=p;contents=c;storage=s;clock=k;visits=v;}
  @Transactional public TextResponse save(Long id,String language,String text){
    if(!"sr".equals(language)&&!"en".equals(language))throw new InvalidRequestException("Language must be sr or en");
    if(text==null||text.isBlank()||text.length()>100000)throw new InvalidRequestException("Text is required and must be at most 100000 characters");
    var p=parts.findLockedById(id).orElseThrow(()->new NotFoundException("Participation not found"));
    int v=contents.findByParticipationIdOrderByVersionDescId(id).stream().filter(d->d.getType()==ContentType.TEKST&&language.equals(d.getLanguage())).mapToInt(DigitalContent::getVersion).max().orElse(0)+1;
    String ref=storage.reference(id,language,v);storage.write(ref,text.trim());
    try{var d=new DigitalContent();d.setParticipation(p);d.setType(ContentType.TEKST);d.setPath(ref);d.setLanguage(language);d.setVersion(v);d.setAutomaticallyGenerated(false);d.setCreatedAt(clock.instant());contents.saveAndFlush(d);return new TextResponse(d.getId(),id,language,v,text.trim());}
    catch(RuntimeException e){storage.delete(ref);throw e;}
  }
  @Transactional(readOnly=true) public TextResponse curator(Long id,String language){return latest(id,language);}
  /** Non-throwing lookup for callers that may legitimately fall back to contextual text. */
  @Transactional(readOnly=true) public Optional<TextResponse> curatorIfPresent(Long id,String language){
    try{return Optional.of(latest(id,language));}catch(NotFoundException e){return Optional.empty();}
  }
  @Transactional(readOnly=true) public TextResponse visitor(Long visit,Long id,Long content){visits.content(visit,id);var d=contents.findById(content).orElseThrow(()->new NotFoundException("Text not found"));if(!d.getParticipation().getId().equals(id)||d.getType()!=ContentType.TEKST)throw new NotFoundException("Text not found");String value=d.getPath().startsWith(TextStorage.PREFIX)?storage.read(d.getPath()):d.getParticipation().getContextDescription();if(value==null||value.isBlank())throw new NotFoundException("Text not found");return new TextResponse(d.getId(),id,d.getLanguage(),d.getVersion(),value);}
  private TextResponse latest(Long id,String lang){var rows=contents.findByParticipationIdOrderByVersionDescId(id).stream().filter(d->d.getType()==ContentType.TEKST&&lang.equals(d.getLanguage())).toList();if(rows.isEmpty())throw new NotFoundException("Text not found");var d=rows.get(0);return new TextResponse(d.getId(),id,lang,d.getVersion(),storage.read(d.getPath()));}
}

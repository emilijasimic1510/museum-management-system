package rs.ac.uns.ftn.museum.audio;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.support.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import javax.sound.sampled.*;
import java.io.*;
import rs.ac.uns.ftn.museum.catalog.domain.*;
import rs.ac.uns.ftn.museum.catalog.repository.*;
import rs.ac.uns.ftn.museum.catalog.exception.*;
import rs.ac.uns.ftn.museum.journey.service.VisitService;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;
class AudioServiceTests {
 @TempDir Path root;
 ExhibitParticipationRepository parts;DigitalContentRepository contents;TtsService tts;AudioStorage storage;VisitService visits;AudioService service;ExhibitParticipation part;
 @BeforeEach void setup(){parts=mock(ExhibitParticipationRepository.class);contents=mock(DigitalContentRepository.class);tts=mock(TtsService.class);visits=mock(VisitService.class);storage=new AudioStorage(root.toString());
 service=new AudioService(parts,contents,tts,storage,Clock.systemUTC(),visits);part=mock(ExhibitParticipation.class);when(part.getId()).thenReturn(7L);when(part.getContextDescription()).thenReturn("Kontekst ove postavke");when(parts.findLockedById(7L)).thenReturn(Optional.of(part));when(contents.findByParticipationIdOrderByVersionDescId(7L)).thenReturn(List.of());TransactionSynchronizationManager.initSynchronization();}
 @AfterEach void cleanup(){TransactionSynchronizationManager.clearSynchronization();}
 static void wav(Path file)throws Exception{try(var in=new AudioInputStream(new ByteArrayInputStream(new byte[160]),new AudioFormat(8000,16,1,true,false),80)){AudioSystem.write(in,AudioFileFormat.Type.WAVE,file.toFile());}}
 @Test void generatesExactParticipationAndNextVersion()throws Exception{
 var old=new DigitalContent();old.setType(ContentType.AUDIO);old.setLanguage("sr");old.setVersion(4);when(contents.findByParticipationIdOrderByVersionDescId(7L)).thenReturn(List.of(old));
 doAnswer(a->{wav(a.getArgument(2));return null;}).when(tts).generate(anyString(),eq("sr"),any());
 var response=service.generate(7L,"sr");assertThat(response.version()).isEqualTo(5);assertThat(response.path()).isEmpty();assertThat(response.automaticallyGenerated()).isTrue();
 verify(contents).saveAndFlush(argThat(d->d.getParticipation()==part&&d.getType()==ContentType.AUDIO&&d.getCreatedAt()!=null&&d.getVersion()==5));verify(tts).generate(eq("Kontekst ove postavke"),eq("sr"),any());
 for(var sync:TransactionSynchronizationManager.getSynchronizations())sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
 try(var files=Files.list(root)){assertThat(files.count()).isZero();}
 }
 @Test void blankContextNeverCallsTts(){for(String text:Arrays.asList(null,""," \n\t")){when(part.getContextDescription()).thenReturn(text);assertThatThrownBy(()->service.generate(7L,"sr")).isInstanceOf(InvalidRequestException.class);}verifyNoInteractions(tts);verify(contents,never()).saveAndFlush(any());}
 @Test void failureCleansPartialFileAndDoesNotSave()throws Exception{doAnswer(a->{Files.writeString(a.getArgument(2),"partial");throw new AudioException("failed");}).when(tts).generate(anyString(),anyString(),any());assertThatThrownBy(()->service.generate(7L,"sr")).isInstanceOf(AudioException.class);verify(contents,never()).saveAndFlush(any());try(var files=Files.list(root)){assertThat(files.count()).isZero();}}
 @Test void missingWavDoesNotSave(){assertThatThrownBy(()->service.generate(7L,"sr")).isInstanceOf(AudioException.class);verify(contents,never()).saveAndFlush(any());}
 @Test void invalidLanguageAndMissingParticipation(){assertThatThrownBy(()->service.generate(7L,"../sr")).isInstanceOf(InvalidRequestException.class);assertThatThrownBy(()->service.generate(99L,"sr")).isInstanceOf(NotFoundException.class);verifyNoInteractions(tts);}
 @Test void downloadUsesVisitGuardBeforeReading(){doThrow(new org.springframework.security.access.AccessDeniedException("scan required")).when(visits).content(1L,7L);assertThatThrownBy(()->service.download(1L,7L,9L)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);verifyNoInteractions(contents);}
 @Test void otherParticipationContentIsRejected(){var d=new DigitalContent();var other=mock(ExhibitParticipation.class);when(other.getId()).thenReturn(8L);d.setParticipation(other);when(contents.findById(9L)).thenReturn(Optional.of(d));assertThatThrownBy(()->service.download(1L,7L,9L)).isInstanceOf(NotFoundException.class);}
 @Test void validDownloadReturnsWav()throws Exception{var d=new DigitalContent();d.setParticipation(part);d.setType(ContentType.AUDIO);d.setPath(storage.reference(7,"sr",1));wav(storage.prepare(d.getPath()));when(contents.findById(9L)).thenReturn(Optional.of(d));assertThat(service.download(1L,7L,9L)).startsWith((byte)'R',(byte)'I',(byte)'F',(byte)'F');verify(visits).content(1L,7L);}
 @Test void traversalRejected(){for(String path:List.of("../secret.wav","private-audio:../secret.wav","C:/secret.wav","private-audio:7-sr-v1-../../secret.wav"))assertThatThrownBy(()->storage.resolve(path)).isInstanceOf(AudioException.class);}
 @Test void missingExecutableFailsOnlyWhenCalled(){var engine=new EspeakTtsService(root.resolve("missing-engine").toString(),1);assertThatThrownBy(()->engine.generate("tekst","sr",root.resolve("output.wav"))).isInstanceOf(AudioException.class).hasMessageContaining("nije dostupan");try(var files=Files.list(root)){assertThat(files.count()).isZero();}catch(IOException e){throw new RuntimeException(e);}}
 @Test void processFailureTimeoutAndMissingWavAreReported()throws Exception{
   for(int scenario=0;scenario<3;scenario++){
     Process process=mock(Process.class);when(process.waitFor(anyLong(),eq(java.util.concurrent.TimeUnit.SECONDS))).thenReturn(scenario!=1);
     when(process.exitValue()).thenReturn(scenario==0?1:0);when(process.isAlive()).thenReturn(scenario==1);
     try(var construction=mockConstruction(ProcessBuilder.class,withSettings().defaultAnswer(RETURNS_SELF),(builder,context)->{when(builder.start()).thenReturn(process);})){
       int current=scenario;
       assertThatThrownBy(()->new EspeakTtsService("test-engine",1).generate("tekst","sr",root.resolve("output.wav")))
         .isInstanceOf(AudioException.class).hasMessageContaining(current==0?"nije uspešno":current==1?"vremensko":"ispravan WAV");
       if(scenario==1)verify(process).destroyForcibly();
     }
   }
 }

}

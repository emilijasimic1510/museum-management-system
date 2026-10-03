package rs.ac.uns.ftn.museum.manager;

import java.time.*;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import static rs.ac.uns.ftn.museum.manager.ManagerDtos.*;

@Service
public class ManagerAnalyticsService {
    private final ManagerAnalyticsRepository repository;
    private final Clock clock;
    public static final ZoneId ZONE=ZoneId.of("Europe/Belgrade");
    public ManagerAnalyticsService(ManagerAnalyticsRepository repository,Clock clock){this.repository=repository;this.clock=clock;}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Dashboard dashboard(LocalDate from,LocalDate to){
        if(from==null||to==null||from.isAfter(to)||from.getYear()<1900||to.getYear()>9998)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Invalid date range");
        Instant start=from.atStartOfDay(ZONE).toInstant(),until=to.plusDays(1).atStartOfDay(ZONE).toInstant();
        var comments=repository.comments(start,until);
        return new Dashboard(from,to,ZONE.getId(),clock.instant(),repository.attendance(start,until),repository.revenue(start,until),
            repository.tickets(start,until),repository.popularity(start,until),repository.events(start,until),repository.ratings(start,until),
            comments.stream().limit(200).toList(),comments.size()>200,repository.transitions(start,until),repository.contentInventory(),List.of(
                "Datumi uključuju oba izabrana dana u zoni Europe/Belgrade. Posete: vreme ulaska; prodaja: datum kupovine; prihodi: datum transakcije; skeniranja: vreme skeniranja; ocene: datum ocene; događaji: početak događaja.",
                "Prihod je zbir iznosa transakcija koje trenutno imaju status USPESNA. Refundirane, neuspešne i pokrenute transakcije nisu prihod. Nema datuma refundacije niti istorije statusa: ovo nije rekonstrukcija stanja na poslednji dan perioda.",
                "Vrednost plaćenih karata sabira CenaPlaćena jednom po karti sa uspešnom transakcijom u periodu; cenovnik se ne koristi. Statusi karata i prijava prikazuju sadašnje stanje, ne istoriju promena.",
                "Prelazi predstavljaju uzastopna zabeležena skeniranja u istoj poseti, po vremenu drugog skena. Jednaka vremena i ponavljanje iste postavke nisu prelazi. To nije dokaz fizičke putanje; mesto i razlog prekida obilaska se ne mogu pouzdano utvrditi.",
                "Trajanje je prosek samo unetih TrajanjePregleda vrednosti (sekunde), nije automatski izmereno zadržavanje. Nema play/view evidencije: digitalni sadržaj je trenutni inventar zapisa/verzija po jeziku, tipu i postavci, nezavisno od datuma filtera."));
    }
}

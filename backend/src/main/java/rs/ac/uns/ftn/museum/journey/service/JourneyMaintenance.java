package rs.ac.uns.ftn.museum.journey.service;

import java.sql.Timestamp;
import java.time.Clock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.museum.notification.EmailService;
import rs.ac.uns.ftn.museum.journey.repository.VisitRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JourneyMaintenance {
    private final EmailService email;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final VisitRepository visits;
    public JourneyMaintenance(JdbcTemplate jdbc,Clock clock,EmailService email,VisitRepository visits) {
        this.jdbc=jdbc;this.clock=clock;this.email=email;this.visits=visits;
    }
    @Transactional public void advanceStatuses() {
        Timestamp now=Timestamp.from(clock.instant());
        // Same parent-first ordering as checkout; no deletion of sales or visit history.
        jdbc.queryForList("select id from termin_izlozbe order by id for update");
        jdbc.queryForList("select id from ulaznica where status in ('KUPLJENA','AKTIVNA') order by id for update");
        var completed=jdbc.queryForList("""
            update poseta p set status='ZAVRSENA',
             vreme_izlaska=greatest(p.vreme_ulaska,least(t.kraj,u.vazi_do))
            from ulaznica u join termin_izlozbe t on t.id=u.termin_id
            where p.ulaznica_id=u.id and p.status='AKTIVNA' and (t.kraj<=? or u.vazi_do<=?)
            returning p.id
            """,Long.class,now,now);
        jdbc.update("update ulaznica u set status='ISKORISCENA' where u.status='AKTIVNA' and exists (select 1 from poseta p where p.ulaznica_id=u.id and p.status='ZAVRSENA')");
        jdbc.update("update preporucena_ruta r set status='ZAVRSENA' from poseta p where p.id=r.poseta_id and p.status='ZAVRSENA' and r.status='AKTIVNA'");
        jdbc.update("update ulaznica u set status='ISTEKLA' from termin_izlozbe t where t.id=u.termin_id and u.status='KUPLJENA' and (u.vazi_do<=? or t.kraj<=?)",now,now);
        jdbc.update("update termin_izlozbe set status='ZAVRSEN' where kraj<=? and status not in ('OTKAZAN','ZAVRSEN')",now);
        jdbc.update("update izlozba set status='ZAVRSENA' where datum_zavrsetka<=? and status not in ('OTKAZANA','ZAVRSENA')",now);
        jdbc.update("update izlozba set status='AKTIVNA' where datum_pocetka<=? and datum_zavrsetka>? and status='PLANIRANA'",now,now);
        for(Long id:completed)visits.findById(id).ifPresent(email::sendVisitCompletionThankYou);
    }
}

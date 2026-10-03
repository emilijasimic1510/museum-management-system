package rs.ac.uns.ftn.museum.journey.controller;

import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.museum.journey.service.VisitService;

/** Deliberately predictable QR examples are available only in the local profile. */
@RestController
@Profile("local & !production & !prod")
@RequestMapping("/api/local-demo")
public class LocalDemoQrController {
    private final JdbcTemplate jdbc;
    private final VisitService visits;
    public LocalDemoQrController(JdbcTemplate jdbc, VisitService visits) { this.jdbc=jdbc;this.visits=visits; }
    public record DemoQr(long participationId,String name,String token,long exhibitionId,String exhibitionName) {}
    @GetMapping("/enabled") public Map<String,Boolean> enabled() { return Map.of("enabled",true); }
    @GetMapping("/board") public List<DemoQr> board() { return entries(null); }
    @GetMapping("/visits/{id}") public List<DemoQr> forVisit(@PathVariable Long id) {
        visits.requireActiveOwner(id);
        Long exhibition=jdbc.queryForObject("select t.izlozba_id from poseta p join ulaznica u on u.id=p.ulaznica_id join termin_izlozbe t on t.id=u.termin_id where p.id=?",Long.class,id);
        return entries(exhibition);
    }
    private List<DemoQr> entries(Long exhibition) {
        return jdbc.query("""
            select p.id_ucesca,e.naziv,p.qr_token,p.izlozba_id,x.naziv from ucesce_eksponata_na_izlozbi p
            join eksponat e on e.id=p.eksponat_id
            join izlozba x on x.id=p.izlozba_id
            where p.qr_token in ('DEMO-QR-001','DEMO-QR-002','DEMO-QR-003',
                'DEMO-SCULPTURE-QR-001','DEMO-SCULPTURE-QR-002','DEMO-SCULPTURE-QR-003',
                'DEMO-CITY-QR-001','DEMO-CITY-QR-002','DEMO-CITY-QR-003','DEMO-LIGHT-CITY-QR-001')
            and (cast(? as bigint) is null or p.izlozba_id=?)
            and (p.datum_postavljanja is null or p.datum_postavljanja<=now())
            and (p.datum_uklanjanja is null or p.datum_uklanjanja>now())
            order by p.izlozba_id,p.redosled_obilaska,p.id_ucesca
            """,(rs,row)->new DemoQr(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getLong(4),rs.getString(5)),exhibition,exhibition);
    }
}

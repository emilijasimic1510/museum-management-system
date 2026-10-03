package rs.ac.uns.ftn.museum.manager;

import com.fasterxml.jackson.databind.*;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.museum.support.CatalogAuthTestBase;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc(print=MockMvcPrint.NONE) @ActiveProfiles("test")
class ManagerAnalyticsTests extends CatalogAuthTestBase {
    @Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;@Autowired ObjectMapper json;
    @Autowired ManagerAnalyticsService service;@Autowired PasswordEncoder encoder;
    private final LocalDate day=LocalDate.of(2035,1,1);
    private Timestamp ts(String value){return Timestamp.from(Instant.parse(value));}
    private long id(String sql,Object... args){return jdbc.queryForObject(sql,Long.class,args);}
    private record Fixture(long exhibition,long term,long first,long second,long visit,long ticket){}
    private long ticket(long exhibition,long term,String time){
        long id=id("insert into ulaznica(termin_id,cenovnik_izlozba_id,cenovnik_vazi_od,kategorija_cene,datum_kupovine,cena_placena,status,qr_token,vazi_do) values(?,?,'2034-01-01Z','ODRASLI',?,10,'KUPLJENA',?,'2035-01-03Z') returning id",term,exhibition,ts(time),UUID.randomUUID().toString());
        jdbc.update("insert into transakcija(ulaznica_id,iznos,nacin_placanja,status,datum_transakcije) values(?,10,'GOTOVINA','USPESNA',?)",id,ts(time));return id;
    }
    private Fixture fixture(){
        long m=id("insert into muzej(naziv,adresa,aktivan) values('Analytics museum','Adresa',true) returning id");
        long h=id("insert into sala(muzej_id,naziv) values(?,'Analytics hall') returning id",m);
        long e=id("insert into izlozba(sala_id,naziv,datum_pocetka,datum_zavrsetka,status,online_vidljiva) values(?,'Analytics exhibition','2034-01-01Z','2036-01-01Z','AKTIVNA',true) returning id",h);
        long tr=id("insert into termin_izlozbe(izlozba_id,pocetak,kraj,kapacitet,status) values(?,'2034-12-30Z','2035-01-03Z',50,'OTVOREN') returning id",e);
        jdbc.update("insert into cenovnik_izlozbe(izlozba_id,datum_vazenja_od,kategorija,cena) values(?,'2034-01-01Z','ODRASLI',10)",e);
        long x=id("insert into eksponat(naziv,inventarski_broj) values('Analytics exhibit',?) returning id",UUID.randomUUID().toString());
        long p1=id("insert into ucesce_eksponata_na_izlozbi(izlozba_id,eksponat_id,qr_token) values(?,?,?) returning id_ucesca",e,x,UUID.randomUUID().toString());
        long p2=id("insert into ucesce_eksponata_na_izlozbi(izlozba_id,eksponat_id,qr_token) values(?,?,?) returning id_ucesca",e,x,UUID.randomUUID().toString());
        long t1=ticket(e,tr,"2034-12-31T23:00:00Z"),t2=ticket(e,tr,"2035-01-01T01:00:00Z");
        long v1=id("insert into poseta(ulaznica_id,vreme_ulaska,status) values(?,'2035-01-01T01:00Z','AKTIVNA') returning id",t1);
        long v2=id("insert into poseta(ulaznica_id,vreme_ulaska,status) values(?,'2035-01-01T02:00Z','AKTIVNA') returning id",t2);
        scan(v1,p1,"2035-01-01T01:01:00Z",10);scan(v1,p1,"2035-01-01T01:02:00Z",null);scan(v1,p2,"2035-01-01T01:03:00Z",30);scan(v2,p1,"2035-01-01T02:01:00Z",20);
        jdbc.update("update poseta set status='ZAVRSENA',vreme_izlaska='2035-01-01T03:00Z' where id in (?,?)",v1,v2);
        jdbc.update("insert into ocena_izlozbe(poseta_id,ocena,komentar,datum_ocene) values(?,4,'Jasna postavka','2035-01-01T04:00Z'),(?,2,'Više objašnjenja','2035-01-01T04:01Z')",v1,v2);
        String[] statuses={"KUPLJENA","AKTIVNA","ISKORISCENA","ISTEKLA","OTKAZANA"};
        long[] tickets={t1,t2,ticket(e,tr,"2035-01-01T01:00:00Z"),ticket(e,tr,"2035-01-01T01:00:00Z"),ticket(e,tr,"2035-01-01T01:00:00Z")};
        for(int i=0;i<5;i++)jdbc.update("update ulaznica set status=? where id=?",statuses[i],tickets[i]);
        jdbc.update("update transakcija set status='REFUNDIRANA' where ulaznica_id=?",tickets[4]);
        jdbc.update("update transakcija set nacin_placanja='KARTICA' where ulaznica_id=?",t2);
        jdbc.update("insert into transakcija(ulaznica_id,iznos,nacin_placanja,status,datum_transakcije) values(?,999,'DRUGO','NEUSPESNA','2035-01-01Z'),(?,999,'DRUGO','POKRENUTA','2035-01-01Z')",tickets[4],tickets[4]);
        long event=id("insert into dogadjaj(izlozba_id,naziv,tip,pocetak,kraj,kapacitet,status) values(?,'Analytics event','TURA','2035-01-01T10:00Z','2035-01-01T11:00Z',4,'PLANIRAN') returning id",e);
        for(String status:List.of("PRIJAVLJEN","PRISUSTVOVAO","OTKAZAN")){
            long a=id("insert into korisnicki_nalog(email,lozinka_hash,aktivan,datum_registracije) values(?,?,true,now()) returning id",UUID.randomUUID()+"@private.test",encoder.encode("Test-only-password"));
            long v=id("insert into posetilac(nalog_id,ime,prezime,saglasnost_za_preporuke) values(?,'Private','Identity',false) returning id",a);
            jdbc.update("insert into prijava_na_dogadjaj(posetilac_id,dogadjaj_id,datum_prijave,status) values(?,?,'2034-12-01Z',?)",v,event,status);
        }
        jdbc.update("insert into dogadjaj(izlozba_id,naziv,tip,pocetak,kraj,kapacitet,status) values(?,'Empty event','TURA','2035-01-01T10:00Z','2035-01-01T11:00Z',0,'OTKAZAN')",e);
        jdbc.update("insert into digitalni_sadrzaj(ucesce_id,tip_sadrzaja,putanja,jezik,verzija,generisan_automatski,datum_kreiranja) values(?,'TEKST','private/path','sr',1,false,now()),(?,'TEKST','private/path2','sr',2,false,now())",p1,p1);
        return new Fixture(e,tr,p1,p2,v1,t1);
    }
    private void scan(long visit,long p,String time,Integer duration){jdbc.update("insert into skeniranje_eksponata(poseta_id,ucesce_id,vreme_skeniranja,trajanje_pregleda) values(?,?,?,?)",visit,p,ts(time),duration);}
    @Test void onlyManagerMayReadAndDatesAreRequired() throws Exception {
        mvc.perform(get("/api/manager/dashboard").param("from",day.toString()).param("to",day.toString())).andExpect(status().isUnauthorized());
        for(String role:List.of("VISITOR","CASHIER","GUIDE","CURATOR","ADMINISTRATOR"))mvc.perform(get("/api/manager/dashboard").param("from",day.toString()).param("to",day.toString()).header("Authorization","Bearer "+tokenFor(role))).andExpect(status().isForbidden());
        String token=tokenFor("MANAGER");
        mvc.perform(get("/api/manager/dashboard").param("from",day.toString()).param("to",day.toString()).header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/manager/dashboard").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/manager/dashboard").param("from","2035-02-01").param("to","2035-01-01").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/manager/dashboard").param("from","invalid").param("to","2035-01-01").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
    }
    @Test @Transactional void attendanceRevenueAndTicketStatusesUseFactsNotCurrentPrices(){
        Fixture f=fixture();jdbc.update("update cenovnik_izlozbe set cena=99 where izlozba_id=?",f.exhibition());
        var r=service.dashboard(day,day);
        assertThat(r.attendance().total()).isEqualTo(2);assertThat(r.attendance().museums()).singleElement().satisfies(x->assertThat(x.count()).isEqualTo(2));
        assertThat(r.attendance().exhibitions()).singleElement().satisfies(x->assertThat(x.count()).isEqualTo(2));
        assertThat(r.attendance().terms()).singleElement().satisfies(x->assertThat(x.key()).isEqualTo(""+f.term()));
        assertThat(r.attendance().days()).singleElement().satisfies(x->assertThat(x.key()).isEqualTo("2035-01-01"));
        assertThat(r.revenue().total()).isEqualByComparingTo("40");assertThat(r.revenue().paidTicketValue()).isEqualByComparingTo("40");
        assertThat(r.revenue().categories()).singleElement().satisfies(x->assertThat(x.amount()).isEqualByComparingTo("40"));
        assertThat(r.revenue().methods()).hasSize(2);assertThat(r.revenue().methods().stream().filter(x->x.key().equals("GOTOVINA")).findFirst().orElseThrow().amount()).isEqualByComparingTo("30");
        assertThat(r.tickets()).hasSize(5).allSatisfy(x->assertThat(x.count()).isEqualTo(1));
    }
    @Test @Transactional void popularityEventsRatingsAndInventoryPreserveParticipationAndPrivacy() throws Exception {
        Fixture f=fixture();var r=service.dashboard(day,day);
        assertThat(r.popularity()).hasSize(2);var p=r.popularity().getFirst();assertThat(p.participationId()).isEqualTo(f.first());
        assertThat(p.scans()).isEqualTo(3);assertThat(p.visits()).isEqualTo(2);assertThat(p.averageSeconds()).isEqualTo(15);assertThat(p.measuredScans()).isEqualTo(2);
        assertThat(r.events()).hasSize(2);var event=r.events().stream().filter(e->e.capacity()==4).findFirst().orElseThrow();
        assertThat(event.registrations()).isEqualTo(3);assertThat(event.registered()).isEqualTo(1);assertThat(event.attended()).isEqualTo(1);assertThat(event.cancelled()).isEqualTo(1);assertThat(event.occupancyPercent()).isEqualTo(50);
        assertThat(r.events().stream().filter(e->e.capacity()==0).findFirst().orElseThrow().occupancyPercent()).isNull();
        assertThat(r.ratings()).singleElement().satisfies(x->{assertThat(x.average()).isEqualTo(3);assertThat(x.count()).isEqualTo(2);});
        assertThat(r.comments()).extracting(ManagerDtos.Comment::comment).containsExactly("Više objašnjenja","Jasna postavka");
        assertThat(r.contentInventory()).filteredOn(x->x.participationId()==f.first()).singleElement().satisfies(x->{assertThat(x.records()).isEqualTo(2);assertThat(x.language()).isEqualTo("sr");});
        assertThat(r.transitions()).singleElement().satisfies(x->{assertThat(x.key()).isEqualTo(f.first()+">"+f.second());assertThat(x.count()).isEqualTo(1);});
        assertThat(json.writeValueAsString(r)).doesNotContain("qrToken","qr_token","private/path","@private.test","Private","visitorId","externalReference");
    }
    @Test @Transactional void dateBoundariesUseLocalDaysAndInventoryIsNotUsage(){
        Fixture f=fixture();
        ticket(f.exhibition(),f.term(),"2034-12-31T22:59:59Z");ticket(f.exhibition(),f.term(),"2035-01-01T23:00:00Z");
        var r=service.dashboard(day,day);assertThat(r.revenue().total()).isEqualByComparingTo("40");
        var empty=service.dashboard(day.plusDays(5),day.plusDays(5));assertThat(empty.attendance().total()).isZero();assertThat(empty.revenue().total()).isEqualByComparingTo("0");
        assertThat(empty.tickets()).allSatisfy(x->assertThat(x.count()).isZero());assertThat(empty.events()).isEmpty();assertThat(empty.comments()).isEmpty();assertThat(empty.popularity()).isEmpty();assertThat(empty.contentInventory()).filteredOn(x->x.participationId()==f.first()).hasSize(1);
        assertThat(service.dashboard(LocalDate.of(2035,3,25),LocalDate.of(2035,3,25)).zone()).isEqualTo("Europe/Belgrade");
    }
    @Test @Transactional void ambiguousScanTimesAreNotOrderedByArtificialIds(){
        Fixture f=fixture();jdbc.update("update poseta set status='AKTIVNA',vreme_izlaska=null where id=?",f.visit());
        scan(f.visit(),f.second(),"2035-01-01T01:02:00Z",null);
        assertThat(service.dashboard(day,day).transitions()).isEmpty();
    }
}

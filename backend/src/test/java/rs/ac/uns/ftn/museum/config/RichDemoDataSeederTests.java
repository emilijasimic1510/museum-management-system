package rs.ac.uns.ftn.museum.config;

import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import rs.ac.uns.ftn.museum.support.PostgresTestBase;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles({"test", "local"})
class RichDemoDataSeederTests extends PostgresTestBase {
    @Autowired JdbcTemplate db;
    @Autowired RichDemoDataSeeder seed;
    @Autowired LocalImageSeeder imageSeed;
    @Autowired rs.ac.uns.ftn.museum.media.ImageStorage imageStorage;
    @org.junit.jupiter.api.io.TempDir static Path imageRoot;
    @org.springframework.test.context.DynamicPropertySource static void imageProperties(org.springframework.test.context.DynamicPropertyRegistry r){r.add("museum.image.storage-path",()->imageRoot.toString());}
    @Autowired DemoDataSeeder base;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactions;
    @Autowired rs.ac.uns.ftn.museum.manager.ManagerAnalyticsService manager;
    private static final String RICH_SHOW = "select distinct izlozba_id from ucesce_eksponata_na_izlozbi p join eksponat e on e.id=p.eksponat_id where e.inventarski_broj like 'DEMO-RICH-EX-%'";
    private int count(String sql) { return db.queryForObject(sql, Integer.class); }
    private void zero(String sql) { assertThat(count(sql)).as(sql).isZero(); }

    @Test void imageSeedIsIdempotentAndPreservesCuratorCover() {
        int before=count("select count(*) from digitalni_sadrzaj where tip_sadrzaja='SLIKA'");
        long id=db.queryForObject("select min(id) from eksponat where inventarski_broj like 'DEMO-RICH-EX-%'",Long.class);
        String key="exhibit-"+id+".png";
        byte[] original=imageStorage.read(key),edited=LocalImageSeeder.illustration(999);
        try{imageStorage.write(key,edited);imageSeed.run(null);imageSeed.run(null);
            assertThat(imageStorage.read(key)).isEqualTo(edited);
            assertThat(count("select count(*) from digitalni_sadrzaj where tip_sadrzaja='SLIKA'")).isEqualTo(before);
            assertThat(before).isGreaterThanOrEqualTo(96);
        }finally{imageStorage.write(key,original);}
    }

    @Test void collectionHasConnectedRealisticMinimumsAndUniqueTokens() {
        assertThat(count("select count(*) from muzej")).isGreaterThanOrEqualTo(4);
        assertThat(count("select count(*) from sala")).isGreaterThanOrEqualTo(12);
        assertThat(count("select count(*) from autor")).isGreaterThanOrEqualTo(28);
        assertThat(count("select count(*) from eksponat where inventarski_broj like 'DEMO-RICH-EX-%'")).isEqualTo(56);
        assertThat(count("select count(*) from izlozba where id in (" + RICH_SHOW + ")")).isEqualTo(12);
        assertThat(count("select count(*) from ucesce_eksponata_na_izlozbi where izlozba_id in (" + RICH_SHOW + ")")).isEqualTo(96);
        zero("select count(*) from (select qr_token from ucesce_eksponata_na_izlozbi group by qr_token having count(*)>1) q");
        zero("select count(*) from eksponat e where inventarski_broj like 'DEMO-RICH-EX-%' and (length(opis)<100 or not exists(select 1 from autorstvo a where a.eksponat_id=e.id))");
        assertThat(count("select count(*) from (select a.eksponat_id from autorstvo a join eksponat e on e.id=a.eksponat_id where e.inventarski_broj like 'DEMO-RICH-EX-%' group by a.eksponat_id having count(*)>1) q")).isEqualTo(8);
        zero("select count(*) from (select izlozba_id from ucesce_eksponata_na_izlozbi where izlozba_id in (" + RICH_SHOW + ") group by izlozba_id having count(*)<>8) q");
    }
    @Test void repeatedWorksHaveDifferentContextsPositionsOrderQrAndContent() {
        var pairs = db.queryForList("""
            select a.id_ucesca from ucesce_eksponata_na_izlozbi a join ucesce_eksponata_na_izlozbi b
             on a.eksponat_id=b.eksponat_id and a.izlozba_id<>b.izlozba_id and a.id_ucesca<b.id_ucesca
             join eksponat e on e.id=a.eksponat_id where e.inventarski_broj like 'DEMO-RICH-EX-%'
            """);
        assertThat(pairs.size()).isGreaterThanOrEqualTo(28);
        zero("""
            select count(*) from ucesce_eksponata_na_izlozbi a join ucesce_eksponata_na_izlozbi b
             on a.eksponat_id=b.eksponat_id and a.izlozba_id<>b.izlozba_id
             join eksponat e on e.id=a.eksponat_id join digitalni_sadrzaj da on da.ucesce_id=a.id_ucesca
             join digitalni_sadrzaj db on db.ucesce_id=b.id_ucesca
             where e.inventarski_broj like 'DEMO-RICH-EX-%' and
             (a.kontekstualni_opis=b.kontekstualni_opis or a.pozicija=b.pozicija or a.redosled_obilaska=b.redosled_obilaska or a.qr_token=b.qr_token or da.putanja=db.putanja)
            """);
    }
    @Test void textReferencesResolveToActualUtf8AssetsWithoutFakeAudio() throws Exception {
        var rows = db.queryForList("select d.* from digitalni_sadrzaj d join ucesce_eksponata_na_izlozbi p on p.id_ucesca=d.ucesce_id where d.tip_sadrzaja='TEKST' and p.izlozba_id in (" + RICH_SHOW + ")");
        assertThat(rows).hasSize(96);
        for (var row : rows) {
            assertThat(row.get("tip_sadrzaja")).isEqualTo("TEKST");
            assertThat(row.get("jezik")).isEqualTo("sr");
            assertThat(row.get("verzija")).isEqualTo(1);
            assertThat(row.get("generisan_automatski")).isEqualTo(false);
            Path path = Path.of("../frontend/public").resolve((String) row.get("putanja"));
            assertThat(Files.readString(path)).contains("Na izložbi", "Materijal:").doesNotContain("lorem ipsum", "test opis", "demo text");
        }
    }
    @Test void schedulesPricesAndCapacityAreValid() {
        zero("select count(*) from termin_izlozbe t join izlozba e on e.id=t.izlozba_id join sala s on s.id=e.sala_id where e.id in (" + RICH_SHOW + ") and (t.pocetak<e.datum_pocetka or t.kraj>e.datum_zavrsetka or t.kraj<=t.pocetak or t.kapacitet<=0 or t.kapacitet>s.kapacitet)");
        zero("select count(*) from izlozba a join izlozba b on a.sala_id=b.sala_id and a.id<b.id where a.status<>'OTKAZANA' and b.status<>'OTKAZANA' and a.datum_pocetka<b.datum_zavrsetka and b.datum_pocetka<a.datum_zavrsetka");
        zero("select count(*) from cenovnik_izlozbe a join cenovnik_izlozbe b on a.izlozba_id=b.izlozba_id and a.kategorija=b.kategorija and a.datum_vazenja_od<b.datum_vazenja_od where (a.datum_vazenja_do is null or a.datum_vazenja_do>b.datum_vazenja_od)");
        assertThat(count("select count(*) from termin_izlozbe where izlozba_id in (" + RICH_SHOW + ")")).isEqualTo(30);
        assertThat(count("select count(*) from cenovnik_izlozbe where izlozba_id in (" + RICH_SHOW + ")")).isEqualTo(96);
        zero("select count(*) from izlozba e where e.id in (" + RICH_SHOW + ") and e.status in ('AKTIVNA','PLANIRANA') and not exists(select 1 from termin_izlozbe t where t.izlozba_id=e.id and t.status='OTVOREN' and t.kraj>now())");
    }
    @Test void historicalLifecyclePaymentsRatingsAndRegistrationsAreConsistent() {
        assertThat(count("select count(*) from ulaznica where qr_token like 'DEMO-RICH-%'")).isEqualTo(97);
        assertThat(count("select count(*) from poseta p join ulaznica u on u.id=p.ulaznica_id where u.qr_token like 'DEMO-RICH-%'")).isEqualTo(58);
        zero("""
            select count(*) from poseta p join ulaznica u on u.id=p.ulaznica_id join termin_izlozbe t on t.id=u.termin_id
            where u.qr_token like 'DEMO-RICH-%' and (p.status<>'ZAVRSENA' or u.status<>'ISKORISCENA' or p.vreme_ulaska<t.pocetak
            or p.vreme_izlaska>t.kraj or p.vreme_izlaska<p.vreme_ulaska or u.datum_kupovine>p.vreme_ulaska
            or not exists(select 1 from transakcija x where x.ulaznica_id=u.id and x.status='USPESNA' and x.iznos=u.cena_placena))
            """);
        zero("""
            select count(*) from skeniranje_eksponata s join poseta p on p.id=s.poseta_id join ulaznica u on u.id=p.ulaznica_id
            join termin_izlozbe t on t.id=u.termin_id join ucesce_eksponata_na_izlozbi a on a.id_ucesca=s.ucesce_id
            where u.qr_token like 'DEMO-RICH-%' and (s.vreme_skeniranja<p.vreme_ulaska or s.vreme_skeniranja>p.vreme_izlaska
            or a.izlozba_id<>t.izlozba_id or s.vreme_skeniranja<a.datum_postavljanja or s.vreme_skeniranja>=a.datum_uklanjanja)
            """);
        zero("select count(*) from ocena_izlozbe o join poseta p on p.id=o.poseta_id where p.status<>'ZAVRSENA' or o.ocena not between 1 and 5 or o.datum_ocene<p.vreme_izlaska");
        zero("select count(*) from (select poseta_id from ocena_izlozbe group by poseta_id having count(*)>1) q");
        zero("select count(*) from dogadjaj d join izlozba e on e.id=d.izlozba_id where e.id in (" + RICH_SHOW + ") and (d.pocetak<e.datum_pocetka or d.kraj>e.datum_zavrsetka or d.kapacitet<(select count(*) from prijava_na_dogadjaj r where r.dogadjaj_id=d.id and r.status<>'OTKAZAN'))");
        zero("select count(*) from prijava_na_dogadjaj r join dogadjaj d on d.id=r.dogadjaj_id where d.izlozba_id in (" + RICH_SHOW + ") and (r.datum_prijave>d.pocetak or (r.status='PRISUSTVOVAO' and d.status<>'ZAVRSEN'))");
        System.out.println("RICH_COUNTS " + db.queryForMap("""
            select (select count(*) from muzej) museums,(select count(*) from sala) halls,
            (select count(*) from autor) authors,(select count(*) from eksponat) exhibits,
            (select count(*) from izlozba) exhibitions,(select count(*) from ucesce_eksponata_na_izlozbi) participations,
            (select count(*) from autorstvo) authorships,(select count(*) from termin_izlozbe) terms,
            (select count(*) from cenovnik_izlozbe) prices,(select count(*) from dogadjaj) events,
            (select count(*) from ulaznica) tickets,(select count(*) from transakcija) payments,
            (select count(*) from poseta) visits,(select count(*) from skeniranje_eksponata) scans,
            (select count(*) from ocena_izlozbe) ratings,(select count(*) from prijava_na_dogadjaj) registrations
            """));
    }
    @Test void secondSeedAndLaterRestartPreserveEveryRowIncludingLegacyAndPasswords() {
        var before = snapshot();
        base.seed(); seed.seed();
        assertThat(snapshot()).isEqualTo(before);
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            new RichDemoDataSeeder(db, Clock.fixed(Instant.now().plus(Duration.ofDays(40)), ZoneOffset.UTC), encoder).seed();
            assertThat(snapshot()).isEqualTo(before);
            tx.setRollbackOnly();
        });
    }
    @Test void managerDashboardShowsRevenuePopularityRatingsEventsAndTransitions() {
        LocalDate day = db.queryForObject("select datum_registracije from korisnicki_nalog where email='ana.ristic@museum.local'", java.sql.Timestamp.class)
                .toInstant().atZone(ZoneId.of("Europe/Belgrade")).toLocalDate().plusDays(120);
        var dashboard = manager.dashboard(day.minusDays(90), day.plusDays(16));
        assertThat(dashboard.attendance().total()).isGreaterThanOrEqualTo(58);
        assertThat(dashboard.revenue().total()).isPositive();
        assertThat(dashboard.revenue().total()).isEqualByComparingTo(dashboard.revenue().paidTicketValue());
        assertThat(dashboard.revenue().categories()).hasSize(4);
        assertThat(dashboard.revenue().methods()).hasSize(3);
        assertThat(dashboard.popularity().stream().mapToLong(p -> p.scans()).sum()).isGreaterThanOrEqualTo(288);
        assertThat(dashboard.ratings().stream().mapToLong(r -> r.count()).sum()).isGreaterThanOrEqualTo(47);
        assertThat(dashboard.events()).hasSizeGreaterThanOrEqualTo(20);
        assertThat(dashboard.transitions().stream().mapToLong(r -> r.count()).sum()).isGreaterThanOrEqualTo(230);
        System.out.println("RICH_MANAGER revenue=" + dashboard.revenue().total() + " from=" + dashboard.from() + " to=" + dashboard.to());
    }
    @Test void partiallyPopulatedCollectionIsRepairedAndCuratorEditsSurvive() {
        new TransactionTemplate(transactions).executeWithoutResult(tx -> {
            db.update("update ucesce_eksponata_na_izlozbi set kontekstualni_opis='Sačuvana kustoska dopuna.' where qr_token='DEMO-PHOTO-001'");
            db.update("delete from digitalni_sadrzaj where putanja='demo/rich/demo-photo-002.txt'");
            db.update("delete from autorstvo where eksponat_id=(select id from eksponat where inventarski_broj='DEMO-RICH-EX-002')");
            seed.seed();
            assertThat(db.queryForObject("select kontekstualni_opis from ucesce_eksponata_na_izlozbi where qr_token='DEMO-PHOTO-001'", String.class)).isEqualTo("Sačuvana kustoska dopuna.");
            assertThat(count("select count(*) from digitalni_sadrzaj where putanja='demo/rich/demo-photo-002.txt'")).isEqualTo(1);
            assertThat(count("select count(*) from autorstvo where eksponat_id=(select id from eksponat where inventarski_broj='DEMO-RICH-EX-002')")).isEqualTo(1);
            tx.setRollbackOnly();
        });
    }
    private Map<String, List<Map<String, Object>>> snapshot() {
        var rows = new LinkedHashMap<String, List<Map<String, Object>>>();
        for (String table : List.of("muzej", "sala", "autor", "eksponat", "autorstvo", "izlozba", "ucesce_eksponata_na_izlozbi", "digitalni_sadrzaj", "termin_izlozbe", "cenovnik_izlozbe", "dogadjaj", "korisnicki_nalog", "posetilac", "ulaznica", "transakcija", "poseta", "skeniranje_eksponata", "ocena_izlozbe", "prijava_na_dogadjaj"))
            rows.put(table, db.queryForList("select * from " + table + " order by 1,2"));
        return rows;
    }
}

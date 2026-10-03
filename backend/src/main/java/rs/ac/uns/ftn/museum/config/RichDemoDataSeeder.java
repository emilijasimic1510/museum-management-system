package rs.ac.uns.ftn.museum.config;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Additive local collection; no schema changes, graph writes, or resets of curator edits. */
@Component
@Profile("local & !production & !prod")
@Order(20)
public class RichDemoDataSeeder implements ApplicationRunner {
    private final JdbcTemplate db;
    private final Clock clock;
    private final PasswordEncoder encoder;
    private static final ZoneId ZONE = ZoneId.of("Europe/Belgrade");
    private static final String[] CATEGORIES = {"ODRASLI", "STUDENT", "PENZIONER", "DETE"};
    private static final String[] COMMENTS = {
        "Odličan izbor radova i jasan tok obilaska.", "Najviše su mi značila poređenja materijala.",
        "Volela bih još objašnjenja uz pojedine radove.", "Miran prostor, ostao sam duže nego što sam planirao.",
        "Zanimljiva tema, ali mi redosled nije svuda bio jasan.", "Vraćam se zbog drugačijeg čitanja istog dela.",
        "Deci su posebno prijale kratke priče uz postavke.", "Nisam uspeo da se povežem sa temom ove izložbe."
    };
    public RichDemoDataSeeder(JdbcTemplate db, Clock clock, PasswordEncoder encoder) {
        this.db = db; this.clock = clock; this.encoder = encoder;
    }
    @Override @Transactional public void run(ApplicationArguments args) { seed(); }

    @Transactional
    public void seed() {
        // Serializes concurrent local starts without requiring new tables or unique constraints.
        db.execute("select pg_advisory_xact_lock(20261003, 1)");
        // Registration date is not editable through the account API. It keeps the calendar
        // stable even when a curator reschedules an exhibition (no new seed-marker table).
        Instant registered = db.query("select datum_registracije from korisnicki_nalog where email=?",
                rs -> rs.next() ? rs.getTimestamp(1).toInstant() : null, "demo.history.01@museum.local");
        LocalDate day = registered == null ? LocalDate.now(clock.withZone(ZONE))
                : registered.atZone(ZONE).toLocalDate().plusDays(120);
        long[] halls = seedSpaces();
        long[] authors = seedAuthors();
        long[] works = seedWorks(authors);
        long[] visitors = seedVisitors(day);
        Long guide = id("select z.id from zaposleni z join korisnicki_nalog n on n.id=z.nalog_id join vodic v on v.zaposleni_id=z.id where n.email=?", DemoDataSeeder.STAFF_EMAIL);
        if (guide == null) throw new IllegalStateException("Base local seed must run before the rich collection");
        for (int i = 0; i < RichDemoCatalog.SHOWS.size(); i++) {
            String[] show = RichDemoCatalog.SHOWS.get(i);
            boolean past = i >= 9, future = i >= 6 && i < 9;
            Instant start = at(day.plusDays(past ? -90 : future ? 14 : -45), 0);
            Instant end = at(day.plusDays(past ? -49 : future ? 100 : 120), 0);
            long exhibition = ensure("select id from izlozba where naziv=?", new Object[]{show[0]},
                    "insert into izlozba(sala_id,naziv,opis,datum_pocetka,datum_zavrsetka,status,online_vidljiva) values(?,?,?,?,?,?,true) returning id",
                    halls[Integer.parseInt(show[4])], show[0], show[1] + ". " + show[6] + " " + show[7]
                            + " Izložba pripada izmišljenoj lokalnoj muzejskoj zbirci.", ts(start), ts(end), show[5]);
            // Existing exhibition dates win, including curator changes.
            Instant installed = db.queryForObject("select datum_pocetka from izlozba where id=?", Timestamp.class, exhibition).toInstant();
            long[] participations = new long[8];
            for (int j = 0; j < 8; j++) {
                String token = RichDemoCatalog.token(i, j);
                participations[j] = ensure("select id_ucesca from ucesce_eksponata_na_izlozbi where qr_token=?", new Object[]{token},
                        "insert into ucesce_eksponata_na_izlozbi(izlozba_id,eksponat_id,datum_postavljanja,datum_uklanjanja,pozicija,redosled_obilaska,kontekstualni_opis,qr_token) values(?,?,?,?,?,?,?,?) returning id_ucesca",
                        exhibition, works[RichDemoCatalog.workIndex(i, j)], ts(installed), past ? ts(end) : null,
                        show[0] + " · celina " + (j / 2 + 1) + " · mesto " + (j + 1), j + 1, RichDemoCatalog.context(i, j), token);
                if (id("select id from digitalni_sadrzaj where ucesce_id=? and tip_sadrzaja='TEKST' and jezik='sr' and verzija=1", participations[j]) == null)
                    db.update("insert into digitalni_sadrzaj(ucesce_id,tip_sadrzaja,putanja,jezik,verzija,generisan_automatski,datum_kreiranja) values(?,'TEKST',?,'sr',1,false,?)",
                            participations[j], RichDemoCatalog.path(i, j), ts(installed));
            }
            seedPrices(exhibition, day, i);
            if (future) {
                term(exhibition, at(day.plusDays(14), 10), at(day.plusDays(14), 18), 30 + i, "OTVOREN");
                term(exhibition, at(day.plusDays(16), 10), at(day.plusDays(16), 18), 30 + i, "OTVOREN");
            } else {
                LocalDate historyDay = day.minusDays(past ? 55 + (i - 9) * 8 : 2 + i * 3);
                Instant historyStart = at(historyDay, 10);
                // Start open so the unmodified DB visit/scan triggers validate the real lifecycle.
                long historicTerm = term(exhibition, historyStart, at(historyDay, 18), 35 + i, "OTVOREN");
                seedHistory(i, exhibition, historicTerm, historyStart, participations, visitors);
                db.update("update termin_izlozbe set status='ZAVRSEN' where id=? and status='OTVOREN'", historicTerm);
                if (past) term(exhibition, at(historyDay.plusDays(1), 10), at(historyDay.plusDays(1), 18), 35 + i, "ZAVRSEN");
                else {
                    // One extended opening-day demo slot supports immediate cashier validation at any hour.
                    term(exhibition, at(day, 0), at(day.plusDays(1), 0), 45 + i * 3, "OTVOREN");
                    long upcoming = term(exhibition, at(day.plusDays(2), 10), at(day.plusDays(2), 18), 40 + i, "OTVOREN");
                    for (int n = 0; n < 2; n++) seedTicket("DEMO-RICH-BOOKING-" + i + "-" + n, exhibition, upcoming,
                            visitors[(i + n) % visitors.length], at(day.minusDays(1), 12).plusSeconds(n * 90), "KUPLJENA", n, i);
                }
            }
            seedEvent(i, exhibition, guide, visitors, day, past, future, false);
            if (i < 4) seedEvent(i, exhibition, guide, visitors, day, false, false, true);
        }
    }

    private long[] seedSpaces() {
        List<String[]> museums = RichDemoCatalog.rows("""
            Demo Muzej savremenih tokova|1984|Ulica ateljea 18, Novi Sad|Izmišljeni muzej posvećen savremenoj umetnosti i susretu različitih materijala. Program povezuje skulpturu, slikarstvo i male prostorne eksperimente.
            Demo Galerija obalske fotografije|2006|Kej svetionika 7, Beograd|Izmišljena galerija za fotografiju i grafiku sa čitaonicom vizuelnih dnevnika. Njena zbirka prati obalu, gradske rubove i svakodnevne putanje.
            Demo Muzej forme i zanata|1972|Trg radionica 4, Novi Sad|Izmišljeni muzej skulpture i primenjene umetnosti. Zbirka približava posetiocima odnos ručnog rada, industrijskog oblikovanja i života predmeta.
            """);
        String[] names = {"Velika galerija", "Skulptorski salon", "Sala moderne umetnosti", "Grafički kabinet", "Galerija predmeta", "Mala galerija", "Portretni salon", "Atrijum", "Galerija fotografije"};
        long[] result = new long[9];
        for (int m = 0; m < 3; m++) {
            String[] row = museums.get(m);
            long museum = ensure("select id from muzej where naziv=?", new Object[]{row[0]},
                    "insert into muzej(naziv,godina_osnivanja,adresa,opis,aktivan) values(?,?,?,?,true) returning id", row[0], Integer.parseInt(row[1]), row[2], row[3]);
            for (int h = 0; h < 3; h++) {
                int k = m * 3 + h;
                result[k] = ensure("select id from sala where muzej_id=? and naziv=?", new Object[]{museum, names[k]},
                        "insert into sala(muzej_id,naziv,povrsina,sprat,kapacitet) values(?,?,?,?,?) returning id",
                        museum, names[k], new BigDecimal(145 + k * 23), h, 75 + k * 9);
            }
        }
        return result;
    }
    private long[] seedAuthors() {
        long[] result = new long[RichDemoCatalog.AUTHORS.size()];
        for (int i = 0; i < result.length; i++) {
            String[] a = RichDemoCatalog.AUTHORS.get(i);
            result[i] = ensure("select id from autor where ime=? and prezime=?", new Object[]{a[0], a[1]},
                    "insert into autor(ime,prezime,godina_rodjenja,biografija) values(?,?,?,?) returning id",
                    a[0], a[1], Integer.parseInt(a[2]), a[3] + " Izmišljena autorska biografija za lokalnu zbirku.");
        }
        return result;
    }
    private long[] seedWorks(long[] authors) {
        long[] result = new long[RichDemoCatalog.WORKS.size()];
        String[] origins = {"Novi Sad — zamišljeni atelje na obali", "Beograd — izmišljena radionička zbirka", "Zrenjanin — zamišljeni autorski legat", "Subotica — izmišljeni studijski ciklus"};
        for (int i = 0; i < result.length; i++) {
            String[] w = RichDemoCatalog.WORKS.get(i);
            result[i] = ensure("select id from eksponat where inventarski_broj=?", new Object[]{"DEMO-RICH-EX-%03d".formatted(i + 1)},
                    "insert into eksponat(naziv,godina_nastanka,tip,materijal,poreklo,opis,inventarski_broj) values(?,?,?,?,?,?,?) returning id",
                    w[0], String.valueOf(1998 + i % 27), w[1], w[2], origins[i % 4], w[3], "DEMO-RICH-EX-%03d".formatted(i + 1));
            db.update("insert into autorstvo(autor_id,eksponat_id,vrsta_doprinosa,napomena) values(?,?,?,?) on conflict do nothing",
                    authors[i / 7 * 3 + i % 3], result[i], i / 7 == 4 ? "Dizajn i prototip" : "Autor koncepta i realizacije", "Autorski rad u izmišljenoj zbirci.");
            if (i % 7 == 5) db.update("insert into autorstvo(autor_id,eksponat_id,vrsta_doprinosa,napomena) values(?,?,?,?) on conflict do nothing",
                    authors[24 + (i / 7) % 2], result[i], i % 2 == 0 ? "Konstrukcija i prostorna realizacija" : "Tekstilni i svetlosni elementi", "Kolektivna realizacija uz glavnog autora.");
        }
        return result;
    }
    private long[] seedVisitors(LocalDate day) {
        long[] result = new long[18];
        String[] first = {"Ana", "Petar", "Jovana", "Nikola", "Mina", "Stefan", "Lana", "Dušan", "Jelena", "Aleksa", "Tamara", "Milan", "Sara", "Ivan", "Nevena", "Lazar", "Milica", "Nenad"};
        String[] last = {"Ristić", "Babić", "Đurić", "Vasić", "Ilić", "Lazić"};
        String hash = null;
        for (int i = 0; i < result.length; i++) {
            String email = "demo.history.%02d@museum.local".formatted(i + 1);
            Long account = id("select id from korisnicki_nalog where email=?", email);
            if (account == null) {
                if (hash == null) hash = encoder.encode(UUID.randomUUID().toString());
                account = insert("insert into korisnicki_nalog(email,lozinka_hash,aktivan,datum_registracije) values(?,?,false,?) returning id", email, hash, ts(at(day.minusDays(120), 9)));
                db.update("insert into nalog_uloga(nalog_id,uloga_id,datum_dodele) select ?,id,? from uloga where naziv='VISITOR'", account, ts(at(day.minusDays(120), 9)));
            }
            result[i] = ensure("select id from posetilac where nalog_id=?", new Object[]{account},
                    "insert into posetilac(nalog_id,ime,prezime,saglasnost_za_preporuke) values(?,?,?,true) returning id", account, first[i], last[i % last.length]);
        }
        return result;
    }
    private void seedPrices(long exhibition, LocalDate day, int index) {
        for (int c = 0; c < CATEGORIES.length; c++) {
            int amount = switch (c) { case 0 -> 600 + index % 4 * 50; case 1 -> 400; case 2 -> 350; default -> 250; };
            Instant boundary = at(day.minusDays(10), 0);
            price(exhibition, CATEGORIES[c], at(day.minusDays(120), 0), boundary, amount - 50);
            price(exhibition, CATEGORIES[c], boundary, null, amount);
        }
    }
    private void price(long exhibition, String category, Instant start, Instant end, int amount) {
        // Preserve manual periods; do not insert an overlapping period even if its original key was changed.
        if (id("select izlozba_id from cenovnik_izlozbe where izlozba_id=? and kategorija=? and (datum_vazenja_do is null or datum_vazenja_do>?) and (cast(? as timestamptz) is null or datum_vazenja_od<cast(? as timestamptz))",
                exhibition, category, ts(start), end == null ? null : ts(end), end == null ? null : ts(end)) == null)
            db.update("insert into cenovnik_izlozbe(izlozba_id,datum_vazenja_od,kategorija,datum_vazenja_do,cena,napomena) values(?,?,?,?,?,?)",
                    exhibition, ts(start), category, end == null ? null : ts(end), BigDecimal.valueOf(amount), "Lokalna zbirka · cena izražena u RSD");
    }
    private long term(long exhibition, Instant start, Instant end, int capacity, String status) {
        return ensure("select id from termin_izlozbe where izlozba_id=? and pocetak=?", new Object[]{exhibition, ts(start)},
                "insert into termin_izlozbe(izlozba_id,pocetak,kraj,kapacitet,status) values(?,?,?,?,?) returning id", exhibition, ts(start), ts(end), capacity, status);
    }
    private long seedTicket(String token, long exhibition, long term, long visitor, Instant bought, String status, int category, int method) {
        Long existing = id("select id from ulaznica where qr_token=?", token);
        if (existing != null) return existing;
        var price = db.queryForMap("select datum_vazenja_od,cena from cenovnik_izlozbe where izlozba_id=? and kategorija=? and datum_vazenja_od<=? and (datum_vazenja_do is null or datum_vazenja_do>?)", exhibition, CATEGORIES[category % 4], ts(bought), ts(bought));
        Timestamp expiry = db.queryForObject("select kraj from termin_izlozbe where id=?", Timestamp.class, term);
        long ticket = insert("insert into ulaznica(posetilac_id,termin_id,cenovnik_izlozba_id,cenovnik_vazi_od,kategorija_cene,datum_kupovine,cena_placena,status,qr_token,vazi_do) values(?,?,?,?,?,?,?,?,?,?) returning id",
                visitor, term, exhibition, price.get("datum_vazenja_od"), CATEGORIES[category % 4], ts(bought), price.get("cena"), status, token, expiry);
        db.update("insert into transakcija(ulaznica_id,iznos,nacin_placanja,status,eksterna_referenca,datum_transakcije) values(?,?,?,?,?,?)", ticket, price.get("cena"), new String[]{"KARTICA", "GOTOVINA", "DRUGO"}[method % 3],
                "OTKAZANA".equals(status) ? "REFUNDIRANA" : "USPESNA", token + "-PAYMENT", ts(bought.plusSeconds(15)));
        return ticket;
    }
    private void seedHistory(int index, long exhibition, long term, Instant start, long[] parts, long[] visitors) {
        int visits = 5 + index % 4;
        int expired = 1 + index % 3;
        for (int n = 0; n < visits + expired + 1; n++) {
            String token = "DEMO-RICH-HISTORY-%02d-%02d".formatted(index + 1, n + 1);
            if (id("select id from ulaznica where qr_token=?", token) != null) continue;
            boolean used = n < visits;
            long ticket = seedTicket(token, exhibition, term, visitors[(index * 3 + n) % visitors.length],
                    start.minus(Duration.ofDays(1)).plusSeconds(n * 137), used ? "KUPLJENA" : n < visits + expired ? "ISTEKLA" : "OTKAZANA", n, index + n);
            if (!used) continue;
            Instant enter = start.plusSeconds(600 + n * 900);
            long visit = insert("insert into poseta(ulaznica_id,vreme_ulaska,status) values(?,?,'AKTIVNA') returning id", ticket, ts(enter));
            db.update("update ulaznica set status='AKTIVNA' where id=?", ticket);
            int scans = 3 + (n + index) % 5;
            for (int s = 0; s < scans; s++) db.update("insert into skeniranje_eksponata(poseta_id,ucesce_id,vreme_skeniranja,trajanje_pregleda) values(?,?,?,?)",
                    visit, parts[(s + n % 3) % 8], ts(enter.plusSeconds(90 + s * 240)), (n + s) % 6 == 0 ? null : 35 + (n * 19 + s * 31 + index * 7) % 170);
            Instant exit = enter.plusSeconds(scans * 240L + 180);
            db.update("update poseta set status='ZAVRSENA',vreme_izlaska=? where id=?", ts(exit), visit);
            db.update("update ulaznica set status='ISKORISCENA' where id=?", ticket);
            if (n % 4 != 3) db.update("insert into ocena_izlozbe(poseta_id,ocena,komentar,datum_ocene) values(?,?,?,?) on conflict(poseta_id) do nothing",
                    visit, new int[]{5, 4, 3, 5, 2, 5, 4, 1}[(index + n) % 8], COMMENTS[(index + n) % COMMENTS.length], ts(exit.plusSeconds(420)));
        }
    }
    private void seedEvent(int index, long exhibition, long guide, long[] visitors, LocalDate day, boolean past, boolean future, boolean extra) {
        String[] show = RichDemoCatalog.SHOWS.get(index);
        String[] types = {"TURA", "PREDAVANJE", "RADIONICA"};
        String label = extra ? "Porodična radionica" : new String[]{"Vođeno čitanje", "Razgovor sa kustosom", "Radionica posmatranja"}[index % 3];
        String name = label + " · " + show[0];
        LocalDate eventDay = day.plusDays(past ? -55 - (index - 9) * 8 : future ? 14 + index - 6 : extra ? -2 - index * 3 : 3 + index);
        Instant start = at(eventDay, 12 + index % 3);
        boolean finished = past || extra;
        long event = ensure("select id from dogadjaj where naziv=?", new Object[]{name},
                "insert into dogadjaj(izlozba_id,vodic_id,naziv,tip,pocetak,kraj,kapacitet,jezik,status) values(?,?,?,?,?,?,?,?,?) returning id",
                exhibition, guide, name, extra ? "RADIONICA" : types[index % 3], ts(start), ts(start.plusSeconds(3600 + index % 2 * 1800)), 18 + index * 2, index == 7 ? "en" : "sr", finished ? "ZAVRSEN" : "PLANIRAN");
        for (int n = 0; n < 3 + index % 6; n++) db.update("insert into prijava_na_dogadjaj(posetilac_id,dogadjaj_id,datum_prijave,status) values(?,?,?,?) on conflict do nothing",
                visitors[(index * 2 + n) % visitors.length], event, ts(finished ? start.minus(Duration.ofDays(2)).plusSeconds(n * 180) : at(day.minusDays(1), 9).plusSeconds(n * 180)), n == 2 ? "OTKAZAN" : finished ? "PRISUSTVOVAO" : "PRIJAVLJEN");
    }
    private long ensure(String lookup, Object[] key, String insert, Object... args) {
        Long existing = id(lookup, key);
        return existing == null ? insert(insert, args) : existing;
    }
    private long insert(String sql, Object... args) { return Objects.requireNonNull(db.queryForObject(sql, Long.class, args)); }
    private Long id(String sql, Object... args) { return db.query(sql, rs -> rs.next() ? rs.getLong(1) : null, args); }
    private static Instant at(LocalDate day, int hour) { return day.atTime(hour, 0).atZone(ZONE).toInstant(); }
    private static Timestamp ts(Instant time) { return Timestamp.from(time); }
}

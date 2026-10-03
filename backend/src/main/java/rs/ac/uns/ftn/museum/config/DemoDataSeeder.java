package rs.ac.uns.ftn.museum.config;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.*;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Idempotent local-only bootstrap data. It never runs for test, staging or production profiles. */
@Component
@org.springframework.core.annotation.Order(10)
@Profile({"local", "development"})
public class DemoDataSeeder implements ApplicationRunner {
    public static final String VISITOR_EMAIL = "demo.visitor@museum.local";
    public static final String STAFF_EMAIL = "demo.staff@museum.local";
    public static final String GUIDE_EMAIL = "demo.guide@museum.local";
    public static final String PASSWORD = "MuseumDemo!2026";
    private static final String MUSEUM = "Demo Museum Novi Sad";
    private static final String EXHIBITION = "Tragovi svetlosti";
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    @org.springframework.beans.factory.annotation.Autowired private org.springframework.core.env.Environment environment;
    public DemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder encoder) { this.jdbc = jdbc; this.encoder = encoder; }

    @Override @Transactional public void run(ApplicationArguments args) { seed(); }

    @Transactional
    public void seed() {
        Long museum = id("select id from muzej where naziv=?", MUSEUM);
        if (museum == null) {
            jdbc.update("insert into muzej(naziv,godina_osnivanja,adresa,opis,aktivan) values(?,?,?,?,true)", MUSEUM, 2026, "Bulevar muzeja 1, Novi Sad", "Bezbedna razvojna zbirka za demonstraciju muzejske aplikacije.");
            museum = id("select id from muzej where naziv=?", MUSEUM);
        }
        Long hallA = ensureHall(museum, "Galerija svetlosti", 1, 120);
        Long hallB = ensureHall(museum, "Galerija susreta", 2, 80);
        Instant now = Instant.now();
        Long exhibition = id("select id from izlozba where naziv=?", EXHIBITION);
        if (exhibition == null) {
            jdbc.update("insert into izlozba(sala_id,naziv,opis,datum_pocetka,datum_zavrsetka,status,online_vidljiva) values(?,?,?,?,?,'AKTIVNA',true)", hallA, EXHIBITION, "Tri eksponata, jedan autor i put kroz boju, materijal i sećanje.", ts(now.minus(Duration.ofDays(3))), ts(now.plus(Duration.ofDays(180))));
            exhibition = id("select id from izlozba where naziv=?", EXHIBITION);
        }
        // Stable calendar: restarting on another day must not append another pair of slots.
        Instant demoDay = jdbc.queryForObject("select datum_pocetka from izlozba where id=?", Timestamp.class, exhibition)
                .toInstant().plus(Duration.ofDays(5)).truncatedTo(java.time.temporal.ChronoUnit.DAYS);
        ensureTerm(exhibition, demoDay.plus(Duration.ofHours(10)), 40);
        ensureTerm(exhibition, demoDay.plus(Duration.ofHours(15)), 40);
        // Create the initial live slot once; later scheduling belongs to the collection workspace.
        ensureLiveDemoTerm(exhibition, now, 40);
        ensurePrice(exhibition, "ODRASLI", new BigDecimal("8.00"));
        ensurePrice(exhibition, "STUDENT", new BigDecimal("5.00"));
        Long eveningExhibition = ensureExhibition(hallB, "Veče skulpture", "Skulpture i materijali u dijalogu.", now);
        ensureLiveDemoTerm(eveningExhibition, now, 30);
        ensurePrice(eveningExhibition, "ODRASLI", new BigDecimal("6.00"));
        ensurePrice(eveningExhibition, "STUDENT", new BigDecimal("4.00"));
        Long author = ensureAuthor("Mila", "Jovanović");
        Long secondAuthor = ensureAuthor("Luka", "Petrović");
        Long exhibit1 = ensureExhibit("Tišina u plavom", "2024", "SLIKA", "Ulje na platnu", "Srbija", "Dnevnik svetlosti.", "DEMO-EX-001");
        Long exhibit2 = ensureExhibit("Dodir prostora", "2023", "SKULPTURA", "Drvo i metal", "Vojvodina", "Materijal kao sećanje.", "DEMO-EX-002");
        Long exhibit3 = ensureExhibit("Pre nego što svane", "2025", "FOTOGRAFIJA", "Analogna fotografija", "Novi Sad", "Grad između dva trenutka.", "DEMO-EX-003");
        ensureAuthorship(author, exhibit1); ensureAuthorship(author, exhibit2); ensureAuthorship(author, exhibit3);
        ensureParticipation(exhibition, exhibit1, 1, "DEMO-QR-001", "Prvi korak kroz plavu tišinu.");
        ensureParticipation(exhibition, exhibit2, 2, "DEMO-QR-002", "Materijal i trag ruke.");
        ensureParticipation(exhibition, exhibit3, 3, "DEMO-QR-003", "Pogled ka gradu koji se budi.");
        ensureContent("DEMO-QR-001", "TEKST", "demo/tišina-u-plavom.txt");
        ensureContent("DEMO-QR-002", "TEKST", "demo/dodir-prostora.txt");
        // Replace only the original missing demo audio placeholder; preserve all other content.
        jdbc.update("update digitalni_sadrzaj d set tip_sadrzaja='TEKST',putanja='demo/pre-nego-sto-svane.txt' from ucesce_eksponata_na_izlozbi p where p.id_ucesca=d.ucesce_id and p.qr_token=? and d.tip_sadrzaja='AUDIO' and d.putanja='demo/pre-nego-sto-svane.mp3'", "DEMO-QR-003");
        ensureContent("DEMO-QR-003", "TEKST", "demo/pre-nego-sto-svane.txt");
        Long sculpture1=ensureExhibit("Krug u bronzi", "2023", "SKULPTURA", "Bronza", "Srbija", "Odnos pune forme i praznog prostora.", "DEMO-SC-001");
        Long sculpture2=ensureExhibit("Ritam kamena", "2024", "SKULPTURA", "Kamen", "Srbija", "Ponavljanje oblika i promena perspektive.", "DEMO-SC-002");
        Long sculpture3=ensureExhibit("Linija u pokretu", "2025", "SKULPTURA", "Metal", "Srbija", "Svetlost i senka u pokretu posetioca.", "DEMO-SC-003");
        ensureAuthorship(author,sculpture1);ensureAuthorship(author,sculpture2);ensureAuthorship(author,sculpture3);
        ensureAuthorship(secondAuthor,sculpture1);ensureAuthorship(secondAuthor,sculpture2);
        ensureParticipation(eveningExhibition,sculpture1,1,"DEMO-SCULPTURE-QR-001","Počnite obilazak odnosom bronzane forme i praznine.");
        ensureParticipation(eveningExhibition,sculpture2,2,"DEMO-SCULPTURE-QR-002","Uporedite teksturu kamena sa prethodnom bronzanom formom.");
        ensureParticipation(eveningExhibition,sculpture3,3,"DEMO-SCULPTURE-QR-003","Završite obilazak posmatranjem senke iz različitih uglova.");
        ensureContent("DEMO-SCULPTURE-QR-001","TEKST","demo/krug-u-bronzi.txt");
        ensureContent("DEMO-SCULPTURE-QR-002","TEKST","demo/ritam-kamena.txt");
        ensureContent("DEMO-SCULPTURE-QR-003","TEKST","demo/linija-u-pokretu.txt");
        Long photoHall=ensureHall(museum,"Galerija gradskih priča",3,100);
        Long photoExhibition=ensureExhibition(photoHall,"Grad kroz objektiv","Tri pogleda na grad: jutro, reka i noć.",now);
        ensureLiveDemoTerm(photoExhibition,now,60);
        ensurePrice(photoExhibition,"ODRASLI",new BigDecimal("7.00"));
        ensurePrice(photoExhibition,"STUDENT",new BigDecimal("4.00"));
        String[] photoNames={"Jutro na trgu","Odraz na Dunavu","Noćna svetla"};
        String[] photoContexts={"Počnite obilazak svetlom jutra na gradskom trgu.","Uporedite arhitekturu grada sa njenim odrazom na reci.","Završite obilazak kontrastima svetla i senke posle zalaska sunca."};
        String[] installationContexts={
            "U izložbi Grad kroz objektiv, trg posmatramo kao zajednički prostor svakodnevnih susreta. Jutarnji prolaznici i praznine između njih otkrivaju ritam grada pre početka radnog dana. Obratite pažnju na odnos ljudskih figura i pročelja zgrada.",
            "U ovoj celini izložbe Grad kroz objektiv, Dunav postaje druga slika grada. Obrisi obale gube čvrstinu na površini vode i podsećaju nas da se doživljaj mesta menja sa stanovištem posmatrača. Uporedite stabilne vertikale zgrada sa njihovim razlomljenim odrazima.",
            "Završna celina izložbe Grad kroz objektiv prati grad nakon povlačenja dnevne svetlosti. Izlozi i ulične svetiljke određuju nove putanje pogleda i drugačije granice javnog prostora. Potražite delove prizora koji ostaju izvan osvetljenih površina."};
        for(int index=0;index<photoNames.length;index++) {
            String suffix=String.format("%03d",index+1);
            String qr="DEMO-CITY-QR-"+suffix;
            Long photo=ensureExhibit(photoNames[index],"2026","FOTOGRAFIJA","Digitalna fotografija","Novi Sad",photoContexts[index],"DEMO-CITY-EX-"+suffix);
            ensureAuthorship(author,photo);
            ensureParticipation(photoExhibition,photo,index+1,qr,photoContexts[index]);
            upgradeDemoContext(qr,photoContexts[index],installationContexts[index]);
            ensureContent(qr,"TEKST","demo/grad-"+suffix+".txt");
            if(index==0) {
                ensureParticipation(exhibition,photo,4,"DEMO-LIGHT-CITY-QR-001",
                    "U izložbi Tragovi svetlosti, ista fotografija trga čita se kao studija prelaska iz senke u svetlo. Kosa jutarnja svetlost razdvaja površine fasada i usmerava pogled ka sitnim detaljima. Pored slike Tišina u plavom, fotografija pokazuje kako različiti mediji beleže kratkotrajno svetlosno stanje.");
                ensureContent("DEMO-LIGHT-CITY-QR-001","TEKST","demo/jutro-tragovi-svetlosti.txt");
            }
        }
        Long visitorAccount = ensureAccount(VISITOR_EMAIL, "Demo", "Posetilac", "0600000001", true, "VISITOR", null);
        if (environment.matchesProfiles("local")) {
            ensureAccount("demo.admin@museum.local", "Demo", "Administrator", "0600000006", false, "ADMINISTRATOR", museum);
            Long administrator=id("select z.id from zaposleni z join korisnicki_nalog a on a.id=z.nalog_id where a.email=?", "demo.admin@museum.local");
            jdbc.update("update zaposleni set tip_zaposlenog='ADMINISTRATOR' where id=?",administrator);
            jdbc.update("insert into administrator(zaposleni_id,nivo_administracije) values(?,'Nalozi, zaposleni i muzejski program') on conflict do nothing",administrator);
            ensureAccount("demo.manager@museum.local", "Demo", "Menadžer", "0600000005", false, "MANAGER", museum);
            Long manager=id("select z.id from zaposleni z join korisnicki_nalog a on a.id=z.nalog_id where a.email=?", "demo.manager@museum.local");
            jdbc.update("update zaposleni set tip_zaposlenog='MANAGER' where id=?",manager);
            jdbc.update("insert into menadzer(zaposleni_id,nivo_izvestavanja) values(?,'Svi muzeji — zbirni operativni izveštaji') on conflict do nothing",manager);
            ensureAccount("demo.curator@museum.local", "Demo", "Kustos", "0600000003", false, "CURATOR", museum);
            Long curator=id("select z.id from zaposleni z join korisnicki_nalog a on a.id=z.nalog_id where a.email=?", "demo.curator@museum.local");
            jdbc.update("update zaposleni set tip_zaposlenog='CURATOR' where id=?",curator);
            jdbc.update("insert into kustos(zaposleni_id) values(?) on conflict do nothing",curator);
            ensureAccount(GUIDE_EMAIL, "Demo", "Vodič", "0600000004", false, "GUIDE", museum);
            Long ownGuide=id("select z.id from zaposleni z join korisnicki_nalog a on a.id=z.nalog_id where a.email=?",GUIDE_EMAIL);
            jdbc.update("update zaposleni set tip_zaposlenog='GUIDE' where id=?",ownGuide);
            jdbc.update("insert into vodic(zaposleni_id,primarni_jezik) values(?,'sr') on conflict do nothing",ownGuide);
            ensureGuideEvent(exhibition,ownGuide,"Demo razgovor o svetlosti","PREDAVANJE",now.plus(Duration.ofDays(4)),20,"PLANIRAN",null);
            Long demoVisitor=id("select id from posetilac where nalog_id=?",visitorAccount);
            ensureGuideEvent(exhibition,ownGuide,"Demo radionica boje","RADIONICA",now.plus(Duration.ofDays(5)),2,"PLANIRAN",demoVisitor);
            ensureGuideEvent(exhibition,ownGuide,"Demo otkazana tura","TURA",now.plus(Duration.ofDays(6)),10,"OTKAZAN",null);
        }
        Long staffAccount = ensureAccount(STAFF_EMAIL, "Demo", "Zaposleni", "0600000002", false, "CASHIER", museum);
        ensureRole(staffAccount, "GUIDE");
        Long cashier = id("select z.id from zaposleni z join korisnicki_nalog a on a.id=z.nalog_id where a.email=?", STAFF_EMAIL);
        Long guide = cashier;
        ensureSubtype(cashier, "CASHIER");
        // The same development employee is also a guide so the event and cashier flows are immediately testable.
        jdbc.update("update zaposleni set tip_zaposlenog='GUIDE' where id=?", guide);
        jdbc.update("delete from blagajnik where zaposleni_id=?", guide);
        ensureSubtype(guide, "GUIDE");
        Instant eventStart = now.plus(Duration.ofDays(3));
        ensureEvent(exhibition, guide, eventStart, eventStart.plus(Duration.ofHours(2)));
    }

    private Long ensureHall(Long museum, String name, int floor, int capacity) {
        Long id=id("select id from sala where muzej_id=? and naziv=?", museum, name); if(id!=null)return id;
        jdbc.update("insert into sala(muzej_id,naziv,povrsina,sprat,kapacitet) values(?,?,?,?,?)",museum,name,250.0,floor,capacity); return id("select id from sala where muzej_id=? and naziv=?",museum,name);
    }
    private void ensureTerm(Long exhibition, Instant start, int capacity) { if(id("select id from termin_izlozbe where izlozba_id=? and pocetak=?",exhibition,ts(start))==null) jdbc.update("insert into termin_izlozbe(izlozba_id,pocetak,kraj,kapacitet,status) values(?,?,?,?,'OTVOREN')",exhibition,ts(start),ts(start.plus(Duration.ofHours(2))),capacity); }
    private void ensureLiveDemoTerm(Long exhibition, Instant now, int capacity) {
        Long active=id("select id from termin_izlozbe where izlozba_id=? and pocetak<=?",exhibition,ts(now));
        if(active==null) jdbc.update("insert into termin_izlozbe(izlozba_id,pocetak,kraj,kapacitet,status) values(?,?,?,?,'OTVOREN')",exhibition,ts(now.minus(Duration.ofMinutes(5))),ts(now.plus(Duration.ofHours(2))),capacity);
    }
    private Long ensureExhibition(Long hall, String name, String description, Instant now) {
        Long existing=id("select id from izlozba where naziv=?",name);
        if(existing!=null)return existing;
        jdbc.update("insert into izlozba(sala_id,naziv,opis,datum_pocetka,datum_zavrsetka,status,online_vidljiva) values(?,?,?,?,?,'AKTIVNA',true)",hall,name,description,ts(now.minus(Duration.ofDays(1))),ts(now.plus(Duration.ofDays(180))));
        return id("select id from izlozba where naziv=?",name);
    }
    private void ensurePrice(Long exhibition,String category,BigDecimal price) { if(id("select izlozba_id from cenovnik_izlozbe where izlozba_id=? and kategorija=?",exhibition,category)==null) jdbc.update("insert into cenovnik_izlozbe(izlozba_id,datum_vazenja_od,kategorija,datum_vazenja_do,cena,napomena) values(?,?,?,null,?,?)",exhibition,ts(Instant.now().minusSeconds(60)),category,price,"Demo cenovnik"); }
    private Long ensureAuthor(String first,String last) { Long x=id("select id from autor where ime=? and prezime=?",first,last);if(x!=null)return x;jdbc.update("insert into autor(ime,prezime,godina_rodjenja,biografija) values(?,?,?,?)",first,last,1988,"Demo autor lokalne zbirke.");return id("select id from autor where ime=? and prezime=?",first,last); }
    private Long ensureExhibit(String name,String year,String type,String material,String origin,String description,String inventory) { Long x=id("select id from eksponat where inventarski_broj=?",inventory);if(x!=null)return x;jdbc.update("insert into eksponat(naziv,godina_nastanka,tip,materijal,poreklo,opis,inventarski_broj) values(?,?,?,?,?,?,?)",name,year,type,material,origin,description,inventory);return id("select id from eksponat where inventarski_broj=?",inventory); }
    private void ensureAuthorship(Long author,Long exhibit) { jdbc.update("insert into autorstvo(autor_id,eksponat_id,vrsta_doprinosa) values(?,?,?) on conflict do nothing",author,exhibit,"Autor"); }
    private void upgradeDemoContext(String qr,String original,String replacement) {
        // Upgrade only the known seed text; preserve curator edits and the participation identity.
        jdbc.update("update ucesce_eksponata_na_izlozbi set kontekstualni_opis=? where qr_token=? and kontekstualni_opis=?",replacement,qr,original);
    }
    private void ensureParticipation(Long exhibition,Long exhibit,int order,String qr,String context) { Long x=id("select id_ucesca from ucesce_eksponata_na_izlozbi where qr_token=?",qr);if(x!=null)return;jdbc.update("insert into ucesce_eksponata_na_izlozbi(izlozba_id,eksponat_id,datum_postavljanja,pozicija,redosled_obilaska,kontekstualni_opis,qr_token,qr_putanja) values(?,?,now(),?,?,?,?,?)",exhibition,exhibit,"Sala A"+order,order,context,qr,"/demo/qr/"+qr); }
    private void ensureContent(String qr,String type,String path) { if(id("select d.id from digitalni_sadrzaj d join ucesce_eksponata_na_izlozbi p on p.id_ucesca=d.ucesce_id where p.qr_token=?",qr)==null)jdbc.update("insert into digitalni_sadrzaj(ucesce_id,tip_sadrzaja,putanja,jezik,verzija,generisan_automatski,datum_kreiranja) select id_ucesca,?,?,?,1,false,now() from ucesce_eksponata_na_izlozbi where qr_token=?","TEKST".equals(type)?"TEKST":type,path,"sr",qr); }
    private Long ensureAccount(String email,String first,String last,String phone,boolean consent,String role,Long museum) { Long x=id("select id from korisnicki_nalog where email=?",email);if(x==null){jdbc.update("insert into korisnicki_nalog(email,lozinka_hash,aktivan,datum_registracije) values(?,?,true,now())",email,encoder.encode(PASSWORD));x=id("select id from korisnicki_nalog where email=?",email);jdbc.update("insert into nalog_uloga(nalog_id,uloga_id,datum_dodele) select ?,id,now() from uloga where naziv=?",x,role);if("VISITOR".equals(role))jdbc.update("insert into posetilac(nalog_id,ime,prezime,telefon,saglasnost_za_preporuke) values(?,?,?,?,?)",x,first,last,phone,consent);else {jdbc.update("insert into zaposleni(nalog_id,muzej_id,ime,prezime,telefon,datum_zaposlenja,tip_zaposlenog) values(?,?,?,?,?,current_date,'CASHIER')",x,museum,first,last,phone);}}return x; }
    private void ensureRole(Long account,String role){jdbc.update("insert into nalog_uloga(nalog_id,uloga_id,datum_dodele) select ?,id,now() from uloga where naziv=? and not exists(select 1 from nalog_uloga ar join uloga r on r.id=ar.uloga_id where ar.nalog_id=? and r.naziv=?)",account,role,account,role);}
    private void ensureSubtype(Long employee, String type) {
        String table;
        switch (type) {
            case "CASHIER": table = "blagajnik"; break;
            case "GUIDE": table = "vodic"; break;
            default: throw new IllegalArgumentException("Unsupported demo subtype");
        }
        jdbc.update("insert into " + table + "(zaposleni_id) values(?) on conflict do nothing", employee);
    }
    private void ensureEvent(Long exhibition,Long guide,Instant start,Instant end){if(id("select id from dogadjaj where naziv=?","Demo vođena tura") == null)jdbc.update("insert into dogadjaj(izlozba_id,vodic_id,naziv,tip,pocetak,kraj,kapacitet,jezik,status) values(?,?,?,'TURA',?,?,20,'sr','PLANIRAN')",exhibition,guide,"Demo vođena tura",ts(start),ts(end));}
    private void ensureGuideEvent(Long exhibition,Long guide,String name,String type,Instant start,int capacity,String status,Long visitor) {
        // Seed each named event once. Restarts never reset dates, statuses or registrations edited by users.
        if(id("select id from dogadjaj where naziv=?",name)!=null)return;
        Long event=jdbc.queryForObject("insert into dogadjaj(izlozba_id,vodic_id,naziv,tip,pocetak,kraj,kapacitet,jezik,status) values(?,?,?,?,?,?,?,'sr',?) returning id",Long.class,exhibition,guide,name,type,ts(start),ts(start.plus(Duration.ofHours(1))),capacity,status);
        if(visitor!=null)jdbc.update("insert into prijava_na_dogadjaj(posetilac_id,dogadjaj_id,datum_prijave,status) values(?,?,now(),'PRIJAVLJEN') on conflict do nothing",visitor,event);
    }
    private Timestamp ts(Instant value) { return Timestamp.from(value); }
    private Long id(String sql,Object... args){return jdbc.query(sql,rs->rs.next()?rs.getLong(1):null,args);}
}

package rs.ac.uns.ftn.museum.config;

import org.springframework.jdbc.core.JdbcTemplate;

/** Updates known local bootstrap labels in place; identities, credentials and history survive. */
final class LocalContentUpgrade {
    private LocalContentUpgrade() {}
    private static final String[][] ACCOUNTS = {
        {"visitor","ana.petrovic","Ana","Petrović"},
        {"staff","milica.jovanovic","Milica","Jovanović"},
        {"curator","nikola.markovic","Nikola","Marković"},
        {"guide","jelena.stojanovic","Jelena","Stojanović"},
        {"manager","ivan.nikolic","Ivan","Nikolić"},
        {"admin","marija.ilic","Marija","Ilić"}
    };
    private static final String[] HISTORY = {"ana.ristic","petar.babic","jovana.djuric","nikola.vasic","mina.ilic","stefan.lazic","lana.ristic","dusan.babic","jelena.djuric","aleksa.vasic","tamara.ilic","milan.lazic","sara.ristic","ivan.babic","nevena.djuric","lazar.vasic","milica.ilic","nenad.lazic"};
    static String historyEmail(int index) { return HISTORY[index]+"@museum.local"; }
    static void apply(JdbcTemplate db) {
        db.execute("select pg_advisory_xact_lock(20261003, 1)");
        for (String[] a : ACCOUNTS) {
            String old="demo."+a[0]+"@museum.local", email=a[1]+"@museum.local";
            db.update("update korisnicki_nalog set email=? where email=? and not exists(select 1 from korisnicki_nalog where email=?)",email,old,email);
            db.update("update korisnicki_nalog set email=? || '.arhiva.' || id || '@museum.local' where email=?",a[1],old);
            db.update("update posetilac set ime=?,prezime=? where nalog_id in(select id from korisnicki_nalog where email=?) and ime='Demo'",a[2],a[3],email);
            db.update("update zaposleni set ime=?,prezime=? where nalog_id in(select id from korisnicki_nalog where email=?) and ime='Demo'",a[2],a[3],email);
        }
        for(int i=0;i<HISTORY.length;i++) db.update("update korisnicki_nalog set email=? where email=? and not exists(select 1 from korisnicki_nalog where email=?)",historyEmail(i),"demo.history.%02d@museum.local".formatted(i+1),historyEmail(i));
        rename(db,"muzej","Demo Museum Novi Sad","Muzej gradskih priča");
        for(String name:new String[]{"Muzej savremenih tokova","Galerija obalske fotografije","Muzej forme i zanata"}) rename(db,"muzej","Demo "+name,name);
        rename(db,"dogadjaj","Demo razgovor o svetlosti","Razgovor o svetlosti");
        rename(db,"dogadjaj","Demo radionica boje","Radionica boje i pigmenta");
        rename(db,"dogadjaj","Demo otkazana tura","Šetnja kroz skulpturu");
        rename(db,"dogadjaj","Demo vođena tura","Tragovi svetlosti — vođeni obilazak");
        db.update("update muzej set opis=? where naziv='Muzej gradskih priča' and opis=?","Zbirka umetnosti i vizuelne kulture posvećena gradskom životu, svetlosti i sećanju.","Bezbedna razvojna zbirka za demonstraciju muzejske aplikacije.");
        db.update("update cenovnik_izlozbe set napomena='Redovni cenovnik' where napomena='Demo cenovnik'");
        db.update("update autor set biografija=? where biografija='Demo autor lokalne zbirke.'","Autor posvećen odnosu svetlosti, materijala i svakodnevnog života.");
        db.update("update izlozba set opis=replace(opis,' Izložba pripada izmišljenoj lokalnoj muzejskoj zbirci.','') where opis like '% Izložba pripada izmišljenoj lokalnoj muzejskoj zbirci.'");
        db.update("update autor set biografija=replace(biografija,' Izmišljena autorska biografija za lokalnu zbirku.','') where biografija like '% Izmišljena autorska biografija za lokalnu zbirku.'");
        db.update("update muzej set opis=replace(replace(opis,'Izmišljeni muzej','Muzej'),'Izmišljena galerija','Galerija') where naziv in ('Muzej savremenih tokova','Galerija obalske fotografije','Muzej forme i zanata')");
    }
    private static void rename(JdbcTemplate db,String table,String old,String replacement) {
        // Table names are private constants above, never request data.
        db.update("update "+table+" set naziv=? where naziv=? and not exists(select 1 from "+table+" where naziv=?)",replacement,old,replacement);
    }
}

package rs.ac.uns.ftn.museum.manager;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import static rs.ac.uns.ftn.museum.manager.ManagerDtos.*;

/** Read-only SQL aggregates; no visitor identity, QR token or payment reference is selected. */
@Repository
public class ManagerAnalyticsRepository {
    private final JdbcTemplate jdbc;
    public ManagerAnalyticsRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private static final String VISITS=" from poseta v join ulaznica t on t.id=v.ulaznica_id join termin_izlozbe tr on tr.id=t.termin_id join izlozba e on e.id=tr.izlozba_id join sala h on h.id=e.sala_id join muzej m on m.id=h.muzej_id where v.vreme_ulaska>=? and v.vreme_ulaska<? ";
    private static final String PAYMENTS=" from transakcija p join ulaznica t on t.id=p.ulaznica_id join termin_izlozbe tr on tr.id=t.termin_id join izlozba e on e.id=tr.izlozba_id where p.status='USPESNA' and p.datum_transakcije>=? and p.datum_transakcije<? ";
    private Object[] range(Instant from,Instant until){return new Object[]{Timestamp.from(from),Timestamp.from(until)};}
    private List<CountRow> counts(String key,String label,String source,Instant from,Instant until){
        return jdbc.query("select cast("+key+" as text) k,"+label+" label,count(*) n"+source+" group by 1,2 order by 1",
            (r,n)->new CountRow(r.getString("k"),r.getString("label"),r.getLong("n")),range(from,until));
    }
    private List<MoneyRow> money(String key,String label,Instant from,Instant until){
        return jdbc.query("select cast("+key+" as text) k,"+label+" label,sum(p.iznos) amount"+PAYMENTS+" group by 1,2 order by 1",
            (r,n)->new MoneyRow(r.getString("k"),r.getString("label"),r.getBigDecimal("amount")),range(from,until));
    }
    private String day(String column){return "to_char("+column+" at time zone 'Europe/Belgrade','YYYY-MM-DD')";}
    public Attendance attendance(Instant from,Instant until){
        var museums=counts("m.id","m.naziv",VISITS,from,until);
        return new Attendance(museums.stream().mapToLong(CountRow::count).sum(),museums,counts("e.id","e.naziv",VISITS,from,until),
            counts("tr.id","e.naziv || ' · ' || to_char(tr.pocetak at time zone 'Europe/Belgrade','YYYY-MM-DD HH24:MI') || ' (#' || tr.id || ')'",VISITS,from,until),
            counts(day("v.vreme_ulaska"),day("v.vreme_ulaska"),VISITS,from,until));
    }
    public Revenue revenue(Instant from,Instant until){
        var exhibitions=money("e.id","e.naziv",from,until);
        // EXISTS counts each paid ticket once, even if legacy data contains multiple successful attempts.
        BigDecimal paid=jdbc.queryForObject("select coalesce(sum(t.cena_placena),0) from ulaznica t where exists(select 1 from transakcija p where p.ulaznica_id=t.id and p.status='USPESNA' and p.datum_transakcije>=? and p.datum_transakcije<?)",BigDecimal.class,range(from,until));
        return new Revenue(exhibitions.stream().map(MoneyRow::amount).reduce(BigDecimal.ZERO,BigDecimal::add),paid,exhibitions,
            money("t.kategorija_cene","t.kategorija_cene",from,until),money("p.nacin_placanja","p.nacin_placanja",from,until),money(day("p.datum_transakcije"),day("p.datum_transakcije"),from,until));
    }
    public List<CountRow> tickets(Instant from,Instant until){
        var rows=counts("t.status","t.status"," from ulaznica t where t.datum_kupovine>=? and t.datum_kupovine<? ",from,until);
        return java.util.Arrays.stream(rs.ac.uns.ftn.museum.journey.domain.TicketStatus.values()).map(s->rows.stream().filter(r->r.key().equals(s.name())).findFirst().orElse(new CountRow(s.name(),s.name(),0))).toList();
    }
    public List<Popularity> popularity(Instant from,Instant until){
        return jdbc.query("""
            select p.id_ucesca,x.id,x.naziv exhibit,e.naziv exhibition,count(*) scans,count(distinct s.poseta_id) visits,
                   avg(s.trajanje_pregleda) average,count(s.trajanje_pregleda) measured
            from skeniranje_eksponata s join ucesce_eksponata_na_izlozbi p on p.id_ucesca=s.ucesce_id
            join eksponat x on x.id=p.eksponat_id join izlozba e on e.id=p.izlozba_id
            where s.vreme_skeniranja>=? and s.vreme_skeniranja<?
            group by p.id_ucesca,x.id,x.naziv,e.naziv order by scans desc,p.id_ucesca
            """,(r,n)->new Popularity(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getLong(5),r.getLong(6),r.getObject(7)==null?null:r.getDouble(7),r.getLong(8)),range(from,until));
    }
    public List<EventRow> events(Instant from,Instant until){
        return jdbc.query("""
            select e.id,e.naziv,e.pocetak,e.status,e.kapacitet,count(r.dogadjaj_id) total,
            count(*) filter(where r.status='PRIJAVLJEN') registered,
            count(*) filter(where r.status='PRISUSTVOVAO') attended,
            count(*) filter(where r.status='OTKAZAN') cancelled
            from dogadjaj e left join prijava_na_dogadjaj r on r.dogadjaj_id=e.id
            where e.pocetak>=? and e.pocetak<? group by e.id order by e.pocetak,e.id
            """,(r,n)->new EventRow(r.getLong(1),r.getString(2),r.getTimestamp(3).toInstant(),r.getString(4),r.getInt(5),r.getLong(6),r.getLong(7),r.getLong(8),r.getLong(9),r.getInt(5)==0?null:100.0*(r.getLong(7)+r.getLong(8))/r.getInt(5)),range(from,until));
    }
    private static final String RATINGS=" from ocena_izlozbe r join poseta v on v.id=r.poseta_id join ulaznica t on t.id=v.ulaznica_id join termin_izlozbe tr on tr.id=t.termin_id join izlozba e on e.id=tr.izlozba_id where r.datum_ocene>=? and r.datum_ocene<? ";
    public List<RatingRow> ratings(Instant from,Instant until){return jdbc.query("select e.id,e.naziv,count(*),avg(r.ocena)"+RATINGS+" group by e.id,e.naziv order by e.id",(r,n)->new RatingRow(r.getLong(1),r.getString(2),r.getLong(3),r.getDouble(4)),range(from,until));}
    public List<Comment> comments(Instant from,Instant until){return jdbc.query("select r.id,e.naziv,r.ocena,r.komentar,r.datum_ocene"+RATINGS+" and nullif(trim(r.komentar),'') is not null order by r.datum_ocene desc,r.id desc limit 201",(r,n)->new Comment(r.getLong(1),r.getString(2),r.getInt(3),r.getString(4),r.getTimestamp(5).toInstant()),range(from,until));}
    public List<CountRow> transitions(Instant from,Instant until){
        // Compute adjacency BEFORE the date filter; do not invent an ordering for equal timestamps.
        return jdbc.query("""
            with timed as (select s.*,count(*) over(partition by poseta_id,vreme_skeniranja) ties from skeniranje_eksponata s),
            sequenced as (select *,lead(ucesce_id) over w dest,lead(vreme_skeniranja) over w next_time,lead(ties) over w next_ties
                from timed window w as (partition by poseta_id order by vreme_skeniranja,id))
            select a.id_ucesca||'>'||b.id_ucesca k,
                e.naziv||': '||x.naziv||' (#'||a.id_ucesca||') → '||y.naziv||' (#'||b.id_ucesca||')' label,count(*) n
            from sequenced s join ucesce_eksponata_na_izlozbi a on a.id_ucesca=s.ucesce_id
            join ucesce_eksponata_na_izlozbi b on b.id_ucesca=s.dest
            join eksponat x on x.id=a.eksponat_id join eksponat y on y.id=b.eksponat_id join izlozba e on e.id=a.izlozba_id
            where s.ties=1 and s.next_ties=1 and s.ucesce_id<>s.dest and s.next_time>=? and s.next_time<?
            group by 1,2 order by n desc,k
            """,(r,n)->new CountRow(r.getString(1),r.getString(2),r.getLong(3)),range(from,until));
    }
    public List<ContentRow> contentInventory(){return jdbc.query("""
        select p.id_ucesca,x.naziv,e.naziv,d.jezik,d.tip_sadrzaja,count(*)
        from digitalni_sadrzaj d join ucesce_eksponata_na_izlozbi p on p.id_ucesca=d.ucesce_id
        join eksponat x on x.id=p.eksponat_id join izlozba e on e.id=p.izlozba_id
        group by p.id_ucesca,x.naziv,e.naziv,d.jezik,d.tip_sadrzaja order by p.id_ucesca,d.jezik,d.tip_sadrzaja
        """,(r,n)->new ContentRow(r.getLong(1),r.getString(2),r.getString(3),r.getString(4),r.getString(5),r.getLong(6)));}
}

package rs.ac.uns.ftn.museum.journey.service;

import java.util.*;
import org.neo4j.driver.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rs.ac.uns.ftn.museum.journey.domain.RouteCriterion;

/** Rebuildable, anonymous projection; never sends account/visitor/visit/payment identifiers. */
@Service
public class GraphProjection {
    // No queue: a stuck driver call must not accumulate request threads or tasks.
    private final java.util.concurrent.ExecutorService reads=new java.util.concurrent.ThreadPoolExecutor(
        0,1,30,java.util.concurrent.TimeUnit.SECONDS,new java.util.concurrent.SynchronousQueue<>(),
        task->{Thread thread=new Thread(task,"recommendation-read");thread.setDaemon(true);return thread;});
    @jakarta.annotation.PreDestroy
    public void close() { reads.shutdownNow(); }
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(GraphProjection.class);
    private final Driver driver;
    private final JdbcTemplate jdbc;
    private static final TransactionConfig SYNC_TIMEOUT=TransactionConfig.builder().withTimeout(java.time.Duration.ofSeconds(30)).build();
    private static final TransactionConfig READ_TIMEOUT=TransactionConfig.builder().withTimeout(java.time.Duration.ofSeconds(10)).build();
    public GraphProjection(Driver driver,JdbcTemplate jdbc) { this.driver=driver;this.jdbc=jdbc; }
    @Transactional(readOnly=true)
    public void synchronize() {
        List<Map<String,Object>> catalog=jdbc.queryForList("""
            select e.id, e.naziv as naziv, coalesce(e.tip,'') as tema,
             coalesce(e.godina_nastanka,'') as period, u.id_ucesca as ucesce,
             u.izlozba_id as izlozba, coalesce(u.redosled_obilaska,0) as redosled
            from eksponat e join ucesce_eksponata_na_izlozbi u on u.eksponat_id=e.id
            """);
        List<Map<String,Object>> authors=jdbc.queryForList("select autor_id as autor, eksponat_id as eksponat from autorstvo");
        List<Map<String,Object>> transitions=jdbc.queryForList("""
            select previous as prethodni, current as naredni, count(*) as broj from (
             select u.eksponat_id as current,
              lag(u.eksponat_id) over (partition by s.poseta_id order by s.vreme_skeniranja,s.id) as previous
             from skeniranje_eksponata s join ucesce_eksponata_na_izlozbi u on u.id_ucesca=s.ucesce_id
            ) x where previous is not null and previous<>current group by previous,current
            """);
        try(Session session=driver.session()) {
            session.executeWrite(tx->{
                tx.run("""
                    UNWIND $rows AS row
                    MERGE (e:Eksponat {id:row.id}) SET e.naziv=row.naziv, e.tema=row.tema, e.period=row.period
                    MERGE (i:Izlozba {id:row.izlozba})
                    MERGE (e)-[r:IZLOZEN_NA {ucesceId:row.ucesce}]->(i) SET r.redosled=row.redosled
                    """,Map.of("rows",catalog)).consume();
                tx.run("MATCH (:Autor)-[r:AUTOR_JE]->(:Eksponat) DELETE r").consume();
                tx.run("""
                    UNWIND $rows AS row MERGE (a:Autor {id:row.autor})
                    MERGE (e:Eksponat {id:row.eksponat}) MERGE (a)-[:AUTOR_JE]->(e)
                    """,Map.of("rows",authors)).consume();
                tx.run("MATCH (:Eksponat)-[r:CESTO_SLEDI]->(:Eksponat) SET r.broj=0").consume();
                tx.run("""
                    UNWIND $rows AS row MERGE (a:Eksponat {id:row.prethodni})
                    MERGE (b:Eksponat {id:row.naredni}) MERGE (a)-[r:CESTO_SLEDI]->(b) SET r.broj=row.broj
                    """,Map.of("rows",transitions)).consume();
                return null;
            },SYNC_TIMEOUT);
        }
    }
    public Map<Long,Double> scores(Long previous,List<Long> candidates,List<String> interests) {
        return scores(previous,candidates,interests,RouteCriterion.KOMBINACIJA);
    }
    public Map<Long,Double> scores(Long previous,List<Long> candidates,List<String> interests,RouteCriterion criterion) {
        if(candidates.isEmpty())return Map.of();
        java.util.concurrent.Future<Map<Long,Double>> pending=null;
        try {
            pending=reads.submit(()->readScores(previous,candidates,interests,criterion));
            return pending.get(2,java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();return Map.of();
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException | java.util.concurrent.RejectedExecutionException unavailable) {
            LOG.warn("Neo4j recommendation unavailable within budget; using PostgreSQL ordering");
            return Map.of();
        } finally { if(pending!=null && !pending.isDone())pending.cancel(true); }
    }
    private Map<Long,Double> readScores(Long previous,List<Long> candidates,List<String> interests,RouteCriterion criterion) {
        try(Session session=driver.session()) {
            return session.executeRead(tx->{
                Map<Long,Double> scores=new HashMap<>();
                tx.run("""
                    MATCH (e:Eksponat) WHERE e.id IN $ids
                    OPTIONAL MATCH (p:Eksponat {id:$previous})
                    OPTIONAL MATCH (p)-[f:CESTO_SLEDI]->(e)
                    WITH e,p,coalesce(f.broj,0) AS frequency
                    OPTIONAL MATCH (p)<-[:AUTOR_JE]-(a:Autor)-[:AUTOR_JE]->(e)
                    WITH e,p,frequency,count(DISTINCT a) AS sharedAuthors
                    RETURN e.id AS id, toFloat(frequency*$behavior
                     + CASE WHEN p.period<>'' AND p.period=e.period THEN 2*$related ELSE 0 END
                     + sharedAuthors*2*$related
                     + CASE WHEN any(x IN $interests WHERE toLower(e.tema) CONTAINS toLower(x)) THEN 5*$interest ELSE 0 END) AS score
                    """,Map.of("ids",candidates,"previous",previous==null?-1L:previous,"interests",interests,
                        "behavior",criterion==RouteCriterion.INTERESOVANJA?0:1,
                        "related",criterion==RouteCriterion.KOMBINACIJA?1:0,
                        "interest",criterion==RouteCriterion.POPULARNOST?0:1))
                    .list().forEach(r->scores.put(r.get("id").asLong(),r.get("score").asDouble()));
                return scores;
            },READ_TIMEOUT);
        } catch (RuntimeException unavailable) {
            LOG.warn("Neo4j recommendation query failed; using PostgreSQL ordering", unavailable);
            return Map.of(); // PostgreSQL ordering remains available even if graph is down.
        }
    }
}
